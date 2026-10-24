package com.pantheon3d.jellykeep;

import android.content.SharedPreferences;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Random;

/**
 * Everything that outlives a run: gems, the Royal Vault (gem upgrades), the Royal Patron ladder (every ad
 * watched is a star; stars unlock permanent perks), Royal Jelly from rebirths, the daily gift streak, daily
 * quests, purchases and settings. Rules and persistence only; no drawing.
 */
final class Meta {
    // ================================================================ Royal Vault (permanent gem upgrades)
    static final int V_TAPS = 0, V_SPEED = 1, V_JELLY = 2, V_SPELLS = 3,
            V_LUCKY = 4, V_POUCH = 5, V_NAP = 6, V_NIGHT = 7,
            V_POWER = 8, V_TIME = 9, V_PURSE = 10, V_FINDER = 11, V_N = 12;
    static final String[] V_NAME = {"Sharp Taps", "Swift Towers", "Thick Jelly", "Spell Mastery",
            "Lucky Drops", "Fat Pouches", "Long Nap", "Night Shift",
            "Boost Power", "Boost Time", "Courier's Purse", "Gem Finder"};
    static final String[] V_WHAT = {"tap damage", "tower attack speed", "king health", "spell cooldowns",
            "chance a kill drops a coin pouch", "coin pouch size", "offline earning time", "offline earnings",
            "×coins from the ad boost", "boost time per ad", "courier coin gift", "chance the courier brings gems"};
    static final int[] V_MAX = {30, 25, 30, 20, 15, 20, 20, 15, 20, 15, 20, 14};
    static final double[] V_BASE = {15, 20, 15, 25, 15, 20, 20, 20, 25, 20, 15, 30};
    static final double[] V_GROW = {1.22, 1.25, 1.20, 1.25, 1.25, 1.20, 1.22, 1.25, 1.25, 1.25, 1.22, 1.30};
    static final String[] V_GROUP = {"BATTLE", "INCOME", "ADS & BONUSES"};         // four items each, in order

    // ================================================================ Royal Patron (ads watched → perks)
    static final int[] P_AT = {1, 3, 5, 8, 12, 16, 20, 25, 30, 40, 50};
    static final String[] P_DESC = {"+10% coins forever", "+30 gems", "+10% tower damage forever", "+30 min boost bank",
            "+60 gems", "+15% offline earnings", "+15% coins forever", "+100 gems", "+15% tower damage forever",
            "+150 gems", "Golden Crown: +25% coins & damage"};
    static final int[] P_GEMS = {0, 30, 0, 0, 60, 0, 0, 100, 0, 150, 0};
    static final int P_REPEAT = 10, P_REPEAT_GEMS = 50;                                // after the last tier

    // ================================================================ daily gift (7-day streak)
    static final int[] GIFT_GEMS = {20, 0, 30, 0, 50, 0, 100};
    static final int[] GIFT_COIN_MIN = {0, 15, 0, 30, 0, 60, 0};
    static final int[] GIFT_BOOST_MIN = {0, 0, 0, 0, 0, 10, 0};

    // ================================================================ daily quests
    static final int Q_TAPKILL = 0, Q_KILL = 1, Q_WAVE = 2, Q_BOSS = 3, Q_COMBO = 4, Q_SPELL = 5, Q_UPGRADE = 6,
            Q_CHEST = 7, Q_COURIER = 8, Q_STREAK = 9, Q_N = 10;
    static final String[] Q_TEXT = {"Squish %d gloom by tapping", "Defeat %d enemies", "Clear %d waves", "Defeat a boss",
            "Reach a ×%d tap combo", "Cast %d spells", "Upgrade towers %d times", "Open %d chests",
            "Take a courier gift", "Reach a %d-wave streak"};
    static final int[][] Q_GOAL = {{40, 80, 150}, {120, 250, 450}, {6, 12, 20}, {1, 1, 1}, {15, 25, 40}, {3, 5, 8},
            {8, 15, 25}, {1, 2, 3}, {1, 1, 1}, {5, 8, 12}};
    static final int[] Q_GEMS = {10, 10, 12, 15, 12, 12, 10, 12, 10, 12};
    static final int[] Q_UNLOCK = {1, 1, 1, 10, 1, 6, 1, 5, 5, 1};                    // best wave needed
    static final boolean[] Q_REACH = {false, false, false, false, true, false, false, false, false, true};
    static final int Q_BONUS = 25;

