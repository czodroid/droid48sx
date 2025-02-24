package org.czo.droid48sx;

import android.content.Context;

public class EmulatorThread extends Thread {

    private final X48 x48;

    public EmulatorThread(Context x48) {
        this.x48 = (X48)x48;
    }

    public void run() {
        x48.registerClass(x48);
        x48.startHPEmulator();
    }
}
