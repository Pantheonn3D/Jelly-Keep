package com.pantheon3d.jellykeep;

import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileOutputStream;

/** Stumps, king and UI line up on phones, and wide screens (tablets, landscape on Android 16) get a portrait stage. */
@RunWith(RobolectricTestRunner.class)
public class AspectTest {
    @Test public void everyScreenShape() throws Exception {
        int[][] sizes = {{1080, 1920}, {1440, 3200}, {720, 1600}, {1600, 2560}, {2560, 1600}};
        new File("build/shots").mkdirs();
        for (int[] s : sizes) {
            MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
            Game g = new Game(a);
            g.measure(s[0], s[1]); g.layout(0, 0, s[0], s[1]);
            g.screens.clear(); g.entered = true;
            int[] lay = {1, 0, 2, 2, 0, 1};
            for (int i = 0; i < 6; i++) g.tType[i] = lay[i];
            for (int k = 0; k < 30; k++) g.update(1 / 60f);
            String tag = s[0] + "x" + s[1];
            assertTrue(tag + " stage is portrait", g.W <= g.H * 0.62f + 1);
            for (int i = 0; i < 6; i++) {
                assertTrue(tag + " pad " + i + " on stage", g.padX[i] > 5 * g.u && g.padX[i] < g.W - 5 * g.u);
                assertTrue(tag + " pad " + i + " in the field", g.padY[i] > g.fieldTop && g.padY[i] < g.fieldBot);
            }
            Bitmap b = Bitmap.createBitmap(s[0], s[1], Bitmap.Config.ARGB_8888);
            g.frame(new Canvas(b));
            try (FileOutputStream o = new FileOutputStream("build/shots/aspect_" + tag + ".png")) { b.compress(Bitmap.CompressFormat.PNG, 100, o); }
        }
    }
}