    // ================================================================ other tuning
    static final int AD_GEMS = 15, AD_GEMS_PER_DAY = 5;
    static final int JAR_CAP = 600, JAR_MIN = 150, JAR_PER_WAVE = 2;
    static final int REBIRTH_AT = 30;
    static final long INTER_GAP_MS = 5 * 60_000L;            // interstitials: never closer than this to any ad
    static final double INTER_AFTER_PLAY_S = 15 * 60;        // ...and never in a player's first 15 minutes
    static final int V16_GIFT = 50;

    // ================================================================ state
    long gems, jelly, kills, tapKills, hints, clockOffset, starterSeenMs, lastAdMs;
    final int[] vault = new int[V_N];
    int patron, rebirths, bossBest, bestCombo, jar, sessions;
    int giftNext; long giftLast = -1;
    long questDay = -1; final int[] qType = {-1, -1, -1}, qGoal = new int[3], qProg = new int[3];
    final boolean[] qDone = new boolean[3]; boolean qBonus;
    long adGemDay = -1; int adGemCount;
    boolean royalPass, starter, spoon;
    boolean music = true, haptics = true, shake = true, notify = true, notifyAsked;
    double playSec;
    boolean v16Gift;                                         // upgraded from 1.6: a thank-you is waiting

    private final SharedPreferences prefs;

    Meta(SharedPreferences prefs) { this.prefs = prefs; load(); }

    // ================================================================ time
    long now() { return System.currentTimeMillis() + clockOffset; }

    long today() { return Instant.ofEpochMilli(now()).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay(); }

    // ================================================================ multipliers
    double jellyMult() { return 1 + 0.05 * jelly; }
    double patronCoin() { return 1 + (patron >= 1 ? 0.10 : 0) + (patron >= 20 ? 0.15 : 0) + (patron >= 50 ? 0.25 : 0); }
    double patronDmg() { return 1 + (patron >= 5 ? 0.10 : 0) + (patron >= 30 ? 0.15 : 0) + (patron >= 50 ? 0.25 : 0); }
    double coinMult() { return jellyMult() * patronCoin() * (spoon ? 2 : 1) * (starter ? 1.5 : 1); }
    double dmgMult() { return jellyMult() * patronDmg(); }
    double tapMult() { return 1 + 0.15 * vault[V_TAPS]; }
    double towerSpeed() { return 1 + 0.03 * vault[V_SPEED]; }
    double kingHpMult() { return 1 + 0.10 * vault[V_JELLY]; }
    double spellCdMult() { return 1 - 0.03 * vault[V_SPELLS]; }
    double luckyChance() { return 0.02 * vault[V_LUCKY]; }
    double pouchMult() { return 5 + vault[V_POUCH]; }
    double offlineCapSec() { return (4 + vault[V_NAP]) * 3600.0; }
    double offlineFactor() { return (0.5 + 0.1 * vault[V_NIGHT]) * (patron >= 16 ? 1.15 : 1); }
    double boostMult() { return 2 + 0.1 * vault[V_POWER]; }
    long boostPerAdMs() { return (5 + vault[V_TIME]) * 60_000L; }
    long boostBankMs() { return boostPerAdMs() * 6 + (patron >= 8 ? 30 * 60_000L : 0); }
    double courierMinutes() { return 4 + 0.5 * vault[V_PURSE]; }
    double gemFinderChance() { return 0.10 + 0.03 * vault[V_FINDER]; }

