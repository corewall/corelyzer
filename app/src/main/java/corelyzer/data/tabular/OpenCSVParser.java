package corelyzer.data.tabular;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.opencsv.exceptions.CsvException;
import com.opencsv.*;

public class OpenCSVParser {
    public static List<String[]> parseCSV(File csvFile, char delimiter) throws IOException, CsvException {
        CSVParser parser = new CSVParserBuilder().withSeparator(delimiter).build();
        CSVReader reader = new CSVReaderBuilder(new FileReader(csvFile)).withCSVParser(parser).build();
        List<String[]> parsedData = reader.readAll();
        // System.out.println("Parsed data has " + lines.get(0).length + " columns");
        // System.out.println("Parsed first data row has " + lines.get(2).length + " columns");
        reader.close();
        return parsedData;
    }
}