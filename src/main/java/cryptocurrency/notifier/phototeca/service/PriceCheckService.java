package cryptocurrency.notifier.phototeca.service;

import cryptocurrency.notifier.phototeca.model.RequestData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
public class PriceCheckService {

    private Map<String, BigDecimal> prices;

    @Value("${crypto.price.url}")
    private String url;

    @Value("${crypto.percentage.change}")
    private BigDecimal N;
    private final RestTemplate restTemplate;

    public PriceCheckService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Scheduled(fixedRateString = "${fixedRate.in.milliseconds}")
    public void checkCryptocurrencyPrice() {
//        System.out.println("----------STARTED!----------");
        ResponseEntity<List<RequestData>> rateResponse =
                restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        List<RequestData> requestData = rateResponse.getBody();
        if (requestData != null) {
            if (prices == null) {
                prices = new HashMap<>();
                requestData.forEach(data -> prices.put(data.getSymbol(), data.getPrice()));
//                System.out.println("----INITIAL INSERT HAVE BEEN FINISHED!----");
            } else {
                List<String> changedPrices = new LinkedList<>();
                requestData.forEach(System.out::println);
                requestData.forEach(data -> {
                    String symbol = data.getSymbol();
                    BigDecimal currentPrice = data.getPrice();
                    BigDecimal initialPrice = prices.get(symbol);

                    if (initialPrice.compareTo(BigDecimal.ZERO) != 0) {
                        BigDecimal priceChange = ((currentPrice.subtract(initialPrice)).divide(initialPrice, 9, RoundingMode.HALF_UP)).multiply(BigDecimal.valueOf(100));

                        if (priceChange.abs().compareTo(N) >= 0) {
                            changedPrices.add(symbol);
//                            System.out.println("Price changed for " + symbol + " on " + priceChange + " percents");
//                        sendMessageToUsers("Price change: " + priceChange + "%");
                        }
                    } else if (initialPrice.compareTo(BigDecimal.ZERO) == 0 && currentPrice.compareTo(BigDecimal.ZERO) != 0) {
                        changedPrices.add(symbol);
//                        System.out.println("Price changed for " + symbol + " from ZERO to " + currentPrice);
                    }
                });
//                System.out.println(changedPrices.size());
//                System.out.println("------------ITERATION IS FINISHED!------------");
            }
        }
    }

    public void sendMessageToUsers(String message) {
        // Sending messages to users using the Telegram bot API
    }

    public void setPrices(Map<String, BigDecimal> prices) {
        this.prices = prices;
    }

}
