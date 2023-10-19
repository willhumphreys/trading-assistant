package uk.co.threebugs.mochiwhattotrade3.metatrader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.co.threebugs.mochiwhattotrade3.MetaTraderDir;
import uk.co.threebugs.mochiwhattotrade3.account.AccountDto;
import uk.co.threebugs.mochiwhattotrade3.account.AccountService;
import uk.co.threebugs.mochiwhattotrade3.accountsetupgroups.AccountSetupGroupsDto;
import uk.co.threebugs.mochiwhattotrade3.accountsetupgroups.AccountSetupGroupsService;
import uk.co.threebugs.mochiwhattotrade3.setup.Setup;
import uk.co.threebugs.mochiwhattotrade3.setup.SetupFileRepository;
import uk.co.threebugs.mochiwhattotrade3.setup.SetupRepository;
import uk.co.threebugs.mochiwhattotrade3.setupgroup.SetupGroup;
import uk.co.threebugs.mochiwhattotrade3.setupgroup.SetupGroupService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class Client {

    private static final Logger logger = LogManager.getLogger(Client.class);
    private final int sleepDelay;
    private final int maxRetryCommandSeconds;
    private final SetupGroupService setupGroupService;
    private final AccountService accountService;
    private final String metaTraderName;
    private final AccountSetupGroupsService accountSetupGroupsService;
    private final ObjectMapper objectMapper;
    private final TradeEventHandler eventHandler;
    private final SetupFileRepository setupFileRepository;
    private final SetupRepository setupRepository;
    public Map<Integer, TradeInfo> openOrders;
    public JSONObject accountInfo = new JSONObject();
    public JSONObject marketData = new JSONObject();
    public JSONObject barData = new JSONObject();
    public JSONObject historicData = new JSONObject();
    public JSONObject historicTrades = new JSONObject();
    public boolean ACTIVE = true;
    private Path pathOrders;
    private Path pathMessages;
    private Path pathMarketData;
    private Path pathBarData;
    private Path pathHistoricData;
    private Path pathHistoricTrades;
    private Path pathOrdersStored;
    private Path pathMessagesStored;
    private Path pathCommandsPrefix;
    private int commandID = 0;
    private long lastMessagesMillis = 0;
    private String lastOpenOrdersStr = "";
    private String lastMessagesStr = "";
    private String lastMarketDataStr = "";
    private String lastBarDataStr = "";
    private String lastHistoricDataStr = "";
    private String lastHistoricTradesStr = "";
    private JSONObject lastBarData = new JSONObject();
    private JSONObject lastMarketData = new JSONObject();
    private boolean START = false;
    private AccountDto account;

    public Client(TradeEventHandler eventHandler, AccountService accountService, @Value("${metatrader-name}") String metaTraderName, @Value("${sleep-delay}") int sleepDelay, @Value("${max-retry-command-seconds}") int maxRetryCommandSeconds, SetupGroupService setupGroupService, SetupRepository setupRepository, SetupFileRepository setupFileRepository, AccountSetupGroupsService accountSetupGroupsService, ObjectMapper objectMapper) {

        this.eventHandler = eventHandler;
        this.accountService = accountService;
        this.metaTraderName = metaTraderName;
        this.sleepDelay = sleepDelay;
        this.maxRetryCommandSeconds = maxRetryCommandSeconds;
        this.setupGroupService = setupGroupService;
        this.setupRepository = setupRepository;
        this.setupFileRepository = setupFileRepository;
        this.accountSetupGroupsService = accountSetupGroupsService;
        this.objectMapper = objectMapper;

        this.openOrders = Map.of();
    }

    private List<AccountDto> loadMetaTraderInstalls(Path path) {

        List<MetaTraderDir> metaTraderDirs;
        try {
            metaTraderDirs = objectMapper.readValue(path.toFile(), new TypeReference<>() {
            });
        } catch (Exception e) {
            logger.error("Error loading MetaTrader directories from JSON file", e);
            throw new RuntimeException("Error loading MetaTrader directories from JSON file", e);
        }

        return metaTraderDirs.stream()
                             .map(metaTraderDir -> accountService.findByName(metaTraderDir.getName())
                                                                 .orElseGet(() -> {
                                                                     logger.info("MetaTraderDir {} not found in database", metaTraderDir.getName());
                                                                     return accountService.save(AccountDto.builder()
                                                                                                          .metatraderAdvisorPath(Paths.get(metaTraderDir.getDirPath()))
                                                                                                          .name(metaTraderDir.getName())
                                                                                                          .build());
                                                                 }))
                             .toList();


    }

    @PostConstruct
    public void init() throws JsonProcessingException {
        objectMapper.registerModule(new JavaTimeModule());


        var accountDtos = loadMetaTraderInstalls(Paths.get("accounts", "metatrader_dirs.json"));


        var setupGroupsPath = Paths.get("accounts", "setup-groups");

        var symbols = new String[]{"EURUSD", "GBPUSD", "USDCAD", "NZDUSD", "AUDUSD", "USDJPY", "USDCHF"};


        try (var paths = Files.list(setupGroupsPath)) {
            paths.forEach(setupsPath -> {

                var setupGroups = setupGroupService.loadSetupsFromFile(setupsPath);

                Arrays.stream(symbols)
                      .forEach(symbol -> loadDataFromCsv(symbol, setupGroups));
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var accounts = accountSetupGroupsService.loadAccountSetupGroups(Paths.get("accounts", "account-setup-groups.json"));

        AccountSetupGroupsDto accountSetupGroupsDto = accountSetupGroupsService.findByName(metaTraderName)
                                                                               .orElseThrow(() -> new RuntimeException("Failed to find account setup groups: " + metaTraderName));


        account = accountSetupGroupsDto.getAccount();
        var metaTraderDirPath = account.getMetatraderAdvisorPath();


        var f = metaTraderDirPath.toFile();
        if (!f.exists()) {
            logger.info("ERROR: MetaTraderDirPath does not exist!");
            System.exit(1);
        }

        var dwxPath = metaTraderDirPath.resolve("DWX");
        this.pathOrders = dwxPath.resolve("DWX_Orders.txt");
        this.pathMessages = dwxPath.resolve("DWX_Messages.txt");
        this.pathMarketData = dwxPath.resolve("DWX_Market_Data.txt");
        this.pathBarData = dwxPath.resolve("DWX_Bar_Data.txt");
        this.pathHistoricData = dwxPath.resolve("DWX_Historic_Data.txt");
        this.pathHistoricTrades = dwxPath.resolve("DWX_Historic_Trades.txt");
        this.pathOrdersStored = dwxPath.resolve("DWX_Orders_Stored.txt");
        this.pathMessagesStored = dwxPath.resolve("DWX_Messages_Stored.txt");
        this.pathCommandsPrefix = dwxPath.resolve("DWX_Commands_");

        loadMessages();

        var openOrdersThread = new Thread(() -> {
            try {
                checkOpenOrders();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        openOrdersThread.start();

        var messageThread = new Thread(this::checkMessages);
        messageThread.start();

        var marketDataThread = new Thread(this::checkMarketData);
        marketDataThread.start();

        var barDataThread = new Thread(this::checkBarData);
        barDataThread.start();

        var historicDataThread = new Thread(this::checkHistoricData);
        historicDataThread.start();

        resetCommandIDs();

        loadOrders();

        logger.info("\nAccount info:\n" + accountInfo + "\n");

        // subscribe to tick data:
        subscribeSymbols(symbols);

        START = true;

    }

    private List<Setup> loadDataFromCsv(String symbol, List<SetupGroup> setupGroups) {

        return setupGroups.stream()
                          .filter(setupGroup -> setupGroup.getSymbol()
                                                          .equalsIgnoreCase(symbol))
                          //.filter(SetupGroup::getEnabled)
                          .map(setupGroup -> setupFileRepository.readCsv(Path.of(setupGroup.getPath()), setupGroup.getSymbol(), setupGroup))
                          .flatMap(Collection::stream)
                          .map(setup -> {
                              var optionalSetup = setupRepository.findBySymbolAndRankAndSetupGroup(symbol, setup.getRank(), setup.getSetupGroup());
                              return optionalSetup.orElseGet(() -> setupRepository.save(setup));
                          })
                          .collect(Collectors.toList());
    }

    /*Regularly checks the file for open orders and triggers
    the eventHandler.onOrderEvent() function.
    */
    private void checkOpenOrders() throws JsonProcessingException {
        while (ACTIVE) {

            Helpers.sleep(sleepDelay);

            if (!START) continue;

            var newDataStr = Helpers.tryReadFile(pathOrders);

            if (newDataStr.isEmpty() || newDataStr.equals(lastOpenOrdersStr)) continue;

            Map<Integer, TradeInfo> previousDataOrders = Map.of();
            if(lastOpenOrdersStr != null && !lastOpenOrdersStr.isEmpty()) {
                var previousDataJSON = new JSONObject(lastOpenOrdersStr);
                previousDataOrders = objectMapper.readValue(previousDataJSON.getJSONObject("orders")
                                                                                .toString(), new TypeReference<>() {
                });
            }


            lastOpenOrdersStr = newDataStr;

            var newDataJSON = new JSONObject(newDataStr);


            var dataOrders = objectMapper.readValue(newDataJSON.getJSONObject("orders")
                                                               .toString(), new TypeReference<Map<Integer, TradeInfo>>() {
            });



            for (var ticket : openOrders.keySet()) {
                if (!dataOrders.containsKey(ticket)) {
                    logger.info("Order removed: " + openOrders.get(ticket));
                    var tradeInfo = objectMapper.convertValue(openOrders.get(ticket), TradeInfo.class);
                    eventHandler.onClosedOrder(tradeInfo);
                }
            }

            for (var ticket : dataOrders.keySet()) {
                if (!openOrders.containsKey(ticket)) {

                    var tradeInfo = dataOrders.get(ticket);

                    logger.info("New order: " + tradeInfo);
                    eventHandler.onNewOrder(tradeInfo, ticket);
                }
            }

            for (var entry : dataOrders.entrySet()) {
                var key = entry.getKey();
                var currentValue = entry.getValue();

                // Check if the key exists in previousDataOrders
                if (previousDataOrders.containsKey(key)) {
                    var previousValue = previousDataOrders.get(key);

                    // Compare the TradeInfo objects
                    compareTradeInfo(key, currentValue, previousValue);
                } else {
                    // Log new orders that didn't exist in previousDataOrders
                    logger.info("New order: " + key + ", Value: " + currentValue);
                }
            }

            openOrders = dataOrders;
            accountInfo = (JSONObject) newDataJSON.get("account_info");

            //if (loadOrdersFromFile) Helpers.tryWriteToFile(pathOrdersStored, data.toString());

            //if (newEvent) eventHandler.onOrderEvent(this.openOrders);
        }
    }

    private void compareTradeInfo(Integer ticket, TradeInfo currentValue, TradeInfo previousValue) {
        // Compare fields in TradeInfo and log differences
        var log = false;
        if (!currentValue.equals(previousValue)) {
            var changes = new StringBuilder("Changes for Order " + ticket + ": ");

            if (currentValue.getMagic() != previousValue.getMagic()) {
                changes.append("Magic: ")
                       .append(previousValue.getMagic())
                       .append(" -> ")
                       .append(currentValue.getMagic())
                       .append(", ");
                log = true;
            }
            if (!currentValue.getLots()
                             .equals(previousValue.getLots())) {
                changes.append("Lots: ")
                       .append(previousValue.getLots())
                       .append(" -> ")
                       .append(currentValue.getLots())
                       .append(", ");
                log = true;
            }
            if (!currentValue.getSymbol()
                             .equals(previousValue.getSymbol())) {
                changes.append("Symbol: ")
                       .append(previousValue.getSymbol())
                       .append(" -> ")
                       .append(currentValue.getSymbol())
                       .append(", ");
                log = true;
            }
            if (!currentValue.getSwap()
                             .equals(previousValue.getSwap())) {
                changes.append("Swap: ")
                       .append(previousValue.getSwap())
                       .append(" -> ")
                       .append(currentValue.getSwap())
                       .append(", ");
                log = true;
            }
            if (!currentValue.getOpenTime()
                             .equals(previousValue.getOpenTime())) {
                changes.append("Open Time: ")
                       .append(previousValue.getOpenTime())
                       .append(" -> ")
                       .append(currentValue.getOpenTime())
                       .append(", ");
                log = true;
            }
            if (!currentValue.getStopLoss()
                             .equals(previousValue.getStopLoss())) {
                changes.append("Stop Loss: ")
                       .append(previousValue.getStopLoss())
                       .append(" -> ")
                       .append(currentValue.getStopLoss())
                       .append(", ");
                log = true;
            }
            if (!currentValue.getComment()
                             .equals(previousValue.getComment())) {
                changes.append("Comment: ")
                       .append(previousValue.getComment())
                       .append(" -> ")
                       .append(currentValue.getComment())
                       .append(", ");
                log = true;
            }
            if (!currentValue.getType()
                             .equals(previousValue.getType())) {
                changes.append("Type: ")
                       .append(previousValue.getType())
                       .append(" -> ")
                       .append(currentValue.getType())
                       .append(", ");
                log = true;
            }
            if (!currentValue.getOpenPrice()
                             .equals(previousValue.getOpenPrice())) {
                changes.append("Open Price: ")
                       .append(previousValue.getOpenPrice())
                       .append(" -> ")
                       .append(currentValue.getOpenPrice())
                       .append(", ");
                log = true;
            }
            if (!currentValue.getTakeProfit()
                             .equals(previousValue.getTakeProfit())) {
                changes.append("Take Profit: ")
                       .append(previousValue.getTakeProfit())
                       .append(" -> ")
                       .append(currentValue.getTakeProfit())
                       .append(", ");
                log = true;
            }
//            if (!currentValue.getProfitAndLoss()
//                             .equals(previousValue.getProfitAndLoss())) {
//                changes.append("Profit and Loss: ")
//                       .append(previousValue.getProfitAndLoss())
//                       .append(" -> ")
//                       .append(currentValue.getProfitAndLoss())
//
//            }
            if (!Objects.equals(currentValue.getMapType(), previousValue.getMapType())) {
                changes.append("Map Type: ")
                       .append(previousValue.getMapType())
                       .append(" -> ")
                       .append(currentValue.getMapType())
                       .append(", ");
                log = true;
            }
            if (!Objects.equals(currentValue.getEmpty(), previousValue.getEmpty())) {
                changes.append("Empty: ")
                       .append(previousValue.getEmpty())
                       .append(" -> ")
                       .append(currentValue.getEmpty());
            }

            if (log) {

                logger.info(changes);
            }
        }
    }


    /*Regularly checks the file for messages and triggers
    the eventHandler.onMessage() function.
    */
    private void checkMessages() {
        while (ACTIVE) {

            Helpers.sleep(sleepDelay);

            if (!START) continue;

            var text = Helpers.tryReadFile(pathMessages);

            if (text.isEmpty() || text.equals(lastMessagesStr)) continue;

            lastMessagesStr = text;

            JSONObject data;

            try {
                data = new JSONObject(text);
            } catch (Exception e) {
                continue;
            }

            // the objects are not ordered. because of (millis > lastMessagesMillis) it would miss messages if we just looped through them directly.
            var millisList = new ArrayList<String>();
            for (var millisStr : data.keySet()) {
                if (data.get(millisStr) != null) {
                    millisList.add(millisStr);
                }
            }
            Collections.sort(millisList);
            for (var millisStr : millisList) {
                if (data.get(millisStr) != null) {
                    var millis = Long.parseLong(millisStr);
                    if (millis > lastMessagesMillis) {
                        lastMessagesMillis = millis;
                        if (eventHandler != null) eventHandler.onMessage(this, (JSONObject) data.get(millisStr));
                    }
                }
            }
            Helpers.tryWriteToFile(pathMessagesStored, data.toString());
        }
    }

    /*Regularly checks the file for market data and triggers
    the eventHandler.onTick() function.
    */
    private void checkMarketData() {
        while (ACTIVE) {

            Helpers.sleep(sleepDelay);

            if (!START) continue;

            var text = Helpers.tryReadFile(pathMarketData);

            if (text.isEmpty() || text.equals(lastMarketDataStr)) continue;

            lastMarketDataStr = text;

            JSONObject data;

            try {
                data = new JSONObject(text);
            } catch (Exception e) {
                continue;
            }

            marketData = data;

            if (eventHandler != null) {
                for (var symbol : marketData.keySet()) {

                    if (!lastMarketData.has(symbol) || !marketData.get(symbol)
                                                                  .equals(lastMarketData.get(symbol))) {
                        var jo = (JSONObject) marketData.get(symbol);
                        eventHandler.onTick(this, symbol, (BigDecimal) jo.get("bid"), (BigDecimal) jo.get("ask"), account);
                    }
                }
            }
            lastMarketData = data;
        }
    }

    /*Regularly checks the file for bar data and triggers
    the eventHandler.onBarData() function.
    */
    private void checkBarData() {

        while (ACTIVE) {

            Helpers.sleep(sleepDelay);

            if (!START) continue;

            var text = Helpers.tryReadFile(pathBarData);

            if (text.isEmpty() || text.equals(lastBarDataStr)) continue;

            lastBarDataStr = text;

            JSONObject data;

            try {
                data = new JSONObject(text);
            } catch (Exception e) {
                continue;
            }

            barData = data;

            if (eventHandler != null) {
                for (var st : barData.keySet()) {

                    if (!lastBarData.has(st) || !barData.get(st)
                                                        .equals(lastBarData.get(st))) {
                        var stSplit = st.split("_");
                        if (stSplit.length != 2) continue;
                        var jo = (JSONObject) barData.get(st);
                        eventHandler.onBarData(this, stSplit[0], stSplit[1], (String) jo.get("time"), (BigDecimal) jo.get("open"), (BigDecimal) jo.get("high"), (BigDecimal) jo.get("low"), (BigDecimal) jo.get("close"), (int) jo.get("tick_volume"));
                    }
                }
            }
            lastBarData = data;
        }
    }

    /*Regularly checks the file for historic data and triggers
    the eventHandler.onHistoricData() function.
    */
    private void checkHistoricData() {

        while (ACTIVE) {

            Helpers.sleep(sleepDelay);

            if (!START) continue;

            var text = Helpers.tryReadFile(pathHistoricData);

            if (!text.isEmpty() && !text.equals(lastHistoricDataStr)) {

                lastHistoricDataStr = text;

                JSONObject data;

                try {
                    data = new JSONObject(text);
                } catch (Exception e) {
                    data = null;
                }

                if (data != null) {

                    for (var st : data.keySet()) {
                        historicData.put(st, data.get(st));
                    }

                    Helpers.tryDeleteFile(pathHistoricData);

                    if (eventHandler != null) {
                        for (var st : data.keySet()) {
                            var stSplit = st.split("_");
                            if (stSplit.length != 2) continue;
                            eventHandler.onHistoricData(this, stSplit[0], stSplit[1], (JSONObject) data.get(st));
                        }
                    }
                }
            }

            // also check historic trades in the same thread.
            text = Helpers.tryReadFile(pathHistoricTrades);

            if (!text.isEmpty() && !text.equals(lastHistoricTradesStr)) {

                lastHistoricTradesStr = text;

                JSONObject data;

                try {
                    data = new JSONObject(text);
                } catch (Exception e) {
                    data = null;
                }

                if (data != null) {
                    historicTrades = data;

                    if (eventHandler != null) eventHandler.onHistoricTrades(this);
                }
            }
        }
    }

    /*Loads stored orders from file (in case of a restart).
     */
    private void loadOrders() throws JsonProcessingException {

        var text = Helpers.tryReadFile(pathOrdersStored);

        if (text.isEmpty()) return;

        JSONObject data;

        try {
            data = new JSONObject(text);
        } catch (Exception e) {
            return;
        }

        lastOpenOrdersStr = text;
        openOrders = objectMapper.readValue(data.getJSONObject("orders")
                                                .toString(), new TypeReference<>() {
        });
        accountInfo = (JSONObject) data.get("account_info");
    }

    /*Loads stored messages from file (in case of a restart).
     */
    private void loadMessages() {

        var text = Helpers.tryReadFile(pathMessagesStored);

        if (text.isEmpty()) return;

        JSONObject data;

        try {
            data = new JSONObject(text);
        } catch (Exception e) {
            return;
        }

        lastMessagesStr = text;

        // here we don't have to sort because we just need the latest millis value.
        for (var millisStr : data.keySet()) {
            if (data.has(millisStr)) {
                var millis = Long.parseLong(millisStr);
                if (millis > lastMessagesMillis) lastMessagesMillis = millis;
            }
        }
    }

    /*Sends a SUBSCRIBE_SYMBOLS command to subscribe to market (tick) data.

    Args:
        symbols (String[]): List of symbols to subscribe to.

    Returns:
        null

        The data will be stored in marketData.
        On receiving the data the eventHandler.onTick()
        function will be triggered.
    */
    public void subscribeSymbols(String[] symbols) {
        sendCommand("SUBSCRIBE_SYMBOLS", String.join(",", symbols));
    }

    /*Sends a SUBSCRIBE_SYMBOLS_BAR_DATA command to subscribe to bar data.

    Args:
        symbols (String[][]): List of lists containing symbol/time frame
        combinations to subscribe to. For example:
        String[][] symbols = {{"EURUSD", "M1"}, {"USDJPY", "H1"}};

    Returns:
        null

        The data will be stored in barData.
        On receiving the data the eventHandler.onBarData()
        function will be triggered.
    */
    public void subscribeSymbolsBarData(String[][] symbols) {
        StringBuilder content = new StringBuilder();
        for (var i = 0; i < symbols.length; i++) {
            if (i != 0) content.append(",");
            content.append(symbols[i][0])
                   .append(",")
                   .append(symbols[i][1]);
        }
        sendCommand("SUBSCRIBE_SYMBOLS_BAR_DATA", content.toString());
    }

    /*Sends a GET_HISTORIC_DATA command to request historic data.

    Args:
        symbol (String): Symbol to get historic data.
        timeFrame (String): Time frame for the requested data.
        start (long): Start timestamp (seconds since epoch) of the requested data.
        end (long): End timestamp of the requested data.

    Returns:
        null

        The data will be stored in historicData.
        On receiving the data the eventHandler.onHistoricData()
        function will be triggered.
    */
    public void getHistoricData(String symbol, String timeFrame, long start, long end) {
        var content = symbol + "," + timeFrame + "," + start + "," + end;
        sendCommand("GET_HISTORIC_DATA", content);
    }

    /*Sends a GET_HISTORIC_TRADES command to request historic trades.

    Kwargs:
        lookbackDays (int): Days to look back into the trade history.
                            The history must also be visible in MT4.

    Returns:
        None

        The data will be stored in historicTrades.
        On receiving the data the eventHandler.onHistoricTrades()
        function will be triggered.
    */
    public void getHistoricTrades(int lookbackDays) {
        sendCommand("GET_HISTORIC_TRADES", String.valueOf(lookbackDays));
    }

    /*Sends an OPEN_ORDER command to open an order.

    Args:
        symbol (String): Symbol for which an order should be opened.
        order_type (String): Order type. Can be one of:
            'buy', 'sell', 'buylimit', 'selllimit', 'buystop', 'sellstop'
        lots (double): Volume in lots
        price (double): Price of the (pending) order. Can be zero
            for market orders.
        stop_loss (double): SL as absoute price. Can be zero
            if the order should not have an SL.
        take_profit (double): TP as absoute price. Can be zero
            if the order should not have a TP.
        magic (int): Magic number
        comment (String): Order comment
        expriation (long): Expiration time given as timestamp in seconds.
            Can be zero if the order should not have an expiration time.
    */
    public void openOrder(Order order) {

        logger.info("openOrder: " + order.getSymbol() + ", " + order.getOrderType() + ", " + order.getLots() + ", " + order.getPrice() + ", " + order.getStopLoss() + ", " + order.getTakeProfit() + ", " + order.getMagic() + ", " + order.getComment() + ", " + order.getExpiration());

        var content = order.getSymbol() + "," + order.getOrderType() + "," + order.getLots() + "," + order.getPrice() + "," + order.getStopLoss() + "," + order.getTakeProfit() + "," + order.getMagic() + "," + order.getComment() + "," + order.getExpiration();
        sendCommand("OPEN_ORDER", content);

        logger.info("order sent: " + content);
    }

    /*Sends a MODIFY_ORDER command to modify an order.

    Args:
        ticket (int): Ticket of the order that should be modified.
        lots (double): Volume in lots
        price (double): Price of the (pending) order. Non-zero only
            works for pending orders.
        stop_loss (double): New stop loss price.
        take_profit (double): New take profit price.
        expriation (long): New expiration time given as timestamp in seconds.
            Can be zero if the order should not have an expiration time.
    */
    public void modifyOrder(int ticket, double lots, double price, double stopLoss, double takeProfit, long expiration) {

        var content = ticket + "," + lots + "," + price + "," + stopLoss + "," + takeProfit + "," + expiration;
        sendCommand("MODIFY_ORDER", content);
    }

    /*Sends a CLOSE_ORDER command with lots=0 to close an order completely.
     */
    public void closeOrder(int ticket) {

        var content = ticket + ",0";
        sendCommand("CLOSE_ORDER", content);
    }

    /*Sends a CLOSE_ORDER command to close an order.

    Args:
        ticket (int): Ticket of the order that should be closed.
        lots (double): Volume in lots. If lots=0 it will try to
            close the complete position.
    */
    public void closeOrder(int ticket, double lots) {

        var content = ticket + "," + lots;
        sendCommand("CLOSE_ORDER", content);
    }

    /*Sends a CLOSE_ALL_ORDERS command to close all orders.
     */
    public void closeAllOrders() {

        sendCommand("CLOSE_ALL_ORDERS", "");
    }

    /*Sends a CLOSE_ORDERS_BY_SYMBOL command to close all orders
    with a given symbol.

    Args:
        symbol (str): Symbol for which all orders should be closed.
	*/
    public void closeOrdersBySymbol(String symbol) {

        sendCommand("CLOSE_ORDERS_BY_SYMBOL", symbol);
    }

    /*Sends a CLOSE_ORDERS_BY_MAGIC command to close all orders
    with a given magic number.

    Args:
        magic (str): Magic number for which all orders should
            be closed.
	*/
    public void closeOrdersByMagic(int magic) {

        sendCommand("CLOSE_ORDERS_BY_MAGIC", Integer.toString(magic));
    }

    /*Sends a RESET_COMMAND_IDS command to reset stored command IDs.
    This should be used when restarting the java side without restarting
    the mql side.
	*/
    public void resetCommandIDs() {

        commandID = 0;

        sendCommand("RESET_COMMAND_IDS", "");

        // sleep to make sure it is read before other commands.
        Helpers.sleep(500);
    }

    /*Sends a command to the mql server by writing it to
    one of the command files.

    Multiple command files are used to allow for fast execution
    of multiple commands in the correct chronological order.

    The method needs to be synchronized so that different threads
    do not use the same commandID or write at the same time.
    */
    synchronized void sendCommand(String command, String content) {

        commandID = (commandID + 1) % 100000;

        var text = "<:" + commandID + "|" + command + "|" + content + ":>";

        var now = System.currentTimeMillis();
        var endMillis = now + maxRetryCommandSeconds * 1000L;

        // trying again for X seconds in case all files exist or are
        // currently read from mql side.
        while (now < endMillis) {

            // using 10 different files to increase the execution speed
            // for multiple commands.
            var success = false;
            var maxCommandFiles = 20;
            for (var i = 0; i < maxCommandFiles; i++) {

                var filePath = Paths.get(pathCommandsPrefix.toString() + i + ".txt");
                var f = filePath.toFile();
                if (!f.exists() && Helpers.tryWriteToFile(filePath, text)) {
                    success = true;
                    break;
                }
            }
            if (success) break;
            Helpers.sleep(sleepDelay);
            now = System.currentTimeMillis();
        }
    }
}
