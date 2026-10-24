package com.pantheon3d.jellykeep;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

/** Background music: loops the first assets/music/*.ogg. Silent until that file exists (see the audio requests doc). */
final class Music {
    private final Context ctx;
    private final String file;
    private MediaPlayer mp;

    Music(Context ctx) {
        this.ctx = ctx.getApplicationContext() != null ? ctx.getApplicationContext() : ctx;
        String found = null;
        try {
            String[] f = this.ctx.getAssets().list("music");
            if (f != null) for (String s : f) if (s.endsWith(".ogg")) { found = "music/" + s; break; }
        } catch (Exception ignored) { }
        file = found;
    }

    void play(boolean on) {
        if (!on || file == null) { stop(); return; }
        if (mp == null) {
            try (AssetFileDescriptor fd = ctx.getAssets().openFd(file)) {
                mp = new MediaPlayer();
                mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                mp.setLooping(true);
                mp.setVolume(0.35f, 0.35f);
                mp.prepare();
            } catch (Exception e) {
                stop();
                return;
            }
        }
        if (!mp.isPlaying()) mp.start();
    }

    void pause() { if (mp != null && mp.isPlaying()) mp.pause(); }

    private void stop() {
        if (mp != null) { mp.release(); mp = null; }
    }
}
