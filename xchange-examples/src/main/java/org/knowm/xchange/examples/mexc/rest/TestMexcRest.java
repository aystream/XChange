import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.derivative.FuturesContract;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.mexc.MEXCExchange;

public class TestMexcRest {
  public static void main(String[] args) {
    try {
      Exchange exchange = ExchangeFactory.INSTANCE.createExchange(MEXCExchange.class);
      FuturesContract btcPerp = new FuturesContract(CurrencyPair.BTC_USDT, "PERP");
      
      System.out.println("Fetching BTC/USDT funding rate from MEXC REST API...");
      FundingRate rate = exchange.getMarketDataService().getFundingRate(btcPerp);
      
      System.out.println("Success!");
      System.out.println("Instrument: " + rate.getInstrument());
      System.out.println("Funding Rate (1h): " + rate.getFundingRate1h());
      System.out.println("Funding Rate (8h): " + rate.getFundingRate8h());
      System.out.println("Next Funding Time: " + rate.getFundingRateDate());
    } catch (Exception e) {
      System.out.println("Error: " + e.getClass().getName() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }
}
