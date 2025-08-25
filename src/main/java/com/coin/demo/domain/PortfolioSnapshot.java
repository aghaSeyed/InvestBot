package com.coin.demo.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Instant createdAt;

    private BigDecimal totalToman;

    private BigDecimal dollarValue;
    private BigDecimal euroValue;
    private BigDecimal coinValue;
    private BigDecimal halfCoinValue;
    private BigDecimal quarterCoinValue;
    private BigDecimal cryptoValue;
}


