package cryptocurrency.notifier.phototeca.bot;

import cryptocurrency.notifier.phototeca.model.User;
import cryptocurrency.notifier.phototeca.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Optional;

import static cryptocurrency.notifier.phototeca.service.PriceCheckService.prices;

@Getter
@Log4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Value("${bot.username}")
    private String botUsername;
    @Value("${bot.token}")
    private String botToken;
    private final UserService userService;

    public TelegramBot(UserService userService) {
        this.userService = userService;

    }

    @PostConstruct
    public void initBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
        } catch (TelegramApiException e) {
            log.error("Error initializing Telegram Bot {}", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message requestMessage = update.getMessage();
        SendMessage response = new SendMessage();
        response.setChatId(requestMessage.getChatId().toString());
        handleMessage(requestMessage, response);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
//TODO implement queue to process messages
    private void defaultMsg(SendMessage response, String responseMessage) {
        response.setText(responseMessage);
        try {
            execute(response);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
//TODO refactor string responseMessages into constants
    private void handleMessage(Message requestMessage, SendMessage response) {
        Long chatId = requestMessage.getChatId();
        if (requestMessage.getText().equals("/start")) {
            handleStartMessage(requestMessage, response, chatId);
        } else if (requestMessage.getText().equals("/notify")) {
            handleNotifyMessage(response, chatId);
        } else if (requestMessage.getText().equals("/restart")) {
            handleRestartMessage(response, chatId);
        }
    }

    private void handleStartMessage(Message requestMessage, SendMessage response, Long chatId) {
        String responseMessage;
        Optional<User> user = userService.findUserByChatId(chatId);
        if (user.isEmpty()) {
            userService.saveTgUser(requestMessage);
            responseMessage = "Welcome! You have been added to our community";
        } else {
            responseMessage = "You're already with us";
        }
        defaultMsg(response, responseMessage);
    }

    private void handleNotifyMessage(SendMessage response, Long chatId) {
        String responseMessage;
        Optional<User> user = userService.findUserByChatId(chatId);
        if (user.isEmpty()) {
            responseMessage = "You haven't been joined yet, send me a '/start' message";
        } else if (!user.get().isSubscribed()) {
            User updateUser = user.get();
            updateUser.setSubscribed(true);
            userService.updateUser(updateUser);
            responseMessage = "Starting cryptocurrency check...";
        } else {
            responseMessage = "You've already been subscribed for updates";
        }
        defaultMsg(response, responseMessage);
    }

    private void handleRestartMessage(SendMessage response, Long chatId) {
        String responseMessage;
        if (prices.get(chatId) == null) {
            responseMessage = "You are not subscribed yet, send me a '/notify' message to receive updates";
        } else {
            prices.remove(chatId);
            responseMessage = "Restarting cryptocurrency check...";
        }
        defaultMsg(response, responseMessage);
    }
}
