package com.pantheon3d.jellykeep;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;

import java.util.Locale;

import static com.pantheon3d.jellykeep.Art.*;
import static com.pantheon3d.jellykeep.Game.fmt;

/** The title screen and every modal panel. Each screen draws with the kit (Art/Ui) and handles its own taps. */
final class Screens {
    private Screens() { }

    static final int OK = 100, ALT = 101, LIST = 200;   // shared action ids; LIST + n for rows

    /** A soft rounded row on parchment (grouping by common region). */
    static void card(Game g, Canvas c, RectF r, boolean lit) {
        float u = g.u;
        g.fill.setShader(null); g.fill.setColor(lit ? 0x40FFD25A : 0x1C7A3E16); c.drawRoundRect(r, 2.2f * u, 2.2f * u, g.fill);
        g.stroke.setColor(lit ? A5 : A3); g.stroke.setAlpha(lit ? 230 : 110); g.stroke.setStrokeWidth(0.35f * u);
        c.drawRoundRect(r, 2.2f * u, 2.2f * u, g.stroke);
    }

    static void bar(Game g, Canvas c, RectF r, float f) {
        float u = g.u;
        g.fill.setShader(null); g.fill.setColor(A1); g.fill.setAlpha(200); c.drawRoundRect(r, r.height() / 2, r.height() / 2, g.fill);
        g.art.barFill(c, r, Math.max(0, Math.min(1, f)), A6, 0xFFF29A1E);
        g.stroke.setColor(A0); g.stroke.setAlpha(255); g.stroke.setStrokeWidth(0.3f * u); c.drawRoundRect(r, r.height() / 2, r.height() / 2, g.stroke);
    }

    /** Price pill content: a price, or a skeleton shimmer while the store hasn't answered yet. */
    static String priceOrNull(Game g, String sku) { return g.store.price(sku); }

    // =============================================================== title
    static final class Title extends Screen {
        private Paint shade;
        private String version = "";

        Title(Game g) {
            super(g);
            try {
                PackageInfo pi = g.getContext().getPackageManager().getPackageInfo(g.getContext().getPackageName(), 0);
                version = "v" + pi.versionName;
            } catch (Exception ignored) { }
        }

        @Override boolean dims() { return false; }
        @Override boolean dismissable() { return false; }

        @Override void draw(Canvas c) {
            float u = u(), W = g.W, H = g.H;
            if (shade == null) {
                shade = new Paint();
                shade.setShader(new LinearGradient(0, 0, 0, H, new int[]{0xE00B0918, 0x700B0918, 0x100B0918, 0x700B0918, 0xF00B0918},
                        new float[]{0, 0.22f, 0.45f, 0.68f, 1}, Shader.TileMode.CLAMP));
            }
            float a = out >= 0 ? 1 - Math.min(1, out / CLOSE_S) : 1;
            shade.setAlpha((int) (255 * a));
            c.drawRect(0, 0, W, H, shade);
            float in = Math.min(1, t / 0.5f);
            // logo
            float ly = g.safeT + 26 * u - (1 - Game.easeOut(in)) * 10 * u;
            if (art.logo != null) art.icon(c, art.logo, W / 2, ly, 78 * u);
            else {
                drawWord(c, "JELLY", W / 2, ly - 2 * u, 17 * u, 0);
                drawWord(c, "KEEP", W / 2, ly + 15 * u, 17 * u, 5);
            }
            if (art.uiRibbon != null) {
                float rw = 62 * u, rh = rw * art.uiRibbon.getHeight() / art.uiRibbon.getWidth();
                tmp.set(W / 2 - rw / 2, ly + 21 * u, W / 2 + rw / 2, ly + 21 * u + rh);
                c.drawBitmap(art.uiRibbon, null, tmp, null);
                art.label(c, "Defend the Jelly King!", W / 2, tmp.top + rh * 0.62f, 4 * u, A7);
            } else art.label(c, "Defend the Jelly King!", W / 2, ly + 27 * u, 4 * u, A7);
            // play: the one big target, in the thumb zone
            boolean returning = g.bestWave > 1 || g.anyTower();
            String lbl = returning ? "CONTINUE" : "PLAY";
            float bw = 62 * u, bh = 17 * u, by = g.H - g.safeB - 44 * u;
            float breathe = 1 + 0.03f * (float) Math.sin(t * 4);
            tmp.set(W / 2 - bw / 2, by, W / 2 + bw / 2, by + bh);
            c.save();
            c.scale(breathe, breathe, tmp.centerX(), tmp.centerY());
            ui.button(c, OK, tmp, GOLD);
            float off = ui.off(OK, tmp);
            art.label(c, lbl, W / 2, tmp.centerY() + 2.4f * u + off, 8 * u, A7);
            c.restore();
            String sub = returning ? "wave " + g.wave + "  ·  best " + g.bestWave + (g.meta.jelly > 0 ? "  ·  " + g.meta.jelly + " royal jelly" : "")
                    : "a cozy idle tower defense";
            art.label(c, sub, W / 2, by + bh + 7 * u, ui.fit(sub, W - 12 * u, 3.6f * u), A7);
            closeLessGear(c);
            art.ink(c, version, W / 2, g.H - g.safeB - 2 * u, 2.6f * u, 0x99FFEBD2, Paint.Align.CENTER);
        }

        private void drawWord(Canvas c, String s, float cx, float y, float size, int phase) {
            float total = art.width(s, size) + (s.length() - 1) * 0.6f * u(), x = cx - total / 2;
            for (int i = 0; i < s.length(); i++) {
                String ch = String.valueOf(s.charAt(i));
                float w = art.width(ch, size), bob = (float) Math.sin(t * 3 + (i + phase) * 0.7f) * 1.2f * u();
                art.label(c, ch, x + w / 2, y + bob, size, (i + phase) % 2 == 0 ? A6 : A5);
                x += w + 0.6f * u();
            }
        }

        private void closeLessGear(Canvas c) {
            float u = u();
            float sink = ui.pressed(ALT) ? 0.6f * u : 0;
            c.drawBitmap(g.bRound, g.gearBtn.left - 1.2f * u, g.gearBtn.top - 1.2f * u + sink, null);
            art.gear(c, g.gearBtn.centerX(), g.gearBtn.centerY() - 0.3f * u + sink, 6 * u);
            ui.add(ALT, g.gearBtn);
        }

        @Override void tap(int id) {
            if (id == OK) { close(); g.enterKeep(); }
            else if (id == ALT) g.push(new Settings(g));
        }

        @Override void tapOutside(float x, float y) { }
    }

    // =============================================================== first-run story card (one tap, skippable)
    static final class Story extends Screen {
        Story(Game g) { super(g); }

        @Override void draw(Canvas c) {
            float u = u();
            RectF v = begin(c, "NIGHT FALLS", 82 * u, 56 * u, g.H * 0.46f, false);
            float y = v.top + 5 * u;
            y = ui.ink(c, "The Gloom crave the Jelly King's crown.", g.W / 2, y, v.width() - 4 * u, 4 * u, A1, Paint.Align.CENTER) + 4 * u;
            ui.ink(c, "Squish them, build towers, keep him safe!", g.W / 2, y, v.width() - 4 * u, 4 * u, A1, Paint.Align.CENTER);
            tmp.set(g.W / 2 - 24 * u, box.bottom - 18 * u, g.W / 2 + 24 * u, box.bottom - 5 * u);
            action(c, OK, tmp, GOLD, "LET'S GO!", false);
            end(c);
        }