    // ================================================================ vault
    long vaultCost(int i) { return (long) Math.ceil(V_BASE[i] * Math.pow(V_GROW[i], vault[i])); }

    boolean vaultMaxed(int i) { return vault[i] >= V_MAX[i]; }

    boolean buyVault(int i) {
        if (vaultMaxed(i) || gems < vaultCost(i)) return false;
        gems -= vaultCost(i);
        vault[i]++;
        save();
        return true;
    }

    /** The effect of item i at level lv, as players read it ("+45%", "×2.4", "8 h"). */
    static String vaultValue(int i, int lv) {
        switch (i) {
            case V_TAPS: return "+" + 15 * lv + "%";
            case V_SPEED: return "+" + 3 * lv + "%";
            case V_JELLY: return "+" + 10 * lv + "%";
            case V_SPELLS: return "-" + 3 * lv + "%";
            case V_LUCKY: return 2 * lv + "%";
            case V_POUCH: return "×" + (5 + lv);
            case V_NAP: return (4 + lv) + " h";
            case V_NIGHT: return (50 + 10 * lv) + "%";
            case V_POWER: return String.format(Locale.US, "×%.1f", 2 + 0.1 * lv);
            case V_TIME: return (5 + lv) + " min";
            case V_PURSE: return Game.trim1(4 + 0.5 * lv) + " min";
            default: return (10 + 3 * lv) + "%";
        }
    }

    // ================================================================ patron
    /** Counts one ad watched (or skipped with the Royal Pass). Returns the perk just unlocked, or null. */
    String addPatron() {
        patron++;
        lastAdMs = now();
        String perk = null;
        for (int k = 0; k < P_AT.length; k++) if (P_AT[k] == patron) { gems += P_GEMS[k]; perk = P_DESC[k]; }
        int last = P_AT[P_AT.length - 1];
        if (patron > last && (patron - last) % P_REPEAT == 0) { gems += P_REPEAT_GEMS; perk = "+" + P_REPEAT_GEMS + " gems"; }
        save();
        return perk;
    }

    int patronNextAt() {
        for (int at : P_AT) if (at > patron) return at;
        int last = P_AT[P_AT.length - 1];
        return last + ((patron - last) / P_REPEAT + 1) * P_REPEAT;
    }

    String patronNextDesc() {
        for (int k = 0; k < P_AT.length; k++) if (P_AT[k] > patron) return P_DESC[k];
        return "+" + P_REPEAT_GEMS + " gems";
    }

    /** Stars at the previous milestone, for drawing progress toward the next one. */
    int patronPrevAt() {
        int prev = 0;
        for (int at : P_AT) if (at <= patron) prev = at;
        int last = P_AT[P_AT.length - 1];
        if (patron > last) prev = last + (patron - last) / P_REPEAT * P_REPEAT;
        return prev;
    }

    // ================================================================ rebirth
    /** Royal Jelly for a run that reached runBest: 5 at wave 30, roughly doubling every 9 waves after. */
    static long jellyFor(int runBest) { return runBest < REBIRTH_AT ? 0 : (long) Math.floor(5 * Math.pow(1.08, runBest - REBIRTH_AT)); }

    void rebirth(int runBest) { jelly += jellyFor(runBest); rebirths++; save(); }

    // ================================================================ daily gift
    boolean giftReady() { return today() > giftLast; }

    /** Which day of the 7 the next claim is; a missed day starts the streak over. */
    int giftIndex() { return giftLast >= 0 && today() - giftLast > 1 ? 0 : giftNext; }

    boolean giftStreakBroken() { return giftLast >= 0 && today() - giftLast > 1 && giftNext != 0; }

    /** Marks today's gift claimed and returns its day index; the caller pays it out (coins need the run). */
    int claimGift() {
        int i = giftIndex();
        giftNext = (i + 1) % 7;
        giftLast = today();
        gems += GIFT_GEMS[i];
        save();
        return i;
    }

