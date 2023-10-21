package cryptocurrency.notifier.phototeca.service;

import cryptocurrency.notifier.phototeca.bot.TelegramBot;
import cryptocurrency.notifier.phototeca.model.RequestData;
import cryptocurrency.notifier.phototeca.model.User;
import cryptocurrency.notifier.phototeca.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PriceCheckService {
//TODO implement redis HSET instead
    public static Map<Long, HashMap<String, BigDecimal>> prices = new HashMap<>();

    @Value("${crypto.price.url}")
    private String url;

    @Value("${crypto.percentage.change}")
    private BigDecimal N;
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final TelegramBot telegramBot;

    public PriceCheckService(RestTemplate restTemplate, UserRepository userRepository, TelegramBot telegramBot) {
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
        this.telegramBot = telegramBot;
    }

    @Scheduled(fixedRateString = "${fixedRate.in.milliseconds}")
    public void checkCryptocurrencyPrice() {
        List<User> subscribedUsers = userRepository.findBySubscribedTrue();
        ResponseEntity<List<RequestData>> rateResponse =
                restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        List<RequestData> requestData = rateResponse.getBody();
        if (requestData != null) {
            subscribedUsers.forEach(user -> {
                Long chatId = user.getChatId();
                HashMap<String, BigDecimal> innerPrices = prices.get(chatId);
                if (innerPrices == null) {
                    prices.put(chatId, new HashMap<>());
                    sendInitMessageToUser(chatId);
                    requestData.forEach(data -> prices.get(chatId).put(data.getSymbol(), data.getPrice()));
                } else {
                    Map<String, BigDecimal> changedPrices = new HashMap<>();
                    requestData.forEach(data -> {
                        String symbol = data.getSymbol();
                        BigDecimal currentPrice = data.getPrice();
                        BigDecimal initialPrice = innerPrices.get(symbol);

                        if (initialPrice.compareTo(BigDecimal.ZERO) != 0) {
                            BigDecimal priceChange = ((currentPrice.subtract(initialPrice)).divide(initialPrice, 9, RoundingMode.HALF_UP)).multiply(BigDecimal.valueOf(100));

                            if (priceChange.abs().compareTo(N) >= 0) {
                                changedPrices.put(symbol, priceChange);
                            }
                        } else if (initialPrice.compareTo(BigDecimal.ZERO) == 0 && currentPrice.compareTo(BigDecimal.ZERO) != 0) {
                            changedPrices.put(symbol, BigDecimal.valueOf(100));
                        }
                    });
                    sendMessageToUser(chatId, changedPrices);
                }
            });
        }
    }

    public void sendMessageToUser(Long chatId, Map<String, BigDecimal> changedPrices) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Percentage changes: COIN-AMOUNT" + "\n" +  changedPrices.entrySet());
        try {
            telegramBot.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendInitMessageToUser(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Initial prices set up..." + "\n" + "Loading price data for coins");
        try {
            telegramBot.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
