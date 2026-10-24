package com.pantheon3d.jellykeep;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * Hand-built "wooden game UI kit": panels, plank grain, parchment, gold buttons with a 3D lip,
 * resource pills, bars, tower pads and outlined display text. Two hue families only:
 * AMBER for everything friendly, INDIGO for the night and the enemies.
 */
final class Art {
    // amber ramp
    static final int A0 = 0xFF2B1406, A1 = 0xFF4A2410, A2 = 0xFF7A3E16, A3 = 0xFFB5652A, A4 = 0xFFE08A2E,
            A5 = 0xFFFFB13B, A6 = 0xFFFFD25A, A7 = 0xFFFFEBD2, LIP = 0xFFB3600F;
    // indigo ramp
    static final int I0 = 0xFF0B0918, I1 = 0xFF15112E, I2 = 0xFF221B4A, I3 = 0xFF3A2F78, I4 = 0xFF5B4BC4,
            I5 = 0xFF9C8CFF, I6 = 0xFFD6CEFF;

    final Typeface font;
    final Bitmap popper, mortar, wall;
    /** Painted sprites; any can be null, in which case the coded fallback is drawn. */
    final Bitmap gloom, skitter, brute, boss, stump, coinB, pebble, gooBall, puddle, chest, chestOpen, ground;
    final Bitmap king, kingHappy, kingHurt;
    final Bitmap[] towers = new Bitmap[10];
    final Bitmap flapper, shieldE, splitter, healer, fxPoof, fxBurst, fxArrow, uiPlus, uiAd, uiLock;
    Bitmap tower(int t) { return towers[t]; }

