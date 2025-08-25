package com.coin.demo.bot;

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.coin.demo.domain.AssetType;
import com.coin.demo.domain.OperationType;
import com.coin.demo.service.ChartService;
import com.coin.demo.service.PortfolioService;

import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "telegram.bot", name = "enabled", havingValue = "true")
public class TelegramBotService extends TelegramLongPollingBot {

    private final PortfolioService portfolioService;
    private final ChartService chartService;
    private final TelegramProperties properties;

    @Override
    public String getBotUsername() {
        return properties.getUsername();
    }

    @Override
    public String getBotToken() {
        return properties.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        String chatId = update.getMessage().getChatId().toString();
        String text = update.getMessage().getText().trim();

        try {
            if (text.startsWith("/buy")) {
                handleBuy(chatId, text);
            } else if (text.startsWith("/sell")) {
                handleSell(chatId, text);
            } else if (text.startsWith("/portfolio")) {
                handlePortfolio(chatId, text);
            } else {
                sendText(chatId, "Commands: /buy, /sell, /portfolio");
            }
        } catch (Exception e) {
            sendText(chatId, "Error: " + e.getMessage());
        }
    }

    private void handleBuy(String chatId, String text) {
        // Format: /buy <ASSET> <AMOUNT> <CURRENCY> <PRICE>
        String[] parts = text.split("\\s+");
        if (parts.length < 5) {
            sendText(chatId, "Usage: /buy <ASSET> <AMOUNT> <CURRENCY> <PRICE>");
            return;
        }
        AssetType type = AssetType.valueOf(parts[1].toUpperCase());
        BigDecimal amount = new BigDecimal(parts[2]);
        String currency = parts[3];
        BigDecimal price = new BigDecimal(parts[4]);
        portfolioService.recordTransaction(Long.valueOf(chatId), type, amount, currency, price, OperationType.BUY,
                Instant.now());
        sendText(chatId, "Recorded BUY of " + amount + " " + type + ".");
    }

    private void handleSell(String chatId, String text) {
        // Format: /sell <ASSET> <AMOUNT> <CURRENCY> <PRICE>
        String[] parts = text.split("\\s+");
        if (parts.length < 5) {
            sendText(chatId, "Usage: /sell <ASSET> <AMOUNT> <CURRENCY> <PRICE>");
            return;
        }
        AssetType type = AssetType.valueOf(parts[1].toUpperCase());
        BigDecimal amount = new BigDecimal(parts[2]);
        String currency = parts[3];
        BigDecimal price = new BigDecimal(parts[4]);
        portfolioService.recordTransaction(Long.valueOf(chatId), type, amount, currency, price, OperationType.SELL,
                Instant.now());
        sendText(chatId, "Recorded SELL of " + amount + " " + type + ".");
    }

    private void handlePortfolio(String chatId, String text) throws Exception {
        Map<AssetType, java.math.BigDecimal> balances = portfolioService.calculateCurrentBalances(Long.valueOf(chatId));
        if (balances.isEmpty()) {
            sendText(chatId, "Your portfolio is empty.");
            return;
        }
        StringBuilder sb = new StringBuilder("Portfolio:\n");
        balances.forEach((k, v) -> sb.append(k.name()).append(": ").append(v).append('\n'));
        sendText(chatId, sb.toString());

        File chart = chartService.generatePortfolioPieChart(balances, "Portfolio");
        SendPhoto photo = new SendPhoto(chatId, new InputFile(chart));
        try {
            execute(photo);
        } finally {
            chart.delete();
        }
    }

    private void sendText(String chatId, String msg) {
        SendMessage sm = new SendMessage(chatId, msg);
        try {
            execute(sm);
        } catch (Exception ignored) {
        }
    }
}
