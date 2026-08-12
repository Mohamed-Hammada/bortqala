package com.bemo.hr.notification.push;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WebPushNotificationListener {
    private final WebPushService webPushService;

    @Async("webPushTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterNotificationCommitted(NotificationCreatedEvent event) {
        webPushService.deliver(event);
    }
}
