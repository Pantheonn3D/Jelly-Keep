package com.pantheon3d.jellykeep;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Insets;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import static com.pantheon3d.jellykeep.Art.*;

/**
 * Jelly Keep: idle/clicker tower defense. One view: the battle simulation, its rendering and input, the HUD and
 * bottom panel, and a stack of modal {@link Screen}s (title, vault, shop...) drawn on top. Persistent progress that
 * outlives a run lives in {@link Meta}.
 */
public class Game extends View implements Ads.Host, Store.Host {

    // =============================================================== definitions
    static final int POPPER = 0, WALL = 1, MORTAR = 2, SPARKLER = 3, SNIPER = 4, MAGE = 5, BOMB = 6, BANKER = 7, DRUMMER = 8, LANTERN = 9;
    static final String[] T_NAME = {"Pebble Popper", "Jelly Wall", "Goo Mortar", "Sparkler", "Spyglass Sniper", "Bubble Mage", "Bomb Buddy", "Piggy Banker", "Drummer", "Lantern Keeper"};
    static final String[] T_SHORT = {"Popper", "Wall", "Mortar", "Sparkler", "Sniper", "Mage", "Bomb", "Banker", "Drummer", "Lantern"};
    static final String[] T_ROLE = {"fast single shots", "slams & knocks back", "goo splash, slows", "rapid sparks, short range", "long range, hits the biggest",
            "bubbles slow everything", "huge blast, slow fuse", "makes coins on its own", "speeds up both towers beside it", "burns everything nearby"};
    static final double[] T_COST = {30, 60, 120, 90, 220, 160, 400, 150, 300, 500};
    static final double[] T_DMG = {4, 5, 6, 1.6, 24, 1.5, 45, 0, 0, 3};          // lantern: damage per second
    static final float[] T_RATE = {1.2f, 0.55f, 0.4f, 4.5f, 0.45f, 1f, 0.28f, 0.33f, 2f, 4f};
    static final float[] T_RANGE = {36, 17, 50, 26, 80, 30, 40, 0, 0, 24};     // in u
    static final float[] T_WIND = {0.16f, 0.32f, 0.28f, 0, 0.4f, 0.2f, 0.45f, 0.35f, 0.15f, 0};   // anticipation before each attack
    static final int[] T_UNLOCK = {1, 1, 1, 3, 6, 4, 10, 5, 8, 13};             // best wave needed
    static final int[] T_FLIP = {1, 0, 1, 0, 1, 1, 1, 0, 0, -1};               // 1 faces right, -1 faces left, 0 never flips
    static final float[] T_H = {18.5f, 19.5f, 18.5f, 15f, 20f, 16.5f, 16.5f, 16.5f, 18f, 17.5f};
    static final int[] DISPLAY = {0, 1, 2, 3, 5, 7, 4, 8, 6, 9};                // build menu order = unlock order

    static final int GLOOM = 0, SKITTER = 1, BRUTE = 2, BOSS = 3, FLAPPER = 4, SHIELD = 5, SPLITTER = 6, HEALER = 7, MINI = 8;
    static final float[] E_HP = {1, 0.45f, 4, 26, 0.6f, 2.2f, 2.4f, 1.3f, 0.4f};
    static final float[] E_SPD = {8, 14, 5, 3.5f, 11, 6, 6, 7, 11};
    static final float[] E_R = {4.8f, 3.6f, 7.2f, 11.5f, 4.2f, 6.2f, 6.8f, 4.6f, 3.4f};
    static final float[] E_DMG = {5, 3, 14, 40, 4, 8, 9, 3, 3};
    static final float[] E_REWARD = {1, 0.7f, 4, 35, 1.2f, 2.5f, 2, 2, 0.5f};
    static final float[] E_HH = {2.35f, 2.35f, 2.55f, 2.55f, 2.15f, 2.3f, 1.9f, 2.45f, 2.35f};
    static final int[] E_FLIP = {0, -1, 1, 0, 0, -1, 0, 0, 0};
    static final String[] E_NAME = {"", "", "", "", "Flapper!", "Shieldling!", "Splitter!", "Wisp Healer!"};
    static final String[] E_HINT = {"", "", "", "", "flies over walls & splashes", "blocks shots, not splash", "splits in two when squished", "heals its friends, hit it first"};

    // King's spells
    static final int SLAM = 0, FREEZE = 1, RUSH = 2;
    static final String[] S_NAME = {"Jelly Slam", "Bubble Trap", "Gold Rush"};
    static final String[] S_WHAT = {"smash everything near the king", "freeze every enemy for 4 s", "coins ×2 for 15 s"};
    static final int[] S_UNLOCK = {6, 12, 18};
    static final float[] S_CD = {40, 60, 90};

    // elite waves (every 5th from 15) and bosses (every 10th)
    static final int M_SWARM = 0, M_STAMPEDE = 1, M_AIR = 2, M_IRON = 3, M_GIANTS = 4;
    static final String[] MOD_NAME = {"SWARM!", "STAMPEDE!", "AIR RAID!", "IRON WALL!", "GIANTS!"};
    static final String[] MOD_HINT = {"twice as many, half as tough", "skitters rush the king", "a sky full of flappers", "shieldlings: splash beats shields", "a march of brutes"};
    static final String[] BOSS_NAME = {"Gloomlord", "Bramble Brute", "Night Mother", "Shade Colossus", "The Hollow Crown"};

    // in-context hints, each shown once (bits in Meta.hints)
    static final int H_STORY = 0, H_TAP = 1, H_BUILD = 2, H_STRIKE = 3, H_BOOST = 4, H_CHEST = 5, H_UPGRADE = 6, H_SPELL = 7,
            H_BOSS = 8, H_VAULT = 9, H_QUESTS = 10, H_COURIER = 11, H_REBIRTH = 12, H_NOTIFY = 13, H_REVIEW = 14;

    // when things appear (best wave reached)
    static final int UNLOCK_BOOST = 3, UNLOCK_COURIER = 5, UNLOCK_GIFT = 5, UNLOCK_STARTER = 8;
    static final int NAV_QUESTS = 0, NAV_VAULT = 1, NAV_SHOP = 2, NAV_REBIRTH = 3;
    static final String[] NAV_NAME = {"QUESTS", "VAULT", "SHOP", "REBIRTH"};
    static final int[] NAV_UNLOCK = {4, 10, 3, Meta.REBIRTH_AT};
    static final float COURIER_S = 18;
    static final double START_COINS = 40;

    // battle-screen action ids (screens use 100+)
    static final int B_STRIKE = 1, B_FORT = 2, B_UP = 3, B_SELL = 4, B_CLOSE = 5, B_BOOST = 6, B_GEAR = 7, B_NAV = 10, B_SPELL = 20, B_CARD = 30;

    // =============================================================== systems
    final Art art;
    final Sfx sfx;
    final Meta meta;
    final Ui ui;
    final Music music;
    final Activity activity;
    Ads ads;
    Store store;
    private final SharedPreferences prefs;
    private final Vibrator vib;
    final Random rnd = new Random();
    private final Random sndRnd = new Random();   // sound pitch only: sounds are rate-limited by wall clock, so they
                                                  // must not draw from rnd or the simulation stops being reproducible

    // =============================================================== saved run state
    double coins, coinRate;
    int wave = 1, bestWave = 1, runBest = 1, tapLv, fortLv, wavesCleared, streak, seenMask;
    final int[] tType = {-1, -1, -1, -1, -1, -1}, tLv = new int[6];
    long lastSaveMs, boostUntil;
    boolean chestUp;

    // =============================================================== runtime state
    double baseHp, baseMax;
    int phase;                  // 0 = countdown, 1 = fighting, 2 = defeated (the defeat screen decides)
    float phaseT = 2.5f;
    int toSpawn, waveTotal, mod = -1;
    float spawnT;
    int selected = -1;          // pad index or -1
    float chestX, chestY, chestT;
    final ArrayList<Screen> screens = new ArrayList<>();
    double[] pendingOffline;    // {coins, seconds away}, shown once the player enters the keep
    boolean entered;            // left the title screen this session
    boolean inAd;               // a full-screen ad paused us: coming back from it isn't "time away"

    int combo, burstKills;
    float comboT, comboPop;
    boolean tapping;
    final float[] spellCd = new float[3], spellFlash = new float[3];
    final boolean[] spellAd = new boolean[3];
    float freezeT, rushT, shieldT, bonkT;
    Enemy boss;
    float bossSummonT;
    int revivedWave = -1;
    float reviveReady;                                  // play time when the next revive is allowed
    static final float REVIVE_COOLDOWN = 240;
    boolean courierUp;
    float courierT = 150, courierLife, courierX, courierY;
    boolean newBestShown;
    String toastMsg = "";
    float toastT;
    int hintNow = -1;
    float hintStart;
    int holdId;
    float holdT, holdGap;
    String callout = "";
    float calloutT;
    int calloutCol = A6;
    float questCheck;

    final ArrayList<Enemy> enemies = new ArrayList<>();
    final ArrayList<Shot> shots = new ArrayList<>();
    final ArrayList<P> parts = new ArrayList<>();
    final ArrayList<Pop> pops = new ArrayList<>();
    final ArrayList<float[]> puddles = new ArrayList<>(); // x, y, r, life
    final float[] tBuff = {1, 1, 1, 1, 1, 1}, tBeat = new float[6];
    final RectF buildR = new RectF(), sellBtn = new RectF();
    RectF curPanel;
    final int[] shown = new int[10];
    int nShown, gridCols = 3;
    final float[] tCool = new float[6], tSq = new float[6], tSqV = new float[6], tFace = {1, 1, 1, 1, 1, 1}, tWind = new float[6];

    // feel
    boolean running;
    private long lastNs;
    float time, hitStop, trauma, flashA, shakeX, shakeY;
    int flashCol = 0xFFFFFFFF;
    float baseSq, baseSqV, baseHurtT, baseHappyT, coinBump, coinBumpV, gemBump, gemBumpV, waveBannerT, rateClock;
    double rateAcc;
    String bannerA = "", bannerB = "";
    float bannerLife = 2f;
    float lookX, lookY;
    long lastDieSnd, lastHitSnd, lastShootSnd;

    // =============================================================== layout
    float W, H, u = 10, safeT, safeB, safeL, safeR, stageX, screenW, insetL, insetR;
    float fieldTop, fieldBot, baseX, baseY, baseR;
    final float[] padX = new float[6], padY = new float[6];
    final RectF coinPill = new RectF(), gemPill = new RectF(), waveBar = new RectF(), gearBtn = new RectF(), boostBtn = new RectF(),
            panelR = new RectF(), btnA = new RectF(), btnB = new RectF(), closeBtn = new RectF(), upBtn = new RectF();
    final RectF[] cardR = new RectF[10], navR = new RectF[4], spellR = new RectF[3];
    { for (int k = 0; k < 10; k++) cardR[k] = new RectF(); for (int k = 0; k < 4; k++) navR[k] = new RectF(); for (int k = 0; k < 3; k++) spellR[k] = new RectF(); }
    final RectF tmp = new RectF(), tmp2 = new RectF();
    Bitmap bField, bPanel, bPlank, bCoinPill, bGemPill, bWaveBar, bHpBar, bBossBar, bRound, bSpell, bPad;
    private final HashMap<String, Bitmap> plankCache = new HashMap<>(), roundCache = new HashMap<>();

