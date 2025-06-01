package org.czo.droid48sx;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class X48 extends Activity {

    private HPView mainView;
    private boolean need_to_quit;
    public static String config_dir;
    public static String sdcard_dir;
    static final private int LOAD_ID = Menu.FIRST + 1;
    static final private int SAVE_ID = Menu.FIRST + 2;
    static final private int QUIT_ID = Menu.FIRST + 3;
    static final private int RESET_ID = Menu.FIRST + 4;
    static final private int SETTINGS_ID = Menu.FIRST + 5;
    static final private int LITEKBD_ID = Menu.FIRST + 6;
    static final private int SAVE_CHECKPOINT_ID = Menu.FIRST + 7;
    static final private int RESTORE_CHECKPOINT_ID = Menu.FIRST + 8;
    static final private int FULL_RESET_ID = Menu.FIRST + 9;
    static final private int SAVE_ZIP_ID = Menu.FIRST + 10;
    static final private int RESTORE_ZIP_ID = Menu.FIRST + 11;
    static final private int MANUAL_VOL1_ID = Menu.FIRST + 12;
    static final private int MANUAL_VOL2_ID = Menu.FIRST + 13;
    static final private int LOADOBJECT_ID = Menu.FIRST + 14;

    static final private int ROM_ID = 123;
    private static EmulatorThread thread;

    private static final String ACTION_FULL_RESET = "android.intent.action.FULL_RESET";
    private static final String ACTION_RESTORE_CHECKPOINT = "android.intent.action.RESTORE_CHECKPOINT";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {

            Dlog.d("===================== starting activity");
            // try
            //  /sdcard/Android/data/org.czo.droid48sx/files/
            if (getExternalFilesDir(null) != null) {
                config_dir = getExternalFilesDir(null).getAbsolutePath() + "/";
            } else {
                config_dir = getFilesDir().getAbsolutePath() + "/";
            }

            // config_dir = "/badone" ;
            File hpDir = new File(config_dir);
            if (!hpDir.exists() || !hpDir.isDirectory()) {
                Dlog.e("ERROR: cannot open " + config_dir);
                Toast.makeText(getApplicationContext(), "ERROR: cannot open " + config_dir, Toast.LENGTH_LONG).show();
                finish();
            }
            Dlog.e("config_dir java: " + config_dir);
            getExternalPath(config_dir);

            // /sdcard/
            sdcard_dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS;
            File sdcardDir = new File(sdcard_dir);
            if (!sdcardDir.exists() || !sdcardDir.isDirectory()) {
                sdcard_dir = "/sdcard";
            }
            Dlog.e("sdcard_dir: " + sdcard_dir);

        } catch (Throwable e) {
            Dlog.d("Error: " + e.getMessage());
        }

        Dlog.d("===================== getIntentAction = " + getIntent().getAction() + " =====================");

        if (ACTION_FULL_RESET.equals(getIntent().getAction())) {
            fullReset();
        }
        if (ACTION_RESTORE_CHECKPOINT.equals(getIntent().getAction())) {
            restoreCheckpoint();
        }

        Dlog.d("copyAsset...");
        AssetUtil.copyAsset(getResources().getAssets(), false);
        saveFirstCheckpoint();
        verifyNoFileZero();

        Dlog.d("mainView and getPrefs...");
        setContentView(R.layout.main);
        mainView = findViewById(R.id.hpview);
        getPrefs();

        readyToGo();

        if (!AssetUtil.isFilesReady()) {
            Dlog.e("ERROR: cannot open hp48sx");
            Toast.makeText(getApplicationContext(), "ERROR: cannot open hp48sx", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void readyToGo() {

        // Build.VERSION.SDK_INT >= 21

        // getWindow().setStatusBarColor(Color.parseColor("#AA252523"));
        // getWindow().setNavigationBarColor(Color.parseColor("#AA252523"));
        getWindow().setStatusBarColor(Color.rgb(0, 0, 0));
        getWindow().setNavigationBarColor(Color.parseColor("#393938"));
        Objects.requireNonNull(getActionBar()).setBackgroundDrawable(new ColorDrawable(Color.parseColor("#AA252523")));

        getPrefs();
        checkPrefs();

        thread = new EmulatorThread(this);
        thread.start();
        mainView.resume();
    }

    private static boolean hide = false;
    private static boolean hidekey = false;

    public void Menu() {
        hide ^= true;
        hidekey = true;
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (hide) { // hide action bar
            if (mPrefs.getBoolean("fullScreen", false)) {
                if (mainView != null) {
                    mainView.setSystemUiVisibility(
                        HPView.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        HPView.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        HPView.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        HPView.SYSTEM_UI_FLAG_HIDE_NAVIGATION | // hide nav bar
                        HPView.SYSTEM_UI_FLAG_FULLSCREEN |      // hide status bar
                        HPView.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // | HPView.SYSTEM_UI_FLAG_IMMERSIVE
                    );
                }
            }
            Objects.requireNonNull(getActionBar()).hide();
        } else { // show action bar
            if (mPrefs.getBoolean("fullScreen", false)) {
                if (mainView != null) {
                    mainView.setSystemUiVisibility(
                        HPView.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        HPView.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        HPView.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                }
            }
            Objects.requireNonNull(getActionBar()).show();
        }
    }

    public void hideSystemUI() {

        // Build.VERSION.SDK_INT >= 21

        hide = true;
        getWindow().setStatusBarColor(Color.parseColor("#AA252523"));
        getWindow().setNavigationBarColor(Color.parseColor("#AA252523"));
        Objects.requireNonNull(getActionBar()).setBackgroundDrawable(new ColorDrawable(Color.parseColor("#AA252523")));

        if (mainView != null) {
            mainView.setSystemUiVisibility(
                HPView.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                HPView.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                HPView.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                HPView.SYSTEM_UI_FLAG_HIDE_NAVIGATION | // hide nav bar
                HPView.SYSTEM_UI_FLAG_FULLSCREEN |      // hide status bar
                HPView.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // | HPView.SYSTEM_UI_FLAG_IMMERSIVE
            );
            Objects.requireNonNull(getActionBar()).hide();
        }
    }

    public void showSystemUI() {

        // Build.VERSION.SDK_INT >= 21

        hide = true;
        getWindow().setStatusBarColor(Color.rgb(0, 0, 0));
        getWindow().setNavigationBarColor(Color.parseColor("#393938"));

        if (mainView != null) {
            mainView.setSystemUiVisibility(
                HPView.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            Objects.requireNonNull(getActionBar()).hide();
        }
    }

    public void hideActionBar() {
        if (hidekey) {
            checkfullscreen();
            hidekey = false;
        }
    }

    public void checkfullscreen() {
        Dlog.d("===================== checkfullscreen");
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (mPrefs.getBoolean("fullScreen", false)) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
    }

    public void getPrefs() {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        saveonExit = mPrefs.getBoolean("saveOnExit", true);
        if (mainView != null) {
            mainView.setKeybLite(mPrefs.getBoolean("keybLite", false));
            mainView.setHapticFeedbackEnabled(mPrefs.getBoolean("haptic", true));
            mainView.setSound(mPrefs.getBoolean("sound", false));
            // large_width is indeed !lcd_int
            mainView.setFullWidth(mPrefs.getBoolean("large_width", false));
            mainView.setScaleControls(mPrefs.getBoolean("scale_buttons", true));
        }
    }

    public void checkPrefs() {

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        checkfullscreen();
        if (mainView != null) {
            mainView.requestLayout();
        }
    }

    public void changeKeybLite() {
        if (mainView != null) {
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            mainView.setKeybLite(!mainView.isKeybLite());
            Editor e = mPrefs.edit();
            e.putBoolean("keybLite", mainView.isKeybLite());
            e.commit();
            mainView.backBuffer = null;
            mainView.needFlip = true;
        }
    }

    public void refreshMainScreen(short[] data) {
        mainView.refreshMainScreen(data);
    }

    public int waitEvent() {
        return mainView.waitEvent();
    }

    public void refreshIcons(boolean[] i) {
        mainView.refreshIcons(i);
    }

    public native void startHPEmulator();

    public native void resetHPEmulator();

    public native void saveState();

    public native void stopHPEmulator();

    public native int buttonPressed(int code);

    public native int buttonReleased(int code);

    public native void registerClass(X48 instance);

    public native int fillAudioData(short[] data);

    public native int fillScreenData(short[] data, boolean[] ann);

    public native void flipScreen();

    public native int loadProg(String filename);

    public native void getExternalPath(String path);

    public native void setBlankColor(short s);

    public void emulatorReady() {
        mainView.emulatorReady();
    }

    public void pauseEvent() {
        mainView.pauseEvent();
    }

    static {
        System.loadLibrary("droid48sx");
    }

    @Override
    protected void onResume() {
        Dlog.d("===================== resume");
        super.onResume();
        if (mainView != null) {
            mainView.resume();
            checkfullscreen();
            hidekey = false;
            mainView.requestLayout();
        }
    }

    /**
     * Called when your activity's options menu needs to be created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item;
        super.onCreateOptionsMenu(menu);

        item = menu.add(0, FULL_RESET_ID, 0, R.string.full_reset_memory);
        item.setIcon(R.drawable.ic_action_reset);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        item = menu.add(0, RESTORE_CHECKPOINT_ID, 0, R.string.restore_checkpoint);
        item.setIcon(R.drawable.ic_action_restore);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        item = menu.add(0, SAVE_CHECKPOINT_ID, 0, R.string.save_checkpoint);
        item.setIcon(R.drawable.ic_action_archive);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        item = menu.add(0, SETTINGS_ID, 0, R.string.settings);
        item.setIcon(R.drawable.ic_action_settings);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        item = menu.add(0, LOAD_ID, 0, R.string.load_prog);
        item.setIcon(R.drawable.ic_action_load_object);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        item = menu.add(0, SAVE_ZIP_ID, 0, R.string.save_zip);
        item.setIcon(R.drawable.ic_action_save_zip);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        item = menu.add(0, RESTORE_ZIP_ID, 0, R.string.restore_zip);
        item.setIcon(R.drawable.ic_action_restore_zip);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        // Hp gave the HP48 ROM in 2000, but I don't know if they also gave the manuals...
        /*
        item = menu.add(0, MANUAL_VOL1_ID, 0, R.string.manual_vol1);
        item.setIcon(R.drawable.ic_action_info);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        item = menu.add(0, MANUAL_VOL2_ID, 0, R.string.manual_vol2);
        item.setIcon(R.drawable.ic_action_info);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        */

        item = menu.add(0, QUIT_ID, 0, R.string.button_quit);
        item.setIcon(R.drawable.ic_action_power);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }

    public void deleteFile(File src) throws IOException {
        if (src.exists()) {
            src.delete();
        }
    }

    private boolean deleteDirectory(File directoryToBeDeleted) throws IOException {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    public void copyFile(File src, File dst) throws IOException {
        if (src.exists()) {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    protected void restoreCheckpoint() {
        Dlog.d("===================== Checkpoint restored...");

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Editor spe = mPrefs.edit();
        boolean ResultOK = true;

        try {
            for (String s : new String[] {"port1", "port2"}) {
                deleteFile(new File(X48.config_dir + s));
            }
            for (String s : new String[] {"hp48", "ram", "rom", "port1", "port2"}) {
                copyFile(new File(X48.config_dir + "checkpoint/" + s), new File(X48.config_dir + s));
            }
            File p1 = new File(X48.config_dir + "port1");
            if (p1.exists()) {
                spe.putString("port1", "128");
            } else {
                spe.putString("port1", "0");
            }
            File p2 = new File(X48.config_dir + "port2");
            if (p2.exists()) {
                spe.putString("port2", "128");
            } else {
                spe.putString("port2", "0");
            }

            spe.commit();

        } catch (Throwable e) {
            Dlog.d("Error: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            ResultOK = false;
        }
        if (ResultOK)
            Toast.makeText(getApplicationContext(), "Checkpoint restored. Please re-run the app!", Toast.LENGTH_LONG).show();

        need_to_quit = true;
        saveonExit = false;
        finish();
    }

    protected void fullReset() {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Editor spe = mPrefs.edit();

        Dlog.d("===================== Full reset done...");
        Toast.makeText(getApplicationContext(), "Full reset done. Please re-run the app!", Toast.LENGTH_LONG).show();
        try {
            spe.putString("port1", "0");
            spe.putString("port2", "0");
            spe.commit();
            for (String s : new String[] {"hp48", "ram", "rom", "port1", "port2"}) {
                deleteFile(new File(X48.config_dir + s));
            }
        } catch (Throwable e) {
            Dlog.d("Error: " + e.getMessage());
        }

        need_to_quit = true;
        saveonExit = false;
        finish();
    }

    protected void saveCheckpoint() {

        Dlog.d("===================== Checkpoint saved...");
        Toast.makeText(getApplicationContext(), "Checkpoint saved...", Toast.LENGTH_LONG).show();

        saveState();

        File hpDir = new File(X48.config_dir, "checkpoint");
        if (!hpDir.exists())
            hpDir.mkdir();

        try {
            for (String s : new String[] {"port1", "port2"}) {
                deleteFile(new File(X48.config_dir + "checkpoint/" + s));
            }
            for (String s : new String[] {"hp48", "ram", "rom", "port1", "port2"}) {
                copyFile(new File(X48.config_dir + s), new File(X48.config_dir + "checkpoint/" + s));
            }
        } catch (Throwable e) {
            Dlog.d("Error: " + e.getMessage());
        }
        checkPrefs();
    }

    protected void saveFirstCheckpoint() {

        Dlog.d("saveFirstCheckpoint...");

        File hpDir = new File(X48.config_dir, "checkpoint");
        if (!hpDir.exists()) {
            hpDir.mkdir();
            try {
                for (String s : new String[] {"hp48", "ram", "rom", "port1", "port2"}) {
                    copyFile(new File(X48.config_dir + s), new File(X48.config_dir + "checkpoint/" + s));
                }
            } catch (Throwable e) {
                Dlog.d("Error: " + e.getMessage());
            }
        }
    }

    protected void verifyNoFileZero() {
        Dlog.d("verifyNoFileZero...");
        boolean ResetIt = false;
        try {
            for (String s : new String[] {"hp48", "ram", "rom", "port1", "port2"}) {
                File T = new File(X48.config_dir + s);

                if (T.exists() && T.length() == 0) {
                    Dlog.d(s + " = " + T.length());
                    ResetIt = true;
                }
            }
        } catch (Throwable e) {
            Dlog.d("Error: " + e.getMessage());
        }

        if (ResetIt) {
            restoreCheckpoint();
        }
    }

    public void restoreZIP(String outfile) {
        Dlog.d("===================== restoreZIP...");

        boolean ResultOK = false;
        File restoredDir = new File(X48.config_dir, "restored");
        Dlog.e("zip infile: " + outfile);

        try {
            String fileZip = outfile;

            if (restoredDir.exists())
                deleteDirectory(restoredDir);

            if (!restoredDir.exists())
                restoredDir.mkdir();

            byte[] buffer = new byte[1024];
            ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                String FileName = zipEntry.getName();
                File newFile = new File(restoredDir, FileName);
                if (FileName.equals("hp48") ||
                    FileName.equals("rom") ||
                    FileName.equals("ram") ||
                    FileName.equals("port1") ||
                    FileName.equals("port2")) {
                    Dlog.d("new name: " + FileName);
                    // write file content
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();

            for (String s : new String[] {"hp48", "ram", "rom", "port1", "port2"}) {
                File T = new File(X48.config_dir + "restored/" + s);
                if (T.exists()) {
                    ResultOK = true;
                }
            }

            if (ResultOK) {

                File checkpointDir = new File(X48.config_dir, "checkpoint");

                if (checkpointDir.exists())
                    deleteDirectory(checkpointDir);

                if (!checkpointDir.exists())
                    checkpointDir.mkdir();

                for (String s : new String[] {"hp48", "ram", "rom", "port1", "port2"}) {
                    copyFile(new File(X48.config_dir + "restored/" + s), new File(X48.config_dir + "checkpoint/" + s));
                }
            }
            if (restoredDir.exists()) {
                deleteDirectory(restoredDir);
            }
        } catch (Throwable e) {
            Dlog.d("Error: " + e.getMessage());
        }

        if (ResultOK)
            restoreCheckpoint();
        else
            Toast.makeText(getApplicationContext(), "ERROR: WRONG ZIP! This zip file must contain the files 'hp48', 'rom', 'ram' and maybe 'port1' or 'port2'.", Toast.LENGTH_LONG).show();

        checkPrefs();
    }

    private void saveZip() {
        Dlog.d("===================== saveZIP...");

        boolean ResultOK = true;
        File checkpointDir = new File(X48.config_dir, "checkpoint");
        String date = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm").format(new Date(System.currentTimeMillis()));
        String outfile = sdcard_dir + "/48sx_cp_" + date + ".zip";
        Dlog.e("zip outfile: " + outfile);

        if (checkpointDir.exists()) {
            try {
                ArrayList<String> files = new ArrayList<String>();

                for (String s : new String[] {"hp48", "ram", "rom", "port1", "port2"}) {
                    File T = new File(X48.config_dir + "checkpoint/" + s);
                    if (T.exists()) {
                        files.add(X48.config_dir + "checkpoint/" + s);
                    }
                }

                FileOutputStream fos = new FileOutputStream(outfile);
                ZipOutputStream zipOut = new ZipOutputStream(fos);
                for (String srcFile : files) {
                    File fileToZip = new File(srcFile);
                    FileInputStream fis = new FileInputStream(fileToZip);
                    ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                    zipOut.putNextEntry(zipEntry);

                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = fis.read(bytes)) >= 0) {
                        zipOut.write(bytes, 0, length);
                    }
                    fis.close();
                }
                zipOut.close();
                fos.close();
            } catch (Throwable e) {
                Dlog.e("Error: " + e.getMessage());
                Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                ResultOK = false;
            }
            if (ResultOK) {
                Toast.makeText(getApplicationContext(), outfile + " saved...", Toast.LENGTH_LONG).show();
            }
        }
        checkPrefs();
    }

    private boolean accessStorage() {
        boolean ResultAcc = true;

        try {
            if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 29) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
                    Toast.makeText(getApplicationContext(), "Please re-run again if you accepted the permissions", Toast.LENGTH_LONG).show();
                    ResultAcc = false;
                } else {
                    ResultAcc = true;
                }
            }
            /*
            if (Build.VERSION.SDK_INT >= 30 && Build.VERSION.SDK_INT <= 32) {
                if (checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_MEDIA_LOCATION}, 123);
                    Toast.makeText(getApplicationContext(), "Please re-run again if you accepted the permissions", Toast.LENGTH_LONG).show();
                    ResultAcc = false;
                } else {
                    ResultAcc = true;
                }
            }
            */
            if (Build.VERSION.SDK_INT >= 33) {
                if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(new String[] {Manifest.permission.READ_MEDIA_IMAGES}, 123);
                    Toast.makeText(getApplicationContext(), "Please re-run again if you accepted the permissions", Toast.LENGTH_LONG).show();
                    ResultAcc = false;
                } else {
                    ResultAcc = true;
                }
            }

        } catch (Throwable e) {
            Dlog.e("Error: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            ResultAcc = false;
        }

        return ResultAcc;
    }

    private void storageEnabledSaveZip() {
        if (accessStorage()) saveZip();
    }

    private void storageEnabledRestoreZip() {
        if (accessStorage()) openZip();
    }

    private void storageEnabledOpenLoad() {
        if (accessStorage()) openLoad();
    }

    private void openLoad() {
        Intent loadFileIntent = new Intent();
        loadFileIntent.setClass(this, ProgListView.class);
        startActivityForResult(loadFileIntent, LOAD_ID);
    }

    private void loadObject(final int requestCode, final int resultCode, final Intent extras) {

        final String filename = extras.getStringExtra("currentFile");

        if (filename != null) {
            Dlog.d("===================== LoadObjet = " + filename);
            int retCode = loadProg(filename);
            if (retCode == 1) {
                flipScreen();
                SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                boolean msgbox = mPrefs.getBoolean("no_loadprog_msgbox", false);
                if (!msgbox) {
                    if (mainView != null)
                        mainView.pressON();
                    // showDialog(DIALOG_PROG_OK);
                    Toast.makeText(getApplicationContext(), filename + " loaded...", Toast.LENGTH_LONG).show();
                }
            } else {
                // showDialog(DIALOG_PROG_KO);
                Toast.makeText(getApplicationContext(), "Error: " + getString(R.string.prog_ko), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void storageEnabledOpenLoadObject() {
        if (accessStorage()) openDocObject();
    }

    private void openDocObject() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, LOADOBJECT_ID);
    }

    private void loadDocObject(final int requestCode, final int resultCode, final Intent extras) {

        String filename = null;

        if (extras != null) {
            Uri uri = extras.getData();
            String url = null;
            if (uri != null)
                url = uri.toString();
            if (url != null) {
                Dlog.d("===================== URL = " + url);
                filename = url;
            }
        }

        if (filename != null) {
            Dlog.e("===================== LoadObjet = " + filename);
            int retCode = loadProg(filename);
            if (retCode == 1) {
                flipScreen();
                SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                boolean msgbox = mPrefs.getBoolean("no_loadprog_msgbox", false);
                if (!msgbox) {
                    if (mainView != null)
                        mainView.pressON();
                    // showDialog(DIALOG_PROG_OK);
                    Toast.makeText(getApplicationContext(), filename + " loaded...", Toast.LENGTH_LONG).show();
                }
            } else {
                // showDialog(DIALOG_PROG_KO);
                Toast.makeText(getApplicationContext(), "Error: " + getString(R.string.prog_ko), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openZip() {
        Intent loadFileIntent = new Intent();
        loadFileIntent.setClass(this, ProgListView.class);
        startActivityForResult(loadFileIntent, RESTORE_ZIP_ID);
    }

    private void loadZip(final int requestCode, final int resultCode, final Intent extras) {

        final String filename = extras.getStringExtra("currentFile");

        if (filename != null) {
            Dlog.d("===================== LoadZIP = " + filename);
            restoreZIP(filename);
        }
    }

    public void manualVol1() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/czodroid/droid48sx/raw/master/store/doc/HP48sx-owner-manual-vol-1.pdf"));
        startActivity(browserIntent);
    }

    public void manualVol2() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/czodroid/droid48sx/raw/master/store/doc/HP48sx-owner-manual-vol-2.pdf"));
        startActivity(browserIntent);
    }

    /**
     * Called when a menu item is selected.
     */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Editor spe = mPrefs.edit();

        switch (item.getItemId()) {

        case LOAD_ID:
            storageEnabledOpenLoad();
            break;

        case LOADOBJECT_ID:
            storageEnabledOpenLoadObject();
            break;

        case SAVE_ZIP_ID:
            storageEnabledSaveZip();
            return true;

        case RESTORE_ZIP_ID:
            storageEnabledRestoreZip();
            return true;

        case FULL_RESET_ID:
            fullReset();
            return true;

        case RESTORE_CHECKPOINT_ID:
            restoreCheckpoint();
            return true;

        case SAVE_CHECKPOINT_ID:
            saveCheckpoint();
            return true;

        case MANUAL_VOL1_ID:
            manualVol1();
            return true;

        case MANUAL_VOL2_ID:
            manualVol2();
            return true;

        case RESET_ID:
            Dlog.d("===================== Reset done...");
            Toast.makeText(getApplicationContext(), "Reset done...", Toast.LENGTH_LONG).show();
            AssetUtil.copyAsset(getResources().getAssets(), true);
            need_to_quit = true;
            saveonExit = false;
            finish();
            return true;

        case SAVE_ID:
            Dlog.d("===================== State saved...");
            Toast.makeText(getApplicationContext(), "State saved...", Toast.LENGTH_LONG).show();
            saveState();
            return true;

        case SETTINGS_ID:
            Intent settingsIntent = new Intent();
            settingsIntent.setClass(this, Settings.class);
            startActivityForResult(settingsIntent, SETTINGS_ID);
            break;

        case QUIT_ID:
            finish();
            return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    private static final int DIALOG_PROG_OK = 1;
    private static final int DIALOG_PROG_KO = 2;
    private static final int DIALOG_ROM_KO = 3;
    private static final int DIALOG_RAM_KO = 4;
    private static final int DIALOG_RAM_OK = 5;
    private static final int DIALOG_RAM_OK2 = 6;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_PROG_OK:
            return new AlertDialog.Builder(X48.this)
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.help)
                .setMessage(R.string.prog_ok)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        checkPrefs();
                    }
                })
                .create();
        case DIALOG_PROG_KO:
            return new AlertDialog.Builder(X48.this)
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.help)
                .setMessage(R.string.prog_ko)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        checkPrefs();
                    }
                })
                .create();
        case DIALOG_ROM_KO:
            return new AlertDialog.Builder(X48.this)
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.help)
                .setMessage(R.string.rom_ko)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        onDestroy();
                    }
                })
                .create();
        case DIALOG_RAM_KO:
            return new AlertDialog.Builder(X48.this)
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.help)
                .setMessage(R.string.ram_install_error)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        checkPrefs();
                    }
                })
                .create();
        case DIALOG_RAM_OK:
            return new AlertDialog.Builder(X48.this)
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.help)
                .setMessage(R.string.ram_install_warning)
                .setPositiveButton(R.string.restart, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        checkPrefs();
                        need_to_quit = true;
                        saveonExit = false;
                        finish();
                    }
                })
                .create();
        }
        return null;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent extras) {
        Dlog.d("requestCode: " + requestCode + " / " + resultCode);
        super.onActivityResult(requestCode, resultCode, extras);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {

            case LOAD_ID: {
                loadObject(requestCode, resultCode, extras);
                break;
            }

            case LOADOBJECT_ID: {
                loadDocObject(requestCode, resultCode, extras);
                break;
            }

            case RESTORE_ZIP_ID: {
                loadZip(requestCode, resultCode, extras);
                break;
            }

            case SETTINGS_ID: {
                SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                String port1 = mPrefs.getString("port1", "0");
                managePort(1, port1);
                String port2 = mPrefs.getString("port2", "0");
                managePort(2, port2);
                getPrefs();
                checkPrefs();
            }
            }
        }
    }

    private boolean saveonExit;

    private void managePort(int number, String value) {
        int size = Integer.parseInt(value);
        File f = AssetUtil.getSDDir();
        if (f != null) {
            boolean change = false;
            File port = new File(f, "port" + number);
            /*
             * if (port.exists()) {
             * port.renameTo(new File(f, "bkp.port" + number));
             * }
             */
            if (size == 0) {
                if (port.exists()) {
                    port.delete();
                    Dlog.d("===================== Deleting port" + number + " file.");
                    change = true;
                }
            } else {
                if (port.exists() && (port.length() == 1024L * size)) {

                } else {
                    Dlog.d("===================== Port" + number + " file does not exists or is incomplete. Writing a blank file.");
                    byte[] data = new byte[1024];
                    for (int i = 0; i < data.length; i++)
                        data[i] = 0;
                    try {
                        FileOutputStream fout = new FileOutputStream(port);
                        for (int l = 0; l < size; l++)
                            fout.write(data);
                        fout.close();
                    } catch (Throwable e) {
                        Dlog.d("Error: " + e.getMessage());
                    }
                    change = true;
                }
            }
            if (change)
                showDialog(DIALOG_RAM_OK);
        } else {
            showDialog(DIALOG_RAM_KO);
        }
    }

    @Override
    protected void onStop() {
        Dlog.d("===================== stop");
        super.onStop();
    }

    @Override
    protected void onStart() {
        Dlog.d("===================== start");
        super.onStart();
    }

    @Override
    protected void onPause() {
        Dlog.d("===================== pause");
        super.onPause();
        if (mainView != null)
            mainView.pause();
    }

    @Override
    protected void onDestroy() {
        Dlog.d("===================== onDestroy");
        super.onDestroy();
        if (saveonExit)
            saveState();
        stopHPEmulator();
        if (mainView != null)
            mainView.unpauseEvent();
        // if (need_to_quit)
        //     System.exit(0);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
