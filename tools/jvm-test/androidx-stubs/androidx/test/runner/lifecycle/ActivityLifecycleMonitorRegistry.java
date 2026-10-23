package androidx.test.runner.lifecycle;

public final class ActivityLifecycleMonitorRegistry {
    private static volatile ActivityLifecycleMonitor instance;

    private ActivityLifecycleMonitorRegistry() { }

    public static ActivityLifecycleMonitor getInstance() {
        if (instance == null) throw new IllegalStateException("No lifecycle monitor registered");
        return instance;
    }

    public static void registerInstance(ActivityLifecycleMonitor m) { instance = m; }
}
