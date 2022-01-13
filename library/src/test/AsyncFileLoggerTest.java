import com.adam.swing_project.library.logger.AsyncFileLogger;

import java.io.*;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsyncFileLoggerTest implements ITest{
    @Override
    public void doTest() throws Exception{
        File logFile = new File("test.log");
        FileWriter writer = new FileWriter(logFile);
        writer.write("");
        writer.flush();
        writer.close();
        AsyncFileLogger logger = AsyncFileLogger.createLogger(new Object(), logFile);
        Pattern logPattern = Pattern.compile(".{8}\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} \\[.*] (.*)");
        Random random = new Random();
        int testRandomInt = random.nextInt(15) + 5;
        int[] logRandomInts = new int[testRandomInt];
        for(int i=0;i<testRandomInt;i++) {
            int logRandomInt = random.nextInt(5000) + 1000;
            logRandomInts[i] = logRandomInt;
            System.out.println("logRandomInt=" + logRandomInt);
            for(int j=0;j<logRandomInt;j++) {
                logger.logInfo("log " + j);
            }
        }
        Thread.sleep(500);
        logger.flushRequired();
        BufferedReader reader = new BufferedReader(new FileReader(logFile));
        String logLine;
        int j=0;
        int i=0;
        while((logLine = reader.readLine()) != null) {
            Matcher matcher = logPattern.matcher(logLine);
            TestAssert.assertIsTrue(matcher.matches());
            try {
                String group0 = matcher.group(1);
                TestAssert.assertIsTrue(group0.equals("log " + j), "Unmatched logGroup=" + group0 + ",j=" + j);
                j++;
                if(j==logRandomInts[i]) {
                    i++;
                    j=0;
                }
            } catch (IllegalStateException e) {
                System.err.println(logLine);
            }
        }
        reader.close();
    }
}
