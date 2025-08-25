package com.coin.demo.service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.coin.demo.domain.AssetType;

@Service
public class PricingService {

    @Value("${pricing.fx.usd_to_toman:60000}")
    private BigDecimal usdToToman;

    @Value("${pricing.fx.eur_to_toman:65000}")
    private BigDecimal eurToToman;

    @Value("${pricing.asset.coin:45000000}")
    private BigDecimal coinToman;

    @Value("${pricing.asset.half_coin:23000000}")
    private BigDecimal halfCoinToman;

    @Value("${pricing.asset.quarter_coin:13000000}")
    private BigDecimal quarterCoinToman;

    @Value("${pricing.asset.crypto_btc_toman:4000000000}")
    private BigDecimal cryptoBtcToman;

    @Value("${pricing.live.enabled:false}")
    private boolean liveEnabled;

    @Value("${pricing.live.source:https://alanchand.com/en}")
    private String liveSourceUrl;

    public BigDecimal getUnitPriceToman(AssetType type) {
        return switch (type) {
            case DOLLAR -> usdToToman;
            case EURO -> eurToToman;
            case COIN -> coinToman;
            case HALF_COIN -> halfCoinToman;
            case QUARTER_COIN -> quarterCoinToman;
            case CRYPTO -> cryptoBtcToman; // naive: treat crypto as BTC for now
        };
    }

    private final AtomicLong lastFetchMs = new AtomicLong(0);
    @Value("${pricing.refresh.ms:300000}")
    private long refreshMs;

    public synchronized void refreshLivePricesIfStale() {
        long now = System.currentTimeMillis();
        if (now - lastFetchMs.get() < refreshMs)
            return;
        if (!liveEnabled) {
            lastFetchMs.set(now);
            return;
        }
        // Skip if jsoup is not on the classpath (e.g., before app restart after adding
        // dependency)
        try {
            Class.forName("org.jsoup.Jsoup");
        } catch (ClassNotFoundException e) {
            lastFetchMs.set(now);
            return;
        }
        try {
            Document doc = Jsoup.connect(liveSourceUrl).userAgent("Mozilla/5.0").timeout(10000).get();
            // Currencies (USD/EUR) - pick Buy Price
            Elements rows = doc.select("table:contains(Currency Name) tr");
            for (Element tr : rows) {
                Elements tds = tr.select("td");
                if (tds.size() >= 3) {
                    String name = tds.get(0).text().trim();
                    String buy = tds.get(1).text().replace(",", "").trim();
                    try {
                        BigDecimal val = new BigDecimal(buy);
                        if (name.equalsIgnoreCase("US Dollar"))
                            usdToToman = val;
                        if (name.equalsIgnoreCase("Euro"))
                            eurToToman = val;
                    } catch (Exception ignored) {
                    }
                }
            }
            // Gold coin prices
            Elements h3s = doc.select("h3");
            for (Element h3 : h3s) {
                String txt = h3.text();
                Element priceEl = h3.nextElementSibling();
                if (priceEl != null) {
                    String priceTxt = priceEl.text().replace(",", "").trim();
                    try {
                        BigDecimal val = new BigDecimal(priceTxt);
                        if (txt.contains("Full Coin") || txt.contains("Imami"))
                            coinToman = val;
                        // NOTE: page may not list half/quarter; leave configured if not found
                    } catch (Exception ignored) {
                    }
                }
            }
            lastFetchMs.set(now);
        } catch (Throwable ignored) {
            // jsoup absent or network errors; keep existing configured values
        }
    }
}
