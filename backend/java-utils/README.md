# Yahoo HTML to CSV Utility

This project provides utilities for converting and processing data from HTML files from Yahoo finance into CSV format. 

This is div to copy from yahoo finance. Here is link https://finance.yahoo.com/quote/GC%3DF/history/

```html
<div class="table-container yf-1jecxey">
    <table class="table yf-1jecxey noDl">
```

## Current Features

### **YahooHTMLToDailyEACsvParser.java**
This utility processes an HTML file from yahoo finance containing daily financial data (e.g., stock prices, forex rates) and exports it as a CSV file.

#### Current Functionality:
- Parses an **HTML table** with date, open, high, low, and close values.
- Formats the date to the standard `yyyy-MM-dd` format (e.g., `Jan 31, 2025` → `2025-01-31`).
- Writes column headers: `Date`, `Open`, `High`, `Low`, `Close`.
- Outputs a CSV file with cleaned and formatted data.

#### Input and Output
- **Input**: An HTML file containing a table format (e.g., data exported from Yahoo Finance).
- **Output**: A CSV file with structured financial data.

### Example:
**Input Table Structure**:
```html
<table>
    <tr>
        <td>Jan 28, 2025</td>
        <td>1,000.12</td>
        <td>1,050.25</td>
        <td>980.75</td>
        <td>1,040.80</td>
    </tr>
</table>
```

**Output Data**:
```csv
Date,Open,High,Low,Close
2025-01-28,1000.12,1050.25,980.75,1040.80
```

## How to Run

1. Prepare an input HTML file with the financial data in table format (example file: `input/xauusd_input.html`).
2. Set the input and output file paths in the `main` method:
   ```java
   String inputFileName = "input/xauusd_input.html";
   String outputFileName = "output/xauusd_output.csv";
   ```
3. Open the project in **IntelliJ IDEA**.  
   Ensure the **Working Directory** is set correctly.  
   Go to **Run > Edit Configurations... > Working Directory** and set it to the project root directory where the `input` and `output` folders are located.
4. Run the program from IntelliJ by selecting the `YahooHTMLToDailyEACsvParser` as the main class.

5. The output CSV will be saved at the location specified in `outputFileName` (e.g., `output/xauusd_output.csv`).

## Supported Date Format

The utility supports parsing dates in the format:
- `MMM dd, yyyy` (e.g., `Jan 28, 2025`)

It will convert the date to:
- `yyyy-MM-dd` (e.g., `2025-01-28`)


