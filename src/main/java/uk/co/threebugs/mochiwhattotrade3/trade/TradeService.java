package uk.co.threebugs.mochiwhattotrade3.trade;

import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.co.threebugs.mochiwhattotrade3.SlackClient;
import uk.co.threebugs.mochiwhattotrade3.Type;
import uk.co.threebugs.mochiwhattotrade3.account.Account;
import uk.co.threebugs.mochiwhattotrade3.accountsetupgroups.AccountSetupGroups;
import uk.co.threebugs.mochiwhattotrade3.accountsetupgroups.AccountSetupGroupsRepository;
import uk.co.threebugs.mochiwhattotrade3.metatrader.Client;
import uk.co.threebugs.mochiwhattotrade3.metatrader.Order;
import uk.co.threebugs.mochiwhattotrade3.metatrader.TradeInfo;
import uk.co.threebugs.mochiwhattotrade3.setup.SetupRepository;
import uk.co.threebugs.mochiwhattotrade3.utils.Constants;
import uk.co.threebugs.mochiwhattotrade3.utils.TimeHelper;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static uk.co.threebugs.mochiwhattotrade3.setup.SetupFileRepository.getNextEventTime;

@AllArgsConstructor
@Service
public class TradeService {

    private static final Logger logger = LogManager.getLogger(TradeService.class);

    private final TradeRepository tradeRepository;
    private final SetupRepository setupRepository;
    private final AccountSetupGroupsRepository accountSetupGroupsRepository;
    private final TradeMapper tradeMapper;
    private final TimeHelper timeHelper;
    private final SlackClient slackClient;

    public static BigDecimal addTicks(BigDecimal initialPrice, int ticksToAdd, BigDecimal tickSize) {
        var ticks = tickSize.multiply(BigDecimal.valueOf(ticksToAdd));
        return initialPrice.add(ticks);
    }

    public Optional<TradeDto> findById(int id) {
        return tradeRepository.findById(id)
                              .map(tradeMapper::toDto);
    }

    public TradeDto save(TradeDto tradeDto) {

        var setup = this.setupRepository.findById(tradeDto.getSetup()
                                                          .getId())
                                        .orElseThrow(() -> new RuntimeException("Setup not found"));

        var record = tradeMapper.toEntity(tradeDto, setup);
        record = tradeRepository.save(record);
        return tradeMapper.toDto(record);
    }

    public void deleteById(int id) {
        tradeRepository.deleteById(id);
    }

    public List<TradeDto> findAll() {


        return this.tradeRepository.findAll()
                                   .stream()
                                   .map(tradeMapper::toDto)
                                   .toList();
    }

    public List<TradeDto> findTrades(TradeDto exampleRecord, Sort sort) {

        var example = Example.of(tradeMapper.toEntity(exampleRecord));

        return tradeRepository.findAll(example, sort)
                              .stream()
                              .map(tradeMapper::toDto)
                              .toList();
    }

    public void createTradesToPlaceFromEnabledSetups(String symbol, Account account) {

        var accountSetupGroups = accountSetupGroupsRepository.findByAccount(account);

        accountSetupGroups.forEach(accountSetupGroup -> createAndPlaceTradesForEnabledSetupsInSetupGroup(symbol, account, accountSetupGroup));

    }

    private void createAndPlaceTradesForEnabledSetupsInSetupGroup(String symbol, Account account, AccountSetupGroups accountSetupGroup) {
        var setups = setupRepository.findEnabledSetups(symbol, accountSetupGroup.getSetupGroups());
        setups.forEach(setup -> {
            var placedDateTime = getNextEventTime(setup.getDayOfWeek(), setup.getHourOfDay());
            tradeRepository.findBySetupAndPlacedDateTimeAndAccount(setup, placedDateTime, account)
                           .ifPresentOrElse(trade -> {
                               // logger.info("Trade already exists: " + trade);
                           }, () -> {
                               var trade = tradeMapper.toEntity(setup, placedDateTime, account);
                               trade.setType(Type.WAITING_TO_PLACED);
                               tradeRepository.save(trade);
                               slackClient.sendSlackNotification(trade.getNewTradeMessage());
                           });

        });
    }

    public void placeTrades(Client dwx, String symbol, BigDecimal bid, BigDecimal ask, Account account) {


        tradeRepository.findByTypeAndSetup_SymbolAndAccount(Type.WAITING_TO_PLACED, symbol, account)
                       .forEach(trade -> {
                           var now = ZonedDateTime.now(UTC);
                           var placedDateTime = trade.getPlacedDateTime();
                           if (now.isAfter(placedDateTime)) {
                               tradeRepository.save(placeTrade(dwx, bid, ask, trade));
                           }
                       });


    }

