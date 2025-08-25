package com.coin.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coin.demo.domain.AssetType;
import com.coin.demo.domain.Investment;
import com.coin.demo.domain.OperationType;
import com.coin.demo.repository.InvestmentRepository;
import com.coin.demo.repository.PortfolioSnapshotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final InvestmentRepository investmentRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final PricingService pricingService;

    @Transactional
    public Investment recordTransaction(Long userId, AssetType type, BigDecimal amount, String currency,
            BigDecimal price, OperationType operationType, Instant date) {
        Investment investment = Investment.builder()
                .userId(userId)
                .type(type)
                .amount(amount)
                .currency(currency)
                .price(price)
                .operationType(operationType)
                .date(date == null ? Instant.now() : date)
                .build();
        return investmentRepository.save(investment);
    }

    @Transactional(readOnly = true)
    public Map<AssetType, BigDecimal> calculateCurrentBalances(Long userId) {
        List<Investment> investments = investmentRepository.findByUserId(userId);
        Map<AssetType, BigDecimal> totals = new EnumMap<>(AssetType.class);
        for (Investment inv : investments) {
            BigDecimal signed = inv.getOperationType() == OperationType.BUY ? inv.getAmount()
                    : inv.getAmount().negate();
            totals.merge(inv.getType(), signed, BigDecimal::add);
        }
        return totals.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Transactional(readOnly = true)
    public Map<AssetType, BigDecimal> calculateCurrentValuesToman(Long userId) {
        pricingService.refreshLivePricesIfStale();
        Map<AssetType, BigDecimal> balances = calculateCurrentBalances(userId);
        Map<AssetType, BigDecimal> values = new EnumMap<>(AssetType.class);
        for (Map.Entry<AssetType, BigDecimal> e : balances.entrySet()) {
            BigDecimal unit = pricingService.getUnitPriceToman(e.getKey());
            values.put(e.getKey(), unit.multiply(e.getValue()).setScale(2, RoundingMode.HALF_UP));
        }
        return values;
    }

    @Transactional(readOnly = true)
    public Map<AssetType, BigDecimal> calculatePnlByAsset(Long userId, Instant start, Instant end) {
        List<Investment> investments = investmentRepository.findByUserIdAndDateBetween(userId, start, end);
        Map<AssetType, BigDecimal> pnl = new EnumMap<>(AssetType.class);
        for (Investment inv : investments) {
            BigDecimal unitCurrent = pricingService.getUnitPriceToman(inv.getType());
            BigDecimal signedQty = inv.getOperationType() == OperationType.BUY ? inv.getAmount()
                    : inv.getAmount().negate();
            BigDecimal currentValue = unitCurrent.multiply(signedQty);

            BigDecimal unitPaidToman;
            switch (inv.getCurrency().toUpperCase()) {
                case "DOLLAR" -> unitPaidToman = pricingService.getUnitPriceToman(AssetType.DOLLAR);
                case "EURO" -> unitPaidToman = pricingService.getUnitPriceToman(AssetType.EURO);
                case "TOMAN" -> unitPaidToman = BigDecimal.ONE;
                default -> unitPaidToman = BigDecimal.ONE;
            }
            BigDecimal paidTotalToman = inv.getPrice().multiply(unitPaidToman);
            BigDecimal signedPaid = inv.getOperationType() == OperationType.BUY ? paidTotalToman
                    : paidTotalToman.negate();

            BigDecimal delta = currentValue.subtract(signedPaid);
            pnl.merge(inv.getType(), delta, BigDecimal::add);
        }
        // round values
        return pnl.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().setScale(2, RoundingMode.HALF_UP)));
    }

    @Transactional(readOnly = true)
    public Map<AssetType, BigDecimal> calculateAllocationPercent(Long userId) {
        Map<AssetType, BigDecimal> values = calculateCurrentValuesToman(userId);
        BigDecimal total = values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<AssetType, BigDecimal> pct = new EnumMap<>(AssetType.class);
        if (total.compareTo(BigDecimal.ZERO) == 0)
            return pct;
        for (Map.Entry<AssetType, BigDecimal> e : values.entrySet()) {
            BigDecimal p = e.getValue().divide(total, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            pct.put(e.getKey(), p);
        }
        return pct;
    }

    @Transactional
    public void snapshotNow(Long userId) {
        Map<AssetType, BigDecimal> values = calculateCurrentValuesToman(userId);
        BigDecimal total = values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        com.coin.demo.domain.PortfolioSnapshot s = new com.coin.demo.domain.PortfolioSnapshot();
        s.setUserId(userId);
        s.setCreatedAt(Instant.now());
        s.setTotalToman(total);
        s.setDollarValue(values.getOrDefault(AssetType.DOLLAR, BigDecimal.ZERO));
        s.setEuroValue(values.getOrDefault(AssetType.EURO, BigDecimal.ZERO));
        s.setCoinValue(values.getOrDefault(AssetType.COIN, BigDecimal.ZERO));
        s.setHalfCoinValue(values.getOrDefault(AssetType.HALF_COIN, BigDecimal.ZERO));
        s.setQuarterCoinValue(values.getOrDefault(AssetType.QUARTER_COIN, BigDecimal.ZERO));
        s.setCryptoValue(values.getOrDefault(AssetType.CRYPTO, BigDecimal.ZERO));
        snapshotRepository.save(s);
    }

    @Transactional(readOnly = true)
    public java.util.List<com.coin.demo.domain.PortfolioSnapshot> getSnapshots(Long userId, Instant start,
            Instant end) {
        return snapshotRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(userId, start, end);
    }
}
