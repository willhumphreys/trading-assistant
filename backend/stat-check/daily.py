#!/usr/bin/env python3
import pandas as pd
import numpy as np


def calculate_atr(df, period=14):
    """
    Calculates the ATR (Average True Range) for a DataFrame containing
    columns: 'High', 'Low', 'Close'. The first ATR value is computed as
    the simple moving average of the True Range for the first 'period' rows,
    then using Wilder's smoothing method thereafter.
    """
    # Ensure the relevant columns exist
    for col in ['High', 'Low', 'Close']:
        if col not in df.columns:
            raise ValueError(f"Column '{col}' is missing from the data.")

    # Calculate True Range (TR)
    # For each day, compute three distances:
    # 1. Current High minus current Low
    # 2. Absolute difference between current High and previous Close
    # 3. Absolute difference between current Low and previous Close
    df['Prev_Close'] = df['Close'].shift(1)
    df['TR'] = df[['High', 'Low']].apply(lambda x: x['High'] - x['Low'], axis=1)
    df['TR'] = np.maximum(df['TR'], (df['High'] - df['Prev_Close']).abs())
    df['TR'] = np.maximum(df['TR'], (df['Low'] - df['Prev_Close']).abs())

    # Calculate ATR column - using Wilder's method. For the first ATR, use the simple average.
    atr = [np.nan] * len(df)

    # Simple average on the first period rows
    first_atr = df['TR'].iloc[1:period + 1].mean()  # note: start with index 1 since index0 has no previous close TR
    atr[period] = first_atr  # ATR is available only from 'period' index onwards

    # Then, from period+1 to the end use Wilder's smoothing method:
    for i in range(period + 1, len(df)):
        atr[i] = (atr[i - 1] * (period - 1) + df['TR'].iloc[i]) / period

    df['ATR'] = atr

    # Clean up the temporary columns
    df.drop(columns=['Prev_Close', 'TR'], inplace=True)
    return df


def main():
    # File paths (adjust as needed)
    input_csv = "sp500.csv"  # Input CSV file with columns Date, Open, High, Low, Close
    output_csv = "daily_with_atr.csv"

    # Read CSV data into DataFrame
    df = pd.read_csv(input_csv, parse_dates=['Date'])

    # Ensure numeric columns - sometimes CSV parsing may need conversion
    for col in ['Open', 'High', 'Low', 'Close']:
        df[col] = pd.to_numeric(df[col], errors='coerce')

    # Sort data by date just in case
    df.sort_values('Date', inplace=True)
    df.reset_index(drop=True, inplace=True)

    # Calculate 14-day ATR
    df = calculate_atr(df, period=14)

    # Write the new DataFrame to CSV
    df.to_csv(output_csv, index=False)
    print(f"ATR calculation complete. Updated data saved to {output_csv}")


if __name__ == '__main__':
    main()
