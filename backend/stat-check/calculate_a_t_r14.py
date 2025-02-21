#!/usr/bin/env python3
import pandas as pd
import numpy as np


def compute_atr14(df):
    # Calculate previous closing values
    df['prev_close'] = df['close'].shift(1)

    # Compute True Range (TR) for each row:
    # For the current row, TR is the maximum of:
    #   (high - low),
    #   abs(high - previous close),
    #   abs(low - previous close)
    def calc_tr(row):
        if pd.isna(row['prev_close']):
            return row['high'] - row['low']
        return max(
            row['high'] - row['low'],
            abs(row['high'] - row['prev_close']),
            abs(row['low'] - row['prev_close'])
        )

    df['TR'] = df.apply(calc_tr, axis=1)

    # Initialize the atr14 series using Wilder's method:
    atr14 = [np.nan] * len(df)

    # First valid ATR14 value is the simple average of the first 14 TR values.
    if len(df) >= 14:
        initial_atr = df['TR'].iloc[:14].mean()
        atr14[13] = initial_atr

        # Apply recursive smoothing for subsequent periods:
        for i in range(14, len(df)):
            atr14[i] = (atr14[i - 1] * 13 + df['TR'].iloc[i]) / 14

    df['atr14'] = atr14

    # Remove the temporary columns if desired
    df.drop(columns=['prev_close', 'TR'], inplace=True)

    return df


def main():
    # Define file paths (customize if needed)
    input_csv = "5_hourly_atr_es-1dF_with_date.csv"
    output_csv = "5_hourly_atr_es-1dF_with_date_atr14.csv"

    # Read CSV file into DataFrame
    df = pd.read_csv(input_csv)

    # Compute the 14-period ATR and add it as a new column
    df = compute_atr14(df)

    # Save the updated DataFrame to a new CSV file
    df.to_csv(output_csv, index=False)
    print(f"Updated file saved as {output_csv}")


if __name__ == '__main__':
    main()