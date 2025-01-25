//+------------------------------------------------------------------+
//|                                                   PriceWriter.mq5
//|                                Example EA to log prices to files
//+------------------------------------------------------------------+
#property copyright ""
#property link      ""
#property version   "1.00"
#property strict

//--- Input parameters
input string InpSymbolsList = "EURUSD,GBPUSD,USDJPY,SP500,XAUUSD"; // Comma-separated list of symbols to log
input bool   InpWriteOnEveryTick = true;                           // If true, write to file on every new tick
input bool   InpLogOHLC         = false;                           // If true, log OHLC of the latest bar instead of Bid/Ask

//--- We'll store the parsed symbols in a global array
string symbols[];

//+------------------------------------------------------------------+
//| Expert initialization function                                   |
//+------------------------------------------------------------------+
int OnInit()
{
    ParseSymbolsList(InpSymbolsList, symbols);

    if (ArraySize(symbols) < 1)
    {
        Print("No symbols specified. Expert will not run.");
        return INIT_PARAMETERS_INCORRECT;
    }

    Print("PriceWriter initialized. Symbols to log: ", InpSymbolsList);
    return INIT_SUCCEEDED;
}

//+------------------------------------------------------------------+
//| Expert deinitialization function                                 |
//+------------------------------------------------------------------+
void OnDeinit(const int reason)
{
    Print("PriceWriter deinitialized.");
}

//+------------------------------------------------------------------+
//| Expert tick function                                             |
//+------------------------------------------------------------------+
void OnTick()
{
    if (!InpWriteOnEveryTick)
        return;

    const int count = ArraySize(symbols);
    for (int i = 0; i < count; i++)
    {
        const string symbol = symbols[i];
        if (!SymbolSelect(symbol, true))
        {
            Print("Failed to select symbol: ", symbol);
            continue;
        }

        //--- Get current time (server time)
        MqlDateTime dateTime;
        TimeToStruct(TimeCurrent(), dateTime);
        const string timeString = StringFormat(
            "%04d-%02d-%02d %02d:%02d:%02d",
            dateTime.year, dateTime.mon, dateTime.day,
            dateTime.hour, dateTime.min, dateTime.sec
        );

        //--- Build file name, e.g. "EURUSD.csv"
        const string fileName = symbol + ".csv";

        //--- Open the file in append mode (CSV), then seek to end
        const int fileHandle = FileOpen(fileName, FILE_CSV | FILE_WRITE | FILE_READ | FILE_ANSI);
        if (fileHandle == INVALID_HANDLE)
        {
            Print("Failed to open file for symbol: ", symbol);
            continue;
        }

        FileSeek(fileHandle, 0, SEEK_END);

        if (!InpLogOHLC)
        {
            //--- Log Bid/Ask
            const double bid = SymbolInfoDouble(symbol, SYMBOL_BID);
            const double ask = SymbolInfoDouble(symbol, SYMBOL_ASK);
            FileWrite(fileHandle, timeString, DoubleToString(bid, 5), DoubleToString(ask, 5));
        }
        else
        {
            //--- Log OHLC from the DAILY timeframe, always
            MqlTick tickInfo;
            if (!SymbolInfoTick(symbol, tickInfo))
            {
                Print("Failed to get tick info for symbol: ", symbol);
                FileClose(fileHandle);
                continue;
            }

            // Latest closed daily bar is shift=1
            const int    shift  = 1;
            const double open_  = iOpen(symbol, PERIOD_D1, shift);
            const double high_  = iHigh(symbol, PERIOD_D1, shift);
            const double low_   = iLow(symbol, PERIOD_D1, shift);
            const double close_ = iClose(symbol, PERIOD_D1, shift);

            FileWrite(fileHandle,
                      timeString,
                      DoubleToString(open_,  5),
                      DoubleToString(high_,  5),
                      DoubleToString(low_,   5),
                      DoubleToString(close_, 5));
        }

        FileClose(fileHandle);
    }
}

//+------------------------------------------------------------------+
//| Helper function: Parse a comma-separated list of symbols         |
//+------------------------------------------------------------------+
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
