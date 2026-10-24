package com.pantheon3d.jellykeep;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Opt-in local reminders, no server: one alarm per app close, for whichever comes first of "treasury full"
 * (offline earnings hit their cap) or "daily gift ready" (next morning). Never between 21:00 and 09:00.
 */
public class Notify extends BroadcastReceiver {
    static final String CHANNEL = "reminders";
    static final int ALARM = 7, PERMISSION_REQUEST = 7;

    static void schedule(Context ctx, Game g) {
        cancel(ctx);
        if (!allowed(ctx)) return;
        long now = g.meta.now(), at = -1;
        String title = null, text = null;
        if (g.coinRate > 0.05 && g.bestWave >= 5) {
            at = civil(now + (long) (g.meta.offlineCapSec() * 1000));
            title = "Your treasury is full!";
            text = "The Jelly King saved " + Game.fmt(g.coinRate * g.meta.offlineCapSec() * g.meta.offlineFactor()) + " coins for you.";
        }
        if (g.bestWave >= Game.UNLOCK_GIFT) {
            long gift = civil(nextMorning(now));
            if (at < 0 || gift < at) {
                at = gift;
                title = "Your daily gift is ready";
                text = "Day " + (g.meta.giftReady() ? g.meta.giftIndex() + 1 : (g.meta.giftNext + 1)) + " of 7: keep the streak going!";
            }
        }
        if (at < 0) return;
        Intent i = new Intent(ctx, Notify.class).putExtra("title", title).putExtra("text", text);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, ALARM, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        if (am != null) am.set(AlarmManager.RTC, at, pi);
    }

    static void cancel(Context ctx) {
        PendingIntent pi = PendingIntent.getBroadcast(ctx, ALARM, new Intent(ctx, Notify.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        if (pi != null && am != null) { am.cancel(pi); pi.cancel(); }
    }

    static boolean allowed(Context ctx) {
        return Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    /** Android 13+: the system permission dialog, asked only after the player said yes in-game. */
    static void requestPermission(Game g) {
        if (Build.VERSION.SDK_INT >= 33 && !allowed(g.getContext()) && g.getContext() instanceof Activity)
            ((Activity) g.getContext()).requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST);
    }

    /** 10:00 tomorrow, local time. */
    static long nextMorning(long now) {
        ZonedDateTime t = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault());
        return t.toLocalDate().plusDays(1).atTime(10, 0).atZone(t.getZone()).toInstant().toEpochMilli();
    }

    /** Moves a time that falls in the night (21:00–09:00) to 09:00 the next morning. */
    static long civil(long at) {
        ZonedDateTime t = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault());
        int h = t.getHour();
        if (h >= 21) t = t.toLocalDate().plusDays(1).atTime(9, 0).atZone(t.getZone());
        else if (h < 9) t = t.toLocalDate().atTime(9, 0).atZone(t.getZone());
        return t.toInstant().toEpochMilli();
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!allowed(ctx)) return;
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null) return;
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = new Intent(ctx, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(ctx, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_fg)
                .setContentTitle(intent.getStringExtra("title"))
                .setContentText(intent.getStringExtra("text"))
                .setContentIntent(content)
                .setAutoCancel(true)
                .build();
        nm.notify(ALARM, n);
    }
}
