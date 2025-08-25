package com.coin.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.coin.demo.domain.Investment;
import com.coin.demo.domain.OperationType;
import com.coin.demo.repository.InvestmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ValuationService {

    private final InvestmentRepository investmentRepository;
    private final PricingService pricingService;

    public ValuationSummary computePortfolioValuation(Long userId) {
        pricingService.refreshLivePricesIfStale();
        List<Investment> list = investmentRepository.findByUserId(userId);
        BigDecimal initialToman = BigDecimal.ZERO;
        BigDecimal currentToman = BigDecimal.ZERO;

        for (Investment inv : list) {
            // ==== CURRENT VALUE ====
            BigDecimal amount = inv.getAmount(); // units (e.g. 2 gold coins, 100 USD, etc.)
            BigDecimal unitCurrent = pricingService.getUnitPriceToman(inv.getType());
            BigDecimal signedQty = inv.getOperationType() == OperationType.BUY ? amount : amount.negate();
            currentToman = currentToman.add(unitCurrent.multiply(signedQty));

            // ==== INITIAL COST ====
            // convert original payment into toman using FX
            BigDecimal unitPaidToman;
            switch (inv.getCurrency().toUpperCase()) {
                case "DOLLAR" ->
                    unitPaidToman = pricingService.getUnitPriceToman(com.coin.demo.domain.AssetType.DOLLAR);
                case "EURO" ->
                    unitPaidToman = pricingService.getUnitPriceToman(com.coin.demo.domain.AssetType.EURO);
                case "TOMAN" ->
                    unitPaidToman = BigDecimal.ONE;
                default ->
                    unitPaidToman = BigDecimal.ONE; // fallback: already in toman
            }

            // here: inv.getPrice() is TOTAL paid in that currency
            BigDecimal paidTotalToman = inv.getPrice().multiply(unitPaidToman);
            BigDecimal signedTotal = inv.getOperationType() == OperationType.BUY
                    ? paidTotalToman
                    : paidTotalToman.negate();

            initialToman = initialToman.add(signedTotal);
        }

        BigDecimal init = initialToman.setScale(2, RoundingMode.HALF_UP);
        BigDecimal curr = currentToman.setScale(2, RoundingMode.HALF_UP);
        BigDecimal roi = init.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : curr.subtract(init).divide(init, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return new ValuationSummary(init, curr, roi.setScale(2, RoundingMode.HALF_UP));
    }

    public record ValuationSummary(BigDecimal initialToman, BigDecimal currentToman, BigDecimal roiPercent) {
    }
}