        @Override void tap(int id) { finish(); }
        @Override void tapOutside(float x, float y) { finish(); }
        @Override void back() { finish(); }

        private void finish() {
            if (out >= 0) return;
            g.meta.see(Game.H_STORY);
            close();
            g.showPending();
        }
    }

    // =============================================================== rewards (offline, chest, gifts)
    static final class Reward extends Screen {
        final String title, sub, placement;
        final double amount;
        final boolean gems, granted, chestArt, offline;
        final int mult;

        private Reward(Game g, String title, String sub, double amount, boolean gems, String placement, int mult, boolean granted, boolean chestArt, boolean offline) {
            super(g);
            this.title = title; this.sub = sub; this.amount = amount; this.gems = gems; this.placement = placement;
            this.mult = mult; this.granted = granted; this.chestArt = chestArt; this.offline = offline;
        }

        static Reward offline(Game g, double amt, double away) {
            return new Reward(g, "Welcome back!", "your keep earned " + Meta.fmtDur((long) away) + " of coins while you were away", amt, false, Ads.OFFLINE, 2, false, false, true);
        }

        static Reward chest(Game g, double amt) { return new Reward(g, "Treasure!", "a chest full of coins", amt, false, Ads.CHEST, 2, false, true, false); }

        /** Gems that are already in the account (shown, not paid again). */
        static Reward granted(Game g, String title, String sub, long gems) { return new Reward(g, title, sub, gems, true, null, 1, true, false, false); }

        @Override boolean dismissable() { return false; }

        @Override void draw(Canvas c) {
            float u = u();
            RectF v = begin(c, title, 80 * u, 62 * u, g.H * 0.5f, false);
            if (chestArt && art.chestOpen != null) {
                float cw = 22 * u, ch = cw * art.chestOpen.getHeight() / art.chestOpen.getWidth();
                tmp.set(box.left - 6 * u, box.top - ch * 0.55f, box.left - 6 * u + cw, box.top + ch * 0.45f);
                c.drawBitmap(art.chestOpen, null, tmp, null);
            }
            ui.ink(c, sub, g.W / 2, v.top + 4 * u, v.width() - 6 * u, 3.4f * u, A1, Paint.Align.CENTER);
            String amt = "+" + fmt(amount);
            float w = art.width(amt, 9 * u);
            if (gems) art.gem(c, g.W / 2 - w / 2 - 2.5f * u, box.top + 22 * u, 8 * u);
            else art.coin(c, g.W / 2 - w / 2 - 1.5f * u, box.top + 22 * u, 4 * u);
            art.label(c, amt, g.W / 2 + 3 * u, box.top + 25.5f * u, 9 * u, A7);
            if (granted || placement == null) {
                tmp.set(box.left + 10 * u, box.bottom - 18 * u, box.right - 10 * u, box.bottom - 5 * u);
                action(c, OK, tmp, GOLD, "COLLECT", false);
            } else {
                tmp.set(box.left + 6 * u, box.bottom - 31 * u, box.right - 6 * u, box.bottom - 18 * u);
                action(c, OK, tmp, GOLD, "COLLECT ×" + mult, !g.meta.royalPass);
                if (!g.meta.royalPass && g.ads.rewardedState() == Ads.LOADING) g.shimmer(c, tmp);
                tmp.set(box.left + 14 * u, box.bottom - 15 * u, box.right - 14 * u, box.bottom - 5 * u);
                action(c, ALT, tmp, WOOD, "collect " + fmt(amount), false);
            }
            end(c);
        }

        @Override void tap(int id) {
            if (id == OK && !granted && placement != null) g.watchAd(placement, () -> collect(mult));
            else if (id == OK || id == ALT) collect(1);
        }

        void collect(int m) {
            if (out >= 0) return;
            close();
            if (granted) { g.gemShower((long) amount); return; }
            double v = amount * m;
            if (gems) { g.meta.gems += (long) v; g.meta.save(); g.gemShower((long) v); }
            else { g.coins += v; g.coinShower(v); }
            g.flash(A6, 0.4f); g.trauma = Math.min(1, g.trauma + 0.4f); g.baseHappyT = 1;
            g.save();
            if (offline && Build.VERSION.SDK_INT >= 33 && !g.meta.notifyAsked) g.push(Confirm.notifyAsk(g));
        }
    }

    // =============================================================== defeat
    static final class Defeat extends Screen {
        static final float AUTO_S = 10;
        final int left, total;
        final double bossLeft;
        final boolean canRevive;
        final float reviveIn;
        float wait;

        Defeat(Game g, int left, int total, double bossLeft, boolean canRevive, float reviveIn) {
            super(g);
            this.left = left; this.total = total; this.bossLeft = bossLeft; this.canRevive = canRevive; this.reviveIn = reviveIn;
        }

        @Override boolean dismissable() { return false; }

        @Override void update(float dt) {
            super.update(dt);
            if (g.topLive() == this) {                    // idle players: no one is stuck on this screen
                wait += dt;
                if (wait >= AUTO_S) retreat();
            }
        }

        String nearMiss() {
            if (bossLeft >= 0 && bossLeft < 0.35) return "So close! The boss had " + Math.max(1, Math.round(bossLeft * 100)) + "% left";
            if (bossLeft >= 0) return "The boss was too strong this time";
            if (left > 0 && left <= Math.max(3, total / 5)) return "So close! Only " + left + " gloom left";
            return "The gloom reached your king";
        }

        @Override void draw(Canvas c) {
            float u = u();
            begin(c, "DEFEAT", 80 * u, 66 * u, g.H * 0.5f, false);
            ui.ink(c, nearMiss(), g.W / 2, box.top + 11 * u, box.width() - 10 * u, 3.6f * u, A1, Paint.Align.CENTER);
            art.ink(c, "wave " + g.wave, g.W / 2, box.top + 22 * u, 7 * u, A2, Paint.Align.CENTER);
            if (canRevive) {
                tmp.set(box.left + 6 * u, box.bottom - 33 * u, box.right - 6 * u, box.bottom - 19 * u);
                action(c, OK, tmp, GOLD, g.streak >= 2 ? "REVIVE · keep streak " + g.streak : "REVIVE", !g.meta.royalPass);
                if (!g.meta.royalPass && g.ads.rewardedState() == Ads.LOADING) g.shimmer(c, tmp);
            } else {
                String why = reviveIn > 0 ? "revive recharges in " + Meta.fmtDur((long) Math.ceil(reviveIn)) : "already revived this wave";
                ui.ink(c, why, g.W / 2, box.bottom - 25 * u, box.width(), 3 * u, A2, Paint.Align.CENTER);
            }
            tmp.set(box.left + 12 * u, box.bottom - 16 * u, box.right - 12 * u, box.bottom - 5 * u);
            action(c, ALT, tmp, WOOD, "retreat to wave " + Math.max(1, g.wave - 3), false);
            float f = Math.min(1, wait / AUTO_S);                      // the auto-retreat timer, visible
            g.fill.setShader(null); g.fill.setColor(A6); g.fill.setAlpha(200);
            tmp2.set(tmp.left + 2.5f * u, tmp.bottom - 1.6f * u, tmp.left + 2.5f * u + (tmp.width() - 5 * u) * (1 - f), tmp.bottom - 0.9f * u);
            c.drawRoundRect(tmp2, u, u, g.fill);
            end(c);
        }

