package com.google.android.play.core.review;
import android.app.Activity;
import com.google.android.gms.tasks.Task;
public interface ReviewManager {
    Task<ReviewInfo> requestReviewFlow();
    Task<Void> launchReviewFlow(Activity activity, ReviewInfo reviewInfo);
}
