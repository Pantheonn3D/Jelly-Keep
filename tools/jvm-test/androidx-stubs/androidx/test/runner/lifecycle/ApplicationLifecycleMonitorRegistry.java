package androidx.test.runner.lifecycle;

public final class ApplicationLifecycleMonitorRegistry {
    private static volatile ApplicationLifecycleMonitor instance;

    private ApplicationLifecycleMonitorRegistry() { }

    public static ApplicationLifecycleMonitor getInstance() {
        if (instance == null) throw new IllegalStateException("No application lifecycle monitor registered");
        return instance;
    }

    public static void registerInstance(ApplicationLifecycleMonitor m) { instance = m; }
}