        @Override void tap(int id) {
            if (id == OK && canRevive) g.watchAd(Ads.REVIVE, () -> { close(); g.revive(); });
            else if (id == ALT) retreat();
        }

        @Override void back() { retreat(); }

        void retreat() {
            if (out >= 0) return;
            close();
            g.retreat();
        }
    }

    // =============================================================== courier (Eatventure's investor)
    static final class Courier extends Screen {
        final boolean gems;
        final double coins;
        static final int GEM_GIFT = 20;

        Courier(Game g, boolean gems) {
            super(g);
            this.gems = gems;
            this.coins = g.incomePerMin() * g.meta.courierMinutes();
        }

        @Override void draw(Canvas c) {
            float u = u();
            begin(c, "ROYAL COURIER", 82 * u, 70 * u, g.H * 0.5f, true);
            Bitmap pig = art.courier != null ? art.courier : art.tower(Game.BANKER);
            if (pig != null) art.icon(c, pig, g.W / 2, box.top + 15 * u, 15 * u);
            ui.ink(c, "brings a gift from the treasury", g.W / 2, box.top + 27 * u, box.width() - 8 * u, 3.4f * u, A1, Paint.Align.CENTER);
            String amt = gems ? "+" + GEM_GIFT + " gems" : "+" + fmt(coins);
            float w = art.width(amt, 8 * u);
            if (gems) art.gem(c, g.W / 2 - w / 2 - 3 * u, box.top + 36 * u, 7.5f * u); else art.coin(c, g.W / 2 - w / 2 - 1.5f * u, box.top + 36 * u, 3.6f * u);
            art.label(c, amt, g.W / 2 + 2.5f * u, box.top + 39 * u, 8 * u, A7);
            tmp.set(box.left + 6 * u, box.bottom - 29 * u, box.right - 6 * u, box.bottom - 16 * u);
            action(c, OK, tmp, GOLD, "TAKE THE GIFT", !g.meta.royalPass);
            if (!g.meta.royalPass && g.ads.rewardedState() == Ads.LOADING) g.shimmer(c, tmp);
            tmp.set(box.left + 12 * u, box.bottom - 13 * u, box.right - 12 * u, box.bottom - 4 * u);
            action(c, ALT, tmp, WOOD, "small gift: +" + fmt(g.incomePerMin() * 0.5), false);
            end(c);
        }

        @Override void tap(int id) {
            if (id == OK) g.watchAd(Ads.COURIER, () -> {
                close();
                g.meta.quest(Meta.Q_COURIER, 1);
                if (gems) { g.meta.gems += GEM_GIFT; g.meta.save(); g.gemShower(GEM_GIFT); }
                else { g.coins += coins; g.coinShower(coins); }
                g.save();
            });
            else if (id == ALT) {
                close();
                g.meta.quest(Meta.Q_COURIER, 1);
                double v = g.incomePerMin() * 0.5;
                g.coins += v; g.coinShower(v); g.save();
            } else super.tap(id);
        }
    }

    // =============================================================== Royal Vault
    static final class Vault extends Screen {
        Vault(Game g) { super(g); }

        @Override void draw(Canvas c) {
            float u = u();
            RectF v = begin(c, "ROYAL VAULT", 92 * u, Math.min(g.H - g.safeT - g.safeB - 24 * u, 170 * u), g.H * 0.5f + 2 * u, true);
            gemBalance(c);
            v.top += 6 * u;
            c.save();
            c.clipRect(v);
            float y = v.top - scroll + 1 * u;
            y = patronCard(c, v, y) + 3 * u;
            for (int grp = 0; grp < 3; grp++) {
                art.label(c, Meta.V_GROUP[grp], v.left + 1 * u, y + 3.6f * u, 3.6f * u, A6, Paint.Align.LEFT);
                y += 6 * u;
                for (int k = 0; k < 4; k++) y = row(c, v, y, grp * 4 + k, grp) + 1.6f * u;
                y += 2 * u;
            }
            c.restore();
            scrollMax = Math.max(0, y + scroll - v.bottom);
            if (scrollMax > 0) fadeBottom(g, c, v);
            end(c);
        }

        float patronCard(Canvas c, RectF v, float y) {
            float u = u();
            Meta m = g.meta;
            tmp.set(v.left, y, v.right, y + 27 * u);
            card(g, c, tmp, true);
            art.crown(c, tmp.left + 6 * u, tmp.top + 6.5f * u, 8 * u);
            art.label(c, "ROYAL PATRON", tmp.left + 12 * u, tmp.top + 6.4f * u, 4.2f * u, A7, Paint.Align.LEFT);
            art.ink(c, "every ad you watch is a star; stars unlock perks forever", tmp.left + 12 * u, tmp.top + 10.2f * u,
                    ui.fit("every ad you watch is a star; stars unlock perks forever", tmp.width() - 14 * u, 2.6f * u), A2, Paint.Align.LEFT);
            int prev = m.patronPrevAt(), next = m.patronNextAt();
            tmp2.set(tmp.left + 3 * u, tmp.top + 13 * u, tmp.right - 3 * u, tmp.top + 16.4f * u);
            bar(g, c, tmp2, (m.patron - prev) / (float) Math.max(1, next - prev));
            art.label(c, m.patron + " / " + next + " stars", tmp2.centerX(), tmp2.bottom - 0.6f * u, 2.6f * u, A7);
            art.ink(c, "next: " + m.patronNextDesc(), tmp.left + 3 * u, tmp.top + 21.6f * u, ui.fit("next: " + m.patronNextDesc(), tmp.width() - 30 * u, 3.2f * u), A1, Paint.Align.LEFT);
            tmp2.set(tmp.right - 26 * u, tmp.top + 18 * u, tmp.right - 2.5f * u, tmp.top + 25 * u);
            int left = m.adGemsLeft();
            if (left > 0) action(c, OK, tmp2, GOLD, "+" + Meta.AD_GEMS + " GEMS", !m.royalPass, true); else art.ink(c, "free gems: back tomorrow", tmp2.centerX(), tmp2.centerY() + u, 2.6f * u, A2, Paint.Align.CENTER);
            return tmp.bottom;
        }

