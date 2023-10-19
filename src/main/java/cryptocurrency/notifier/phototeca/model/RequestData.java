package cryptocurrency.notifier.phototeca.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RequestData {
    private String symbol;
    private BigDecimal price;
}
