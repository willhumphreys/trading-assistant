#!/usr/bin/env python3
import pandas as pd


def compute_atr14_sma(df, window=14):
    # Calculate previous closing values
    df['prev_close'] = df['close'].shift(1)

    # Compute True Range (TR) for each row.
    def calc_tr(row):
        if pd.isna(row['prev_close']):
            return row['high'] - row['low']
        return max(
            row['high'] - row['low'],
            abs(row['high'] - row['prev_close']),
            abs(row['low'] - row['prev_close'])
        )

    df['TR'] = df.apply(calc_tr, axis=1)

    # Calculate the simple moving average (rolling mean) of the TR values.
    df['atr14'] = df['TR'].rolling(window=window).mean()

    # Remove temporary columns, if desired.
    df.drop(columns=['prev_close', 'TR'], inplace=True)

    return df


def main():
    input_csv = "5_hourly_atr_es-1dF_with_date.csv"
    output_csv = "5_hourly_atr_es-1dF_with_date_atr14.csv"

    df = pd.read_csv(input_csv)
    df = compute_atr14_sma(df)
    df.to_csv(output_csv, index=False)
    print(f"Updated file saved as {output_csv}")


if __name__ == '__main__':
    main()