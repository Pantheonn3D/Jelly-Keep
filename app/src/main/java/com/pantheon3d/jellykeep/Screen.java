package com.pantheon3d.jellykeep;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

import static com.pantheon3d.jellykeep.Art.*;

/**
 * A modal layer over the battle (or the title over everything). Draws itself each frame, registering its buttons
 * with Ui; taps arrive as action ids on finger-up so lists can scroll. Most screens pause the battle.
 */
abstract class Screen {
    static final int CLOSE = 99;
    static final float OPEN_S = 0.22f, CLOSE_S = 0.14f;

    final Game g;
    final Ui ui;
    final Art art;
    float t, out = -1, scroll, scrollMax, vel;
    final RectF box = new RectF(), view = new RectF(), tmp = new RectF(), tmp2 = new RectF();

    Screen(Game g) { this.g = g; this.ui = g.ui; this.art = g.art; }

    float u() { return g.u; }

    boolean pauses() { return true; }

    boolean dims() { return true; }

    /** Back and taps outside the box close it. Decisions (defeat, offers) override this. */
    boolean dismissable() { return true; }

    abstract void draw(Canvas c);

    void tap(int id) { if (id == CLOSE) close(); }

    void back() { if (dismissable()) close(); }

    void tapOutside(float x, float y) { if (dismissable() && !box.contains(x, y)) close(); }

    void close() {
        if (out >= 0) return;
        out = 0;
        g.sfx.bank("panel_close", 0.45f, 0, 50);
    }

    boolean gone() { return out >= CLOSE_S; }

    void update(float dt) {
        t += dt;
        if (out >= 0) out += dt;
        if (Math.abs(vel) > 1) {                                   // fling
            scroll = Math.max(0, Math.min(scrollMax, scroll + vel * dt));
            vel *= Math.max(0, 1 - dt * 5);
        }
    }

    void scrollBy(float dy) { scroll = Math.max(0, Math.min(scrollMax, scroll + dy)); }

    /** 0..1 open amount with a little overshoot, then back down while closing. */
    float pop() {
        if (out >= 0) return 1 - Math.min(1, out / CLOSE_S);
        return t >= OPEN_S ? 1 : Game.easeOutBack(t / OPEN_S);
    }

    /** Dim alpha for the backdrop. */
    float dimA() { return out >= 0 ? 1 - Math.min(1, out / CLOSE_S) : Math.min(1, t / 0.15f); }

    /**
     * Standard framed panel: parchment box centred at cy with a title plank and a close button, scaled in with pop().
     * Call end(c) after drawing the contents. Returns the inner content rect.
     */
    RectF begin(Canvas c, String title, float w, float h, float cy, boolean closeBtn) {
        float u = u();
        box.set(g.W / 2 - w / 2, cy - h / 2, g.W / 2 + w / 2, cy + h / 2);
        float k = 0.85f + 0.15f * pop();
        c.save();
        c.scale(k, k, box.centerX(), box.centerY());
        ui.panel(c, box);
        if (title != null) {
            float pw = Math.max(44 * u, art.width(title, 6 * u) + 18 * u);
            Bitmap plank = g.plankBmp(pw, 11 * u);
            c.drawBitmap(plank, g.W / 2 - plank.getWidth() / 2f, box.top - 7.5f * u, null);
            art.label(c, title, g.W / 2, box.top + 1.8f * u, ui.fit(title, pw - 10 * u, 6 * u), A6);
        }
        if (closeBtn) closeButton(c, box.right - 9 * u, box.top - 3.5f * u);
        view.set(box.left + 4 * u, box.top + 7.5f * u, box.right - 4 * u, box.bottom - 4 * u);
        return view;
    }

    void end(Canvas c) { c.restore(); }

    void closeButton(Canvas c, float l, float t) {
        float u = u(), d = 10 * u;
        tmp2.set(l, t, l + d, t + d);
        Bitmap b = g.roundBmp(d);
        float sink = ui.pressed(CLOSE) ? 0.6f * u : 0;
        c.drawBitmap(b, l - 1.2f * u, t - 1.2f * u + sink, null);
        if (art.icClose != null) art.icon(c, art.icClose, tmp2.centerX(), tmp2.centerY() - 0.2f * u + sink, d * 0.55f);
        else art.label(c, "×", tmp2.centerX(), tmp2.centerY() + 2 * u + sink, 6 * u, A7);
        ui.add(CLOSE, tmp2);
    }

    /** Big gold or wood action button with a centred label; returns the label baseline used. */
    void action(Canvas c, int id, RectF r, int style, String label, boolean ad) { action(c, id, r, style, label, ad, false); }

    /** clipped: inside a scrolling list, so only the visible part of the button is touchable. */
    void action(Canvas c, int id, RectF r, int style, String label, boolean ad, boolean clipped) {
        float u = u();
        boolean dim = style == DIM;
        ui.face(c, id, r, dim ? WOOD : style, dim ? g.dimPaint : null);
        if (clipped) addClipped(id, r); else ui.add(id, r);
        float off = ui.off(id, r);
        float size = ui.fit(label, r.width() - (ad ? 12 : 4) * u, Math.min(5.4f * u, r.height() * 0.42f));
        float cx = r.centerX() + (ad ? 3 * u : 0);
        if (ad) g.drawAdIcon(c, cx - art.width(label, size) / 2 - 4 * u, r.centerY() - 0.6f * u + off, Math.min(2.2f * u, r.height() * 0.16f));
        art.label(c, label, cx, r.centerY() + size * 0.36f + off, size, A7);
    }

    /** "gems: 640" right-aligned under the close button; callers start their list 6u lower to make room. */
    void gemBalance(Canvas c) {
        float u = u();
        String gs = Game.fmt(g.meta.gems);
        float x = box.right - 5 * u, y = box.top + 11.4f * u, w = art.width(gs, 4 * u);
        art.label(c, gs, x, y, 4 * u, A7, android.graphics.Paint.Align.RIGHT);
        art.gem(c, x - w - 3 * u, y - 1.4f * u, 5.4f * u);
    }

    /** Registers a button only where it is visible inside the scrolling view. */
    void addClipped(int id, RectF r) {
        float t = Math.max(r.top, view.top), b = Math.min(r.bottom, view.bottom);
        if (b > t) ui.add(id, r.left, t, r.right, b);
    }
}
