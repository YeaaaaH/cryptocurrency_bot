package cryptocurrency.notifier.phototeca.repository;

import cryptocurrency.notifier.phototeca.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository  extends JpaRepository<User, Long> {
    Optional<User> findUserByChatId(Long chatId);
    List<User> findBySubscribedTrue();
}
