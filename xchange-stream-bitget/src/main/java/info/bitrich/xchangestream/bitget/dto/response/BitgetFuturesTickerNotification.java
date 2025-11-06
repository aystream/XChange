package info.bitrich.xchangestream.bitget.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import info.bitrich.xchangestream.bitget.dto.common.Action;
import info.bitrich.xchangestream.bitget.dto.common.BitgetChannel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@Jacksonized
public class BitgetFuturesTickerNotification extends BitgetWsNotification<BitgetFuturesTickerNotification.BitgetFuturesTicker> {

  @Data
  public static class BitgetFuturesTicker {
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
    private String timestamp;
  }
}