    /** Painted UI kit (null = fall back to the coded kit). */
    final Bitmap uiPanel, uiGold, uiPlank, uiSlot, uiRibbon, uiRound, uiBadge, icShield, icClose, icPlay, icMute, icSound, icStrike;
    /** 2.0 icons and props. Not painted yet: each draws a coded glyph until its asset exists (see docs/art-and-audio-requests.md). */
    final Bitmap icGem, icJelly, icQuests, icShop, icRebirth, icGear, icFlame, icBubble, icCheck, icMusic, icHaptics, icBell,
            icPouch, icShake, icGift, courier, logo;
    private final Paint np = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG), npDark = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG),
            npShadow = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    { npDark.setColorFilter(new android.graphics.LightingColorFilter(0xFFC8C0B8, 0)); npShadow.setColorFilter(new android.graphics.PorterDuffColorFilter(0xFF000000, android.graphics.PorterDuff.Mode.SRC_IN)); npShadow.setAlpha(95); }

    /**
     * Draws src stretched into dst with fixed borders (9-slice). l,t,r,b are the border widths in source pixels;
     * k is the on-screen scale of those borders. Pass t=b=0 for a horizontal 3-slice.
     */
    void nine(Canvas c, Bitmap src, RectF dst, int l, int t, int r, int b, float k, Paint p) {
        int sw = src.getWidth(), sh = src.getHeight();
        float L = l * k, R = r * k, T = t * k, B = b * k;
        if (t == 0 && b == 0) { T = 0; B = 0; }
        if (L + R > dst.width()) { float f = dst.width() / (L + R); L *= f; R *= f; }
        if (T + B > dst.height()) { float f = dst.height() / (T + B); T *= f; B *= f; }
        int[] sx = {0, l, sw - r, sw}, sy = {0, t, sh - b, sh};
        float[] dx = {dst.left, dst.left + L, dst.right - R, dst.right}, dy = {dst.top, dst.top + T, dst.bottom - B, dst.bottom};
        android.graphics.Rect s0 = new android.graphics.Rect(); RectF d0 = new RectF();
        for (int j = 0; j < 3; j++) for (int i = 0; i < 3; i++) {
            if (sx[i + 1] <= sx[i] || sy[j + 1] <= sy[j] || dx[i + 1] <= dx[i] || dy[j + 1] <= dy[j]) continue;
            s0.set(sx[i], sy[j], sx[i + 1], sy[j + 1]); d0.set(dx[i], dy[j], dx[i + 1] + 0.5f, dy[j + 1] + 0.5f);
            c.drawBitmap(src, s0, d0, p);
        }
    }

    /** Draws a bitmap fitted inside a box (keeps aspect), centred. */
    void icon(Canvas c, Bitmap b, float cx, float cy, float size) {
        float k = size / Math.max(b.getWidth(), b.getHeight());
        r.set(cx - b.getWidth() * k / 2, cy - b.getHeight() * k / 2, cx + b.getWidth() * k / 2, cy + b.getHeight() * k / 2);
        c.drawBitmap(b, null, r, np);
    }

    /** Background with the six stumps painted in. STUMPS = x, y, w, h in that image's pixels (1438x2576). */
    final Bitmap groundBaked;
    static final int[][] STUMPS = {{246, 907, 226, 204}, {606, 637, 226, 204}, {966, 907, 226, 204}, {966, 1389, 226, 204}, {606, 1695, 226, 204}, {246, 1389, 226, 204}};
    /** Where the king stands in the baked image (centre of the stump ring). */
    static final float KING_GX = 719, KING_GY = 1195;
    private final Paint bp = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

    Bitmap enemy(int type) {
        switch (type) {
            case 1: return skitter; case 2: return brute; case 3: return boss; case 4: return flapper;
            case 5: return shieldE; case 6: return splitter; case 7: return healer; default: return gloom;
        }
    }

    private static Bitmap opt(Context ctx, String name) {
        try (InputStream in = ctx.getAssets().open(name)) { return BitmapFactory.decodeStream(in); }
        catch (IOException e) { return null; }
    }
    float u = 10;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG), s = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path path = new Path();

    Art(Context ctx) {
        font = Typeface.createFromAsset(ctx.getAssets(), "lilita.ttf");
        text.setTypeface(font);
        text.setTextAlign(Paint.Align.CENTER);
        s.setStyle(Paint.Style.STROKE);
        s.setStrokeJoin(Paint.Join.ROUND);
        s.setStrokeCap(Paint.Cap.ROUND);
        popper = load(ctx, "popper.webp");
        gloom = opt(ctx, "gloom.webp"); skitter = opt(ctx, "skitter.webp"); brute = opt(ctx, "brute.webp"); boss = opt(ctx, "boss.webp");
        stump = opt(ctx, "stump.webp"); coinB = opt(ctx, "coin.webp"); pebble = opt(ctx, "pebble.webp"); gooBall = opt(ctx, "goo_ball.webp");
        groundBaked = opt(ctx, "ground_stumps.webp");
        String[] tn = {"popper", "wall", "mortar", "t_sparkler", "t_sniper", "t_mage", "t_bomb", "t_banker", "t_drummer", "t_lantern"};
        for (int i = 0; i < 10; i++) towers[i] = opt(ctx, tn[i] + ".webp");
        flapper = opt(ctx, "e_flapper.webp"); shieldE = opt(ctx, "e_shield.webp"); splitter = opt(ctx, "e_splitter.webp"); healer = opt(ctx, "e_healer.webp");
        fxPoof = opt(ctx, "fx_poof.webp"); fxBurst = opt(ctx, "fx_burst.webp"); fxArrow = opt(ctx, "fx_arrow.webp");
        uiPlus = opt(ctx, "ui_plus.webp"); uiAd = opt(ctx, "ui_adtoken.webp"); uiLock = opt(ctx, "ui_lock.webp");
        king = opt(ctx, "king.webp"); kingHappy = opt(ctx, "king_happy.webp"); kingHurt = opt(ctx, "king_hurt.webp");
        uiPanel = opt(ctx, "ui_panel.webp"); uiGold = opt(ctx, "ui_btn_gold.webp"); uiPlank = opt(ctx, "ui_plank.webp"); uiSlot = opt(ctx, "ui_slot.webp");
        uiRibbon = opt(ctx, "ui_ribbon.webp"); uiRound = opt(ctx, "ui_round.webp"); uiBadge = opt(ctx, "ui_badge.webp");
        icShield = opt(ctx, "ui_ic_shield.webp"); icClose = opt(ctx, "ui_ic_close.webp"); icPlay = opt(ctx, "ui_ic_play.webp");
        icMute = opt(ctx, "ui_ic_mute.webp"); icSound = opt(ctx, "ui_ic_sound.webp"); icStrike = opt(ctx, "ui_ic_strike.webp");
        puddle = opt(ctx, "puddle.webp"); chest = opt(ctx, "chest.webp"); chestOpen = opt(ctx, "chest_open.webp");
        ground = groundBaked == null ? opt(ctx, "ground.webp") : null;        // the baked one replaces it (saves ~8 MB)
        mortar = load(ctx, "mortar.webp");
        wall = load(ctx, "wall.webp");
        icGem = opt(ctx, "ui_ic_gem.webp"); icJelly = opt(ctx, "ui_ic_jelly.webp"); icQuests = opt(ctx, "ui_ic_quests.webp");
        icShop = opt(ctx, "ui_ic_shop.webp"); icRebirth = opt(ctx, "ui_ic_rebirth.webp"); icGear = opt(ctx, "ui_ic_gear.webp");
        icFlame = opt(ctx, "ui_ic_flame.webp"); icBubble = opt(ctx, "ui_ic_bubble.webp"); icCheck = opt(ctx, "ui_ic_check.webp");
        icMusic = opt(ctx, "ui_ic_music.webp"); icHaptics = opt(ctx, "ui_ic_haptics.webp"); icBell = opt(ctx, "ui_ic_bell.webp");
        icPouch = opt(ctx, "ui_ic_pouch.webp"); icShake = opt(ctx, "ui_ic_shake.webp"); icGift = opt(ctx, "ui_ic_gift.webp");
        courier = opt(ctx, "courier.webp"); logo = opt(ctx, "logo.webp");
    }

    private static Bitmap load(Context ctx, String name) {
        try (InputStream in = ctx.getAssets().open(name)) { return BitmapFactory.decodeStream(in); }
        catch (IOException e) { return Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888); }
    }

    // ================================================================= text

    /** Chunky game text: drop shadow, thick dark outline, bright fill. */
    void label(Canvas c, String str, float x, float y, float size, int fill, Paint.Align align) {
        text.setTextAlign(align);
        text.setTextSize(size);
        text.setStyle(Paint.Style.STROKE);
        text.setStrokeJoin(Paint.Join.ROUND);
        text.setStrokeWidth(size * 0.2f);
        text.setColor(A0);
        c.drawText(str, x, y + size * 0.08f, text);
        c.drawText(str, x, y, text);
        text.setStyle(Paint.Style.FILL);
        text.setColor(fill);
        c.drawText(str, x, y, text);
        text.setTextAlign(Paint.Align.CENTER);
    }

    void label(Canvas c, String str, float x, float y, float size, int fill) { label(c, str, x, y, size, fill, Paint.Align.CENTER); }

    /** label() faded by alpha, without an offscreen layer (cheap enough for many floating numbers). */
    void label(Canvas c, String str, float x, float y, float size, int fill, Paint.Align align, int alpha) {
        if (alpha >= 255) { label(c, str, x, y, size, fill, align); return; }
        if (alpha <= 0) return;
        text.setTextAlign(align);
        text.setTextSize(size);
        text.setStyle(Paint.Style.STROKE);
        text.setStrokeJoin(Paint.Join.ROUND);
        text.setStrokeWidth(size * 0.2f);
        text.setColor(A0); text.setAlpha(alpha);
        c.drawText(str, x, y + size * 0.08f, text);
        c.drawText(str, x, y, text);
        text.setStyle(Paint.Style.FILL);
        text.setColor(fill); text.setAlpha(alpha);
        c.drawText(str, x, y, text);
        text.setAlpha(255);
        text.setTextAlign(Paint.Align.CENTER);
    }

    /** Plain dark text for parchment surfaces. */
    void ink(Canvas c, String str, float x, float y, float size, int col, Paint.Align align) {
        text.setTextAlign(align);
        text.setStyle(Paint.Style.FILL);
        text.setTextSize(size);
        text.setColor(col);
        c.drawText(str, x, y, text);
        text.setTextAlign(Paint.Align.CENTER);
    }

    float width(String str, float size) { text.setTextSize(size); return text.measureText(str); }

    // ================================================================= wood

    private void grain(Canvas c, RectF b, long seed, int dark, int light) {
        Random rnd = new Random(seed);
        float plank = 6.5f * u;
        int planks = Math.max(1, Math.round(b.height() / plank));
        float ph = b.height() / planks;
        for (int k = 0; k < planks; k++) {
            float top = b.top + k * ph;
            // per-plank tone
            p.setShader(null);
            p.setColor(k % 2 == 0 ? light : dark);
            p.setAlpha(28);
            c.drawRect(b.left, top, b.right, top + ph, p);
            // grain lines
            int lines = (int) (ph / (0.75f * u));
            for (int i = 0; i < lines; i++) {
                float y = top + (i + 0.5f) * ph / lines + (rnd.nextFloat() - 0.5f) * 0.4f * u;
                path.reset();
                path.moveTo(b.left, y);
                float amp = (0.15f + rnd.nextFloat() * 0.35f) * u, freq = 0.02f + rnd.nextFloat() * 0.03f, phase = rnd.nextFloat() * 6f;
                for (float x = b.left; x <= b.right + 4; x += u) {
                    path.lineTo(x, y + (float) Math.sin(x / u * freq * 6 + phase) * amp);
                }
                s.setShader(null);
                s.setColor(dark);
                s.setAlpha(40 + rnd.nextInt(50));
                s.setStrokeWidth((0.12f + rnd.nextFloat() * 0.25f) * u);
                c.drawPath(path, s);
            }
            // a knot now and then
            if (rnd.nextFloat() < 0.6f) {
                float kx = b.left + rnd.nextFloat() * b.width(), ky = top + ph * (0.3f + rnd.nextFloat() * 0.4f);
                s.setColor(dark);
                for (int ring = 0; ring < 3; ring++) {
                    s.setAlpha(70 - ring * 18);
                    s.setStrokeWidth(0.25f * u);
                    r.set(kx - (1.6f + ring) * u, ky - (0.5f + ring * 0.35f) * u, kx + (1.6f + ring) * u, ky + (0.5f + ring * 0.35f) * u);
                    c.drawOval(r, s);
                }
            }
            // seam: dark line, light line under it
            if (k > 0) {
                s.setStrokeWidth(0.45f * u); s.setColor(A0); s.setAlpha(150);
                c.drawLine(b.left, top, b.right, top, s);
                s.setStrokeWidth(0.3f * u); s.setColor(A5); s.setAlpha(60);
                c.drawLine(b.left, top + 0.45f * u, b.right, top + 0.45f * u, s);
            }
        }
    }

    /** Wooden board with plank grain, bevel, outline, rivets and a soft drop shadow. */
    Bitmap panel(float w, float h, boolean parchment, long seed) {
        float pad = 1.6f * u;
        if (uiPanel != null) {
            Bitmap bm = Bitmap.createBitmap(Math.max(1, (int) (w + pad * 2)), Math.max(1, (int) (h + pad * 2)), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            float k = Math.min(4.2f * u / 130f, Math.min(w, h) * 0.22f / 130f);   // frame ~4u thick
            r.set(pad, pad + 0.9f * u, pad + w, pad + h + 0.9f * u); nine(c, uiPanel, r, 130, 130, 130, 130, k, npShadow);
            r.set(pad, pad, pad + w, pad + h); nine(c, uiPanel, r, 130, 130, 130, 130, k, np);
            return bm;
        }
        Bitmap bm = Bitmap.createBitmap(Math.max(1, (int) (w + pad * 2)), Math.max(1, (int) (h + pad * 2)), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        c.translate(pad, pad);
        float rad = 3.2f * u;
        // drop shadow
        p.setShader(null); p.setColor(0xFF000000); p.setAlpha(110);
        r.set(0, 1.2f * u, w, h + 1.2f * u); c.drawRoundRect(r, rad, rad, p);
        // body
        r.set(0, 0, w, h);
        p.setColor(0xFFFFFFFF); p.setShader(new LinearGradient(0, 0, 0, h, A3, A2, Shader.TileMode.CLAMP));
        p.setAlpha(255);
        c.drawRoundRect(r, rad, rad, p);
        p.setShader(null);
        c.save();
        path.reset(); path.addRoundRect(r, rad, rad, Path.Direction.CW); c.clipPath(path);
        grain(c, new RectF(0, 0, w, h), seed, A1, A5);
        c.restore();
        // bevel: light inner top edge, dark inner bottom edge
        s.setShader(null);
        s.setStrokeWidth(0.7f * u); s.setColor(A5); s.setAlpha(150);
        r.set(0.9f * u, 0.9f * u, w - 0.9f * u, h - 0.9f * u);
        c.save(); c.clipRect(0, 0, w, h * 0.5f); c.drawRoundRect(r, rad * 0.8f, rad * 0.8f, s); c.restore();
        s.setColor(A0); s.setAlpha(120);
        c.save(); c.clipRect(0, h * 0.5f, w, h); c.drawRoundRect(r, rad * 0.8f, rad * 0.8f, s); c.restore();
        // outline
        s.setColor(A0); s.setAlpha(255); s.setStrokeWidth(0.8f * u);
        r.set(0, 0, w, h); c.drawRoundRect(r, rad, rad, s);

        if (parchment) {
            float in = 2.6f * u;
            RectF pr = new RectF(in, in, w - in, h - in);
            float pr2 = 2f * u;
            // recess shadow
            p.setColor(A0); p.setAlpha(140);
            r.set(pr.left, pr.top - 0.3f * u, pr.right, pr.bottom + 0.3f * u); c.drawRoundRect(r, pr2, pr2, p);
            p.setColor(0xFFFFFFFF); p.setShader(new LinearGradient(0, pr.top, 0, pr.bottom, 0xFFFFE9C0, 0xFFF2C47E, Shader.TileMode.CLAMP));
            p.setAlpha(255);
            c.drawRoundRect(pr, pr2, pr2, p);
            p.setShader(null);
            // stains and edge burn
            c.save(); path.reset(); path.addRoundRect(pr, pr2, pr2, Path.Direction.CW); c.clipPath(path);
            Random rnd = new Random(seed * 31);
            for (int i = 0; i < 9; i++) {
                p.setColor(0xFFC98A45); p.setAlpha(18 + rnd.nextInt(20));
                float cx = pr.left + rnd.nextFloat() * pr.width(), cy = pr.top + rnd.nextFloat() * pr.height(), rr = (2 + rnd.nextFloat() * 6) * u;
                c.drawCircle(cx, cy, rr, p);
            }
            s.setColor(0xFFB8732F); s.setAlpha(90); s.setStrokeWidth(1.6f * u);
            c.drawRoundRect(pr, pr2, pr2, s);
            s.setColor(A1); s.setAlpha(60); s.setStrokeWidth(0.9f * u);
            c.drawLine(pr.left, pr.top + 0.4f * u, pr.right, pr.top + 0.4f * u, s); // inner shadow under top lip
            c.restore();
            s.setColor(A1); s.setAlpha(255); s.setStrokeWidth(0.45f * u);
            c.drawRoundRect(pr, pr2, pr2, s);
        }
        // brass rivets
        float rv = 0.75f * u, ri = 1.5f * u;
        rivet(c, ri, ri, rv); rivet(c, w - ri, ri, rv); rivet(c, ri, h - ri, rv); rivet(c, w - ri, h - ri, rv);
        return bm;
    }

    void rivet(Canvas c, float x, float y, float rr) {
        p.setColor(0xFFFFFFFF); p.setShader(new RadialGradient(x - rr * 0.3f, y - rr * 0.3f, rr * 1.2f, A7, A5, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, rr, p);
        p.setShader(null);
        s.setColor(A0); s.setAlpha(255); s.setStrokeWidth(rr * 0.35f);
        c.drawCircle(x, y, rr, s);
    }

    /** Wooden sign plank (title boards: "WAVE 12", "DEFEAT"). */
    Bitmap plank(float w, float h, long seed) {
        float pad = 1.4f * u;
        if (uiPlank != null) {
            Bitmap bm = Bitmap.createBitmap((int) (w + pad * 2), (int) (h + pad * 2), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            float k = h / uiPlank.getHeight();
            r.set(pad, pad + 0.7f * u, pad + w, pad + h + 0.7f * u); nine(c, uiPlank, r, 75, 0, 75, 0, k, npShadow);
            r.set(pad, pad, pad + w, pad + h); nine(c, uiPlank, r, 75, 0, 75, 0, k, np);
            return bm;
        }
        Bitmap bm = Bitmap.createBitmap((int) (w + pad * 2), (int) (h + pad * 2), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        c.translate(pad, pad);
        float rad = h * 0.32f;
        p.setShader(null); p.setColor(0xFF000000); p.setAlpha(110);
        r.set(0, u, w, h + u); c.drawRoundRect(r, rad, rad, p);
        r.set(0, 0, w, h);
        p.setColor(0xFFFFFFFF); p.setShader(new LinearGradient(0, 0, 0, h, A4, A2, Shader.TileMode.CLAMP)); p.setAlpha(255);
        c.drawRoundRect(r, rad, rad, p); p.setShader(null);
        c.save(); path.reset(); path.addRoundRect(r, rad, rad, Path.Direction.CW); c.clipPath(path);
        RectF g = new RectF(0, 0, w, h);
        Random rnd = new Random(seed);
        for (int i = 0; i < (int) (h / (0.8f * u)); i++) {
            float y = (i + 0.5f) * 0.8f * u;
            s.setColor(A1); s.setAlpha(35 + rnd.nextInt(40)); s.setStrokeWidth((0.12f + rnd.nextFloat() * 0.2f) * u);
            path.reset(); path.moveTo(0, y);
            float ph = rnd.nextFloat() * 6;
            for (float x = 0; x <= w + 4; x += u) path.lineTo(x, y + (float) Math.sin(x / u * 0.15f + ph) * 0.35f * u);
            c.drawPath(path, s);
        }
        p.setColor(0xFFFFFFFF); p.setAlpha(45);
        r.set(0, 0, w, h * 0.42f); c.drawRect(r, p);
        c.restore();
        s.setColor(A0); s.setAlpha(255); s.setStrokeWidth(0.8f * u);
        r.set(0, 0, w, h); c.drawRoundRect(r, rad, rad, s);
        return bm;
    }

    // ================================================================= buttons

    static final int GOLD = 0, WOOD = 1, DIM = 2, PLANK = 3;   // WOOD picks plank or framed card by shape; PLANK is always a plank

    /** Pill/rounded button with a 3D lip. Returns [up, down] bitmaps; the face drops onto the lip when pressed. */
    Bitmap[] button(float w, float h, int style) {
        return new Bitmap[]{buttonState(w, h, style, false), buttonState(w, h, style, true)};
    }

    private Bitmap buttonState(float w, float h, int style, boolean down) {
        float pad = 1.2f * u, lip = h * 0.13f;
        Bitmap src = style == GOLD ? uiGold : style == PLANK || h <= w * 0.6f ? uiPlank : uiPanel;
        if (src != null) {
            Bitmap bm = Bitmap.createBitmap((int) (w + pad * 2), (int) (h + pad * 2), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            int l, rr, t, b; float k;
            if (src == uiGold) { l = rr = 155; t = b = 0; k = h / src.getHeight(); }
            else if (src == uiPlank) { l = rr = 75; t = b = 0; k = h / src.getHeight(); }
            else { l = rr = t = b = 130; k = 2.6f * u / 130f; }
            float drop = down ? h * 0.07f : 0;                       // pressed: face sinks onto its lip
            if (!down) { r.set(pad, pad + 0.6f * u, pad + w, pad + h + 0.6f * u); nine(c, src, r, l, t, rr, b, k, npShadow); }
            r.set(pad, pad + drop, pad + w, pad + h);
            nine(c, src, r, l, t, rr, b, k * (down ? 0.97f : 1), down ? npDark : (style == DIM ? npDark : np));
            return bm;
        }
        Bitmap bm = Bitmap.createBitmap((int) (w + pad * 2), (int) (h + pad * 2), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        c.translate(pad, pad);
        float rad = Math.min(h * 0.38f, 3.2f * u);
        int top, mid, bot, lipC, gloss;
        if (style == GOLD) { top = A6; mid = A5; bot = 0xFFF29A1E; lipC = LIP; gloss = 110; }
        else if (style == WOOD || style == PLANK) { top = A4; mid = A3; bot = A2; lipC = A1; gloss = 60; }
        else { top = 0xFF8C6448; mid = 0xFF6E4C36; bot = 0xFF5A3C2A; lipC = 0xFF3A2416; gloss = 30; }
        if (!down) { p.setShader(null); p.setColor(0xFF000000); p.setAlpha(100); r.set(0, lip + 0.6f * u, w, h + 0.6f * u); c.drawRoundRect(r, rad, rad, p); }
        // lip
        p.setShader(null); p.setColor(lipC); p.setAlpha(255);
        r.set(0, lip, w, h); c.drawRoundRect(r, rad, rad, p);
        s.setColor(A0); s.setAlpha(255); s.setStrokeWidth(0.7f * u);
        c.drawRoundRect(r, rad, rad, s);
        // face
        float off = down ? lip * 0.85f : 0;
        RectF f = new RectF(0, off, w, h - lip + off);
        p.setColor(0xFFFFFFFF); p.setShader(new LinearGradient(0, f.top, 0, f.bottom, new int[]{top, mid, bot}, new float[]{0, 0.5f, 1}, Shader.TileMode.CLAMP));
        c.drawRoundRect(f, rad, rad, p);
        p.setShader(null);
        if (style == WOOD) {
            c.save(); path.reset(); path.addRoundRect(f, rad, rad, Path.Direction.CW); c.clipPath(path);
            grain(c, f, (long) (w * 7 + h), A1, A5);
            c.restore();
        }
        // gloss band
        p.setColor(0xFFFFFFFF); p.setAlpha(gloss);
        r.set(f.left + 1.2f * u, f.top + 0.7f * u, f.right - 1.2f * u, f.top + f.height() * 0.42f);
        c.drawRoundRect(r, rad * 0.7f, rad * 0.7f, p);
        s.setColor(A0); s.setStrokeWidth(0.7f * u);
        c.drawRoundRect(f, rad, rad, s);
        return bm;
    }

    /** Round wooden icon button. */
    Bitmap roundButton(float d) {
        float pad = 1.2f * u;
        if (uiRound != null) {
            Bitmap bm = Bitmap.createBitmap((int) (d + pad * 2), (int) (d + pad * 2), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            r.set(pad, pad + 0.5f * u, pad + d, pad + d + 0.5f * u); c.drawBitmap(uiRound, null, r, npShadow);
            r.set(pad, pad, pad + d, pad + d); c.drawBitmap(uiRound, null, r, np);
            return bm;
        }
        Bitmap bm = Bitmap.createBitmap((int) (d + pad * 2), (int) (d + pad * 2), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        c.translate(pad, pad);
        float rr = d / 2, lip = d * 0.1f;
        p.setShader(null); p.setColor(0xFF000000); p.setAlpha(100); c.drawCircle(rr, rr + lip + 0.5f * u, rr - lip * 0.5f, p);
        p.setColor(A1); p.setAlpha(255); c.drawCircle(rr, rr + lip * 0.5f, rr - lip * 0.5f, p);
        p.setColor(0xFFFFFFFF); p.setShader(new LinearGradient(0, 0, 0, d, A4, A2, Shader.TileMode.CLAMP));
        c.drawCircle(rr, rr - lip * 0.3f, rr - lip * 0.5f, p);
        p.setShader(null);
        c.save(); path.reset(); path.addCircle(rr, rr - lip * 0.3f, rr - lip * 0.5f, Path.Direction.CW); c.clipPath(path);
        grain(c, new RectF(0, 0, d, d), 77, A1, A5);
        p.setColor(0xFFFFFFFF); p.setAlpha(55); r.set(rr * 0.35f, rr * 0.15f, rr * 1.65f, rr * 0.85f); c.drawOval(r, p);
        c.restore();
        s.setColor(A0); s.setStrokeWidth(0.7f * u);
        c.drawCircle(rr, rr - lip * 0.3f, rr - lip * 0.5f, s);
        return bm;
    }

    /** Dark recessed pill for resource counters and progress bars. */
    Bitmap recess(float w, float h) {
        float pad = 0.8f * u;
        if (uiSlot != null) {
            Bitmap bm = Bitmap.createBitmap((int) (w + pad * 2), (int) (h + pad * 2), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            r.set(pad - 0.3f * u, pad - 0.3f * u, pad + w + 0.3f * u, pad + h + 0.3f * u);
            nine(c, uiSlot, r, 53, 0, 53, 0, r.height() / uiSlot.getHeight(), np);
            return bm;
        }
        Bitmap bm = Bitmap.createBitmap((int) (w + pad * 2), (int) (h + pad * 2), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        c.translate(pad, pad);
        float rad = h / 2;
        r.set(0, 0, w, h);
        p.setColor(0xFFFFFFFF); p.setShader(new LinearGradient(0, 0, 0, h, 0xFF1E0D04, 0xFF3B1C0A, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, rad, rad, p); p.setShader(null);
        s.setColor(0xFF000000); s.setAlpha(120); s.setStrokeWidth(0.6f * u);
        c.save(); c.clipRect(0, 0, w, h * 0.45f); r.set(0.4f * u, 0.4f * u, w - 0.4f * u, h); c.drawRoundRect(r, rad, rad, s); c.restore();
        s.setColor(A3); s.setAlpha(130); s.setStrokeWidth(0.35f * u);
        c.save(); c.clipRect(0, h * 0.6f, w, h); r.set(0.3f * u, 0, w - 0.3f * u, h - 0.3f * u); c.drawRoundRect(r, rad, rad, s); c.restore();
        s.setColor(A0); s.setAlpha(255); s.setStrokeWidth(0.6f * u);
        r.set(0, 0, w, h); c.drawRoundRect(r, rad, rad, s);
        return bm;
    }

    /** Gold fill for bars, drawn live (width changes every frame). */
    void barFill(Canvas c, RectF b, float f, int hi, int lo) {
        if (f <= 0) return;
        float in = 0.45f * u, h = b.height() - in * 2, rad = h / 2;
        float w = Math.max(h, (b.width() - in * 2) * Math.min(1, f));
        r.set(b.left + in, b.top + in, b.left + in + w, b.bottom - in);
        p.setColor(0xFFFFFFFF); p.setShader(new LinearGradient(0, r.top, 0, r.bottom, hi, lo, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, rad, rad, p); p.setShader(null);
        p.setColor(0xFFFFFFFF); p.setAlpha(90);
        r.set(r.left + rad * 0.5f, r.top + h * 0.12f, r.right - rad * 0.5f, r.top + h * 0.45f);
        c.drawRoundRect(r, rad * 0.4f, rad * 0.4f, p);
    }

    // ================================================================= icons & props

    void coin(Canvas c, float x, float y, float rr) {
        if (coinB != null) { r.set(x - rr * 1.12f, y - rr * 1.12f, x + rr * 1.12f, y + rr * 1.12f); c.drawBitmap(coinB, null, r, bp); return; }
        p.setShader(null); p.setColor(A0); p.setAlpha(255); c.drawCircle(x, y + rr * 0.12f, rr * 1.06f, p);
        p.setColor(LIP); c.drawCircle(x, y + rr * 0.08f, rr, p);
        p.setColor(0xFFFFFFFF); p.setShader(new RadialGradient(x - rr * 0.3f, y - rr * 0.35f, rr * 1.3f, A7, A5, Shader.TileMode.CLAMP));
        c.drawCircle(x, y - rr * 0.04f, rr * 0.86f, p); p.setShader(null);
        s.setColor(LIP); s.setAlpha(255); s.setStrokeWidth(rr * 0.12f);
        c.drawCircle(x, y - rr * 0.04f, rr * 0.58f, s);
        p.setColor(0xFFFFFFFF); p.setAlpha(170); c.drawCircle(x - rr * 0.35f, y - rr * 0.4f, rr * 0.16f, p);
    }

    void star(Canvas c, float x, float y, float ro, int fill) {
        path.reset();
        for (int i = 0; i < 10; i++) {
            float a = (float) (i * Math.PI / 5 - Math.PI / 2), rr = i % 2 == 0 ? ro : ro * 0.48f;
            float px = x + (float) Math.cos(a) * rr, py = y + (float) Math.sin(a) * rr;
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        path.close();
        p.setShader(null); p.setColor(fill); p.setAlpha(255); c.drawPath(path, p);
        s.setColor(A0); s.setAlpha(255); s.setStrokeWidth(ro * 0.16f); c.drawPath(path, s);
    }

    // ================================================================= 2.0 icon glyphs
    // Each draws its painted asset when present, otherwise a simple outlined glyph in the kit's palette.
    // "size" is the edge of the box the icon fits in, centred on (x, y).

    private final Path path2 = new Path();

    private boolean painted(Canvas c, Bitmap b, float x, float y, float size) {
        if (b == null) return false;
        icon(c, b, x, y, size);
        return true;
    }

    private void outline(Canvas c, Path pth, float w) {
        s.setShader(null); s.setColor(A0); s.setAlpha(255); s.setStrokeWidth(w);
        c.drawPath(pth, s);
    }

    private void grad(float y0, float y1, int top, int bot) {
        p.setColor(0xFFFFFFFF); p.setAlpha(255);
        p.setShader(new LinearGradient(0, y0, 0, y1, top, bot, Shader.TileMode.CLAMP));
    }

    /** Amber gem: the premium currency. */
    void gem(Canvas c, float x, float y, float size) {
        if (painted(c, icGem, x, y, size)) return;
        float r = size * 0.46f, t = y - r * 0.62f, g = y - r * 0.12f, b = y + r * 0.9f;
        path.reset();
        path.moveTo(x - r * 0.55f, t); path.lineTo(x + r * 0.55f, t); path.lineTo(x + r, g); path.lineTo(x, b); path.lineTo(x - r, g); path.close();
        p.setShader(null); p.setColor(A5); p.setAlpha(255); c.drawPath(path, p);
        path2.reset(); path2.moveTo(x - r * 0.55f, t); path2.lineTo(x + r * 0.55f, t); path2.lineTo(x + r, g); path2.lineTo(x - r, g); path2.close();
        grad(t, g, A7, A6); c.drawPath(path2, p); p.setShader(null);
        path2.reset(); path2.moveTo(x - r, g); path2.lineTo(x, g); path2.lineTo(x, b); path2.close();
        p.setColor(A4); c.drawPath(path2, p);
        s.setColor(A2); s.setAlpha(140); s.setStrokeWidth(r * 0.07f);
        c.drawLine(x - r, g, x + r, g, s); c.drawLine(x - r * 0.2f, t, x - r * 0.45f, g, s); c.drawLine(x + r * 0.2f, t, x + r * 0.45f, g, s);
        c.drawLine(x - r * 0.45f, g, x, b, s); c.drawLine(x + r * 0.45f, g, x, b, s);
        outline(c, path, r * 0.13f);
        p.setColor(0xFFFFFFFF); p.setAlpha(230); c.drawCircle(x - r * 0.38f, y - r * 0.4f, r * 0.11f, p);
    }

    /** Royal Jelly: the rebirth currency, a golden drop. */
    void jellyDrop(Canvas c, float x, float y, float size) {
        if (painted(c, icJelly, x, y, size)) return;
        float r = size * 0.36f, cy = y + r * 0.35f;
        path.reset();
        path.moveTo(x, y - r * 1.25f);
        path.cubicTo(x + r * 0.35f, cy - r * 1.1f, x + r, cy - r * 0.55f, x + r, cy);
        path.cubicTo(x + r, cy + r * 0.62f, x + r * 0.5f, cy + r, x, cy + r);
        path.cubicTo(x - r * 0.5f, cy + r, x - r, cy + r * 0.62f, x - r, cy);
        path.cubicTo(x - r, cy - r * 0.55f, x - r * 0.35f, cy - r * 1.1f, x, y - r * 1.25f);
        path.close();
        grad(y - r, cy + r, A6, A4); c.drawPath(path, p); p.setShader(null);
        outline(c, path, r * 0.16f);
        p.setColor(A7); p.setAlpha(220);
        r2.set(x - r * 0.62f, cy - r * 0.55f, x - r * 0.2f, cy + r * 0.1f); c.drawOval(r2, p);
    }

    /** Streak flame. */
    void flame(Canvas c, float x, float y, float size) {
        if (painted(c, icFlame, x, y, size)) return;
        float r = size * 0.42f;
        flamePath(path, x, y + r * 0.1f, r);
        grad(y - r, y + r, A5, A4); c.drawPath(path, p); p.setShader(null);
        outline(c, path, r * 0.13f);
        flamePath(path2, x, y + r * 0.42f, r * 0.55f);
        p.setColor(A7); p.setAlpha(255); c.drawPath(path2, p);
    }

    private static void flamePath(Path pth, float x, float y, float r) {
        pth.reset();
        pth.moveTo(x, y - r * 1.15f);
        pth.cubicTo(x + r * 0.2f, y - r * 0.55f, x + r * 0.95f, y - r * 0.35f, x + r * 0.85f, y + r * 0.25f);
        pth.cubicTo(x + r * 0.78f, y + r * 0.75f, x + r * 0.4f, y + r, x, y + r);
        pth.cubicTo(x - r * 0.4f, y + r, x - r * 0.8f, y + r * 0.75f, x - r * 0.85f, y + r * 0.25f);
        pth.cubicTo(x - r * 0.9f, y - r * 0.2f, x - r * 0.55f, y - r * 0.45f, x - r * 0.35f, y - r * 0.75f);
        pth.cubicTo(x - r * 0.25f, y - r * 0.35f, x - r * 0.05f, y - r * 0.6f, x, y - r * 1.15f);
        pth.close();
    }

    void gear(Canvas c, float x, float y, float size) {
        if (painted(c, icGear, x, y, size)) return;
        float r = size * 0.42f;
        path.reset();
        for (int i = 0; i < 16; i++) {
            float a0 = (float) (i * Math.PI / 8), rr = i % 2 == 0 ? r : r * 0.78f;
            float a1 = a0 + (float) (Math.PI / 8);
            float px = x + (float) Math.cos(a0) * rr, py = y + (float) Math.sin(a0) * rr;
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
            path.lineTo(x + (float) Math.cos(a1 - 0.06f) * rr, y + (float) Math.sin(a1 - 0.06f) * rr);
        }
        path.close();
        grad(y - r, y + r, A6, A4); c.drawPath(path, p); p.setShader(null);
        outline(c, path, r * 0.12f);
        p.setColor(A2); c.drawCircle(x, y, r * 0.3f, p);
        s.setColor(A0); s.setStrokeWidth(r * 0.1f); c.drawCircle(x, y, r * 0.3f, s);
    }

    /** Quest scroll. */
    void scroll(Canvas c, float x, float y, float size) {
        if (painted(c, icQuests, x, y, size)) return;
        float r = size * 0.42f;
        r2.set(x - r * 0.62f, y - r * 0.7f, x + r * 0.62f, y + r * 0.7f);
        grad(r2.top, r2.bottom, A7, 0xFFF2C47E); c.drawRect(r2, p); p.setShader(null);
        s.setColor(A0); s.setStrokeWidth(r * 0.1f); c.drawRect(r2, s);
        s.setColor(A3); s.setStrokeWidth(r * 0.09f);
        for (int k = 0; k < 3; k++) { float ly = y - r * 0.3f + k * r * 0.3f; c.drawLine(x - r * 0.38f, ly, x + r * (k == 2 ? 0.1f : 0.38f), ly, s); }
        for (int e = -1; e <= 1; e += 2) {
            r2.set(x - r * 0.82f, y + e * r * 0.7f - r * 0.2f, x + r * 0.82f, y + e * r * 0.7f + r * 0.2f);
            p.setColor(A4); c.drawRoundRect(r2, r * 0.2f, r * 0.2f, p);
            s.setColor(A0); s.setStrokeWidth(r * 0.1f); c.drawRoundRect(r2, r * 0.2f, r * 0.2f, s);
        }
    }

    /** Shop bag. */
    void bag(Canvas c, float x, float y, float size) {
        if (painted(c, icShop, x, y, size)) return;
        float r = size * 0.42f;
        s.setColor(A0); s.setStrokeWidth(r * 0.22f);
        r2.set(x - r * 0.38f, y - r * 0.95f, x + r * 0.38f, y - r * 0.2f); c.drawArc(r2, 180, 180, false, s);
        s.setColor(A5); s.setStrokeWidth(r * 0.1f); c.drawArc(r2, 180, 180, false, s);
        path.reset();
        path.moveTo(x - r * 0.7f, y - r * 0.5f); path.lineTo(x + r * 0.7f, y - r * 0.5f);
        path.lineTo(x + r * 0.85f, y + r * 0.85f); path.lineTo(x - r * 0.85f, y + r * 0.85f); path.close();
        grad(y - r * 0.5f, y + r * 0.85f, A5, A3); c.drawPath(path, p); p.setShader(null);
        outline(c, path, r * 0.12f);
        gem(c, x, y + r * 0.2f, r * 0.9f);
    }

    /** Crown (rebirth). */
    void crown(Canvas c, float x, float y, float size) {
        if (painted(c, icRebirth, x, y, size)) return;
        float r = size * 0.45f;
        path.reset();
        path.moveTo(x - r * 0.8f, y + r * 0.55f); path.lineTo(x - r * 0.95f, y - r * 0.45f); path.lineTo(x - r * 0.45f, y);
        path.lineTo(x, y - r * 0.8f); path.lineTo(x + r * 0.45f, y); path.lineTo(x + r * 0.95f, y - r * 0.45f);
        path.lineTo(x + r * 0.8f, y + r * 0.55f); path.close();
        grad(y - r * 0.8f, y + r * 0.55f, A7, A5); c.drawPath(path, p); p.setShader(null);
        outline(c, path, r * 0.12f);
        for (int k = -1; k <= 1; k++) {
            p.setColor(A0); c.drawCircle(x + k * r * 0.45f, y + r * 0.25f, r * 0.12f, p);
            p.setColor(A4); c.drawCircle(x + k * r * 0.45f, y + r * 0.25f, r * 0.08f, p);
        }
    }

    void check(Canvas c, float x, float y, float size) {
        if (painted(c, icCheck, x, y, size)) return;
        float r = size * 0.42f;
        path.reset(); path.moveTo(x - r * 0.65f, y); path.lineTo(x - r * 0.15f, y + r * 0.5f); path.lineTo(x + r * 0.7f, y - r * 0.55f);
        s.setColor(A0); s.setAlpha(255); s.setStrokeWidth(r * 0.48f); c.drawPath(path, s);
        s.setColor(A6); s.setStrokeWidth(r * 0.26f); c.drawPath(path, s);
    }

    /** Freeze-spell bubble. */
    void bubble(Canvas c, float x, float y, float size) {
        if (painted(c, icBubble, x, y, size)) return;
        float r = size * 0.4f;
        p.setShader(null); p.setColor(A7); p.setAlpha(90); c.drawCircle(x, y, r, p);
        s.setColor(A0); s.setAlpha(255); s.setStrokeWidth(r * 0.2f); c.drawCircle(x, y, r, s);
        s.setColor(A7); s.setStrokeWidth(r * 0.1f); c.drawCircle(x, y, r, s);
        p.setColor(0xFFFFFFFF); p.setAlpha(230); c.drawCircle(x - r * 0.38f, y - r * 0.38f, r * 0.2f, p);
        p.setAlpha(170); c.drawCircle(x + r * 0.3f, y + r * 0.35f, r * 0.08f, p);
    }

    void music(Canvas c, float x, float y, float size, boolean on) {
        if (painted(c, icMusic, x, y, size)) { if (!on) slash(c, x, y, size * 0.42f); return; }
        float r = size * 0.42f;
        path.reset();
        path.moveTo(x - r * 0.35f, y + r * 0.45f); path.lineTo(x - r * 0.35f, y - r * 0.7f); path.lineTo(x + r * 0.6f, y - r * 0.9f);
        path.lineTo(x + r * 0.6f, y + r * 0.25f);
        s.setColor(A0); s.setStrokeWidth(r * 0.3f); c.drawPath(path, s);
        s.setColor(A7); s.setStrokeWidth(r * 0.14f); c.drawPath(path, s);
        for (float hx : new float[]{x - r * 0.55f, x + r * 0.4f}) {
            float hy = hx < x ? y + r * 0.5f : y + r * 0.3f;
            r2.set(hx - r * 0.3f, hy - r * 0.22f, hx + r * 0.3f, hy + r * 0.22f);
            p.setShader(null); p.setColor(A7); p.setAlpha(255); c.drawOval(r2, p);
            s.setColor(A0); s.setStrokeWidth(r * 0.1f); c.drawOval(r2, s);
        }
        if (!on) slash(c, x, y, r);
    }

    void haptics(Canvas c, float x, float y, float size, boolean on) {
        if (painted(c, icHaptics, x, y, size)) { if (!on) slash(c, x, y, size * 0.42f); return; }
        float r = size * 0.42f;
        r2.set(x - r * 0.35f, y - r * 0.7f, x + r * 0.35f, y + r * 0.7f);
        p.setShader(null); p.setColor(A5); p.setAlpha(255); c.drawRoundRect(r2, r * 0.15f, r * 0.15f, p);
        s.setColor(A0); s.setStrokeWidth(r * 0.12f); c.drawRoundRect(r2, r * 0.15f, r * 0.15f, s);
        p.setColor(A1); r2.set(x - r * 0.22f, y - r * 0.52f, x + r * 0.22f, y + r * 0.38f); c.drawRect(r2, p);
        s.setColor(A7); s.setStrokeWidth(r * 0.1f);
        for (int e = -1; e <= 1; e += 2)
            for (int k = 0; k < 2; k++) { float lx = x + e * r * (0.58f + k * 0.22f); c.drawLine(lx, y - r * (0.3f - k * 0.08f), lx, y + r * (0.3f - k * 0.08f), s); }
        if (!on) slash(c, x, y, r);
    }

    void bell(Canvas c, float x, float y, float size, boolean on) {
        if (painted(c, icBell, x, y, size)) { if (!on) slash(c, x, y, size * 0.42f); return; }
        float r = size * 0.42f;
        path.reset();
        path.moveTo(x - r * 0.75f, y + r * 0.45f);
        path.cubicTo(x - r * 0.55f, y + r * 0.3f, x - r * 0.6f, y - r * 0.75f, x, y - r * 0.75f);
        path.cubicTo(x + r * 0.6f, y - r * 0.75f, x + r * 0.55f, y + r * 0.3f, x + r * 0.75f, y + r * 0.45f);
        path.close();
        grad(y - r * 0.75f, y + r * 0.45f, A6, A4); c.drawPath(path, p); p.setShader(null);
        outline(c, path, r * 0.12f);
        p.setColor(A0); c.drawCircle(x, y + r * 0.62f, r * 0.2f, p);
        p.setColor(A5); c.drawCircle(x, y + r * 0.6f, r * 0.12f, p);
        if (!on) slash(c, x, y, r);
    }

    void shakeIcon(Canvas c, float x, float y, float size, boolean on) {
        if (painted(c, icShake, x, y, size)) { if (!on) slash(c, x, y, size * 0.42f); return; }
        float r = size * 0.42f;
        r2.set(x - r * 0.5f, y - r * 0.42f, x + r * 0.5f, y + r * 0.42f);
        p.setShader(null); p.setColor(A5); p.setAlpha(255); c.drawRoundRect(r2, r * 0.12f, r * 0.12f, p);
        s.setColor(A0); s.setStrokeWidth(r * 0.12f); c.drawRoundRect(r2, r * 0.12f, r * 0.12f, s);
        p.setColor(A2); c.drawCircle(x, y, r * 0.2f, p);
        s.setColor(A7); s.setStrokeWidth(r * 0.1f);
        for (int e = -1; e <= 1; e += 2) { c.drawLine(x + e * r * 0.7f, y - r * 0.35f, x + e * r * 0.82f, y, s); c.drawLine(x + e * r * 0.82f, y, x + e * r * 0.7f, y + r * 0.35f, s); }
        if (!on) slash(c, x, y, r);
    }

    private void slash(Canvas c, float x, float y, float r) {
        s.setColor(A0); s.setAlpha(255); s.setStrokeWidth(r * 0.3f); c.drawLine(x - r * 0.75f, y - r * 0.75f, x + r * 0.75f, y + r * 0.75f, s);
        s.setColor(0xFFE0583A); s.setStrokeWidth(r * 0.15f); c.drawLine(x - r * 0.75f, y - r * 0.75f, x + r * 0.75f, y + r * 0.75f, s);
    }

    /** Coin pouch (lucky drops, gem jar). */
    void pouch(Canvas c, float x, float y, float size) {
        if (painted(c, icPouch, x, y, size)) return;
        float r = size * 0.42f;
        path.reset();
        path.moveTo(x - r * 0.3f, y - r * 0.45f);
        path.cubicTo(x - r * 0.95f, y - r * 0.2f, x - r, y + r * 0.85f, x, y + r * 0.85f);
        path.cubicTo(x + r, y + r * 0.85f, x + r * 0.95f, y - r * 0.2f, x + r * 0.3f, y - r * 0.45f);
        path.close();
        grad(y - r * 0.45f, y + r * 0.85f, A4, A2); c.drawPath(path, p); p.setShader(null);
        outline(c, path, r * 0.12f);
        path2.reset(); path2.moveTo(x - r * 0.3f, y - r * 0.45f); path2.lineTo(x - r * 0.5f, y - r * 0.85f);
        path2.lineTo(x + r * 0.5f, y - r * 0.85f); path2.lineTo(x + r * 0.3f, y - r * 0.45f); path2.close();
        p.setColor(A3); c.drawPath(path2, p); outline(c, path2, r * 0.1f);
        coin(c, x, y + r * 0.25f, r * 0.38f);
    }

    /** Daily gift box. */
    void gift(Canvas c, float x, float y, float size) {
        if (painted(c, icGift, x, y, size)) return;
        float r = size * 0.42f;
        r2.set(x - r * 0.8f, y - r * 0.25f, x + r * 0.8f, y + r * 0.85f);
        grad(r2.top, r2.bottom, A4, A2); c.drawRect(r2, p); p.setShader(null);
        s.setColor(A0); s.setStrokeWidth(r * 0.12f); c.drawRect(r2, s);
        r2.set(x - r * 0.9f, y - r * 0.5f, x + r * 0.9f, y - r * 0.2f);
        p.setColor(A3); c.drawRect(r2, p); c.drawRect(r2, s);
        p.setColor(A6); r2.set(x - r * 0.15f, y - r * 0.5f, x + r * 0.15f, y + r * 0.85f); c.drawRect(r2, p); c.drawRect(r2, s);
        for (int e = -1; e <= 1; e += 2) {
            r2.set(x + (e < 0 ? -r * 0.6f : 0), y - r * 0.9f, x + (e < 0 ? 0 : r * 0.6f), y - r * 0.5f);
            p.setColor(A6); c.drawOval(r2, p); s.setStrokeWidth(r * 0.1f); c.drawOval(r2, s);
        }
    }

    private final RectF r2 = new RectF();

    /** Tree-stump tower pad. */
    Bitmap pad(float w) {
        float h = w * 0.62f, pad = u;
        Bitmap bm = Bitmap.createBitmap((int) (w + pad * 2), (int) (h + pad * 2), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        c.translate(pad, pad);
        float top = h * 0.48f; // height of the elliptical top face
        p.setShader(null); p.setColor(0xFF000000); p.setAlpha(120);
        r.set(-0.5f * u, h - top * 0.55f, w + 0.5f * u, h + 0.4f * u); c.drawOval(r, p);
        // side band
        p.setColor(0xFFFFFFFF); p.setShader(new LinearGradient(0, 0, w, 0, new int[]{A1, A2, A1}, null, Shader.TileMode.CLAMP)); p.setAlpha(255);
        path.reset();
        path.addRect(0, top / 2, w, h - top / 2, Path.Direction.CW);
        r.set(0, h - top, w, h); path.addOval(r, Path.Direction.CW);
        c.drawPath(path, p); p.setShader(null);
        s.setColor(A0); s.setAlpha(110); s.setStrokeWidth(0.3f * u);
        for (int i = 1; i < 7; i++) { float x = w * i / 7f; c.drawLine(x, top / 2 + u, x + (i % 2 == 0 ? 0.5f : -0.5f) * u, h - top * 0.25f, s); }
        s.setAlpha(255); s.setStrokeWidth(0.7f * u);
        r.set(0, h - top, w, h); c.drawArc(r, 0, 180, false, s);
        c.drawLine(0, top / 2, 0, h - top / 2, s); c.drawLine(w, top / 2, w, h - top / 2, s);
        // top face with rings
        r.set(0, 0, w, top);
        p.setColor(0xFFFFFFFF); p.setShader(new RadialGradient(w / 2, top / 2, w / 2, new int[]{A5, A4, A3}, new float[]{0, 0.6f, 1}, Shader.TileMode.CLAMP));
        c.drawOval(r, p); p.setShader(null);
        s.setColor(A2); s.setStrokeWidth(0.25f * u);
        for (int k = 1; k <= 3; k++) { s.setAlpha(150 - k * 25); float f = k / 4f; r.set(w / 2 - w / 2 * f, top / 2 - top / 2 * f, w / 2 + w / 2 * f, top / 2 + top / 2 * f); c.drawOval(r, s); }
        s.setColor(A0); s.setAlpha(255); s.setStrokeWidth(0.7f * u);
        r.set(0, 0, w, top); c.drawOval(r, s);
        return bm;
    }

    /** Night field: indigo ground, stones, grass tufts, vignette. Pure indigo family. */
    /** Cover-fit transform of the background onto a w x h screen: {scale, offsetX, offsetY}. */
    float[] cover(int w, int h) {
        Bitmap g = groundBaked != null ? groundBaked : ground;
        if (g == null) return null;
        float sc = Math.max(w / (float) g.getWidth(), h / (float) g.getHeight());
        return new float[]{sc, (w - g.getWidth() * sc) / 2, (h - g.getHeight() * sc) / 2};
    }

    Bitmap field(int w, int h, long seed) {
        Bitmap bm = Bitmap.createBitmap(Math.max(1, w), Math.max(1, h), Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bm);
        Bitmap ground = groundBaked != null ? groundBaked : this.ground;
        if (ground != null) {                          // painted night meadow, scaled to cover
            float sc = Math.max(w / (float) ground.getWidth(), h / (float) ground.getHeight());
            float gw = ground.getWidth() * sc, gh = ground.getHeight() * sc;
            c.drawBitmap(ground, null, new RectF((w - gw) / 2, (h - gh) / 2, (w + gw) / 2, (h + gh) / 2), bp);
            return bm;
        }
        p.setColor(0xFFFFFFFF); p.setShader(new LinearGradient(0, 0, 0, h, I1, I2, Shader.TileMode.CLAMP)); c.drawRect(0, 0, w, h, p); p.setShader(null);
        Random rnd = new Random(seed);
        for (int i = 0; i < 60; i++) {
            p.setColor(rnd.nextBoolean() ? I3 : I0); p.setAlpha(18 + rnd.nextInt(22));
            float x = rnd.nextFloat() * w, y = rnd.nextFloat() * h, rr = (4 + rnd.nextFloat() * 14) * u;
            r.set(x - rr, y - rr * 0.55f, x + rr, y + rr * 0.55f); c.drawOval(r, p);
        }
        for (int i = 0; i < 120; i++) {               // grass tufts
            float x = rnd.nextFloat() * w, y = rnd.nextFloat() * h;
            s.setColor(rnd.nextBoolean() ? I3 : I4); s.setAlpha(70 + rnd.nextInt(60)); s.setStrokeWidth(0.3f * u);
            for (int k = -1; k <= 1; k++) c.drawLine(x, y, x + k * 0.7f * u + (rnd.nextFloat() - 0.5f) * 0.4f * u, y - (1 + rnd.nextFloat()) * u, s);
        }
        for (int i = 0; i < 22; i++) {                // stones
            float x = rnd.nextFloat() * w, y = rnd.nextFloat() * h, rr = (0.8f + rnd.nextFloat() * 1.8f) * u;
            p.setColor(I0); p.setAlpha(140); r.set(x - rr * 1.1f, y - rr * 0.3f, x + rr * 1.1f, y + rr * 0.75f); c.drawOval(r, p);
            p.setColor(I3); p.setAlpha(255); r.set(x - rr, y - rr * 0.7f, x + rr, y + rr * 0.5f); c.drawOval(r, p);
            p.setColor(I4); p.setAlpha(160); r.set(x - rr * 0.6f, y - rr * 0.6f, x + rr * 0.2f, y - rr * 0.1f); c.drawOval(r, p);
            s.setColor(I0); s.setAlpha(255); s.setStrokeWidth(0.3f * u); r.set(x - rr, y - rr * 0.7f, x + rr, y + rr * 0.5f); c.drawOval(r, s);
        }
        p.setColor(0xFFFFFFFF); p.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.75f, new int[]{0x00000000, 0x00000000, 0xC0060410},
                new float[]{0, 0.45f, 1}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p); p.setShader(null);
        return bm;
    }
}
