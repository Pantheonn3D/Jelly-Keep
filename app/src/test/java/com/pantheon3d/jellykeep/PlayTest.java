package com.pantheon3d.jellykeep;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.MotionEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(RobolectricTestRunner.class)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
public class PlayTest {
    static final int W = 1080, H = 2340;
    Game g;
    final File out = new File("build/shots");

    @Before public void setUp() {
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        a.getSharedPreferences("jellykeep", Context.MODE_PRIVATE).edit().clear().commit();
        g = new Game(a);
        g.measure(W, H); g.layout(0, 0, W, H);
        g.resume();
        g.overlay = 0;
        out.mkdirs();
    }

    @Test public void newContent() throws Exception {
        g.coins = 0;
        g.bestWave = 15; g.coins = 5000;
        tap(g.padX[4], g.padY[4]); step(3); shot("20_build_grid_all");
        g.bestWave = 5; tap(g.padX[4], g.padY[4]); tap(g.padX[4], g.padY[4]); step(3); shot("21_build_grid_some_locked");
        tap(g.padX[4], g.padY[4]);
        int[] lay = {Game.SNIPER, Game.BOMB, Game.MAGE, Game.LANTERN, Game.BANKER, Game.DRUMMER};
        for (int i = 0; i < 6; i++) { g.tType[i] = lay[i]; g.tLv[i] = 4; }
        g.selected = -1; g.tapLv = 6;
        g.wave = 14; g.phase = 0; g.phaseT = 0.01f; g.enemies.clear(); g.seenMask = 0;
        step(2);
        g.spawnAt(Game.FLAPPER, 200, 900, 0); g.spawnAt(Game.SHIELD, 900, 700, 0); g.spawnAt(Game.SPLITTER, 850, 1500, 0); g.spawnAt(Game.HEALER, 180, 1450, 0);
        step(150); shot("22_new_towers_enemies");
        step(40); shot("23_new_towers_enemies_b");
        tap(g.padX[4], g.padY[4] - 30); step(4); shot("24_banker_panel_sell");
        double c0 = g.coins; tap(g.sellBtn.centerX(), g.sellBtn.centerY());
        org.junit.Assert.assertEquals(-1, g.tType[4]);
        org.junit.Assert.assertTrue(g.coins > c0);
        step(6); shot("25_after_sell");
        g.selected = -1;
        g.spawnAt(Game.FLAPPER, 500, 800, 0).hp = 1e9;
        for (int k = 0; k < 6; k++) { Game.Enemy e = g.enemies.get(g.enemies.size() - 1); tap(e.x, g.cy(e)); step(1); }
        shot("26_tap_flyer_burst");
    }

    @Test public void screens() throws Exception {
        g.coins = 0;
        step(20); shot("01_start");
        step(100); // wave 1 starts
        tap(g.enemies.isEmpty() ? W / 2f : g.enemies.get(0).x, g.enemies.isEmpty() ? 800 : g.enemies.get(0).y - 20);
        step(2); shot("02_first_enemies_strike");
        g.coins = 1000;
        tap(g.padX[4], g.padY[4]);
        step(3); shot("03_build_panel");
        tap(g.cardR[0].centerX(), g.cardR[0].centerY());
        assertEquals(Game.POPPER, g.tType[4]);
        tap(g.padX[0], g.padY[0]); tap(g.cardR[1].centerX(), g.cardR[1].centerY());
        tap(g.padX[2], g.padY[2]); tap(g.cardR[2].centerX(), g.cardR[2].centerY());
        assertEquals(Game.MORTAR, g.tType[2]);
        assertEquals(2, g.selected);
        step(5); shot("04_tower_panel_range");
        double c0 = g.coins;
        tap(g.upBtn.centerX(), g.upBtn.centerY());
        assertEquals(1, g.tLv[2]);
        assertTrue(g.coins < c0);
        tap(W / 2f, g.fieldTop + 40); // close
        int[] lay = {1, 0, 2, 2, 0, 1};
        for (int i = 0; i < 6; i++) { g.tType[i] = lay[i]; g.tLv[i] = 3; }
        g.tapLv = 4;
        g.wave = 12; g.phase = 0; g.phaseT = 0.01f; g.enemies.clear();
        step(330); shot("05_battle");
        step(60); shot("06_battle_more");
        g.baseHurtT = 0.3f; g.baseHp = g.baseMax * 0.2; shot("king_moods_hurt");
        g.baseHurtT = 0; g.baseHappyT = 1; g.baseHp = g.baseMax; shot("king_moods_happy");
        g.baseHappyT = 0; shot("king_moods_neutral");
        // force a wave clear
        g.enemies.clear(); g.toSpawn = 0; step(1); step(14); shot("07_wave_clear");
        // boss
        g.wave = 10; g.phase = 0; g.phaseT = 0.01f; step(2);
        g.toSpawn = 1; step(80);
        for (int i = 0; i < 400 && g.enemies.stream().noneMatch(e -> e.type == Game.BOSS); i++) step(1);
        step(90); shot("08_boss");
        // defeat
        g.baseHp = 1; for (int i = 0; i < 600 && g.overlay != 2; i++) step(1);
        if (g.overlay != 2) { g.baseHp = 0; g.defeat(); }
        step(20); shot("09_defeat");
        tap(g.ovB.centerX(), g.ovB.centerY());
        assertEquals(0, g.overlay);
        g.showReward("Welcome back!", "your keep earned while you slept", 12345, Ads.OFFLINE_X2, 2);
        step(10); shot("10_welcome_back");
        double c1 = g.coins;
        tap(g.ovA.centerX(), g.ovA.centerY());
        assertEquals(c1 + 24690, g.coins, 0.5);
        g.chestUp = true; g.chestX = g.baseX + 150; g.chestY = g.baseY + 160;
        step(25); shot("11_chest_collect_shower");
    }

