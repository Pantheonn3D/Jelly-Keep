package com.pantheon3d.jellykeep;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.MotionEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemClock;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;

/** Drives the real game with touches and writes every screen to build/shots/ (look at them). */
@RunWith(RobolectricTestRunner.class)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
public class PlayTest {
    static final int W = 1080, H = 2340;
    MainActivity act;
    Game g;
    final File out = new File("build/shots");
    final Bitmap canvasBmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);

    @Before public void setUp() {
        act = Robolectric.buildActivity(MainActivity.class).setup().get();
        act.getSharedPreferences("jellykeep", Context.MODE_PRIVATE).edit().clear().commit();
        g = fresh();
        out.mkdirs();
    }

    Game fresh() {
        Game n = new Game(act);
        n.measure(W, H); n.layout(0, 0, W, H);
        n.resume();
        return n;
    }

    // ------------------------------------------------------------------------------------ first session
    @Test public void firstSession() throws Exception {
        assertTrue(g.topLive() instanceof Screens.Title);
        step(40); shot("00_title");
        tapId(Screens.OK); settle();
        assertTrue("first launch tells the story in one card", g.topLive() instanceof Screens.Story);
        shot("01_story");
        tapId(Screens.OK); settle();
        assertNull(g.topLive());
        assertTrue(g.entered);

        for (int i = 0; i < 600 && !(g.phase == 1 && !g.enemies.isEmpty() && g.enemies.get(0).emerge <= 0); i++) step(1);
        assertEquals("wave 1 is tiny", 4, g.waveTotal);
        step(30); shot("02_wave1_tap_hint");
        for (int k = 0; k < 900 && g.wave == 1; k++) {
            Game.Enemy e = g.enemies.isEmpty() ? null : g.enemies.get(0);
            if (e != null && e.emerge <= 0 && k % 4 == 0) tap(e.x, g.cy(e));
            step(2);
        }
        assertEquals("tapping alone clears wave 1", 2, g.wave);
        assertTrue(g.meta.seen(Game.H_TAP));

        step(400);
        assertEquals("the countdown waits for the first tower", 0, g.phase);
        assertTrue(g.waitingForFirstTower());
        shot("03_build_hint");
        tap(g.padX[4], g.padY[4]);
        step(8); shot("04_build_menu");
        tapId(Game.B_CARD);
        assertEquals(Game.POPPER, g.tType[4]);
        assertFalse(g.waitingForFirstTower());
        for (int i = 0; i < 400 && g.phase != 1; i++) step(1);
        assertEquals(1, g.phase);
        step(120); shot("05_wave2_popper");
    }

    // ------------------------------------------------------------------------------------ menus
    @Test public void menusAndScreens() throws Exception {
        enter();
        midGame();
        step(2); shot("10_hud_midgame");

        tapId(Game.B_NAV + Game.NAV_VAULT); settle();
        assertTrue(g.topLive() instanceof Screens.Vault);
        shot("11_vault");
        long gems = g.meta.gems;
        tapId(Screens.LIST + Meta.V_TAPS);
        assertEquals(4 + 1, g.meta.vault[Meta.V_TAPS]);
        assertTrue(g.meta.gems < gems);
        drag(H * 0.7f, H * 0.25f); settle(); shot("11b_vault_scrolled");
        assertTrue(g.topLive().scroll > 0);
        tapId(Screen.CLOSE); settle();
        assertNull(g.topLive());

        tapId(Game.B_NAV + Game.NAV_QUESTS); settle(); shot("12_quests");
        int k0 = -1; for (int k = 0; k < 3; k++) if (g.meta.questComplete(k) && !g.meta.qDone[k]) k0 = k;
        assertTrue("midGame() finished one quest", k0 >= 0);
        long before = g.meta.gems;
        tapId(Screens.LIST + k0);
        assertTrue(g.meta.qDone[k0]);
        assertTrue(g.meta.gems > before);
        tapId(Screens.ALT); settle();                               // the daily gift row
        assertTrue(g.topLive() instanceof Screens.Daily);
        shot("13_daily");
        long g0 = g.meta.gems;
        tapId(Screens.ALT); settle();                               // plain claim
        assertEquals(g0 + Meta.GIFT_GEMS[0], g.meta.gems);
        assertFalse(g.giftAvailable());
        tapId(Screen.CLOSE); settle();
        assertNull(g.topLive());

        tapId(Game.B_NAV + Game.NAV_SHOP); settle(); shot("14_shop");
        drag(H * 0.75f, H * 0.2f); settle(); shot("14b_shop_scrolled");
        tapId(Screen.CLOSE); settle();

        tapId(Game.B_NAV + Game.NAV_REBIRTH); settle(); shot("15_rebirth");
        tapId(Screen.CLOSE); settle();

        tapId(Game.B_GEAR); settle(); shot("16_settings");
        boolean music = g.meta.music;
        tapId(Screens.LIST + 1);
        assertNotEquals(music, g.meta.music);
        tapId(Screen.CLOSE); settle();

        g.courierUp = true; g.courierLife = 7; g.courierT = 999;
        step(1); shot("17_courier_flying");
        tap(g.courierX, g.courierY - 5 * g.u); settle();
        assertTrue(g.topLive() instanceof Screens.Courier);
        shot("18_courier_offer");
        double c0 = g.coins;
        tapId(Screens.ALT); settle();
        assertTrue(g.coins > c0);

        g.chestUp = true; g.chestT = 0; g.chestX = g.baseX + 15 * g.u; g.chestY = g.baseY + 15 * g.u;
        step(1);
        tap(g.chestX, g.chestY - 3 * g.u); settle();
        assertTrue(g.topLive() instanceof Screens.Reward);
        shot("19_chest_reward");
        c0 = g.coins;
        int stars = g.meta.patron;
        tapId(Screens.OK); step(30);
        assertTrue("watching the ad shows the simulated ad", g.topLive() instanceof Screens.AdSim);
        shot("20_ad_sim");
        step(200); settle();
        assertTrue(g.coins > c0);
        assertEquals("every ad is a patron star", stars + 1, g.meta.patron);
        step(30); shot("21_after_chest_ad");

        g.push(Screens.Reward.offline(g, 12345, 3 * 3600 + 300)); settle(); shot("22_welcome_back");
        tapId(Screens.ALT); settle();
        if (g.topLive() instanceof Screens.Confirm) { shot("22b_reminder_ask"); tapId(Screens.ALT); settle(); }

        g.push(new Screens.Offer(g)); settle(); shot("23_starter_offer");
        tapId(Screen.CLOSE); settle();

        g.store.buy(Store.SPOON); settle();
        assertTrue(g.topLive() instanceof Screens.Confirm);
        shot("24_test_purchase");
        tapId(Screens.OK); settle();
        assertTrue(g.meta.spoon);
        step(40); shot("25_after_purchase");
    }

    // ------------------------------------------------------------------------------------ battle systems
    @Test public void battleSystems() throws Exception {
        enter();
        midGame();
        g.enemies.clear();
        // Drummer: speeds up exactly the two stumps beside it (1.6 buffed nothing at all)
        int[] lay = {Game.POPPER, Game.DRUMMER, Game.POPPER, Game.POPPER, Game.POPPER, Game.POPPER};
        for (int i = 0; i < 6; i++) { g.tType[i] = lay[i]; g.tLv[i] = 3; }
        g.updateTowers(1 / 60f);
        assertTrue(g.tBuff[0] > 1.15f);
        assertTrue(g.tBuff[2] > 1.15f);
        assertEquals(1f, g.tBuff[3], 1e-6);
        assertEquals(1f, g.tBuff[5], 1e-6);

        // tap combo: hits within 1.2 s stack up to ×2 damage, with callouts
        g.phase = 1; g.toSpawn = 0;
        Game.Enemy dummy = g.spawnAt(Game.BRUTE, g.W * 0.3f, g.fieldTop + 40 * g.u, 0);
        dummy.hp = dummy.max = 1e12; dummy.spd = 0;
        for (int i = 0; i < 25; i++) { tap(dummy.x, g.cy(dummy)); step(3); }
        assertEquals(25, g.combo);
        assertEquals("GREAT!", g.callout);
        assertTrue(g.comboMult() > 1.3f);
        step(90);
        assertEquals("combo times out", 0, g.combo);

        // Jelly Slam: big damage + knockback around the king
        g.enemies.clear();
        Game.Enemy near = g.spawnAt(Game.GLOOM, g.baseX + 20 * g.u, g.baseY + 4 * g.u, 0);
        near.hp = near.max = 1e12;
        g.castSpell(Game.SLAM);
        assertTrue(near.hp < near.max);
        assertTrue(near.kx > 0);
        assertTrue(g.spellCd[Game.SLAM] > 0);
        step(20); shot("30_slam");

        // Bubble Trap: for 4 s nothing advances on the king (blasts can still push the bubbles)
        for (int i = 0; i < 6; i++) g.tType[i] = -1;
        g.shots.clear();                                     // pebbles already in flight would still push it
        g.spellCd[Game.FREEZE] = 0;
        near.kx = near.ky = 0;
        float d0 = (float) Math.hypot(near.x - g.baseX, near.y - g.baseY);
        g.castSpell(Game.FREEZE);
        step(120);
        float d1 = (float) Math.hypot(near.x - g.baseX, near.y - g.baseY);
        assertEquals(d0, d1, 0.5f);
        shot("31_freeze");
        step(200);
        assertTrue("walks again after", Math.hypot(near.x - g.baseX, near.y - g.baseY) < d1 - 5 * g.u);
        for (int i = 0; i < 6; i++) g.tType[i] = lay[i];

        // Gold Rush: coins ×2
        double base = g.gain(10, Game.G_FLAT);
        g.castSpell(Game.RUSH);
        double a = g.gain(10, Game.G_KILL);
        g.rushT = 0;
        double b = g.gain(10, Game.G_KILL);
        assertEquals(2, a / b, 1e-6);
        assertEquals(10, base, 1e-9);

        // streak: +2% per wave, capped at +30%
        g.streak = 10;
        double s10 = g.gain(100, Game.G_KILL);
        g.streak = 0;
        double s0 = g.gain(100, Game.G_KILL);
        assertEquals(1.2, s10 / s0, 1e-6);

        // tower stars: ×2 at every 10th level
        assertEquals(1.45 * 2, Game.towerDmgAt(Game.POPPER, 10) / Game.towerDmgAt(Game.POPPER, 9), 1e-9);
        assertEquals(1.45, Game.towerDmgAt(Game.POPPER, 11) / Game.towerDmgAt(Game.POPPER, 10), 1e-9);

        // elite waves and bosses
        assertEquals(Game.M_SWARM, Game.eliteMod(15));
        assertEquals(-1, Game.eliteMod(16));
        g.enemies.clear(); g.wave = 15; g.phase = 0; g.phaseT = 0.01f;
        step(2);
        assertEquals(2 * g.waveSize(15), g.waveTotal);
        g.enemies.clear(); g.toSpawn = 0; step(2);
        g.wave = 20; g.phase = 0; g.phaseT = 0.01f; g.seenMask = -1;
        g.baseMax = g.baseHp = 1e12;                         // level-3 poppers can't hold wave 20; we're here to meet the boss
        step(2);
        for (int i = 0; i < 3000 && g.boss == null; i++) step(1);
        assertNotNull("a boss closes every 10th wave", g.boss);
        int before = g.waveTotal;
        g.boss.hp = g.boss.max = 1e15;
        step(60 * 6);
        assertTrue("the boss summons help", g.waveTotal >= before + 2);
        step(30); shot("32_boss_bar");

        // hold to upgrade: keeps buying while the finger stays down
        g.enemies.clear(); g.phase = 0; g.boss = null;
        g.coins = 1e9; int lv = g.tapLv;
        frame();
        RectF r = g.ui.rectOf(Game.B_STRIKE);
        long t = SystemClock.uptimeMillis();
        g.onTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, r.centerX(), r.centerY(), 0));
        step(90);
        g.onTouchEvent(MotionEvent.obtain(t, t + 1500, MotionEvent.ACTION_UP, r.centerX(), r.centerY(), 0));
        assertTrue("held: " + (g.tapLv - lv) + " levels", g.tapLv - lv >= 5);
    }

    // ------------------------------------------------------------------------------------ 1.6 bugs stay fixed
    @Test public void auditFixes() throws Exception {
        enter();
        midGame();
        g.enemies.clear(); g.phase = 1; g.toSpawn = 0;
        // a reward overlay pauses the battle (1.6 kept fighting and could lose the reward to a defeat)
        Game.Enemy biter = g.spawnAt(Game.BRUTE, g.baseX + 10 * g.u, g.baseY, 0);
        biter.hp = biter.max = 1e12;
        step(120);
        assertTrue("it is biting", g.baseHp < g.baseMax);
        double hp = g.baseHp;
        g.push(Screens.Reward.chest(g, 100));
        step(300);
        assertEquals(hp, g.baseHp, 1e-9);
        g.screens.clear();

        // a tap on an enemy next to a stump squishes it instead of opening the stump
        g.enemies.clear();
        Game.Enemy byStump = g.spawnAt(Game.GLOOM, g.padX[0], g.padY[0] + 2 * g.u, 0);
        byStump.hp = byStump.max = 1e12; byStump.spd = 0;
        g.selected = -1;
        tap(byStump.x, g.cy(byStump));
        assertEquals(-1, g.selected);
        assertTrue(byStump.hp < byStump.max);

        // defeat never blocks idle play: auto-retreat after 10 s
        g.enemies.clear();
        g.wave = 12; g.streak = 6; g.phase = 1;
        g.defeat();
        settle(); shot("40_defeat");
        assertTrue(g.topLive() instanceof Screens.Defeat);
        step(60 * 11); settle();
        assertNull(g.topLive());
        assertEquals(9, g.wave);
        assertEquals(0, g.streak);

        // revive (ad): same wave, full health, a shield, streak kept
        g.wave = 14; g.streak = 5; g.phase = 1; g.toSpawn = 3;
        Game.Enemy pest = g.spawnAt(Game.GLOOM, g.baseX + 12 * g.u, g.baseY, 0);
        g.baseHp = 0;
        g.defeat();
        settle();
        tapId(Screens.OK);
        step(200); settle();
        assertNull(g.topLive());
        assertEquals(14, g.wave);
        assertEquals(5, g.streak);
        assertEquals(g.baseMax, g.baseHp, 1e-9);
        assertTrue(pest.kx != 0 || pest.ky != 0);
        assertEquals("one revive per wave", false, revivable());
        g.wave++;
        assertEquals("and a cooldown between revives", false, revivable());
        g.time += Game.REVIVE_COOLDOWN;
        assertEquals(true, revivable());

        // offline earnings show up even if the app was left on the defeat screen (1.6 dropped them)
        g.defeat(); settle();
        g.coinRate = 10;
        g.pause();
        g.lastSaveMs = g.meta.now() - 2 * 3600_000L;
        g.resume(); settle();
        assertTrue(g.topLive() instanceof Screens.Reward);

        // back: closes the top screen, then goes to the title, which lets the app close
        g.screens.clear();
        g.push(new Screens.Vault(g)); settle();
        assertTrue(g.onBack()); settle();
        assertNull(g.topLive());
        assertTrue(g.onBack());
        assertTrue(g.topLive() instanceof Screens.Title);
        assertFalse(g.onBack());
    }

    boolean revivable() {
        g.defeat();
        settle();
        boolean can = ((Screens.Defeat) g.topLive()).canRevive;
        g.screens.clear();
        return can;
    }

    // ------------------------------------------------------------------------------------ ads, patron, purchases
    @Test public void adsAndPurchases() {
        enter();
        midGame();
        g.enemies.clear(); g.phase = 0;
        int stars = g.meta.patron;
        tapId(Game.B_BOOST);
        assertTrue(g.topLive() instanceof Screens.AdSim);
        step(200); settle();
        assertTrue(g.boosted());
        assertEquals(stars + 1, g.meta.patron);
        assertEquals("the next ad is still loading", Ads.LOADING, g.ads.rewardedState());
        tapId(Game.B_BOOST);
        assertNull("no ad while loading", g.topLive());
        assertTrue(g.toastT > 0);
        ShadowSystemClock.advanceBy(Duration.ofMillis(Ads.Sim.LOAD_MS + 100));
        assertEquals(Ads.READY, g.ads.rewardedState());

        ((Ads.Sim) g.ads).noFill = true;
        g.push(new Screens.Shop(g)); settle();
        long gems = g.meta.gems;
        tapId(Screens.LIST + 1);
        assertEquals("no fill: nothing granted", gems, g.meta.gems);
        ((Ads.Sim) g.ads).noFill = false;
        tapId(Screens.LIST + 1); step(200); settle();
        assertEquals(gems + Meta.AD_GEMS, g.meta.gems);
        assertEquals(Meta.AD_GEMS_PER_DAY - 1, g.meta.adGemsLeft());
        g.screens.clear();

        // Royal Pass: rewards without the ad, still a patron star
        g.granted(Store.ROYAL_PASS);
        int p = g.meta.patron;
        g.boostUntil = 0;
        tapId(Game.B_BOOST);
        assertNull(g.topLive());
        assertTrue(g.boosted());
        assertEquals(p + 1, g.meta.patron);
        assertFalse(g.meta.interstitialDue());
    }

    // ------------------------------------------------------------------------------------ helpers
    void enter() {
        tapId(Screens.OK); settle();
        if (g.topLive() instanceof Screens.Story) { tapId(Screens.OK); settle(); }
        while (g.topLive() != null) { g.topLive().close(); settle(); }
    }

    /** A believable mid-game save: wave 31, six towers, gems, some vault, a quest done, streak going. */
    void midGame() {
        g.bestWave = 32; g.runBest = 32; g.wave = 31; g.coins = 2.5e6;
        g.meta.gems = 640; g.meta.vault[Meta.V_POWER] = 2; g.meta.vault[Meta.V_TAPS] = 4; g.meta.patron = 7;
        int[] lay = {Game.SNIPER, Game.BOMB, Game.MAGE, Game.LANTERN, Game.BANKER, Game.DRUMMER};
        int[] lv = {12, 8, 9, 7, 10, 6};
        for (int i = 0; i < 6; i++) { g.tType[i] = lay[i]; g.tLv[i] = lv[i]; }
        g.tapLv = 14; g.fortLv = 9; g.baseMax = g.baseMaxFor(9); g.baseHp = g.baseMax; g.streak = 7; g.wavesCleared = 60;
        g.meta.see(Game.H_BUILD); g.meta.see(Game.H_TAP); g.meta.see(Game.H_STRIKE);
        g.meta.questDay = -1; g.meta.ensureQuests(32);
        g.meta.quest(g.meta.qType[1], 1_000_000);
        g.phase = 0; g.phaseT = 2;
    }

    void step(int n) { for (int k = 0; k < n; k++) g.update(1 / 60f); }

    void settle() { step(24); }

    void frame() { g.frame(new Canvas(canvasBmp)); }

    void shot(String name) throws Exception {
        frame();
        try (FileOutputStream o = new FileOutputStream(new File(out, name + ".png"))) { canvasBmp.compress(Bitmap.CompressFormat.PNG, 100, o); }
    }

    void tap(float x, float y) {
        frame();
        long t = SystemClock.uptimeMillis();
        g.onTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0));
        g.onTouchEvent(MotionEvent.obtain(t, t + 30, MotionEvent.ACTION_UP, x, y, 0));
    }

    void tapId(int id) {
        frame();
        RectF r = g.ui.rectOf(id);
        assertNotNull("button " + id + " is on screen", r);
        float x = r.centerX(), y = r.centerY();
        long t = SystemClock.uptimeMillis();
        g.onTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0));
        g.onTouchEvent(MotionEvent.obtain(t, t + 30, MotionEvent.ACTION_UP, x, y, 0));
    }

    void drag(float y0, float y1) {
        frame();
        long t = SystemClock.uptimeMillis();
        float x = W / 2f;
        g.onTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y0, 0));
        for (int i = 1; i <= 8; i++) g.onTouchEvent(MotionEvent.obtain(t, t + i * 16, MotionEvent.ACTION_MOVE, x, y0 + (y1 - y0) * i / 8f, 0));
        g.onTouchEvent(MotionEvent.obtain(t, t + 200, MotionEvent.ACTION_UP, x, y1, 0));
    }

    // ------------------------------------------------------------------------------------ balance
    /**
     * A free-to-play bot: taps 3x/s at the nearest enemy, casts spells on crowds and bosses, greedily spends coins,
     * opens chests and couriers (free gifts), retreats on defeat, rebirths once a run stalls past wave 30.
     * No ads, no gems spent. Prints the curve; the asserts keep the game endless but not trivial.
     */
    @Test public void balance() {
        String log = bot(60, 3, false);
        System.out.println("=== tapper, no ads ===\n" + log);
        assertTrue("progresses", g.bestWave >= 25);
        assertTrue("but not trivially: walls exist", g.bestWave <= 80);
    }

    /** Same bot with its hands off the screen: towers, spells and chests only. Idle play must still progress. */
    @Test public void balanceIdle() {
        String log = bot(60, 0, false);
        System.out.println("=== idle, no ads ===\n" + log);
        assertTrue("idle progresses", g.bestWave >= 15);
    }

    /** An engaged player: taps 2x/s, keeps the ×2 boost running, revives with ads and spends gems in the vault. */
    @Test public void balanceWithAds() {
        String log = bot(60, 2, true);
        System.out.println("=== 2 taps/s, watches ads, spends gems ===\n" + log);
        assertTrue(g.bestWave >= 25);
    }

    String bot(int minutes, float tapsPerSec, boolean ads) {
        enter();
        if (ads) g.meta.royalPass = true;                    // stands in for watching each ad (same rewards, no wait)
        float dt = 1 / 30f, tapAcc = 0, shopAcc = 0, lastBestT = 0;
        int defeats = 0, lastRunBest = 0;
        StringBuilder log = new StringBuilder();
        for (int s = 0; s < 30 * 60 * minutes; s++) {
            float t = s * dt;
            g.update(dt);
            tapAcc += dt; shopAcc += dt;
            Screen top = g.topLive();
            if (top instanceof Screens.Defeat) {
                defeats++;
                Screens.Defeat d = (Screens.Defeat) top;
                if (ads && d.canRevive) { log.append(String.format("  revive at wave %d (t=%.1fmin)%n", g.wave, t / 60)); d.tap(Screens.OK); }
                else { log.append(String.format("  defeat at wave %d (t=%.1fmin)%n", g.wave, t / 60)); d.retreat(); }
            }
            else if (top instanceof Screens.Reward) ((Screens.Reward) top).collect(ads ? ((Screens.Reward) top).mult : 1);
            else if (top != null && !(top instanceof Screens.AdSim)) top.close();
            if (g.chestUp) g.openChest();
            if (g.courierUp) { g.openCourier(); Screen c = g.topLive(); if (c != null) c.tap(ads ? Screens.OK : Screens.ALT); }
            if (tapsPerSec > 0 && tapAcc > 1 / tapsPerSec && !g.enemies.isEmpty()) {
                tapAcc = 0;
                Game.Enemy e = g.nearest(g.baseX, g.baseY, 1e9f);
                g.strike(e.x, g.cy(e));
            }
            if (g.phase == 1 && (g.enemies.size() >= 6 || g.boss != null)) for (int k = 0; k < 3; k++) if (g.spellUnlocked(k) && g.spellCd[k] <= 0) g.castSpell(k);
            if (shopAcc > 1) {
                shopAcc = 0;
                shop();
                if (ads) {
                    if (!g.boosted()) g.boostAd();
                    if (g.meta.adGemsLeft() > 0) { g.meta.claimAdGems(); g.meta.addPatron(); }
                    if (g.giftAvailable()) g.claimGift(true);
                    vault();
                }
            }
            if (g.runBest > lastRunBest) { lastRunBest = g.runBest; lastBestT = t; }
            if (g.canRebirth() && t - lastBestT > 240) {      // stalled 4 minutes: rebirth
                log.append(String.format("  REBIRTH at t=%.1fmin, run best %d, +%d jelly%n", t / 60, g.runBest, Meta.jellyFor(g.runBest)));
                g.rebirth(); lastRunBest = 0; lastBestT = t;
            }
            if (s % (30 * 60 * 5) == 0) log.append(String.format("t=%2dmin wave=%d best=%d run=%d coins=%s towers=%s tap=%d fort=%d jelly=%d gems=%d patron=%d%n",
                    s / 1800, g.wave, g.bestWave, g.runBest, Game.fmt(g.coins), towers(), g.tapLv, g.fortLv, g.meta.jelly, g.meta.gems, g.meta.patron));
        }
        StringBuilder ts = new StringBuilder(); for (int i = 0; i < 6; i++) ts.append(g.tType[i] < 0 ? "-" : Game.T_SHORT[g.tType[i]] + g.tLv[i]).append(' ');
        return log + "final wave " + g.wave + ", best " + g.bestWave + ", defeats " + defeats + ", rebirths " + g.meta.rebirths + ", towers " + ts;
    }

    /** Buys the cheapest affordable vault level, battle and income first. */
    void vault() {
        for (int k = 0; k < 4; k++) {
            int pick = -1; long best = Long.MAX_VALUE;
            for (int i = 0; i < Meta.V_N; i++) if (!g.meta.vaultMaxed(i) && g.meta.vaultCost(i) < best) { best = g.meta.vaultCost(i); pick = i; }
            if (pick < 0 || g.meta.gems < best) return;
            g.buyVault(pick);
        }
    }

    static final int[][] PREF = {{Game.SNIPER, Game.POPPER}, {Game.BOMB, Game.MORTAR}, {Game.MAGE, Game.WALL},
            {Game.LANTERN, Game.SPARKLER, Game.POPPER}, {Game.BANKER, Game.POPPER}, {Game.DRUMMER, Game.MORTAR}};

    int desired(int pad) { for (int t : PREF[pad]) if (g.bestWave >= Game.T_UNLOCK[t]) return t; return Game.POPPER; }

    void shop() {
        for (int k = 0; k < 12; k++) {
            int empty = -1;
            for (int i = 0; i < 6; i++) if (g.tType[i] < 0) { empty = i; break; }
            if (empty >= 0) {
                int want = desired(empty);
                if (g.coins >= Game.T_COST[want]) { g.coins -= Game.T_COST[want]; g.tType[empty] = want; g.tLv[empty] = 0; g.meta.see(Game.H_BUILD); continue; }
                return;
            }
            for (int i = 0; i < 6; i++) {                          // swap in better towers as they unlock
                int want = desired(i);
                if (g.tType[i] != want && g.coins + g.sellValue(i) >= Game.T_COST[want] * 1.3) {
                    g.coins += g.sellValue(i); g.coins -= Game.T_COST[want]; g.tType[i] = want; g.tLv[i] = 0;
                }
            }
            double best = Double.MAX_VALUE; int pick = -1;
            for (int i = 0; i < 6; i++) if (g.tType[i] >= 0 && g.upCost(i) < best) { best = g.upCost(i); pick = i; }
            if (g.tapCost() * 1.5 < best) { best = g.tapCost(); pick = 10; }
            if (g.fortCost() * 2 < best) { best = g.fortCost(); pick = 11; }
            if (pick < 0 || g.coins < best) return;
            g.coins -= best;
            if (pick == 10) g.tapLv++;
            else if (pick == 11) { g.fortLv++; g.baseMax = g.baseMaxFor(g.fortLv); }
            else g.tLv[pick]++;
        }
    }

    String towers() { StringBuilder b = new StringBuilder(); for (int i = 0; i < 6; i++) b.append(g.tType[i] < 0 ? "-" : String.valueOf(g.tLv[i] + 1)).append(i < 5 ? "," : ""); return b.toString(); }
}
