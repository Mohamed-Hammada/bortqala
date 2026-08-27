package com.bemo.hr.finance.paylink.api;

import com.bemo.hr.finance.paylink.application.PaymentLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/p")
@RequiredArgsConstructor
public class PublicPaymentController {

    private final PaymentLinkService service;

    @GetMapping("/{token}")
    public PaylinkApi.PublicPagePayload getPage(@PathVariable String token) {
        return service.getPublicPage(token);
    }

    @PostMapping("/{token}/webhook")
    public void webhook(@PathVariable String token,
                        @RequestBody PaylinkApi.WebhookPayload payload) {
        service.handleWebhook(token, payload);
    }
}
