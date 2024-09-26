package nl.theepicblock.scanner;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Pattern;

public class ReadableScannerTests {
    @Test
    public void skipThenRead() throws IOException {
        var testString = "aaaaabbbbcccdddd";
        var readable = new StringReader(testString);
        var readableReader = new ReadableScanner(readable, 2);

        readableReader.skip(Pattern.compile("a*"));

        Assertions.assertEquals(
                "bbbbccc",
                readableReader.read(Pattern.compile("[bc]*"))
        );
        Assertions.assertEquals(
                "dddd",
                readableReader.read(Pattern.compile("d*"))
        );
    }

    @ParameterizedTest
    @CsvSource(value = """
            2, 1, 2
            3, 1, 2
            4, 1, 3
            2, 1, 3
            7, 3, 3
            8, 5, 2
            2, 2, 2
            3, 2, 2
            100, 100, 100
            """)
    public void readRepeatedly(int capacity, int horizon1, int horizon2) throws IOException {
        var testString = "aaaaaPaPbPcPFPaPbPc";
        var readable = new StringReader(testString);
        var readableReader = new ReadableScanner(readable, capacity);

        Assertions.assertEquals(
                "aaaaa",
                readableReader.readRepeatedly(Pattern.compile("a"), horizon1)
        );
        // Test horizon
        Assertions.assertEquals(
                "PaPbPc",
                readableReader.readRepeatedly(Pattern.compile("P[^F]"), horizon2)
        );
        Assertions.assertEquals(
                "PFPaPbPc",
                readableReader.readRepeatedly(Pattern.compile(".*"), horizon1)
        );
    }
}