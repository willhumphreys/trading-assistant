//+------------------------------------------------------------------+
//|                                                   PriceWriter.mq5
//|                           Logs the *just-closed* daily OHLC once
//+------------------------------------------------------------------
#property copyright ""
#property link      ""
#property version   "1.02"
#property strict

//--- Input parameters
input string InpSymbolsList = "EURUSD,GBPUSD,USDJPY,SP500,XAUUSD"; // Comma-separated list of symbols to log

//--- We'll store the parsed symbols in an array
string symbols[];

//--- We track the time of the *current* daily bar
static datetime lastDailyBarTime[];

//+------------------------------------------------------------------+
//| Expert initialization function                                   |
//+------------------------------------------------------------------
int OnInit()
{
    ParseSymbolsList(InpSymbolsList, symbols);

    if (ArraySize(symbols) < 1)
    {
        Print("No symbols specified. Expert will not run.");
        return INIT_PARAMETERS_INCORRECT;
    }

    // Prepare array for tracking daily bar time
    const int count = ArraySize(symbols);
    ArrayResize(lastDailyBarTime, count);

    // Initialize lastDailyBarTime to the currently open daily bar
    for (int i = 0; i < count; i++)
    {
        const string sym = symbols[i];
        // If there's no valid daily bar data yet, we'll store 0
        if (Bars(sym, PERIOD_D1) > 0)
            lastDailyBarTime[i] = iTime(sym, PERIOD_D1, 0); // time of the currently open daily bar
        else
            lastDailyBarTime[i] = 0; // means no data
    }

    Print("PriceWriter initialized. Symbols to track: ", InpSymbolsList);
    return INIT_SUCCEEDED;
}

//+------------------------------------------------------------------+
//| Expert deinitialization function                                 |
//+------------------------------------------------------------------
void OnDeinit(const int reason)
{
    Print("PriceWriter deinitialized.");
}

//+------------------------------------------------------------------+
//| Expert tick function                                             |
//+------------------------------------------------------------------
void OnTick()
{
    const int count = ArraySize(symbols);
    if (count < 1) return;

    // For each symbol, check if a new daily bar has formed
    for (int i = 0; i < count; i++)
    {
        const string sym = symbols[i];

        // Make sure we can select the symbol
        if (!SymbolSelect(sym, true))
        {
            Print("Failed to select symbol: ", sym);
            continue;
        }

        // If there aren't at least 2 bars in daily timeframe, skip
        if (Bars(sym, PERIOD_D1) < 2)
            continue;

        // Grab the time of the currently forming daily bar (shift=0)
        datetime currentDailyBarTime = iTime(sym, PERIOD_D1, 0);

        // If the daily bar's time changed vs. what we stored,
        // that means a new daily bar was created => log the just-closed daily bar.
        if (currentDailyBarTime != lastDailyBarTime[i] && currentDailyBarTime > 0)
        {
            // Let's log the *closed* bar at shift=1
            // shift=1 means "the bar that just finished"
            double open_  = iOpen(sym, PERIOD_D1, 1);
            double high_  = iHigh(sym, PERIOD_D1, 1);
            double low_   = iLow(sym, PERIOD_D1, 1);
            double close_ = iClose(sym, PERIOD_D1, 1);

            // We'll also record the time of that just-closed bar
            // which is iTime(sym, PERIOD_D1, 1)
            datetime closedBarTime = iTime(sym, PERIOD_D1, 1);

            // Convert that time to a human-readable string
            MqlDateTime dt;
            TimeToStruct(closedBarTime, dt);
            string timeString = StringFormat("%04d-%02d-%02d", dt.year, dt.mon, dt.day);

            // Write to our CSV
            string fileName = sym + ".csv";
            int fileHandle = FileOpen(fileName, FILE_CSV | FILE_WRITE | FILE_READ | FILE_ANSI);
            if (fileHandle != INVALID_HANDLE)
            {
                FileSeek(fileHandle, 0, SEEK_END);
                FileWrite(fileHandle,
                          timeString,
                          DoubleToString(open_,  5),
                          DoubleToString(high_,  5),
                          DoubleToString(low_,   5),
                          DoubleToString(close_, 5));
                FileClose(fileHandle);
            }
            else
            {
                Print("Failed to open file for symbol: ", sym);
            }

            // Now update our stored bar time to the *newly forming* daily bar
            lastDailyBarTime[i] = currentDailyBarTime;
        }
    }
}

//+------------------------------------------------------------------+
//| Helper function: Parse a comma-separated list of symbols         |
//+------------------------------------------------------------------
void ParseSymbolsList(const string symbolList, string &resultArray[])
{
    ArrayResize(resultArray, 0);

    const int MAX_SYMBOLS = 50;
    const int listLen     = StringLen(symbolList);
    if (listLen <= 0) return;

    string tempSymbols[];
    StringSplit(symbolList, ',', tempSymbols);

    const int splitCount = ArraySize(tempSymbols);
    for (int i = 0; i < splitCount && i < MAX_SYMBOLS; i++)
    {
        string trimmed = tempSymbols[i];
        StringTrimLeft(trimmed);
        StringTrimRight(trimmed);

        if (StringLen(trimmed) > 0)
        {
            const int newSize = ArraySize(resultArray) + 1;
            ArrayResize(resultArray, newSize);
            resultArray[newSize - 1] = trimmed;
        }
    }
}
