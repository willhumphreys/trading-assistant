package uk.co.threebugs.mochiwhattotrade3.metatrader;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import uk.co.threebugs.mochiwhattotrade3.SlackClient;
import uk.co.threebugs.mochiwhattotrade3.account.AccountDto;
import uk.co.threebugs.mochiwhattotrade3.account.AccountMapper;
import uk.co.threebugs.mochiwhattotrade3.trade.TradeService;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;


/*Custom event handler implementing the EventHandler interface.
 */
@Component
@RequiredArgsConstructor
public class TradeEventHandler {

    private static final Logger logger = LogManager.getLogger(TradeEventHandler.class);

    private final AtomicBoolean executed = new AtomicBoolean(false);

    private final TradeService tradeService;

    private final SlackClient slackClient;

    private final AccountMapper accountMapper;


//    public void start(Client dwx, String[] symbols) {
//
//        // account information is stored in dwx.accountInfo.
//        logger.info("\nAccount info:\n" + dwx.accountInfo + "\n");
//
//        // subscribe to tick data:
//        dwx.subscribeSymbols(symbols);
//
//
//        // subscribe to bar data:
////        var symbolsBarData = new String[][]{{"EURUSD", "M1"}, {"AUDCAD", "M5"}, {"GBPCAD", "M15"}};
////        dwx.subscribeSymbolsBarData(symbolsBarData);
//
//        // request historic data:
////        var end = System.currentTimeMillis() / 1000;
////        var start = end - 10 * 24 * 60 * 60;  // last 10 days
////        dwx.getHistoricData("AUDCAD", "D1", start, end);
//
//        // dwx.closeOrdersByMagic(77);
//        // sleep(2000);
//    }

    // use synchronized so that price updates and execution updates are not processed one after the other.
    public synchronized void onTick(Client dwx, String symbol, BigDecimal bid, BigDecimal ask, AccountDto accountDto) {

//        if (executed.compareAndSet(false, true)) {
//                    dwx.openOrder(Order.builder()
//                           .symbol("EURUSD")
//                           .orderType("buylimit")
//                           .lots(0.01)
//                           .price(new BigDecimal("1.05750"))
//                           .stopLoss(new BigDecimal("1.05500"))
//                           .takeProfit(new BigDecimal("1.05800"))
//                           .magic(100)
//                           .comment("test")
//                           .expiration(TradeService.addSecondsToCurrentTime(8))
//                           .build());
//
//        }

        var account = accountMapper.toEntity(accountDto);
        tradeService.createTradesToPlaceFromEnabledSetups(symbol, account);

        tradeService.placeTrades(dwx, symbol, bid, ask, account);

        tradeService.closeTradesAtTime(dwx, symbol, account);

    }


    public synchronized void onBarData(Client dwx, String symbol, String timeFrame, String time, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, int tickVolume) {

        //logger.info("onBarData: " + symbol + ", " + timeFrame + ", " + time + ", " + open + ", " + high + ", " + low + ", " + close + ", " + tickVolume);
    }


    public synchronized void onMessage(Client dwx, JSONObject message) {

        if (message.get("type")
                   .equals("ERROR"))
            logger.info(message.get("type") + " | " + message.get("error_type") + " | " + message.get("description"));
        else if (message.get("type")
                        .equals("INFO")) logger.info(message.get("type") + " | " + message.get("message"));

        slackClient.sendSlackNotification("message: " + message);
    }

    public synchronized void onHistoricTrades(Client dwx) {

        logger.info("onHistoricTrades: " + dwx.historicTrades);
    }

    public synchronized void onHistoricData(Client dwx, String symbol, String timeFrame, JSONObject data) {

        // you can also access historic data via: dwx.historicData
        logger.info("onHistoricData: " + symbol + ", " + timeFrame + ", " + data);
    }

    public void onNewOrder(TradeInfo tradeInfo, Integer metaTraderId) {

        tradeService.fillTrade(tradeInfo, metaTraderId);


    }

    public void onClosedOrder(TradeInfo tradeInfo) {

        tradeService.closeTrade(tradeInfo);


    }
}