    final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG), bmp = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    final Path path = new Path();
    final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG), haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG), glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            warmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        shadowPaint.setShader(new RadialGradient(0, 0, 1, new int[]{0xFF000000, 0xA0000000, 0x00000000}, new float[]{0, 0.45f, 1}, Shader.TileMode.CLAMP));
        haloPaint.setShader(new RadialGradient(0, 0, 1, new int[]{0x889C8CFF, 0x339C8CFF, 0x009C8CFF}, new float[]{0, 0.55f, 1}, Shader.TileMode.CLAMP));
        glowPaint.setShader(new RadialGradient(0, 0, 1, new int[]{0x55FFB13B, 0x22FF9F1C, 0x00FF9F1C}, new float[]{0, 0.6f, 1}, Shader.TileMode.CLAMP));
        warmPaint.setShader(new RadialGradient(0, 0, 1, new int[]{0x66FF9F1C, 0x22E08A2E, 0x00E08A2E}, new float[]{0, 0.55f, 1}, Shader.TileMode.CLAMP));
    }

    /** Soft elliptical shadow with a feathered edge (no hard disc). */
    void softShadow(Canvas c, float x, float y, float rx, float ry, float strength) {
        c.save(); c.translate(x, y); c.scale(rx, ry);
        shadowPaint.setAlpha((int) (255 * clamp(strength, 0, 1)));
        c.drawCircle(0, 0, 1, shadowPaint);
        c.restore();
    }

    /** A cached unit-radius radial paint, drawn as an ellipse at (x, y). */
    static void glow(Canvas c, Paint p, float x, float y, float rx, float ry, int alpha) {
        c.save(); c.translate(x, y); c.scale(rx, ry);
        p.setAlpha(alpha);
        c.drawCircle(0, 0, 1, p);
        c.restore();
    }

    final Paint lowHpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    { lowHpPaint.setColorFilter(new PorterDuffColorFilter(0x55FF3020, PorterDuff.Mode.SRC_ATOP)); }
    final Paint burnPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    { burnPaint.setColorFilter(new PorterDuffColorFilter(0x77FF8A1F, PorterDuff.Mode.SRC_ATOP)); }
    final Paint ragePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    { ragePaint.setColorFilter(new PorterDuffColorFilter(0x44FF5040, PorterDuff.Mode.SRC_ATOP)); }
    final Paint flashPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    { flashPaint.setColorFilter(new PorterDuffColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN)); flashPaint.setAlpha(190); }
    final Paint dimPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    { dimPaint.setColorFilter(new ColorMatrixColorFilter(new float[]{
            0.55f, 0.15f, 0.05f, 0, 0,  0.12f, 0.5f, 0.05f, 0, 0,  0.08f, 0.12f, 0.4f, 0, 0,  0, 0, 0, 1, 0})); }

    // =============================================================== setup
    public Game(Context ctx) {
        super(ctx);
        activity = ctx instanceof Activity ? (Activity) ctx : null;
        art = new Art(ctx);
        sfx = new Sfx(ctx);
        prefs = ctx.getSharedPreferences("jellykeep", Context.MODE_PRIVATE);
        meta = new Meta(prefs);
        ui = new Ui(art);
        music = new Music(ctx);
        vib = ctx.getSystemService(Vibrator.class);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        load();
        ads = MonetizationFactory.ads(activity, this);
        store = MonetizationFactory.store(activity, this);
        meta.sessions++;
        meta.ensureQuests(bestWave);
        if (meta.v16Gift) for (int h : new int[]{H_STORY, H_TAP, H_BUILD, H_STRIKE}) meta.see(h);   // 1.6 players know the basics
        if (anyTower()) meta.see(H_BUILD);
        screens.add(new Screens.Title(this));
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
            safeT = Math.max(t, 20 * dp); safeB = Math.max(b, 10 * dp); insetL = l; insetR = r;
            layout();
            return in;
        });
    }

    void resume() {
        running = true;
        lastNs = 0;
        long now = meta.now();
        if (lastSaveMs > 0) {
            double away = Math.min((now - lastSaveMs) / 1000.0, meta.offlineCapSec());
            double amt = coinRate * away * meta.offlineFactor();
            if (away >= (inAd ? 300 : 60) && amt >= 1) {
                pendingOffline = new double[]{amt, away};
                if (entered) showPending();
            }
        }
        lastSaveMs = now;
        store.refresh();
        Notify.cancel(getContext());
        music.play(meta.music);
        invalidate();
    }

    void pause() {
        running = false;
        save();
        meta.save();
        music.pause();
        if (meta.notify) Notify.schedule(getContext(), this);
    }

    private void load() {
        SharedPreferences p = prefs;
        coins = p.contains("coins") ? Double.longBitsToDouble(p.getLong("coins", 0)) : START_COINS;   // enough for a first tower
        coinRate = Double.longBitsToDouble(p.getLong("rate", 0));
        wave = Math.max(1, p.getInt("wave", 1)); bestWave = Math.max(wave, p.getInt("best", 1));
        runBest = Math.max(wave, p.getInt("runBest", bestWave));
        seenMask = p.getInt("seen", 0); tapLv = p.getInt("tap", 0); fortLv = p.getInt("fort", 0); wavesCleared = p.getInt("cleared", 0);
        for (int i = 0; i < 6; i++) { tType[i] = p.getInt("tt" + i, -1); tLv[i] = p.getInt("tl" + i, 0); }
        if (!p.getBoolean("tutB", true)) meta.see(H_BUILD);        // 1.6 tutorial flags
        if (!p.getBoolean("tutS", true)) meta.see(H_TAP);
        streak = p.getInt("streak", 0); chestUp = p.getBoolean("chest", false);
        lastSaveMs = p.getLong("t", 0); boostUntil = p.getLong("boost", 0);
        sfx.on = p.getBoolean("sound", true);
        baseMax = baseMaxFor(fortLv); baseHp = baseMax;
    }

    void save() {
        lastSaveMs = meta.now();
        SharedPreferences.Editor e = prefs.edit()
                .putLong("coins", Double.doubleToLongBits(coins)).putLong("rate", Double.doubleToLongBits(coinRate))
                .putInt("wave", wave).putInt("best", bestWave).putInt("runBest", runBest).putInt("tap", tapLv).putInt("fort", fortLv)
                .putInt("cleared", wavesCleared).putInt("seen", seenMask).putInt("streak", streak).putBoolean("chest", chestUp)
                .putBoolean("sound", sfx.on).putLong("t", lastSaveMs).putLong("boost", boostUntil);
        for (int i = 0; i < 6; i++) e.putInt("tt" + i, tType[i]).putInt("tl" + i, tLv[i]);
        e.apply();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        screenW = w; H = h;
        W = Math.min(w, Math.round(h * 0.62f));          // portrait stage; tablets and landscape get side bars
        stageX = (w - W) / 2f;
        float dp = getResources().getDisplayMetrics().density;
        if (safeT == 0) safeT = 20 * dp;
        if (safeB == 0) safeB = 10 * dp;
        layout();
    }

    void layout() {
        if (W == 0) return;
        safeL = Math.max(0, insetL - stageX); safeR = Math.max(0, insetR - stageX);
        u = Math.min(W, H * 0.5f) / 100f;
        art.u = u; ui.u = u;
        ui.clear(); plankCache.clear(); roundCache.clear();
        float L = safeL + 2.5f * u, R = W - safeR - 2.5f * u, top = safeT + 2 * u;

        coinPill.set(L + 4 * u, top + 1.2f * u, L + 31 * u, top + 8.2f * u);
        gemPill.set(L + 4 * u, top + 10.6f * u, L + 23 * u, top + 16.6f * u);
        gearBtn.set(R - 10 * u, top, R, top + 10 * u);
        boostBtn.set(R - 26 * u, top + 12.2f * u, R, top + 19.2f * u);
        float pw = 34 * u;
        waveBar.set(W / 2 - 13 * u, top + 10.2f * u, W / 2 + 13 * u, top + 13.2f * u);
        fieldTop = top + 20 * u;

        float ph = 39 * u, in = 3.5f * u;
        panelR.set(L, H - safeB - ph - 1.5f * u, R, H - safeB - 1.5f * u);
        fieldBot = panelR.top - 1 * u;
        float bw = (panelR.width() - in * 2 - 3 * u) / 2, bh = 17 * u, by = panelR.top + in;
        btnA.set(panelR.left + in, by, panelR.left + in + bw, by + bh);
        btnB.set(btnA.right + 3 * u, by, panelR.right - in, by + bh);
        float nw = (panelR.width() - in * 2 - 6 * u) / 4, ny = btnA.bottom + 2 * u;
        for (int k = 0; k < 4; k++) navR[k].set(panelR.left + in + k * (nw + 2 * u), ny, panelR.left + in + k * (nw + 2 * u) + nw, panelR.bottom - in);
        closeBtn.set(panelR.right - 9.5f * u, panelR.top - 3.5f * u, panelR.right + 0.5f * u, panelR.top + 6.5f * u);
        upBtn.set(panelR.right - in - 34 * u, panelR.bottom - in - 17 * u, panelR.right - in, panelR.bottom - in);
        float sd = 12.5f * u;
        for (int k = 0; k < 3; k++) {
            float l = R - (3 - k) * sd - (2 - k) * 2 * u;
            spellR[k].set(l, panelR.top - sd - 3 * u, l + sd, panelR.top - 3 * u);
        }

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
        if (chestUp) { chestX = baseX + 15 * u; chestY = baseY + 15 * u; }

        // pre-render the kit pieces at their exact sizes
        bField = art.field((int) W, (int) H, 7);
        bPanel = art.panel(panelR.width(), panelR.height(), true, 3);
        bPlank = art.plank(pw, 9 * u, 11);
        bCoinPill = art.recess(coinPill.width(), coinPill.height());
        bGemPill = art.recess(gemPill.width(), gemPill.height());
        bWaveBar = art.recess(waveBar.width(), waveBar.height());
        bBossBar = art.recess(60 * u, 3.6f * u);
        bHpBar = art.recess(24 * u, 3f * u);
        bRound = art.roundButton(gearBtn.width());
        bSpell = art.roundButton(sd);
        bPad = art.pad(17 * u);
    }

    Bitmap plankBmp(float w, float h) {
        String k = (int) w + "x" + (int) h;
        Bitmap b = plankCache.get(k);
        if (b == null) { b = art.plank(w, h, 13); plankCache.put(k, b); }
        return b;
    }

    Bitmap roundBmp(float d) {
        String k = String.valueOf((int) d);
        Bitmap b = roundCache.get(k);
        if (b == null) { b = art.roundButton(d); roundCache.put(k, b); }
        return b;
    }

    // =============================================================== economy
    double baseMaxFor(int lv) { return 100 * Math.pow(1.3, lv) * meta.kingHpMult(); }
    double tapBase() { return 3 * Math.pow(1.5, tapLv) * meta.dmgMult() * meta.tapMult(); }
    float comboMult() { return 1 + 0.0125f * Math.min(combo, 40); }
    double tapDmg() { return tapBase() * comboMult(); }
    double tapCost() { return Math.ceil(15 * Math.pow(1.8, tapLv)); }
    double fortCost() { return Math.ceil(25 * Math.pow(1.7, fortLv)); }
    /** Tower damage at a level: ×1.45 per level and ×2 at every 10th ("stars"); averages 1.6's ×1.55. */
    static double towerDmgAt(int t, int lv) { return T_DMG[t] * Math.pow(1.45, lv) * Math.pow(2, lv / 10); }
    double towerDmg(int i) { return towerDmgAt(tType[i], tLv[i]) * meta.dmgMult(); }
    double upCost(int i) { return Math.ceil(T_COST[tType[i]] * Math.pow(1.7, tLv[i] + 1)); }
    /** Sawtooth: the two waves after each boss are a breather. */
    static double saw(int w) { if (w <= 10) return 1; int m = w % 10; return m == 1 ? 0.85 : m == 2 ? 0.92 : 1; }
    static final double HP_RAMP = 1.0015, BANK_GROW = 1.25;   // tuned by a seeded bot sweep (see CLAUDE.md, Balance)
    /**
     * +17% per wave; past wave 40 the climb steepens a little more every wave. Bankers, boosts and Royal Jelly can
     * keep pace with a flat 17% for ever, so without the ramp an engaged player stops meeting walls after a rebirth.
     */
    double hpScale(int w) { int x = Math.max(0, w - 40); return 8 * Math.pow(1.17, w - 1) * saw(w) * Math.pow(HP_RAMP, x * x); }
    double rewardScale(int w) { return 2 * Math.pow(1.12, w - 1); }
    boolean boosted() { return meta.now() < boostUntil; }
    double streakMult() { return 1 + 0.02 * Math.min(streak, 15); }
    double bankerPay(int lv) { return Math.ceil(rewardScale(wave) * 1.2 * Math.pow(BANK_GROW, lv)); }
    /** Sizes every lump-sum reward (chest, courier, gift, crates): the live income rate, never below a wave's worth. */
    double incomePerMin() { return Math.max(coinRate * 60, rewardScale(wave) * 40 * meta.coinMult()); }

    static final int G_KILL = 0, G_BANK = 1, G_WAVE = 2, G_FLAT = 3;

    /** Adds coins with every multiplier that applies to the source; returns what was actually added. */
    double gain(double v, int src) {
        if (src != G_FLAT) {
            v *= meta.coinMult();
            if (src != G_BANK) v *= streakMult();
            rateAcc += v;                                  // the offline rate learns from income without temporary boosts
            if (src != G_WAVE) {
                if (boosted()) v *= meta.boostMult();
                if (rushT > 0) v *= 2;
            }
        }
        coins += v;
        return v;
    }

    // =============================================================== loop
    @Override
    protected void onDraw(Canvas c) {
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        update(dt);
        frame(c);
        if (running) postInvalidateOnAnimation();
    }

    /** Draws one frame on the full screen: side bars (wide screens) plus the portrait stage. */
    void frame(Canvas c) {
        if (stageX > 0) drawSides(c);
        c.save();
        c.translate(stageX, 0);
        c.clipRect(0, 0, W, H);
        render(c);
        c.restore();
    }

    boolean paused() {
        for (Screen s : screens) if (s.pauses()) return true;
        return false;
    }

    Screen top() { return screens.isEmpty() ? null : screens.get(screens.size() - 1); }

    /** The topmost screen that isn't on its way out: the one that gets touches and Back. */
    Screen topLive() {
        for (int i = screens.size() - 1; i >= 0; i--) if (screens.get(i).out < 0) return screens.get(i);
        return null;
    }

    void push(Screen s) {
        screens.add(s);
        ui.down = 0;
        holdId = 0;
        sfx.bank("panel_open", 0.45f, 0, 50);
    }

    void update(float dt) {
        time += dt;
        ui.tick(dt);
        toastT -= dt;
        calloutT -= dt;
        trauma = Math.max(0, trauma - dt * 1.6f);
        flashA = Math.max(0, flashA - dt * 3.5f);
        float sh = meta.shake ? trauma * trauma : 0;
        shakeX = (float) (Math.sin(time * 71.3) + 0.5 * Math.sin(time * 37.9)) * sh * u * 2.6f;
        shakeY = (float) (Math.cos(time * 63.1) + 0.5 * Math.sin(time * 29.3)) * sh * u * 2.6f;
        for (int i = screens.size() - 1; i >= 0; i--) {
            Screen s = screens.get(i);
            s.update(dt);
            if (s.gone()) screens.remove(i);
        }
        if (hitStop > 0) { hitStop -= dt; return; }
        boolean paused = paused();

        baseSqV += (-baseSq * 300 - baseSqV * 12) * dt; baseSq += baseSqV * dt;
        coinBumpV += (-coinBump * 420 - coinBumpV * 15) * dt; coinBump += coinBumpV * dt;
        gemBumpV += (-gemBump * 420 - gemBumpV * 15) * dt; gemBump += gemBumpV * dt;
        for (int i = 0; i < 6; i++) { tSqV[i] += (-tSq[i] * 380 - tSqV[i] * 13) * dt; tSq[i] += tSqV[i] * dt; }
        baseHurtT -= dt; baseHappyT -= dt; waveBannerT -= dt;
        comboPop = Math.max(0, comboPop - dt * 4);

        if (!paused) {
            meta.playSec += dt;
            rateClock += dt;                               // income rate for offline earnings and reward sizing
            if (rateClock >= 5) { coinRate = coinRate * 0.9 + (rateAcc / rateClock) * 0.1; rateAcc = 0; rateClock = 0; }
            updateWave(dt);
            updateEnemies(dt);
            updateTowers(dt);
            updateShots(dt);
            updateSpells(dt);
            updateKing(dt);
            updateCourier(dt);
            updateCombo(dt);
            updateHold(dt);
            for (int i = puddles.size() - 1; i >= 0; i--) { float[] q = puddles.get(i); q[3] -= dt; if (q[3] <= 0) puddles.remove(i); }
            if (chestUp) chestT += dt;
            questCheck -= dt;
            if (questCheck <= 0) { questCheck = 5; meta.ensureQuests(bestWave); }
        }
        updateParts(dt);
        for (int i = pops.size() - 1; i >= 0; i--) { Pop p = pops.get(i); p.age += dt; p.y += p.vy * dt; p.vy *= (1 - dt * 3); if (p.age >= p.max) pops.remove(i); }

        // eyes follow nearest enemy, else the last tap
        Enemy n = nearest(baseX, baseY, 1e9f);
        if (n != null) { lookX = n.x; lookY = n.y; }
    }

    // ---------------------------------------------------------------- front door
    /** The player tapped PLAY on the title. */
    void enterKeep() {
        entered = true;
        if (!meta.seen(H_STORY)) { push(new Screens.Story(this)); return; }
        showPending();
    }

    /**
     * Welcome-back things, newest on top: offline earnings, then the 1.6 thank-you, then the daily gift, and once ever
     * the Starter Pack offer (at a session start, never mid-fight: every modal pauses the keep).
     */
    void showPending() {
        if (bestWave >= UNLOCK_STARTER && !meta.starter && meta.starterSeenMs == 0) {
            meta.starterSeenMs = meta.now();
            meta.save();
            push(new Screens.Offer(this));
        }
        if (giftAvailable()) push(new Screens.Daily(this));
        if (meta.v16Gift) { meta.v16Gift = false; push(Screens.Reward.granted(this, "Thanks for testing!", "a gift for playing early Jelly Keep", Meta.V16_GIFT)); }
        if (pendingOffline != null) { push(Screens.Reward.offline(this, pendingOffline[0], pendingOffline[1])); pendingOffline = null; }
    }

    boolean giftAvailable() { return meta.giftReady() && (bestWave >= UNLOCK_GIFT || meta.sessions > 1); }

    /** Back button: close the top screen, then the stump panel, then go to the title. False = let the app close. */
    boolean onBack() {
        Screen s = topLive();
        if (s != null) {
            if (s instanceof Screens.Title) return false;
            s.back();
            return true;
        }
        if (selected >= 0) { closePanel(); return true; }
        save();
        push(new Screens.Title(this));
        return true;
    }

    // ---------------------------------------------------------------- waves
    static int eliteMod(int w) { return w >= 15 && w % 10 == 5 ? (w / 10 - 1) % 5 : -1; }

    int waveSize(int w) { return w == 1 ? 4 : w == 2 ? 6 : w == 3 ? 8 : Math.min(6 + (int) (w * 1.3f), 42); }

    boolean anyTower() { for (int t : tType) if (t >= 0) return true; return false; }

    /** First run only: the countdown after wave 1 waits until a tower is built (the one blocking hint). */
    boolean waitingForFirstTower() { return !meta.seen(H_BUILD) && wavesCleared >= 1 && !anyTower(); }

    void updateWave(float dt) {
        if (phase == 0) {
            if (waitingForFirstTower() || !entered) { phaseT = Math.max(phaseT, 1.2f); return; }
            phaseT -= dt;
            if (phaseT <= 0) startWave();
        } else if (phase == 1) {
            if (toSpawn > 0) {
                spawnT -= dt;
                if (spawnT <= 0) { spawnT = (float) Math.max(0.33, 1.1 - wave * 0.025) * (mod == M_SWARM ? 0.55f : 1); spawn(); }
            } else if (enemies.isEmpty()) waveCleared();
        }
    }

    void startWave() {
        phase = 1;
        mod = eliteMod(wave);
        boss = null;
        int n = waveSize(wave);
        if (mod == M_SWARM) n *= 2;
        else if (mod == M_GIANTS) n = Math.max(4, (int) (n * 0.6f));
        waveTotal = toSpawn = n;
        spawnT = 0.3f;
        for (int k = 0; k < 3; k++) spellAd[k] = false;
        cue("wave_start", sfx.horn, 0.6f, 1f);
        if (mod >= 0) { banner(MOD_NAME[mod], MOD_HINT[mod], 2.4f); trauma = Math.min(1, trauma + 0.2f); }
    }

    int pickType() {
        float r = rnd.nextFloat(), acc = 0;
        switch (mod) {
            case M_SWARM: return r < 0.75f ? GLOOM : SKITTER;
            case M_STAMPEDE: return r < 0.75f ? SKITTER : GLOOM;
            case M_AIR: return r < 0.65f ? FLAPPER : GLOOM;
            case M_IRON: return r < 0.55f ? SHIELD : GLOOM;
            case M_GIANTS: return r < 0.6f ? BRUTE : GLOOM;
            default: break;
        }
        if (wave >= 13 && r < (acc += 0.07f)) return HEALER;
        if (wave >= 11 && r < (acc += 0.10f)) return SPLITTER;
        if (wave >= 9 && r < (acc += 0.11f)) return SHIELD;
        if (wave >= 7 && r < (acc += Math.min(0.14f, 0.05f + (wave - 7) * 0.015f))) return FLAPPER;
        if (wave >= 5 && r < (acc += Math.min(0.2f, (wave - 4) * 0.03f))) return BRUTE;
        if (wave >= 3 && r > 0.75f) return SKITTER;
        return GLOOM;
    }

    void spawn() {
        toSpawn--;
        int type = wave % 10 == 0 && toSpawn == 0 ? BOSS : pickType();
        double a = rnd.nextDouble() * Math.PI * 2;
        float far = Math.max(W, H);
        float rr0 = E_R[type] * u;
        float x = clamp(baseX + (float) Math.cos(a) * far, safeL + rr0 + u, W - safeR - rr0 - u);
        float y = clamp(baseY + (float) Math.sin(a) * far, fieldTop + rr0 * 2.2f, fieldBot - rr0 * 0.5f);
        Enemy e = spawnAt(type, x, y, 0.55f);
        if (mod == M_SWARM) e.hp = e.max = e.max * 0.5;
        if (mod == M_STAMPEDE) e.spd *= 1.3f;
        P ring = add(RING, e.x, e.y); if (ring != null) { ring.max = 0.5f; ring.sz = rr0 * 1.6f; ring.col = I4; }
        if (type == BOSS) bossArrives(e);
        if (type >= FLAPPER && type <= HEALER && (seenMask & (1 << type)) == 0) {          // first sighting: teach in context
            seenMask |= 1 << type;
            banner(E_NAME[type], E_HINT[type], 2.4f);
            cue("enemy_reveal", sfx.horn, 0.5f, 0.8f);
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

    String bossName(int w) {
        int k = Math.max(0, w / 10 - 1), cyc = k / BOSS_NAME.length;
        String n = BOSS_NAME[k % BOSS_NAME.length];
        return cyc == 0 ? n : n + " " + (cyc == 1 ? "II" : cyc == 2 ? "III" : String.valueOf(cyc + 1));
    }

    void bossArrives(Enemy e) {
        boss = e;
        bossSummonT = 5;
        trauma = Math.min(1, trauma + 0.5f);
        cue("boss_appear", sfx.hurt, 0.7f, 0.6f);
        banner("BOSS!", bossName(wave), 2.4f);
        buzz(new long[]{0, 60, 50, 60});
    }

    static boolean flyer(Enemy e) { return e.type == FLAPPER; }
    float lift(Enemy e) { return e.type == FLAPPER ? e.r * 1.9f + (float) Math.sin(e.ph * 0.6f) * e.r * 0.3f : 0; }
    /** Visual centre of an enemy's body (what taps and projectiles aim at). */
    float cy(Enemy e) { return e.y - e.r - lift(e); }

    void waveCleared() {
        double bonus = gain(Math.ceil(rewardScale(wave) * 5), G_WAVE);
        int cleared = wave;
        wavesCleared++;
        streak++;
        meta.quest(Meta.Q_WAVE, 1);
        meta.quest(Meta.Q_STREAK, streak);
        meta.jarAdd();
        wave++;
        int prevBest = bestWave;
        bestWave = Math.max(bestWave, wave);
        runBest = Math.max(runBest, wave);
        phase = 0; phaseT = 2.2f; mod = -1; boss = null;
        baseHp = baseMax;
        banner("WAVE CLEAR!", "+" + fmt(bonus) + " bonus" + (streak >= 2 ? "  ·  streak " + streak : ""), 2f);
        for (int t = 0; t < T_UNLOCK.length; t++)
            if (T_UNLOCK[t] > prevBest && T_UNLOCK[t] <= bestWave) { banner("NEW TOWER!", T_NAME[t] + " unlocked", 2.4f); cue("unlock", sfx.star, 0.8f, 1f); }
        for (int k = 0; k < 3; k++)
            if (S_UNLOCK[k] > prevBest && S_UNLOCK[k] <= bestWave) { banner("NEW SPELL!", S_NAME[k] + ": " + S_WHAT[k], 2.6f); cue("unlock", sfx.star, 0.8f, 1.15f); }
        if (prevBest >= 10 && cleared >= prevBest && !newBestShown) {            // the high-score moment
            newBestShown = true;
            banner("NEW BEST!", "wave " + cleared + " cleared", 2.4f);
            for (int i = 0; i < 40; i++) confetti(W / 2, fieldTop + 10 * u);
            cue("new_best", sfx.star, 0.7f, 1.25f);
        }
        baseHappyT = 1.6f; baseSqV -= 10;
        hitStop = 0.06f; trauma = Math.min(1, trauma + 0.3f);
        for (int i = 0; i < 40; i++) confetti(baseX, baseY - baseR * 2);
        for (int i = 0; i < 8; i++) coinPart(baseX, baseY - baseR, 1.2f);
        cue("wave_clear", sfx.levelUp, 0.8f, 1f);
        if (wavesCleared % 5 == 0 && !chestUp) { chestUp = true; chestT = 0; chestX = baseX + 15 * u; chestY = baseY + 15 * u; }
        if (cleared == 10 && meta.bossBest == 10 && !meta.seen(H_REVIEW)) {   // first boss down: a peak moment to ask
            meta.see(H_REVIEW);
            MonetizationFactory.askForReview(activity);
        }
        save();
        meta.save();
    }

    void defeat() {
        phase = 2;
        hitStop = 0.2f; trauma = 1; flash(I4, 0.8f);
        cue("defeat", sfx.defeat, 0.9f, 1f);
        buzz(new long[]{0, 80, 60, 120});
        int left = toSpawn + enemies.size();
        double bossLeft = boss != null && !boss.dead ? boss.hp / boss.max : -1;
        push(new Screens.Defeat(this, left, waveTotal, bossLeft, revivedWave != wave && time >= reviveReady, Math.max(0, reviveReady - time)));
        save();
    }

    /** Second chance in the same fight: full health, a 3 s shield, enemies knocked back, streak kept. */
    void revive() {
        phase = toSpawn > 0 || !enemies.isEmpty() ? 1 : 0;
        phaseT = Math.max(phaseT, 1f);
        revivedWave = wave;
        reviveReady = time + REVIVE_COOLDOWN;            // ads can't brute-force every wall
        baseHp = baseMax; baseHappyT = 1.2f; shieldT = 3f;
        for (Enemy e : enemies) {
            float dx = e.x - baseX, dy = e.y - baseY, d = (float) Math.max(1, Math.hypot(dx, dy));
            e.kx += dx / d * 120 * u; e.ky += dy / d * 120 * u; e.atkT = 1f;
        }
        ring(baseX, baseY, A6, 3f);
        flash(A6, 0.5f); cue("revive", sfx.levelUp, 0.7f, 1.1f);
    }

    void retreat() {
        phase = 0; phaseT = 2f;
        for (Enemy e : enemies) wisps(e.x, e.y, 5);
        enemies.clear(); shots.clear(); boss = null; freezeT = 0;
        wave = Math.max(1, wave - 3);
        streak = 0;
        baseHp = baseMax;
        save();
        if (meta.interstitialDue() && ads.interstitialReady()) {
            meta.lastAdMs = meta.now();
            inAd = true;
            ads.showInterstitial(() -> inAd = false);
        }
    }

    // ---------------------------------------------------------------- enemies
    void updateEnemies(float dt) {
        if (boss != null && !boss.dead && boss.emerge <= 0 && freezeT <= 0) {
            bossSummonT -= dt;
            if (bossSummonT <= 0) {                                   // the boss calls for help
                bossSummonT = 5;
                for (int k = -1; k <= 1; k += 2) {
                    Enemy m = spawnAt(GLOOM, boss.x + k * boss.r * 1.4f, boss.y + u, 0.4f);
                    m.hp = m.max = m.max * 0.8;
                    P r = add(RING, m.x, m.y); if (r != null) { r.max = 0.45f; r.sz = 9 * u; r.col = I5; }
                }
                waveTotal += 2;
                boss.hitT = 0.12f;
                cue("boss_summon", sfx.horn, 0.35f, 0.7f);
            }
            if (!boss.enraged && boss.hp < boss.max * 0.3) {
                boss.enraged = true; boss.spd *= 1.4f;
                pop(boss.x, cy(boss) - boss.r * 1.8f, "ENRAGED!", A5, 6);
                trauma = Math.min(1, trauma + 0.4f); cue("boss_enrage", sfx.hurt, 0.8f, 0.5f);
            }
        }
        for (int i = enemies.size() - 1; i >= 0; i--) {
            if (i >= enemies.size()) continue;
            Enemy e = enemies.get(i);
            if (e.emerge > 0) { e.emerge -= dt; continue; }   // ANTICIPATION: rising out of the dark
            e.hitT -= dt; e.slowT -= dt; e.bubT -= dt; e.burnT -= dt;
            e.kx *= (1 - dt * 7); e.ky *= (1 - dt * 7);
            if (freezeT > 0) { e.x += e.kx * dt; e.y += e.ky * dt; continue; }   // trapped in bubbles
            e.ph += dt * (e.type == SKITTER ? 14 : 7);
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
                    if (healed && !sfx.bank("healer_heal", 0.3f, 0.1f, 300)) sfx.play(sfx.star, 0.18f, 1.5f);
                }
            }
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
        if (shieldT > 0) {                                                    // revive shield
            ring(baseX, baseY - baseR * 0.4f, A7, 1.1f);
            sfx.bank("click", 0.3f, 0.1f, 60, 1.4f);
            return;
        }
        baseHp -= e.dmg;
        baseHurtT = 0.35f; baseSqV += 6;
        trauma = Math.min(1, trauma + (e.type == BOSS ? 0.6f : 0.15f));
        flash(I4, e.type == BOSS ? 0.5f : 0.22f);
        long now = System.currentTimeMillis();
        if (now - lastHitSnd > 120) { lastHitSnd = now; if (!sfx.bank("king_hurt", 0.6f, 0.08f, 0)) sfx.play(sfx.hurt, 0.6f, 0.9f + sndRnd.nextFloat() * 0.2f); haptic(HapticFeedbackConstants.KEYBOARD_TAP); }
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
        if (freezeT > 0) d *= 1.25;
        e.hp -= d; e.hitT = 0.09f;
        float mass = e.type == BOSS ? 0.15f : e.type == BRUTE || e.type == SHIELD ? 0.4f : 1f;
        e.kx += kx * mass; e.ky += ky * mass;
        if (e.hp <= 0 && !e.dead) kill(e);
    }

    void kill(Enemy e) {
        e.dead = true;
        enemies.remove(e);
        burstKills++;
        meta.kills++;
        meta.quest(Meta.Q_KILL, 1);
        if (tapping) { meta.tapKills++; meta.quest(Meta.Q_TAPKILL, 1); }
        double got = gain(e.reward, G_KILL);
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
        pop(e.x, cy(e) - e.r, "+" + fmt(got), A6, e.type == BOSS ? 7 : 4.2f);
        if (meta.luckyChance() > 0 && rnd.nextDouble() < meta.luckyChance()) {          // Vault: Lucky Drops
            double pouch = gain(e.reward * meta.pouchMult(), G_KILL);
            for (int i = 0; i < 8; i++) coinPart(e.x, e.y - e.r, 1.3f);
            pop(e.x, cy(e) - e.r * 2.6f, "LUCKY +" + fmt(pouch), A7, 4.8f);
            cue("lucky_pouch", sfx.star, 0.5f, 1.3f);
        }
        long now = System.currentTimeMillis();
        if (now - lastDieSnd > 50) { lastDieSnd = now; if (!sfx.bank("enemy_die", 0.55f, 0.12f, 0)) sfx.play(sfx.die, 0.55f, 0.85f + sndRnd.nextFloat() * 0.35f); }
        if (e.type == BRUTE || e.type == BOSS) {
            hitStop = e.type == BOSS ? 0.14f : 0.05f;
            trauma = Math.min(1, trauma + (e.type == BOSS ? 0.9f : 0.3f));
            flash(e.type == BOSS ? A6 : 0xFFFFFFFF, e.type == BOSS ? 0.6f : 0.2f);
        }
        if (e.type == BOSS) {
            boss = null;
            for (int i = 0; i < 50; i++) confetti(e.x, e.y);
            buzz(new long[]{0, 50, 40, 90}); cue("boss_die", sfx.crit, 1f, 0.9f);
            boolean first = wave > meta.bossBest;
            long gems = first ? 25 : 3;
            if (first) meta.bossBest = wave;
            meta.gems += gems;
            meta.quest(Meta.Q_BOSS, 1);
            for (int i = 0; i < (first ? 10 : 3); i++) gemPart(e.x, e.y - e.r * 2, 1.2f);
            pop(e.x, cy(e) - e.r * 3.2f, "+" + gems + " gems", A7, 6);
            meta.save();
        }
    }

    Enemy nearest(float x, float y, float range) {
        Enemy best = null; float bd = range;
        for (Enemy e : enemies) { float d = (float) Math.hypot(e.x - x, e.y - y); if (d < bd) { bd = d; best = e; } }
        return best;
    }

    /** An enemy under the finger: taps on it must squish, even right next to a stump. */
    boolean enemyAt(float x, float y) {
        for (Enemy e : enemies) if (e.emerge <= 0 && Math.hypot(e.x - x, cy(e) - y) < e.r * 1.3f + 3 * u) return true;
        return false;
    }

    /** Closest to the base within range of (x,y): the classic "first" targeting. */
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

    /**
     * The Jelly King bonks whatever reaches him, once a second, for a tap's worth of damage: idle players can hold the
     * first waves without tapping, and STRIKE upgrades matter even to those who never tap.
     */
    void updateKing(float dt) {
        bonkT -= dt;
        if (bonkT > 0 || phase != 1) return;
        Enemy t = null; float bd = Float.MAX_VALUE;
        for (Enemy e : enemies) {
            if (e.emerge > 0) continue;
            float d = (float) Math.hypot(e.x - baseX, e.y - (baseY - baseR * 0.3f));
            if (d < baseR * 1.1f + e.r + 3 * u && d < bd) { bd = d; t = e; }
        }
        if (t == null) return;
        bonkT = 1f;
        baseSqV -= 9;
        double d = tapBase();
        float dx = t.x - baseX, dy = t.y - baseY, dist = (float) Math.max(1, Math.hypot(dx, dy));
        ring(t.x, cy(t), A6, 0.5f);
        hitNumber(t.x, cy(t) - t.r, d);
        if (!sfx.bank("king_bonk", 0.45f, 0.1f, 0)) sfx.play(sfx.bonk, 0.4f, 0.9f + sndRnd.nextFloat() * 0.2f);
        damage(t, d, dx / dist * 70 * u, dy / dist * 70 * u);
    }

    // ---------------------------------------------------------------- towers
    /** Drummer speed bonus for its two neighbours: +15% at level 1, levelling toward a +50% cap. */
    static float drumBuff(int lv) { return 1.15f + 0.35f * (1 - (float) Math.pow(0.88, lv)); }

    /** Stumps form a ring 0..5; a Drummer speeds up the two stumps beside it. */
    static int ringNext(int i) { return (i + 1) % 6; }
    static int ringPrev(int i) { return (i + 5) % 6; }

    void updateTowers(float dt) {
        for (int i = 0; i < 6; i++) tBuff[i] = 1;
        for (int j = 0; j < 6; j++) {
            if (tType[j] != DRUMMER) continue;
            float b = drumBuff(tLv[j]);
            int a = ringNext(j), c = ringPrev(j);
            if (tType[a] >= 0 && tType[a] != DRUMMER) tBuff[a] = Math.max(tBuff[a], b);
            if (tType[c] >= 0 && tType[c] != DRUMMER) tBuff[c] = Math.max(tBuff[c], b);
        }
        float speed = (float) meta.towerSpeed();
        for (int i = 0; i < 6; i++) {
            int t = tType[i];
            if (t < 0) continue;
            tBeat[i] -= dt;
            tCool[i] -= dt * tBuff[i] * speed;
            if (t == BANKER || t == DRUMMER) {                       // no target needed
                if (phase == 2) continue;
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
        double v = gain(bankerPay(tLv[i]), G_BANK);
        tSqV[i] -= 9;
        sfx.bank("banker_coin", 0.3f, 0.08f, 250);
        for (int k = 0; k < 3; k++) coinPart(padX[i], padY[i] - 12 * u, 0.9f);
        pop(padX[i], padY[i] - 18 * u, "+" + fmt(v), A6, 3.8f);
    }

    void beat(int i) {
        tSqV[i] -= 5;
        tBeat[i] = 0.18f;
        if (((int) (time * 2)) % 2 == 0) {
            P ring = add(RING, padX[i], padY[i] - u); if (ring != null) { ring.max = 0.45f; ring.sz = 12 * u; ring.col = A5; }
            if (!sfx.bank("drum_hit", 0.25f, 0.05f, 90)) sfx.play(sfx.bonk, 0.12f, 0.7f);
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
            if (!sfx.bank("popper_shoot", 0.45f, 0.07f, 45) && now - lastShootSnd > 40) { lastShootSnd = now; sfx.play(sfx.shoot, 0.35f, 0.9f + sndRnd.nextFloat() * 0.3f); }
        } else if (t == SPARKLER) {
            tSqV[i] -= 3;
            float side = rnd.nextBoolean() ? 1 : -1;                 // alternates hands
            Shot s = new Shot(2, sx + side * 6 * u, muzzleY(i, t) - 2 * u);
            s.tg = tg; s.dmg = towerDmg(i);
            shots.add(s);
            if (!sfx.bank("sparkler_fizz", 0.3f, 0.1f, 70)) sfx.bank("popper_shoot", 0.22f, 0.12f, 70, 1.7f);
        } else if (t == SNIPER) {
            tSqV[i] -= 10;
            float mx = sx + tFace[i] * 9 * u, my = muzzleY(i, t);
            P tr = add(TRACER, mx, my); if (tr != null) { tr.max = 0.16f; tr.hx[0] = tg.x; tr.hy[0] = cy(tg); }
            double d = towerDmg(i);
            fxBurst(tg.x, cy(tg), tg.r * 2.2f);
            hitNumber(tg.x, cy(tg) - tg.r, d);
            damage(tg, d, (tg.x - mx) * 0.6f, 0, true);
            trauma = Math.min(1, trauma + 0.1f);
            if (!sfx.bank("sniper_shot", 0.6f, 0.05f, 0)) sfx.play(sfx.shoot, 0.5f, 0.55f);
            sfx.bank("pebble_hit", 0.5f, 0.05f, 40, 0.8f);
        } else if (t == MORTAR || t == BOMB) {
            tSqV[i] -= 10;
            Shot s = new Shot(t == BOMB ? 4 : 1, sx + tFace[i] * 3 * u, sy - 4 * u);
            s.tx = tg.x; s.ty = tg.y;
            s.ox = s.x; s.oy = s.y; s.dur = t == BOMB ? 1.05f : 0.9f; s.dmg = towerDmg(i);
            shots.add(s);
            if (!sfx.bank("mortar_launch", 0.6f, 0.06f, 60, t == BOMB ? 0.75f : 1f)) sfx.play(sfx.lob, 0.5f, 0.9f + sndRnd.nextFloat() * 0.2f);
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
                if (k >= enemies.size()) continue;
                Enemy e = enemies.get(k);
                if (Math.hypot(e.x - padX[i], e.y - padY[i]) < rr + e.r) { e.bubT = 1.3f; damage(e, d, 0, 0); }
            }
            if (!sfx.bank("bubble_pop", 0.4f, 0.1f, 120)) sfx.bank("strike_hit", 0.3f, 0.1f, 120, 1.3f);
        } else if (t == LANTERN) {                                      // steady burn on everything nearby
            sfx.bank("lantern_crackle", 0.22f, 0.1f, 450);
            float rr = T_RANGE[LANTERN] * u;
            double d = towerDmg(i) / T_RATE[LANTERN];
            for (int k = enemies.size() - 1; k >= 0; k--) {
                if (k >= enemies.size()) continue;
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
                if (k >= enemies.size()) continue;
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
            if (i >= shots.size()) continue;
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
                    if (!sfx.bank("goo_splat", 0.7f, 0.08f, 50)) sfx.play(sfx.splat, 0.5f, 1f + sndRnd.nextFloat() * 0.2f);
                    trauma = Math.min(1, trauma + 0.1f);
                    for (int n = enemies.size() - 1; n >= 0; n--) {
                        if (n >= enemies.size()) continue;
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
        if (!sfx.bank("bomb_blast", 0.8f, 0.06f, 60)) {
            if (!sfx.bank("wall_slam", 0.8f, 0.05f, 60, 0.85f)) sfx.play(sfx.splat, 0.6f, 0.7f);
            sfx.play(sfx.crit, 0.45f, 0.9f);
        }
        burstKills = 0;
        for (int n = enemies.size() - 1; n >= 0; n--) {
            if (n >= enemies.size()) continue;
            Enemy e = enemies.get(n);
            if (flyer(e)) continue;
            float dx = e.x - s.tx, dy = e.y - s.ty, dist = (float) Math.max(1, Math.hypot(dx, dy));
            if (dist < rr + e.r) damage(e, s.dmg, dx / dist * 90 * u, dy / dist * 90 * u);
        }
        multiKill();
        hitNumber(s.tx, s.ty - 6 * u, s.dmg);
    }

    // ---------------------------------------------------------------- spells
    boolean spellUnlocked(int k) { return bestWave >= S_UNLOCK[k]; }

    float spellCdMax(int k) { return S_CD[k] * (float) meta.spellCdMult(); }

    void updateSpells(float dt) {
        for (int k = 0; k < 3; k++) {
            spellFlash[k] = Math.max(0, spellFlash[k] - dt * 1.5f);
            if (spellCd[k] > 0) {
                spellCd[k] -= dt;
                if (spellCd[k] <= 0) { spellCd[k] = 0; if (spellUnlocked(k)) { spellFlash[k] = 1; cue("spell_ready", sfx.star, 0.25f, 1.6f); } }
            }
        }
        if (freezeT > 0) freezeT -= dt;
        if (shieldT > 0) shieldT -= dt;
        if (rushT > 0) {
            rushT -= dt;
            if (rnd.nextFloat() < dt * 14) { P p = add(COIN, rnd.nextFloat() * W, fieldTop); if (p != null) { p.vy = 30 * u; p.vx = 0; p.delay = 0.5f + rnd.nextFloat() * 0.4f; p.max = 3; p.sz = 1.4f * u; } }
        }
    }

    void castSpell(int k) {
        if (!spellUnlocked(k)) { toast(S_NAME[k] + " unlocks at wave " + S_UNLOCK[k]); return; }
        if (phase == 2) return;
        if (spellCd[k] > 0) { offerRecharge(k); return; }
        spellCd[k] = spellCdMax(k);
        meta.quest(Meta.Q_SPELL, 1);
        meta.see(H_SPELL);
        if (k == SLAM) slam();
        else if (k == FREEZE) freeze();
        else goldRush();
    }

    void offerRecharge(int k) {
        if (spellAd[k] || Math.ceil(spellCd[k]) <= 3) { toast(S_NAME[k] + " ready in " + (int) Math.ceil(spellCd[k]) + "s"); return; }
        push(Screens.Confirm.ad(this, "Recharge " + S_NAME[k] + "?", "Watch a short ad to cast it again right now (once per wave).",
                "RECHARGE", Ads.SPELL, () -> { spellCd[k] = 0; spellAd[k] = true; spellFlash[k] = 1; }));
    }

    void slam() {
        float rr = 45 * u;
        double d = tapBase() * 12;
        burstKills = 0;
        ring(baseX, baseY, A6, rr / (14 * u)); ring(baseX, baseY, A5, rr / (14 * u) * 0.6f);
        fxBurst(baseX, baseY - baseR, 34 * u);
        for (int k = 0; k < 18; k++) dust(baseX + (rnd.nextFloat() - 0.5f) * 30 * u, baseY + (rnd.nextFloat() - 0.5f) * 8 * u);
        trauma = Math.min(1, trauma + 0.7f); hitStop = Math.max(hitStop, 0.08f); flash(A6, 0.35f);
        baseSqV += 14;
        if (!sfx.bank("wall_slam", 0.9f, 0.04f, 0, 0.7f)) sfx.play(sfx.splat, 0.8f, 0.6f);
        cue("spell_slam", sfx.crit, 0.7f, 0.8f);
        buzz(new long[]{0, 40, 30, 70});
        for (int n = enemies.size() - 1; n >= 0; n--) {
            if (n >= enemies.size()) continue;
            Enemy e = enemies.get(n);
            if (e.emerge > 0) continue;
            float dx = e.x - baseX, dy = e.y - baseY, dist = (float) Math.max(1, Math.hypot(dx, dy));
            if (dist < rr + e.r) { hitNumber(e.x, cy(e) - e.r, d); damage(e, d, dx / dist * 160 * u, dy / dist * 160 * u); }
        }
        multiKill();
    }

    void freeze() {
        freezeT = 4;
        for (Enemy e : enemies) e.bubT = 4;
        ring(baseX, baseY, A7, 4f);
        for (int k = 0; k < 24; k++) {
            P b = add(BUBBLE, baseX + (rnd.nextFloat() - 0.5f) * W * 0.8f, fieldTop + rnd.nextFloat() * (fieldBot - fieldTop));
            if (b == null) break;
            b.vy = -(6 + rnd.nextFloat() * 10) * u; b.max = 1.4f; b.sz = (1.2f + rnd.nextFloat() * 1.6f) * u;
        }
        flash(A7, 0.3f);
        sfx.bank("strike_hit", 0.8f, 0.05f, 0, 0.75f);
        cue("spell_freeze", sfx.star, 0.5f, 0.7f);
    }

    void goldRush() {
        rushT = 15;
        flash(A6, 0.4f);
        for (int i = 0; i < 16; i++) coinPart(baseX, baseY - baseR, 1.5f);
        banner("GOLD RUSH!", "coins ×2 for 15 s", 1.8f);
        cue("spell_rush", sfx.levelUp, 0.8f, 1.3f);
    }

    // ---------------------------------------------------------------- combos, callouts
    static final int[] COMBO_AT = {10, 20, 30, 40, 60, 100};
    static final String[] COMBO_TXT = {"NICE!", "GREAT!", "AMAZING!", "JELLY-TASTIC!", "UNSTOPPABLE!", "LEGENDARY!"};

    void updateCombo(float dt) {
        comboT += dt;
        if (combo > 0 && comboT > 1.2f) combo = 0;
    }

    void callout(String s, int col) { callout = s; calloutT = 1.1f; calloutCol = col; }

    void multiKill() {
        if (burstKills < 2) return;
        callout(burstKills == 2 ? "DOUBLE!" : burstKills == 3 ? "TRIPLE!" : "MULTI ×" + burstKills + "!", A7);
        float rise = Math.min(burstKills, 6) * 0.06f;                       // each bigger multi-kill rings higher
        if (!sfx.bank("multi_kill", 0.5f, 0, 0, 1 + rise)) sfx.play(sfx.star, 0.4f, 1.2f + rise);
    }

    // ---------------------------------------------------------------- courier & chest
    void updateCourier(float dt) {
        if (bestWave < UNLOCK_COURIER || !entered) return;
        if (!courierUp) {
            if (phase != 2) courierT -= dt;
            if (courierT <= 0) { courierUp = true; courierLife = 0; cue("courier", sfx.star, 0.35f, 1.8f); }
            return;
        }
        courierLife += dt;
        float k = courierLife / COURIER_S;
        courierX = lerp(-14 * u, W + 14 * u, k);
        courierY = fieldTop + 16 * u + (float) Math.sin(courierLife * 2.2f) * 2 * u;
        if (k >= 1) { courierUp = false; courierT = nextCourier(); }
    }

    float nextCourier() { return 360 + rnd.nextFloat() * 180; }

    void openCourier() {
        courierUp = false;
        courierT = nextCourier();
        meta.see(H_COURIER);
        push(new Screens.Courier(this, rnd.nextDouble() < meta.gemFinderChance()));
    }

    void openChest() {
        chestUp = false;
        meta.see(H_CHEST);
        meta.quest(Meta.Q_CHEST, 1);
        cue("chest_open", sfx.star, 0.9f, 1f); flash(A6, 0.4f); trauma = Math.min(1, trauma + 0.3f);
        burst(chestX, chestY - 3 * u, 25, A6, 1.4f);
        push(Screens.Reward.chest(this, incomePerMin() * 0.6));
    }

    // ---------------------------------------------------------------- ads & purchases (shared flows)
    /** Every rewarded ad goes through here: Royal Pass skips it, every one earns a Patron star. */
    void watchAd(String placement, Runnable reward) {
        if (meta.royalPass) { reward.run(); patronStar(); toast("Royal Pass: reward granted, no ad"); return; }
        int st = ads.rewardedState();
        if (st != Ads.READY) {
            ads.load();
            toast(st == Ads.LOADING ? "The ad is still loading, try again in a moment" : "No ad available right now, try again soon");
            return;
        }
        inAd = true;
        ads.showRewarded(placement, earned -> {
            inAd = false;
            if (earned) { reward.run(); patronStar(); }
            else toast("No reward: the ad didn't finish");
        });
    }

    void patronStar() {
        String perk = meta.addPatron();
        P s = add(GEMP, W / 2, H * 0.45f); if (s != null) { s.vy = -30 * u; s.delay = 0.3f; s.max = 3; s.sz = 2 * u; }
        if (perk != null) {
            banner("PATRON PERK!", perk, 2.6f);
            for (int i = 0; i < 30; i++) confetti(W / 2, fieldTop + 10 * u);
            cue("perk", sfx.star, 0.9f, 1.1f);
        } else toast("+1 Patron star · " + meta.patron + "/" + meta.patronNextAt());
    }

    void boostAd() {
        meta.see(H_BOOST);
        long now = meta.now();
        if (boostUntil - now > meta.boostBankMs() - meta.boostPerAdMs() / 2) { toast("Boost bank is full (" + Meta.fmtDur((boostUntil - now) / 1000) + ")"); return; }
        watchAd(Ads.BOOST, () -> {
            long n2 = meta.now();
            boostUntil = Math.min(Math.max(n2, boostUntil) + meta.boostPerAdMs(), n2 + meta.boostBankMs());
            flash(A6, 0.35f); cue("boost_on", sfx.buy, 0.8f, 1.2f); coinBumpV -= 8;
            save();
        });
    }

    void claimGift(boolean doubled) {
        int i = meta.claimGift();
        if (doubled) { meta.gems += Meta.GIFT_GEMS[i]; meta.save(); }
        int m = doubled ? 2 : 1;
        double c = incomePerMin() * Meta.GIFT_COIN_MIN[i] * m;
        if (c > 0) { coins += c; coinShower(c); }
        if (Meta.GIFT_BOOST_MIN[i] > 0) {
            long n = meta.now();
            boostUntil = Math.min(Math.max(n, boostUntil) + Meta.GIFT_BOOST_MIN[i] * 60_000L * m, n + meta.boostBankMs());
        }
        if (Meta.GIFT_GEMS[i] > 0) gemShower(Meta.GIFT_GEMS[i] * m);
        save();
    }

    boolean buyCrate(int k) {
        long cost = k == 0 ? 40 : 120;
        if (meta.gems < cost) { deny(); return false; }
        meta.gems -= cost;
        double c = incomePerMin() * (k == 0 ? 15 : 60);
        coins += c;
        coinShower(c);
        save(); meta.save();
        return true;
    }

    boolean buyVault(int i) {
        double f = baseHp / baseMax;
        if (!meta.buyVault(i)) { deny(); return false; }
        baseMax = baseMaxFor(fortLv); baseHp = baseMax * f;          // Thick Jelly applies at once
        sfx.bank("tower_upgrade", 0.7f, 0.03f, 0); gemBumpV -= 6;
        return true;
    }

    boolean canRebirth() { return runBest >= Meta.REBIRTH_AT; }

    void rebirth() {
        long got = Meta.jellyFor(runBest);
        meta.rebirth(runBest);
        coins = START_COINS; wave = 1; runBest = 1; tapLv = 0; fortLv = 0; streak = 0;
        coinRate = 0; rateAcc = 0; rateClock = 0;          // lump rewards size from the new run, not the old one's income
        for (int i = 0; i < 6; i++) { tType[i] = -1; tLv[i] = 0; }
        enemies.clear(); shots.clear(); puddles.clear(); boss = null; chestUp = false; selected = -1;
        phase = 0; phaseT = 3f; freezeT = rushT = shieldT = 0;
        for (int k = 0; k < 3; k++) spellCd[k] = 0;
        baseMax = baseMaxFor(0); baseHp = baseMax;
        flash(A7, 0.9f); trauma = 1; hitStop = 0.2f;
        for (int i = 0; i < 80; i++) confetti(baseX, baseY - baseR * 2);
        banner("REBORN!", "+" + got + " Royal Jelly · +" + Math.round(got * 5) + "% coins & damage", 3f);
        cue("rebirth", sfx.levelUp, 1f, 0.9f);
        save(); meta.save();
    }

    void coinShower(double amt) {
        for (int i = 0; i < 24; i++) coinPart(W / 2 + (rnd.nextFloat() - 0.5f) * 40 * u, H * 0.48f, 1.4f);
        pop(W / 2, H * 0.42f, "+" + fmt(amt), A6, 9);
        cue("coin_shower", sfx.levelUp, 0.8f, 1.2f);
    }

    void gemShower(long n) {
        for (int i = 0; i < Math.min(14, 3 + n / 5); i++) gemPart(W / 2 + (rnd.nextFloat() - 0.5f) * 30 * u, H * 0.48f, 1.3f);
        pop(W / 2, H * 0.36f, "+" + n + " gems", A7, 7);
        cue("gem_shower", sfx.star, 0.8f, 1.2f);
    }

    void deny() {
        if (!sfx.bank("deny", 0.6f, 0, 120)) sfx.play(sfx.deny, 0.7f, 1f);
        haptic(HapticFeedbackConstants.LONG_PRESS);
    }

    void toast(String s) { toastMsg = s; toastT = 2.4f; }

    // Ads.Host
    @Override public void simulateAd(boolean rewarded, Runnable done) { push(new Screens.AdSim(this, rewarded, done)); }
    @Override public void adMessage(String msg) { toast(msg); }

    // Store.Host
    @Override public void granted(String sku) {
        boolean had = Store.ROYAL_PASS.equals(sku) ? meta.royalPass : Store.STARTER.equals(sku) ? meta.starter : Store.SPOON.equals(sku) && meta.spoon;
        long before = meta.gems;
        meta.grant(sku);
        if (had) return;                                   // a restore of something already applied: nothing to celebrate
        if (meta.gems > before) gemShower(meta.gems - before);
        if (Store.SPOON.equals(sku)) banner("GOLDEN SPOON!", "all coins ×2, forever", 2.6f);
        else if (Store.ROYAL_PASS.equals(sku)) banner("ROYAL PASS!", "no more ad breaks, rewards stay", 2.6f);
        else if (Store.STARTER.equals(sku)) banner("STARTER PACK!", "+50% coins forever", 2.6f);
        for (int i = 0; i < 40; i++) confetti(W / 2, fieldTop + 10 * u);
        save();
    }
    @Override public void confirmTestPurchase(String sku, String price, Runnable buy) { push(Screens.Confirm.purchase(this, sku, price, buy)); }
    @Override public void storeMessage(String msg) { toast(msg); }

    // ================================================================== input
    float downX, downY, lastDragY;
    boolean dragging, gestureOnModal;
    long lastMoveNs;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int a = ev.getActionMasked();
        int pi = ev.getActionIndex();
        float x = ev.getX(pi) - stageX, y = ev.getY(pi);
        Screen top = topLive();
        if (a == MotionEvent.ACTION_DOWN) gestureOnModal = top != null;
        if (top == null && !screens.isEmpty()) return true;      // a screen is closing: swallow the tap
        if (top != null) {                                       // modal: buttons fire on release so lists can scroll
            if (a != MotionEvent.ACTION_DOWN && !gestureOnModal) {  // the press that opened this screen: its release isn't a tap on it
                if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) { ui.down = 0; holdId = 0; }
                return true;
            }
            if (a == MotionEvent.ACTION_DOWN) {
                ui.down = ui.at(x, y); downX = x; downY = y; lastDragY = y; dragging = false; top.vel = 0; lastMoveNs = System.nanoTime();
            } else if (a == MotionEvent.ACTION_MOVE) {
                float yy = ev.getY(0);
                if (!dragging && Math.abs(yy - downY) > 2.2f * u && top.scrollMax > 0) { dragging = true; ui.down = 0; }
                if (dragging) {
                    long now = System.nanoTime();
                    float dy = yy - lastDragY, dt = Math.max(0.001f, (now - lastMoveNs) / 1e9f);
                    top.scrollBy(-dy); top.vel = -dy / dt * 0.6f;
                    lastDragY = yy; lastMoveNs = now;
                }
            } else if (a == MotionEvent.ACTION_UP) {
                int id = ui.down;
                ui.down = 0;
                if (!dragging) {
                    if (id != 0 && ui.contains(id, x, y)) { click(); top.tap(id); }
                    else if (id == 0) top.tapOutside(x, y);
                }
                performClick();
            } else if (a == MotionEvent.ACTION_CANCEL) ui.down = 0;
            return true;
        }
        if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) { press(x, y); return true; }
        if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) { ui.down = 0; holdId = 0; if (a == MotionEvent.ACTION_UP) performClick(); }
        return true;
    }

    @Override public boolean performClick() { return super.performClick(); }

    void click() { if (!sfx.bank("click", 0.6f, 0.04f, 20)) sfx.play(sfx.pop, 0.5f, 1.25f); haptic(HapticFeedbackConstants.VIRTUAL_KEY); }

    /** A recorded bank once it has been delivered (assets/sfx/<bank>_N.ogg), else the synth stand-in. */
    void cue(String bank, int synth, float vol, float rate) { if (!sfx.bank(bank, vol, 0.03f, 0)) sfx.play(synth, vol, rate); }

    void haptic(int kind) { if (meta.haptics) performHapticFeedback(kind); }

    /** Battle screen: everything acts on touch-down for snappy play; upgrade buttons repeat while held. */
    void press(float x, float y) {
        panelGeom();
        int id = ui.at(x, y);
        if (id != 0) {
            ui.down = id;
            ui.flash(id);
            if (battleButton(id) && (id == B_STRIKE || id == B_FORT || id == B_UP)) { holdId = id; holdT = 0.45f; holdGap = 0.16f; }
            return;
        }
        if (courierUp && Math.hypot(x - courierX, y - (courierY - 5 * u)) < 11 * u) { click(); openCourier(); return; }
        if (chestUp && Math.hypot(x - chestX, y - (chestY - 3 * u)) < 8 * u) { openChest(); return; }
        if (curPanel.contains(x, y)) return;                     // the panel's wood swallows stray taps
        if (!enemyAt(x, y)) {
            for (int i = 0; i < 6; i++) {
                if (Math.hypot(x - padX[i], y - (padY[i] - (tType[i] >= 0 ? 7 * u : 0))) < 11 * u) {
                    selected = selected == i ? -1 : i;
                    tSqV[i] -= 5;
                    click();
                    sfx.bank(selected < 0 ? "panel_close" : "panel_open", 0.45f, 0, 50);
                    return;
                }
            }
        }
        if (y > fieldTop - 6 * u && y < fieldBot) { if (selected >= 0) { sfx.bank("panel_close", 0.4f, 0, 50); selected = -1; } strike(x, y); }
    }

    /** Returns true when a purchase went through (so holding the button keeps buying). */
    boolean battleButton(int id) {
        if (id == B_STRIKE) return buyStrike();
        if (id == B_FORT) return buyFort();
        if (id == B_UP) return buyUpgrade();
        if (id == B_SELL) { sell(); return false; }
        if (id == B_CLOSE) { closePanel(); return false; }
        if (id == B_BOOST) { click(); boostAd(); return false; }
        if (id == B_GEAR) { click(); push(new Screens.Settings(this)); return false; }
        if (id >= B_NAV && id < B_NAV + 4) { nav(id - B_NAV); return false; }
        if (id >= B_SPELL && id < B_SPELL + 3) { castSpell(id - B_SPELL); return false; }
        if (id >= B_CARD && id < B_CARD + 10) { buildCard(id - B_CARD); return false; }
        return false;
    }

    void closePanel() { selected = -1; click(); sfx.bank("panel_close", 0.5f, 0, 50); }

    boolean navUnlocked(int k) {
        if (k == NAV_VAULT) return bestWave >= NAV_UNLOCK[k] || meta.gems > 0 || meta.patron > 0;
        if (k == NAV_REBIRTH) return bestWave >= NAV_UNLOCK[k] || meta.rebirths > 0;
        return bestWave >= NAV_UNLOCK[k];
    }

    void nav(int k) {
        if (!navUnlocked(k)) { deny(); toast(NAV_NAME[k].charAt(0) + NAV_NAME[k].substring(1).toLowerCase(Locale.US) + " unlocks at wave " + NAV_UNLOCK[k]); return; }
        click();
        if (k == NAV_QUESTS) { meta.see(H_QUESTS); push(new Screens.Quests(this)); }
        else if (k == NAV_VAULT) { meta.see(H_VAULT); push(new Screens.Vault(this)); }
        else if (k == NAV_SHOP) push(new Screens.Shop(this));
        else { meta.see(H_REBIRTH); push(new Screens.Rebirth(this)); }
    }

    void updateHold(float dt) {
        if (holdId == 0) return;
        if (ui.down != holdId) { holdId = 0; return; }
        holdT -= dt;
        if (holdT > 0) return;
        boolean ok = holdId == B_STRIKE ? buyStrike() : holdId == B_FORT ? buyFort() : buyUpgrade();
        if (!ok) { holdId = 0; return; }
        ui.flash(holdId);
        holdGap = Math.max(0.045f, holdGap * 0.82f);
        holdT = holdGap;
    }

    boolean buyStrike() {
        boolean ok = buy(tapCost(), () -> tapLv++, btnA, null);
        if (ok) meta.see(H_STRIKE);
        return ok;
    }

    boolean buyFort() {
        return buy(fortCost(), () -> {
            fortLv++;
            double f = baseHp / baseMax;
            baseMax = baseMaxFor(fortLv);
            baseHp = Math.min(baseMax, baseMax * f + baseMax * 0.35);            // fortifying also patches the king up
        }, btnB, null);
    }

    boolean buyUpgrade() {
        final int s = selected;
        if (s < 0 || tType[s] < 0) return false;
        boolean ok = buy(upCost(s), () -> tLv[s]++, upBtn, "tower_upgrade");
        if (!ok) return false;
        tSqV[s] -= 10;
        P a = add(ARROW, padX[s], padY[s] - 10 * u); if (a != null) { a.vy = -22 * u; a.max = 0.8f; a.sz = 6 * u; }
        meta.quest(Meta.Q_UPGRADE, 1);
        meta.see(H_UPGRADE);
        if (tLv[s] % 10 == 0) {                                              // a star: damage doubles
            pop(padX[s], padY[s] - 24 * u, "STAR! ×2 DAMAGE", A6, 5);
            for (int i = 0; i < 30; i++) confetti(padX[s], padY[s] - 12 * u);
            cue("tower_star", sfx.star, 0.9f, 1.1f);
            trauma = Math.min(1, trauma + 0.3f);
        }
        return true;
    }

    void buildCard(int k) {
        if (selected < 0 || k >= nShown) return;
        final int tt = shown[k], s = selected;
        if (bestWave < T_UNLOCK[tt]) {
            deny();
            pop(cardR[k].centerX(), cardR[k].top, "reach wave " + T_UNLOCK[tt], A7, 3.6f);
            return;
        }
        if (buy(T_COST[tt], () -> { tType[s] = tt; tLv[s] = 0; tCool[s] = 0.5f; }, cardR[k], "tower_build")) {
            meta.see(H_BUILD);
            tSqV[s] -= 14; ring(padX[s], padY[s], A6, 0.8f); for (int q = 0; q < 10; q++) dust(padX[s], padY[s]);
        }
    }

    void sell() {
        int s = selected;
        if (s < 0 || tType[s] < 0) return;
        double v = sellValue(s);
        coins += v; coinBumpV -= 6;
        P pf = add(POOF, padX[s], padY[s] - 8 * u); if (pf != null) { pf.max = 0.45f; pf.sz = 16 * u; }
        for (int k = 0; k < 5; k++) coinPart(padX[s], padY[s] - 8 * u, 1f);
        pop(padX[s], padY[s] - 16 * u, "+" + fmt(v), A6, 4.5f);
        tType[s] = -1; tLv[s] = 0;
        sfx.bank("panel_close", 0.5f, 0, 50);
        save();
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
            float gap = 2 * u, ch = rows == 1 ? 29 * u : 24.5f * u;
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
        sellBtn.set(tx, panelR.bottom - in - 9 * u, tx + 27 * u, panelR.bottom - in);
    }

    boolean buy(double cost, Runnable apply, RectF r, String snd) {
        if (coins < cost) {
            deny();
            pop(r.centerX(), r.top, "need " + fmt(cost - coins) + " more", A7, 3.6f);
            return false;
        }
        coins -= cost;
        apply.run();
        coinBumpV -= 6;
        burst(r.centerX(), r.top + 2 * u, 14, A6, 1f);
        if (snd == null || !sfx.bank(snd, 0.8f, 0.03f, 0)) sfx.play(sfx.buy, 0.8f, 1f);
        haptic(Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.VIRTUAL_KEY);
        save();
        return true;
    }

    /** The clicker half: squish the field where you tap. Hits within 1.2 s build a combo (up to ×1.5 damage). */
    void strike(float x, float y) {
        float rr = 8.5f * u;
        ring(x, y, A6, 0.65f);
        burst(x, y, 7, A5, 0.8f);
        boolean any = false;
        burstKills = 0;
        tapping = true;
        for (int i = enemies.size() - 1; i >= 0; i--) {
            if (i >= enemies.size()) continue;
            Enemy e = enemies.get(i);
            if (e.emerge > 0) continue;
            float dx = e.x - x, dy = cy(e) - y, dist = (float) Math.max(1, Math.hypot(dx, dy));
            if (dist < rr + e.r) {
                if (!any) { any = true; combo++; comboT = 0; comboPop = 1; }
                double d = tapDmg();
                hitNumber(e.x, cy(e) - e.r * 1.2f, d); fxBurst(e.x, cy(e), e.r * 1.6f);
                damage(e, d, dx / dist * 60 * u, dy / dist * 60 * u);
            }
        }
        tapping = false;
        lookX = x; lookY = y;
        baseSqV += 2.5f;
        if (any) {
            meta.see(H_TAP);
            meta.quest(Meta.Q_COMBO, combo);
            if (combo > meta.bestCombo) meta.bestCombo = combo;
            for (int k = 0; k < COMBO_AT.length; k++) if (combo == COMBO_AT[k]) {
                callout(COMBO_TXT[k], A6);
                if (!sfx.bank("combo", 0.55f, 0, 0, 1 + k * 0.08f)) sfx.play(sfx.star, 0.55f, 1f + k * 0.08f);
                trauma = Math.min(1, trauma + 0.2f);
            }
            trauma = Math.min(1, trauma + 0.12f);
            float pitch = 1 + Math.min(combo, 30) * 0.012f;                  // rising pitch: the combo is audible
            if (!sfx.bank("strike_hit", 0.7f, 0.04f, 30, pitch)) sfx.play(sfx.hit, 0.7f, pitch);
            sfx.bank("pebble_hit", 0.35f, 0.15f, 30);
            haptic(HapticFeedbackConstants.KEYBOARD_TAP);
            multiKill();
        } else {
            if (!sfx.bank("strike", 0.4f, 0.08f, 30)) sfx.play(sfx.pop, 0.4f, 0.9f + sndRnd.nextFloat() * 0.2f);
        }
    }

    // ================================================================== particles
    static final int SPARK = 0, COIN = 1, CONF = 2, RING = 3, DUST = 4, WISP = 5, GOO = 6, POOF = 7, BURSTFX = 8, ARROW = 9, TRACER = 10, BUBBLE = 11, GEMP = 12;

    static final class P {
        int t, col; float x, y, vx, vy, age, max, sz, rot, vr, delay;
        final float[] hx = new float[5], hy = new float[5];
    }

    static final class Pop { String s; int col; float x, y, vy, age, max, size; }

    static final class Enemy {
        int type; float x, y, r, spd, ph, hitT, slowT, bubT, burnT, abT = 1f, kx, ky, atkT = 0.4f, emerge; boolean atk, dead, enraged; double hp, max, dmg, reward;
    }

    static final class Shot {
        final int type; float x, y, tx, ty, ox, oy, age, dur; double dmg; Enemy tg;
        final float[] hx = new float[6], hy = new float[6];
        Shot(int type, float x, float y) { this.type = type; this.x = x; this.y = y; for (int i = 0; i < 6; i++) { hx[i] = x; hy[i] = y; } }
    }

    P add(int t, float x, float y) {
        if (parts.size() > 450 && t != COIN && t != GEMP) return null;
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

    void coinPart(float x, float y, float power) { flyer(COIN, x, y, power, 1.5f * u); }

    void gemPart(float x, float y, float power) { flyer(GEMP, x, y, power, 1.8f * u); }

    private void flyer(int t, float x, float y, float power, float size) {
        P p = add(t, x, y); if (p == null) return;
        float a = -1.5708f + (rnd.nextFloat() - 0.5f) * 2.4f, s = (25 + rnd.nextFloat() * 40) * u * power;
        p.vx = (float) Math.cos(a) * s; p.vy = (float) Math.sin(a) * s;
        p.delay = 0.25f + rnd.nextFloat() * 0.25f; p.max = 3f; p.sz = size; p.rot = rnd.nextFloat() * 6;
    }

    void confetti(float x, float y) {
        P p = add(CONF, x, y); if (p == null) return;
        float a = -1.5708f + (rnd.nextFloat() - 0.5f) * 2.2f, s = (45 + rnd.nextFloat() * 80) * u;
        p.vx = (float) Math.cos(a) * s; p.vy = (float) Math.sin(a) * s;
        p.max = 1.6f + rnd.nextFloat(); p.sz = (1 + rnd.nextFloat()) * u;
        p.col = CONF_COLS[rnd.nextInt(4)]; p.rot = rnd.nextFloat() * 360; p.vr = (rnd.nextFloat() - 0.5f) * 900;
    }

    static final int[] CONF_COLS = {A4, A5, A6, A7};

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
                case COIN: case GEMP: {
                    RectF pill = p.t == COIN ? coinPill : gemPill;
                    float tx = pill.left + 1 * u, ty = pill.centerY();
                    p.rot += dt * 12;
                    if (p.age < p.delay) { p.vy += 110 * u * dt; p.vx *= (1 - dt * 3); p.vy *= (1 - dt * 3); }
                    else {
                        float dx = tx - p.x, dy = ty - p.y, d = (float) Math.max(1, Math.hypot(dx, dy));
                        float sp = (90 + (p.age - p.delay) * 520) * u, k = Math.min(1, dt * 9);
                        p.vx += (dx / d * sp - p.vx) * k; p.vy += (dy / d * sp - p.vy) * k;
                        if (d < 3.5f * u) {
                            parts.remove(i);
                            if (p.t == COIN) { coinBumpV += 3.5f; coinLand(); } else { gemBumpV += 4; if (!sfx.bank("gem_land", 0.25f, 0.1f, 40)) sfx.play(sfx.coin, 0.2f, 1.4f); }
                            continue;
                        }
                    }
                    System.arraycopy(p.hx, 0, p.hx, 1, 4); System.arraycopy(p.hy, 0, p.hy, 1, 4); p.hx[0] = p.x; p.hy[0] = p.y;
                    break;
                }
                default: break;
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
        if (!sfx.nth("coin", n, 0.18f, rate)) sfx.play(sfx.coin, 0.22f, 0.95f + sndRnd.nextFloat() * 0.15f);
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

    void banner(String a, String b, float life) { bannerA = a; bannerB = b; bannerLife = life; waveBannerT = life; }

    void buzz(long[] pattern) {
        if (!meta.haptics || vib == null || !vib.hasVibrator()) return;
        try { vib.vibrate(VibrationEffect.createWaveform(pattern, -1)); } catch (Exception ignored) { }
    }

    // ================================================================== render
    private boolean frontNow;
    private float[] sortY = new float[64];
    private int[] sortId = new int[64];

    void render(Canvas c) {
        if (bField == null) return;
        ui.begin();
        Screen top = top();
        boolean front = top instanceof Screens.Title || top instanceof Screens.Story;   // the HUD waits until you're in
        frontNow = front;
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
        }
        for (int i = 0; i < 6; i++) if (tType[i] == LANTERN) {                // warm lantern light on the ground
            float rr = T_RANGE[LANTERN] * u * (1 + 0.04f * (float) Math.sin(time * 9 + i) + 0.03f * (float) Math.sin(time * 23));
            glow(c, glowPaint, padX[i], padY[i] - 4 * u, rr, rr, 255);
        }
        if (selected >= 0 && tType[selected] >= 0 && T_RANGE[tType[selected]] > 0) {
            float rr = T_RANGE[tType[selected]] * u;
            stroke.setPathEffect(new DashPathEffect(new float[]{2 * u, 1.5f * u}, time * 6 * u));
            stroke.setColor(A6); stroke.setAlpha(170); stroke.setStrokeWidth(0.5f * u);
            c.drawCircle(padX[selected], padY[selected], rr, stroke);
            stroke.setPathEffect(null);
            fill.setShader(null); fill.setColor(A6); fill.setAlpha(25); c.drawCircle(padX[selected], padY[selected], rr, fill);
        }
        drawDrumLinks(c);
        for (int i = 0; i < 6; i++) drawPad(c, i);

        // depth-sorted actors: towers, base, enemies, chest
        int n = enemies.size();
        if (sortY.length < n + 8) { sortY = new float[(n + 8) * 2]; sortId = new int[(n + 8) * 2]; }
        float[] ys = sortY; int[] ids = sortId; int k = 0;
        for (int i = 0; i < 6; i++) if (tType[i] >= 0) { ys[k] = padY[i]; ids[k++] = 1000 + i; }
        ys[k] = baseY + baseR * 0.85f; ids[k++] = 2000;
        if (chestUp && !front) { ys[k] = chestY; ids[k++] = 3000; }
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
        for (P p : parts) if (p.t != COIN && p.t != GEMP) drawPart(c, p);
        if (!front && !paused()) { drawBanner(c); drawCallout(c); }      // battle text hides under modals
        if (!front) drawCourier(c);
        if (!front && !paused()) drawHints(c);
        c.restore();

        if (!front) {
            drawHud(c);
            drawPanel(c);
            drawSpells(c);
            drawCombo(c);
        }
        for (P p : parts) if (p.t == COIN || p.t == GEMP) drawPart(c, p);
        for (Pop p : pops) {
            float s = p.age < 0.15f ? easeOutBack(p.age / 0.15f) : 1;
            int alpha = (int) (255 * Math.min(1, (1 - p.age / p.max) * 2.5f));
            art.label(c, p.s, p.x + shakeX, p.y + shakeY, p.size * s, p.col, Paint.Align.CENTER, alpha);
        }
        if (flashA > 0) { fill.setShader(null); fill.setColor(flashCol); fill.setAlpha((int) (flashA * 170)); c.drawRect(0, 0, W, H, fill); }
        for (Screen s : screens) {
            if (s.dims()) { fill.setShader(null); fill.setColor(I0); fill.setAlpha((int) (185 * s.dimA())); c.drawRect(0, 0, W, H, fill); }
            ui.layer();
            s.draw(c);
        }
        drawToast(c);
    }

    void drawSides(Canvas c) {
        fill.setShader(new LinearGradient(0, 0, 0, H, I1, I0, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, screenW, H, fill);
        fill.setShader(null);
        stroke.setColor(A1); stroke.setAlpha(255); stroke.setStrokeWidth(0.8f * u);
        c.drawLine(stageX - 0.4f * u, 0, stageX - 0.4f * u, H, stroke);
        c.drawLine(stageX + W + 0.4f * u, 0, stageX + W + 0.4f * u, H, stroke);
    }

    void drawWarmLight(Canvas c) {
        float rr = 52 * u * (1 + 0.04f * (float) Math.sin(time * 2));
        glow(c, warmPaint, baseX, baseY, rr, rr, 255);
    }

    /** Uniform connectedness: a Drummer's beat visibly reaches the two towers it speeds up. */
    void drawDrumLinks(Canvas c) {
        for (int j = 0; j < 6; j++) {
            if (tType[j] != DRUMMER) continue;
            for (int nb = 0; nb < 2; nb++) {
                int i = nb == 0 ? ringNext(j) : ringPrev(j);
                if (tType[i] < 0 || tType[i] == DRUMMER) continue;
                float pulse = 0.5f + 0.5f * (float) Math.sin(time * 6.28f * 2);
                stroke.setPathEffect(new DashPathEffect(new float[]{1.6f * u, 1.6f * u}, -time * 10 * u));
                stroke.setColor(A6); stroke.setAlpha((int) (70 + 80 * pulse)); stroke.setStrokeWidth(0.6f * u);
                c.drawLine(padX[j], padY[j] - 4 * u, padX[i], padY[i] - 4 * u, stroke);
                stroke.setPathEffect(null);
            }
        }
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
            softShadow(c, padX[i] + w * 0.07f, top + h * 0.87f, w * 0.62f, w * 0.14f, 0.6f);
            softShadow(c, padX[i] + w * 0.03f, top + h * 0.9f, w * 0.44f, w * 0.06f, 0.75f);
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
        if (frontNow) return;                                        // the title shows the keep, not its controls
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
            stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(0.7f * u);
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
        if (tLv[i] > 0) {
            float bx = padX[i] + wid * 0.36f, by = baseLine - 1.2f * u;
            if (art.uiBadge != null) {
                art.icon(c, art.uiBadge, bx, by, 5.2f * u);
                art.label(c, String.valueOf(tLv[i] + 1), bx, by + 1.0f * u, 2.9f * u, A7);
            } else {
                fill.setColor(A0); c.drawCircle(bx, by, 2.4f * u, fill);
                fill.setColor(A6); c.drawCircle(bx, by - 0.2f * u, 2.0f * u, fill);
                art.ink(c, String.valueOf(tLv[i] + 1), bx, by + 0.75f * u, 2.6f * u, A0, Paint.Align.CENTER);
            }
            int stars = tLv[i] / 10;
            for (int s = 0; s < Math.min(stars, 3); s++) art.star(c, bx - (Math.min(stars, 3) - 1) * 1.2f * u + s * 2.4f * u, by - 3.6f * u, 1.25f * u, A6);
        }
    }

    void drawBase(Canvas c) {
        float r = baseR;
        float s = clamp(baseSq * 0.06f, -0.3f, 0.3f), br = (float) Math.sin(time * 2.2f) * 0.02f;
        float sy = 1 - s + br, sx = 1 + s * 0.8f - br;
        float gy = baseY + r * 0.85f;
        softShadow(c, baseX + r * 0.08f, gy, r * 1.25f * sx, r * 0.3f, 0.6f);
        if (shieldT > 0) {                                                   // revive shield bubble
            float a = Math.min(1, shieldT / 0.5f);
            fill.setShader(null); fill.setColor(A7); fill.setAlpha((int) (40 * a)); c.drawCircle(baseX, baseY - r * 0.9f, r * 1.9f, fill);
            stroke.setColor(A6); stroke.setAlpha((int) (180 * a)); stroke.setStrokeWidth(0.5f * u); c.drawCircle(baseX, baseY - r * 0.9f, r * 1.9f, stroke);
        }
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
            tmp.set(-1.05f, -1.85f, 1.05f, -0.12f);
            fill.setShader(new RadialGradient(-0.35f, -1.45f, 1.6f, new int[]{0xFFFFD36B, 0xFFFF9F1C, 0xFFE07010, 0xFFB5520A}, new float[]{0, 0.4f, 0.8f, 1}, Shader.TileMode.CLAMP));
            c.drawOval(tmp, fill); fill.setShader(null);
            stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(0.11f); c.drawOval(tmp, stroke);
            c.restore();
        }
        if (frontNow) return;
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
            sy *= easeOutBack(Math.max(0, k)) * 0.9f + 0.05f; sx *= 0.7f + 0.3f * k;
        }
        float hop = flyer(e) || freezeT > 0 ? 0 : (float) Math.abs(Math.sin(e.ph));
        float lf = lift(e);
        softShadow(c, e.x + r * 0.08f, e.y + r * 0.08f, r * (1.1f - hop * 0.2f) * sx * (lf > 0 ? 0.7f : 1), r * 0.3f, (0.55f - hop * 0.15f) * (lf > 0 ? 0.6f : 1));
        glow(c, haloPaint, e.x, e.y - r - lf - hop * r * 0.12f, r * 1.7f, r * 1.6f, e.type == BOSS ? 200 : 150);   // lavender halo: readable on indigo ground
        float face = baseX - e.x > 0 ? 1 : -1;
        c.save();
        c.translate(e.x + (baseX - e.x) * lunge * 0.06f, e.y - hop * r * 0.12f - lf);
        if (e.type == FLAPPER) c.scale(1, 1 + 0.06f * (float) Math.sin(time * 18 + e.ph));   // wing beat
        c.scale(sx * r, sy * r);
        Bitmap spr = art.enemy(e.type);
        float hh = E_HH[e.type];
        if (spr != null) {
            float ww = hh * spr.getWidth() / spr.getHeight();
            float flip = E_FLIP[e.type] == 0 ? 1 : E_FLIP[e.type] * face;         // turn toward the king
            c.save(); c.scale(flip, 1);
            tmp.set(-ww / 2, -hh + 0.14f, ww / 2, 0.14f);
            c.drawBitmap(spr, null, tmp, bmp);
            if (e.enraged && ((int) (time * 8)) % 2 == 0) c.drawBitmap(spr, null, tmp, ragePaint);
            if (e.burnT > 0 && ((int) (time * 14 + e.ph)) % 2 == 0) c.drawBitmap(spr, null, tmp, burnPaint);
            if (e.hitT > 0) c.drawBitmap(spr, null, tmp, flashPaint);          // HIT FLASH
            c.restore();
        } else {
            fill.setShader(null); fill.setColor(I4); fill.setAlpha(255); c.drawCircle(0, -1, 1, fill);
            stroke.setColor(I0); stroke.setAlpha(255); stroke.setStrokeWidth(0.12f); c.drawCircle(0, -1, 1, stroke);
            fill.setColor(I6); c.drawCircle(-0.35f, -1.15f, 0.16f, fill); c.drawCircle(0.35f, -1.15f, 0.16f, fill);
        }
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
        c.restore();
        if (e.hp < e.max && e != boss) {
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
                stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(0.3f * u); c.drawCircle(s.x, s.y, 1.1f * u, stroke);
            }
        } else {
            float k = s.age / s.dur, gx = lerp(s.ox, s.tx, k), gy = lerp(s.oy + 9 * u, s.ty, k);
            fill.setShader(null); fill.setColor(0xFF000000); fill.setAlpha((int) (40 + 60 * k));
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
                c.save(); c.translate(p.x, p.y); c.rotate(p.rot);
                art.icon(c, art.fxPoof, 0, 0, p.sz * sc);      // fades by shrinking into the dark
                c.restore();
                break;
            }
            case BURSTFX: {
                if (art.fxBurst == null) break;
                float sc = (0.6f + easeOut(Math.min(1, k * 2.5f)) * 0.5f) * Math.min(1, a * 2);
                c.save(); c.translate(p.x, p.y); c.rotate(p.rot);
                art.icon(c, art.fxBurst, 0, 0, p.sz * sc);
                c.restore();
                break;
            }
            case ARROW: {
                if (art.fxArrow == null) break;
                art.icon(c, art.fxArrow, p.x, p.y, p.sz * (k < 0.15f ? easeOutBack(k / 0.15f) : 1) * Math.min(1, a * 2.5f));
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
                fill.setShader(null); fill.setColor(p.col == 0 ? A5 : p.col); fill.setAlpha((int) (255 * a)); c.drawCircle(p.x, p.y, p.sz * (0.4f + a * 0.6f), fill); break;
            case WISP:
                fill.setShader(null); fill.setColor(p.col); fill.setAlpha((int) (180 * a)); c.drawCircle(p.x, p.y, p.sz * (0.6f + k), fill); break;
            case CONF:
                fill.setShader(null); fill.setColor(p.col); fill.setAlpha((int) (255 * Math.min(1, a * 3)));
                c.save(); c.translate(p.x, p.y); c.rotate(p.rot); c.scale(1, (float) Math.cos(p.age * 9 + p.vr));
                tmp.set(-p.sz / 2, -p.sz / 4, p.sz / 2, p.sz / 4); c.drawRect(tmp, fill);
                stroke.setColor(A0); stroke.setAlpha((int) (200 * Math.min(1, a * 3))); stroke.setStrokeWidth(0.2f * u); c.drawRect(tmp, stroke);
                c.restore(); break;
            case RING:
                stroke.setColor(p.col); stroke.setAlpha((int) (220 * a)); stroke.setStrokeWidth(1.1f * u * a);
                tmp.set(p.x - p.sz * easeOut(k), p.y - p.sz * easeOut(k) * 0.55f, p.x + p.sz * easeOut(k), p.y + p.sz * easeOut(k) * 0.55f);
                c.drawOval(tmp, stroke); break;
            case DUST:
                fill.setShader(null); fill.setColor(p.col); fill.setAlpha((int) (130 * a)); c.drawCircle(p.x, p.y, p.sz * (0.5f + k), fill); break;
            case COIN:
                stroke.setColor(A6);
                for (int i = 4; i > 0; i--) { stroke.setAlpha((int) (110 * (1 - i / 5f))); stroke.setStrokeWidth(p.sz * 1.3f * (1 - i / 5f)); c.drawLine(p.hx[i], p.hy[i], p.hx[i - 1], p.hy[i - 1], stroke); }
                art.coin(c, p.x, p.y, p.sz);
                break;
            case GEMP:
                stroke.setColor(A7);
                for (int i = 4; i > 0; i--) { stroke.setAlpha((int) (110 * (1 - i / 5f))); stroke.setStrokeWidth(p.sz * 1.1f * (1 - i / 5f)); c.drawLine(p.hx[i], p.hy[i], p.hx[i - 1], p.hy[i - 1], stroke); }
                art.gem(c, p.x, p.y, p.sz * 2.2f);
                break;
            default: break;
        }
    }

    void drawChest(Canvas c) {
        float bounce = (float) Math.abs(Math.sin(chestT * 4)) * 1.5f * u, x = chestX, y = chestY - bounce;
        softShadow(c, x + 0.5f * u, chestY, 10 * u * 0.72f * (1 - bounce / (6 * u)), 1.6f * u, 0.55f);
        fill.setShader(null); fill.setColor(A6); fill.setAlpha((int) (60 + 50 * Math.sin(time * 5))); c.drawCircle(x, y - 4 * u, 9 * u, fill);
        float cw = 13 * u;
        if (art.chest != null) {
            float ch = cw * art.chest.getHeight() / art.chest.getWidth();
            float wob = (float) Math.sin(chestT * 9) * 4 * (float) Math.max(0, Math.sin(chestT * 1.3f));   // impatient wiggle
            c.save(); c.rotate(wob, x, y);
            tmp.set(x - cw / 2, y - ch, x + cw / 2, y); c.drawBitmap(art.chest, null, tmp, bmp);
            c.restore();
            art.label(c, "TAP!", x, y - ch - 1.5f * u, 3.6f * u, A7);
        } else {
            art.pouch(c, x, y - 4 * u, 10 * u);
            art.label(c, "TAP!", x, y - 10 * u, 3.6f * u, A7);
        }
    }

    /** The Royal Courier: Piggy Banker under a balloon (painted courier.webp when it exists). */
    void drawCourier(Canvas c) {
        if (!courierUp) return;
        float x = courierX, y = courierY;
        float sway = (float) Math.sin(courierLife * 1.7f) * 6;
        glow(c, glowPaint, x, y - 5 * u, 14 * u, 14 * u, 200);
        if (art.courier != null) { c.save(); c.rotate(sway, x, y - 12 * u); art.icon(c, art.courier, x, y - 5 * u, 20 * u); c.restore(); }
        else {
            c.save(); c.rotate(sway, x, y - 14 * u);
            stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(0.35f * u); c.drawLine(x, y - 9 * u, x, y - 3 * u, stroke);
            float bx = x, by = y - 14 * u, br = 5.2f * u;
            fill.setColor(0xFFFFFFFF); fill.setShader(new RadialGradient(bx - br * 0.35f, by - br * 0.4f, br * 1.4f, A6, A4, Shader.TileMode.CLAMP));
            tmp.set(bx - br, by - br * 1.12f, bx + br, by + br); c.drawOval(tmp, fill); fill.setShader(null);
            stroke.setStrokeWidth(0.5f * u); c.drawOval(tmp, stroke);
            fill.setColor(A7); fill.setAlpha(200); tmp.set(bx - br * 0.6f, by - br * 0.75f, bx - br * 0.15f, by - br * 0.15f); c.drawOval(tmp, fill);
            fill.setColor(A4); fill.setAlpha(255); path.reset(); path.moveTo(bx - 0.8f * u, by + br + 0.8f * u); path.lineTo(bx + 0.8f * u, by + br + 0.8f * u); path.lineTo(bx, by + br - 0.2f * u); path.close(); c.drawPath(path, fill);
            c.restore();
            Bitmap pig = art.tower(BANKER);
            if (pig != null) { float ph = 9 * u, pw = ph * pig.getWidth() / pig.getHeight(); tmp.set(x - pw / 2, y - 3.5f * u, x + pw / 2, y - 3.5f * u + ph); c.drawBitmap(pig, null, tmp, bmp); }
        }
        if (((int) (time * 3)) % 2 == 0) art.label(c, "TAP!", x, y + 8 * u, 3.4f * u, A7);
    }

    // ---------------------------------------------------------------- hints (in-context onboarding)
    static final int[] OPTIONAL_HINTS = {H_STRIKE, H_BOOST, H_UPGRADE, H_VAULT, H_QUESTS, H_REBIRTH};

    int affordableUpgrade() {
        for (int i = 0; i < 6; i++) if (tType[i] >= 0 && coins >= upCost(i)) return i;
        return -1;
    }

    int activeHint() {
        if (!entered || paused()) return -1;
        if (!meta.seen(H_TAP) && phase == 1 && !enemies.isEmpty() && enemies.get(0).emerge <= 0) return H_TAP;
        if (!meta.seen(H_BUILD) && wavesCleared >= 1 && phase == 0 && coins >= T_COST[0]) return H_BUILD;
        if (!meta.seen(H_CHEST) && chestUp) return H_CHEST;
        if (!meta.seen(H_COURIER) && courierUp) return H_COURIER;
        if (!meta.seen(H_SPELL) && spellUnlocked(SLAM) && spellCd[SLAM] <= 0 && phase == 1 && enemies.size() >= 3 && selected < 0) return H_SPELL;
        if (selected >= 0) return -1;
        if (!meta.seen(H_STRIKE) && meta.seen(H_BUILD) && wavesCleared >= 2 && coins >= tapCost()) return H_STRIKE;
        if (!meta.seen(H_UPGRADE) && wavesCleared >= 3 && affordableUpgrade() >= 0 && phase == 0) return H_UPGRADE;
        if (!meta.seen(H_BOOST) && bestWave >= UNLOCK_BOOST && phase == 0 && wavesCleared >= 3) return H_BOOST;
        if (!meta.seen(H_QUESTS) && navUnlocked(NAV_QUESTS) && phase == 0) return H_QUESTS;
        if (!meta.seen(H_VAULT) && navUnlocked(NAV_VAULT) && meta.gems > 0 && phase == 0) return H_VAULT;
        if (!meta.seen(H_REBIRTH) && canRebirth() && phase == 0) return H_REBIRTH;
        return -1;
    }

    void drawHints(Canvas c) {
        int h = activeHint();
        if (h != hintNow) { hintNow = h; hintStart = time; }
        if (h < 0) return;
        for (int o : OPTIONAL_HINTS) if (o == h && time - hintStart > 9) { meta.see(h); return; }   // never nag
        float x, y; String txt;
        switch (h) {
            case H_TAP: {
                Enemy e = enemies.get(0);
                float k = (time * 1.2f) % 1f;
                stroke.setColor(A7); stroke.setAlpha((int) (200 * (1 - k))); stroke.setStrokeWidth(0.6f * u);
                c.drawCircle(e.x, cy(e), e.r * (1.3f + k), stroke);
                x = e.x; y = cy(e) - e.r * 1.6f; txt = "Tap to squish!";
                break;
            }
            case H_BUILD:
                if (selected == 4 && nShown > 0) { pointAt(c, cardR[0].centerX(), cardR[0].top, "Build a Pebble Popper!"); return; }
                x = padX[4]; y = padY[4] - 8 * u; txt = "Build a tower!";
                break;
            case H_CHEST: x = chestX; y = chestY - 14 * u; txt = "Open the chest!"; break;
            case H_COURIER: x = courierX; y = courierY + 12 * u; pointUp(c, x, y, "The courier brings gifts!"); return;
            case H_SPELL: x = spellR[SLAM].centerX(); y = spellR[SLAM].top; txt = "Cast Jelly Slam!"; break;
            case H_STRIKE: x = btnA.centerX(); y = btnA.top; txt = "Upgrade your tap!"; break;
            case H_UPGRADE: { int i = affordableUpgrade(); x = padX[i]; y = padY[i] - 22 * u; txt = "Tap a tower to upgrade it"; break; }
            case H_BOOST: x = boostBtn.centerX(); y = boostBtn.bottom + 9 * u; pointUp(c, x, y, "Watch an ad: ×2 coins"); return;
            case H_QUESTS: x = navR[NAV_QUESTS].centerX(); y = navR[NAV_QUESTS].top; txt = "Daily quests earn gems"; break;
            case H_VAULT: x = navR[NAV_VAULT].centerX(); y = navR[NAV_VAULT].top; txt = "Gems buy permanent upgrades"; break;
            default: x = navR[NAV_REBIRTH].centerX(); y = navR[NAV_REBIRTH].top; txt = "Rebirth for Royal Jelly!"; break;
        }
        pointAt(c, x, y, txt);
    }

    /** A bobbing arrow pointing down at (x, y) with a label above it. */
    void pointAt(Canvas c, float x, float y, String txt) {
        float bob = (float) Math.abs(Math.sin(time * 5)) * 2 * u;
        if (art.fxArrow != null) { c.save(); c.rotate(180, x, y - 4 * u - bob); art.icon(c, art.fxArrow, x, y - 4 * u - bob, 6 * u); c.restore(); }
        float tx = clamp(x, safeL + art.width(txt, 3.8f * u) / 2 + 2 * u, W - safeR - art.width(txt, 3.8f * u) / 2 - 2 * u);
        art.label(c, txt, tx, y - 9 * u - bob, 3.8f * u, A6);
    }

    /** Same, pointing up from below (for things near the top of the screen). */
    void pointUp(Canvas c, float x, float y, String txt) {
        float bob = (float) Math.abs(Math.sin(time * 5)) * 2 * u;
        if (art.fxArrow != null) art.icon(c, art.fxArrow, x, y - 3 * u + bob, 6 * u);
        float tx = clamp(x, safeL + art.width(txt, 3.8f * u) / 2 + 2 * u, W - safeR - art.width(txt, 3.8f * u) / 2 - 2 * u);
        art.label(c, txt, tx, y + 6 * u + bob, 3.8f * u, A6);
    }

    // ---------------------------------------------------------------- banners, callouts, toasts
    float bannerY() { return fieldTop + (boss != null && !boss.dead ? 17 : 11) * u; }

    void drawBanner(Canvas c) {
        String a, b;
        float t, life;
        if (waveBannerT > 0) { a = bannerA; b = bannerB; t = bannerLife - waveBannerT; life = waveBannerT; }
        else if (phase == 0 && entered && !waitingForFirstTower()) {
            a = "WAVE " + wave;
            int m = eliteMod(wave);
            b = wave % 10 == 0 ? "a boss approaches..." : m >= 0 ? "elite wave: " + MOD_NAME[m].replace("!", "").toLowerCase(Locale.US) : nextUnlockLine();
            t = 2.2f - phaseT; life = phaseT;
        } else return;
        float s = t < 0.25f ? easeOutBack(Math.max(0, t) / 0.25f) : 1;
        float y = bannerY();
        int alpha = (int) (255 * Math.min(1, life / 0.35f));
        float size = ui.fit(a, 70 * u, 8.5f * u) * s;
        if (art.uiRibbon != null) {
            float rw = Math.max(art.width(a, size) + 24 * u, 56 * u) * s, rh = rw * art.uiRibbon.getHeight() / art.uiRibbon.getWidth();
            bmp.setAlpha(alpha);
            tmp.set(W / 2 - rw / 2, y - rh * 0.72f, W / 2 + rw / 2, y + rh * 0.28f); c.drawBitmap(art.uiRibbon, null, tmp, bmp);
            bmp.setAlpha(255);
        }
        art.label(c, a, W / 2, y, size, A6, Paint.Align.CENTER, alpha);
        if (b != null && !b.isEmpty()) art.label(c, b, W / 2, y + 5.4f * u, ui.fit(b, 80 * u, 3.6f * u) * s, A7, Paint.Align.CENTER, alpha);
    }

    String nextUnlockLine() {
        for (int t : DISPLAY) if (bestWave < T_UNLOCK[t]) return T_SHORT[t] + " unlocks at wave " + T_UNLOCK[t];
        for (int k = 0; k < 3; k++) if (bestWave < S_UNLOCK[k]) return S_NAME[k] + " unlocks at wave " + S_UNLOCK[k];
        return "get ready";
    }

    void drawCallout(Canvas c) {
        if (calloutT <= 0) return;
        float t = 1.1f - calloutT, s = t < 0.18f ? easeOutBack(t / 0.18f) : 1;
        int alpha = (int) (255 * Math.min(1, calloutT / 0.3f));
        art.label(c, callout, W / 2, fieldTop + 32 * u - t * 4 * u, ui.fit(callout, 80 * u, 9 * u) * s, calloutCol, Paint.Align.CENTER, alpha);
    }

    void drawToast(Canvas c) {
        if (toastT <= 0 || toastMsg.isEmpty()) return;
        int alpha = (int) (255 * Math.min(1, toastT / 0.3f));
        float size = 3.4f * u, w = Math.min(W - 8 * u, art.width(toastMsg, size) + 8 * u);
        size = ui.fit(toastMsg, w - 6 * u, size);
        float y = H * 0.30f;
        tmp.set(W / 2 - w / 2, y - 5 * u, W / 2 + w / 2, y + 2.4f * u);
        fill.setShader(null); fill.setColor(A0); fill.setAlpha((int) (alpha * 0.85f)); c.drawRoundRect(tmp, 3.5f * u, 3.5f * u, fill);
        stroke.setColor(A3); stroke.setAlpha(alpha); stroke.setStrokeWidth(0.4f * u); c.drawRoundRect(tmp, 3.5f * u, 3.5f * u, stroke);
        art.label(c, toastMsg, W / 2, y, size, A7, Paint.Align.CENTER, alpha);
    }

    // ---------------------------------------------------------------- HUD
    void drawHud(Canvas c) {
        float bump = 1 + clamp(coinBump * 0.07f, -0.15f, 0.25f);
        c.drawBitmap(bCoinPill, coinPill.left - 0.8f * u, coinPill.top - 0.8f * u, null);
        art.label(c, fmt(coins), coinPill.left + 6 * u, coinPill.bottom - 1.7f * u, 5 * u * bump, A7, Paint.Align.LEFT);
        art.coin(c, coinPill.left + 0.5f * u, coinPill.centerY(), 4.3f * u * bump);
        if (navUnlocked(NAV_VAULT) || meta.gems > 0) {
            float gb = 1 + clamp(gemBump * 0.07f, -0.15f, 0.25f);
            c.drawBitmap(bGemPill, gemPill.left - 0.8f * u, gemPill.top - 0.8f * u, null);
            art.label(c, fmt(meta.gems), gemPill.left + 5 * u, gemPill.bottom - 1.5f * u, 4.2f * u * gb, A7, Paint.Align.LEFT);
            art.gem(c, gemPill.left + 0.6f * u, gemPill.centerY(), 7 * u * gb);
        }
        // wave sign + progress (or the boss)
        float pw = bPlank.getWidth(), px = W / 2 - pw / 2, py = coinPill.top - 2.2f * u;
        c.drawBitmap(bPlank, px, py, null);
        art.label(c, "WAVE " + wave, W / 2, py + 8.4f * u, 5.4f * u, A7);
        if (streak >= 2) {
            float fx = px + pw - 0.5f * u, fy = py + 5.6f * u, fl = 1 + 0.06f * (float) Math.sin(time * 9);
            art.flame(c, fx, fy, 6.5f * u * fl);
            art.label(c, String.valueOf(streak), fx, fy + 2.2f * u, 3.2f * u, A7);
        }
        c.drawBitmap(bWaveBar, waveBar.left - 0.8f * u, waveBar.top + 1.4f * u - 0.8f * u, null);
        tmp.set(waveBar.left, waveBar.top + 1.4f * u, waveBar.right, waveBar.bottom + 1.4f * u);
        float prog = phase == 1 && waveTotal > 0 ? 1 - (toSpawn + enemies.size()) / (float) waveTotal : phase == 0 ? 0 : 1;
        art.barFill(c, tmp, clamp(prog, 0, 1), I5, I4);
        if (boss != null && !boss.dead && boss.emerge <= 0) drawBossBar(c);
        // settings
        c.drawBitmap(bRound, gearBtn.left - 1.2f * u, gearBtn.top - 1.2f * u + (ui.pressed(B_GEAR) ? 0.6f * u : 0), null);
        art.gear(c, gearBtn.centerX(), gearBtn.centerY() - 0.3f * u + (ui.pressed(B_GEAR) ? 0.6f * u : 0), 6 * u);
        ui.add(B_GEAR, gearBtn);
        if (bestWave >= UNLOCK_BOOST) drawBoost(c);
    }

    void drawBossBar(Canvas c) {
        float bw = bBossBar.getWidth(), bx = W / 2 - bw / 2, by = fieldTop + 4 * u;
        art.label(c, bossName(wave), W / 2, by - 0.6f * u, 3.6f * u, boss.enraged ? A5 : I6);
        c.drawBitmap(bBossBar, bx, by, null);
        tmp2.set(bx + 0.8f * u, by + 0.8f * u, bx + bw - 0.8f * u, by + bBossBar.getHeight() - 0.8f * u);
        art.barFill(c, tmp2, (float) Math.max(0, boss.hp / boss.max), I6, I4);
    }

    void drawBoost(Canvas c) {
        boolean on = boosted();
        ui.button(c, B_BOOST, boostBtn, GOLD);
        float off = ui.off(B_BOOST, boostBtn);
        String lbl;
        if (on) { long s = (boostUntil - meta.now()) / 1000; lbl = String.format(Locale.US, "×%s %d:%02d", trim1(meta.boostMult()), s / 60, s % 60); }
        else lbl = "×" + trim1(meta.boostMult()) + " COINS";
        float ix = boostBtn.left + 3.8f * u, iy = boostBtn.centerY() - 0.6f * u + off;
        if (!on) {
            if (meta.royalPass) art.crown(c, ix, iy, 4.4f * u); else drawAdIcon(c, ix, iy, 1.5f * u);
        }
        art.label(c, lbl, boostBtn.left + (on ? 2.6f : 7) * u, boostBtn.centerY() + 1.2f * u + off, ui.fit(lbl, boostBtn.width() - (on ? 4 : 8) * u, 3.4f * u), A7, Paint.Align.LEFT);
        if (!on && !meta.royalPass && ads.rewardedState() == Ads.LOADING) shimmer(c, boostBtn);
    }

    static String trim1(double v) { return v == Math.floor(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.1f", v); }

    /** Skeleton shimmer over a control whose content is still loading (an ad, a price). */
    void shimmer(Canvas c, RectF r) {
        float k = (time * 0.9f) % 1f, x = r.left + (r.width() + 20 * u) * k - 10 * u;
        fill.setShader(new LinearGradient(x - 8 * u, 0, x + 8 * u, 0, new int[]{0x00FFFFFF, 0x66FFF3DC, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.save(); c.clipRect(r); c.drawRect(r, fill); c.restore();
        fill.setShader(null);
    }

    void drawAdIcon(Canvas c, float x, float y, float s) {
        if (art.uiAd != null) { art.icon(c, art.uiAd, x + s * 0.2f, y + s * 0.15f, s * 2.6f); return; }
        if (art.icPlay != null) { art.icon(c, art.icPlay, x + s * 0.2f, y, s * 2.3f); return; }
        path.reset(); path.moveTo(x - s * 0.6f, y - s); path.lineTo(x + s, y); path.lineTo(x - s * 0.6f, y + s); path.close();
        fill.setShader(null); fill.setColor(A7); fill.setAlpha(255); c.drawPath(path, fill);
        stroke.setColor(A0); stroke.setAlpha(255); stroke.setStrokeWidth(0.4f * u); c.drawPath(path, stroke);
    }

    void drawCost(Canvas c, double cost, float cx, float y, float size, boolean can) {
        String s = fmt(cost);
        float w = art.width(s, size);
        art.coin(c, cx - w / 2 - size * 0.45f, y - size * 0.35f, size * 0.42f);
        art.label(c, s, cx + size * 0.35f, y, size, can ? A7 : 0xFFC9A27E);
    }

    // ---------------------------------------------------------------- bottom panel
    void drawPanel(Canvas c) {
        RectF P = curPanel;
        Bitmap pb = P == panelR ? bPanel : ui.panelBmp(P.width(), P.height());
        c.drawBitmap(pb, P.left - 1.6f * u, P.top - 1.6f * u, null);
        float in = 4 * u;
        if (selected < 0) {
            drawUpgradeBtn(c, B_STRIKE, btnA, "STRIKE", "lv " + (tapLv + 1) + " · " + fmt(tapBase()) + " dmg", tapCost(), 0);
            drawUpgradeBtn(c, B_FORT, btnB, "FORTIFY", "lv " + (fortLv + 1) + " · " + fmt(baseMax) + " hp", fortCost(), 1);
            for (int k = 0; k < 4; k++) drawNav(c, k);
        } else if (tType[selected] < 0) {
            art.ink(c, "BUILD A TOWER", P.left + in + u, P.top + 7.3f * u, 3.6f * u, A1, Paint.Align.LEFT);
            boolean big = gridCols == 3;
            for (int k = 0; k < nShown; k++) {
                int t = shown[k];
                RectF r = cardR[k];
                boolean locked = bestWave < T_UNLOCK[t];
                ui.button(c, B_CARD + k, r, WOOD, locked ? dimPaint : null);
                float off = ui.off(B_CARD + k, r);
                if (!locked && hintNow == H_BUILD && k == 0) {                // the hinted card glows
                    stroke.setColor(A6); stroke.setAlpha((int) (150 + 100 * Math.sin(time * 6))); stroke.setStrokeWidth(0.8f * u);
                    c.drawRoundRect(r, 2 * u, 2 * u, stroke);
                }
                Bitmap b = art.tower(t);
                float hgt = r.height() * 0.5f, wid = hgt * b.getWidth() / b.getHeight();
                float maxW = r.width() - 2.4f * u;
                if (wid > maxW) { hgt *= maxW / wid; wid = maxW; }
                float artTop = r.top + 1.2f * u + (r.height() * 0.5f - hgt) * 0.6f;
                tmp.set(r.centerX() - wid / 2, artTop + off, r.centerX() + wid / 2, artTop + hgt + off);
                c.drawBitmap(b, null, tmp, locked ? dimPaint : bmp);
                float nameY = r.top + r.height() * 0.5f + (big ? 4.6f : 3.6f) * u + off;
                art.label(c, big ? T_NAME[t] : T_SHORT[t], r.centerX(), nameY, ui.fit(big ? T_NAME[t] : T_SHORT[t], r.width() - 1.5f * u, (big ? 2.9f : 2.6f) * u), locked ? 0xFFC9A27E : A7);
                if (locked) {
                    if (art.uiLock != null) art.icon(c, art.uiLock, r.centerX(), artTop + hgt * 0.55f, 6 * u);
                    art.label(c, "wave " + T_UNLOCK[t], r.centerX(), r.bottom - (big ? 4.6f : 3.6f) * u, (big ? 3.4f : 3f) * u, 0xFFC9A27E);
                } else drawCost(c, T_COST[t], r.centerX(), r.bottom - (big ? 4.6f : 3.6f) * u + off, (big ? 3.8f : 3.2f) * u, coins >= T_COST[t]);
            }
            drawCloseBtn(c);
        } else {
            int i = selected, t = tType[i];
            Bitmap b = art.tower(t);
            float hgt = 20 * u, wid = hgt * b.getWidth() / b.getHeight();
            if (wid > 19 * u) { hgt *= 19 * u / wid; wid = 19 * u; }
            tmp.set(panelR.left + in + 1 * u, panelR.top + 4 * u + (20 * u - hgt) / 2, panelR.left + in + 1 * u + wid, panelR.top + 4 * u + (20 * u + hgt) / 2);
            c.drawBitmap(b, null, tmp, bmp);
            float tx = panelR.left + in + 22 * u;
            art.label(c, T_NAME[t], tx, panelR.top + 8.6f * u, ui.fit(T_NAME[t], panelR.right - tx - 12 * u, 4.4f * u), A7, Paint.Align.LEFT);
            art.ink(c, "level " + (tLv[i] + 1) + " · " + T_ROLE[t], tx, panelR.top + 12.8f * u, ui.fit("level " + (tLv[i] + 1) + " · " + T_ROLE[t], panelR.right - tx - 4 * u, 2.8f * u), A1, Paint.Align.LEFT);
            art.ink(c, statLine(i), tx, panelR.top + 17 * u, ui.fit(statLine(i), panelR.right - tx - 4 * u, 3.1f * u), A2, Paint.Align.LEFT);
            if (t != BANKER && t != DRUMMER) {                                    // goal-gradient: the next ×2 is always in view
                int toStar = 10 - tLv[i] % 10;
                float sx = panelR.left + in + 2.4f * u, sy = panelR.bottom - in - 2 * u;
                art.star(c, sx, sy - 0.9f * u, 1.6f * u, A6);
                art.ink(c, toStar + " lv → ×2", sx + 2.4f * u, sy, 2.7f * u, A2, Paint.Align.LEFT);
            }
            ui.button(c, B_UP, upBtn, GOLD);
            float off = ui.off(B_UP, upBtn);
            art.label(c, "UPGRADE", upBtn.centerX(), upBtn.top + 6.6f * u + off, 4.6f * u, A7);
            drawCost(c, upCost(i), upBtn.centerX(), upBtn.top + 12.6f * u + off, 3.9f * u, coins >= upCost(i));
            ui.button(c, B_SELL, sellBtn, WOOD);
            float so = ui.off(B_SELL, sellBtn);
            art.label(c, "SELL +" + fmt(sellValue(i)), sellBtn.centerX(), sellBtn.centerY() + 1.1f * u + so, ui.fit("SELL +" + fmt(sellValue(i)), sellBtn.width() - 2 * u, 3.2f * u), A7);
            drawCloseBtn(c);
        }
    }

    String statLine(int i) {
        int t = tType[i], lv = tLv[i];
        double now = towerDmg(i), next = towerDmgAt(t, lv + 1) * meta.dmgMult();
        switch (t) {
            case BANKER: return "+" + fmt(bankerPay(lv)) + " every 3s  →  +" + fmt(bankerPay(lv + 1));
            case DRUMMER: return "neighbours +" + Math.round((drumBuff(lv) - 1) * 100) + "% speed  →  +" + Math.round((drumBuff(lv + 1) - 1) * 100) + "%";
            case LANTERN: return "burns " + fmt(now) + "/s  →  " + fmt(next) + "/s";
            default: return "damage " + fmt(now) + "  →  " + fmt(next);
        }
    }

    void drawUpgradeBtn(Canvas c, int id, RectF r, String title, String sub, double cost, int icon) {
        boolean can = coins >= cost;
        ui.button(c, id, r, GOLD, can ? null : dimPaint);                    // muted, not hidden
        float off = ui.off(id, r);
        float ix = r.left + 6.2f * u, iy = r.top + 7 * u + off;
        if (icon == 0) { if (art.icStrike != null) art.icon(c, art.icStrike, ix, iy, 7.6f * u); else art.star(c, ix, iy, 3.2f * u, A7); }
        else { if (art.icShield != null) art.icon(c, art.icShield, ix, iy, 7.6f * u); else art.star(c, ix, iy, 3.2f * u, A6); }
        float tx = r.left + 12 * u;
        art.label(c, title, tx, r.top + 5.8f * u + off, 4.2f * u, A7, Paint.Align.LEFT);
        art.ink(c, sub, tx, r.top + 9.4f * u + off, ui.fit(sub, r.right - tx - 2 * u, 2.6f * u), A1, Paint.Align.LEFT);
        art.coin(c, tx + 1.6f * u, r.top + 12.6f * u + off, 1.8f * u);
        art.label(c, fmt(cost), tx + 4.2f * u, r.top + 14 * u + off, 4 * u, can ? A7 : 0xFFC9A27E, Paint.Align.LEFT);
    }

    void drawNav(Canvas c, int k) {
        RectF r = navR[k];
        int id = B_NAV + k;
        boolean open = navUnlocked(k);
        ui.button(c, id, r, PLANK, open ? null : dimPaint);
        float off = ui.off(id, r);
        float cx = r.centerX(), iy = r.top + r.height() * 0.4f + off, is = r.height() * 0.62f;
        if (!open) c.saveLayerAlpha(r.left, r.top, r.right, r.bottom, 110);      // recognition over recall: show what's coming
        if (k == NAV_QUESTS) art.scroll(c, cx, iy, is);
        else if (k == NAV_VAULT) { if (art.chest != null) art.icon(c, art.chest, cx, iy, is); else art.pouch(c, cx, iy, is); }
        else if (k == NAV_SHOP) art.bag(c, cx, iy, is);
        else art.crown(c, cx, iy, is);
        if (!open) {
            c.restore();
            if (art.uiLock != null) art.icon(c, art.uiLock, cx + is * 0.42f, iy + is * 0.2f, is * 0.55f);
            art.label(c, "wave " + NAV_UNLOCK[k], cx, r.bottom - 1.6f * u + off, 2.6f * u, 0xFFC9A27E);
            return;
        }
        art.label(c, NAV_NAME[k], cx, r.bottom - 1.6f * u + off, ui.fit(NAV_NAME[k], r.width() - 1.5f * u, 2.8f * u), A7);
        String badge = navBadge(k);
        if (badge != null) {
            float bx = r.right - 1.2f * u, by = r.top + 1.2f * u, bs = 5.4f * u * (1 + 0.06f * (float) Math.sin(time * 6));
            if (art.uiBadge != null) art.icon(c, art.uiBadge, bx, by, bs);
            else { fill.setShader(null); fill.setColor(A6); fill.setAlpha(255); c.drawCircle(bx, by, bs / 2, fill); }
            art.label(c, badge, bx, by + 1 * u, ui.fit(badge, bs * 0.8f, 2.8f * u), A7);
        }
    }

    /** Zeigarnik: badges show what's waiting. */
    String navBadge(int k) {
        if (k == NAV_QUESTS) { int n = meta.questsReady() + (giftAvailable() ? 1 : 0); return n > 0 ? String.valueOf(n) : null; }
        if (k == NAV_VAULT) { for (int i = 0; i < Meta.V_N; i++) if (!meta.vaultMaxed(i) && meta.gems >= meta.vaultCost(i)) return "!"; return null; }
        if (k == NAV_SHOP) return meta.adGemsLeft() > 0 && bestWave >= 5 ? "FREE" : null;
        return canRebirth() ? "!" : null;
    }

    void drawSpells(Canvas c) {
        if (selected >= 0) return;
        for (int k = 0; k < 3; k++) {
            if (!spellUnlocked(k)) continue;
            RectF r = spellR[k];
            int id = B_SPELL + k;
            float sink = ui.pressed(id) ? 0.7f * u : 0;
            boolean ready = spellCd[k] <= 0;
            if (ready) {
                float pulse = (float) (0.5 + 0.5 * Math.sin(time * 5 + k));
                glow(c, glowPaint, r.centerX(), r.centerY(), r.width() * (0.75f + 0.1f * pulse + 0.3f * spellFlash[k]), r.width() * (0.75f + 0.1f * pulse + 0.3f * spellFlash[k]), 255);
            }
            c.drawBitmap(bSpell, r.left - 1.2f * u, r.top - 1.2f * u + sink, ready ? null : dimPaint);
            float cx = r.centerX(), cyy = r.centerY() - 0.4f * u + sink, is = r.width() * 0.6f;
            if (k == SLAM) { if (art.icStrike != null) art.icon(c, art.icStrike, cx, cyy, is); else art.star(c, cx, cyy, is * 0.4f, A6); }
            else if (k == FREEZE) art.bubble(c, cx, cyy, is);
            else art.coin(c, cx, cyy, is * 0.42f);
            if (!ready) {
                float frac = spellCd[k] / spellCdMax(k);
                fill.setShader(null); fill.setColor(0xAA0B0918);
                tmp.set(r.left + 0.9f * u, r.top + 0.9f * u + sink, r.right - 0.9f * u, r.bottom - 0.9f * u + sink);
                c.drawArc(tmp, -90, 360 * frac, true, fill);
                art.label(c, String.valueOf((int) Math.ceil(spellCd[k])), cx, cyy + 1.6f * u, 4.2f * u, A7);
            }
            ui.add(id, r);
        }
    }

    void drawCombo(Canvas c) {
        if (combo < 3) return;
        float fade = Math.min(1, (1.2f - comboT) / 0.3f);
        if (fade <= 0) return;
        int alpha = (int) (255 * fade);
        float s = 1 + comboPop * 0.25f, x = panelR.left + 4 * u, y = panelR.top - 9 * u;
        art.label(c, "×" + String.format(Locale.US, "%.2f", comboMult()).replaceAll("0+$", "").replaceAll("\\.$", ""), x, y, 6.5f * u * s, A6, Paint.Align.LEFT, alpha);
        art.label(c, combo + " combo", x, y + 4.6f * u, 3.2f * u, A7, Paint.Align.LEFT, alpha);
    }

    void drawCloseBtn(Canvas c) {
        float sink = ui.pressed(B_CLOSE) ? 0.6f * u : 0;
        c.drawBitmap(roundBmp(closeBtn.width()), closeBtn.left - 1.2f * u, closeBtn.top - 1.2f * u + sink, null);
        if (art.icClose != null) art.icon(c, art.icClose, closeBtn.centerX(), closeBtn.centerY() - 0.2f * u + sink, closeBtn.width() * 0.55f);
        else art.label(c, "×", closeBtn.centerX(), closeBtn.centerY() + 2 * u + sink, 6 * u, A7);
        ui.add(B_CLOSE, closeBtn);
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
