package cryptocurrency.notifier.phototeca.service;

import cryptocurrency.notifier.phototeca.model.User;
import cryptocurrency.notifier.phototeca.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findUserByChatId(Long chatId) {
        return userRepository.findUserByChatId(chatId);
    }

    public void saveUser(Message requestMessage) {
        userRepository.save(User.builder()
                .chatId(requestMessage.getChatId())
                .telegramId(requestMessage.getFrom().getId())
                .firstName(requestMessage.getFrom().getFirstName())
                .lastName(requestMessage.getFrom().getLastName())
                .userName(requestMessage.getFrom().getUserName())
                .languageCode(requestMessage.getFrom().getLanguageCode())
                .build()
        );
    }

    public void updateUser(User user) {
        userRepository.save(user);
    }
}
