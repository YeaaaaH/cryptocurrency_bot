package cryptocurrency.notifier.phototeca.controller;

import cryptocurrency.notifier.phototeca.service.PriceCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BotManagementController {
    private final PriceCheckService priceCheckService;

    public BotManagementController(PriceCheckService priceCheckService) {
        this.priceCheckService = priceCheckService;
    }

    @PostMapping("/restart")
    public ResponseEntity<String> restartAlgorithm() {
        priceCheckService.setPrices(null);
        return ResponseEntity.ok("Algorithm restarted.");
    }
}
