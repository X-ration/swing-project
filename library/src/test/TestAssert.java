public class TestAssert {

    public static void assertIsTrue(boolean value, String msg) {
        if(!value) {
            throw new TestException(msg);
        }
    }

    public static void assertIsTrue(boolean value) {
        assertIsTrue(value, null);
    }

}
