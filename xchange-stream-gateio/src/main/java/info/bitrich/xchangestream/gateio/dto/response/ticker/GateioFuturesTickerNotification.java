package info.bitrich.xchangestream.gateio.dto.response.ticker;

import com.fasterxml.jackson.annotation.JsonProperty;
import info.bitrich.xchangestream.gateio.config.Config;
import info.bitrich.xchangestream.gateio.dto.response.GateioWsNotification;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Jacksonized
public class GateioFuturesTickerNotification extends GateioWsNotification {

  @JsonProperty("result")
  private List<FuturesTickerPayload> result;

  @Override
  public String getUniqueChannelName() {
    // Include contract name from the first result element to match subscription
    String suffix = "";
    if (result != null && !result.isEmpty() && result.get(0).getContract() != null) {
      suffix = Config.CHANNEL_NAME_DELIMITER + result.get(0).getContract();
    }
    return super.getUniqueChannelName() + suffix;
  }
}
