package org.czo.droid48sx;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class HPView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final int MAX_TOUCHES = 49;
    private Thread drawThread;
    private final X48 x48;
    private final Bitmap mainScreen;
    private final SurfaceHolder mSurfaceHolder;
    private boolean surfaceValid;
    private final Bitmap[] annImages;
    boolean[] ann;
    int[] ann_pos = {62, 105, 152, 197, 244, 287};
    private final List<Integer> queuedCodes;
    private final int[] touches = new int[MAX_TOUCHES];
    protected boolean needFlip;
    private final short[] buf;
    private final short[] audiobuf;
    private int currentOrientation;
    private final boolean multiTouch;
    private final AudioTrack track;
    private TimerTask audioTask;
    private Timer audioTimer;

    int[][] buttons_coords = new int[MAX_TOUCHES][4];
    int[][] icons_coords = new int[6][2];

    Matrix matrixScreen;
    Matrix matrixBack;
    Paint paint;
    Paint screenPaint = null;

    public HPView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setFocusableInTouchMode(true);
        x48 = ((X48)context);
        multiTouch = x48.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mainScreen = Bitmap.createBitmap(262, 14 + 128, Bitmap.Config.RGB_565);
        queuedCodes = new Vector<Integer>();
        ann = new boolean[6];
        buf = new short[(14 + 128) * 262];
        audiobuf = new short[44100]; // 1s worth
        track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 16384, AudioTrack.MODE_STREAM);
        annImages = new Bitmap[6];
        updateContrast();
        matrixScreen = new Matrix();
        matrixBack = new Matrix();
        annImages[0] = BitmapFactory.decodeResource(x48.getResources(), R.drawable.ann01);
        annImages[1] = BitmapFactory.decodeResource(x48.getResources(), R.drawable.ann02);
        annImages[2] = BitmapFactory.decodeResource(x48.getResources(), R.drawable.ann03);
        annImages[3] = BitmapFactory.decodeResource(x48.getResources(), R.drawable.ann04);
        annImages[4] = BitmapFactory.decodeResource(x48.getResources(), R.drawable.ann05);
        annImages[5] = BitmapFactory.decodeResource(x48.getResources(), R.drawable.ann06);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        for (int i = 0; i < MAX_TOUCHES; i++) {
            keys[i] = BitmapFactory.decodeResource(x48.getResources(), R.drawable.k01 + i, opts);
        }

        paint = new Paint();
        paint.setStyle(Style.FILL);
        paint.setARGB(128, 250, 250, 250);

        screenPaint = null;
        screenPaint = new Paint();

        // audioTask = new TimerTask() {
        // @Override
        // public void run() {
        // if (pause && track.getState() == AudioTrack.STATE_INITIALIZED) {
        // if (sound) {
        // track.play();
        // track.write(audiobuf, 0, x48.fillAudioData(audiobuf));
        // } else {
        // track.stop();
        // }
        // }
        // }
        // };
        // audioTimer = new Timer();
        // audioTimer.schedule(audioTask, 0, 100);
    }

    public void updateContrast() {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(x48);
        String cString = mPrefs.getString("contrast", "1");
        int c = Integer.parseInt(cString);
        if (c < 0)
            c = 0;
        if (c > 2)
            c = 2;
        setContrast(0.5 * c);
        // x48.flipScreen();
    }

    private void setContrast(double contrast_factor) {
        x48.setBlankColor((short)(((int)(15 * contrast_factor + 16) << 11) + ((int)(30 * contrast_factor + 33) << 5) + (int)(15 * contrast_factor + 13)));
    }

    // private short data [];
    private final Bitmap[] keys = new Bitmap[MAX_TOUCHES];
    protected volatile Bitmap backBuffer;
    private boolean fullWidth;

    public boolean isFullWidth() {
        return fullWidth;
    }

    public void setFullWidth(boolean fullWidth) {
        this.fullWidth = fullWidth;
    }

    private boolean scaleControls;

    public boolean isScaleControls() {
        return scaleControls;
    }

    public void setScaleControls(boolean scaleControls) {
        this.scaleControls = scaleControls;
    }

    public void refreshMainScreen(short[] data) {
        Canvas c = null;
        try {
            c = mSurfaceHolder.lockCanvas(null);
            synchronized (mSurfaceHolder) {
                // boolean land = currentOrientation == Configuration.ORIENTATION_LANDSCAPE;
                boolean land = false;

                if (c != null) {

                    if (backBuffer == null) {
                        Dlog.d("init backBuffer !: " + keybLite);
                        backBuffer = Bitmap.createBitmap(c.getWidth(), c.getHeight(), Bitmap.Config.ARGB_8888);
                        Canvas backCanvas = new Canvas(backBuffer);

                        int w = backBuffer.getWidth();
                        int h = backBuffer.getHeight();

                        Dlog.d("w: " + w + ", h:" + h);
                        if (w != 0) {
                            Dlog.d("h/w: " + (float)h / w);
                        }

                        Paint p = new Paint();
                        /* couleur fond bouttons (landscape enter) */
                        int srcColor = Color.rgb(57, 57, 56);
                        p.setColor(srcColor);
                        backCanvas.drawRect(0, 0, w, h, p);
                        /* presque fullscreen */
                        float lcd_ratio = (int)((land ? h : w) / 135f); // was 131
                        screenPaint.setFilterBitmap(false);
                        if (!land && !fullWidth) {
                            screenPaint.setFilterBitmap(true);
                            lcd_ratio = ((land ? (float)h : (float)w) + 0f) / 135f; // was 133
                        }
                        Dlog.d("lcd ratio: " + lcd_ratio);
                        int start_w = (int)(131 * lcd_ratio);
                        int start_h = (int)(73 * lcd_ratio); // was 71
                        float usable_w = w;
                        float remaning_w = 0;
                        float usable_h = h - start_h;
                        if (land) {
                            usable_w = ((float)w) * 5f / 9f;
                            remaning_w = w - usable_w;
                        }
                        int lcd_pos_x = land ? (((int)usable_w - start_w) / 2) : ((w - start_w) / 2);
                        int lcd_pos_y = 0;
                        int lcd_pos_x_end = lcd_pos_x + start_w;
                        int lcd_pos_y_end = lcd_pos_y + start_h;
                        float regular_key_height = usable_h / 9f;
                        // float regular_key_height = usable_h / (8f + 11f / 18f);
                        float regular_key_height_right = h / 7f;
                        float menu_key_height = regular_key_height;
                        // float menu_key_height = regular_key_height * 11f / 18f;

                        if (land) {
                            regular_key_height = usable_h / 4f;
                            // regular_key_height = usable_h / (3f + 11f / 18f);
                            menu_key_height = regular_key_height;
                            // menu_key_height = regular_key_height * 11f / 18f;
                        }
                        Dlog.d("icons_coord y: " + (int)(1 * lcd_ratio));
                        icons_coords = new int[][] {
                            {lcd_pos_x, (int)(1 * lcd_ratio)},
                            {(int)(lcd_pos_x + 21 * lcd_ratio), (int)(1 * lcd_ratio)},
                            {(int)(lcd_pos_x + 45 * lcd_ratio), (int)(1 * lcd_ratio)},
                            {(int)(lcd_pos_x + 67 * lcd_ratio), (int)(1 * lcd_ratio)},
                            {(int)(lcd_pos_x + 91 * lcd_ratio), (int)(1 * lcd_ratio)},
                            {(int)(lcd_pos_x + 112 * lcd_ratio), (int)(1 * lcd_ratio)}};
                        /* couleur num central gx, meme que srcColor pour la sx */
                        int central = Color.rgb(57, 57, 56);
                        /* couleur ligne func */
                        int func = Color.rgb(83, 81, 82);
                        /* couleur fond screen */
                        int green = Color.rgb(189, 195, 165);
                        p.setColor(green);
                        backCanvas.drawRect(0, 0, usable_w, start_h, p);
                        p.setColor(func);
                        if (!keybLite || land)
                            backCanvas.drawRect(0, start_h, usable_w, start_h, p);
                        matrixScreen = new Matrix();

                        matrixScreen.preScale(lcd_ratio / 2, lcd_ratio / 2);
                        matrixScreen.postTranslate(lcd_pos_x, lcd_pos_y);
                        p.setColor(green);
                        backCanvas.drawRect(lcd_pos_x, lcd_pos_y, lcd_pos_x_end, lcd_pos_y_end, p);
                        Paint keyPaint = new Paint();
                        keyPaint.setFilterBitmap(true);

                        Matrix[] keyMatrix = new Matrix[MAX_TOUCHES];

                        ArrayList<Integer> orderKeys = new ArrayList<Integer>();
                        for (int k = 0; k < keys.length; k++) {
                            orderKeys.add(k);
                        }
                        orderKeys.add(0, orderKeys.remove(30));
                        orderKeys.add(0, orderKeys.remove(31));
                        orderKeys.add(0, orderKeys.remove(32));

                        for (int k : orderKeys) {
                            float key_x = 0f;
                            float key_width = 0f;
                            float key_y = 0f;
                            float key_height = 0f;
                            if (!keybLite || land) {
                                if (k < 6) {
                                    // A, B, C...
                                    key_width = usable_w / 6;
                                    key_height = menu_key_height;
                                    key_x = key_width * k;
                                    key_y = start_h;
                                } else if (k < 24) {
                                    key_width = usable_w / 6;
                                    key_height = regular_key_height;
                                    key_x = key_width * (k % 6);
                                    key_y = start_h + menu_key_height + regular_key_height * ((k / 6) - 1);
                                } else if (!land) {
                                    if (k == 24) {
                                        // ENTER
                                        key_width = usable_w / 3;
                                        key_height = regular_key_height;
                                        key_x = 0;
                                        key_y = start_h + menu_key_height + regular_key_height * 3f;
                                    } else if (k > 24 && k < 29) {
                                        key_width = usable_w / 6;
                                        key_height = regular_key_height;
                                        key_x = key_width * ((k + 1) % 6);
                                        int rank = ((k + 1) / 6);
                                        key_y = start_h + menu_key_height + regular_key_height * (rank - 1);
                                    } else if (k >= 29) {
                                        key_width = usable_w / 5;
                                        key_height = regular_key_height;
                                        key_x = key_width * ((k + 1) % 5);
                                        key_y = start_h + menu_key_height + regular_key_height * (((k + 1) / 5) - 2);
                                    }
                                } else if (land) {
                                    if (k == 24) {
                                        key_width = remaning_w / 2;
                                        key_height = regular_key_height_right;
                                        key_x = w - key_width;
                                        int rank = 7;
                                        key_y = regular_key_height_right * (rank - 1);
                                    } else if (k == 29 || k == 34 || k == 39 || k == 44) {
                                        key_width = remaning_w / 4;
                                        key_height = regular_key_height_right;
                                        int xrank = 4;
                                        if (k == 34)
                                            xrank = 3;
                                        else if (k == 39)
                                            xrank = 2;
                                        else if (k == 44)
                                            xrank = 1;
                                        key_x = w - key_width * xrank;
                                        key_y = 0;
                                    } else {
                                        key_width = remaning_w / 4;
                                        key_height = regular_key_height_right;
                                        int xrank = 0;
                                        int yrank = 0;
                                        if (k == 25) {
                                            xrank = 4;
                                            yrank = 0;
                                        } else if (k == 26) {
                                            xrank = 3;
                                            yrank = 0;
                                        } else if (k == 27) {
                                            xrank = 2;
                                            yrank = 0;
                                        } else if (k == 28) {
                                            xrank = 1;
                                            yrank = 0;
                                        } else if (k == 30) {
                                            xrank = 4;
                                            yrank = 1;
                                        } else if (k == 31) {
                                            xrank = 3;
                                            yrank = 1;
                                        } else if (k == 32) {
                                            xrank = 2;
                                            yrank = 1;
                                        } else if (k == 33) {
                                            xrank = 1;
                                            yrank = 1;
                                        } else if (k == 35) {
                                            xrank = 4;
                                            yrank = 2;
                                        } else if (k == 36) {
                                            xrank = 3;
                                            yrank = 2;
                                        } else if (k == 37) {
                                            xrank = 2;
                                            yrank = 2;
                                        } else if (k == 38) {
                                            xrank = 1;
                                            yrank = 2;
                                        } else if (k == 40) {
                                            xrank = 4;
                                            yrank = 3;
                                        } else if (k == 41) {
                                            xrank = 3;
                                            yrank = 3;
                                        } else if (k == 42) {
                                            xrank = 2;
                                            yrank = 3;
                                        } else if (k == 43) {
                                            xrank = 1;
                                            yrank = 3;
                                        } else if (k == 45) {
                                            xrank = 4;
                                            yrank = 4;
                                        } else if (k == 46) {
                                            xrank = 3;
                                            yrank = 4;
                                        } else if (k == 47) {
                                            xrank = 2;
                                            yrank = 4;
                                        } else if (k == 48) {
                                            xrank = 1;
                                            yrank = 4;
                                        }
                                        key_x = w - key_width * xrank;
                                        key_y = regular_key_height_right * (2 + yrank - 1);
                                    }
                                }
                            } else {
                                if (k == 24) {
                                    key_width = 2f * (float)w / 5f;
                                    key_height = usable_h / 5f;
                                    key_x = 0;
                                    key_y = start_h;
                                } else if (k == 25) {
                                    key_width = w / 5;
                                    key_height = usable_h / 5f;
                                    key_x = 2 * key_width;
                                    key_y = start_h;
                                } else if (k == 26) {
                                    key_width = w / 5;
                                    key_height = usable_h / 5f;
                                    key_x = 3 * key_width;
                                    key_y = start_h;
                                } else if (k == 28) {
                                    key_width = w / 5;
                                    key_height = usable_h / 5f;
                                    key_x = 4 * key_width;
                                    key_y = start_h;
                                } else if (k >= 30 && k <= 48 && k != 34 && k != 39 && k != 44) {
                                    key_width = w / 4;
                                    key_height = usable_h / 5f;
                                    int xrank = 0;
                                    int yrank = 0;
                                    if (k == 30) {
                                        xrank = 4;
                                        yrank = 1;
                                    } else if (k == 31) {
                                        xrank = 3;
                                        yrank = 1;
                                    } else if (k == 32) {
                                        xrank = 2;
                                        yrank = 1;
                                    } else if (k == 33) {
                                        xrank = 1;
                                        yrank = 1;
                                    } else if (k == 35) {
                                        xrank = 4;
                                        yrank = 2;
                                    } else if (k == 36) {
                                        xrank = 3;
                                        yrank = 2;
                                    } else if (k == 37) {
                                        xrank = 2;
                                        yrank = 2;
                                    } else if (k == 38) {
                                        xrank = 1;
                                        yrank = 2;
                                    } else if (k == 40) {
                                        xrank = 4;
                                        yrank = 3;
                                    } else if (k == 41) {
                                        xrank = 3;
                                        yrank = 3;
                                    } else if (k == 42) {
                                        xrank = 2;
                                        yrank = 3;
                                    } else if (k == 43) {
                                        xrank = 1;
                                        yrank = 3;
                                    } else if (k == 45) {
                                        xrank = 4;
                                        yrank = 4;
                                    } else if (k == 46) {
                                        xrank = 3;
                                        yrank = 4;
                                    } else if (k == 47) {
                                        xrank = 2;
                                        yrank = 4;
                                    } else if (k == 48) {
                                        xrank = 1;
                                        yrank = 4;
                                    }
                                    key_x = key_width * (4 - xrank);
                                    key_y = start_h + key_height * (yrank);
                                } else
                                    key_width = 0;
                            }
                            if (key_width == 0) {
                                buttons_coords[k][0] = buttons_coords[k][1] = buttons_coords[k][2] = buttons_coords[k][3] = 0;
                                continue;
                            }
                            buttons_coords[k][0] = (int)key_x;
                            buttons_coords[k][1] = (int)key_y;
                            buttons_coords[k][2] = (int)(key_x + key_width);
                            buttons_coords[k][3] = (int)(key_y + key_height);
                            int bw = keys[k].getWidth();
                            int bh = keys[k].getHeight();
                            int delta_x = 0;
                            int delta_y = 0;
                            float ratio_kx = 0.0f;
                            float ratio_ky = 0.0f;

                            float scaler = 1.0f;
                            // if (k < 29) scaler = 1.0f;

                            delta_x = ((int)key_width - bw) / 2;
                            ratio_kx = scaler * key_width / (float)bw;

                            delta_y = ((int)key_height - bh) / 2;
                            ratio_ky = key_height / (float)bh;

                            if (!keybLite && !land && (k == 30 || k == 31 || k == 32 || k == 35 || k == 36 || k == 37 || k == 40 || k == 41 || k == 42 || k == 39)) {
                                Paint p2 = new Paint();
                                p2.setColor(central);
                                backCanvas.drawRect(buttons_coords[k][0], buttons_coords[k][1], buttons_coords[k][2], buttons_coords[k][3], p2);
                            }
                            // slight off:
                            // czo
                            // buttons_coords[k][1] += bh*9/96; //36
                            // buttons_coords[k][3] += bh*9/96;
                            Matrix matrixKey = new Matrix();
                            if (ratio_kx != 0 && ratio_ky != 0) {
                                matrixKey.preScale(ratio_kx, ratio_ky);
                            }
                            // matrixKey.postTranslate(key_x + delta_x, key_y + delta_y);
                            matrixKey.postTranslate(key_x, key_y);
                            keyMatrix[k] = matrixKey;
                        }
                        for (int k : orderKeys) {
                            if (keyMatrix[k] != null)
                                backCanvas.drawBitmap(keys[k], keyMatrix[k], keyPaint);
                        }
                    }

                    c.drawBitmap(backBuffer, 0, 0, null);
                    if (data != null)
                        mainScreen.copyPixelsFromBuffer(ShortBuffer.wrap(data));
                    c.drawBitmap(mainScreen, matrixScreen, screenPaint);
                    for (int i = 0; i < MAX_TOUCHES; i++) {
                        if (touches[i] != 0) {
                            c.drawRoundRect(new RectF(new Rect(buttons_coords[i][0], buttons_coords[i][1], buttons_coords[i][2], buttons_coords[i][3])), 20f, 20f, paint); // 12f
                        }
                    }

                    for (int i = 0; i < 6; i++) {
                        if (ann[i])
                            c.drawBitmap(annImages[i], icons_coords[i][0], icons_coords[i][1], null);
                    }

                } else {
                    // Dlog.d("null canvas !");
                }
            }
        } finally {
            // do this in a finally so that if an exception is thrown
            // during the above, we don't leave the Surface in an
            // inconsistent state
            if (c != null) {
                mSurfaceHolder.unlockCanvasAndPost(c);
            }
        }
        // Dlog.d("data: " + data.length);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        synchronized (mSurfaceHolder) {
            float x, y;
            int action = event.getAction();
            int actionCode = action & MotionEvent.ACTION_MASK;
            int code = -1;
            int pointerID = 0;

            if (multiTouch) {
                if (actionCode == MotionEvent.ACTION_DOWN || actionCode == MotionEvent.ACTION_UP ||
                    actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP) {
                    pointerID = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                    x = event.getX(pointerID);
                    y = event.getY(pointerID);
                    pointerID = event.getPointerId(pointerID) + 1;
                } else {
                    return false;
                }

                // *_DOWN : lookup by coordinates
                // *_UP : lookup by pointer pressed
                if (actionCode == MotionEvent.ACTION_DOWN || actionCode == MotionEvent.ACTION_POINTER_DOWN) {
                    for (int i = 0; i < MAX_TOUCHES; i++) {
                        if (x >= buttons_coords[i][0] && x < buttons_coords[i][2] && y >= buttons_coords[i][1] && y < buttons_coords[i][3]) {
                            code = i;
                            break;
                        }
                    }
                } else {
                    for (int i = 0; i < MAX_TOUCHES; i++) {
                        if (touches[i] == pointerID)
                            code = i;
                    }
                }
                if (code == -1 && actionCode == MotionEvent.ACTION_DOWN) {
                    /* 2013/10/05 : Modified by Olivier Sirol <czo@free.fr> */
                    x48.Menu();
                    // ((X48) getContext()).changeKeybLite();
                    return false;
                }

                if (code > -1) {
                    key(code, actionCode == MotionEvent.ACTION_DOWN || actionCode == MotionEvent.ACTION_POINTER_DOWN, pointerID);
                    return true;
                }
            } else {
                // old code used before the 1.29 version:
                x = event.getX();
                y = event.getY();

                if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_UP)
                    return false;

                for (int i = 0; i < MAX_TOUCHES; i++) {
                    if (x >= buttons_coords[i][0] && x < buttons_coords[i][2] && y >= buttons_coords[i][1] && y < buttons_coords[i][3]) {
                        code = i;
                        break;
                    }
                }
                if (code == -1 && action == MotionEvent.ACTION_DOWN) {
                    x48.Menu();
                    // ((X48) getContext()).changeKeybLite();
                    return true;
                }

                if (code > -1) {
                    key(code, action == MotionEvent.ACTION_DOWN);
                    return action == MotionEvent.ACTION_DOWN;
                }
            }
        }
        return false;
    }

    private boolean keybLite = false;

    public boolean isKeybLite() {
        return keybLite;
    }

    public void setKeybLite(boolean keybLite) {
        this.keybLite = keybLite;
    }

    private boolean sound = false;

    public boolean isSound() {
        return sound;
    }

    public void setSound(boolean sound) {
        this.sound = sound;
    }

    public void key(int code, boolean down) {
        key(code, down, 255); // Use pointerID 255 for keyboard
    }

    public synchronized void key(int code, boolean down, int pointerID) {

        if (code < MAX_TOUCHES) {
            if (down) {
                if (!multiTouch) {
                    for (int i = 0; i < MAX_TOUCHES; i++) {
                        if (touches[i] != 0) {
                            Dlog.d("no multitouch !, force up of " + i);
                            queuedCodes.add(i + 100);
                            touches[i] = 0;
                            break;
                        }
                    }
                }
                Integer cI = code + 1;
                if (!queuedCodes.contains(cI)) {
                    queuedCodes.add(cI);
                    touches[code] = pointerID;
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                } else {
                    Dlog.d("rejected down");
                }
            } else {
                Integer cI = code + 100;
                if (!queuedCodes.contains(cI) && touches[code] != 0) {
                    queuedCodes.add(cI);
                    touches[code] = 0;
                } else {
                    Dlog.d("rejected up");
                    if (!multiTouch) {
                        for (int i = 0; i < MAX_TOUCHES; i++) {
                            if (touches[i] != 0) {
                                Dlog.d("forced up of " + i);
                                queuedCodes.add(i + 100);
                                touches[i] = 0;
                            }
                        }
                    }
                }
                // Dlog.d("key up code: " + code);
                x48.hideActionBar();
            }
            x48.flipScreen();
            this.notify();
        }
    }

    public synchronized void pauseEvent() {
        // Dlog.d("pauseEvent begin");
        try {
            this.wait();
        } catch (InterruptedException e) {
            Dlog.e("pauseEvent: " + e.getMessage());
        }
        // Dlog.d("pauseEvent end");
    }

    public synchronized void unpauseEvent() {
        // Dlog.d("unpauseEvent");
        this.notify();
    }

    public synchronized int waitEvent() {
        if (queuedCodes.size() == 0) {
            return 0;
        } else {
            int c = queuedCodes.remove(0);
            return c;
        }
    }

    protected int width;
    protected int height;

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Dlog.d("surfaceChanged w: " + width + ", h: " + height);
        this.width = width;
        this.height = height;
        currentOrientation = Configuration.ORIENTATION_PORTRAIT;
        // if (width < height)
        // currentOrientation = Configuration.ORIENTATION_PORTRAIT;
        // else
        // currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
        // initGraphicsElements();
        backBuffer = null;
        x48.flipScreen();
    }

    public void emulatorReady() {
        resume();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Dlog.d("Surface created");

        surfaceValid = true;
        drawThread = new Thread(this);
        drawThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Dlog.d("Surface destroyed");
        surfaceValid = false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (hasWindowFocus) {
            // x48.checkfullscreen();
            // mRun = false;
        }
    }

    public void refreshIcons(boolean[] ann) {
        this.ann = ann;
    }

    private boolean pause;

    public void pause() {
        pause = true;
    }

    public void resume() {
        pause = false;
    }

    @Override
    public void run() {
        // Dlog.d("drawing thread started");
        x48.flipScreen();
        while (surfaceValid) {
            if (needFlip || x48.fillScreenData(buf, ann) == 1) {
                needFlip = false;
                refreshMainScreen(buf);
            }
            do {
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    // Dlog.e("run: " + e.getMessage());
                }
            } while (pause && surfaceValid);
        }
        // Dlog.d("drawing thread stopped");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Dlog.d("-->"+keyCode);
        return actionKey(true, keyCode);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Dlog.d("--<"+keyCode);
        return actionKey(false, keyCode);
    }

    public void pressON() {
        key(44, true);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Dlog.e("pressON: " + e.getMessage());
        }
        key(44, false);
    }

    private boolean actionKey(boolean d, int code) {
        switch (code) {
        case KeyEvent.KEYCODE_BACK: {
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(x48);
            String bString = mPrefs.getString("backkey", "0");
            if (bString.equals("0"))
                return false;
            else if (bString.equals("1")) {
                key(28, d);
                return true;
            } else if (bString.equals("2")) {
                key(44, d);
                return true;
            }
        }

        case KeyEvent.KEYCODE_0:
            key(45, d);
            return true;
        case KeyEvent.KEYCODE_1:
            key(40, d);
            return true;
        case KeyEvent.KEYCODE_2:
            key(41, d);
            return true;
        case KeyEvent.KEYCODE_3:
            key(42, d);
            return true;
        case KeyEvent.KEYCODE_4:
            key(35, d);
            return true;
        case KeyEvent.KEYCODE_5:
            key(36, d);
            return true;
        case KeyEvent.KEYCODE_6:
            key(37, d);
            return true;
        case KeyEvent.KEYCODE_7:
            key(30, d);
            return true;
        case KeyEvent.KEYCODE_8:
            key(31, d);
            return true;
        case KeyEvent.KEYCODE_9:
            key(32, d);
            return true;
        case KeyEvent.KEYCODE_ENTER:
            key(24, d);
            return true;
        case KeyEvent.KEYCODE_DEL:
            key(28, d);
            return true;
        case KeyEvent.KEYCODE_PERIOD:
            key(46, d);
            return true;
        case KeyEvent.KEYCODE_AT:
            key(29, d);
            return true;
        case KeyEvent.KEYCODE_A:
            key(0, d);
            return true;
        case KeyEvent.KEYCODE_B:
            key(1, d);
            return true;
        case KeyEvent.KEYCODE_C:
            key(2, d);
            return true;
        case KeyEvent.KEYCODE_D:
            key(3, d);
            return true;
        case KeyEvent.KEYCODE_E:
            key(4, d);
            return true;
        case KeyEvent.KEYCODE_F:
            key(5, d);
            return true;
        case KeyEvent.KEYCODE_G:
            key(6, d);
            return true;
        case KeyEvent.KEYCODE_H:
            key(7, d);
            return true;
        case KeyEvent.KEYCODE_I:
            key(8, d);
            return true;
        case KeyEvent.KEYCODE_J:
            key(9, d);
            return true;
        case KeyEvent.KEYCODE_K:
            key(10, d);
            return true;
        case KeyEvent.KEYCODE_L:
            key(11, d);
            return true;
        case KeyEvent.KEYCODE_M:
            key(12, d);
            return true;
        case KeyEvent.KEYCODE_N:
            key(13, d);
            return true;
        case KeyEvent.KEYCODE_O:
            key(14, d);
            return true;
        case KeyEvent.KEYCODE_P:
            key(15, d);
            return true;
        case KeyEvent.KEYCODE_Q:
            key(16, d);
            return true;
        case KeyEvent.KEYCODE_R:
            key(17, d);
            return true;
        case KeyEvent.KEYCODE_S:
            key(18, d);
            return true;
        case KeyEvent.KEYCODE_T:
            key(19, d);
            return true;
        case KeyEvent.KEYCODE_U:
            key(20, d);
            return true;
        case KeyEvent.KEYCODE_V:
            key(21, d);
            return true;
        case KeyEvent.KEYCODE_W:
            key(22, d);
            return true;
        case KeyEvent.KEYCODE_X:
            key(23, d);
            return true;
        case KeyEvent.KEYCODE_Y:
            key(25, d);
            return true;
        case KeyEvent.KEYCODE_Z:
            key(26, d);
            return true;
        case KeyEvent.KEYCODE_SPACE:
            key(47, d);
            return true;

            // case KeyEvent.KEYCODE_SHIFT_LEFT: if (d) key(34, true); else key(34, false);
            // return true;
            // case KeyEvent.KEYCODE_SHIFT_RIGHT: if (d) key(39, true); else key(39, false);
            // return true;

        case KeyEvent.KEYCODE_DPAD_UP:
            key(10, d);
            return true;
        case KeyEvent.KEYCODE_DPAD_DOWN:
            key(16, d);
            return true;
        case KeyEvent.KEYCODE_DPAD_LEFT:
            key(15, d);
            return true;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            key(17, d);
            return true;
        case KeyEvent.KEYCODE_DPAD_CENTER:
            key(24, d);
            return true;

        default:
            return false;
        }
    }
}
