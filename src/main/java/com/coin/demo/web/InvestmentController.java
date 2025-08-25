package com.coin.demo.web;

import java.io.File;
import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.Instant;

import com.coin.demo.domain.AssetType;
import com.coin.demo.service.ChartService;
import com.coin.demo.service.PortfolioService;
import com.coin.demo.service.InvestmentService;
import com.coin.demo.service.ValuationService;
import com.coin.demo.service.InvestmentService;
import com.coin.demo.web.dto.InvestmentRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InvestmentController {

    private final PortfolioService portfolioService;
    private final ChartService chartService;
    private final InvestmentService investmentService;
    private final ValuationService valuationService;

    @PostMapping("/investments")
    public ResponseEntity<?> create(@Validated @RequestBody InvestmentRequest req) {
        portfolioService.recordTransaction(
                req.getUserId(),
                req.getType(),
                req.getAmount(),
                req.getCurrency(),
                req.getPrice(),
                req.getOperationType(),
                req.getDate());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/investments/{userId}")
    public ResponseEntity<java.util.List<com.coin.demo.domain.Investment>> list(@PathVariable Long userId) {
        return ResponseEntity.ok(investmentService.getInvestmentsForUser(userId));
    }

    @GetMapping("/portfolio/{userId}")
    public ResponseEntity<Map<AssetType, BigDecimal>> portfolio(@PathVariable Long userId) {
        return ResponseEntity.ok(portfolioService.calculateCurrentBalances(userId));
    }

    @GetMapping("/portfolio/{userId}/valuation")
    public ResponseEntity<com.coin.demo.service.ValuationService.ValuationSummary> valuation(
            @PathVariable Long userId) {
        return ResponseEntity.ok(valuationService.computePortfolioValuation(userId));
    }

    @GetMapping("/investments/{userId}/page")
    public ResponseEntity<org.springframework.data.domain.Page<com.coin.demo.domain.Investment>> page(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("date").descending());
        return ResponseEntity.ok(investmentService.getInvestmentsPage(userId, pageable));
    }

    @GetMapping(value = "/portfolio/{userId}/chart", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> portfolioChart(@PathVariable Long userId) throws Exception {
        Map<AssetType, BigDecimal> data = portfolioService.calculateCurrentValuesToman(userId);
        File file = chartService.generatePortfolioPieChart(data, "Portfolio (Toman)");
        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/portfolio/{userId}/values")
    public ResponseEntity<Map<AssetType, BigDecimal>> portfolioValues(@PathVariable Long userId) {
        return ResponseEntity.ok(portfolioService.calculateCurrentValuesToman(userId));
    }

    @GetMapping("/portfolio/{userId}/pnl")
    public ResponseEntity<Map<AssetType, BigDecimal>> pnl(
            @PathVariable Long userId,
            @RequestParam("start") String start,
            @RequestParam("end") String end) {
        Instant s = Instant.parse(start);
        Instant e = Instant.parse(end);
        return ResponseEntity.ok(portfolioService.calculatePnlByAsset(userId, s, e));
    }

    @GetMapping("/portfolio/{userId}/allocation")
    public ResponseEntity<Map<AssetType, BigDecimal>> allocation(@PathVariable Long userId) {
        return ResponseEntity.ok(portfolioService.calculateAllocationPercent(userId));
    }
}
