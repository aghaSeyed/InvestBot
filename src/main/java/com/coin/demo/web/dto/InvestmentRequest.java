package com.coin.demo.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.coin.demo.domain.AssetType;
import com.coin.demo.domain.OperationType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InvestmentRequest {
    @NotNull
    private Long userId;

    @NotNull
    private AssetType type;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private String currency;

    @NotNull
    private BigDecimal price;

    @NotNull
    private OperationType operationType;

    private Instant date;
}
