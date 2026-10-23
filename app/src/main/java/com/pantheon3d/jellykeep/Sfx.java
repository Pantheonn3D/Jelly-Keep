package com.pantheon3d.jellykeep;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import android.content.res.AssetFileDescriptor;

/** All sounds are synthesized here at first launch (no audio assets, no licensing). */
final class Sfx {
    static final int SR = 44100;
    private final SoundPool pool;
    final int pop, crit, coin, buy, levelUp, star, deny, bonk, bounce, miss, shoot, lob, splat, hit, die, hurt, horn, defeat;
    boolean on = true;

    Sfx(Context ctx) {
        pool = new SoundPool.Builder().setMaxStreams(16).setAudioAttributes(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        File dir = ctx.getCacheDir();
        pop = load(dir, "pop", pop());
        crit = load(dir, "crit", crit());
        coin = load(dir, "coin", coin());
        buy = load(dir, "buy", notes(new float[]{660, 880, 1320}, 0.055f, 0.16f, 0.45f));
        levelUp = load(dir, "lvl", notes(new float[]{523, 659, 784, 1046, 1318}, 0.075f, 0.45f, 0.5f));
        star = load(dir, "star", star());
        deny = load(dir, "deny", deny());
        bonk = load(dir, "bonk", sweep(0.07f, 560, 190, 0.5f, 45));
        bounce = load(dir, "bounce", sweep(0.09f, 320, 110, 0.6f, 35));
        miss = load(dir, "miss", sweep(0.45f, 420, 140, 0.45f, 6));
        shoot = load(dir, "shoot", noisySweep(0.08f, 700, 1500, 0.35f, 40, 0.25));
        lob = load(dir, "lob", sweep(0.16f, 260, 720, 0.5f, 14));
        splat = load(dir, "splat", noisySweep(0.22f, 220, 70, 0.6f, 16, 0.7));
        hit = load(dir, "hit", noisySweep(0.06f, 620, 260, 0.45f, 50, 0.5));
        die = load(dir, "die", noisySweep(0.2f, 520, 120, 0.55f, 18, 0.35));
        hurt = load(dir, "hurt", noisySweep(0.3f, 160, 60, 0.8f, 10, 0.9));
        horn = load(dir, "horn", notes(new float[]{392, 523}, 0.14f, 0.5f, 0.45f));
        defeat = load(dir, "defeat", notes(new float[]{523, 415, 330, 262}, 0.16f, 0.6f, 0.45f));
        loadBanks(ctx);
    }

    private static short[] noisySweep(float dur, double f0, double f1, double vol, double decay, double noise) {
        int n = (int) (dur * SR);
        short[] o = new short[n];
        double ph = 0, lp = 0;
        Random r = new Random(n);
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            double f = f0 * Math.pow(f1 / f0, t / dur);
            ph += 2 * Math.PI * f / SR;
            lp += ((r.nextDouble() * 2 - 1) - lp) * 0.3;
            double env = (1 - Math.exp(-t * 1500)) * Math.exp(-t * decay) * (1 - t / dur);
            o[i] = s16((Math.sin(ph) * (1 - noise) + lp * noise * 1.6) * env * vol);
        }
        return o;
    }

    // ------------------------------------------------------------ recorded banks (assets/sfx/name_N.ogg)
    private final HashMap<String, int[]> banks = new HashMap<>();
    private final HashMap<String, Long> lastPlay = new HashMap<>();
    private final HashMap<String, Integer> lastIdx = new HashMap<>();
    private final Random rnd = new Random();

    private void loadBanks(Context ctx) {
        try {
            String[] files = ctx.getAssets().list("sfx");
            if (files == null) return;
            HashMap<String, java.util.TreeMap<Integer, Integer>> tmp = new HashMap<>();
            for (String f : files) {
                if (!f.endsWith(".ogg")) continue;
                String stem = f.substring(0, f.length() - 4);
                int us = stem.lastIndexOf('_');
                String name = stem.substring(0, us);
                int n = Integer.parseInt(stem.substring(us + 1));
                try (AssetFileDescriptor fd = ctx.getAssets().openFd("sfx/" + f)) {
                    tmp.computeIfAbsent(name, k -> new java.util.TreeMap<>()).put(n, pool.load(fd, 1));
                }
            }
            for (String k : tmp.keySet()) {
                java.util.TreeMap<Integer, Integer> m = tmp.get(k);
                int[] ids = new int[m.size()]; int i = 0;
                for (int id : m.values()) ids[i++] = id;
                banks.put(k, ids);
            }
        } catch (Exception ignored) { }
    }

