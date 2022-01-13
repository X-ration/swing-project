import com.adam.swing_project.library.logger.ConsoleLogger;
import com.adam.swing_project.library.logger.Logger;

import java.util.*;

public class TestRunner {

    private static final Queue<ITest> testQueue = new LinkedList<>();
    private static final Logger logger = ConsoleLogger.createLogger(TestRunner.class);

    public static void registerTest(ITest test) {
        testQueue.add(test);
    }

    public static void main(String[] args) {
        registerTest(new AsyncFileLoggerTest());
        runTests();
    }

    private static void runTests() {
        int total = testQueue.size(), successTotal = 0, failTotal = 0;
        List<String> failTests = new LinkedList<>();
        while(testQueue.peek() != null) {
            ITest test = testQueue.poll();
            try {
                test.doTest();
                successTotal++;
            } catch (Exception e) {
                e.printStackTrace();
                failTotal++;
                failTests.add(test.getClass().getName());
            }
        }
        logger.logInfo("TestRunner report");
        logger.logInfo("Total:" + total + " Success:" + successTotal + " Fail:" + failTotal);
        if(failTotal > 0) {
            logger.logInfo("Failed tests:" + Arrays.toString(failTests.toArray()));
        }
        System.exit(0);
    }
}
