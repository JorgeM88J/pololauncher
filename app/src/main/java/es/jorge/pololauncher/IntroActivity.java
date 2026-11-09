package es.jorge.pololauncher;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

/**
 * Vídeo de bienvenida a pantalla completa (res/raw/intro.mp4). Se salta tocando la pantalla.
 * Si el vídeo falla o tarda demasiado, se cierra solo para no dejar la unidad colgada.
 */
public class IntroActivity extends Activity implements SurfaceHolder.Callback {

    static final long MAX_MS = 25000;

    final Handler handler = new Handler(Looper.getMainLooper());
    MediaPlayer mp;
    boolean done = false;

    final Runnable watchdog = new Runnable() {
        @Override
        public void run() { finishIntro(); }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setBackgroundDrawable(new ColorDrawable(0xFF000000));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        SurfaceView sv = new SurfaceView(this);
        sv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finishIntro(); }
        });
        sv.getHolder().addCallback(this);
        setContentView(sv);
        handler.postDelayed(watchdog, MAX_MS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (done || mp != null) return;
        int id = getResources().getIdentifier("intro", "raw", getPackageName());
        if (id == 0) {
            finishIntro();
            return;
        }
        try {
            mp = MediaPlayer.create(this, id);
            if (mp == null) {
                finishIntro();
                return;
            }
            mp.setDisplay(holder);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer m) { finishIntro(); }
            });
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer m, int what, int extra) {
                    finishIntro();
                    return true;
                }
            });
            mp.start();
        } catch (Exception e) {
            finishIntro();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { }

    void finishIntro() {
        if (done) return;
        done = true;
        handler.removeCallbacks(watchdog);
        release();
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    void release() {
        if (mp != null) {
            try {
                mp.release();
            } catch (Exception e) {
                // nada
            }
            mp = null;
        }
    }

    @Override
    public void onBackPressed() { finishIntro(); }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(watchdog);
        release();
        super.onDestroy();
    }
}
