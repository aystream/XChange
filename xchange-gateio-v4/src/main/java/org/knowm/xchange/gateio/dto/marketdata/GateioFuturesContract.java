package org.knowm.xchange.gateio.dto.marketdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class GateioFuturesContract {

  @JsonProperty("name")
  private String name;

  @JsonProperty("type")
  private String type;

  @JsonProperty("quanto_multiplier")
  private BigDecimal quantoMultiplier;

  @JsonProperty("ref_discount_rate")
  private BigDecimal refDiscountRate;

  @JsonProperty("order_price_deviate")
  private BigDecimal orderPriceDeviate;

  @JsonProperty("maintenance_rate")
  private BigDecimal maintenanceRate;

  @JsonProperty("mark_type")
  private String markType;

  @JsonProperty("last_price")
  private BigDecimal lastPrice;

  @JsonProperty("mark_price")
  private BigDecimal markPrice;

  @JsonProperty("index_price")
  private BigDecimal indexPrice;

  @JsonProperty("funding_rate_indicative")
  private BigDecimal fundingRateIndicative;

  @JsonProperty("mark_price_round")
  private BigDecimal markPriceRound;

  @JsonProperty("funding_offset")
  private Integer fundingOffset;

  @JsonProperty("in_delisting")
  private Boolean inDelisting;

  @JsonProperty("risk_limit_base")
  private BigDecimal riskLimitBase;

  @JsonProperty("interest_rate")
  private BigDecimal interestRate;

  @JsonProperty("order_price_round")
  private BigDecimal orderPriceRound;

  @JsonProperty("order_size_min")
  private Long orderSizeMin;

  @JsonProperty("ref_rebate_rate")
  private BigDecimal refRebateRate;

  @JsonProperty("funding_interval")
  private Integer fundingInterval;

  @JsonProperty("risk_limit_step")
  private BigDecimal riskLimitStep;

  @JsonProperty("leverage_min")
  private BigDecimal leverageMin;

  @JsonProperty("leverage_max")
  private BigDecimal leverageMax;

  @JsonProperty("risk_limit_max")
  private BigDecimal riskLimitMax;

  @JsonProperty("maker_fee_rate")
  private BigDecimal makerFeeRate;

  @JsonProperty("taker_fee_rate")
  private BigDecimal takerFeeRate;

  @JsonProperty("funding_rate")
  private BigDecimal fundingRate;

  @JsonProperty("order_size_max")
  private Long orderSizeMax;

  @JsonProperty("funding_next_apply")
  private Long fundingNextApply;

  @JsonProperty("short_users")
  private Integer shortUsers;

  @JsonProperty("config_change_time")
  private Long configChangeTime;

  @JsonProperty("trade_size")
  private Long tradeSize;

  @JsonProperty("position_size")
  private Long positionSize;

  @JsonProperty("long_users")
  private Integer longUsers;

  @JsonProperty("funding_impact_value")
  private BigDecimal fundingImpactValue;

  @JsonProperty("orders_limit")
  private Integer ordersLimit;

  @JsonProperty("trade_id")
  private Long tradeId;

  @JsonProperty("orderbook_id")
  private Long orderbookId;

  @JsonProperty("enable_bonus")
  private Boolean enableBonus;

  @JsonProperty("enable_credit")
  private Boolean enableCredit;

  @JsonProperty("create_time")
  private Long createTime;

  @JsonProperty("funding_cap_ratio")
  private BigDecimal fundingCapRatio;
}
