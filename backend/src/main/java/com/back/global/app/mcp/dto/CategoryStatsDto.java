package com.back.global.app.mcp.dto;

public record CategoryStatsDto(
        String categoryName,
        long tradeCount,
        int totalFee
) {
}
