package org.knowm.xchange.gateio.dto.marketdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class GateioFundingRate {

  @JsonProperty("t")
  private Long timestamp;

  @JsonProperty("r")
  private BigDecimal rate;

  @JsonProperty("contract")
  private String contract;
}
