import pandas as pd

# Load data from CSV with the correct structure and headers
df = pd.read_csv('SP500.csv')  # No need for custom headers, use those in CSV
df['Date'] = pd.to_datetime(df['Date'])
df.set_index('Date', inplace=True)

# Calculate True Range
df['Previous Close'] = df['Close'].shift(1)
df['High-Low'] = df['High'] - df['Low']
df['High-PrevClose'] = abs(df['High'] - df['Previous Close'])
df['Low-PrevClose'] = abs(df['Low'] - df['Previous Close'])

# True Range is the maximum of these three values
df['True Range'] = df[['High-Low', 'High-PrevClose', 'Low-PrevClose']].max(axis=1)

# Calculate ATR using a rolling mean (14-day period)
df['ATR'] = df['True Range'].rolling(window=14).mean()

# Output the latest ATR value
latest_atr = df['ATR'].iloc[-1]  # Get the last value from the ATR column
print(f"Latest ATR: {latest_atr}")
