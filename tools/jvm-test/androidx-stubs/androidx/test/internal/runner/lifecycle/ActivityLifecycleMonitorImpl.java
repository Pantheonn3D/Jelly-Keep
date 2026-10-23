package androidx.test.internal.runner.lifecycle;

import android.app.Activity;

import androidx.test.runner.lifecycle.ActivityLifecycleCallback;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitor;
import androidx.test.runner.lifecycle.Stage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ActivityLifecycleMonitorImpl implements ActivityLifecycleMonitor {
    private final List<ActivityLifecycleCallback> callbacks = new CopyOnWriteArrayList<>();
    private final Map<Activity, Stage> stages = new WeakHashMap<>();

    public ActivityLifecycleMonitorImpl() { }

    public ActivityLifecycleMonitorImpl(boolean declawThreadCheck) { }

    @Override public void addLifecycleCallback(ActivityLifecycleCallback callback) { callbacks.add(callback); }

    @Override public void removeLifecycleCallback(ActivityLifecycleCallback callback) { callbacks.remove(callback); }

    @Override public synchronized Stage getLifecycleStageOf(Activity activity) {
        Stage s = stages.get(activity);
        if (s == null) throw new IllegalArgumentException("Unknown activity: " + activity);
        return s;
    }

    @Override public synchronized Collection<Activity> getActivitiesInStage(Stage stage) {
        List<Activity> out = new ArrayList<>();
        for (Map.Entry<Activity, Stage> e : stages.entrySet()) if (e.getValue() == stage) out.add(e.getKey());
        return out;
    }

    public void signalLifecycleChange(Stage stage, Activity activity) {
        synchronized (this) {
            if (stage == Stage.DESTROYED) stages.remove(activity); else stages.put(activity, stage);
        }
        for (ActivityLifecycleCallback c : callbacks) c.onActivityLifecycleChanged(activity, stage);
    }
}