    public Trade placeTrade(Client dwx, BigDecimal bid, BigDecimal ask, Trade trade) {
        var fillPrice = trade.getSetup()
                             .isLong() ? ask : bid;

        var orderType = trade.getSetup()
                             .isLong() ? "buylimit" : "selllimit";

        var tickSize = new BigDecimal("0.00001");
        if (trade.getSetup()
                 .getSymbol()
                 .equalsIgnoreCase(Constants.USDJPY)) {
            tickSize = new BigDecimal("0.01");
        }

        var price = addTicks(fillPrice, trade.getSetup()
                                             .getTickOffset(), tickSize);
        var stopLoss = addTicks(fillPrice, trade.getSetup()
                                                .getStop(), tickSize);
        var takeProfit = addTicks(fillPrice, trade.getSetup()
                                                  .getLimit(), tickSize);


        //        dwx.openOrder(Order.builder()
//                           .symbol("EURUSD")
//                           .orderType("buylimit")
//                           .lots(0.01)
//                           .price(new BigDecimal("1.02831"))
//                           .stopLoss(new BigDecimal("1.00831"))
//                           .takeProfit(new BigDecimal("1.0931"))
//                           .magic(100)
//                           .comment("test")
//                           .expiration(TradeService.addSecondsToCurrentTime(8))
//                           .build());


        var magic = trade.getId();
        dwx.openOrder(Order.builder()
                           .symbol(trade.getSetup()
                                        .getSymbol())
                           .orderType(orderType)
                           .lots(0.01)
                           .price(price)
                           .stopLoss(stopLoss)
                           .takeProfit(takeProfit)
                           .magic(magic)
                           .comment(trade.getSetup()
                                         .concatenateFields())
                           .expiration(timeHelper.addSecondsToCurrentTime(trade.getSetup()
                                                                               .getOutOfTime()))
                           .build());

        trade.setType(Type.PLACED);

        slackClient.sendSlackNotification("Order placed: " + trade.getSetup()
                                                                  .concatenateFields());
        return trade;
    }

    public void closeTradesAtTime(Client dwx, String symbol, Account account) {


        tradeRepository.findByTypeAndSetup_SymbolAndAccount(Type.FILLED, symbol, account)
                       .stream()
                       .filter(record -> record.getPlacedDateTime()
                                               .plusHours(record.getSetup()
                                                                .getTradeDuration())
                                               .isBefore(ZonedDateTime.now(UTC)))
                       .forEach(trade -> {
                           dwx.closeOrdersByMagic(trade.getId());
                           trade.setType(Type.CLOSED_BY_TIME);
                           trade.setClosedDateTime(ZonedDateTime.now());
                           tradeRepository.save(trade);

                           slackClient.sendSlackNotification("Order closed: " + trade.getSetup()
                                                                                     .getRank() + " " + trade.getSetup()
                                                                                                             .getSymbol() + " " + (trade.getSetup()
                                                                                                                                        .isLong() ? "LONG" : "SHORT") + " " + trade.getProfit());

                       });


    }

    public void fillTrade(TradeInfo tradeInfo, Integer metatraderId) {

        tradeRepository.findById(tradeInfo.getMagic())
                       .ifPresentOrElse(trade -> {
                                   trade.setPlacedPrice(tradeInfo.getOpenPrice());
                                   trade.setPlacedDateTime(ZonedDateTime.of(tradeInfo.getOpenTime(), ZoneId.of("Europe/Zurich")));
                                   trade.setType(Type.PLACED_IN_MT);
                                   trade.setMetatraderId(metatraderId);

                                   tradeRepository.save(trade);

                           slackClient.sendSlackNotification("Order placed in MT: " + trade);
                               }, () -> logger.info("Trade not found: " + tradeInfo)

                       );
    }

    public void closeTrade(TradeInfo tradeInfo) {
        tradeRepository.findById(tradeInfo.getMagic())
                       .ifPresentOrElse(trade -> closeTrade(tradeInfo, trade), () -> logger.info("Trade not found: " + tradeInfo));

    }

    private void closeTrade(TradeInfo tradeInfo, Trade trade) {
        trade.setType(Type.CLOSED_BY_USER);
        trade.setClosedPrice(tradeInfo.getTakeProfit());
        trade.setClosedDateTime(ZonedDateTime.now());
        trade.setProfit(tradeInfo.getProfitAndLoss());

        tradeRepository.save(trade);

        slackClient.sendSlackNotification("Order closed: %d %s %s %s".formatted(trade.getSetup()
                                                                                     .getRank(), trade.getSetup()
                                                                                                      .getSymbol(), trade.getSetup()
                                                                                                                         .isLong() ? "LONG" : "SHORT", trade.getProfit()));
    }
}
