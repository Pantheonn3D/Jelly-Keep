package com.pantheon3d.jellykeep;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Rules that outlive a run: vault, patron ladder, daily gift streak, quests, rebirth, purchases, persistence. */
@RunWith(RobolectricTestRunner.class)
public class MetaTest {
    static final long DAY = 24 * 3600_000L;
    SharedPreferences prefs;
    Meta m;

    @Before public void setUp() {
        prefs = RuntimeEnvironment.getApplication().getSharedPreferences("jellykeep", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        m = new Meta(prefs);
        m.clockOffset = noonToday() - System.currentTimeMillis();       // deterministic: middle of a local day
    }

    static long noonToday() {
        java.time.ZonedDateTime t = java.time.ZonedDateTime.now();
        return t.toLocalDate().atTime(12, 0).atZone(t.getZone()).toInstant().toEpochMilli();
    }

    @Test public void freshPlayerIsNotTakenForA16Tester() {
        assertFalse(m.v16Gift);
        assertEquals(0, m.gems);
        prefs.edit().putLong("coins", 5).commit();          // the game saves its run later...
        assertFalse(new Meta(prefs).v16Gift);               // ...but m.ver was stamped first
    }

    @Test public void testersFrom16GetAThankYou() {
        prefs.edit().clear().putLong("coins", Double.doubleToLongBits(1234)).putInt("wave", 17).commit();
        Meta old = new Meta(prefs);
        assertTrue(old.v16Gift);
        assertEquals(Meta.V16_GIFT, old.gems);
        assertFalse("only once", new Meta(prefs).v16Gift);
        assertEquals(Meta.V16_GIFT, new Meta(prefs).gems);
    }

    @Test public void vaultCostsGrowAndEffectsApply() {
        m.gems = 10_000;
        long c0 = m.vaultCost(Meta.V_POWER);
        assertEquals(25, c0);
        assertTrue(m.buyVault(Meta.V_POWER));
        assertEquals(10_000 - 25, m.gems);
        assertTrue(m.vaultCost(Meta.V_POWER) > c0);
        assertEquals(2.1, m.boostMult(), 1e-9);
        assertEquals("×2.1", Meta.vaultValue(Meta.V_POWER, 1));
        for (int i = 0; i < 40; i++) m.buyVault(Meta.V_TIME);
        assertEquals(Meta.V_MAX[Meta.V_TIME], m.vault[Meta.V_TIME]);
        assertFalse("maxed", m.buyVault(Meta.V_TIME));
        assertEquals(20 * 60_000L, m.boostPerAdMs());
        m.gems = 0;
        assertFalse("can't afford", m.buyVault(Meta.V_TAPS));
    }

    @Test public void everyAdIsAPatronStarAndMilestonesPayOut() {
        assertEquals(1, m.patronNextAt());
        assertEquals("+10% coins forever", m.addPatron());
        assertEquals(1.10, m.patronCoin(), 1e-9);
        assertNull(m.addPatron());                            // 2: nothing
        assertEquals("+30 gems", m.addPatron());              // 3
        assertEquals(30, m.gems);
        for (int i = 3; i < 50; i++) m.addPatron();
        assertEquals(1 + 0.10 + 0.15 + 0.25, m.patronCoin(), 1e-9);
        assertEquals(30 + 60 + 100 + 150, m.gems);
        for (int i = 50; i < 60; i++) m.addPatron();
        assertEquals("repeat tier", 30 + 60 + 100 + 150 + Meta.P_REPEAT_GEMS, m.gems);
        assertEquals(70, m.patronNextAt());
        assertEquals(60, m.patronPrevAt());
    }

    @Test public void dailyGiftStreak() {
        assertTrue(m.giftReady());
        assertEquals(0, m.giftIndex());
        assertEquals(0, m.claimGift());
        assertEquals(Meta.GIFT_GEMS[0], m.gems);
        assertFalse("once a day", m.giftReady());
        m.clockOffset += DAY;
        assertTrue(m.giftReady());
        assertEquals(1, m.claimGift());
        m.clockOffset += DAY;
        assertEquals(2, m.claimGift());
        m.clockOffset += 2 * DAY;                             // skipped a day
        assertTrue(m.giftStreakBroken());
        assertEquals("back to day 1", 0, m.giftIndex());
        assertEquals(0, m.claimGift());
        for (int d = 1; d < 7; d++) { m.clockOffset += DAY; assertEquals(d, m.claimGift()); }
        m.clockOffset += DAY;
        assertEquals("cycles after day 7", 0, m.giftIndex());
        m.clockOffset -= 3 * DAY;                             // clock moved backwards: no free gift
        assertFalse(m.giftReady());
    }

    @Test public void questsAreDailyAndPay() {
        m.ensureQuests(1);
        int[] first = m.qType.clone();
        for (int k = 0; k < 3; k++) assertTrue("only unlocked quests", Meta.Q_UNLOCK[m.qType[k]] <= 1);
        m.ensureQuests(1);
        assertArrayEquals("same day, same quests", first, m.qType);
        for (int k = 0; k < 3; k++) {
            int q = m.qType[k];
            if (Meta.Q_REACH[q]) { m.quest(q, m.qGoal[k] - 1); assertFalse(m.questComplete(k)); m.quest(q, m.qGoal[k] - 3); assertEquals(m.qGoal[k] - 1, m.qProg[k]); }
            m.quest(q, m.qGoal[k] * 5);
            assertTrue(m.questComplete(k));
            assertEquals(m.qGoal[k], m.qProg[k]);
        }
        assertEquals(3, m.questsReady());
        long g0 = m.gems;
        for (int k = 0; k < 3; k++) assertTrue(m.claimQuest(k));
        assertFalse("no double claims", m.claimQuest(0));
        assertTrue(m.bonusReady());
        assertTrue(m.claimBonus());
        assertEquals(0, m.questsReady());
        int paid = 0; for (int k = 0; k < 3; k++) paid += Meta.Q_GEMS[m.qType[k]];
        assertEquals(g0 + paid + Meta.Q_BONUS, m.gems);
        m.clockOffset += DAY;
        m.ensureQuests(40);
        assertFalse("new day, fresh quests", m.qDone[0]);
        assertEquals(Meta.Q_GOAL[m.qType[0]][2], m.qGoal[0]);
    }

    @Test public void royalJellyNeedsWave30AndGrows() {
        assertEquals(0, Meta.jellyFor(29));
        assertEquals(5, Meta.jellyFor(30));
        assertTrue(Meta.jellyFor(40) > Meta.jellyFor(35));
        m.rebirth(40);
        assertEquals(Meta.jellyFor(40), m.jelly);
        assertEquals(1, m.rebirths);
        assertEquals(1 + 0.05 * m.jelly, m.jellyMult(), 1e-9);
    }

    @Test public void freeGemAdsAreCappedDaily() {
        for (int i = 0; i < Meta.AD_GEMS_PER_DAY; i++) m.claimAdGems();
        assertEquals(0, m.adGemsLeft());
        assertEquals(Meta.AD_GEMS * Meta.AD_GEMS_PER_DAY, m.gems);
        m.clockOffset += DAY;
        assertEquals(Meta.AD_GEMS_PER_DAY, m.adGemsLeft());
    }

    @Test public void interstitialsNeverEarlyNeverForRoyalPass() {
        assertFalse("first 15 minutes", m.interstitialDue());
        m.playSec = Meta.INTER_AFTER_PLAY_S;
        assertTrue(m.interstitialDue());
        m.addPatron();                                       // just watched a rewarded ad
        assertFalse("not right after an ad", m.interstitialDue());
        m.clockOffset += Meta.INTER_GAP_MS;
        assertTrue(m.interstitialDue());
        m.grant(Store.ROYAL_PASS);
        assertFalse(m.interstitialDue());
    }

    @Test public void purchasesGrantOnceAndRestoreSafely() {
        m.grant(Store.ROYAL_PASS);
        m.grant(Store.ROYAL_PASS);                           // restore at next start
        assertEquals(100, m.gems);
        m.grant(Store.SPOON);
        assertEquals(2, m.coinMult(), 1e-9);
        m.grant(Store.GEMS_M);
        assertEquals(600, m.gems);
        for (int i = 0; i < 400; i++) m.jarAdd();
        assertEquals(Meta.JAR_CAP, m.jar);
        m.grant(Store.JAR);
        assertEquals(600 + Meta.JAR_CAP, m.gems);
        assertEquals(0, m.jar);
    }

    @Test public void everythingSurvivesARestart() {
        m.gems = 321; m.vault[Meta.V_NAP] = 4; m.patron = 9; m.jelly = 12; m.giftNext = 3; m.giftLast = 777; m.hints = 0b1011;
        m.music = false; m.royalPass = true; m.jar = 44; m.bestCombo = 31;
        m.save();
        Meta b = new Meta(prefs);
        assertEquals(321, b.gems); assertEquals(4, b.vault[Meta.V_NAP]); assertEquals(9, b.patron); assertEquals(12, b.jelly);
        assertEquals(3, b.giftNext); assertEquals(777, b.giftLast); assertEquals(0b1011, b.hints); assertFalse(b.music);
        assertTrue(b.royalPass); assertEquals(44, b.jar); assertEquals(31, b.bestCombo);
    }
}
