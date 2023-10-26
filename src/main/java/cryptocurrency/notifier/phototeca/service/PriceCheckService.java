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
        if (!subscribedUsers.isEmpty()) {
            List<RequestData> requestData = getDataFromApi();
            if (requestData != null) {
                subscribedUsers.forEach(user -> {
                    Long chatId = user.getChatId();
                    HashMap<String, BigDecimal> innerPrices = prices.get(chatId);
                    if (innerPrices == null) {
                        prices.put(chatId, new HashMap<>());
                        sendMessageToUser(chatId, null);
                        requestData.forEach(data -> prices.get(chatId).put(data.getSymbol(), data.getPrice()));
                    } else {
                        Map<String, BigDecimal> changedPrices = new HashMap<>();
                        requestData.forEach(data -> {
                            processPricesDifference(data, innerPrices, changedPrices);
                        });
                        sendMessageToUser(chatId, changedPrices);
                    }
                });
            }
        }
    }

    private void processPricesDifference(RequestData data, HashMap<String, BigDecimal> innerPrices, Map<String, BigDecimal> changedPrices) {
        String symbol = data.getSymbol();
        BigDecimal currentPrice = data.getPrice();
        BigDecimal initialPrice = innerPrices.get(symbol);
        BigDecimal priceChange = findPriceDifference(currentPrice, initialPrice);
        if (priceChange.abs().compareTo(N) >= 0) {
            changedPrices.put(symbol, priceChange);
        }
    }

    private BigDecimal findPriceDifference(BigDecimal initialPrice, BigDecimal currentPrice) {
        BigDecimal priceChange;
        if (initialPrice.compareTo(BigDecimal.ZERO) != 0) {
            priceChange = ((currentPrice.subtract(initialPrice)).divide(initialPrice, 9, RoundingMode.HALF_UP)).multiply(BigDecimal.valueOf(100));
        } else if (initialPrice.compareTo(BigDecimal.ZERO) == 0 && currentPrice.compareTo(BigDecimal.ZERO) != 0) {
            priceChange = BigDecimal.valueOf(100);
        } else {
            priceChange = BigDecimal.ZERO;
        }
        return priceChange;
    }

    private List<RequestData> getDataFromApi() {
        ResponseEntity<List<RequestData>> rateResponse =
                restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                });
        return rateResponse.getBody();
    }

    public void sendMessageToUser(Long chatId, Map<String, BigDecimal> changedPrices) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        if (changedPrices != null) {
            message.setText("Percentage changes: COIN-AMOUNT" + "\n" + changedPrices.entrySet());
        } else {
            message.setText("Initial prices set up..." + "\n" + "Loading price data for coins");
        }
        sendMessage(message);
    }

    private void sendMessage(SendMessage message) {
        try {
            telegramBot.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
