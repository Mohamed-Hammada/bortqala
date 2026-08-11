package org.example.zkteco.gateway.config;

import org.example.zkteco.adapter.adms.AdmsCommandQueue;
import org.example.zkteco.adapter.adms.AdmsIngressStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdmsConfiguration {
    @Bean
    public AdmsIngressStore admsIngressStore() {
        return new AdmsIngressStore(10_000);
    }

    @Bean
    public AdmsCommandQueue admsCommandQueue() {
        return new AdmsCommandQueue();
    }
}
