package org.czo.droid48sx;

import android.content.res.AssetManager;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AssetUtil {

    public static void copyAsset(AssetManager am, boolean force) {
        File hpDir = new File(X48.config_dir);
        copyAsset(am, hpDir.exists() || hpDir.mkdir(), force);
    }

    public static File getSDDir() {
        File hpDir = new File(X48.config_dir);
        if (hpDir.exists())
            return hpDir;
        return null;
    }

    public static void copyAsset(AssetManager am, boolean sd, boolean force) {
        try {
            String[] assets = am.list("");
            for (int i = 0; i < assets.length; i++) {
                boolean hp48 = assets[i].equals("hp48");
                boolean ram = assets[i].equals("ram");
                boolean rom = assets[i].equals("rom");
                int required = 0;
                /*
                 * 2012/08/04 : Modified by Olivier Sirol <czo@free.fr>
                 * hp48sx ram:32KB rom:256KB
                 */
                if (ram)
                    required = 32 * 1024;
                // required = 131072;
                else if (rom)
                    required = 256 * 1024;
                // required = 524288;
                // boolean SKUNK = assets[i].equals("SKUNK");
                if (hp48 || rom || ram) {
                    File fout = new File(X48.config_dir + assets[i]);
                    if (!fout.exists() || fout.length() == 0 || (required > 0 && fout.length() != required) || force) {
                        Dlog.d("Overwriting " + assets[i]);
                        FileOutputStream out = new FileOutputStream(fout);
                        InputStream in = am.open(assets[i]);
                        byte[] buffer = new byte[8192];
                        int n = -1;
                        while ((n = in.read(buffer)) > -1) {
                            out.write(buffer, 0, n);
                        }
                        out.close();
                        in.close();
                    }
                }
            }
        } catch (Throwable e) {
            Dlog.e("Error: " + e.getMessage());
        }
    }

    public static boolean isFilesReady() {
        File hpDir = new File(X48.config_dir);
        File hp = new File(hpDir, "hp48");
        File rom = new File(hpDir, "rom");
        File ram = new File(hpDir, "ram");
        return hp.exists() && hp.length() > 0 && rom.exists() && rom.length() > 0 && ram.exists() && ram.length() > 0;
    }
}
