package androidx.test.runner.intent;

public final class IntentMonitorRegistry {
    private static volatile IntentMonitor instance;

    private IntentMonitorRegistry() { }

    public static IntentMonitor getInstance() {
        if (instance == null) throw new IllegalStateException("No intent monitor registered");
        return instance;
    }

    public static void registerInstance(IntentMonitor m) { instance = m; }
}