    // ================================================================ quests
    void ensureQuests(int bestWave) {
        long t = today();
        if (questDay == t) return;
        questDay = t;
        Random r = new Random(t * 7919L + 17);
        int[] pool = new int[Q_N]; int n = 0;
        for (int q = 0; q < Q_N; q++) if (bestWave >= Q_UNLOCK[q]) pool[n++] = q;
        for (int i = n - 1; i > 0; i--) { int j = r.nextInt(i + 1), s = pool[i]; pool[i] = pool[j]; pool[j] = s; }
        int tier = bestWave < 15 ? 0 : bestWave < 35 ? 1 : 2;
        for (int k = 0; k < 3; k++) {
            int q = pool[k];
            qType[k] = q; qGoal[k] = Q_GOAL[q][tier]; qProg[k] = 0; qDone[k] = false;
        }
        qBonus = false;
        save();
    }

    /** Adds progress to today's quests. "Reach" quests (combo, streak) take the best value instead. */
    void quest(int type, int amount) {
        for (int k = 0; k < 3; k++) {
            if (qType[k] != type || qDone[k]) continue;
            qProg[k] = Q_REACH[type] ? Math.max(qProg[k], Math.min(amount, qGoal[k])) : Math.min(qGoal[k], qProg[k] + amount);
        }
    }

    boolean questComplete(int k) { return qType[k] >= 0 && qProg[k] >= qGoal[k]; }

    String questText(int k) { return String.format(Locale.US, Q_TEXT[qType[k]], qGoal[k]); }

    boolean claimQuest(int k) {
        if (!questComplete(k) || qDone[k]) return false;
        qDone[k] = true;
        gems += Q_GEMS[qType[k]];
        save();
        return true;
    }

    boolean bonusReady() { return !qBonus && qDone[0] && qDone[1] && qDone[2]; }

    boolean claimBonus() {
        if (!bonusReady()) return false;
        qBonus = true;
        gems += Q_BONUS;
        save();
        return true;
    }

    /** Things waiting to be claimed (badge count on the Quests button). */
    int questsReady() {
        int n = bonusReady() ? 1 : 0;
        for (int k = 0; k < 3; k++) if (questComplete(k) && !qDone[k]) n++;
        return n;
    }

    // ================================================================ ads, jar, purchases
    int adGemsLeft() { return adGemDay == today() ? AD_GEMS_PER_DAY - adGemCount : AD_GEMS_PER_DAY; }

    void claimAdGems() {
        long t = today();
        if (adGemDay != t) { adGemDay = t; adGemCount = 0; }
        adGemCount++;
        gems += AD_GEMS;
        save();
    }

    boolean interstitialDue() { return !royalPass && playSec >= INTER_AFTER_PLAY_S && now() - lastAdMs >= INTER_GAP_MS; }

    void jarAdd() { jar = Math.min(JAR_CAP, jar + JAR_PER_WAVE); }

    /** Applies a completed purchase. Safe to call again for restored non-consumables. */
    void grant(String sku) {
        switch (sku) {
            case Store.ROYAL_PASS: if (!royalPass) { royalPass = true; gems += 100; } break;
            case Store.STARTER: if (!starter) { starter = true; gems += 300; } break;
            case Store.SPOON: spoon = true; break;
            case Store.GEMS_S: gems += 80; break;
            case Store.GEMS_M: gems += 500; break;
            case Store.GEMS_L: gems += 1200; break;
            case Store.JAR: gems += jar; jar = 0; break;
            default: return;
        }
        save();
    }

    boolean seen(int hint) { return (hints & (1L << hint)) != 0; }

    void see(int hint) { if (!seen(hint)) { hints |= 1L << hint; save(); } }

