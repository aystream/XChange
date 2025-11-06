package info.bitrich.xchangestream.gateio.dto.response.ticker;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class FuturesTickerPayload {

  @JsonProperty("contract")
  private String contract;

  @JsonProperty("last")
  private BigDecimal lastPrice;

  @JsonProperty("funding_rate")
  private BigDecimal fundingRate;

  @JsonProperty("funding_rate_indicative")
  private BigDecimal fundingRateIndicative;

  @JsonProperty("mark_price")
  private BigDecimal markPrice;

  @JsonProperty("index_price")
  private BigDecimal indexPrice;

  @JsonProperty("change_percentage")
  private BigDecimal changePercent24h;

  @JsonProperty("total_size")
  private BigDecimal totalSize;

  @JsonProperty("volume_24h")
  private BigDecimal volume24h;

  @JsonProperty("volume_24h_btc")
  private BigDecimal volume24hBtc;

  @JsonProperty("volume_24h_usd")
  private BigDecimal volume24hUsd;

  @JsonProperty("volume_24h_base")
  private BigDecimal volume24hBase;

  @JsonProperty("volume_24h_quote")
  private BigDecimal volume24hQuote;

  @JsonProperty("volume_24h_settle")
  private BigDecimal volume24hSettle;

  @JsonProperty("quanto_base_rate")
  private BigDecimal quantoBaseRate;

  @JsonProperty("high_24h")
  private BigDecimal high24h;

  @JsonProperty("low_24h")
  private BigDecimal low24h;
}
