package com.pantheon3d.jellykeep;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.DisplayCutout;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import static com.pantheon3d.jellykeep.Art.*;

/** Jelly Keep: idle/clicker tower defense. One view: simulation, rendering and input. */
public class Game extends View {

    // =============================================================== definitions
    static final int POPPER = 0, WALL = 1, MORTAR = 2, SPARKLER = 3, SNIPER = 4, MAGE = 5, BOMB = 6, BANKER = 7, DRUMMER = 8, LANTERN = 9;
    static final String[] T_NAME = {"Pebble Popper", "Jelly Wall", "Goo Mortar", "Sparkler", "Spyglass Sniper", "Bubble Mage", "Bomb Buddy", "Piggy Banker", "Drummer", "Lantern Keeper"};
    static final String[] T_SHORT = {"Popper", "Wall", "Mortar", "Sparkler", "Sniper", "Mage", "Bomb", "Banker", "Drummer", "Lantern"};
    static final String[] T_ROLE = {"fast single shots", "slams & knocks back", "goo splash, slows", "rapid sparks, short range", "long range, hits the biggest",
            "bubbles slow everything", "huge blast, slow fuse", "makes coins on its own", "speeds up nearby towers", "burns everything nearby"};
    static final double[] T_COST = {30, 60, 120, 90, 220, 160, 400, 150, 300, 500};
    static final double[] T_DMG = {4, 5, 6, 1.6, 24, 1.5, 45, 0, 0, 3};          // lantern: damage per second
    static final float[] T_RATE = {1.2f, 0.55f, 0.4f, 4.5f, 0.45f, 1f, 0.28f, 0.33f, 2f, 4f};
    static final float[] T_RANGE = {36, 17, 50, 26, 80, 30, 40, 0, 32, 24};    // in u
    static final float[] T_WIND = {0.16f, 0.32f, 0.28f, 0, 0.4f, 0.2f, 0.45f, 0.35f, 0.15f, 0};   // anticipation before each attack
    static final int[] T_UNLOCK = {1, 1, 1, 3, 6, 4, 10, 5, 8, 13};             // best wave needed
    static final int[] T_FLIP = {1, 0, 1, 0, 1, 1, 1, 0, 0, -1};               // 1 faces right, -1 faces left, 0 never flips
    static final float[] T_H = {18.5f, 19.5f, 18.5f, 15f, 20f, 16.5f, 16.5f, 16.5f, 18f, 17.5f};
    static final int[] DISPLAY = {0, 1, 2, 3, 5, 7, 4, 8, 6, 9};                // build menu order = unlock order

    static final int GLOOM = 0, SKITTER = 1, BRUTE = 2, BOSS = 3, FLAPPER = 4, SHIELD = 5, SPLITTER = 6, HEALER = 7, MINI = 8;
    static final float[] E_HP = {1, 0.45f, 4, 28, 0.6f, 2.2f, 2.4f, 1.3f, 0.4f};
    static final float[] E_SPD = {8, 14, 5, 3.5f, 11, 6, 6, 7, 11};
    static final float[] E_R = {4.8f, 3.6f, 7.2f, 11.5f, 4.2f, 6.2f, 6.8f, 4.6f, 3.4f};
    static final float[] E_DMG = {5, 3, 14, 40, 4, 8, 9, 3, 3};
    static final float[] E_REWARD = {1, 0.7f, 4, 35, 1.2f, 2.5f, 2, 2, 0.5f};
    static final float[] E_HH = {2.35f, 2.35f, 2.55f, 2.55f, 2.15f, 2.3f, 1.9f, 2.45f, 2.35f};
    static final int[] E_FLIP = {0, -1, 1, 0, 0, -1, 0, 0, 0};
    static final String[] E_NAME = {"", "", "", "", "Flapper!", "Shieldling!", "Splitter!", "Wisp Healer!"};
    static final String[] E_HINT = {"", "", "", "", "flies over walls & splashes", "blocks shots, not splash", "splits in two when squished", "heals its friends, hit it first"};

    // =============================================================== systems
    final Art art;
    final Sfx sfx;
    Ads ads = new Ads.Stub();
    private final SharedPreferences prefs;
    private final Vibrator vib;
    final Random rnd = new Random();

    // =============================================================== saved state
    double coins, coinRate;
    int wave = 1, bestWave = 1, tapLv, fortLv, wavesCleared;
    final int[] tType = {-1, -1, -1, -1, -1, -1}, tLv = new int[6];
    boolean tutStrike = true, tutBuild = true;
    long lastSaveMs, boostUntil;

    // =============================================================== runtime state
    double baseHp, baseMax;
    int phase;                  // 0 = countdown, 1 = fighting, 2 = defeat
    float phaseT = 2.5f;
    int toSpawn, waveTotal;
    float spawnT;
    int selected = -1;          // pad index or -1
    int overlay;                // 0 none, 1 reward, 2 defeat
    double rewardAmt; String rewardTitle = "", rewardSub = ""; String rewardPlacement = Ads.OFFLINE_X2; int rewardMult = 2;
    boolean chestUp; float chestX, chestY, chestT;

    final ArrayList<Enemy> enemies = new ArrayList<>();
    final ArrayList<Shot> shots = new ArrayList<>();
    final ArrayList<P> parts = new ArrayList<>();
    final ArrayList<Pop> pops = new ArrayList<>();
    final ArrayList<float[]> puddles = new ArrayList<>(); // x, y, r, life
    final float[] tBuff = {1, 1, 1, 1, 1, 1}, tBeat = new float[6];
    int seenMask;
    final RectF buildR = new RectF(), sellBtn = new RectF();
    RectF curPanel;
    final int[] shown = new int[10];
    int nShown, gridCols = 3;
    final java.util.HashMap<String, Bitmap[]> btnCache = new java.util.HashMap<>();
    final java.util.HashMap<String, Bitmap> panelCache = new java.util.HashMap<>();
    final float[] tCool = new float[6], tSq = new float[6], tSqV = new float[6], tFace = {1, 1, 1, 1, 1, 1}, tWind = new float[6];

    // feel
    boolean running;
    private long lastNs;
    float time, hitStop, trauma, flashA, shakeX, shakeY;
    int flashCol = 0xFFFFFFFF;
    float baseSq, baseSqV, baseHurtT, baseHappyT, coinBump, coinBumpV, waveBannerT, pressT, rateClock;
    double rateAcc;
    int pressId = -1;
    String bannerA = "", bannerB = "";
    float lookX, lookY;
    long lastDieSnd, lastHitSnd, lastShootSnd;

    // =============================================================== layout
    float W, H, u = 10, safeT, safeB, safeL, safeR;
    float fieldTop, fieldBot, baseX, baseY, baseR;
    final float[] padX = new float[6], padY = new float[6];
    final RectF coinPill = new RectF(), waveBar = new RectF(), soundBtn = new RectF(), boostBtn = new RectF(), panelR = new RectF(),
            btnA = new RectF(), btnB = new RectF(), closeBtn = new RectF(), upBtn = new RectF(), ovA = new RectF(), ovB = new RectF(), ovPanel = new RectF();
    final RectF[] cardR = new RectF[10];
    { for (int k = 0; k < 10; k++) cardR[k] = new RectF(); }
    final RectF tmp = new RectF(), tmp2 = new RectF();
    Bitmap bField, bPanel, bPlank, bCoinPill, bWaveBar, bRound, bPad, bClose, bOvPanel, bOvPlank, bHpBar;
    Bitmap[] bBtnA, bBtnB, bCard, bUp, bBoost, bOvA, bOvB;

