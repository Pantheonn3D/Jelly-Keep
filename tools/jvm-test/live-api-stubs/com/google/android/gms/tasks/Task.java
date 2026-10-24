package com.google.android.gms.tasks;
public abstract class Task<TResult> {
    public abstract Task<TResult> addOnCompleteListener(OnCompleteListener<TResult> listener);
    public abstract boolean isSuccessful();
    public abstract TResult getResult();
}
