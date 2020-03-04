package corelyzer.data.tabular;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import com.opencsv.exceptions.CsvException;
import com.opencsv.*;

public class OpenCSVParser {
    public static List<String[]> parseCSV(File csvFile, char delimiter) throws IOException, CsvException {
        CSVParser parser = new CSVParserBuilder().withSeparator(delimiter).build();
        CSVReader reader = new CSVReaderBuilder(new FileReader(csvFile)).withCSVParser(parser).build();
        // NOTE: the List<String[]> returned by reader.readAll() is a LinkedList, which means
        // .get() calls are *slow* for large files parsed in this manner. Using an iterator
        // instead makes it blazing fast, but also makes for slightly clunkier code if you care about
        // which row number you happen to be on at the moment (as we do in DataImportWizard.java).
        // Thus we build our own Vector<String[]>, which is blazing fast with .get() calls.
        // List<String[]> parsedData = reader.readAll();
        Vector<String[]> parsedData = new Vector<String[]>();
        String[] line = null;
        while ((line = reader.readNext()) != null) {
            parsedData.add(line);
        }

        reader.close();
        return parsedData;
    }
}