    final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG), bmp = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    final Path path = new Path();
    final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    { shadowPaint.setShader(new RadialGradient(0, 0, 1, new int[]{0xFF000000, 0xA0000000, 0x00000000}, new float[]{0, 0.45f, 1}, Shader.TileMode.CLAMP)); }

    /** Soft elliptical shadow with a feathered edge (no hard disc). */
    void softShadow(Canvas c, float x, float y, float rx, float ry, float strength) {
        c.save(); c.translate(x, y); c.scale(rx, ry);
        shadowPaint.setAlpha((int) (255 * clamp(strength, 0, 1)));
        c.drawCircle(0, 0, 1, shadowPaint);
        c.restore();
    }
    final Paint lowHpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    { lowHpPaint.setColorFilter(new android.graphics.PorterDuffColorFilter(0x55FF3020, android.graphics.PorterDuff.Mode.SRC_ATOP)); }
    final Paint burnPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    { burnPaint.setColorFilter(new android.graphics.PorterDuffColorFilter(0x77FF8A1F, android.graphics.PorterDuff.Mode.SRC_ATOP)); }
    final Paint flashPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    { flashPaint.setColorFilter(new android.graphics.PorterDuffColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.SRC_IN)); flashPaint.setAlpha(190); }

    // =============================================================== setup
    public Game(Context ctx) {
        super(ctx);
        art = new Art(ctx);
        sfx = new Sfx(ctx);
        prefs = ctx.getSharedPreferences("jellykeep", Context.MODE_PRIVATE);
        vib = ctx.getSystemService(Vibrator.class);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        load();
        setOnApplyWindowInsetsListener((v, in) -> {
            float t = 0, b = 0, l = 0, r = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets a = in.getInsetsIgnoringVisibility(WindowInsets.Type.displayCutout() | WindowInsets.Type.statusBars());
                Insets g = in.getInsets(WindowInsets.Type.mandatorySystemGestures());
                t = a.top; l = a.left; r = a.right; b = Math.max(a.bottom, g.bottom);
            } else if (Build.VERSION.SDK_INT >= 28) {
                DisplayCutout dc = in.getDisplayCutout();
                if (dc != null) { t = dc.getSafeInsetTop(); b = dc.getSafeInsetBottom(); l = dc.getSafeInsetLeft(); r = dc.getSafeInsetRight(); }
            }
            float dp = getResources().getDisplayMetrics().density;
            safeT = Math.max(t, 20 * dp); safeB = Math.max(b, 10 * dp); safeL = l; safeR = r;
            layout();
            return in;
        });
    }

    void resume() {
        running = true;
        lastNs = 0;
        long now = System.currentTimeMillis();
        if (lastSaveMs > 0 && overlay == 0) {
            double away = Math.min((now - lastSaveMs) / 1000.0, 4 * 3600);
            double amt = coinRate * away * 0.5;
            if (away >= 60 && amt >= 1) showReward("Welcome back!", "your keep earned while you slept", amt, Ads.OFFLINE_X2, 2);
        }
        lastSaveMs = now;
        invalidate();
    }

    void pause() { running = false; save(); }

    private void load() {
        SharedPreferences p = prefs;
        coins = p.contains("coins") ? Double.longBitsToDouble(p.getLong("coins", 0)) : 40;   // enough for a first tower right away
        coinRate = Double.longBitsToDouble(p.getLong("rate", 0));
        wave = Math.max(1, p.getInt("wave", 1)); bestWave = Math.max(wave, p.getInt("best", 1));
        seenMask = p.getInt("seen", 0); tapLv = p.getInt("tap", 0); fortLv = p.getInt("fort", 0); wavesCleared = p.getInt("cleared", 0);
        for (int i = 0; i < 6; i++) { tType[i] = p.getInt("tt" + i, -1); tLv[i] = p.getInt("tl" + i, 0); }
        tutStrike = p.getBoolean("tutS", true); tutBuild = p.getBoolean("tutB", true);
        lastSaveMs = p.getLong("t", 0); boostUntil = p.getLong("boost", 0);
        sfx.on = p.getBoolean("sound", true);
        baseMax = baseMaxFor(fortLv); baseHp = baseMax;
    }

    void save() {
        lastSaveMs = System.currentTimeMillis();
        SharedPreferences.Editor e = prefs.edit()
                .putLong("coins", Double.doubleToLongBits(coins)).putLong("rate", Double.doubleToLongBits(coinRate))
                .putInt("wave", wave).putInt("best", bestWave).putInt("tap", tapLv).putInt("fort", fortLv).putInt("cleared", wavesCleared).putInt("seen", seenMask)
                .putBoolean("tutS", tutStrike).putBoolean("tutB", tutBuild).putBoolean("sound", sfx.on)
                .putLong("t", lastSaveMs).putLong("boost", boostUntil);
        for (int i = 0; i < 6; i++) e.putInt("tt" + i, tType[i]).putInt("tl" + i, tLv[i]);
        e.apply();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        W = w; H = h;
        float dp = getResources().getDisplayMetrics().density;
        if (safeT == 0) safeT = 20 * dp;
        if (safeB == 0) safeB = 10 * dp;
        layout();
    }

    void layout() {
        if (W == 0) return;
        u = Math.min(W, H * 0.5f) / 100f;
        art.u = u;
        float L = safeL + 2.5f * u, R = W - safeR - 2.5f * u, top = safeT + 2 * u;

        coinPill.set(L + 4 * u, top + 1.2f * u, L + 31 * u, top + 8.2f * u);
        boostBtn.set(L + 1 * u, top + 11 * u, L + 25 * u, top + 18 * u);
        soundBtn.set(R - 10 * u, top, R, top + 10 * u);
        float pw = 34 * u;
        waveBar.set(W / 2 - 13 * u, top + 10.2f * u, W / 2 + 13 * u, top + 13.2f * u);
        fieldTop = top + 20 * u;

        float ph = 44 * u;
        panelR.set(L, H - safeB - ph - 1.5f * u, R, H - safeB - 1.5f * u);
        fieldBot = panelR.top - 1 * u;
        float in = 4 * u, bw = (panelR.width() - in * 2 - 3 * u) / 2, bh = 19 * u, by = panelR.bottom - in - bh;
        btnA.set(panelR.left + in, by, panelR.left + in + bw, by + bh);
        btnB.set(btnA.right + 3 * u, by, panelR.right - in, by + bh);
        closeBtn.set(panelR.right - 9.5f * u, panelR.top - 3.5f * u, panelR.right + 0.5f * u, panelR.top + 6.5f * u);
        float cw = (panelR.width() - in * 2 - 4 * u) / 3;
        for (int i = 0; i < 3; i++) cardR[i].set(panelR.left + in + i * (cw + 2 * u), panelR.top + 9 * u, panelR.left + in + i * (cw + 2 * u) + cw, panelR.bottom - in);
        upBtn.set(panelR.right - in - 34 * u, by, panelR.right - in, by + bh);

        baseX = W / 2;
        baseY = (fieldTop + fieldBot) / 2 + 3 * u;
        baseR = 12 * u;
        float rx = Math.min(35 * u, W / 2 - safeL - 11 * u), ry = Math.min(46 * u, (fieldBot - fieldTop) / 2 - 16 * u);
        float[] ang = {210, 270, 330, 30, 90, 150};
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(ang[i]);
            padX[i] = baseX + (float) Math.cos(a) * rx;
            padY[i] = baseY + (float) Math.sin(a) * ry * (i == 1 || i == 4 ? 1 : 0.88f) + (i == 1 ? 3 * u : 0);
        }
        // baked stumps: anchor pads + king to the painting so they match on every aspect ratio
        float[] cv = art.groundBaked != null ? art.cover((int) W, (int) H) : null;
        if (cv != null) {
            for (int i = 0; i < 6; i++) {
                int[] st = Art.STUMPS[i];
                padX[i] = cv[1] + (st[0] + st[2] / 2f) * cv[0];
                padY[i] = cv[2] + (st[1] + st[3] * 0.17f) * cv[0] + u;   // top face, where towers stand
            }
            baseX = cv[1] + Art.KING_GX * cv[0];
            baseY = cv[2] + Art.KING_GY * cv[0];
        }
        lookX = baseX; lookY = baseY + 40 * u;

        ovPanel.set(W / 2 - 40 * u, H * 0.5f - 30 * u, W / 2 + 40 * u, H * 0.5f + 32 * u);
        ovA.set(ovPanel.left + 6 * u, ovPanel.bottom - 31 * u, ovPanel.right - 6 * u, ovPanel.bottom - 18 * u);
        ovB.set(ovPanel.left + 14 * u, ovPanel.bottom - 15 * u, ovPanel.right - 14 * u, ovPanel.bottom - 5 * u);

        // pre-render the kit pieces at their exact sizes
        bField = art.field((int) W, (int) H, 7);
        bPanel = art.panel(panelR.width(), panelR.height(), true, 3);
        bPlank = art.plank(pw, 9 * u, 11);
        bCoinPill = art.recess(coinPill.width(), coinPill.height());
        bWaveBar = art.recess(waveBar.width(), waveBar.height());
        bHpBar = art.recess(24 * u, 3f * u);
        bRound = art.roundButton(soundBtn.width());
        bClose = art.roundButton(closeBtn.width());
        bPad = art.pad(17 * u);
        bBtnA = art.button(btnA.width(), btnA.height(), GOLD);
        bBtnB = art.button(btnB.width(), btnB.height(), GOLD);
        bCard = art.button(cardR[0].width(), cardR[0].height(), WOOD);
        bUp = art.button(upBtn.width(), upBtn.height(), GOLD);
        bBoost = art.button(boostBtn.width(), boostBtn.height(), GOLD);
        bOvPanel = art.panel(ovPanel.width(), ovPanel.height(), true, 5);
        bOvPlank = art.plank(50 * u, 11 * u, 13);
        bOvA = art.button(ovA.width(), ovA.height(), GOLD);
        bOvB = art.button(ovB.width(), ovB.height(), WOOD);
    }

    // =============================================================== economy
    static double baseMaxFor(int lv) { return 100 * Math.pow(1.3, lv); }
    double tapDmg() { return 3 * Math.pow(1.5, tapLv); }
    double tapCost() { return Math.ceil(15 * Math.pow(1.8, tapLv)); }
    double fortCost() { return Math.ceil(25 * Math.pow(1.85, fortLv)); }
    double towerDmg(int i) { return T_DMG[tType[i]] * Math.pow(1.55, tLv[i]); }
    double upCost(int i) { return Math.ceil(T_COST[tType[i]] * Math.pow(1.7, tLv[i] + 1)); }
    double hpScale(int w) { return 8 * Math.pow(1.165, w - 1); }
    double rewardScale(int w) { return 2 * Math.pow(1.12, w - 1); }
    boolean boosted() { return System.currentTimeMillis() < boostUntil; }

    void gain(double v, boolean fromKill) {
        if (fromKill && boosted()) v *= 2;
        coins += v;
        rateAcc += v;
    }

    // =============================================================== loop
    @Override
    protected void onDraw(Canvas c) {
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        update(dt);
        render(c);
        if (running) postInvalidateOnAnimation();
    }

    void update(float dt) {
        time += dt;
        trauma = Math.max(0, trauma - dt * 1.6f);
        flashA = Math.max(0, flashA - dt * 3.5f);
        float sh = trauma * trauma;
        shakeX = (float) (Math.sin(time * 71.3) + 0.5 * Math.sin(time * 37.9)) * sh * u * 2.6f;
        shakeY = (float) (Math.cos(time * 63.1) + 0.5 * Math.sin(time * 29.3)) * sh * u * 2.6f;
        pressT -= dt;
        if (hitStop > 0) { hitStop -= dt; return; }

        baseSqV += (-baseSq * 300 - baseSqV * 12) * dt; baseSq += baseSqV * dt;
        coinBumpV += (-coinBump * 420 - coinBumpV * 15) * dt; coinBump += coinBumpV * dt;
        for (int i = 0; i < 6; i++) { tSqV[i] += (-tSq[i] * 380 - tSqV[i] * 13) * dt; tSq[i] += tSqV[i] * dt; }
        baseHurtT -= dt; baseHappyT -= dt; waveBannerT -= dt;

        // income rate for offline earnings
        rateClock += dt;
        if (rateClock >= 5) { coinRate = coinRate * 0.9 + (rateAcc / rateClock) * 0.1; rateAcc = 0; rateClock = 0; }

        if (overlay == 0) updateWave(dt);
        updateEnemies(dt);
        updateTowers(dt);
        updateShots(dt);
        updateParts(dt);
        for (int i = puddles.size() - 1; i >= 0; i--) { float[] q = puddles.get(i); q[3] -= dt; if (q[3] <= 0) puddles.remove(i); }
        for (int i = pops.size() - 1; i >= 0; i--) { Pop p = pops.get(i); p.age += dt; p.y += p.vy * dt; p.vy *= (1 - dt * 3); if (p.age >= p.max) pops.remove(i); }
        if (chestUp) chestT += dt;

        // eyes follow nearest enemy, else the last tap
        Enemy n = nearest(baseX, baseY, 1e9f);
        if (n != null) { lookX = n.x; lookY = n.y; }
    }

    // ---------------------------------------------------------------- waves
    void updateWave(float dt) {
        if (phase == 0) {
            phaseT -= dt;
            if (phaseT <= 0) startWave();
        } else if (phase == 1) {
            if (toSpawn > 0) {
                spawnT -= dt;
                if (spawnT <= 0) { spawnT = (float) Math.max(0.33, 1.1 - wave * 0.025); spawn(); }
            } else if (enemies.isEmpty()) waveCleared();
        }
    }

    void startWave() {
        phase = 1;
        waveTotal = toSpawn = Math.min(6 + (int) (wave * 1.4f), 45);
        spawnT = 0.3f;
        sfx.play(sfx.horn, 0.6f, 1f);
    }

    void spawn() {
        toSpawn--;
        int type = GLOOM;
        float r = rnd.nextFloat(), acc = 0;
        if (wave % 10 == 0 && toSpawn == 0) type = BOSS;
        else if (wave >= 13 && r < (acc += 0.07f)) type = HEALER;
        else if (wave >= 11 && r < (acc += 0.10f)) type = SPLITTER;
        else if (wave >= 9 && r < (acc += 0.11f)) type = SHIELD;
        else if (wave >= 7 && r < (acc += Math.min(0.14f, 0.05f + (wave - 7) * 0.015f))) type = FLAPPER;
        else if (wave >= 5 && r < (acc += Math.min(0.2f, (wave - 4) * 0.03f))) type = BRUTE;
        else if (wave >= 3 && r > 0.75f) type = SKITTER;
        double a = rnd.nextDouble() * Math.PI * 2;
        float far = Math.max(W, H);
        float rr0 = E_R[type] * u;
        float x = clamp(baseX + (float) Math.cos(a) * far, safeL + rr0 + u, W - safeR - rr0 - u);
        float y = clamp(baseY + (float) Math.sin(a) * far, fieldTop + rr0 * 2.2f, fieldBot - rr0 * 0.5f);
        Enemy e = spawnAt(type, x, y, 0.55f);
        P ring = add(RING, e.x, e.y); if (ring != null) { ring.max = 0.5f; ring.sz = rr0 * 1.6f; ring.col = I4; }
        if (type == BOSS) { trauma = Math.min(1, trauma + 0.5f); sfx.play(sfx.hurt, 0.7f, 0.6f); pop(e.x, e.y - e.r * 2, "BOSS!", I6, 7); }
        if (type >= FLAPPER && type <= HEALER && (seenMask & (1 << type)) == 0) {          // first sighting: teach in context
            seenMask |= 1 << type;
            bannerA = E_NAME[type]; bannerB = E_HINT[type]; waveBannerT = 2.4f;
            sfx.play(sfx.horn, 0.5f, 0.8f);
        }
    }

    Enemy spawnAt(int type, float x, float y, float emerge) {
        Enemy e = new Enemy();
        e.type = type; e.x = x; e.y = y; e.emerge = emerge;
        e.max = e.hp = hpScale(wave) * E_HP[type];
        e.r = E_R[type] * u;
        e.spd = E_SPD[type] * u;
        e.dmg = E_DMG[type] * Math.pow(1.08, wave - 1);
        e.reward = rewardScale(wave) * E_REWARD[type];
        e.ph = rnd.nextFloat() * 6;
        enemies.add(e);
        return e;
    }

    static boolean flyer(Enemy e) { return e.type == FLAPPER; }
    float lift(Enemy e) { return e.type == FLAPPER ? e.r * 1.9f + (float) Math.sin(e.ph * 0.6f) * e.r * 0.3f : 0; }
    /** Visual centre of an enemy's body (what taps and projectiles aim at). */
    float cy(Enemy e) { return e.y - e.r - lift(e); }

    void waveCleared() {
        double bonus = Math.ceil(rewardScale(wave) * 5);
        gain(bonus, false);
        wavesCleared++;
        wave++;
        int prevBest = bestWave;
        bestWave = Math.max(bestWave, wave);
        phase = 0; phaseT = 2.2f;
        baseHp = baseMax;
        bannerA = "WAVE CLEAR!"; bannerB = "+" + fmt(bonus) + " bonus";
        for (int t = 0; t < T_UNLOCK.length; t++)
            if (T_UNLOCK[t] > prevBest && T_UNLOCK[t] <= bestWave) { bannerA = "NEW TOWER!"; bannerB = T_NAME[t] + " unlocked"; sfx.play(sfx.star, 0.8f, 1f); }
        waveBannerT = 2.0f;
        baseHappyT = 1.6f; baseSqV -= 10;
        hitStop = 0.06f; trauma = Math.min(1, trauma + 0.3f);
        for (int i = 0; i < 40; i++) confetti(baseX, baseY - baseR * 2);
        for (int i = 0; i < 8; i++) coinPart(baseX, baseY - baseR, 1.2f);
        sfx.play(sfx.levelUp, 0.8f, 1f);
        if (wavesCleared % 5 == 0 && !chestUp) { chestUp = true; chestT = 0; chestX = baseX + 15 * u; chestY = baseY + 15 * u; }
        save();
    }

    void defeat() {
        phase = 2; overlay = 2;
        hitStop = 0.2f; trauma = 1; flash(I4, 0.8f);
        sfx.play(sfx.defeat, 0.9f, 1f);
        buzz(new long[]{0, 80, 60, 120});
        for (Enemy e : enemies) wisps(e.x, e.y, 6);
        enemies.clear(); shots.clear();
        save();
    }

    void revive() {
        overlay = 0; phase = 0; phaseT = 1.5f;
        baseHp = baseMax; baseHappyT = 1.2f;
        flash(A6, 0.5f); sfx.play(sfx.levelUp, 0.7f, 1.1f);
    }

    void retreat() {
        overlay = 0; phase = 0; phaseT = 2f;
        wave = Math.max(1, wave - 3);
        baseHp = baseMax;
        save();
    }

    // ---------------------------------------------------------------- enemies
    void updateEnemies(float dt) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (e.emerge > 0) { e.emerge -= dt; continue; }   // ANTICIPATION: rising out of the dark
            e.ph += dt * (e.type == SKITTER ? 14 : 7);
            e.hitT -= dt; e.slowT -= dt; e.bubT -= dt; e.burnT -= dt;
            if (e.type == HEALER) {
                e.abT -= dt;
                if (e.abT <= 0) {
                    e.abT = 1.1f;
                    float hr = 22 * u; boolean healed = false;
                    for (Enemy o : enemies) {
                        if (o.dead || o.hp >= o.max || Math.hypot(o.x - e.x, o.y - e.y) > hr) continue;
                        o.hp = Math.min(o.max, o.hp + o.max * 0.08); healed = true;
                        if (o != e) pop(o.x, cy(o) - o.r * 1.4f, "+", I6, 3.4f);
                    }
                    P ring = add(RING, e.x, e.y); if (ring != null) { ring.max = 0.6f; ring.sz = hr; ring.col = I5; }
                    if (healed) sfx.play(sfx.star, 0.18f, 1.5f);
                }
            }
            e.kx *= (1 - dt * 7); e.ky *= (1 - dt * 7);
            float dx = baseX - e.x, dy = (baseY - baseR * 0.3f) - e.y, d = (float) Math.max(1, Math.hypot(dx, dy));
            float reach = baseR * 1.1f + e.r;
            float spd = e.spd * Math.min(e.slowT > 0 ? 0.55f : 1, e.bubT > 0 ? 0.5f : 1);
            if (d > reach) {
                e.atk = false;
                float wob = flyer(e) ? (float) Math.sin(e.ph * 0.3f) * 0.6f : (float) Math.sin(e.ph * 0.5f) * 0.35f;
                e.x += (dx / d - dy / d * wob) * spd * dt + e.kx * dt;
                e.y += (dy / d + dx / d * wob) * spd * dt + e.ky * dt;
            } else {
                e.atk = true;
                e.x += e.kx * dt; e.y += e.ky * dt;
                e.atkT -= dt;
                if (e.atkT <= 0) { e.atkT = 0.9f; biteBase(e); }
            }
            if (phase == 2) return;
        }
    }

    void biteBase(Enemy e) {
        baseHp -= e.dmg;
        baseHurtT = 0.35f; baseSqV += 6;
        trauma = Math.min(1, trauma + (e.type == BOSS ? 0.6f : 0.15f));
        flash(I4, e.type == BOSS ? 0.5f : 0.22f);
        long now = System.currentTimeMillis();
        if (now - lastHitSnd > 120) { lastHitSnd = now; sfx.play(sfx.hurt, 0.6f, 0.9f + rnd.nextFloat() * 0.2f); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); }
        burst(baseX + (e.x - baseX) * 0.5f, baseY - baseR * 0.4f + (e.y - baseY) * 0.4f, 5, A5, 0.8f);
        if (baseHp <= 0) { baseHp = 0; defeat(); }
    }

    void damage(Enemy e, double d, float kx, float ky) { damage(e, d, kx, ky, false); }

    /** proj = a direct shot (pebble, spark, sniper): Shieldlings block most of it. */
    void damage(Enemy e, double d, float kx, float ky, boolean proj) {
        if (proj && e.type == SHIELD) {
            d *= 0.3;
            if (rnd.nextFloat() < 0.5f) { burst(e.x, cy(e), 3, I5, 0.5f); sfx.bank("click", 0.25f, 0.2f, 60, 1.6f); }
        }
        e.hp -= d; e.hitT = 0.09f;
        float mass = e.type == BOSS ? 0.15f : e.type == BRUTE || e.type == SHIELD ? 0.4f : 1f;
        e.kx += kx * mass; e.ky += ky * mass;
        if (e.hp <= 0 && !e.dead) kill(e);
    }

    void kill(Enemy e) {
        e.dead = true;
        enemies.remove(e);
        gain(e.reward, true);
        wisps(e.x, cy(e), e.type == BOSS ? 30 : 5);
        P pf = add(POOF, e.x, cy(e)); if (pf != null) { pf.max = 0.45f; pf.sz = e.r * 2.6f; pf.rot = rnd.nextFloat() * 40 - 20; }
        if (e.type == SPLITTER) {
            for (int k = -1; k <= 1; k += 2) {
                Enemy m = spawnAt(MINI, e.x + k * e.r * 0.8f, e.y + 0.5f * u, 0);
                m.kx = k * 70 * u; m.hp = m.max = e.max * 0.35;
            }
            pop(e.x, e.y - e.r * 2.4f, "split!", I6, 4);
        }
        int n = e.type == BOSS ? 18 : e.type == BRUTE ? 4 : 2;
        for (int i = 0; i < n; i++) coinPart(e.x, e.y - e.r, 1f);
        pop(e.x, cy(e) - e.r, "+" + fmt(e.reward * (boosted() ? 2 : 1)), A6, e.type == BOSS ? 7 : 4.2f);
        long now = System.currentTimeMillis();
        if (now - lastDieSnd > 50) { lastDieSnd = now; sfx.play(sfx.die, 0.55f, 0.85f + rnd.nextFloat() * 0.35f); }
        if (e.type == BRUTE || e.type == BOSS) {
            hitStop = e.type == BOSS ? 0.14f : 0.05f;
            trauma = Math.min(1, trauma + (e.type == BOSS ? 0.9f : 0.3f));
            flash(e.type == BOSS ? A6 : 0xFFFFFFFF, e.type == BOSS ? 0.6f : 0.2f);
            if (e.type == BOSS) { for (int i = 0; i < 50; i++) confetti(e.x, e.y); buzz(new long[]{0, 50, 40, 90}); sfx.play(sfx.crit, 1f, 0.9f); }
        }
    }

    Enemy nearest(float x, float y, float range) {
        Enemy best = null; float bd = range;
        for (Enemy e : enemies) { float d = (float) Math.hypot(e.x - x, e.y - y); if (d < bd) { bd = d; best = e; } }
        return best;
    }

    /** Closest to the base within range of (x,y): the classic "first" targeting. */
    Enemy target(float x, float y, float range) { return target(x, y, range, false); }

    Enemy target(float x, float y, float range, boolean groundOnly) {
        Enemy best = null; float bd = Float.MAX_VALUE;
        for (Enemy e : enemies) {
            if (e.emerge > 0 || (groundOnly && flyer(e)) || Math.hypot(e.x - x, e.y - y) > range) continue;
            float d = (float) Math.hypot(e.x - baseX, e.y - baseY);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    /** Sniper picks the toughest thing it can see. */
    Enemy strongest(float x, float y, float range) {
        Enemy best = null; double bh = -1;
        for (Enemy e : enemies) {
            if (e.emerge > 0 || Math.hypot(e.x - x, e.y - y) > range) continue;
            if (e.hp > bh) { bh = e.hp; best = e; }
        }
        return best;
    }

    // ---------------------------------------------------------------- towers
    void updateTowers(float dt) {
        for (int i = 0; i < 6; i++) tBuff[i] = 1;
        for (int j = 0; j < 6; j++) {                              // drummers buff their neighbours
            if (tType[j] != DRUMMER) continue;
            float b = 1.25f + 0.08f * tLv[j];
            for (int i = 0; i < 6; i++)
                if (i != j && tType[i] >= 0 && Math.hypot(padX[i] - padX[j], padY[i] - padY[j]) <= T_RANGE[DRUMMER] * u) tBuff[i] = Math.max(tBuff[i], b);
        }
        for (int i = 0; i < 6; i++) {
            int t = tType[i];
            if (t < 0) continue;
            tBeat[i] -= dt;
            tCool[i] -= dt * tBuff[i];
            if (t == BANKER || t == DRUMMER) {                       // no target needed
                if (overlay != 0 || phase == 2) continue;
                if (T_WIND[t] > 0 && tCool[i] <= T_WIND[t]) tWind[i] = 1 - Math.max(0, tCool[i]) / T_WIND[t];
                if (tCool[i] <= 0) { tCool[i] = 1 / T_RATE[t]; tWind[i] = 0; if (t == BANKER) payout(i); else beat(i); }
                continue;
            }
            boolean ground = t == WALL || t == MORTAR || t == BOMB;
            Enemy tg = t == SNIPER ? strongest(padX[i], padY[i], T_RANGE[t] * u) : target(padX[i], padY[i], T_RANGE[t] * u, ground);
            if (tg != null && T_FLIP[t] != 0) tFace[i] = tg.x < padX[i] ? -1 : 1;
            if (tg == null) { tWind[i] = 0; continue; }
            if (T_WIND[t] > 0 && tCool[i] <= T_WIND[t]) {            // ANTICIPATION: crouch before the shot
                tWind[i] = 1 - Math.max(0, tCool[i]) / T_WIND[t];
            }
            if (tCool[i] <= 0) {
                tCool[i] = 1 / T_RATE[t];
                tWind[i] = 0;
                fire(i, t, tg);
            }
        }
    }

    void payout(int i) {
        double v = Math.ceil(rewardScale(wave) * 1.2 * Math.pow(1.25, tLv[i]));
        gain(v, true);
        tSqV[i] -= 9;
        for (int k = 0; k < 3; k++) coinPart(padX[i], padY[i] - 12 * u, 0.9f);
        pop(padX[i], padY[i] - 18 * u, "+" + fmt(v * (boosted() ? 2 : 1)), A6, 3.8f);
    }

    void beat(int i) {
        tSqV[i] -= 5;
        tBeat[i] = 0.18f;
        if (((int) (time * 2)) % 2 == 0) {
            P ring = add(RING, padX[i], padY[i] - u); if (ring != null) { ring.max = 0.5f; ring.sz = T_RANGE[DRUMMER] * u; ring.col = A5; }
            sfx.play(sfx.bonk, 0.12f, 0.7f);
        }
    }

    float muzzleY(int i, int t) { return padY[i] - T_H[t] * u * 0.6f; }

    void fire(int i, int t, Enemy tg) {
        float sx = padX[i], sy = padY[i] - 12 * u;
        long now = System.currentTimeMillis();
        if (t == POPPER) {
            tSqV[i] -= 7;
            Shot s = new Shot(0, sx + tFace[i] * 5 * u, sy + 1.5f * u);
            s.tg = tg; s.dmg = towerDmg(i);
            shots.add(s);
            if (!sfx.bank("popper_shoot", 0.45f, 0.07f, 45) && now - lastShootSnd > 40) { lastShootSnd = now; sfx.play(sfx.shoot, 0.35f, 0.9f + rnd.nextFloat() * 0.3f); }
        } else if (t == SPARKLER) {
            tSqV[i] -= 3;
            float side = rnd.nextBoolean() ? 1 : -1;                 // alternates hands
            Shot s = new Shot(2, sx + side * 6 * u, muzzleY(i, t) - 2 * u);
            s.tg = tg; s.dmg = towerDmg(i);
            shots.add(s);
            sfx.bank("popper_shoot", 0.22f, 0.12f, 70, 1.7f);
        } else if (t == SNIPER) {
            tSqV[i] -= 10;
            float mx = sx + tFace[i] * 9 * u, my = muzzleY(i, t);
            P tr = add(TRACER, mx, my); if (tr != null) { tr.max = 0.16f; tr.hx[0] = tg.x; tr.hy[0] = cy(tg); }
            double d = towerDmg(i);
            fxBurst(tg.x, cy(tg), tg.r * 2.2f);
            hitNumber(tg.x, cy(tg) - tg.r, d);
            damage(tg, d, (tg.x - mx) * 0.6f, 0, true);
            trauma = Math.min(1, trauma + 0.1f);
            sfx.play(sfx.shoot, 0.5f, 0.55f);
            sfx.bank("pebble_hit", 0.5f, 0.05f, 40, 0.8f);
        } else if (t == MORTAR || t == BOMB) {
            tSqV[i] -= 10;
            Shot s = new Shot(t == BOMB ? 4 : 1, sx + tFace[i] * 3 * u, sy - 4 * u);
            s.tx = tg.x; s.ty = tg.y;
            s.ox = s.x; s.oy = s.y; s.dur = t == BOMB ? 1.05f : 0.9f; s.dmg = towerDmg(i);
            shots.add(s);
            if (!sfx.bank("mortar_launch", 0.6f, 0.06f, 60, t == BOMB ? 0.75f : 1f)) sfx.play(sfx.lob, 0.5f, 0.9f + rnd.nextFloat() * 0.2f);
        } else if (t == MAGE) {                                         // bubble pulse: slows everything around
            tSqV[i] -= 6;
            float rr = T_RANGE[MAGE] * u;
            P ring = add(RING, padX[i], padY[i]); if (ring != null) { ring.max = 0.55f; ring.sz = rr; ring.col = A7; }
            for (int k = 0; k < 4; k++) {
                P b = add(BUBBLE, padX[i] + tFace[i] * 7 * u, muzzleY(i, t));
                if (b == null) break;
                float a = rnd.nextFloat() * 6.28f, v = (8 + rnd.nextFloat() * 14) * u;
                b.vx = (float) Math.cos(a) * v; b.vy = (float) Math.sin(a) * v - 6 * u; b.max = 0.9f; b.sz = (1 + rnd.nextFloat() * 1.4f) * u;
            }
            double d = towerDmg(i);
            for (int k = enemies.size() - 1; k >= 0; k--) {
                Enemy e = enemies.get(k);
                if (Math.hypot(e.x - padX[i], e.y - padY[i]) < rr + e.r) { e.bubT = 1.3f; damage(e, d, 0, 0); }
            }
            sfx.bank("strike_hit", 0.3f, 0.1f, 120, 1.3f);
        } else if (t == LANTERN) {                                      // steady burn on everything nearby
            float rr = T_RANGE[LANTERN] * u;
            double d = towerDmg(i) / T_RATE[LANTERN];
            for (int k = enemies.size() - 1; k >= 0; k--) {
                Enemy e = enemies.get(k);
                if (e.emerge <= 0 && Math.hypot(e.x - padX[i], e.y - padY[i]) < rr + e.r) {
                    e.burnT = 0.4f;
                    damage(e, d, 0, 0);
                    if (rnd.nextFloat() < 0.35f) { P f = add(SPARK, e.x + (rnd.nextFloat() - 0.5f) * e.r, cy(e)); if (f != null) { f.vy = -25 * u; f.max = 0.4f; f.sz = 0.7f * u; f.col = A5; } }
                }
            }
        } else {                                              // WALL: slam shockwave
            tSqV[i] += 12;
            float rr = T_RANGE[WALL] * u;
            ring(padX[i], padY[i], A5, rr / (14 * u));
            for (int k = 0; k < 6; k++) dust(padX[i] + (rnd.nextFloat() - 0.5f) * 10 * u, padY[i]);
            trauma = Math.min(1, trauma + 0.08f);
            if (!sfx.bank("wall_slam", 0.75f, 0.06f, 80)) sfx.play(sfx.splat, 0.45f, 0.8f);
            double d = towerDmg(i);
            for (int k = enemies.size() - 1; k >= 0; k--) {
                Enemy e = enemies.get(k);
                if (flyer(e)) continue;
                float dx = e.x - padX[i], dy = e.y - padY[i], dist = (float) Math.max(1, Math.hypot(dx, dy));
                if (dist < rr + e.r) damage(e, d, dx / dist * 140 * u, dy / dist * 140 * u);
            }
        }
    }

    void fxBurst(float x, float y, float size) {
        P p = add(BURSTFX, x, y); if (p == null) return;
        p.max = 0.2f; p.sz = size; p.rot = rnd.nextFloat() * 360;
    }

    // ---------------------------------------------------------------- projectiles
    void updateShots(float dt) {
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot s = shots.get(i);
            s.age += dt;
            System.arraycopy(s.hx, 0, s.hx, 1, 5); System.arraycopy(s.hy, 0, s.hy, 1, 5);
            s.hx[0] = s.x; s.hy[0] = s.y;
            if (s.type == 0 || s.type == 2) {
                if (s.tg != null && !s.tg.dead) { s.tx = s.tg.x; s.ty = cy(s.tg); }
                float dx = s.tx - s.x, dy = s.ty - s.y, d = (float) Math.max(1, Math.hypot(dx, dy)), sp = (s.type == 2 ? 170 : 120) * u;
                if (d < sp * dt + 1.5f * u) {
                    shots.remove(i);
                    if (s.tg != null && !s.tg.dead) {
                        damage(s.tg, s.dmg, dx / d * (s.type == 2 ? 10 : 40) * u, dy / d * (s.type == 2 ? 10 : 40) * u, true);
                        burst(s.tx, s.ty, s.type == 2 ? 2 : 4, s.type == 2 ? A6 : A4, 0.6f);
                        if (s.type == 0) { sfx.bank("pebble_hit", 0.5f, 0.1f, 35); hitNumber(s.tx, s.ty, s.dmg); }
                    }
                    continue;
                }
                s.x += dx / d * sp * dt; s.y += dy / d * sp * dt;
            } else {
                float k = s.age / s.dur;
                s.x = lerp(s.ox, s.tx, k);
                s.y = lerp(s.oy, s.ty, k) - (float) Math.sin(k * Math.PI) * (s.type == 4 ? 34 : 26) * u;
                if (s.type == 4) {
                    if (rnd.nextFloat() < 0.6f) { P f = add(SPARK, s.x + 1.2f * u, s.y - 2.2f * u); if (f != null) { f.vy = -10 * u; f.max = 0.25f; f.sz = 0.5f * u; f.col = A6; } }
                    if (k >= 1) { shots.remove(i); explode(s); }
                    continue;
                }
                if (rnd.nextFloat() < 0.4f) { P g = add(GOO, s.x, s.y); if (g != null) { g.vy = 5 * u; g.max = 0.5f; g.sz = 0.7f * u; } }
                if (k >= 1) {
                    shots.remove(i);
                    float rr = 8 * u;
                    puddles.add(new float[]{s.tx, s.ty, rr, 2.2f});
                    for (int n = 0; n < 12; n++) { P g = add(GOO, s.tx, s.ty); if (g == null) break; float a = rnd.nextFloat() * 6.28f, v = (20 + rnd.nextFloat() * 30) * u; g.vx = (float) Math.cos(a) * v; g.vy = (float) Math.sin(a) * v - 25 * u; g.max = 0.6f; g.sz = (0.6f + rnd.nextFloat()) * u; }
                    if (!sfx.bank("goo_splat", 0.7f, 0.08f, 50)) sfx.play(sfx.splat, 0.5f, 1f + rnd.nextFloat() * 0.2f);
                    trauma = Math.min(1, trauma + 0.1f);
                    for (int n = enemies.size() - 1; n >= 0; n--) {
                        Enemy e = enemies.get(n);
                        if (!flyer(e) && Math.hypot(e.x - s.tx, e.y - s.ty) < rr + e.r) { e.slowT = 2f; damage(e, s.dmg, 0, 0); }
                    }
                    hitNumber(s.tx, s.ty - 3 * u, s.dmg);
                }
            }
        }
    }

    void explode(Shot s) {
        float rr = 14 * u;
        fxBurst(s.tx, s.ty - 3 * u, 30 * u);
        ring(s.tx, s.ty, A6, rr / (14 * u) * 1.2f);
        for (int k = 0; k < 10; k++) dust(s.tx + (rnd.nextFloat() - 0.5f) * 14 * u, s.ty);
        burst(s.tx, s.ty - 3 * u, 16, A6, 1.3f);
        trauma = Math.min(1, trauma + 0.35f); hitStop = Math.max(hitStop, 0.035f);
        if (!sfx.bank("wall_slam", 0.8f, 0.05f, 60, 0.85f)) sfx.play(sfx.splat, 0.6f, 0.7f);
        sfx.play(sfx.crit, 0.45f, 0.9f);
        for (int n = enemies.size() - 1; n >= 0; n--) {
            Enemy e = enemies.get(n);
            if (flyer(e)) continue;
            float dx = e.x - s.tx, dy = e.y - s.ty, dist = (float) Math.max(1, Math.hypot(dx, dy));
            if (dist < rr + e.r) damage(e, s.dmg, dx / dist * 90 * u, dy / dist * 90 * u);
        }
        hitNumber(s.tx, s.ty - 6 * u, s.dmg);
    }

    // ================================================================== input
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int a = ev.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) {
            int i = ev.getActionIndex();
            press(ev.getX(i), ev.getY(i));
            return true;
        }
        if (a == MotionEvent.ACTION_UP) performClick();
        return true;
    }

    @Override public boolean performClick() { return super.performClick(); }

    void pressed(int id) { pressId = id; pressT = 0.13f; }

    void click() { if (!sfx.bank("click", 0.6f, 0.04f, 20)) sfx.play(sfx.pop, 0.5f, 1.25f); performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); }

    void press(float x, float y) {
        if (overlay == 1) {
            if (hit(ovA, x, y)) { pressed(20); ads.showRewarded(rewardPlacement, () -> collectReward(rewardMult)); }
            else if (hit(ovB, x, y)) { pressed(21); collectReward(1); }
            return;
        }
        if (overlay == 2) {
            if (hit(ovA, x, y)) { pressed(20); ads.showRewarded(Ads.REVIVE, this::revive); }
            else if (hit(ovB, x, y)) { pressed(21); click(); retreat(); }
            return;
        }
        if (hit(soundBtn, x, y)) { sfx.on = !sfx.on; click(); save(); return; }
        if (hit(boostBtn, x, y)) {
            pressed(5);
            ads.showRewarded(Ads.DOUBLE_COINS, () -> {
                long now = System.currentTimeMillis();
                boostUntil = Math.min(Math.max(now, boostUntil) + 5 * 60_000L, now + 30 * 60_000L);
                flash(A6, 0.35f); sfx.play(sfx.buy, 0.8f, 1.2f); coinBumpV -= 8; save();
            });
            return;
        }
        panelGeom();
        if (selected >= 0 && hit(closeBtn, x, y)) { selected = -1; click(); sfx.bank("panel_close", 0.5f, 0, 50); return; }
        if (curPanel.contains(x, y)) { panelPress(x, y); return; }
        if (chestUp && Math.hypot(x - chestX, y - (chestY - 3 * u)) < 8 * u) {
            chestUp = false;
            double amt = Math.ceil(rewardScale(wave) * 30);
            sfx.play(sfx.star, 0.9f, 1f); flash(A6, 0.4f); trauma = Math.min(1, trauma + 0.3f);
            burst(chestX, chestY - 3 * u, 25, A6, 1.4f);
            showReward("Treasure!", "a chest full of coins", amt, Ads.CHEST, 3);
            return;
        }
        for (int i = 0; i < 6; i++) {
            if (Math.hypot(x - padX[i], y - (padY[i] - (tType[i] >= 0 ? 7 * u : 0))) < 11 * u) {
                selected = selected == i ? -1 : i;
                tSqV[i] -= 5;
                click();
                sfx.bank(selected < 0 ? "panel_close" : "panel_open", 0.45f, 0, 50);
                return;
            }
        }
        if (y > fieldTop - 6 * u && y < fieldBot) { if (selected >= 0) sfx.bank("panel_close", 0.4f, 0, 50); selected = -1; strike(x, y); }
    }

    boolean hit(RectF r, float x, float y) { return x > r.left - u && x < r.right + u && y > r.top - u && y < r.bottom + u; }

    void panelPress(float x, float y) {
        if (selected < 0) {
            if (hit(btnA, x, y)) buy(0, tapCost(), () -> tapLv++, btnA, 1);
            else if (hit(btnB, x, y)) buy(1, fortCost(), () -> { fortLv++; double f = baseHp / baseMax; baseMax = baseMaxFor(fortLv); baseHp = baseMax * f + baseMax * 0.2; baseHp = Math.min(baseHp, baseMax); }, btnB, 2);
        } else if (tType[selected] < 0) {
            for (int k = 0; k < nShown; k++) if (hit(cardR[k], x, y)) {
                final int tt = shown[k], s = selected;
                if (bestWave < T_UNLOCK[tt]) {
                    pressed(10 + k);
                    if (!sfx.bank("deny", 0.6f, 0, 120)) sfx.play(sfx.deny, 0.7f, 1f);
                    pop(cardR[k].centerX(), cardR[k].top, "reach wave " + T_UNLOCK[tt], A7, 3.6f);
                    return;
                }
                buy(10 + k, T_COST[tt], () -> { tType[s] = tt; tLv[s] = 0; tCool[s] = 0.5f; tutBuild = false; }, cardR[k], 10 + k, "tower_build");
                if (tType[s] >= 0) { tSqV[s] -= 14; ring(padX[s], padY[s], A6, 0.8f); for (int q = 0; q < 10; q++) dust(padX[s], padY[s]); }
                return;
            }
        } else if (hit(upBtn, x, y)) {
            final int s = selected;
            int before = tLv[s];
            buy(4, upCost(s), () -> tLv[s]++, upBtn, 4, "tower_upgrade");
            tSqV[s] -= 10;
            if (tLv[s] > before) { P a = add(ARROW, padX[s], padY[s] - 10 * u); if (a != null) { a.vy = -22 * u; a.max = 0.8f; a.sz = 6 * u; } }
        } else if (hit(sellBtn, x, y)) {
            int s = selected;
            double v = sellValue(s);
            pressed(6);
            coins += v; coinBumpV -= 6;
            P pf = add(POOF, padX[s], padY[s] - 8 * u); if (pf != null) { pf.max = 0.45f; pf.sz = 16 * u; }
            for (int k = 0; k < 5; k++) coinPart(padX[s], padY[s] - 8 * u, 1f);
            pop(padX[s], padY[s] - 16 * u, "+" + fmt(v), A6, 4.5f);
            tType[s] = -1; tLv[s] = 0;
            sfx.bank("panel_close", 0.5f, 0, 50);
            save();
        }
    }

    double sellValue(int i) {
        double spent = T_COST[tType[i]];
        for (int k = 1; k <= tLv[i]; k++) spent += Math.ceil(T_COST[tType[i]] * Math.pow(1.7, k));
        return Math.floor(spent * 0.5);
    }

    /** Panel rectangle for the current state; the build menu grows to fit unlocked towers. */
    void panelGeom() {
        RectF p = panelR;
        float in = 4 * u;
        if (selected >= 0 && tType[selected] < 0) {
            nShown = 0; boolean teaser = false;
            for (int t : DISPLAY) {
                if (bestWave >= T_UNLOCK[t]) shown[nShown++] = t;
                else if (!teaser) { shown[nShown++] = t; teaser = true; }   // one locked card shows what's next
            }
            gridCols = nShown <= 3 ? 3 : nShown <= 8 ? 4 : 5;
            int rows = (nShown + gridCols - 1) / gridCols;
            float gap = 2 * u, ch = rows == 1 ? 31 * u : 24.5f * u;
            float h = Math.max(panelR.height(), 9 * u + rows * ch + (rows - 1) * gap + in);
            buildR.set(panelR.left, panelR.bottom - h, panelR.right, panelR.bottom);
            float cw = (buildR.width() - 2 * in - (gridCols - 1) * gap) / gridCols;
            for (int k = 0; k < nShown; k++) {
                int row = k / gridCols, col = k % gridCols;
                float l = buildR.left + in + col * (cw + gap), t = buildR.top + 9 * u + row * (ch + gap);
                cardR[k].set(l, t, l + cw, t + ch);
            }
            p = buildR;
        }
        curPanel = p;
        closeBtn.set(p.right - 9.5f * u, p.top - 3.5f * u, p.right + 0.5f * u, p.top + 6.5f * u);
        float tx = panelR.left + in + 22 * u;
        sellBtn.set(tx, panelR.bottom - in - 10 * u, tx + 27 * u, panelR.bottom - in - 1 * u);
    }

    Bitmap[] cardBmp(float w, float h, int style) {
        String k = (int) w + "x" + (int) h + "s" + style;
        Bitmap[] b = btnCache.get(k);
        if (b == null) { b = art.button(w, h, style); btnCache.put(k, b); }
        return b;
    }

    Bitmap panelBmp(RectF r) {
        String k = (int) r.width() + "x" + (int) r.height();
        Bitmap b = panelCache.get(k);
        if (b == null) { b = art.panel(r.width(), r.height(), true, 3); panelCache.put(k, b); }
        return b;
    }

    void buy(int id, double cost, Runnable apply, RectF r, int pid) { buy(id, cost, apply, r, pid, null); }

    void buy(int id, double cost, Runnable apply, RectF r, int pid, String snd) {
        pressed(pid);
        if (coins < cost) {
            if (!sfx.bank("deny", 0.6f, 0, 120)) sfx.play(sfx.deny, 0.7f, 1f);
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            pop(r.centerX(), r.top, "need " + fmt(cost - coins) + " more", A7, 3.6f);
            return;
        }
        coins -= cost;
        apply.run();
        coinBumpV -= 6;
        burst(r.centerX(), r.top + 2 * u, 14, A6, 1f);
        if (snd == null || !sfx.bank(snd, 0.8f, 0.03f, 0)) sfx.play(sfx.buy, 0.8f, 1f);
        performHapticFeedback(Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.VIRTUAL_KEY);
        save();
    }

    /** The clicker half: squish the field where you tap. */
    void strike(float x, float y) {
        tutStrike = false;
        float rr = 8.5f * u;
        double d = tapDmg();
        ring(x, y, A6, 0.65f);
        burst(x, y, 7, A5, 0.8f);
        boolean any = false;
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (e.emerge > 0) continue;
            float dx = e.x - x, dy = cy(e) - y, dist = (float) Math.max(1, Math.hypot(dx, dy));
            if (dist < rr + e.r) { any = true; hitNumber(e.x, cy(e) - e.r * 1.2f, d); fxBurst(e.x, cy(e), e.r * 1.6f); damage(e, d, dx / dist * 60 * u, dy / dist * 60 * u); }
        }
        lookX = x; lookY = y;
        baseSqV += 2.5f;
        if (any) {
            trauma = Math.min(1, trauma + 0.12f);
            if (!sfx.bank("strike_hit", 0.7f, 0.06f, 30)) sfx.play(sfx.hit, 0.7f, 0.9f + rnd.nextFloat() * 0.25f);
            sfx.bank("pebble_hit", 0.35f, 0.15f, 30);
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } else {
            if (!sfx.bank("strike", 0.4f, 0.08f, 30)) sfx.play(sfx.pop, 0.4f, 0.9f + rnd.nextFloat() * 0.2f);
        }
    }

    void showReward(String title, String sub, double amt, String placement, int mult) {
        overlay = 1; rewardTitle = title; rewardSub = sub; rewardAmt = amt; rewardPlacement = placement; rewardMult = mult;
    }

    void collectReward(int mult) {
        double v = rewardAmt * mult;
        overlay = 0;
        gain(v, false);
        for (int i = 0; i < 24; i++) coinPart(W / 2 + (rnd.nextFloat() - 0.5f) * 40 * u, H * 0.48f, 1.4f);
        flash(A6, 0.4f); trauma = Math.min(1, trauma + 0.4f); baseHappyT = 1;
        pop(W / 2, H * 0.42f, "+" + fmt(v), A6, 9);
        sfx.play(sfx.levelUp, 0.8f, 1.2f);
        save();
    }

    // ================================================================== particles
    static final int SPARK = 0, COIN = 1, CONF = 2, RING = 3, DUST = 4, WISP = 5, GOO = 6, POOF = 7, BURSTFX = 8, ARROW = 9, TRACER = 10, BUBBLE = 11;

    static final class P {
        int t, col; float x, y, vx, vy, age, max, sz, rot, vr, delay;
        final float[] hx = new float[5], hy = new float[5];
    }

    static final class Pop { String s; int col; float x, y, vy, age, max, size; }

    static final class Enemy {
        int type; float x, y, r, spd, ph, hitT, slowT, bubT, burnT, abT = 1f, kx, ky, atkT = 0.4f, emerge; boolean atk, dead; double hp, max, dmg, reward;
        float spdX() { return 0; }
        float spdY() { return 0; }
    }

    static final class Shot {
        final int type; float x, y, tx, ty, ox, oy, age, dur; double dmg; Enemy tg;
        final float[] hx = new float[6], hy = new float[6];
        Shot(int type, float x, float y) { this.type = type; this.x = x; this.y = y; for (int i = 0; i < 6; i++) { hx[i] = x; hy[i] = y; } }
    }

    P add(int t, float x, float y) {
        if (parts.size() > 450 && t != COIN) return null;
        P p = new P(); p.t = t; p.x = x; p.y = y;
        for (int i = 0; i < 5; i++) { p.hx[i] = x; p.hy[i] = y; }
        parts.add(p);
        return p;
    }

    void burst(float x, float y, int n, int col, float power) {
        for (int i = 0; i < n; i++) {
            P p = add(SPARK, x, y); if (p == null) return;
            float a = rnd.nextFloat() * 6.2832f, s = (18 + rnd.nextFloat() * 50) * u * power;
            p.vx = (float) Math.cos(a) * s; p.vy = (float) Math.sin(a) * s - 12 * u;
            p.max = 0.3f + rnd.nextFloat() * 0.3f; p.sz = (0.5f + rnd.nextFloat() * 0.8f) * u; p.col = col;
        }
    }

    void coinPart(float x, float y, float power) {
        P p = add(COIN, x, y); if (p == null) return;
        float a = -1.5708f + (rnd.nextFloat() - 0.5f) * 2.4f, s = (25 + rnd.nextFloat() * 40) * u * power;
        p.vx = (float) Math.cos(a) * s; p.vy = (float) Math.sin(a) * s;
        p.delay = 0.25f + rnd.nextFloat() * 0.25f; p.max = 3f; p.sz = 1.5f * u; p.rot = rnd.nextFloat() * 6;
    }

    void confetti(float x, float y) {
        P p = add(CONF, x, y); if (p == null) return;
        float a = -1.5708f + (rnd.nextFloat() - 0.5f) * 2.2f, s = (45 + rnd.nextFloat() * 80) * u;
        p.vx = (float) Math.cos(a) * s; p.vy = (float) Math.sin(a) * s;
        p.max = 1.6f + rnd.nextFloat(); p.sz = (1 + rnd.nextFloat()) * u;
        int[] cols = {A4, A5, A6, A7};
        p.col = cols[rnd.nextInt(4)]; p.rot = rnd.nextFloat() * 360; p.vr = (rnd.nextFloat() - 0.5f) * 900;
    }

    void ring(float x, float y, int col, float size) { P p = add(RING, x, y); if (p == null) return; p.max = 0.35f; p.sz = 14 * u * size; p.col = col; }

    void dust(float x, float y) {
        P p = add(DUST, x, y); if (p == null) return;
        p.vx = (rnd.nextFloat() - 0.5f) * 40 * u; p.vy = -rnd.nextFloat() * 10 * u; p.max = 0.5f; p.sz = (1.2f + rnd.nextFloat() * 1.5f) * u; p.col = A3;
    }

    void wisps(float x, float y, int n) {
        for (int i = 0; i < n; i++) {
            P p = add(WISP, x + (rnd.nextFloat() - 0.5f) * 4 * u, y + (rnd.nextFloat() - 0.5f) * 4 * u); if (p == null) return;
            p.vx = (rnd.nextFloat() - 0.5f) * 25 * u; p.vy = -(10 + rnd.nextFloat() * 25) * u; p.max = 0.6f + rnd.nextFloat() * 0.4f;
            p.sz = (0.8f + rnd.nextFloat() * 1.4f) * u; p.col = rnd.nextBoolean() ? I4 : I5;
        }
    }

    void updateParts(float dt) {
        float tx = coinPill.left + 1 * u, ty = coinPill.centerY();
        for (int i = parts.size() - 1; i >= 0; i--) {
            P p = parts.get(i);
            p.age += dt;
            switch (p.t) {
                case SPARK: p.vy += 80 * u * dt; p.vx *= (1 - dt * 4); p.vy *= (1 - dt * 2); break;
                case GOO: p.vy += 110 * u * dt; break;
                case CONF: p.vy += 55 * u * dt; p.vx *= (1 - dt * 2.5f); p.vy *= (1 - dt * 2.5f); p.rot += p.vr * dt; p.vx += (float) Math.sin(p.age * 8 + p.rot) * 18 * u * dt; break;
                case DUST: p.vx *= (1 - dt * 5); p.vy *= (1 - dt * 5); break;
                case WISP: p.vx *= (1 - dt * 3); p.vy *= (1 - dt * 1.5f); break;
                case ARROW: p.vy *= (1 - dt * 2.5f); break;
                case BUBBLE: p.vx *= (1 - dt * 2); p.vy = p.vy * (1 - dt * 2) - 12 * u * dt; break;
                case COIN:
                    p.rot += dt * 12;
                    if (p.age < p.delay) { p.vy += 110 * u * dt; p.vx *= (1 - dt * 3); p.vy *= (1 - dt * 3); }
                    else {
                        float dx = tx - p.x, dy = ty - p.y, d = (float) Math.max(1, Math.hypot(dx, dy));
                        float sp = (90 + (p.age - p.delay) * 520) * u, k = Math.min(1, dt * 9);
                        p.vx += (dx / d * sp - p.vx) * k; p.vy += (dy / d * sp - p.vy) * k;
                        if (d < 3.5f * u) { parts.remove(i); coinBumpV += 3.5f; coinLand(); continue; }
                    }
                    System.arraycopy(p.hx, 0, p.hx, 1, 4); System.arraycopy(p.hy, 0, p.hy, 1, 4); p.hx[0] = p.x; p.hy[0] = p.y;
                    break;
            }
            p.x += p.vx * dt; p.y += p.vy * dt;
            if (p.age >= p.max) parts.remove(i);
        }
    }

    private long lastCoinLand; private int coinRun;

    /** GOAL-GRADIENT in sound: each coin landing in quick succession climbs the scale. */
    void coinLand() {
        long now = System.currentTimeMillis();
        if (now - lastCoinLand < 45) return;
        coinRun = now - lastCoinLand < 350 ? coinRun + 1 : 0;
        lastCoinLand = now;
        int n = Math.min(coinRun, 7);
        float rate = coinRun > 7 ? Math.min(1.25f, 1 + (coinRun - 7) * 0.02f) : 1;
        if (!sfx.nth("coin", n, 0.18f, rate)) sfx.play(sfx.coin, 0.22f, 0.95f + rnd.nextFloat() * 0.15f);
    }

    void pop(float x, float y, String s, int col, float size) {
        Pop p = new Pop();
        art.text.setTextSize(size * u);
        float half = art.text.measureText(s) / 2 + 2 * u;
        p.s = s; p.col = col; p.x = clamp(x, safeL + half, W - safeR - half); p.y = Math.max(y, fieldTop); p.vy = -30 * u; p.max = 0.85f; p.size = size * u;
        pops.add(p);
        if (pops.size() > 36) pops.remove(0);
    }

    void hitNumber(float x, float y, double d) { pop(x + (rnd.nextFloat() - 0.5f) * 3 * u, y, fmt(d), A7, 3.4f); }

    void flash(int col, float a) { flashCol = col; flashA = Math.max(flashA, a); }

    void buzz(long[] pattern) {
        if (vib == null || !vib.hasVibrator()) return;
        try { vib.vibrate(VibrationEffect.createWaveform(pattern, -1)); } catch (Exception ignored) { }
    }

    // ================================================================== render
    final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    void render(Canvas c) {
        if (bField == null) return;
        panelGeom();
        c.save();
        c.translate(shakeX, shakeY);
        c.drawBitmap(bField, 0, 0, null);
        drawWarmLight(c);
        for (float[] q : puddles) {
            if (art.puddle != null) {
                float pw = q[2] * 2.3f, ph = pw * art.puddle.getHeight() / art.puddle.getWidth();
                bmp.setAlpha((int) (235 * Math.min(1, q[3] / 0.6f)));
                tmp.set(q[0] - pw / 2, q[1] - ph / 2, q[0] + pw / 2, q[1] + ph / 2); c.drawBitmap(art.puddle, null, tmp, bmp);
                bmp.setAlpha(255);
                continue;
            }
            fill.setShader(null); fill.setColor(A5); fill.setAlpha((int) (150 * Math.min(1, q[3] / 0.6f)));
            tmp.set(q[0] - q[2], q[1] - q[2] * 0.45f, q[0] + q[2], q[1] + q[2] * 0.45f); c.drawOval(tmp, fill);
            fill.setColor(A6); fill.setAlpha((int) (110 * Math.min(1, q[3] / 0.6f)));
            tmp.set(q[0] - q[2] * 0.5f, q[1] - q[2] * 0.25f, q[0] + q[2] * 0.2f, q[1]); c.drawOval(tmp, fill);
        }
        for (int i = 0; i < 6; i++) if (tType[i] == LANTERN) {                // warm lantern light on the ground
            float rr = T_RANGE[LANTERN] * u * (1 + 0.04f * (float) Math.sin(time * 9 + i) + 0.03f * (float) Math.sin(time * 23));
            glowPaint.setShader(new RadialGradient(padX[i], padY[i] - 4 * u, rr, new int[]{0x55FFB13B, 0x22FF9F1C, 0x00FF9F1C}, new float[]{0, 0.6f, 1}, Shader.TileMode.CLAMP));
            c.drawCircle(padX[i], padY[i] - 4 * u, rr, glowPaint);
        }
        if (selected >= 0 && tType[selected] >= 0 && T_RANGE[tType[selected]] > 0) {
            float rr = T_RANGE[tType[selected]] * u;
            stroke.setPathEffect(new DashPathEffect(new float[]{2 * u, 1.5f * u}, time * 6 * u));
            stroke.setColor(A6); stroke.setAlpha(170); stroke.setStrokeWidth(0.5f * u);
            c.drawCircle(padX[selected], padY[selected], rr, stroke);
            stroke.setPathEffect(null);
            fill.setColor(A6); fill.setAlpha(25); c.drawCircle(padX[selected], padY[selected], rr, fill);
        }
        for (int i = 0; i < 6; i++) drawPad(c, i);

        // depth-sorted actors: towers, base, enemies, chest
        int n = enemies.size();
        float[] ys = new float[n + 8]; int[] ids = new int[n + 8]; int k = 0;
        for (int i = 0; i < 6; i++) if (tType[i] >= 0) { ys[k] = padY[i]; ids[k++] = 1000 + i; }
        ys[k] = baseY + baseR * 0.85f; ids[k++] = 2000;
        if (chestUp) { ys[k] = chestY; ids[k++] = 3000; }
        for (int i = 0; i < n; i++) { ys[k] = enemies.get(i).y; ids[k++] = i; }
        for (int a = 1; a < k; a++) { float y = ys[a]; int id = ids[a]; int b = a - 1; while (b >= 0 && ys[b] > y) { ys[b + 1] = ys[b]; ids[b + 1] = ids[b]; b--; } ys[b + 1] = y; ids[b + 1] = id; }
        for (int a = 0; a < k; a++) {
            int id = ids[a];
            if (id >= 3000) drawChest(c);
            else if (id >= 2000) drawBase(c);
            else if (id >= 1000) drawTower(c, id - 1000);
            else drawEnemy(c, enemies.get(id));
        }
        for (Shot s : shots) drawShot(c, s);
        for (P p : parts) if (p.t != COIN) drawPart(c, p);
        drawTutorial(c);
        drawBanner(c);
        c.restore();

        drawHud(c);
        drawPanel(c);
        for (P p : parts) if (p.t == COIN) drawPart(c, p);
        for (Pop p : pops) {
            float s = p.age < 0.15f ? easeOutBack(p.age / 0.15f) : 1;
            int alpha = (int) (255 * Math.min(1, (1 - p.age / p.max) * 2.5f));
            art.text.setAlpha(alpha);
            drawLabelAlpha(c, p.s, p.x + shakeX, p.y + shakeY, p.size * s, p.col, alpha);
        }
        if (flashA > 0) { fill.setShader(null); fill.setColor(flashCol); fill.setAlpha((int) (flashA * 170)); c.drawRect(0, 0, W, H, fill); }
        if (overlay != 0) drawOverlay(c);
    }

    void drawLabelAlpha(Canvas c, String s, float x, float y, float size, int col, int alpha) {
        if (alpha >= 250) { art.label(c, s, x, y, size, col); return; }
        c.saveLayerAlpha(x - size * s.length(), y - size * 1.4f, x + size * s.length(), y + size * 0.6f, alpha);
        art.label(c, s, x, y, size, col);
        c.restore();
    }

    void drawWarmLight(Canvas c) {
        float pulse = 1 + 0.04f * (float) Math.sin(time * 2);
        float rr = 52 * u * pulse;
        fill.setColor(0xFFFFFFFF);
        fill.setShader(new RadialGradient(baseX, baseY, rr, new int[]{0x66FF9F1C, 0x22E08A2E, 0x00E08A2E}, new float[]{0, 0.55f, 1}, Shader.TileMode.CLAMP));
        c.drawCircle(baseX, baseY, rr, fill);
        fill.setShader(null);
    }

    void drawPad(Canvas c, int i) {
        if (art.groundBaked != null) {                     // stump is painted into the background
            if (selected == i) {
                float w = 19 * u;
                fill.setShader(null); fill.setColor(A6); fill.setAlpha((int) (90 + 60 * Math.sin(time * 6)));
                tmp.set(padX[i] - w * 0.5f, padY[i] - u - w * 0.2f, padX[i] + w * 0.5f, padY[i] - u + w * 0.2f); c.drawOval(tmp, fill);
            }
        } else if (art.stump != null) {
            float w = 19 * u, h = w * art.stump.getHeight() / art.stump.getWidth();
            float face = padY[i] - 1f * u, top = face - h * 0.17f;
            softShadow(c, padX[i] + w * 0.07f, top + h * 0.87f, w * 0.62f, w * 0.14f, 0.6f);   // ambient, cast lower-right
            softShadow(c, padX[i] + w * 0.03f, top + h * 0.9f, w * 0.44f, w * 0.06f, 0.75f);  // contact
            if (selected == i) {
                fill.setColor(A6); fill.setAlpha((int) (110 + 70 * Math.sin(time * 6)));
                tmp.set(padX[i] - w * 0.62f, face - w * 0.3f, padX[i] + w * 0.62f, face + w * 0.3f); c.drawOval(tmp, fill);
            }
            tmp.set(padX[i] - w / 2, top, padX[i] + w / 2, top + h);
            c.drawBitmap(art.stump, null, tmp, bmp);
        } else {
            float w = bPad.getWidth(), h = bPad.getHeight();
            float top = padY[i] - h * 0.32f;
            if (selected == i) {
                fill.setColor(A6); fill.setAlpha((int) (90 + 60 * Math.sin(time * 6)));
                tmp.set(padX[i] - w * 0.58f, padY[i] - h * 0.42f, padX[i] + w * 0.58f, padY[i] + h * 0.5f); c.drawOval(tmp, fill);
            }
            c.drawBitmap(bPad, padX[i] - w / 2, top, bmp);
        }
        if (tType[i] < 0 && art.uiPlus != null) {
            float pulse = 1 + 0.08f * (float) Math.sin(time * 4 + i);
            float y = padY[i] - 2.4f * u - (float) Math.abs(Math.sin(time * 3 + i)) * 0.8f * u;
            if (coins < T_COST[0]) bmp.setAlpha(150);
            art.icon(c, art.uiPlus, padX[i], y, 6.2f * u * pulse);
            bmp.setAlpha(255);
        } else if (tType[i] < 0) {
            float pulse = 1 + 0.08f * (float) Math.sin(time * 4 + i);
            float y = padY[i] - 2.4f * u;
            fill.setColor(A0); fill.setAlpha(160); c.drawCircle(padX[i], y + 0.3f * u, 2.6f * u * pulse, fill);
            fill.setColor(coins >= T_COST[0] ? A6 : A3); fill.setAlpha(255); c.drawCircle(padX[i], y, 2.4f * u * pulse, fill);
            stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(0.5f * u); c.drawCircle(padX[i], y, 2.4f * u * pulse, stroke);
            stroke.setStrokeWidth(0.7f * u);
            c.drawLine(padX[i] - 1.1f * u, y, padX[i] + 1.1f * u, y, stroke);
            c.drawLine(padX[i], y - 1.1f * u, padX[i], y + 1.1f * u, stroke);
        }
    }

    void drawTower(Canvas c, int i) {
        int t = tType[i];
        Bitmap b = art.tower(t);
        float hgt = T_H[t] * u, wid = hgt * b.getWidth() / b.getHeight();
        if (wid > 24 * u) { hgt *= 24 * u / wid; wid = 24 * u; }
        float idle = (float) Math.sin(time * 3 + i) * 0.025f;
        float wind = tWind[i] * (t == WALL || t == BANKER ? -0.12f : 0.14f);   // wall & banker rise before acting
        float sq = clamp(tSq[i] * 0.05f + wind, -0.25f, 0.25f) + idle;
        float sy = 1 - sq, sx = 1 + sq * 0.7f;
        float baseLine = padY[i] - 1.5f * u;
        softShadow(c, padX[i] + 0.6f * u, baseLine + 0.2f * u, wid * 0.42f * sx, 1.6f * u, 0.5f);
        c.save();
        c.translate(padX[i], baseLine);
        c.scale(sx * (T_FLIP[t] == 0 ? 1 : T_FLIP[t] * tFace[i]), sy);
        tmp.set(-wid / 2, -hgt, wid / 2, 0);
        c.drawBitmap(b, null, tmp, bmp);
        c.restore();
        if (tBuff[i] > 1.01f) {                                              // drummer's rhythm: small note pip
            float k = (time * 2 + i * 0.3f) % 1f;
            fill.setShader(null); fill.setColor(A6); fill.setAlpha((int) (200 * (1 - k)));
            float nx = padX[i] - wid * 0.38f, ny = baseLine - hgt * 0.85f - k * 3 * u;
            c.drawCircle(nx, ny, 0.8f * u, fill);
            stroke.setColor(A6); stroke.setAlpha((int) (200 * (1 - k))); stroke.setStrokeWidth(0.35f * u);
            c.drawLine(nx + 0.7f * u, ny, nx + 0.7f * u, ny - 2.4f * u, stroke);
        }
        if (tLv[i] > 0 && art.uiBadge != null) {
            float bx = padX[i] + wid * 0.36f, by = baseLine - 1.2f * u;
            art.icon(c, art.uiBadge, bx, by, 5.2f * u);
            art.label(c, String.valueOf(tLv[i] + 1), bx, by + 1.0f * u, 2.9f * u, A7);
        } else if (tLv[i] > 0) {
            float bx = padX[i] + wid * 0.36f, by = baseLine - 1.2f * u;
            fill.setColor(A0); c.drawCircle(bx, by, 2.4f * u, fill);
            fill.setColor(A6); c.drawCircle(bx, by - 0.2f * u, 2.0f * u, fill);
            art.ink(c, String.valueOf(tLv[i] + 1), bx, by + 0.75f * u, 2.6f * u, A0, Paint.Align.CENTER);
        }
    }

    void drawBase(Canvas c) {
        float r = baseR;
        float s = clamp(baseSq * 0.06f, -0.3f, 0.3f), br = (float) Math.sin(time * 2.2f) * 0.02f;
        float sy = 1 - s + br, sx = 1 + s * 0.8f - br;
        float gy = baseY + r * 0.85f;
        softShadow(c, baseX + r * 0.08f, gy, r * 1.25f * sx, r * 0.3f, 0.6f);
        if (art.king != null) {
            boolean hurt = baseHurtT > 0, happy = baseHappyT > 0 && !hurt;
            Bitmap k = hurt && art.kingHurt != null ? art.kingHurt : happy && art.kingHappy != null ? art.kingHappy : art.king;
            float kh = r * 2.75f, kw = kh * k.getWidth() / k.getHeight();
            c.save();
            c.translate(baseX, gy + r * 0.05f);
            c.scale(sx, sy);                                   // squash & stretch from the feet
            if (hurt) c.translate((float) Math.sin(time * 70) * r * 0.04f, 0);   // flinch shake
            tmp.set(-kw / 2, -kh, kw / 2, 0);
            c.drawBitmap(k, null, tmp, bmp);
            if (baseHp < baseMax * 0.3 && ((int) (time * 3)) % 2 == 0) c.drawBitmap(k, null, tmp, lowHpPaint);   // danger pulse
            c.restore();
        } else {
        c.save();
        c.translate(baseX, gy);
        c.scale(sx * r, sy * r);
        float lw = 0.11f;
        // feet
        for (int k = -1; k <= 1; k += 2) {
            tmp.set(k * 0.55f - 0.3f, -0.32f, k * 0.55f + 0.3f, 0.02f);
            fill.setColor(0xFFD9640C); c.drawOval(tmp, fill);
            stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(lw); c.drawOval(tmp, stroke);
        }
        // body
        tmp.set(-1.05f, -1.85f, 1.05f, -0.12f);
        fill.setShader(new RadialGradient(-0.35f, -1.45f, 1.6f, new int[]{0xFFFFD36B, 0xFFFF9F1C, 0xFFE07010, 0xFFB5520A}, new float[]{0, 0.4f, 0.8f, 1}, Shader.TileMode.CLAMP));
        c.drawOval(tmp, fill); fill.setShader(null);
        stroke.setStrokeWidth(lw); c.drawOval(tmp, stroke);
        fill.setColor(0xFFFFFFFF); fill.setAlpha(150);
        c.save(); c.rotate(-25, -0.5f, -1.45f); tmp.set(-0.8f, -1.62f, -0.25f, -1.3f); c.drawOval(tmp, fill); c.restore();
        fill.setAlpha(200); c.drawCircle(-0.15f, -1.6f, 0.06f, fill);
        // cheeks
        fill.setColor(0xFFFF7A3D); fill.setAlpha(120);
        tmp.set(-0.82f, -0.95f, -0.48f, -0.72f); c.drawOval(tmp, fill); tmp.set(0.48f, -0.95f, 0.82f, -0.72f); c.drawOval(tmp, fill);
        // eyes look toward the threat
        float lx = lookX - baseX, ly = lookY - baseY, ld = (float) Math.max(1, Math.hypot(lx, ly));
        float ex = lx / ld * 0.07f, ey = ly / ld * 0.06f;
        boolean hurt = baseHurtT > 0, happy = baseHappyT > 0 && !hurt, worried = baseHp < baseMax * 0.3 && !hurt && !happy;
        stroke.setColor(A0);
        for (int k = -1; k <= 1; k += 2) {
            float cx = k * 0.36f, cy = -1.15f;
            if (hurt) {
                stroke.setStrokeWidth(0.09f);
                c.drawLine(cx - 0.13f, cy - 0.1f * k * -1, cx + 0.13f, cy, stroke);
                c.drawLine(cx - 0.13f, cy + 0.1f * k * -1 + 0.0f, cx + 0.13f, cy, stroke);
            } else if (happy) {
                stroke.setStrokeWidth(0.09f);
                path.reset(); path.moveTo(cx - 0.15f, cy + 0.05f); path.quadTo(cx, cy - 0.18f, cx + 0.15f, cy + 0.05f); c.drawPath(path, stroke);
            } else {
                tmp.set(cx - 0.19f, cy - 0.23f, cx + 0.19f, cy + 0.23f);
                fill.setColor(0xFFFFFFFF); fill.setAlpha(255); c.drawOval(tmp, fill);
                stroke.setStrokeWidth(0.06f); c.drawOval(tmp, stroke);
                fill.setColor(0xFF3A1A08);
                tmp.set(cx + ex - 0.12f, cy + ey - 0.15f, cx + ex + 0.12f, cy + ey + 0.15f); c.drawOval(tmp, fill);
                fill.setColor(0xFFFFFFFF);
                c.drawCircle(cx + ex + 0.04f, cy + ey - 0.06f, 0.045f, fill);
                c.drawCircle(cx + ex - 0.04f, cy + ey + 0.06f, 0.022f, fill);
                if (worried) { stroke.setStrokeWidth(0.08f); c.drawLine(cx - 0.18f * k, cy - 0.36f, cx + 0.14f * k, cy - 0.28f, stroke); }
            }
        }
        // mouth
        stroke.setStrokeWidth(0.08f);
        if (hurt || worried) {
            path.reset(); path.moveTo(-0.14f, -0.6f); path.quadTo(0, -0.72f, 0.14f, -0.6f); c.drawPath(path, stroke);
        } else {
            path.reset(); path.moveTo(-0.2f, -0.74f); path.quadTo(0, -0.42f, 0.2f, -0.74f); path.close();
            fill.setColor(0xFF6B2A0A); c.drawPath(path, fill);
            c.drawPath(path, stroke);
        }
        // crown
        path.reset();
        path.moveTo(-0.5f, -1.72f); path.lineTo(-0.58f, -2.25f); path.lineTo(-0.27f, -2.0f); path.lineTo(0, -2.4f);
        path.lineTo(0.27f, -2.0f); path.lineTo(0.58f, -2.25f); path.lineTo(0.5f, -1.72f); path.close();
        fill.setColor(0xFFFFFFFF);
        fill.setShader(new RadialGradient(-0.2f, -2.2f, 0.8f, A7, A5, Shader.TileMode.CLAMP)); c.drawPath(path, fill); fill.setShader(null);
        stroke.setStrokeWidth(0.08f); c.drawPath(path, stroke);
        fill.setColor(A0); fill.setAlpha(255);
        for (int k = -1; k <= 1; k++) { c.drawCircle(k * 0.27f, -1.86f, 0.075f, fill); }
        fill.setColor(A7); for (int k = -1; k <= 1; k++) c.drawCircle(k * 0.27f, -1.86f, 0.05f, fill);
        c.restore();

        }
        // hp bar above the crown
        float bw = bHpBar.getWidth(), bh = bHpBar.getHeight();
        float bx = baseX - bw / 2, by = baseY - r * (art.king != null ? 2.25f : 1.95f);
        c.drawBitmap(bHpBar, bx, by, null);
        tmp2.set(bx + 0.8f * u, by + 0.8f * u, bx + bw - 0.8f * u, by + bh - 0.8f * u);
        float f = (float) (baseHp / baseMax);
        boolean low = f < 0.3f && ((int) (time * 6)) % 2 == 0;
        art.barFill(c, tmp2, f, low ? A7 : A6, low ? A5 : 0xFFF29A1E);
    }

    void drawEnemy(Canvas c, Enemy e) {
        float r = e.r;
        float bob = (float) Math.sin(e.ph) * 0.08f;
        float lunge = e.atk ? (float) Math.max(0, Math.sin((0.9f - e.atkT) / 0.9f * Math.PI)) : 0;
        float sy = 1 + bob - lunge * 0.15f, sx = 1 - bob * 0.7f + lunge * 0.12f;
        if (e.emerge > 0) {
            float k = 1 - e.emerge / 0.55f;
            fill.setShader(null); fill.setColor(I0); fill.setAlpha(220);
            tmp.set(e.x - r * 1.3f, e.y - r * 0.35f, e.x + r * 1.3f, e.y + r * 0.35f); c.drawOval(tmp, fill);
            stroke.setColor(I4); stroke.setAlpha(200); stroke.setStrokeWidth(0.4f * u); c.drawOval(tmp, stroke);
            sy *= easeOutBack(k) * 0.9f + 0.05f; sx *= 0.7f + 0.3f * k;
        }
        float hop = flyer(e) ? 0 : (float) Math.abs(Math.sin(e.ph));
        float lf = lift(e);
        softShadow(c, e.x + r * 0.08f, e.y + r * 0.08f, r * (1.1f - hop * 0.2f) * sx * (lf > 0 ? 0.7f : 1), r * 0.3f, (0.55f - hop * 0.15f) * (lf > 0 ? 0.6f : 1));
        float face = baseX - e.x > 0 ? 1 : -1;
        float lx = (baseX - e.x) * 0.0f + face * 0.12f;
        c.save();
        c.translate(e.x + (baseX - e.x) * lunge * 0.06f, e.y - hop * r * 0.12f - lf);
        if (e.type == FLAPPER) c.scale(1, 1 + 0.06f * (float) Math.sin(time * 18 + e.ph));   // wing beat
        c.scale(sx * r, sy * r);
        Bitmap spr = art.enemy(e.type);
        if (spr != null) {
            float hh = E_HH[e.type], ww = hh * spr.getWidth() / spr.getHeight();
            float flip = E_FLIP[e.type] == 0 ? 1 : E_FLIP[e.type] * face;         // turn toward the king
            c.save(); c.scale(flip, 1);
            tmp.set(-ww / 2, -hh + 0.14f, ww / 2, 0.14f);
            c.drawBitmap(spr, null, tmp, bmp);
            if (e.burnT > 0 && ((int) (time * 14 + e.ph)) % 2 == 0) c.drawBitmap(spr, null, tmp, burnPaint);
            if (e.hitT > 0) c.drawBitmap(spr, null, tmp, flashPaint);          // HIT FLASH
            c.restore();
            if (e.bubT > 0) {                                                    // trapped in a bubble
                float a = Math.min(1, e.bubT / 0.3f);
                fill.setColor(A7); fill.setAlpha((int) (45 * a)); c.drawCircle(0, -hh * 0.45f, hh * 0.62f, fill);
                stroke.setColor(A7); stroke.setAlpha((int) (170 * a)); stroke.setStrokeWidth(0.07f); c.drawCircle(0, -hh * 0.45f, hh * 0.62f, stroke);
                fill.setColor(0xFFFFFFFF); fill.setAlpha((int) (180 * a)); c.drawCircle(-hh * 0.25f, -hh * 0.75f, 0.12f, fill);
            }
            if (e.slowT > 0) {                                                  // amber goo stuck on it
                fill.setColor(A5); fill.setAlpha(220);
                c.drawCircle(-0.4f, -1.85f, 0.28f, fill); c.drawCircle(0.1f, -2.0f, 0.22f, fill); c.drawCircle(0.5f, -1.8f, 0.2f, fill);
                float drip = (time * 0.8f + e.ph) % 1f;
                c.drawCircle(-0.4f, -1.6f + drip * 0.5f, 0.09f, fill);
            }
        } else {
            // ghost body with a wavy skirt
            path.reset();
            path.moveTo(-1, -1);
            path.cubicTo(-1, -2.15f, 1, -2.15f, 1, -1);
            path.lineTo(1, -0.15f);
            int waves = 4;
            for (int k = 0; k < waves; k++) {
                float x0 = 1 - k * 2f / waves, x1 = x0 - 1f / waves, x2 = x0 - 2f / waves;
                float wv = (float) Math.sin(e.ph * 1.5f + k) * 0.08f;
                path.quadTo(x1, -0.45f + wv, x1, -0.3f);
                path.quadTo(x1, 0.05f + wv, x2, -0.15f);
            }
            path.close();
            fill.setColor(0xFFFFFFFF);
            fill.setShader(new RadialGradient(-0.4f, -1.55f, 1.9f, new int[]{I5, I4, I3, I2}, new float[]{0, 0.35f, 0.75f, 1}, Shader.TileMode.CLAMP));
            c.drawPath(path, fill); fill.setShader(null);
            if (e.type == BRUTE || e.type == BOSS) {                 // horns
                for (int k = -1; k <= 1; k += 2) {
                    Path h = new Path();
                    h.moveTo(k * 0.45f, -1.65f); h.quadTo(k * 0.95f, -2.1f, k * 0.7f, -2.6f); h.quadTo(k * 0.6f, -2.0f, k * 0.15f, -1.82f); h.close();
                    fill.setColor(I1); c.drawPath(h, fill);
                    stroke.setColor(I0); stroke.setStrokeWidth(0.1f); c.drawPath(h, stroke);
                }
            }
            if (e.type == SKITTER) {
                for (int k = -2; k <= 2; k++) {
                    Path h = new Path();
                    h.moveTo(k * 0.3f - 0.14f, -1.75f + Math.abs(k) * 0.12f); h.lineTo(k * 0.38f, -2.25f + Math.abs(k) * 0.18f); h.lineTo(k * 0.3f + 0.14f, -1.75f + Math.abs(k) * 0.12f); h.close();
                    fill.setColor(I2); c.drawPath(h, fill); stroke.setColor(I0); stroke.setStrokeWidth(0.09f); c.drawPath(h, stroke);
                }
            }
            stroke.setColor(I0); stroke.setAlpha(255); stroke.setStrokeWidth(0.12f);
            c.drawPath(path, stroke);
            fill.setColor(I6); fill.setAlpha(110);
            c.save(); c.rotate(-25, -0.5f, -1.5f); tmp.set(-0.8f, -1.75f, -0.3f, -1.45f); c.drawOval(tmp, fill); c.restore();
            // glowing eyes
            int eyes = e.type == BOSS ? 3 : 2;
            for (int k = 0; k < eyes; k++) {
                float ex = (eyes == 2 ? (k == 0 ? -0.36f : 0.36f) : (k - 1) * 0.42f) + lx, ey = eyes == 3 && k == 1 ? -1.38f : -1.15f;
                fill.setColor(I5); fill.setAlpha(110); c.drawCircle(ex, ey, 0.26f, fill);
                fill.setColor(I6); fill.setAlpha(255);
                tmp.set(ex - 0.15f, ey - 0.11f, ex + 0.15f, ey + 0.11f); c.drawOval(tmp, fill);
                fill.setColor(I0); tmp.set(ex - 0.045f + face * 0.04f, ey - 0.09f, ex + 0.045f + face * 0.04f, ey + 0.09f); c.drawOval(tmp, fill);
            }
            // jagged mouth
            path.reset();
            path.moveTo(-0.3f + lx, -0.72f);
            for (int k = 0; k <= 6; k++) path.lineTo(-0.3f + lx + k * 0.1f, k % 2 == 0 ? -0.72f : -0.62f - (e.atk ? 0.1f : 0));
            path.lineTo(0.3f + lx, -0.55f - (e.atk ? 0.15f : 0)); path.lineTo(-0.3f + lx, -0.55f - (e.atk ? 0.15f : 0)); path.close();
            fill.setColor(I0); c.drawPath(path, fill);
            if (e.slowT > 0) {                                      // amber goo stuck on it
                fill.setColor(A5); fill.setAlpha(220);
                c.drawCircle(-0.4f, -1.75f, 0.28f, fill); c.drawCircle(0.1f, -1.9f, 0.22f, fill); c.drawCircle(0.5f, -1.7f, 0.2f, fill);
                float drip = (time * 0.8f + e.ph) % 1f;
                c.drawCircle(-0.4f, -1.5f + drip * 0.5f, 0.09f, fill);
            }
            if (e.hitT > 0) {                                       // hit flash
                fill.setColor(0xFFFFFFFF); fill.setAlpha(200); c.drawPath(path, fill);
                c.save(); c.clipRect(-1.2f, -2.3f, 1.2f, 0.2f);
                Path body = new Path(); body.addCircle(0, -1.1f, 1.02f, Path.Direction.CW); c.drawPath(body, fill); c.restore();
            }
        }
        c.restore();
        if (e.hp < e.max) {
            float bw = Math.max(5 * u, r * 1.6f), bh = 0.9f * u, bx = e.x - bw / 2, by = e.y - r * 2.7f - lf;
            fill.setColor(I0); fill.setAlpha(220); tmp.set(bx - 0.25f * u, by - 0.25f * u, bx + bw + 0.25f * u, by + bh + 0.25f * u); c.drawRoundRect(tmp, u, u, fill);
            fill.setColor(I6); fill.setAlpha(255); tmp.set(bx, by, bx + bw * (float) Math.max(0, e.hp / e.max), by + bh); c.drawRoundRect(tmp, u, u, fill);
        }
    }

    void drawShot(Canvas c, Shot s) {
        if (s.type == 2) {                                                   // spark
            stroke.setColor(A6);
            for (int i = 5; i > 0; i--) { stroke.setAlpha((int) (200 * (1 - i / 6f))); stroke.setStrokeWidth(1.0f * u * (1 - i / 6f)); c.drawLine(s.hx[i], s.hy[i], s.hx[i - 1], s.hy[i - 1], stroke); }
            fill.setShader(null); fill.setColor(A7); fill.setAlpha(255); c.drawCircle(s.x, s.y, 0.75f * u, fill);
            fill.setColor(A6); fill.setAlpha(120); c.drawCircle(s.x, s.y, 1.5f * u, fill);
            return;
        }
        if (s.type == 4) {                                                   // bomb: shadow, body, fuse spark
            float k = s.age / s.dur, gx = lerp(s.ox, s.tx, k), gy = lerp(s.oy + 9 * u, s.ty, k);
            softShadow(c, gx, gy, (1 + k) * 1.6f * u, (1 + k) * 0.6f * u, 0.35f + 0.3f * k);
            c.save(); c.translate(s.x, s.y); c.rotate(s.age * 380);
            fill.setShader(null); fill.setColor(0xFF4A3A14); fill.setAlpha(255); c.drawCircle(0, 0, 2.2f * u, fill);
            fill.setColor(0xFF8C7A3A); c.drawCircle(-0.6f * u, -0.7f * u, 0.8f * u, fill);
            stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(0.4f * u); c.drawCircle(0, 0, 2.2f * u, stroke);
            fill.setColor(0xFF6B5420); tmp.set(-0.7f * u, -3f * u, 0.7f * u, -1.8f * u); c.drawRect(tmp, fill); c.drawRect(tmp, stroke);
            c.restore();
            return;
        }
        if (s.type == 0) {
            stroke.setColor(A6);
            for (int i = 5; i > 0; i--) { stroke.setAlpha((int) (120 * (1 - i / 6f))); stroke.setStrokeWidth(1.4f * u * (1 - i / 6f)); c.drawLine(s.hx[i], s.hy[i], s.hx[i - 1], s.hy[i - 1], stroke); }
            if (art.pebble != null) {
                c.save(); c.translate(s.x, s.y); c.rotate(s.age * 900);
                tmp.set(-1.4f * u, -1.3f * u, 1.4f * u, 1.3f * u); c.drawBitmap(art.pebble, null, tmp, bmp); c.restore();
            } else {
                fill.setColor(A1); c.drawCircle(s.x, s.y, 1.1f * u, fill);
                fill.setColor(A2); c.drawCircle(s.x - 0.2f * u, s.y - 0.2f * u, 0.8f * u, fill);
                stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(0.3f * u); c.drawCircle(s.x, s.y, 1.1f * u, stroke);
            }
        } else {
            float k = s.age / s.dur, gx = lerp(s.ox, s.tx, k), gy = lerp(s.oy + 9 * u, s.ty, k);
            fill.setColor(0xFF000000); fill.setAlpha((int) (40 + 60 * k));
            float sr = (1 + k) * 1.4f * u; tmp.set(gx - sr, gy - sr * 0.4f, gx + sr, gy + sr * 0.4f); c.drawOval(tmp, fill);
            stroke.setColor(A5);
            for (int i = 5; i > 0; i--) { stroke.setAlpha((int) (140 * (1 - i / 6f))); stroke.setStrokeWidth(2.2f * u * (1 - i / 6f)); c.drawLine(s.hx[i], s.hy[i], s.hx[i - 1], s.hy[i - 1], stroke); }
            if (art.gooBall != null) {
                float ang = (float) Math.toDegrees(Math.atan2(s.y - s.hy[2], s.x - s.hx[2]));
                c.save(); c.translate(s.x, s.y); c.rotate(ang + 180);
                float gw = 5.2f * u, gh = gw * art.gooBall.getHeight() / art.gooBall.getWidth();
                tmp.set(-gw / 2, -gh / 2, gw / 2, gh / 2); c.drawBitmap(art.gooBall, null, tmp, bmp); c.restore();
            } else {
                fill.setColor(A5); fill.setAlpha(255); c.drawCircle(s.x, s.y, 1.8f * u, fill);
                fill.setColor(A7); fill.setAlpha(200); c.drawCircle(s.x - 0.5f * u, s.y - 0.6f * u, 0.55f * u, fill);
                stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(0.35f * u); c.drawCircle(s.x, s.y, 1.8f * u, stroke);
            }
        }
    }

    void drawPart(Canvas c, P p) {
        float k = p.age / p.max, a = 1 - k;
        switch (p.t) {
            case POOF: {
                if (art.fxPoof == null) break;
                float sc = 0.55f + easeOut(Math.min(1, k * 1.6f)) * 0.75f;
                bmp.setAlpha((int) (255 * Math.min(1, a * 2.2f)));
                c.save(); c.translate(p.x, p.y); c.rotate(p.rot);
                art.icon(c, art.fxPoof, 0, 0, p.sz * sc);
                c.restore(); bmp.setAlpha(255);
                break;
            }
            case BURSTFX: {
                if (art.fxBurst == null) break;
                float sc = 0.6f + easeOut(Math.min(1, k * 2.5f)) * 0.5f;
                c.save(); c.translate(p.x, p.y); c.rotate(p.rot);
                c.saveLayerAlpha(-p.sz, -p.sz, p.sz, p.sz, (int) (255 * Math.min(1, a * 2)));
                art.icon(c, art.fxBurst, 0, 0, p.sz * sc);
                c.restore(); c.restore();
                break;
            }
            case ARROW: {
                if (art.fxArrow == null) break;
                c.saveLayerAlpha(p.x - p.sz, p.y - p.sz, p.x + p.sz, p.y + p.sz, (int) (255 * Math.min(1, a * 2.5f)));
                art.icon(c, art.fxArrow, p.x, p.y, p.sz * (k < 0.15f ? easeOutBack(k / 0.15f) : 1));
                c.restore();
                break;
            }
            case TRACER:
                stroke.setColor(A7); stroke.setAlpha((int) (230 * a)); stroke.setStrokeWidth(1.1f * u * a);
                c.drawLine(p.x, p.y, p.hx[0], p.hy[0], stroke);
                stroke.setColor(A6); stroke.setAlpha((int) (120 * a)); stroke.setStrokeWidth(2.6f * u * a);
                c.drawLine(p.x, p.y, p.hx[0], p.hy[0], stroke);
                break;
            case BUBBLE:
                fill.setShader(null); fill.setColor(A7); fill.setAlpha((int) (60 * a)); c.drawCircle(p.x, p.y, p.sz, fill);
                stroke.setColor(A7); stroke.setAlpha((int) (220 * a)); stroke.setStrokeWidth(0.25f * u); c.drawCircle(p.x, p.y, p.sz, stroke);
                fill.setColor(0xFFFFFFFF); fill.setAlpha((int) (200 * a)); c.drawCircle(p.x - p.sz * 0.35f, p.y - p.sz * 0.35f, p.sz * 0.22f, fill);
                break;
            case SPARK: case GOO:
                fill.setColor(p.col == 0 ? A5 : p.col); fill.setAlpha((int) (255 * a)); c.drawCircle(p.x, p.y, p.sz * (0.4f + a * 0.6f), fill); break;
            case WISP:
                fill.setColor(p.col); fill.setAlpha((int) (180 * a)); c.drawCircle(p.x, p.y, p.sz * (0.6f + k), fill); break;
            case CONF:
                fill.setColor(p.col); fill.setAlpha((int) (255 * Math.min(1, a * 3)));
                c.save(); c.translate(p.x, p.y); c.rotate(p.rot); c.scale(1, (float) Math.cos(p.age * 9 + p.vr));
                tmp.set(-p.sz / 2, -p.sz / 4, p.sz / 2, p.sz / 4); c.drawRect(tmp, fill);
                stroke.setColor(A0); stroke.setAlpha((int) (200 * Math.min(1, a * 3))); stroke.setStrokeWidth(0.2f * u); c.drawRect(tmp, stroke);
                c.restore(); break;
            case RING:
                stroke.setColor(p.col); stroke.setAlpha((int) (220 * a)); stroke.setStrokeWidth(1.1f * u * a);
                tmp.set(p.x - p.sz * easeOut(k), p.y - p.sz * easeOut(k) * 0.55f, p.x + p.sz * easeOut(k), p.y + p.sz * easeOut(k) * 0.55f);
                c.drawOval(tmp, stroke); break;
            case DUST:
                fill.setColor(p.col); fill.setAlpha((int) (130 * a)); c.drawCircle(p.x, p.y, p.sz * (0.5f + k), fill); break;
            case COIN:
                stroke.setColor(A6);
                for (int i = 4; i > 0; i--) { stroke.setAlpha((int) (110 * (1 - i / 5f))); stroke.setStrokeWidth(p.sz * 1.3f * (1 - i / 5f)); c.drawLine(p.hx[i], p.hy[i], p.hx[i - 1], p.hy[i - 1], stroke); }
                art.coin(c, p.x, p.y, p.sz);
                break;
        }
    }

    void drawChest(Canvas c) {
        float bounce = (float) Math.abs(Math.sin(chestT * 4)) * 1.5f * u, x = chestX, y = chestY - bounce;
        float w = 10 * u, h = 7 * u;
        softShadow(c, x + 0.5f * u, chestY, w * 0.72f * (1 - bounce / (6 * u)), 1.6f * u, 0.55f);
        fill.setColor(A6); fill.setAlpha((int) (60 + 50 * Math.sin(time * 5))); c.drawCircle(x, y - h * 0.6f, 9 * u, fill);
        if (art.chest != null) {
            float cw = 13 * u, ch = cw * art.chest.getHeight() / art.chest.getWidth();
            float wob = (float) Math.sin(chestT * 9) * 4 * (float) Math.max(0, Math.sin(chestT * 1.3f));   // impatient wiggle
            c.save(); c.rotate(wob, x, y);
            tmp.set(x - cw / 2, y - ch, x + cw / 2, y); c.drawBitmap(art.chest, null, tmp, bmp);
            c.restore();
            art.label(c, "TAP!", x, y - ch - 1.5f * u, 3.6f * u, A7);
            return;
        }
        tmp.set(x - w / 2, y - h, x + w / 2, y); fill.setColor(A2); fill.setAlpha(255); c.drawRoundRect(tmp, u, u, fill);
        tmp2.set(x - w / 2, y - h * 1.45f, x + w / 2, y - h * 0.55f); fill.setColor(A3); c.drawRoundRect(tmp2, 2.5f * u, 2.5f * u, fill);
        stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(0.6f * u); c.drawRoundRect(tmp, u, u, stroke); c.drawRoundRect(tmp2, 2.5f * u, 2.5f * u, stroke);
        fill.setColor(A6); tmp.set(x - w / 2 + 1.2f * u, y - h * 1.45f, x - w / 2 + 2.4f * u, y); c.drawRect(tmp, fill);
        tmp.set(x + w / 2 - 2.4f * u, y - h * 1.45f, x + w / 2 - 1.2f * u, y); c.drawRect(tmp, fill);
        art.rivet(c, x, y - h * 0.62f, 1.1f * u);
        art.label(c, "TAP!", x, y - h * 1.75f, 3.6f * u, A7);
    }

    void drawTutorial(Canvas c) {
        if (overlay != 0) return;
        if (tutBuild && coins >= T_COST[0] && selected < 0) {
            int i = 4;
            float bob = (float) Math.abs(Math.sin(time * 5)) * 2 * u;
            art.label(c, "build here!", padX[i], padY[i] - 6 * u - bob, 4 * u, A6);
        } else if (tutStrike && phase == 1 && !enemies.isEmpty()) {
            Enemy e = enemies.get(0);
            float k = (time * 1.2f) % 1f;
            stroke.setColor(A7); stroke.setAlpha((int) (200 * (1 - k))); stroke.setStrokeWidth(0.6f * u);
            c.drawCircle(e.x, e.y - e.r, e.r * (1.3f + k), stroke);
            art.label(c, "tap to squish!", e.x, e.y + e.r + 5 * u, 3.8f * u, A7);
        }
    }

    void drawBanner(Canvas c) {
        String a = null, b = null;
        float t, life;
        if (waveBannerT > 0) { a = bannerA; b = bannerB; t = 2f - waveBannerT; life = waveBannerT; }
        else if (phase == 0 && overlay == 0) { a = "WAVE " + wave; b = wave % 10 == 0 ? "a boss approaches..." : "get ready"; t = 2.2f - phaseT; life = phaseT; }
        else return;
        float s = t < 0.25f ? easeOutBack(Math.max(0, t) / 0.25f) : 1;
        float y = baseY + 1 * u;
        int alpha = (int) (255 * Math.min(1, life / 0.35f));
        if (art.uiRibbon != null) {
            float rw = Math.max(art.width(a, 11 * u) + 26 * u, 62 * u) * s, rh = rw * art.uiRibbon.getHeight() / art.uiRibbon.getWidth();
            bmp.setAlpha(alpha);
            tmp.set(W / 2 - rw / 2, y - rh * 0.72f, W / 2 + rw / 2, y + rh * 0.28f); c.drawBitmap(art.uiRibbon, null, tmp, bmp);
            bmp.setAlpha(255);
        }
        drawLabelAlpha(c, a, W / 2, y, 11 * u * s, A6, alpha);
        drawLabelAlpha(c, b, W / 2, y + 6 * u, 4 * u * s, A7, alpha);
    }

    void drawHud(Canvas c) {
        // coins
        float bump = 1 + clamp(coinBump * 0.07f, -0.15f, 0.25f);
        c.drawBitmap(bCoinPill, coinPill.left - 0.8f * u, coinPill.top - 0.8f * u, null);
        art.label(c, fmt(coins), coinPill.left + 6 * u, coinPill.bottom - 1.7f * u, 5 * u * bump, A7, Paint.Align.LEFT);
        art.coin(c, coinPill.left + 0.5f * u, coinPill.centerY(), 4.3f * u * bump);
        // wave sign + progress
        float pw = bPlank.getWidth(), px = W / 2 - pw / 2, py = coinPill.top - 2.2f * u;
        c.drawBitmap(bPlank, px, py, null);
        art.label(c, "WAVE " + wave, W / 2, py + 8.4f * u, 5.4f * u, A7);
        c.drawBitmap(bWaveBar, waveBar.left - 0.8f * u, waveBar.top + 1.4f * u - 0.8f * u, null);
        tmp.set(waveBar.left, waveBar.top + 1.4f * u, waveBar.right, waveBar.bottom + 1.4f * u);
        float prog = phase == 1 && waveTotal > 0 ? 1 - (toSpawn + enemies.size()) / (float) waveTotal : phase == 0 ? 0 : 1;
        art.barFill(c, tmp, prog, I5, I4);
        // sound
        c.drawBitmap(bRound, soundBtn.left - 1.2f * u, soundBtn.top - 1.2f * u, null);
        drawSpeaker(c, soundBtn.centerX(), soundBtn.centerY() - 0.4f * u);
        // x2 boost (rewarded ad)
        boolean on = boosted();
        Bitmap bb = bBoost[pressId == 5 && pressT > 0 ? 1 : 0];
        c.drawBitmap(bb, boostBtn.left - 1.2f * u, boostBtn.top - 1.2f * u, null);
        float off = pressId == 5 && pressT > 0 ? boostBtn.height() * 0.11f : 0;
        String lbl;
        if (on) { long s = (boostUntil - System.currentTimeMillis()) / 1000; lbl = String.format(Locale.US, "×2  %d:%02d", s / 60, s % 60); }
        else lbl = "×2 COINS";
        art.label(c, lbl, boostBtn.left + (on ? 2.5f : 6.5f) * u, boostBtn.centerY() + 0.6f * u + off, 3.4f * u, A7, Paint.Align.LEFT);
        if (!on) drawPlayIcon(c, boostBtn.left + 3.6f * u, boostBtn.centerY() - 0.6f * u + off, 1.5f * u);
    }

    void drawSpeaker(Canvas c, float x, float y) {
        if (art.icSound != null && art.icMute != null) { art.icon(c, sfx.on ? art.icSound : art.icMute, x, y, 5.6f * u); return; }
        float s = 1.6f * u;
        path.reset();
        path.moveTo(x - 1.6f * s, y - 0.5f * s); path.lineTo(x - 0.9f * s, y - 0.5f * s); path.lineTo(x - 0.1f * s, y - 1.2f * s);
        path.lineTo(x - 0.1f * s, y + 1.2f * s); path.lineTo(x - 0.9f * s, y + 0.5f * s); path.lineTo(x - 1.6f * s, y + 0.5f * s); path.close();
        fill.setColor(A7); fill.setAlpha(255); c.drawPath(path, fill);
        stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(0.45f * u); c.drawPath(path, stroke);
        stroke.setColor(A7); stroke.setStrokeWidth(0.5f * u);
        if (sfx.on) {
            tmp.set(x - 0.6f * s, y - 0.7f * s, x + 0.8f * s, y + 0.7f * s); c.drawArc(tmp, -50, 100, false, stroke);
            tmp.set(x - 0.6f * s, y - 1.3f * s, x + 1.4f * s, y + 1.3f * s); c.drawArc(tmp, -50, 100, false, stroke);
        } else {
            c.drawLine(x + 0.4f * s, y - 0.6f * s, x + 1.4f * s, y + 0.6f * s, stroke);
            c.drawLine(x + 1.4f * s, y - 0.6f * s, x + 0.4f * s, y + 0.6f * s, stroke);
        }
    }

    void drawPlayIcon(Canvas c, float x, float y, float s) {
        if (art.uiAd != null) { art.icon(c, art.uiAd, x + s * 0.2f, y + s * 0.15f, s * 2.6f); return; }
        if (art.icPlay != null) { art.icon(c, art.icPlay, x + s * 0.2f, y, s * 2.3f); return; }
        path.reset(); path.moveTo(x - s * 0.6f, y - s); path.lineTo(x + s, y); path.lineTo(x - s * 0.6f, y + s); path.close();
        fill.setColor(A7); fill.setAlpha(255); c.drawPath(path, fill);
        stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(0.4f * u); c.drawPath(path, stroke);
    }

    void drawButton(Canvas c, Bitmap[] b, RectF r, int id) {
        boolean down = pressId == id && pressT > 0;
        c.drawBitmap(b[down ? 1 : 0], r.left - 1.2f * u, r.top - 1.2f * u, null);
    }

    float btnOff(RectF r, int id) { return pressId == id && pressT > 0 ? r.height() * 0.11f : 0; }

    void drawCost(Canvas c, double cost, float cx, float y, float size, boolean can) {
        String s = fmt(cost);
        float w = art.width(s, size);
        art.coin(c, cx - w / 2 - size * 0.45f, y - size * 0.35f, size * 0.42f);
        art.label(c, s, cx + size * 0.35f, y, size, can ? A7 : 0xFFC9A27E);
    }

    void drawPanel(Canvas c) {
        RectF P = curPanel;
        Bitmap pb = P == panelR ? bPanel : panelBmp(P);
        c.drawBitmap(pb, P.left - 1.6f * u, P.top - 1.6f * u, null);
        float in = 4 * u;
        if (selected < 0) {
            String tip = tutBuild ? "Tap a stump to build a tower" : phase == 1 ? "Tap enemies to squish them!" : "Upgrade between waves";
            art.ink(c, tip, panelR.centerX(), panelR.top + 10.5f * u, 3.8f * u, A1, Paint.Align.CENTER);
            int nextT = -1;
            for (int t : DISPLAY) if (bestWave < T_UNLOCK[t]) { nextT = t; break; }
            String sub = nextT >= 0 ? "best wave " + bestWave + "  ·  " + T_SHORT[nextT] + " at wave " + T_UNLOCK[nextT] : "best wave " + bestWave;
            art.ink(c, sub, panelR.centerX(), panelR.top + 15.5f * u, 3f * u, A2, Paint.Align.CENTER);
            drawUpgradeBtn(c, bBtnA, btnA, 1, "STRIKE", "lv " + (tapLv + 1) + " · " + fmt(tapDmg()) + " dmg", tapCost(), 0);
            drawUpgradeBtn(c, bBtnB, btnB, 2, "FORTIFY", "lv " + (fortLv + 1) + " · " + fmt(baseMax) + " hp", fortCost(), 1);
        } else if (tType[selected] < 0) {
            art.ink(c, "BUILD A TOWER", P.left + in + u, P.top + 7.3f * u, 3.6f * u, A1, Paint.Align.LEFT);
            boolean big = gridCols == 3;
            for (int k = 0; k < nShown; k++) {
                int t = shown[k];
                RectF r = cardR[k];
                boolean locked = bestWave < T_UNLOCK[t];
                Bitmap[] cb = cardBmp(r.width(), r.height(), WOOD);
                if (locked) c.drawBitmap(cb[0], r.left - 1.2f * u, r.top - 1.2f * u, dimPaint);
                else drawButton(c, cb, r, 10 + k);
                float off = btnOff(r, 10 + k);
                Bitmap b = art.tower(t);
                float hgt = r.height() * (big ? 0.5f : 0.5f), wid = hgt * b.getWidth() / b.getHeight();
                float maxW = r.width() - 2.4f * u;
                if (wid > maxW) { hgt *= maxW / wid; wid = maxW; }
                float artTop = r.top + 1.2f * u + (r.height() * 0.5f - hgt) * 0.6f;
                tmp.set(r.centerX() - wid / 2, artTop + off, r.centerX() + wid / 2, artTop + hgt + off);
                c.drawBitmap(b, null, tmp, locked ? dimPaint : bmp);
                float nameY = r.top + r.height() * 0.5f + (big ? 4.6f : 3.6f) * u + off;
                art.label(c, big ? T_NAME[t] : T_SHORT[t], r.centerX(), nameY, (big ? 2.9f : 2.6f) * u, locked ? 0xFFC9A27E : A7);
                if (locked) {
                    if (art.uiLock != null) art.icon(c, art.uiLock, r.centerX(), artTop + hgt * 0.55f, 6 * u);
                    art.label(c, "wave " + T_UNLOCK[t], r.centerX(), r.bottom - (big ? 4.6f : 3.6f) * u, (big ? 3.4f : 3f) * u, 0xFFC9A27E);
                } else drawCost(c, T_COST[t], r.centerX(), r.bottom - (big ? 4.6f : 3.6f) * u + off, (big ? 3.8f : 3.2f) * u, coins >= T_COST[t]);
            }
            drawCloseBtn(c);
        } else {
            int i = selected, t = tType[i];
            Bitmap b = art.tower(t);
            float hgt = 21 * u, wid = hgt * b.getWidth() / b.getHeight();
            if (wid > 20 * u) { hgt *= 20 * u / wid; wid = 20 * u; }
            tmp.set(panelR.left + in + 1 * u, panelR.top + 4.5f * u + (21 * u - hgt) / 2, panelR.left + in + 1 * u + wid, panelR.top + 4.5f * u + (21 * u + hgt) / 2);
            c.drawBitmap(b, null, tmp, bmp);
            float tx = panelR.left + in + 22 * u;
            art.label(c, T_NAME[t], tx, panelR.top + 9.5f * u, 4.6f * u, A7, Paint.Align.LEFT);
            art.ink(c, "level " + (tLv[i] + 1) + " · " + T_ROLE[t], tx, panelR.top + 14 * u, 2.9f * u, A1, Paint.Align.LEFT);
            art.ink(c, statLine(i), tx, panelR.top + 18.5f * u, 3.2f * u, A2, Paint.Align.LEFT);
            drawButton(c, bUp, upBtn, 4);
            float off = btnOff(upBtn, 4);
            art.label(c, "UPGRADE", upBtn.centerX(), upBtn.top + 7 * u + off, 4.8f * u, A7);
            drawCost(c, upCost(i), upBtn.centerX(), upBtn.top + 13.4f * u + off, 4f * u, coins >= upCost(i));
            Bitmap[] sb = cardBmp(sellBtn.width(), sellBtn.height(), WOOD);
            drawButton(c, sb, sellBtn, 6);
            float so = btnOff(sellBtn, 6);
            art.label(c, "SELL +" + fmt(sellValue(i)), sellBtn.centerX(), sellBtn.centerY() + 1.1f * u + so, 3.2f * u, A7);
            drawCloseBtn(c);
        }
    }

    String statLine(int i) {
        int t = tType[i], lv = tLv[i];
        double now = towerDmg(i), next = T_DMG[t] * Math.pow(1.55, lv + 1);
        switch (t) {
            case BANKER: {
                double a = Math.ceil(rewardScale(wave) * 1.2 * Math.pow(1.25, lv)), b = Math.ceil(rewardScale(wave) * 1.2 * Math.pow(1.25, lv + 1));
                return "+" + fmt(a) + " every 3s  →  +" + fmt(b);
            }
            case DRUMMER: return "nearby towers +" + Math.round((0.25f + 0.08f * lv) * 100) + "% speed  →  +" + Math.round((0.33f + 0.08f * lv) * 100) + "%";
            case LANTERN: return "burns " + fmt(now) + "/s  →  " + fmt(next) + "/s";
            default: return "damage " + fmt(now) + "  →  " + fmt(next);
        }
    }

    final Paint dimPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    { dimPaint.setColorFilter(new android.graphics.ColorMatrixColorFilter(new float[]{
            0.55f, 0.15f, 0.05f, 0, 0,  0.12f, 0.5f, 0.05f, 0, 0,  0.08f, 0.12f, 0.4f, 0, 0,  0, 0, 0, 1, 0})); }

    void drawUpgradeBtn(Canvas c, Bitmap[] b, RectF r, int id, String title, String sub, double cost, int icon) {
        boolean afford = coins >= cost;
        if (afford) drawButton(c, b, r, id);
        else c.drawBitmap(b[pressId == id && pressT > 0 ? 1 : 0], r.left - 1.2f * u, r.top - 1.2f * u, dimPaint);   // muted, not hidden
        float off = btnOff(r, id);
        boolean can = coins >= cost;
        float ix = r.left + 6.5f * u, iy = r.top + 7.6f * u + off;
        if (art.icStrike != null && art.icShield != null) {
            art.icon(c, icon == 0 ? art.icStrike : art.icShield, ix, iy, 8.2f * u);
        } else if (icon == 0) { // fist / impact star
            art.star(c, ix, iy, 3.4f * u, A7);
        } else {          // shield
            path.reset();
            path.moveTo(ix - 3 * u, iy - 3 * u); path.lineTo(ix + 3 * u, iy - 3 * u); path.lineTo(ix + 3 * u, iy);
            path.quadTo(ix + 3 * u, iy + 2.5f * u, ix, iy + 3.8f * u); path.quadTo(ix - 3 * u, iy + 2.5f * u, ix - 3 * u, iy); path.close();
            fill.setColor(A7); fill.setAlpha(255); c.drawPath(path, fill);
            stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(0.55f * u); c.drawPath(path, stroke);
            c.drawLine(ix, iy - 2.4f * u, ix, iy + 2.8f * u, stroke);
        }
        float tx = r.left + 12.5f * u;
        art.label(c, title, tx, r.top + 6.2f * u + off, 4.4f * u, A7, Paint.Align.LEFT);
        art.ink(c, sub, tx, r.top + 9.9f * u + off, 2.7f * u, A1, Paint.Align.LEFT);
        String s = fmt(cost);
        art.coin(c, tx + 1.6f * u, r.top + 13.3f * u + off, 1.9f * u);
        art.label(c, s, tx + 4.4f * u, r.top + 14.7f * u + off, 4.2f * u, can ? A7 : 0xFFC9A27E, Paint.Align.LEFT);

    }

    void drawCloseBtn(Canvas c) {
        c.drawBitmap(bClose, closeBtn.left - 1.2f * u, closeBtn.top - 1.2f * u, null);
        if (art.icClose != null) { art.icon(c, art.icClose, closeBtn.centerX(), closeBtn.centerY() - 0.2f * u, closeBtn.width() * 0.55f); return; }
        float x = closeBtn.centerX(), y = closeBtn.centerY() - 0.4f * u, s = 1.8f * u;
        stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(1.3f * u);
        c.drawLine(x - s, y - s, x + s, y + s, stroke); c.drawLine(x + s, y - s, x - s, y + s, stroke);
        stroke.setColor(A7); stroke.setStrokeWidth(0.7f * u);
        c.drawLine(x - s, y - s, x + s, y + s, stroke); c.drawLine(x + s, y - s, x - s, y + s, stroke);
    }

    void drawOverlay(Canvas c) {
        fill.setShader(null); fill.setColor(I0); fill.setAlpha(190); c.drawRect(0, 0, W, H, fill);
        float bob = (float) Math.sin(time * 2.5f) * 0.5f * u;
        c.save(); c.translate(0, bob);
        c.drawBitmap(bOvPanel, ovPanel.left - 1.6f * u, ovPanel.top - 1.6f * u, null);
        float pw = bOvPlank.getWidth();
        c.drawBitmap(bOvPlank, W / 2 - pw / 2, ovPanel.top - 7 * u, null);
        if (overlay == 1 && Ads.CHEST.equals(rewardPlacement) && art.chestOpen != null) {
            float cw = 22 * u, ch = cw * art.chestOpen.getHeight() / art.chestOpen.getWidth();
            tmp.set(ovPanel.left - 6 * u, ovPanel.top - ch * 0.55f, ovPanel.left - 6 * u + cw, ovPanel.top + ch * 0.45f);
            c.drawBitmap(art.chestOpen, null, tmp, bmp);
        }
        if (overlay == 1) {
            art.label(c, rewardTitle, W / 2, ovPanel.top + 2.6f * u, 6.4f * u, A6);
            art.ink(c, rewardSub, W / 2, ovPanel.top + 13 * u, 3.4f * u, A1, Paint.Align.CENTER);
            String amt = "+" + fmt(rewardAmt);
            float w = art.width(amt, 9 * u);
            art.coin(c, W / 2 - w / 2 - 1.5f * u, ovPanel.top + 21 * u, 4 * u);
            art.label(c, amt, W / 2 + 3 * u, ovPanel.top + 24.5f * u, 9 * u, A7);
            drawButton(c, bOvA, ovA, 20);
            float off = btnOff(ovA, 20);
            drawPlayIcon(c, ovA.left + 7 * u, ovA.centerY() - 0.8f * u + off, 2.2f * u);
            art.label(c, "COLLECT ×" + rewardMult, ovA.centerX() + 3 * u, ovA.centerY() + 1.2f * u + off, 5.2f * u, A7);
            drawButton(c, bOvB, ovB, 21);
            art.label(c, "no thanks", ovB.centerX(), ovB.centerY() + 0.9f * u + btnOff(ovB, 21), 3.6f * u, A7);
        } else {
            art.label(c, "DEFEAT", W / 2, ovPanel.top + 2.6f * u, 7 * u, A6);
            art.ink(c, "the gloom reached your king", W / 2, ovPanel.top + 13 * u, 3.4f * u, A1, Paint.Align.CENTER);
            art.ink(c, "wave " + wave, W / 2, ovPanel.top + 23 * u, 7 * u, A2, Paint.Align.CENTER);
            drawButton(c, bOvA, ovA, 20);
            float off = btnOff(ovA, 20);
            drawPlayIcon(c, ovA.left + 7 * u, ovA.centerY() - 0.8f * u + off, 2.2f * u);
            art.label(c, "REVIVE", ovA.centerX() + 3 * u, ovA.centerY() + 1.2f * u + off, 5.6f * u, A7);
            drawButton(c, bOvB, ovB, 21);
            art.label(c, "retreat to wave " + Math.max(1, wave - 3), ovB.centerX(), ovB.centerY() + 0.9f * u + btnOff(ovB, 21), 3.4f * u, A7);
        }
        c.restore();
    }

    // ================================================================== utils
    private static final String[] SUF = {"", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"};

    static String fmt(double v) {
        if (v < 1000) return String.valueOf((long) Math.floor(v + 1e-9));
        int e = (int) Math.floor(Math.log10(v) / 3);
        double m = v / Math.pow(1000, e);
        if (m >= 999.995) { m /= 1000; e++; }
        String s = e < SUF.length ? SUF[e] : "" + (char) ('a' + ((e - SUF.length) / 26) % 26) + (char) ('a' + (e - SUF.length) % 26);
        return (m < 10 ? String.format(Locale.US, "%.2f", m) : m < 100 ? String.format(Locale.US, "%.1f", m) : String.valueOf((int) m)) + s;
    }

    static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    static float easeOut(float t) { return 1 - (1 - t) * (1 - t); }
    static float easeOutBack(float t) { float c1 = 1.70158f, c3 = c1 + 1; return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2); }
}
