package org.knowm.xchange.bitget.dto.marketdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class BitgetFuturesTickerDto {

  @JsonProperty("instId")
  private String symbol;

  @JsonProperty("lastPr")
  private BigDecimal lastPrice;

  @JsonProperty("high24h")
  private BigDecimal high24h;

  @JsonProperty("low24h")
  private BigDecimal low24h;

  @JsonProperty("open24h")
  private BigDecimal open24h;

  @JsonProperty("baseVolume")
  private BigDecimal baseVolume24h;

  @JsonProperty("quoteVolume")
  private BigDecimal quoteVolume24h;

  @JsonProperty("fundingRate")
  private BigDecimal fundingRate;

  @JsonProperty("nextFundingTime")
  private String nextFundingTime;

  @JsonProperty("markPrice")
  private BigDecimal markPrice;

  @JsonProperty("indexPrice")
  private BigDecimal indexPrice;

  @JsonProperty("bidPr")
  private BigDecimal bestBidPrice;

  @JsonProperty("bidSz")
  private BigDecimal bestBidSize;

  @JsonProperty("askPr")
  private BigDecimal bestAskPrice;

  @JsonProperty("askSz")
  private BigDecimal bestAskSize;

  @JsonProperty("ts")
  private Instant timestamp;
}
