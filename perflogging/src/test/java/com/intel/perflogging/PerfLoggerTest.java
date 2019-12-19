package com.intel.perflogging;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;


public class PerfLoggerTest {

    @Rule
    public TemporaryFolder outputFolder_ = new TemporaryFolder();

    private File outputFile_;


    @Before
    public void setup() {
        outputFile_ = new File(outputFolder_.getRoot().getPath()+"/output.log");
        System.setProperty("perfLogging", "true");
        System.setProperty("perfTestNum", "2000");
        System.setProperty("perfTestOutput", outputFolder_.getRoot().getPath()+"/output.log");
    }


    @Test
    public void typicalCountTestCase()  {

        PerfLogger testLogger = PerfLogger.getPerfLogger();

        testLogger.setTargetCount(2000);

        PerfLogger.logEvent(testLogger, "FIRST EVENT!");

        for (int i = 1; i < 2000; i++) {
            PerfLogger.logEvent(testLogger, "EVENT " + i);
        }

        File result = new File(outputFolder_.getRoot(), "output.log");
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(result));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
            return;
        }

        try {
            String firstLine = reader.readLine();
            Assert.assertTrue( firstLine, firstLine.contains("FIRST EVENT!"));
            String lastLine = reader.readLine();
            Assert.assertTrue(lastLine, lastLine.contains("EVENT 1999"));

            Assert.assertNull("Extra output", reader.readLine());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void countWithIncrement() {

        PerfLogger testLogger = PerfLogger.getPerfLogger();

        PerfLogger.logEvent(testLogger, "FIRST EVENT!");
        PerfLogger.incrementCount(testLogger, 1999);
        PerfLogger.logEvent(testLogger, "EVENT 2000");

        File result = new File(outputFolder_.getRoot(), "output.log");
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(result));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
            return;
        }

        try {
            String firstLine = reader.readLine();
            Assert.assertTrue( firstLine, firstLine.contains("FIRST EVENT!"));
            String lastLine = reader.readLine();
            Assert.assertTrue(lastLine, lastLine.contains("EVENT 2000"));

            Assert.assertNull("Extra output", reader.readLine());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void countWithStartLogger() {

        PerfLogger testLogger = PerfLogger.getPerfLogger();


        for (int i = 0; i < 1999; i++) {
            PerfLogger.startLogging(testLogger, "FIRST EVENT!");
            PerfLogger.logEvent(testLogger, "EVENT " + (i+1)  );
        }


        File result = new File(outputFolder_.getRoot(), "output.log");
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(result));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
            return;
        }

        try {
            String firstLine = reader.readLine();
            Assert.assertTrue( firstLine, firstLine.contains("FIRST EVENT!"));
            String lastLine = reader.readLine();
            Assert.assertTrue(lastLine, lastLine.contains("EVENT 1999"));

            Assert.assertNull("Extra output", reader.readLine());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void noLoggingOccurs() {

        PerfLogger testLogger = PerfLogger.getPerfLogger();

        testLogger.setTargetCount(20);

        PerfLogger.logEvent(testLogger, "FIRST EVENT");

        for (int i = 1; i < 15; i++) {
            PerfLogger.logEvent(testLogger, "EVENT " + i);
        }

        Assert.assertFalse(outputFile_.exists());

    }

    @Test
    public void nullLogger() {

        PerfLogger.logEvent(null, "Should not do anything");

        PerfLogger.incrementCount(null, 200 );
        Assert.assertFalse(outputFile_.exists());

    }

    @Test
    public void overCount()  {

        PerfLogger testLogger = PerfLogger.getPerfLogger();

        testLogger.setTargetCount(2000);


        PerfLogger.logEvent(testLogger, "FIRST EVENT!");

        for (int i = 1; i < 3000; i++) {
            PerfLogger.logEvent(testLogger, "EVENT " + i);
        }

        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(outputFile_));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
            return;
        }

        try {
            String firstLine = reader.readLine();
            Assert.assertTrue( firstLine, firstLine.contains("FIRST EVENT!"));
            String lastLine = reader.readLine();
            Assert.assertTrue(lastLine, lastLine.contains("EVENT 1999"));

            Assert.assertNull("Extra output", reader.readLine());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @After
    public void tearDown(){ }
}
