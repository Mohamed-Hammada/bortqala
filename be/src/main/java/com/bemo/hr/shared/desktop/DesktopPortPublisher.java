package com.bemo.hr.shared.desktop;

import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
public class DesktopPortPublisher implements ApplicationListener<WebServerInitializedEvent> {
    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        System.out.println("BEMO_BACKEND_PORT=" + event.getWebServer().getPort());
        System.out.flush();
    }
}
