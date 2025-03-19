//+------------------------------------------------------------------+
//|                                              ListSymbolsCSV.mq5  |
//|  This EA writes all tradeable symbols along with their details   |
//|  to a CSV file named "TradeableSymbols.csv" using CSV mode,       |
//|  which outputs comma separated values.                           |
//+------------------------------------------------------------------+
#property copyright "2025"
#property version   "1.00"
#property strict

int OnInit()
{
   int totalSymbols = SymbolsTotal(false);
   if(totalSymbols <= 0)
   {
      Print("No symbols available.");
      return INIT_FAILED;
   }
   
   // Open CSV file with FILE_CSV flag (automatically comma separated)
   int fileHandle = FileOpen("TradeableSymbols.csv", FILE_WRITE|FILE_CSV);
   if(fileHandle < 0)
   {
      Print("Failed to open file for writing. Error code: ", GetLastError());
      return INIT_FAILED;
   }
   
   // Write header row
   FileWrite(fileHandle, "Symbol,Description,Digits,TickSize,TickValue,ContractSize,CurrencyBase,CurrencyProfit,VolumeMin,VolumeMax,VolumeStep");

   for(int i = 0; i < totalSymbols; i++)
   {
      string symbol = SymbolName(i, false);
      if(symbol == "")
         continue;
         
      if(!SymbolSelect(symbol, true))
         continue;
      
      if(SymbolInfoInteger(symbol, SYMBOL_TRADE_MODE) == SYMBOL_TRADE_MODE_DISABLED)
         continue;
      
      string description    = SymbolInfoString(symbol, SYMBOL_DESCRIPTION);
      int    digits         = (int)SymbolInfoInteger(symbol, SYMBOL_DIGITS);
      double tickSize       = SymbolInfoDouble(symbol, SYMBOL_TRADE_TICK_SIZE);
      double tickValue      = SymbolInfoDouble(symbol, SYMBOL_TRADE_TICK_VALUE);
      double contractSize   = SymbolInfoDouble(symbol, SYMBOL_TRADE_CONTRACT_SIZE);
      string currencyBase   = SymbolInfoString(symbol, SYMBOL_CURRENCY_BASE);
      string currencyProfit = SymbolInfoString(symbol, SYMBOL_CURRENCY_PROFIT);
      double volumeMin      = SymbolInfoDouble(symbol, SYMBOL_VOLUME_MIN);
      double volumeMax      = SymbolInfoDouble(symbol, SYMBOL_VOLUME_MAX);
      double volumeStep     = SymbolInfoDouble(symbol, SYMBOL_VOLUME_STEP);
      
      FileWrite(fileHandle, symbol + "," + description + "," + digits + "," + tickSize + "," + tickValue + "," +  contractSize  + "," + currencyBase + "," +  currencyProfit + "," +  volumeMin  + "," +  volumeMax  + "," +  volumeStep);
   }
   
   FileClose(fileHandle);
   Print("Tradeable symbols exported successfully to TradeableSymbols.csv");
   
   // Set timer to run export once every 86400 seconds (24 hours)
   EventSetTimer(86400);
   return INIT_SUCCEEDED;
}

void OnDeinit(const int reason)
{
   // Kill the timer when the EA is removed
   EventKillTimer();
}

void OnTick() {}

// OnTimer is called once every 24 hours and re-runs the export code
void OnTimer()
{
   int totalSymbols = SymbolsTotal(false);
   if(totalSymbols <= 0)
   {
      Print("No symbols available.");
      return;
   }
   
   int fileHandle = FileOpen("TradeableSymbols.csv", FILE_WRITE|FILE_CSV);
   if(fileHandle < 0)
   {
      Print("Failed to open file for writing. Error code: ", GetLastError());
      return;
   }
   
   FileWrite(fileHandle, "Symbol,Description,Digits,TickSize,TickValue,ContractSize,CurrencyBase,CurrencyProfit,VolumeMin,VolumeMax,VolumeStep");

   for(int i = 0; i < totalSymbols; i++)
   {
      string symbol = SymbolName(i, false);
      if(symbol == "")
         continue;
         
      if(!SymbolSelect(symbol, true))
         continue;
      
      if(SymbolInfoInteger(symbol, SYMBOL_TRADE_MODE) == SYMBOL_TRADE_MODE_DISABLED)
         continue;
      
      string description    = SymbolInfoString(symbol, SYMBOL_DESCRIPTION);
      int    digits         = (int)SymbolInfoInteger(symbol, SYMBOL_DIGITS);
      double tickSize       = SymbolInfoDouble(symbol, SYMBOL_TRADE_TICK_SIZE);
      double tickValue      = SymbolInfoDouble(symbol, SYMBOL_TRADE_TICK_VALUE);
      double contractSize   = SymbolInfoDouble(symbol, SYMBOL_TRADE_CONTRACT_SIZE);
      string currencyBase   = SymbolInfoString(symbol, SYMBOL_CURRENCY_BASE);
      string currencyProfit = SymbolInfoString(symbol, SYMBOL_CURRENCY_PROFIT);
      double volumeMin      = SymbolInfoDouble(symbol, SYMBOL_VOLUME_MIN);
      double volumeMax      = SymbolInfoDouble(symbol, SYMBOL_VOLUME_MAX);
      double volumeStep     = SymbolInfoDouble(symbol, SYMBOL_VOLUME_STEP);
      
      FileWrite(fileHandle, symbol + "," + description + "," + digits + "," + tickSize + "," + tickValue + "," +  contractSize  + "," + currencyBase + "," + currencyProfit + "," + volumeMin + "," + volumeMax + "," + volumeStep);
   }
   
   FileClose(fileHandle);
   Print("Tradeable symbols exported successfully to TradeableSymbols.csv (Daily update)");
}

