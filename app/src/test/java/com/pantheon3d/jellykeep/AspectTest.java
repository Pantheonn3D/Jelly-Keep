package com.pantheon3d.jellykeep;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(RobolectricTestRunner.class)
public class AspectTest {
    @Test public void stumpsLineUpOnEveryScreenShape() throws Exception {
        int[][] sizes = {{1080, 1920}, {1440, 3200}, {720, 1600}};
        new File("build/shots").mkdirs();
        for (int[] s : sizes) {
            MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
            Game g = new Game(a);
            g.measure(s[0], s[1]); g.layout(0, 0, s[0], s[1]);
            g.overlay = 0;
            int[] lay = {1, 0, 2, 2, 0, 1};
            for (int i = 0; i < 6; i++) g.tType[i] = lay[i];
            for (int k = 0; k < 30; k++) g.update(1 / 60f);
            Bitmap b = Bitmap.createBitmap(s[0], s[1], Bitmap.Config.ARGB_8888);
            g.render(new Canvas(b));
            try (FileOutputStream o = new FileOutputStream("build/shots/aspect_" + s[0] + "x" + s[1] + ".png")) { b.compress(Bitmap.CompressFormat.PNG, 100, o); }
        }
    }
}
