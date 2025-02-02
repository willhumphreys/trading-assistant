package uk.co.threebugs;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class YahooHTMLToDailyEACsvParser {

    public static void main(String[] args) {
        // Input and output file paths
        String inputFileName = "input/sp500_input.html"; // HTML file input
        String outputFileName = "output/sp500_output.csv"; // CSV file output

//        // Input and output file paths
//        String inputFileName = "input/xauusd_input.html"; // HTML file input
//        String outputFileName = "output/xauusd_output.csv"; // CSV file output

        // Parse the HTML file and extract table data
        try {
            // Load the HTML file
            File inputFile = new File(inputFileName);
            Document document = Jsoup.parse(inputFile, "UTF-8");

            // Select the table containing the data
            Element table = document.select("table").first(); // Assumes the target SP500 data is in the first table
            Elements tableRows = table.select("tbody tr"); // Select all rows in the table's tbody

            // Prepare the CSV output
            FileWriter csvWriter = new FileWriter(outputFileName);

            // Write the CSV header row
            csvWriter.append("Date,Open,High,Low,Close\n");

            // Process each table row
            for (Element row : tableRows) {
                // Get data from each cell in the row
                Elements cells = row.select("td");

                if (cells.size() >= 5) { // Ensure there are at least 5 columns (Date, Open, High, Low, Close)
                    String date = formatDateString(cells.get(0).text()); // Format the date (e.g., "Jan 31, 2025" -> "2025-01-31")
                    String open = cells.get(1).text().replace(",", ""); // Remove commas
                    String high = cells.get(2).text().replace(",", "");
                    String low = cells.get(3).text().replace(",", "");
                    String close = cells.get(4).text().replace(",", "");

                    // Write the data to the CSV file
                    csvWriter.append(String.format("%s,%s,%s,%s,%s\n", date, open, high, low, close));
                }
            }

            // Close the writer
            csvWriter.flush();
            csvWriter.close();

            // Print success message
            System.out.println("File processed and saved as: " + outputFileName);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("An error occurred while processing the file: " + e.getMessage());
        }
    }

    /**
     * Convert date from 'MMM dd, yyyy' to 'yyyy-MM-dd'.
     *
     * @param dateString the original date string (e.g., "Jan 31, 2025")
     * @return formatted date string in 'yyyy-MM-dd' format
     */
    private static String formatDateString(String dateString) {
        try {
            String[] parts = dateString.split(" ");
            String month = parts[0];
            String day = parts[1].replace(",", "");
            String year = parts[2];

            // Map of month abbreviations to numbers
            Map<String, String> monthMap = new HashMap<>();
            monthMap.put("Jan", "01");
            monthMap.put("Feb", "02");
            monthMap.put("Mar", "03");
            monthMap.put("Apr", "04");
            monthMap.put("May", "05");
            monthMap.put("Jun", "06");
            monthMap.put("Jul", "07");
            monthMap.put("Aug", "08");
            monthMap.put("Sep", "09");
            monthMap.put("Oct", "10");
            monthMap.put("Nov", "11");
            monthMap.put("Dec", "12");

            return year + "-" + monthMap.get(month) + "-" + (day.length() == 1 ? "0" + day : day);
        } catch (Exception e) {
            System.err.println("Error formatting date: " + dateString);
            return dateString; // Return original string if formatting fails
        }
    }

}