package org.knowm.xchange.mexc.dto.marketdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class MEXCFundingRate {

  @JsonProperty("symbol")
  private String symbol;

  @JsonProperty("fundingRate")
  private BigDecimal fundingRate;

  @JsonProperty("nextFundingTime")
  private Long nextFundingTime;

  @JsonProperty("time")
  private Long timestamp;
}
