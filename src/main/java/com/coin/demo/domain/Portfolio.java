package com.coin.demo.domain;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "portfolios")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "portfolio_balances", joinColumns = @JoinColumn(name = "portfolio_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "amount", precision = 19, scale = 6)
    private Map<AssetType, BigDecimal> assetBalances;
}
