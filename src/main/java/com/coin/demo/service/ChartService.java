package com.coin.demo.service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.springframework.stereotype.Service;

import com.coin.demo.domain.AssetType;

@Service
public class ChartService {

    public File generatePortfolioPieChart(Map<AssetType, BigDecimal> assetTotals, String title) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        assetTotals.forEach((assetType, total) -> {
            double v = total == null ? 0d : total.doubleValue();
            dataset.setValue(assetType.name(), v);
        });

        JFreeChart chart = ChartFactory.createPieChart(title, dataset);
        chart.setBackgroundPaint(Color.WHITE);
        // show percentage and absolute
        PiePlot<?> plot = (PiePlot<?>) chart.getPlot();
        plot.setSimpleLabels(true);
        NumberFormat valueFmt = new DecimalFormat("#,##0");
        NumberFormat pctFmt = new DecimalFormat("0.0%");
        plot.setLabelGenerator(new PieSectionLabelGenerator() {
            @Override
            public String generateSectionLabel(PieDataset dataset, Comparable key) {
                try {
                    Number valueNum = dataset.getValue(key);
                    double value = valueNum == null ? 0d : valueNum.doubleValue();
                    double total = 0d;
                    for (Object k : dataset.getKeys()) {
                        Number v = dataset.getValue((Comparable) k);
                        if (v != null)
                            total += v.doubleValue();
                    }
                    double pct = total == 0 ? 0d : (value / total);
                    return key + " (" + valueFmt.format(value) + ", " + pctFmt.format(pct) + ")";
                } catch (Exception e) {
                    return String.valueOf(key);
                }
            }

            @Override
            public java.text.AttributedString generateAttributedSectionLabel(PieDataset dataset, Comparable key) {
                return null;
            }
        });

        BufferedImage image = chart.createBufferedImage(800, 600);
        try {
            File tempFile = Files.createTempFile("portfolio-", ".png").toFile();
            ImageIO.write(image, "png", tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate chart image", e);
        }
    }
}