        float row(Canvas c, RectF v, float y, int i, int grp) {
            float u = u();
            Meta m = g.meta;
            float h = 15 * u;
            tmp.set(v.left, y, v.right, y + h);
            boolean max = m.vaultMaxed(i), afford = !max && m.gems >= m.vaultCost(i);
            card(g, c, tmp, afford);
            float ix = tmp.left + 6 * u, iy = tmp.centerY();
            if (art.uiBadge != null) art.icon(c, art.uiBadge, ix, iy, 10 * u);
            if (grp == 0) { if (art.icStrike != null) art.icon(c, art.icStrike, ix, iy, 6 * u); else art.star(c, ix, iy, 2.4f * u, A6); }
            else if (grp == 1) art.coin(c, ix, iy, 2.6f * u);
            else g.drawAdIcon(c, ix, iy, 1.5f * u);
            float tx = tmp.left + 12.5f * u;
            art.label(c, Meta.V_NAME[i], tx, tmp.top + 5.6f * u, 3.6f * u, A7, Paint.Align.LEFT);
            art.ink(c, "lv " + m.vault[i] + "/" + Meta.V_MAX[i], tx + art.width(Meta.V_NAME[i], 3.6f * u) + 2 * u, tmp.top + 5.6f * u, 2.6f * u, A2, Paint.Align.LEFT);
            String eff = Meta.V_WHAT[i] + ": " + Meta.vaultValue(i, m.vault[i]) + (max ? "" : "  →  " + Meta.vaultValue(i, m.vault[i] + 1));
            art.ink(c, eff, tx, tmp.top + 10.6f * u, ui.fit(eff, tmp.right - tx - 25 * u, 2.8f * u), A1, Paint.Align.LEFT);
            tmp2.set(tmp.right - 23 * u, tmp.top + 2.5f * u, tmp.right - 2.5f * u, tmp.bottom - 2.5f * u);
            int id = LIST + i;
            if (max) { art.label(c, "MAX", tmp2.centerX(), tmp2.centerY() + 1.4f * u, 4.2f * u, A6); return tmp.bottom; }
            ui.face(c, id, tmp2, GOLD, afford ? null : g.dimPaint);
            addClipped(id, tmp2);
            float off = ui.off(id, tmp2);
            String cost = fmt(m.vaultCost(i));
            float cw = art.width(cost, 4 * u);
            art.gem(c, tmp2.centerX() - cw / 2 - 1.8f * u, tmp2.centerY() + off, 5 * u);
            art.label(c, cost, tmp2.centerX() + 1.8f * u, tmp2.centerY() + 1.5f * u + off, 4 * u, afford ? A7 : 0xFFC9A27E);
            return tmp.bottom;
        }

        @Override void tap(int id) {
            if (id == OK) g.watchAd(Ads.GEMS, () -> { g.meta.claimAdGems(); g.gemShower(Meta.AD_GEMS); });
            else if (id >= LIST && id < LIST + Meta.V_N) g.buyVault(id - LIST);
            else super.tap(id);
        }
    }

    /** Fades the bottom of a scrolling list so it reads as "more below". */
    static void fadeBottom(Game g, Canvas c, RectF v) {
        float u = g.u;
        g.fill.setShader(new LinearGradient(0, v.bottom - 6 * u, 0, v.bottom, 0x00F2C47E, 0xEEF2C47E, Shader.TileMode.CLAMP));
        c.drawRect(v.left, v.bottom - 6 * u, v.right, v.bottom, g.fill);
        g.fill.setShader(null);
    }

    // =============================================================== quests + daily gift
    static final class Quests extends Screen {
        Quests(Game g) { super(g); }

        @Override void draw(Canvas c) {
            float u = u();
            Meta m = g.meta;
            m.ensureQuests(g.bestWave);
            RectF v = begin(c, "DAILY QUESTS", 88 * u, 116 * u, g.H * 0.5f, true);
            float y = v.top + 1 * u;
            // daily gift row
            tmp.set(v.left, y, v.right, y + 14 * u);
            boolean gift = g.giftAvailable();
            card(g, c, tmp, gift);
            art.gift(c, tmp.left + 6.5f * u, tmp.centerY(), 9 * u);
            art.label(c, gift ? "Daily gift is ready!" : "Daily gift", tmp.left + 13 * u, tmp.top + 6 * u, 3.8f * u, A7, Paint.Align.LEFT);
            art.ink(c, gift ? "day " + (m.giftIndex() + 1) + " of 7" : "next one in " + Meta.fmtDur(secondsToMidnight(m)), tmp.left + 13 * u, tmp.top + 10.6f * u, 2.8f * u, A2, Paint.Align.LEFT);
            if (gift) { tmp2.set(tmp.right - 22 * u, tmp.top + 2.5f * u, tmp.right - 2.5f * u, tmp.bottom - 2.5f * u); action(c, ALT, tmp2, GOLD, "OPEN", false); }
            y = tmp.bottom + 4 * u;
            for (int k = 0; k < 3; k++) {
                if (m.qType[k] < 0) continue;
                tmp.set(v.left, y, v.right, y + 20 * u);
                boolean done = m.questComplete(k), claimed = m.qDone[k];
                card(g, c, tmp, done && !claimed);
                art.scroll(c, tmp.left + 6.5f * u, tmp.top + 7 * u, 8 * u);
                String q = m.questText(k);
                art.label(c, q, tmp.left + 13 * u, tmp.top + 6 * u, ui.fit(q, tmp.width() - 38 * u, 3.6f * u), A7, Paint.Align.LEFT);
                tmp2.set(tmp.left + 13 * u, tmp.top + 10 * u, tmp.right - 27 * u, tmp.top + 13.4f * u);
                bar(g, c, tmp2, m.qProg[k] / (float) m.qGoal[k]);
                art.label(c, Math.min(m.qProg[k], m.qGoal[k]) + " / " + m.qGoal[k], tmp2.centerX(), tmp2.bottom - 0.6f * u, 2.6f * u, A7);
                int gems = Meta.Q_GEMS[m.qType[k]];
                art.gem(c, tmp.left + 15 * u, tmp.top + 16.8f * u, 4.4f * u);
                art.ink(c, "+" + gems, tmp.left + 18 * u, tmp.top + 18 * u, 3 * u, A1, Paint.Align.LEFT);
                tmp2.set(tmp.right - 24 * u, tmp.top + 4 * u, tmp.right - 2.5f * u, tmp.bottom - 4 * u);
                if (claimed) art.check(c, tmp2.centerX(), tmp2.centerY(), 9 * u);
                else if (done) action(c, LIST + k, tmp2, GOLD, "CLAIM", false);
                else art.ink(c, "in progress", tmp2.centerX(), tmp2.centerY() + u, 2.8f * u, A2, Paint.Align.CENTER);
                y = tmp.bottom + 3 * u;
            }
            tmp.set(v.left, y, v.right, y + 14 * u);
            boolean bonus = m.bonusReady();
            card(g, c, tmp, bonus);
            if (art.chest != null) art.icon(c, art.chest, tmp.left + 6.5f * u, tmp.centerY(), 9 * u);
            art.label(c, "Finish all three", tmp.left + 13 * u, tmp.top + 6 * u, 3.6f * u, A7, Paint.Align.LEFT);
            art.ink(c, "+" + Meta.Q_BONUS + " gems bonus", tmp.left + 13 * u, tmp.top + 10.6f * u, 2.8f * u, A2, Paint.Align.LEFT);
            tmp2.set(tmp.right - 24 * u, tmp.top + 2.5f * u, tmp.right - 2.5f * u, tmp.bottom - 2.5f * u);
            if (m.qBonus) art.check(c, tmp2.centerX(), tmp2.centerY(), 9 * u);
            else if (bonus) action(c, OK, tmp2, GOLD, "CLAIM", false);
            art.ink(c, "new quests in " + Meta.fmtDur(secondsToMidnight(m)), g.W / 2, box.bottom - 4 * u, 2.8f * u, A2, Paint.Align.CENTER);
            end(c);
        }