    boolean has(String name) { return banks.containsKey(name); }

    /**
     * Plays a random take (never the same one twice in a row) with slight pitch variation.
     * minGapMs rate-limits rapid repeats so swarms of towers don't machine-gun.
     */
    boolean bank(String name, float vol, float pitchVar, int minGapMs) { return bank(name, vol, pitchVar, minGapMs, 1f); }

    boolean bank(String name, float vol, float pitchVar, int minGapMs, float basePitch) {
        int[] ids = banks.get(name);
        if (ids == null || !on) return ids != null;
        long now = System.currentTimeMillis();
        Long lp = lastPlay.get(name);
        if (lp != null && now - lp < minGapMs) return true;
        lastPlay.put(name, now);
        int prev = lastIdx.containsKey(name) ? lastIdx.get(name) : -1, i = rnd.nextInt(ids.length);
        if (ids.length > 1 && i == prev) i = (i + 1) % ids.length;
        lastIdx.put(name, i);
        float rate = basePitch * (1 + (rnd.nextFloat() * 2 - 1) * pitchVar);
        pool.play(ids[i], vol, vol, 1, 0, Math.max(0.5f, Math.min(2f, rate)));
        return true;
    }

    /** Plays one specific take (e.g. an ascending run of coin pings). */
    boolean nth(String name, int index, float vol, float rate) {
        int[] ids = banks.get(name);
        if (ids == null || !on) return ids != null;
        pool.play(ids[Math.max(0, Math.min(ids.length - 1, index))], vol, vol, 1, 0, Math.max(0.5f, Math.min(2f, rate)));
        return true;
    }

    void play(int id, float vol, float rate) {
        if (!on || id == 0) return;
        rate = Math.max(0.5f, Math.min(2f, rate));
        pool.play(id, vol, vol, 1, 0, rate);
    }

    // ---------------------------------------------------------------- synthesis