    /** A simple bot: taps 3x/s at the nearest enemy and greedily spends coins. Checks the curve is endless but not trivial. */
    @Test public void balance() {
        float dt = 1 / 30f, tapAcc = 0, shopAcc = 0;
        int defeats = 0;
        StringBuilder log = new StringBuilder();
        for (int s = 0; s < 30 * 60 * 60; s++) {           // 60 minutes of play
            g.update(dt);
            tapAcc += dt; shopAcc += dt;
            if (g.overlay == 2) { defeats++; log.append(String.format("  defeat at wave %d (t=%.1fmin)%n", g.wave, s / 1800f)); g.retreat(); }
            if (g.overlay == 1) g.collectReward(1);
            if (tapAcc > 0.33f && !g.enemies.isEmpty()) {
                tapAcc = 0;
                Game.Enemy e = g.nearest(g.baseX, g.baseY, 1e9f);
                g.strike(e.x, g.cy(e));
            }
            if (shopAcc > 1) { shopAcc = 0; shop(); }
            if (s % (30 * 60 * 5) == 0) log.append(String.format("t=%2dmin wave=%d best=%d coins=%s towers=%s tap=%d fort=%d enemies=%d%n", s / 1800, g.wave, g.bestWave, Game.fmt(g.coins), towers(), g.tapLv, g.fortLv, g.enemies.size()));
        }
        StringBuilder ts = new StringBuilder(); for (int i = 0; i < 6; i++) ts.append(g.tType[i] < 0 ? "-" : Game.T_SHORT[g.tType[i]] + g.tLv[i]).append(' ');
        System.out.println(log + "final wave " + g.wave + ", best " + g.bestWave + ", defeats " + defeats + ", swaps " + sold + ", towers " + ts);
        assertTrue("progresses", g.bestWave >= 15);
    }

    static final int[][] PREF = {{Game.SNIPER, Game.POPPER}, {Game.BOMB, Game.MORTAR}, {Game.MAGE, Game.WALL},
            {Game.LANTERN, Game.SPARKLER, Game.POPPER}, {Game.BANKER, Game.POPPER}, {Game.DRUMMER, Game.MORTAR}};
    int sold;

    int desired(int pad) { for (int t : PREF[pad]) if (g.bestWave >= Game.T_UNLOCK[t]) return t; return Game.POPPER; }

    void shop() {
        for (int k = 0; k < 12; k++) {
            int empty = -1;
            for (int i = 0; i < 6; i++) if (g.tType[i] < 0) { empty = i; break; }
            if (empty >= 0) {
                int want = desired(empty);
                if (g.coins >= Game.T_COST[want]) { g.coins -= Game.T_COST[want]; g.tType[empty] = want; g.tLv[empty] = 0; continue; }
                return;
            }
            for (int i = 0; i < 6; i++) {                          // swap in better towers as they unlock
                int want = desired(i);
                if (g.tType[i] != want && g.coins + g.sellValue(i) >= Game.T_COST[want] * 1.3) {
                    g.coins += g.sellValue(i); g.coins -= Game.T_COST[want]; g.tType[i] = want; g.tLv[i] = 0; sold++;
                }
            }
            double best = Double.MAX_VALUE; int pick = -1;
            for (int i = 0; i < 6; i++) if (g.tType[i] >= 0 && g.upCost(i) < best) { best = g.upCost(i); pick = i; }
            if (g.tapCost() * 1.5 < best) { best = g.tapCost(); pick = 10; }
            if (g.fortCost() * 2 < best) { best = g.fortCost(); pick = 11; }
            if (pick < 0 || g.coins < best) return;
            g.coins -= best;
            if (pick == 10) g.tapLv++; else if (pick == 11) { g.fortLv++; g.baseMax = Game.baseMaxFor(g.fortLv); } else g.tLv[pick]++;
        }
    }

    String towers() { StringBuilder b = new StringBuilder(); for (int i = 0; i < 6; i++) b.append(g.tType[i] < 0 ? "-" : String.valueOf(g.tLv[i] + 1)).append(i < 5 ? "," : ""); return b.toString(); }

    void step(int n) { for (int k = 0; k < n; k++) g.update(1 / 60f); }
    void shot(String name) throws Exception {
        Bitmap b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        g.render(new Canvas(b));
        try (FileOutputStream o = new FileOutputStream(new File(out, name + ".png"))) { b.compress(Bitmap.CompressFormat.PNG, 100, o); }
    }
    void tap(float x, float y) {
        long t = SystemClock.uptimeMillis();
        g.onTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0));
        g.onTouchEvent(MotionEvent.obtain(t, t + 30, MotionEvent.ACTION_UP, x, y, 0));
    }
}