        static long secondsToMidnight(Meta m) {
            java.time.ZonedDateTime now = java.time.Instant.ofEpochMilli(m.now()).atZone(java.time.ZoneId.systemDefault());
            return java.time.Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.getZone())).getSeconds();
        }

        @Override void tap(int id) {
            Meta m = g.meta;
            if (id >= LIST && id < LIST + 3) { if (m.claimQuest(id - LIST)) g.gemShower(Meta.Q_GEMS[m.qType[id - LIST]]); }
            else if (id == OK) { if (m.claimBonus()) g.gemShower(Meta.Q_BONUS); }
            else if (id == ALT) g.push(new Daily(g));
            else super.tap(id);
        }
    }

    // =============================================================== 7-day gift calendar
    static final class Daily extends Screen {
        Daily(Game g) { super(g); }

        @Override void draw(Canvas c) {
            float u = u();
            Meta m = g.meta;
            RectF v = begin(c, "DAILY GIFT", 90 * u, 100 * u, g.H * 0.5f, true);
            boolean ready = g.giftAvailable();
            int today = m.giftIndex();
            String sub = m.giftStreakBroken() ? "you missed a day, so the streak starts over" : ready ? "come back every day: the gifts grow" : "today's gift is claimed. see you tomorrow!";
            ui.ink(c, sub, g.W / 2, v.top + 2 * u, v.width(), 3.2f * u, A1, Paint.Align.CENTER);
            float gap = 2 * u, cw = (v.width() - 3 * gap) / 4, ch = 21 * u, top = v.top + 7 * u;
            for (int d = 0; d < 7; d++) {
                int row = d < 4 ? 0 : 1, col = d < 4 ? d : d - 4;
                float rowW = row == 0 ? v.width() : 3 * cw + 2 * gap, l = (row == 0 ? v.left : g.W / 2 - rowW / 2) + col * (cw + gap);
                tmp.set(l, top + row * (ch + gap), l + cw, top + row * (ch + gap) + ch);
                boolean wrapped = !ready && today == 0 && m.giftLast >= 0;          // day 7 was claimed today
                boolean claimed = wrapped || d < today;
                boolean isToday = d == today && ready;
                card(g, c, tmp, isToday);
                art.label(c, "DAY " + (d + 1), tmp.centerX(), tmp.top + 4.6f * u, 3 * u, isToday ? A6 : A7);
                if (Meta.GIFT_GEMS[d] > 0) { art.gem(c, tmp.centerX(), tmp.top + 10.6f * u, 7 * u); art.ink(c, "+" + Meta.GIFT_GEMS[d], tmp.centerX(), tmp.bottom - 2.4f * u, 3 * u, A1, Paint.Align.CENTER); }
                else { art.coin(c, tmp.centerX(), tmp.top + 10.6f * u, 3 * u); art.ink(c, Meta.GIFT_COIN_MIN[d] + " min", tmp.centerX(), tmp.bottom - 2.4f * u, 3 * u, A1, Paint.Align.CENTER); }
                if (claimed) {
                    g.fill.setShader(null); g.fill.setColor(0x887A3E16); c.drawRoundRect(tmp, 2.2f * u, 2.2f * u, g.fill);
                    art.check(c, tmp.centerX(), tmp.centerY(), 10 * u);
                }
            }
            float by = top + 2 * ch + gap + 5 * u;
            if (ready) {
                tmp.set(box.left + 6 * u, by, box.right - 6 * u, by + 13 * u);
                action(c, OK, tmp, GOLD, "CLAIM ×2", !m.royalPass);
                if (!m.royalPass && g.ads.rewardedState() == Ads.LOADING) g.shimmer(c, tmp);
                tmp.set(box.left + 16 * u, by + 15 * u, box.right - 16 * u, by + 25 * u);
                action(c, ALT, tmp, WOOD, "claim", false);
            } else {
                art.ink(c, "next gift in " + Meta.fmtDur(Quests.secondsToMidnight(m)), g.W / 2, by + 8 * u, 3.4f * u, A2, Paint.Align.CENTER);
            }
            end(c);
        }

        @Override void tap(int id) {
            if (id == OK && g.giftAvailable()) g.watchAd(Ads.DAILY, () -> { if (g.giftAvailable()) { g.claimGift(true); close(); } });
            else if (id == ALT && g.giftAvailable()) { g.claimGift(false); close(); }
            else super.tap(id);
        }
    }

    // =============================================================== shop
    static final class Shop extends Screen {
        Shop(Game g) { super(g); }

        @Override void draw(Canvas c) {
            float u = u();
            Meta m = g.meta;
            RectF v = begin(c, "SHOP", 92 * u, Math.min(g.H - g.safeT - g.safeB - 24 * u, 172 * u), g.H * 0.5f + 2 * u, true);
            gemBalance(c);
            v.top += 6 * u;
            c.save();
            c.clipRect(v);
            float y = v.top - scroll + 1 * u;
            int left = m.adGemsLeft();
            y = offer(c, v, y, 1, "Free gems", left > 0 ? left + " of " + Meta.AD_GEMS_PER_DAY + " left today" : "back tomorrow",
                    null, left > 0 ? "+" + Meta.AD_GEMS : "", 2) + 2.5f * u;
            if (!m.royalPass) y = offer(c, v, y, 2, "Royal Pass", "skip every ad, keep every reward · +100 gems", Store.ROYAL_PASS, null, 3) + 2.5f * u;
            if (!m.starter) y = offer(c, v, y, 3, "Starter Pack", "300 gems and +50% coins forever", Store.STARTER, null, 4) + 2.5f * u;
            if (!m.spoon) y = offer(c, v, y, 4, "Golden Spoon", "×2 coins from everything, forever", Store.SPOON, null, 5) + 2.5f * u;
            art.label(c, "GEMS", v.left + 1 * u, y + 3.6f * u, 3.6f * u, A6, Paint.Align.LEFT);
            y += 6 * u;
            y = offer(c, v, y, 5, "Pouch of gems", "80 gems", Store.GEMS_S, null, 6) + 2 * u;
            y = offer(c, v, y, 6, "Chest of gems", "500 gems", Store.GEMS_M, null, 6) + 2 * u;
            y = offer(c, v, y, 7, "Vault of gems", "1200 gems: the best value", Store.GEMS_L, null, 6) + 2.5f * u;
            y = jar(c, v, y) + 2.5f * u;
            art.label(c, "COIN CRATES", v.left + 1 * u, y + 3.6f * u, 3.6f * u, A6, Paint.Align.LEFT);
            y += 6 * u;
            y = crate(c, v, y, 0) + 2 * u;
            y = crate(c, v, y, 1) + 3 * u;
            tmp.set(g.W / 2 - 22 * u, y, g.W / 2 + 22 * u, y + 9 * u);
            ui.face(c, LIST + 20, tmp, WOOD, null); addClipped(LIST + 20, tmp);
            art.label(c, "restore purchases", tmp.centerX(), tmp.centerY() + 1.2f * u + ui.off(LIST + 20, tmp), 3 * u, A7);
            y = tmp.bottom + 3 * u;
            if (!MonetizationFactory.LIVE) { art.ink(c, "test build: purchases are simulated", g.W / 2, y + 2 * u, 2.6f * u, A2, Paint.Align.CENTER); y += 5 * u; }
            c.restore();
            scrollMax = Math.max(0, y + scroll - v.bottom);
            if (scrollMax > 0) fadeBottom(g, c, v);
            end(c);
        }

        /**
         * One offer row: a store price button for sku, or (adLabel != null) a watch-ad button; an empty adLabel means
         * today's ads are used up. icon: 2/6 gem, 3 crown, 4 pouch, 5 coin.
         */
        float offer(Canvas c, RectF v, float y, int n, String name, String what, String sku, String adLabel, int icon) {
            float u = u();
            float h = 17 * u;
            boolean ad = adLabel != null;
            tmp.set(v.left, y, v.right, y + h);
            card(g, c, tmp, icon == 3 || icon == 5 || (ad && !adLabel.isEmpty()));
            float ix = tmp.left + 6.5f * u, iy = tmp.centerY();
            if (icon == 3) art.crown(c, ix, iy, 9 * u);
            else if (icon == 4) art.pouch(c, ix, iy, 9 * u);
            else if (icon == 5) art.coin(c, ix, iy, 4 * u);
            else art.gem(c, ix, iy, 9 * u);
            float tx = tmp.left + 13 * u, tw = tmp.width() - 13 * u - 27 * u;
            art.label(c, name, tx, tmp.top + 6 * u, ui.fit(name, tw, 3.8f * u), A7, Paint.Align.LEFT);
            ui.ink(c, what, tx, tmp.top + 10.4f * u, tw, 2.6f * u, A1, Paint.Align.LEFT);
            tmp2.set(tmp.right - 25 * u, tmp.top + 3.5f * u, tmp.right - 2.5f * u, tmp.bottom - 3.5f * u);
            int id = LIST + n;
            if (ad) {
                if (adLabel.isEmpty()) return tmp.bottom;
                ui.face(c, id, tmp2, GOLD, null); addClipped(id, tmp2);
                float off = ui.off(id, tmp2);
                if (g.meta.royalPass) art.crown(c, tmp2.left + 4 * u, tmp2.centerY() + off, 4.5f * u); else g.drawAdIcon(c, tmp2.left + 4 * u, tmp2.centerY() - 0.4f * u + off, 1.4f * u);
                art.label(c, adLabel, tmp2.centerX() + 2.5f * u, tmp2.centerY() + 1.4f * u + off, 3.8f * u, A7);
                if (!g.meta.royalPass && g.ads.rewardedState() == Ads.LOADING) g.shimmer(c, tmp2);
                return tmp.bottom;
            }
            String price = priceOrNull(g, sku);
            ui.face(c, id, tmp2, GOLD, null); addClipped(id, tmp2);
            if (price == null) g.shimmer(c, tmp2);
            else art.label(c, price, tmp2.centerX(), tmp2.centerY() + 1.4f * u + ui.off(id, tmp2), ui.fit(price, tmp2.width() - 3 * u, 4 * u), A7);
            return tmp.bottom;
        }

        float jar(Canvas c, RectF v, float y) {
            float u = u();
            Meta m = g.meta;
            tmp.set(v.left, y, v.right, y + 19 * u);
            boolean ready = m.jar >= Meta.JAR_MIN;
            card(g, c, tmp, ready);
            art.pouch(c, tmp.left + 6.5f * u, tmp.centerY(), 9 * u);
            art.label(c, "Gem Jar", tmp.left + 13 * u, tmp.top + 6 * u, 3.8f * u, A7, Paint.Align.LEFT);
            art.ink(c, "fills by " + Meta.JAR_PER_WAVE + " gems every wave you clear", tmp.left + 13 * u, tmp.top + 10 * u, ui.fit("fills by 2 gems every wave you clear", tmp.width() - 42 * u, 2.6f * u), A1, Paint.Align.LEFT);
            tmp2.set(tmp.left + 13 * u, tmp.top + 12.4f * u, tmp.right - 28 * u, tmp.top + 15.6f * u);
            bar(g, c, tmp2, m.jar / (float) Meta.JAR_CAP);
            art.label(c, m.jar + " / " + Meta.JAR_CAP, tmp2.centerX(), tmp2.bottom - 0.5f * u, 2.5f * u, A7);
            tmp2.set(tmp.right - 25 * u, tmp.top + 4 * u, tmp.right - 2.5f * u, tmp.bottom - 4 * u);
            int id = LIST + 8;
            if (!ready) { art.ink(c, "opens at " + Meta.JAR_MIN, tmp2.centerX(), tmp2.centerY() + u, 2.8f * u, A2, Paint.Align.CENTER); return tmp.bottom; }
            String price = priceOrNull(g, Store.JAR);
            ui.face(c, id, tmp2, GOLD, null); addClipped(id, tmp2);
            if (price == null) g.shimmer(c, tmp2);
            else art.label(c, price, tmp2.centerX(), tmp2.centerY() + 1.4f * u + ui.off(id, tmp2), 4 * u, A7);
            return tmp.bottom;
        }

        float crate(Canvas c, RectF v, float y, int k) {
            float u = u();
            long cost = k == 0 ? 40 : 120;
            double coins = g.incomePerMin() * (k == 0 ? 15 : 60);
            tmp.set(v.left, y, v.right, y + 15 * u);
            boolean afford = g.meta.gems >= cost;
            card(g, c, tmp, false);
            if (art.chest != null) art.icon(c, art.chest, tmp.left + 6.5f * u, tmp.centerY(), 9 * u); else art.coin(c, tmp.left + 6.5f * u, tmp.centerY(), 4 * u);
            art.label(c, "+" + fmt(coins) + " coins", tmp.left + 13 * u, tmp.top + 6.4f * u, 3.8f * u, A7, Paint.Align.LEFT);
            art.ink(c, (k == 0 ? "15 minutes" : "a whole hour") + " of income, right now", tmp.left + 13 * u, tmp.top + 10.8f * u, 2.6f * u, A1, Paint.Align.LEFT);
            tmp2.set(tmp.right - 25 * u, tmp.top + 3 * u, tmp.right - 2.5f * u, tmp.bottom - 3 * u);
            int id = LIST + 10 + k;
            ui.face(c, id, tmp2, GOLD, afford ? null : g.dimPaint); addClipped(id, tmp2);
            float off = ui.off(id, tmp2);
            String cs = String.valueOf(cost);
            float cw = art.width(cs, 4 * u);
            art.gem(c, tmp2.centerX() - cw / 2 - 1.8f * u, tmp2.centerY() + off, 5 * u);
            art.label(c, cs, tmp2.centerX() + 1.8f * u, tmp2.centerY() + 1.5f * u + off, 4 * u, afford ? A7 : 0xFFC9A27E);
            return tmp.bottom;
        }

        @Override void tap(int id) {
            switch (id) {
                case LIST + 1: g.watchAd(Ads.GEMS, () -> { g.meta.claimAdGems(); g.gemShower(Meta.AD_GEMS); }); break;
                case LIST + 2: g.store.buy(Store.ROYAL_PASS); break;
                case LIST + 3: g.store.buy(Store.STARTER); break;
                case LIST + 4: g.store.buy(Store.SPOON); break;
                case LIST + 5: g.store.buy(Store.GEMS_S); break;
                case LIST + 6: g.store.buy(Store.GEMS_M); break;
                case LIST + 7: g.store.buy(Store.GEMS_L); break;
                case LIST + 8: if (g.meta.jar >= Meta.JAR_MIN) g.store.buy(Store.JAR); break;
                case LIST + 10: g.buyCrate(0); break;
                case LIST + 11: g.buyCrate(1); break;
                case LIST + 20: g.store.restore(); break;
                default: super.tap(id);
            }
        }
    }

    // =============================================================== rebirth
    static final class Rebirth extends Screen {
        Rebirth(Game g) { super(g); }

        @Override void draw(Canvas c) {
            float u = u();
            Meta m = g.meta;
            RectF v = begin(c, "REBIRTH", 88 * u, 118 * u, g.H * 0.5f, true);
            float y = v.top + 2 * u;
            art.jellyDrop(c, g.W / 2 - 14 * u, y + 7 * u, 12 * u);
            art.label(c, String.valueOf(m.jelly), g.W / 2 - 6 * u, y + 9.5f * u, 8 * u, A7, Paint.Align.LEFT);
            y += 15 * u;
            art.ink(c, "Royal Jelly: +" + Math.round(m.jelly * 5) + "% coins & damage, forever", g.W / 2, y + 2 * u, ui.fit("Royal Jelly: +" + Math.round(m.jelly * 5) + "% coins & damage, forever", v.width(), 3.3f * u), A1, Paint.Align.CENTER);
            y += 7 * u;
            long gain = Meta.jellyFor(g.runBest);
            tmp.set(v.left, y, v.right, y + 18 * u);
            card(g, c, tmp, g.canRebirth());
            if (g.canRebirth()) {
                art.label(c, "Rebirth now: +" + gain + " Royal Jelly", tmp.centerX(), tmp.top + 7 * u, ui.fit("Rebirth now: +" + gain + " Royal Jelly", tmp.width() - 4 * u, 4.4f * u), A6);
                long more = Meta.jellyFor(g.runBest + 5) - gain;
                art.ink(c, "push 5 more waves first for +" + more + " more", tmp.centerX(), tmp.top + 13 * u, 3 * u, A2, Paint.Align.CENTER);
            } else {
                art.label(c, "Reach wave " + Meta.REBIRTH_AT + " to rebirth", tmp.centerX(), tmp.top + 7 * u, 4.4f * u, A7);
                art.ink(c, "this run's best: wave " + g.runBest, tmp.centerX(), tmp.top + 13 * u, 3 * u, A2, Paint.Align.CENTER);
            }
            y = tmp.bottom + 4 * u;
            float half = (v.width() - 3 * u) / 2;
            tmp.set(v.left, y, v.left + half, y + 32 * u);
            card(g, c, tmp, false);
            art.label(c, "STARTS OVER", tmp.centerX(), tmp.top + 5 * u, 3.4f * u, A7);
            float ly = tmp.top + 10 * u;
            for (String s : new String[]{"waves & coins", "towers", "strike & fortify", "wave streak"}) { art.ink(c, s, tmp.centerX(), ly, 2.9f * u, A1, Paint.Align.CENTER); ly += 5 * u; }
            tmp.set(v.left + half + 3 * u, y, v.right, y + 32 * u);
            card(g, c, tmp, true);
            art.label(c, "YOU KEEP", tmp.centerX(), tmp.top + 5 * u, 3.4f * u, A7);
            ly = tmp.top + 10 * u;
            for (String s : new String[]{"gems & vault", "patron perks", "unlocked towers", "spells & quests"}) { art.ink(c, s, tmp.centerX(), ly, 2.9f * u, A1, Paint.Align.CENTER); ly += 5 * u; }
            tmp.set(box.left + 8 * u, box.bottom - 18 * u, box.right - 8 * u, box.bottom - 5 * u);
            action(c, OK, tmp, g.canRebirth() ? GOLD : DIM, "REBIRTH", false);
            end(c);
        }

        @Override void tap(int id) {
            if (id == OK) {
                if (!g.canRebirth()) { g.deny(); g.toast("Reach wave " + Meta.REBIRTH_AT + " first"); return; }
                long gain = Meta.jellyFor(g.runBest);
                g.push(Confirm.plain(g, "Rebirth?", "Your run starts over at wave 1 with +" + gain + " Royal Jelly. Gems, vault and perks stay.", "REBIRTH", () -> { close(); g.rebirth(); }));
            } else super.tap(id);
        }
    }

    // =============================================================== settings
    static final class Settings extends Screen {
        Settings(Game g) { super(g); }

        @Override void draw(Canvas c) {
            float u = u();
            Meta m = g.meta;
            boolean privacy = g.ads.privacyOptionsRequired();
            String policy = policyUrl();
            int rows = 5 + (privacy ? 1 : 0);
            RectF v = begin(c, "SETTINGS", 84 * u, 30 * u + rows * 13 * u + (policy != null ? 13 * u : 0), g.H * 0.5f, true);
            float y = v.top + 1 * u;
            y = toggle(c, v, y, 0, "Sound", g.sfx.on) + 2 * u;
            y = toggle(c, v, y, 1, "Music", m.music) + 2 * u;
            y = toggle(c, v, y, 2, "Vibration", m.haptics) + 2 * u;
            y = toggle(c, v, y, 3, "Screen shake", m.shake) + 2 * u;
            y = toggle(c, v, y, 4, "Reminders", m.notify) + 2 * u;
            if (privacy) y = link(c, v, y, 5, "Privacy choices") + 2 * u;
            if (policy != null) y = link(c, v, y, 6, "Privacy policy") + 2 * u;
            y = link(c, v, y, 7, "Restore purchases") + 3 * u;
            art.ink(c, "Jelly Keep · font: Lilita One (OFL)", g.W / 2, box.bottom - 4 * u, 2.6f * u, A2, Paint.Align.CENTER);
            end(c);
        }

        String policyUrl() {
            String s = g.getContext().getString(R.string.privacy_url);
            return s.isEmpty() ? null : s;
        }

        float toggle(Canvas c, RectF v, float y, int k, String name, boolean on) {
            float u = u();
            tmp.set(v.left, y, v.right, y + 11 * u);
            card(g, c, tmp, false);
            float ix = tmp.left + 6 * u, iy = tmp.centerY(), is = 8 * u;
            switch (k) {
                case 0: if (art.icSound != null && art.icMute != null) art.icon(c, on ? art.icSound : art.icMute, ix, iy, is); else art.music(c, ix, iy, is, on); break;
                case 1: art.music(c, ix, iy, is, on); break;
                case 2: art.haptics(c, ix, iy, is, on); break;
                case 3: art.shakeIcon(c, ix, iy, is, on); break;
                default: art.bell(c, ix, iy, is, on); break;
            }
            art.label(c, name, tmp.left + 13 * u, tmp.centerY() + 1.4f * u, 3.8f * u, A7, Paint.Align.LEFT);
            tmp2.set(tmp.right - 19 * u, tmp.top + 2 * u, tmp.right - 2.5f * u, tmp.bottom - 2 * u);
            ui.button(c, LIST + k, tmp2, on ? GOLD : WOOD);
            art.label(c, on ? "ON" : "OFF", tmp2.centerX(), tmp2.centerY() + 1.3f * u + ui.off(LIST + k, tmp2), 3.6f * u, A7);
            ui.add(LIST + k, tmp);
            return tmp.bottom;
        }

        float link(Canvas c, RectF v, float y, int k, String name) {
            float u = u();
            tmp.set(v.left, y, v.right, y + 11 * u);
            ui.button(c, LIST + k, tmp, WOOD);
            art.label(c, name, tmp.centerX(), tmp.centerY() + 1.3f * u + ui.off(LIST + k, tmp), 3.6f * u, A7);
            return tmp.bottom;
        }

        @Override void tap(int id) {
            Meta m = g.meta;
            switch (id) {
                case LIST: g.sfx.on = !g.sfx.on; g.save(); break;
                case LIST + 1: m.music = !m.music; g.music.play(m.music); m.save(); break;
                case LIST + 2: m.haptics = !m.haptics; m.save(); if (m.haptics) g.buzz(new long[]{0, 30}); break;
                case LIST + 3: m.shake = !m.shake; m.save(); if (m.shake) g.trauma = 0.5f; break;
                case LIST + 4:
                    m.notify = !m.notify; m.save();
                    if (m.notify) Notify.requestPermission(g);
                    else Notify.cancel(g.getContext());
                    break;
                case LIST + 5: g.ads.showPrivacyOptions(); break;
                case LIST + 6:
                    try { g.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(policyUrl()))); }
                    catch (Exception e) { g.toast("No browser found"); }
                    break;
                case LIST + 7: g.store.restore(); break;
                default: super.tap(id);
            }
        }
    }

    // =============================================================== confirm dialogs
    static final class Confirm extends Screen {
        final String title, body, yes, placement;
        final Runnable onYes;
        final boolean dimissOnAlt;

        private Confirm(Game g, String title, String body, String yes, String placement, Runnable onYes) {
            super(g);
            this.title = title; this.body = body; this.yes = yes; this.placement = placement; this.onYes = onYes; this.dimissOnAlt = true;
        }

        static Confirm plain(Game g, String title, String body, String yes, Runnable onYes) { return new Confirm(g, title, body, yes, null, onYes); }

        /** Watch-an-ad offer: the reward is spelled out before the player decides. */
        static Confirm ad(Game g, String title, String body, String yes, String placement, Runnable reward) { return new Confirm(g, title, body, yes, placement, reward); }

        static Confirm purchase(Game g, String sku, String price, Runnable buy) {
            return new Confirm(g, "Test purchase", "This build simulates Google Play. Pretend to buy " + skuName(sku) + " for " + price + "?", "BUY", null, buy);
        }

        static Confirm notifyAsk(Game g) {
            g.meta.notifyAsked = true;
            g.meta.save();
            return new Confirm(g, "Reminders?", "A gentle nudge when your treasury is full or your daily gift is waiting. At most one a day.", "YES PLEASE", null,
                    () -> Notify.requestPermission(g));
        }

        static String skuName(String sku) {
            switch (sku) {
                case Store.ROYAL_PASS: return "the Royal Pass";
                case Store.STARTER: return "the Starter Pack";
                case Store.SPOON: return "the Golden Spoon";
                case Store.GEMS_S: return "80 gems";
                case Store.GEMS_M: return "500 gems";
                case Store.GEMS_L: return "1200 gems";
                default: return "the Gem Jar";
            }
        }

        @Override void draw(Canvas c) {
            float u = u();
            RectF v = begin(c, title, 80 * u, 60 * u, g.H * 0.5f, false);
            ui.ink(c, body, g.W / 2, v.top + 4 * u, v.width() - 4 * u, 3.5f * u, A1, Paint.Align.CENTER);
            boolean ad = placement != null && !g.meta.royalPass;
            tmp.set(box.left + 6 * u, box.bottom - 30 * u, box.right - 6 * u, box.bottom - 17 * u);
            action(c, OK, tmp, GOLD, yes, ad);
            if (ad && g.ads.rewardedState() == Ads.LOADING) g.shimmer(c, tmp);
            tmp.set(box.left + 18 * u, box.bottom - 14 * u, box.right - 18 * u, box.bottom - 5 * u);
            action(c, ALT, tmp, WOOD, "not now", false);
            end(c);
        }

        @Override void tap(int id) {
            if (id == OK) {
                if (placement != null) g.watchAd(placement, () -> { close(); onYes.run(); });
                else { close(); onYes.run(); }
            } else if (id == ALT) close();
        }
    }

    // =============================================================== simulated ad (default build)
    static final class AdSim extends Screen {
        static final float LEN = 2.2f;
        final boolean rewarded;
        final Runnable done;
        boolean paid;

        AdSim(Game g, boolean rewarded, Runnable done) { super(g); this.rewarded = rewarded; this.done = done; }

        @Override boolean dismissable() { return false; }

        @Override void update(float dt) {
            super.update(dt);
            if (t >= LEN && !paid) { paid = true; close(); done.run(); }
        }

        @Override void draw(Canvas c) {
            float u = u();
            g.fill.setShader(null); g.fill.setColor(0xF00B0918); c.drawRect(0, 0, g.W, g.H, g.fill);
            RectF v = begin(c, rewarded ? "AD" : "AD BREAK", 80 * u, 54 * u, g.H * 0.46f, false);
            ui.ink(c, "Simulated ad. The live build shows a real one here.", g.W / 2, v.top + 4 * u, v.width() - 6 * u, 3.4f * u, A1, Paint.Align.CENTER);
            tmp.set(box.left + 8 * u, box.bottom - 16 * u, box.right - 8 * u, box.bottom - 11 * u);
            bar(g, c, tmp, Math.min(1, t / LEN));
            art.ink(c, rewarded ? "reward in " + Math.max(0, (int) Math.ceil(LEN - t)) + "..." : "back in a moment", g.W / 2, box.bottom - 5 * u, 3.2f * u, A2, Paint.Align.CENTER);
            end(c);
        }
    }

    // =============================================================== one-time starter offer
    static final class Offer extends Screen {
        Offer(Game g) { super(g); }

        @Override void draw(Canvas c) {
            float u = u();
            RectF v = begin(c, "STARTER PACK", 82 * u, 80 * u, g.H * 0.5f, true);
            if (art.chestOpen != null) art.icon(c, art.chestOpen, g.W / 2, v.top + 12 * u, 22 * u);
            float y = v.top + 28 * u;
            art.gem(c, g.W / 2 - 22 * u, y - 1.4f * u, 7 * u);
            art.label(c, "300 gems", g.W / 2 - 17 * u, y + 1 * u, 4.4f * u, A7, Paint.Align.LEFT);
            art.coin(c, g.W / 2 - 22 * u, y + 7 * u, 3 * u);
            art.label(c, "+50% coins forever", g.W / 2 - 17 * u, y + 9 * u, 4.4f * u, A7, Paint.Align.LEFT);
            art.ink(c, "one time only", g.W / 2, y + 16 * u, 3 * u, A2, Paint.Align.CENTER);
            tmp.set(box.left + 10 * u, box.bottom - 17 * u, box.right - 10 * u, box.bottom - 5 * u);
            String price = priceOrNull(g, Store.STARTER);
            action(c, OK, tmp, GOLD, price == null ? "BUY" : "BUY · " + price, false);
            if (price == null) g.shimmer(c, tmp);
            end(c);
        }

        @Override void tap(int id) {
            if (id == OK) { close(); g.store.buy(Store.STARTER); }
            else super.tap(id);
        }
    }
}
