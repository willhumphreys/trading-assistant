import pandas as pd

# Load the CSV file
file_name = '5_hourly_atr_spx-1h-btmF.csv'  # Update with your file name
df = pd.read_csv(file_name)

# Convert the 'Timestamp' column to datetime format
df['Timestamp'] = pd.to_datetime(df['Timestamp'], unit='s')

# Set 'Timestamp' as the index
df.set_index('Timestamp', inplace=True)

# Keep only relevant columns for ATR calculation
df = df[['open', 'high', 'low', 'close']]

# Rename columns to match a standard naming convention for better consistency
df.rename(columns={'open': 'Open', 'high': 'High', 'low': 'Low', 'close': 'Close'}, inplace=True)

# Calculate the True Range
df['Previous Close'] = df['Close'].shift(1)
df['High-Low'] = df['High'] - df['Low']
df['High-PrevClose'] = abs(df['High'] - df['Previous Close'])
df['Low-PrevClose'] = abs(df['Low'] - df['Previous Close'])

# True Range is the maximum of the three values
df['True Range'] = df[['High-Low', 'High-PrevClose', 'Low-PrevClose']].max(axis=1)

# Calculate ATR using a rolling mean (14-period ATR)
df['ATR'] = df['True Range'].rolling(window=14 * 24).mean()

# Output the latest ATR value
latest_atr = df['ATR'].iloc[-1]  # Get the last value from the ATR column
print(f"Latest ATR: {latest_atr}")
