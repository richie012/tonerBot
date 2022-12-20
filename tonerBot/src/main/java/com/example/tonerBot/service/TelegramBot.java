package com.example.tonerBot.service;

import com.example.tonerBot.config.BotConfig;
import com.example.tonerBot.model.DailyOrder;
import com.example.tonerBot.model.Order;
import com.example.tonerBot.model.User;
import com.example.tonerBot.model.WeeklyOrder;
import com.example.tonerBot.repo.DailyOrderRepository;
import com.example.tonerBot.repo.OrderRepository;
import com.example.tonerBot.repo.UserRepository;
import com.example.tonerBot.repo.WeeklyOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
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
    final BotConfig config;

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
            if (messageText.contains("-З") || messageText.contains("-з")) {
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
                    case "Заказы на сегодня":
                        var orders = dailyOrderRepository.findAll();
                        var dailyOrdersCount = dailyOrderRepository.count();

                        for (DailyOrder dailyOrder: orders){
                            String message = dailyOrder.getName()+" " + dailyOrder.getComment() + " " + dailyOrder.getDate();
                            sendMessage(chatId, message);
                        }
                        sendMessage(chatId, "Всего заказов:" + dailyOrdersCount);
                        break;
                    case "Баланс/Заказы на неделю":
                        var оrders = weeklyOrderRepository.findAll();
                        var weeklyOrdersCount = weeklyOrderRepository.count();
                        for (WeeklyOrder weeklyOrders: оrders){
                            String message = weeklyOrders.getName()+" " + weeklyOrders.getComment() + " " + weeklyOrders.getDate();
                            sendMessage(chatId, message);
                        }
                        sendMessage(chatId, "Всего заказов: " + weeklyOrdersCount + "\n" + "Баланс: " +weeklyOrdersCount * 400 );//за один заказ я получаю 400 рублей

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
                        row.add("Архив");
                        keyboardRows.add(row);

                        keyboardMarkup.setKeyboard(keyboardRows);
                        mainMenu.setReplyMarkup(keyboardMarkup);

                        try {
                            execute(mainMenu);
                        } catch (TelegramApiException e) {

                        }
                        break;

                    default:
                        sendMessage(chatId, "Command was not supported");

                }
            }
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

//        row.add("Добавить заказ");
        row.add("Главное меню");
        keyboardRows.add(row);


        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);


        try {
            execute(message);
        } catch (TelegramApiException e) {

        }
    }
}
