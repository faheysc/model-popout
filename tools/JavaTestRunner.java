import org.junit.runner.*;
import org.junit.runner.notification.*;
public class JavaTestRunner {
    public static void main(String[] names) throws Exception {
        JUnitCore core = new JUnitCore(); int[] skipped = {0};
        core.addListener(new RunListener() { @Override public void testAssumptionFailure(Failure f) { skipped[0]++; } });
        Class<?>[] tests = new Class<?>[names.length];
        for (int i = 0; i < names.length; i++) tests[i] = Class.forName(names[i]);
        Result result = core.run(tests);
        for (Failure f : result.getFailures()) System.out.println(f.getTrace());
        System.out.println("Passed: " + (result.getRunCount()-result.getFailureCount()-skipped[0]) + ", failed: " + result.getFailureCount() + ", skipped: " + skipped[0]);
        if (!result.wasSuccessful()) System.exit(1);
    }
}
