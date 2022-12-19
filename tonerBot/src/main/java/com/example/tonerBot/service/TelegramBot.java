package com.example.tonerBot.service;

import com.example.tonerBot.config.BotConfig;
import com.example.tonerBot.model.User;
import com.example.tonerBot.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Clock;
import java.time.Instant;


@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;
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
        //проверка пришло ли сообщение и содержит ли оно текст. если да то присваеваем переменной это сообщение
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                //если получили сообщение старт делаем то то
                case "/start":
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/time":
                    sendUtcTime(chatId);
                    break;
                default:
                    sendMessage(chatId, "Command was not supported");

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

        try {
            execute(message);
        } catch (TelegramApiException e) {

        }
    }
}
