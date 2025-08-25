package com.coin.demo.bot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
@Data
public class TelegramProperties {
    private boolean enabled;
    private String username;
    private String token;
}
