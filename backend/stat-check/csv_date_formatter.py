#!/usr/bin/env python3
import pandas as pd


def main():
    # Define file paths (adjust as needed)
    input_csv = "5_hourly_atr_es-1dF.csv"
    output_csv = "5_hourly_atr_es-1dF_with_date.csv"

    # Read the CSV file into a DataFrame
    df = pd.read_csv(input_csv)

    # Convert the 'Timestamp' column (assuming it is in seconds) into a formatted date string YYYY-MM-DD
    df['Date'] = pd.to_datetime(df['Timestamp'], unit='s').dt.strftime('%Y-%m-%d')

    # Save the DataFrame with the new column into a new CSV file
    df.to_csv(output_csv, index=False)
    print(f"File saved as {output_csv}")


if __name__ == '__main__':
    main()