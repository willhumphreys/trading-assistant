package uk.co.threebugs;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YahooHTMLToDailyEACsvParser {

    public static void main(String[] args) {
        // Input and output directory paths
        String inputDirectory = "input"; // Directory containing all HTML files
        String outputDirectory = "output"; // Directory where CSV files will be saved

        // Ensure the output directory exists
        new File(outputDirectory).mkdirs();

        // Get all HTML files in the input directory
        File inputDir = new File(inputDirectory);
        File[] htmlFiles = inputDir.listFiles((dir, name) -> name.endsWith(".html"));

        if (htmlFiles == null || htmlFiles.length == 0) {
            System.out.println("No HTML files found in the input directory: " + inputDirectory);
            return;
        }

        // Process each HTML file
        for (File inputFile : htmlFiles) {
            String outputFileName = outputDirectory + "/" + inputFile.getName().replace(".html", ".csv");

            // Parse and convert each file
            try {
                parseHtmlToCsv(inputFile, new File(outputFileName));
                System.out.println("File processed and saved as: " + outputFileName);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("An error occurred while processing the file: " + inputFile.getName());
            }
        }
    }

    /**
     * Parse an HTML file and save the output as a CSV file.
     *
     * @param inputFile  The input HTML file to parse.
     * @param outputFile The output CSV file to create.
     * @throws IOException If an error occurs while reading/writing files.
     */
    private static void parseHtmlToCsv(File inputFile, File outputFile) throws IOException {
        // Load the HTML file
        Document document = Jsoup.parse(inputFile, "UTF-8");

        // Select the table containing the data
        Element table = document.select("table").first(); // Assumes the data is in the first table
        if (table == null) {
            throw new IOException("No table found in the file: " + inputFile.getName());
        }

        Elements tableRows = table.select("tbody tr"); // Select all rows in the table's tbody

        // Create a list to store the rows
        List<DataRow> rows = new ArrayList<>();

        // Process each table row
        for (Element row : tableRows) {
            Elements cells = row.select("td");

            if (cells.size() >= 5) { // Ensure there are at least 5 columns (Date, Open, High, Low, Close)
                String date = formatDateString(cells.get(0).text()); // Format the date (e.g., "Jan 31, 2025" -> "2025-01-31")
                String open = cells.get(1).text().replace(",", ""); // Remove commas
                String high = cells.get(2).text().replace(",", "");
                String low = cells.get(3).text().replace(",", "");
                String close = cells.get(4).text().replace(",", "");

                // Add the row to the list
                rows.add(new DataRow(date, open, high, low, close));
            }
        }

        // Sort the rows by date in ascending order
        Collections.sort(rows, Comparator.comparing(DataRow::getDate));

        // Prepare the CSV output
        FileWriter csvWriter = new FileWriter(outputFile);

        // Write the CSV header row
        csvWriter.append("Date,Open,High,Low,Close\n");

        // Write each sorted row to the CSV file
        for (DataRow row : rows) {
            csvWriter.append(row.toCsvRow());
        }

        csvWriter.flush();
        csvWriter.close();
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

    /**
     * Data class representing a row in the CSV file.
     */
    private static class DataRow {
        private final LocalDate date;
        private final String open;
        private final String high;
        private final String low;
        private final String close;

        public DataRow(String dateString, String open, String high, String low, String close) {
            this.date = LocalDate.parse(dateString); // Convert the date string to LocalDate for easy sorting
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
        }

        public LocalDate getDate() {
            return date;
        }

        public String toCsvRow() {
            return String.format("%s,%s,%s,%s,%s\n", date, open, high, low, close);
        }
    }
}