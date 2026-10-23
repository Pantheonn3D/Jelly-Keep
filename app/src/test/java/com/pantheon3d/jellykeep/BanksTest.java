package com.pantheon3d.jellykeep;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BanksTest {
    @Test public void everyBankLoads() {
        Sfx s = new Sfx(RuntimeEnvironment.getApplication());
        for (String n : new String[]{"popper_shoot", "pebble_hit", "mortar_launch", "goo_splat", "wall_slam", "tower_build",
                "tower_upgrade", "strike", "strike_hit", "deny", "click", "panel_open", "panel_close", "coin"})
            assertTrue(n, s.has(n) && s.bank(n, 0.5f, 0.05f, 0));
        assertTrue(s.nth("coin", 7, 0.2f, 1f));
    }
}
