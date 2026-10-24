package com.pantheon3d.jellykeep;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.HashMap;

/**
 * Immediate-mode UI glue for the single Canvas: whatever is drawn this frame registers its touch rect with an
 * action id, and touches are matched against the last frame drawn. Also holds the kit-bitmap caches (buttons and
 * panels are pre-rendered per size) and a word wrapper for the Lilita text.
 */
final class Ui {
    private final Art art;
    float u = 10;

    // ---------------------------------------------------------------- hit registry
    private static final int MAX = 160;
    private final RectF[] rects = new RectF[MAX];
    private final int[] ids = new int[MAX];
    private int n, from;
    { for (int i = 0; i < MAX; i++) rects[i] = new RectF(); }

    Ui(Art art) { this.art = art; }

    void begin() { n = 0; from = 0; }

    /** Everything registered after this sits on top and is the only thing touchable (a modal opened). */
    void layer() { from = n; }

    void add(int id, RectF r) { add(id, r.left, r.top, r.right, r.bottom); }

    void add(int id, float l, float t, float r, float b) {
        if (n >= MAX || id == 0) return;
        rects[n].set(l, t, r, b); ids[n++] = id;
    }

    /** Topmost action under (x, y) in the active layer, or 0. Rects get a small slop so thumbs don't miss. */
    int at(float x, float y) {
        float s = 0.8f * u;
        for (int i = n - 1; i >= from; i--) {
            RectF r = rects[i];
            if (x > r.left - s && x < r.right + s && y > r.top - s && y < r.bottom + s) return ids[i];
        }
        return 0;
    }

    /** Where id was drawn last frame (in the touchable layer), or null. For tests and hints. */
    RectF rectOf(int id) {
        for (int i = n - 1; i >= from; i--) if (ids[i] == id) return rects[i];
        return null;
    }

    boolean contains(int id, float x, float y) {
        float s = 2.5f * u;
        for (int i = n - 1; i >= from; i--) {
            if (ids[i] != id) continue;
            RectF r = rects[i];
            if (x > r.left - s && x < r.right + s && y > r.top - s && y < r.bottom + s) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- press state
    int down;                 // id under the finger right now (0 = none)
    private int flashId;
    private float flashT;     // a short pressed look for actions that fire on touch-down

    void flash(int id) { flashId = id; flashT = 0.13f; }

    void tick(float dt) { flashT -= dt; }

    boolean pressed(int id) { return id != 0 && (down == id || (flashId == id && flashT > 0)); }

    /** How far the face of a pressed button has sunk (labels follow it). */
    float off(int id, RectF r) { return pressed(id) ? r.height() * 0.11f : 0; }

    // ---------------------------------------------------------------- kit bitmaps
    private final HashMap<String, Bitmap[]> btn = new HashMap<>();
    private final HashMap<String, Bitmap> pan = new HashMap<>();

    void clear() { btn.clear(); pan.clear(); }

    Bitmap[] buttonBmp(float w, float h, int style) {
        String k = (int) w + "x" + (int) h + "s" + style;
        Bitmap[] b = btn.get(k);
        if (b == null) { b = art.button(w, h, style); btn.put(k, b); }
        return b;
    }

    Bitmap panelBmp(float w, float h) {
        String k = (int) w + "x" + (int) h;
        Bitmap b = pan.get(k);
        if (b == null) { b = art.panel(w, h, true, 3); pan.put(k, b); }
        return b;
    }

    /** Kit button (GOLD/WOOD) at r: draws its up or pressed face and registers the touch. */
    void button(Canvas c, int id, RectF r, int style) { button(c, id, r, style, null); }

    void button(Canvas c, int id, RectF r, int style, Paint paint) {
        face(c, id, r, style, paint);
        add(id, r);
    }

    /** Draws a button without registering it (scrolling lists register only the visible part themselves). */
    void face(Canvas c, int id, RectF r, int style, Paint paint) {
        Bitmap[] b = buttonBmp(r.width(), r.height(), style);
        c.drawBitmap(b[pressed(id) ? 1 : 0], r.left - 1.2f * u, r.top - 1.2f * u, paint);
    }

    void panel(Canvas c, RectF r) { c.drawBitmap(panelBmp(r.width(), r.height()), r.left - 1.6f * u, r.top - 1.6f * u, null); }

    // ---------------------------------------------------------------- text
    private final String[] lines = new String[12];

    /** Word-wraps s into at most 12 lines no wider than maxW at the given size; returns the line count. */
    int wrap(String s, float maxW, float size) {
        art.text.setTextSize(size);
        int count = 0;
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (art.text.measureText(test) <= maxW || line.length() == 0) { line.setLength(0); line.append(test); }
            else { if (count < lines.length) lines[count++] = line.toString(); line.setLength(0); line.append(word); }
        }
        if (line.length() > 0 && count < lines.length) lines[count++] = line.toString();
        return count;
    }

    /** Wrapped plain text (ink on parchment). Returns the y just below the last line. */
    float ink(Canvas c, String s, float x, float y, float maxW, float size, int col, Paint.Align align) {
        int k = wrap(s, maxW, size);
        for (int i = 0; i < k; i++) art.ink(c, lines[i], x, y + i * size * 1.18f, size, col, align);
        return y + (k - 1) * size * 1.18f + size * 0.4f;
    }

    /** Wrapped outlined text (labels over art). Returns the y just below the last line. */
    float label(Canvas c, String s, float x, float y, float maxW, float size, int col, Paint.Align align) {
        int k = wrap(s, maxW, size);
        for (int i = 0; i < k; i++) art.label(c, lines[i], x, y + i * size * 1.15f, size, col, align);
        return y + (k - 1) * size * 1.15f + size * 0.4f;
    }

    /** The largest size <= max at which s fits in one line of width w. */
    float fit(String s, float w, float max) {
        float sz = max;
        while (sz > max * 0.55f && art.width(s, sz) > w) sz *= 0.93f;
        return sz;
    }
}