    // ================================================================ persistence
    private void load() {
        SharedPreferences p = prefs;
        boolean fresh = !p.contains("m.ver");
        if (fresh && p.contains("coins")) { v16Gift = true; gems += V16_GIFT; }                  // tester from 1.6
        gems += p.getLong("m.gems", 0); jelly = p.getLong("m.jelly", 0); kills = p.getLong("m.kills", 0);
        tapKills = p.getLong("m.tapKills", 0); hints = p.getLong("m.hints", 0); starterSeenMs = p.getLong("m.starterSeen", 0);
        lastAdMs = p.getLong("m.lastAd", 0);
        for (int i = 0; i < V_N; i++) vault[i] = Math.min(V_MAX[i], p.getInt("m.v" + i, 0));
        patron = p.getInt("m.patron", 0); rebirths = p.getInt("m.rebirths", 0); bossBest = p.getInt("m.bossBest", 0);
        bestCombo = p.getInt("m.bestCombo", 0); jar = p.getInt("m.jar", 0); sessions = p.getInt("m.sessions", 0);
        giftNext = p.getInt("m.giftNext", 0); giftLast = p.getLong("m.giftLast", -1);
        questDay = p.getLong("m.qDay", -1);
        for (int k = 0; k < 3; k++) {
            qType[k] = p.getInt("m.qt" + k, -1); qGoal[k] = p.getInt("m.qg" + k, 0);
            qProg[k] = p.getInt("m.qp" + k, 0); qDone[k] = p.getBoolean("m.qd" + k, false);
        }
        qBonus = p.getBoolean("m.qBonus", false);
        adGemDay = p.getLong("m.adGemDay", -1); adGemCount = p.getInt("m.adGemCount", 0);
        royalPass = p.getBoolean("m.royalPass", false); starter = p.getBoolean("m.starter", false); spoon = p.getBoolean("m.spoon", false);
        music = p.getBoolean("m.music", true); haptics = p.getBoolean("m.haptics", true); shake = p.getBoolean("m.shake", true);
        notify = p.getBoolean("m.notify", true); notifyAsked = p.getBoolean("m.notifyAsked", false);
        playSec = Double.longBitsToDouble(p.getLong("m.playSec", 0));
        if (fresh) save();                                   // stamp m.ver now, so a new player is never taken for a 1.6 one
    }

    void save() {
        SharedPreferences.Editor e = prefs.edit().putInt("m.ver", 2)
                .putLong("m.gems", gems).putLong("m.jelly", jelly).putLong("m.kills", kills).putLong("m.tapKills", tapKills)
                .putLong("m.hints", hints).putLong("m.starterSeen", starterSeenMs).putLong("m.lastAd", lastAdMs)
                .putInt("m.patron", patron).putInt("m.rebirths", rebirths).putInt("m.bossBest", bossBest)
                .putInt("m.bestCombo", bestCombo).putInt("m.jar", jar).putInt("m.sessions", sessions)
                .putInt("m.giftNext", giftNext).putLong("m.giftLast", giftLast).putLong("m.qDay", questDay)
                .putBoolean("m.qBonus", qBonus).putLong("m.adGemDay", adGemDay).putInt("m.adGemCount", adGemCount)
                .putBoolean("m.royalPass", royalPass).putBoolean("m.starter", starter).putBoolean("m.spoon", spoon)
                .putBoolean("m.music", music).putBoolean("m.haptics", haptics).putBoolean("m.shake", shake)
                .putBoolean("m.notify", notify).putBoolean("m.notifyAsked", notifyAsked)
                .putLong("m.playSec", Double.doubleToLongBits(playSec));
        for (int i = 0; i < V_N; i++) e.putInt("m.v" + i, vault[i]);
        for (int k = 0; k < 3; k++) e.putInt("m.qt" + k, qType[k]).putInt("m.qg" + k, qGoal[k]).putInt("m.qp" + k, qProg[k]).putBoolean("m.qd" + k, qDone[k]);
        e.apply();
    }

    // ================================================================ formatting
    static String fmtDur(long sec) {
        if (sec >= 3600) return String.format(Locale.US, "%dh %02dm", sec / 3600, sec % 3600 / 60);
        if (sec >= 60) return String.format(Locale.US, "%dm %02ds", sec / 60, sec % 60);
        return Math.max(0, sec) + "s";
    }
}
