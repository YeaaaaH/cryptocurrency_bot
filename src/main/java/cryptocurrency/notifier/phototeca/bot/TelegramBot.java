package cryptocurrency.notifier.phototeca.bot;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Getter
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private Message requestMessage = new Message();
    private final SendMessage response = new SendMessage();
    private final String botUsername;
    private final String botToken;

    public TelegramBot(TelegramBotsApi telegramBotsApi, @Value("${bot.username}") String botUsername,
                       @Value("${bot.token}") String botToken) throws TelegramApiException {
        this.botUsername = botUsername;
        this.botToken = botToken;
        telegramBotsApi.registerBot(this);
    }


    @Override
    public void onUpdateReceived(Update update) {
        requestMessage = update.getMessage();
        response.setChatId(requestMessage.getChatId().toString());

        if (requestMessage.getText().equals("/start")) {
            try {
                defaultMsg(response, "Starting cryptocurrency check...");
                //starting logic
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }

        if (requestMessage.getText().equals("/restart")) {
            try {
                defaultMsg(response, "Restarting cryptocurrency check...");
                //logic of restart
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    private void defaultMsg(SendMessage response, String msg) throws TelegramApiException {
        response.setText(msg);
        execute(response);
    }
}