    private static short[] pop() {
        int n = (int) (0.10f * SR);
        short[] o = new short[n];
        double ph = 0;
        Random r = new Random(1);
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            double f = 1100 * Math.pow(260.0 / 1100, t / 0.10);
            ph += 2 * Math.PI * f / SR;
            double env = (1 - Math.exp(-t * 900)) * Math.exp(-t * 38);
            double v = (Math.sin(ph) + 0.25 * Math.sin(2 * ph)) * env * 0.8;
            if (t < 0.004) v += (r.nextDouble() * 2 - 1) * 0.35 * (1 - t / 0.004);
            o[i] = s16(v);
        }
        return o;
    }

    private static short[] crit() {
        int n = (int) (0.38f * SR);
        short[] o = new short[n];
        double ph = 0, ph2 = 0, lp = 0;
        Random r = new Random(2);
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            double f = 190 * Math.pow(45.0 / 190, Math.min(1, t / 0.25));
            ph += 2 * Math.PI * f / SR;
            ph2 += 2 * Math.PI * 1760 / SR;
            lp += ((r.nextDouble() * 2 - 1) - lp) * 0.25;
            double v = Math.sin(ph) * Math.exp(-t * 9) * 0.95
                    + lp * Math.exp(-t * 28) * 0.9
                    + Math.sin(ph2) * Math.exp(-t * 14) * 0.18;
            o[i] = s16(v * (1 - Math.exp(-t * 2000)));
        }
        return o;
    }

    private static short[] coin() {
        int n = (int) (0.13f * SR);
        short[] o = new short[n];
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            double f = t < 0.04 ? 1976 : 2637;
            ph += 2 * Math.PI * f / SR;
            double env = (1 - Math.exp(-t * 3000)) * Math.exp(-t * 22);
            o[i] = s16((Math.sin(ph) + 0.3 * Math.sin(3 * ph)) * env * 0.35);
        }
        return o;
    }

    private static short[] star() {
        int n = (int) (0.42f * SR);
        short[] o = new short[n];
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            double f = 900 * Math.pow(2600.0 / 900, Math.min(1, t / 0.3));
            ph += 2 * Math.PI * f / SR;
            double trem = 0.6 + 0.4 * Math.sin(t * 2 * Math.PI * 28);
            double env = (1 - Math.exp(-t * 400)) * Math.exp(-t * 7);
            o[i] = s16((Math.sin(ph) + 0.4 * Math.sin(2.01 * ph)) * env * trem * 0.4);
        }
        return o;
    }

    private static short[] deny() {
        int n = (int) (0.18f * SR);
        short[] o = new short[n];
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            ph += 2 * Math.PI * 150 / SR;
            double gate = (t < 0.07 || (t > 0.10 && t < 0.17)) ? 1 : 0;
            double sq = Math.tanh(Math.sin(ph) * 4);
            o[i] = s16(sq * gate * 0.22);
        }
        return o;
    }

    private static short[] sweep(float dur, double f0, double f1, double vol, double decay) {
        int n = (int) (dur * SR);
        short[] o = new short[n];
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            double f = f0 * Math.pow(f1 / f0, t / dur);
            ph += 2 * Math.PI * f / SR;
            double env = (1 - Math.exp(-t * 1500)) * Math.exp(-t * decay) * (1 - t / dur);
            o[i] = s16(Math.sin(ph) * env * vol);
        }
        return o;
    }

    /** Little triangle-wave arpeggio; the last note rings for {@code tail} seconds. */
    private static short[] notes(float[] hz, float step, float tail, float vol) {
        float dur = step * (hz.length - 1) + tail + 0.05f;
        int n = (int) (dur * SR);
        double[] mix = new double[n];
        for (int k = 0; k < hz.length; k++) {
            int start = (int) (k * step * SR);
            float len = k == hz.length - 1 ? tail : step * 2.2f;
            int m = Math.min(n - start, (int) (len * SR));
            double ph = 0;
            for (int i = 0; i < m; i++) {
                double t = i / (double) SR;
                ph += 2 * Math.PI * hz[k] / SR;
                double tri = 2 / Math.PI * Math.asin(Math.sin(ph));
                double env = (1 - Math.exp(-t * 800)) * Math.exp(-t * (4.5 / len));
                mix[start + i] += (tri * 0.7 + Math.sin(2 * ph) * 0.15) * env * vol;
            }
        }
        short[] o = new short[n];
        for (int i = 0; i < n; i++) o[i] = s16(mix[i]);
        return o;
    }

    private static short s16(double v) {
        v = Math.max(-1, Math.min(1, v));
        return (short) (v * 32000);
    }

    private int load(File dir, String name, short[] pcm) {
        File f = new File(dir, "sfx2_" + name + ".wav");
        try {
            if (!f.exists() || f.length() != 44 + pcm.length * 2L) writeWav(f, pcm);
            return pool.load(f.getPath(), 1);
        } catch (IOException e) {
            return 0;
        }
    }

    private static void writeWav(File f, short[] pcm) throws IOException {
        int data = pcm.length * 2;
        byte[] b = new byte[44 + data];
        put(b, 0, "RIFF"); le32(b, 4, 36 + data); put(b, 8, "WAVE");
        put(b, 12, "fmt "); le32(b, 16, 16); le16(b, 20, 1); le16(b, 22, 1);
        le32(b, 24, SR); le32(b, 28, SR * 2); le16(b, 32, 2); le16(b, 34, 16);
        put(b, 36, "data"); le32(b, 40, data);
        for (int i = 0; i < pcm.length; i++) le16(b, 44 + i * 2, pcm[i]);
        try (FileOutputStream out = new FileOutputStream(f)) { out.write(b); }
    }

    private static void put(byte[] b, int o, String s) { for (int i = 0; i < 4; i++) b[o + i] = (byte) s.charAt(i); }
    private static void le16(byte[] b, int o, int v) { b[o] = (byte) v; b[o + 1] = (byte) (v >> 8); }
    private static void le32(byte[] b, int o, int v) { le16(b, o, v); le16(b, o + 2, v >> 16); }
}
