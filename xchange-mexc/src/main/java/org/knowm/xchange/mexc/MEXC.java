package org.knowm.xchange.mexc;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import org.knowm.xchange.mexc.dto.marketdata.MEXCFundingRate;
import org.knowm.xchange.mexc.service.MEXCException;

@Path("/api/v3")
@Produces(MediaType.APPLICATION_JSON)
public interface MEXC {

  /**
   * Get funding rate for futures contracts
   *
   * @param symbol Contract symbol (e.g., "BTCUSDT"), optional - if null returns all
   * @return List of funding rates
   * @throws IOException
   * @throws MEXCException
   */
  @GET
  @Path("/premiumIndex")
  List<MEXCFundingRate> getFundingRate(@QueryParam("symbol") String symbol)
      throws IOException, MEXCException;
}
