package cryptocurrency.notifier.phototeca.bot;

import cryptocurrency.notifier.phototeca.model.User;
import cryptocurrency.notifier.phototeca.repository.UserRepository;
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

import java.util.Optional;

@Getter
@Log4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private Message requestMessage = new Message();
    private final SendMessage response = new SendMessage();
    private final String botUsername;
    private final String botToken;
    private final UserRepository userRepository;

    public TelegramBot(TelegramBotsApi telegramBotsApi, @Value("${bot.username}") String botUsername,
                       @Value("${bot.token}") String botToken,
                       UserRepository userRepository) throws TelegramApiException {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.userRepository = userRepository;
        telegramBotsApi.registerBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        requestMessage = update.getMessage();
        Long chatId = requestMessage.getChatId();
        response.setChatId(chatId.toString());
        log.debug(requestMessage.getText());
        if (requestMessage.getText().equals("/start")) {
            try {
                Optional<User> user = userRepository.findUserByChatId(chatId);
                if (user.isEmpty()) {
                    userRepository.save(User.builder()
                            .chatId(requestMessage.getChatId())
                            .telegramId(requestMessage.getFrom().getId())
                            .firstName(requestMessage.getFrom().getFirstName())
                            .lastName(requestMessage.getFrom().getLastName())
                            .userName(requestMessage.getFrom().getUserName())
                            .languageCode(requestMessage.getFrom().getLanguageCode())
                            .build()
                    );
                    defaultMsg(response, "Welcome! You have been added to our community. ");
                } else {
                    defaultMsg(response, "You're already with us");
                }

            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }

        if (requestMessage.getText().equals("/notify")) {
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
