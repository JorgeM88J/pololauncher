package es.jorge.pololauncher;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Servicio en primer plano que:
 * 1) suma los km recorridos con el GPS aunque la pantalla muestre CarPlay u otra app;
 * 2) pone la intro del Polo si la pantalla ha estado apagada (contacto quitado) más de 3 minutos.
 * No usa wake locks: con la unidad dormida no hace nada.
 */
public class KmService extends Service {

    static final String CH = "km";
    static final int NOTIF_ID = 1;
    /** Tiempo mínimo con la pantalla apagada para volver a poner la intro. */
    static final long INTRO_AFTER_MS = 3 * 60 * 1000;

    static final float MAX_ACCURACY_M = 30f;     // fixes peores se ignoran
    static final float MIN_SPEED_MS = 1.0f;      // parado (< 3,6 km/h) no suma: evita el "baile" del GPS
    static final float MAX_SPEED_MS = 70f;       // > 250 km/h = salto falso
    static final long MAX_GAP_MS = 5 * 60 * 1000; // tras más de 5 min sin señal se empieza de cero
    static final double FLUSH_M = 100;           // se guarda cada 100 m

    Location prev;
    double pendingM = 0;

    public static void start(Context c) {
        if (c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        Intent i = new Intent(c, KmService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
            else c.startService(i);
        } catch (Exception e) {
            // el sistema no deja arrancarlo ahora; se reintenta la próxima vez que se vea el launcher
        }
    }

    final LocationListener ll = new LocationListener() {
        @Override
        public void onLocationChanged(Location l) { onFix(l); }

        @Override
        public void onProviderEnabled(String p) { }

        @Override
        public void onProviderDisabled(String p) { }

        @Override
        public void onStatusChanged(String p, int st, Bundle b) { }
    };

    final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_SCREEN_OFF.equals(i.getAction())) onScreenOff();
            else if (Intent.ACTION_SCREEN_ON.equals(i.getAction())) onScreenOn();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIF_ID, buildNotification());
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screen, f);
        startGps();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        flush();
        try {
            unregisterReceiver(screen);
        } catch (Exception e) {
            // nada
        }
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm != null) lm.removeUpdates(ll);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    Notification buildNotification() {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel ch = new NotificationChannel(CH, "Cuentakilómetros GPS", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            if (nm != null) nm.createNotificationChannel(ch);
            b = new Notification.Builder(this, CH);
        } else {
            b = new Notification.Builder(this);
            b.setPriority(Notification.PRIORITY_MIN);
        }
        return b.setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Polo Launcher")
                .setContentText("Contando km con el GPS")
                .setOngoing(true)
                .build();
    }

    // ------------------------------------------------------------------ Km por GPS

    void startGps() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) return;
        try {
            if (lm.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, ll, Looper.getMainLooper());
            }
        } catch (SecurityException e) {
            stopSelf();
        }
    }

    void onFix(Location l) {
        if (!l.hasAccuracy() || l.getAccuracy() > MAX_ACCURACY_M) return;
        if (prev == null) {
            prev = l;
            return;
        }
        long dt = l.getElapsedRealtimeNanos() / 1000000L - prev.getElapsedRealtimeNanos() / 1000000L;
        if (dt <= 0) return;
        if (dt > MAX_GAP_MS) {
            prev = l;
            return;
        }
        float d = prev.distanceTo(l);
        float v = d / (dt / 1000f);
        if (v > MAX_SPEED_MS) {
            prev = l; // salto raro: no se suma
            return;
        }
        boolean moving = l.hasSpeed() ? l.getSpeed() >= MIN_SPEED_MS : v >= MIN_SPEED_MS;
        if (!moving) {
            // parado: no se mueve el punto de referencia para no acumular el ruido
            return;
        }
        pendingM += d;
        prev = l;
        if (pendingM >= FLUSH_M) flush();
    }

    void flush() {
        if (pendingM <= 0) return;
        Store.addMeters(this, pendingM);
        pendingM = 0;
    }

    // ------------------------------------------------------------------ Intro al volver a encender

    void onScreenOff() {
        flush();
        prev = null;
        Store.sp(this).edit()
                .putLong("offWall", System.currentTimeMillis())
                .putLong("offElapsed", SystemClock.elapsedRealtime())
                .apply();
    }

    void onScreenOn() {
        if (offLongEnough(this)) playIntro(this);
    }

    /** True si la pantalla estuvo apagada más de 3 min. Borra la marca para que no se repita. */
    static boolean offLongEnough(Context c) {
        SharedPreferences sp = Store.sp(c);
        long wall = sp.getLong("offWall", 0);
        if (wall == 0) return false;
        long el = sp.getLong("offElapsed", 0);
        sp.edit().remove("offWall").remove("offElapsed").apply();
        long now = SystemClock.elapsedRealtime();
        // Mismo arranque: el reloj "elapsed" cuenta también el tiempo dormido y no lo cambia el GPS.
        long off = (now >= el) ? now - el : System.currentTimeMillis() - wall;
        return off > INTRO_AFTER_MS;
    }

    /** Intro pendiente de hace menos de 1 minuto (si es más antigua ya no tiene sentido ponerla). */
    static boolean takePendingIntro(Context c) {
        SharedPreferences sp = Store.sp(c);
        long at = sp.getLong("introPendingAt", 0);
        if (at == 0) return false;
        sp.edit().remove("introPendingAt").apply();
        long age = SystemClock.elapsedRealtime() - at;
        return age >= 0 && age < 60000;
    }

    static void clearPendingIntro(Context c) {
        Store.sp(c).edit().remove("introPendingAt").apply();
    }

    static void playIntro(Context c) {
        if (c.getResources().getIdentifier("intro", "raw", c.getPackageName()) == 0) return;
        // Por si Android no deja abrirla desde aquí: el launcher la pone al volver a verse.
        Store.sp(c).edit().putLong("introPendingAt", SystemClock.elapsedRealtime()).apply();
        try {
            Intent i = new Intent(c, IntroActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) {
            // queda pendiente
        }
    }
}
