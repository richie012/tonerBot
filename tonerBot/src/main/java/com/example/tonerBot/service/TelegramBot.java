package com.example.tonerBot.service;

import com.example.tonerBot.config.BotConfig;
import com.example.tonerBot.model.*;
import com.example.tonerBot.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private DailyOrderRepository dailyOrderRepository;
    @Autowired
    private WeeklyOrderRepository weeklyOrderRepository;
    @Autowired
    private ReturnedOrderRepository returnedOrderRepository;
    final BotConfig config;
    static final String DELETE_ORDER_BUTTON = "DELETE_ORDER_BUTTON";
    static final String DELETE_RETURNED_ORDER_BUTTON = "DELETE_RETURNED_ORDER_BUTTON";
    public TelegramBot(BotConfig config) {
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            if (messageText.contains("-В") || messageText.contains("-в")){
                var returnedOrderName = messageText.substring(messageText.indexOf(" "));
                registerReturnedOrder(returnedOrderName);
                sendMessage(chatId, "Заказ добавлен в возвраты");
            }
            else if (messageText.contains("-З") || messageText.contains("-з")) {
                var orderNameAndComment = messageText.substring(messageText.indexOf(" "));
                registerOrder(orderNameAndComment);
                sendMessage(chatId, "Заказ добавлен");
            } else {
                switch (messageText) {
                    //если получили сообщение старт делаем то то
                    case "/start":
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/time":
                        sendUtcTime(chatId);
                        break;
                    case "Главное меню":
                        SendMessage mainMenu = new SendMessage();
                        mainMenu.setChatId(String.valueOf(chatId));
                        var mainMenuText = "Главное меню";
                        mainMenu.setText(mainMenuText);

                        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                        keyboardMarkup.setResizeKeyboard(true);

                        List<KeyboardRow> keyboardRows = new ArrayList<>();

                        KeyboardRow row = new KeyboardRow();

                        row.add("Заказы на сегодня");
                        keyboardRows.add(row);

                        row = new KeyboardRow();

                        row.add("Баланс/Заказы на неделю");
                        row.add("Возвраты");
                        keyboardRows.add(row);

                        keyboardMarkup.setKeyboard(keyboardRows);
                        mainMenu.setReplyMarkup(keyboardMarkup);

                        try {
                            execute(mainMenu);
                        } catch (TelegramApiException e) {

                        }
                        break;

                    case "Заказы на сегодня":
                        var orders = dailyOrderRepository.findAll();
                        var dailyOrdersCount = dailyOrderRepository.count();

                        for (DailyOrder dailyOrder : orders) {
                            String text = dailyOrder.getOrderId() + " " + dailyOrder.getName() + " " + dailyOrder.getComment() + " " + dailyOrder.getDate();//соединяем id, имя, коммент и дату создания заказа

                            setDeleteInLineButtonOnDailyOrWeeklyOrders(chatId, text);

                        }
                        sendMessage(chatId, "Всего заказов:" + dailyOrdersCount);
                        break;
                    case "Баланс/Заказы на неделю":
                        var weeklyOrdersCount = weeklyOrderRepository.count();
                        var weeklyOrders = weeklyOrderRepository.findAll();

                        for (WeeklyOrder weeklyOrder : weeklyOrders){
                            String text = weeklyOrder.getOrderId() + " " + weeklyOrder.getName() + " " + weeklyOrder.getComment() + " " + weeklyOrder.getDate();//соединяем id, имя, коммент и дату создания заказа

                            sendMessage(chatId, text);
                        }
                        sendMessage(chatId, "Всего заказов: " + weeklyOrdersCount + "\nБаланс: " + weeklyOrdersCount*400);

                        break;
                    case "Возвраты":
                        var returnedOrders = returnedOrderRepository.findAll();
                        for (ReturnedOrder returnedOrder: returnedOrders){
                            String text = returnedOrder.getOrderId() + " " + returnedOrder.getName() + " " + returnedOrder.getComment() + " " + returnedOrder.getDate();//соединяем id, имя, коммент и дату создания заказа
                            setDeleteInLineButtonOnReturnedOrders(chatId, text);
                        }
                        break;

                    default:
                        sendMessage(chatId, "Command was not supported");

                }
            }

        }
        else if(update.hasCallbackQuery()){
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String callbackMessageText = update.getCallbackQuery().getMessage().getText();
            long orderId = Long.parseLong(callbackMessageText.substring(0, callbackMessageText.indexOf(" ")));

            if(callbackData.equals(DELETE_ORDER_BUTTON)){
                deleteOrder(orderId);
                String text = "Заказ удален из баз данных";
                executeEditMessageText(text, chatId, messageId);
            }
            else if(callbackData.equals(DELETE_RETURNED_ORDER_BUTTON)){
                deleteReturnedOrder(orderId);
                String text = "Заказ удален из возвратов";
                executeEditMessageText(text, chatId, messageId);
            }
//            else if (callbackData.equals(SHOW_ORDERS)){
//                sendWeeklyOrders(chatId);
//            }

        }
    }

    private void setDeleteInLineButtonOnDailyOrWeeklyOrders(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var deleteOrderButton = new InlineKeyboardButton();
        deleteOrderButton.setText("Удалить заказ");
        deleteOrderButton.setCallbackData(DELETE_ORDER_BUTTON);


        rowInLine.add(deleteOrderButton);
        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void setDeleteInLineButtonOnReturnedOrders(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var deleteOrderButton = new InlineKeyboardButton();
        deleteOrderButton.setText("Удалить заказ");
        deleteOrderButton.setCallbackData(DELETE_RETURNED_ORDER_BUTTON);


        rowInLine.add(deleteOrderButton);
        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }
    private void deleteOrder(long orderId){
        dailyOrderRepository.deleteById(orderId);
        weeklyOrderRepository.deleteById(orderId);
    }
    private void deleteReturnedOrder(long orderId){
        returnedOrderRepository.deleteById(orderId);
    }

    private void executeEditMessageText(String text, long chatId, long messageId){
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
        }
    }
    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();
            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setRegisteredTime(getTime());

            userRepository.save(user);
        }
    }

    private void registerOrder(String orderText) {

        String[] nameAndComment = orderText.split(" ");//-З мерс%5 коммент

        String orderName = nameAndComment[1];
        String date = getTime();
        String comment = nameAndComment[2];
        for (int i = 3; i < nameAndComment.length; i++) {
            comment = comment + " " + nameAndComment[i];
        }

        Order order = new Order();
        DailyOrder dailyOrder = new DailyOrder();
        WeeklyOrder weeklyOrder = new WeeklyOrder();

        order.setName(orderName);
        order.setDate(date);
        order.setComment(comment);

        dailyOrder.setName(orderName);
        dailyOrder.setDate(date);
        dailyOrder.setComment(comment);

        weeklyOrder.setName(orderName);
        weeklyOrder.setDate(date);
        weeklyOrder.setComment(comment);

        orderRepository.save(order);
        dailyOrderRepository.save(dailyOrder);
        weeklyOrderRepository.save(weeklyOrder);

    }
    private void registerReturnedOrder(String orderText){
        String[] nameAndComment = orderText.split(" ");//-З мерс%5 коммент

        String orderName = nameAndComment[1];
        String date = getTime();
        String comment = nameAndComment[2];
        for (int i = 3; i < nameAndComment.length; i++) {
            comment = comment + " " + nameAndComment[i];
        }

        ReturnedOrder returnedOrder = new ReturnedOrder();

        returnedOrder.setName(orderName);
        returnedOrder.setDate(date);
        returnedOrder.setComment(comment);

        returnedOrderRepository.save(returnedOrder);
    }
    private void startCommandReceived(long chatId, String name) {
        String answer = "Привет " + name;
        sendMessage(chatId, answer);
    }

    private void sendUtcTime(long chatId) {
        sendMessage(chatId, getTime());

    }

    private static String getTime() {
        Clock clock = Clock.systemUTC();
        Instant instant = clock.instant();
        String time = instant.toString().substring(0, 10) + "\n" + instant.toString().substring(11, 19);//2022-12-01T19:20:25.893070481Z
        return time;
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("Главное меню");
        keyboardRows.add(row);


        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);


        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
        }
    }
}
