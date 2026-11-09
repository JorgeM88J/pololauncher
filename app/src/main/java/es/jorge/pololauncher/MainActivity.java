package es.jorge.pololauncher;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    static final String[] LABELS = {"CarPlay", "Música", "Radio FM", "Repostaje", "Ajustes"};
    static final int SLOTS = 5;
    /** Este botón no abre otra app: abre el registro de repostajes de esta misma app. */
    static final int FUEL = 3;

    final Handler handler = new Handler(Looper.getMainLooper());
    final SimpleDateFormat fH = new SimpleDateFormat("HH", Locale.US);
    final SimpleDateFormat fM = new SimpleDateFormat("mm", Locale.US);
    final SimpleDateFormat fDate = new SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("es", "ES"));

    TextView clock, dateTv, sunTv, speedTv, dirTv, altTv, gpsTv;
    LinearLayout remList;
    final LinearLayout[] btn = new LinearLayout[SLOTS];
    final ImageView[] btnIcon = new ImageView[SLOTS];
    final TextView[] btnHint = new TextView[SLOTS];

    boolean gpsOk = false;          // ya ha habido un fix: la hora se corrige con la del GPS
    long gpsOffset = 0;
    long lastFixElapsed = 0;
    String lastDir = "--";
    String lastAlt = "-- m";
    double lat = 40.4168, lon = -3.7038;

    final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            handler.postDelayed(this, 1000);
        }
    };

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

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);
        fH.setTimeZone(Store.TZ);
        fM.setTimeZone(Store.TZ);
        fDate.setTimeZone(Store.TZ);
        getWindow().setBackgroundDrawable(new ColorDrawable(Ui.BG));

        SharedPreferences sp = Store.sp(this);
        lat = Double.longBitsToDouble(sp.getLong("lat", Double.doubleToLongBits(lat)));
        lon = Double.longBitsToDouble(sp.getLong("lon", Double.doubleToLongBits(lon)));

        buildUi();
        maybePlayIntro();

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    /** El vídeo de bienvenida suena una vez por arranque de la unidad, y solo si existe res/raw/intro.mp4. */
    void maybePlayIntro() {
        if (getResources().getIdentifier("intro", "raw", getPackageName()) == 0) return;
        long boot = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        SharedPreferences sp = Store.sp(this);
        long saved = sp.getLong("introBoot", 0);
        if (Math.abs(boot - saved) > 120000) {
            sp.edit().putLong("introBoot", boot).apply();
            startActivity(new Intent(this, IntroActivity.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startGps();
        refreshButtons();
        refreshReminders();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
        stopGps();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        startGps();
    }

    @Override
    public void onBackPressed() {
        // Un launcher no se cierra con "atrás".
    }

    // ------------------------------------------------------------------ UI

    void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.px(44), Ui.px(30), Ui.px(44), Ui.px(30));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(top, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Columna izquierda: hora, fecha, sol
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(left, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.1f));

        clock = Ui.tv(this, "--:--", 184, Ui.TXT, true);
        left.addView(clock, Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        dateTv = Ui.tv(this, "", 32, Ui.GRAY, false);
        left.addView(dateTv, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4, 8, 0, 0));
        sunTv = Ui.tv(this, "", 24, Ui.GRAY, false);
        left.addView(sunTv, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4, 18, 0, 0));

        // Columna derecha: velocidad y recordatorios
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        top.addView(right, Ui.lpm(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f, 28, 0, 0, 0));

        LinearLayout speedCard = new LinearLayout(this);
        speedCard.setOrientation(LinearLayout.HORIZONTAL);
        speedCard.setGravity(Gravity.CENTER_VERTICAL);
        speedCard.setBackground(Ui.round(Ui.CARD, 20));
        speedCard.setPadding(Ui.px(28), Ui.px(20), Ui.px(28), Ui.px(20));
        right.addView(speedCard, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));

        LinearLayout speedBox = new LinearLayout(this);
        speedBox.setOrientation(LinearLayout.HORIZONTAL);
        speedBox.setGravity(Gravity.BOTTOM);
        speedTv = Ui.tv(this, "--", 88, Ui.TXT, true);
        speedBox.addView(speedTv);
        TextView unit = Ui.tv(this, "km/h", 26, Ui.GRAY, false);
        speedBox.addView(unit, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 10, 0, 0, 12));
        speedCard.addView(speedBox, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(Gravity.END);
        dirTv = Ui.tv(this, "Rumbo --", 22, Ui.GRAY, false);
        altTv = Ui.tv(this, "Altitud -- m", 22, Ui.GRAY, false);
        gpsTv = Ui.tv(this, "Buscando GPS…", 18, Ui.GRAY, false);
        info.addView(dirTv);
        info.addView(altTv, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 8, 0, 0));
        info.addView(gpsTv, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 8, 0, 0));
        speedCard.addView(info);

        LinearLayout remCard = new LinearLayout(this);
        remCard.setOrientation(LinearLayout.VERTICAL);
        remCard.setBackground(Ui.round(Ui.CARD, 20));
        remCard.setPadding(Ui.px(28), Ui.px(20), Ui.px(28), Ui.px(16));
        remCard.setClickable(true);
        remCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RemindersActivity.class));
            }
        });
        right.addView(remCard, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f, 0, 16, 0, 0));

        LinearLayout remHead = new LinearLayout(this);
        remHead.setOrientation(LinearLayout.HORIZONTAL);
        TextView remTitle = Ui.tv(this, "RECORDATORIOS", 18, Ui.GRAY, true);
        remHead.addView(remTitle, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        remHead.addView(Ui.tv(this, "toca para editar", 15, Ui.GRAY, false));
        remCard.addView(remHead, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 0, 12));

        remList = new LinearLayout(this);
        remList.setOrientation(LinearLayout.VERTICAL);
        remCard.addView(remList, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));

        // Fila de botones
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(bottom, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, Ui.px(124), 0, 0, 24, 0, 0));

        for (int i = 0; i < SLOTS; i++) {
            final int slot = i;
            boolean primary = (i == 0);
            LinearLayout b = new LinearLayout(this);
            b.setOrientation(LinearLayout.VERTICAL);
            b.setGravity(Gravity.CENTER);
            b.setBackground(Ui.round(primary ? Ui.ACC : Ui.CARD, 20));
            b.setClickable(true);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { onSlotClick(slot); }
            });
            b.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    pickApp(slot, false);
                    return true;
                }
            });

            ImageView icon = new ImageView(this);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            b.addView(icon, Ui.lp(Ui.px(52), Ui.px(52), 0));
            TextView label = Ui.tv(this, LABELS[i], 28, primary ? Ui.DARK : Ui.TXT, true);
            b.addView(label, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 4, 0, 0));
            TextView hint = Ui.tv(this, "", 14, primary ? Ui.DARK : Ui.GRAY, false);
            b.addView(hint);

            btn[i] = b;
            btnIcon[i] = icon;
            btnHint[i] = hint;
            bottom.addView(b, Ui.lpm(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f, i == 0 ? 0 : 8, 0, i == SLOTS - 1 ? 0 : 8, 0));
        }

        setContentView(root);
    }

    // ------------------------------------------------------------------ Reloj, sol y GPS

    void updateClock() {
        long now = System.currentTimeMillis() + (gpsOk ? gpsOffset : 0);
        String hh = fH.format(now);
        String mm = fM.format(now);
        SpannableStringBuilder sb = new SpannableStringBuilder(hh + ":" + mm);
        sb.setSpan(new ForegroundColorSpan(Ui.ACC), hh.length(), hh.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        clock.setText(sb);
        dateTv.setText(fDate.format(now));

        Calendar c = Calendar.getInstance(Store.TZ);
        c.setTimeInMillis(now);
        String[] st = Sun.times(c, lat, lon);
        sunTv.setText("Amanece " + st[0] + "     Anochece " + st[1]);

        boolean fresh = gpsOk && (SystemClock.elapsedRealtime() - lastFixElapsed) < 5000;
        if (!fresh) {
            speedTv.setText("--");
            dirTv.setText("Rumbo --");
            gpsTv.setText(gpsOk ? "Sin señal GPS" : "Buscando GPS…");
        }
    }

    void startGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            gpsTv.setText("Falta permiso de ubicación");
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) return;
        try {
            if (lm.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, ll);
                Location last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (last != null) {
                    lat = last.getLatitude();
                    lon = last.getLongitude();
                }
            } else {
                gpsTv.setText("Esta unidad no tiene GPS");
            }
        } catch (SecurityException e) {
            gpsTv.setText("Falta permiso de ubicación");
        }
    }

    void stopGps() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm != null) lm.removeUpdates(ll);
    }

    static String compass(float bearing) {
        String[] n = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
        int i = Math.round(bearing / 45f) % 8;
        if (i < 0) i += 8;
        return n[i];
    }

    void onFix(Location l) {
        gpsOk = true;
        lastFixElapsed = SystemClock.elapsedRealtime();
        gpsOffset = l.getTime() - System.currentTimeMillis();
        lat = l.getLatitude();
        lon = l.getLongitude();
        Store.sp(this).edit()
                .putLong("lat", Double.doubleToLongBits(lat))
                .putLong("lon", Double.doubleToLongBits(lon))
                .apply();

        float kmh = l.hasSpeed() ? l.getSpeed() * 3.6f : 0f;
        if (kmh < 2f) kmh = 0f;
        speedTv.setText(l.hasSpeed() ? String.valueOf(Math.round(kmh)) : "--");
        if (l.hasBearing() && kmh > 5f) lastDir = compass(l.getBearing());
        dirTv.setText("Rumbo " + lastDir);
        if (l.hasAltitude()) lastAlt = Math.round(l.getAltitude()) + " m";
        altTv.setText("Altitud " + lastAlt);
        gpsTv.setText("GPS ok");
    }

    // ------------------------------------------------------------------ Recordatorios

    void refreshReminders() {
        remList.removeAllViews();
        final int km = Store.getKm(this);
        List<Store.Item> items = Store.load(this);
        List<Store.Item> due = new ArrayList<>();
        for (Store.Item it : items) {
            if (Store.urgency(it, km) != Double.POSITIVE_INFINITY) due.add(it);
        }
        Collections.sort(due, new Comparator<Store.Item>() {
            @Override
            public int compare(Store.Item a, Store.Item b) {
                return Double.compare(Store.urgency(a, km), Store.urgency(b, km));
            }
        });
        int n = 0;
        for (Store.Item it : due) {
            if (n >= 4) break;
            double u = Store.urgency(it, km);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            TextView name = Ui.tv(this, it.name, 26, Ui.TXT, false);
            row.addView(name, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(Ui.tv(this, Store.shortStatus(it, km), 26, Store.color(u), u <= 90));
            remList.addView(row, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 0, 14));
            n++;
        }
        if (n == 0) {
            TextView hint = Ui.tv(this, "Aún sin fechas.\nToca aquí para poner ITV, seguro, aceite, distribución…", 24, Ui.GRAY, false);
            remList.addView(hint);
        }
    }

    // ------------------------------------------------------------------ Botones

    List<ResolveInfo> launchables() {
        PackageManager pm = getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> all = pm.queryIntentActivities(i, 0);
        List<ResolveInfo> out = new ArrayList<>();
        for (ResolveInfo r : all) {
            if (!r.activityInfo.packageName.equals(getPackageName())) out.add(r);
        }
        return out;
    }

    String labelOf(ResolveInfo r) {
        CharSequence c = r.loadLabel(getPackageManager());
        return c == null ? "" : c.toString();
    }

    /** Busca la primera app cuyo nombre contenga alguno de los textos, en orden de preferencia. */
    String findByLabel(String... words) {
        List<ResolveInfo> apps = launchables();
        for (String w : words) {
            for (ResolveInfo r : apps) {
                if (labelOf(r).toLowerCase(Locale.ROOT).contains(w)) return r.activityInfo.packageName;
            }
        }
        for (String w : words) {
            for (ResolveInfo r : apps) {
                if (r.activityInfo.packageName.toLowerCase(Locale.ROOT).contains(w)) return r.activityInfo.packageName;
            }
        }
        return null;
    }

    String guess(int slot) {
        switch (slot) {
            case 0:
                return findByLabel("zlink", "carplay", "carlink", "car play");
            case 1:
                return findByLabel("bluetooth music", "bt music", "btmusic", "música bluetooth", "bluetooth", "música", "music");
            case 2:
                return findByLabel("radio", "fm");
            case 3:
                return null; // Repostaje: es de esta app, no abre otra
            default:
                PackageManager pm = getPackageManager();
                if (pm.getLaunchIntentForPackage("com.android.settings") != null) return "com.android.settings";
                return findByLabel("ajustes", "settings");
        }
    }

    String resolveSlot(int slot) {
        PackageManager pm = getPackageManager();
        String p = Store.getSlot(this, slot);
        if (p != null && pm.getLaunchIntentForPackage(p) != null) return p;
        String g = guess(slot);
        if (g != null) Store.setSlot(this, slot, g);
        return g;
    }

    void onSlotClick(int slot) {
        if (slot == FUEL) {
            startActivity(new Intent(this, FuelActivity.class));
            return;
        }
        String pkg = resolveSlot(slot);
        if (pkg == null) {
            pickApp(slot, true);
        } else {
            launch(pkg);
        }
        refreshButtons();
    }

    void launch(String pkg) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) {
            Toast.makeText(this, "No se puede abrir esa app", Toast.LENGTH_SHORT).show();
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    void refreshButtons() {
        PackageManager pm = getPackageManager();
        for (int i = 0; i < SLOTS; i++) {
            if (i == FUEL) {
                btnIcon[i].setImageDrawable(getDrawable(R.drawable.ic_fuel));
                btnIcon[i].setVisibility(View.VISIBLE);
                btnHint[i].setVisibility(View.GONE);
                continue;
            }
            String p = Store.getSlot(this, i);
            Drawable icon = null;
            if (p != null) {
                try {
                    icon = pm.getApplicationIcon(p);
                } catch (PackageManager.NameNotFoundException e) {
                    icon = null;
                }
            }
            if (icon == null) {
                // Aún sin app asignada: se intenta adivinar sin guardar; si no hay, se pedirá al pulsar.
                String g = guess(i);
                if (g != null) {
                    Store.setSlot(this, i, g);
                    try {
                        icon = pm.getApplicationIcon(g);
                    } catch (PackageManager.NameNotFoundException e) {
                        icon = null;
                    }
                }
            }
            btnIcon[i].setImageDrawable(icon);
            btnIcon[i].setVisibility(icon == null ? View.GONE : View.VISIBLE);
            btnHint[i].setText(icon == null ? "toca para elegir app" : "");
            btnHint[i].setVisibility(icon == null ? View.VISIBLE : View.GONE);
        }
    }

    void pickApp(final int slot, final boolean launchAfter) {
        if (slot == FUEL) {
            Toast.makeText(this, "Este botón es el registro de repostajes y no se cambia", Toast.LENGTH_SHORT).show();
            return;
        }
        final PackageManager pm = getPackageManager();
        final List<ResolveInfo> apps = launchables();
        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                return labelOf(a).compareToIgnoreCase(labelOf(b));
            }
        });

        ListView lv = new ListView(this);
        lv.setBackgroundColor(Ui.CARD);
        lv.setAdapter(new ArrayAdapter<ResolveInfo>(this, 0, apps) {
            @Override
            public View getView(int pos, View convert, ViewGroup parent) {
                TextView t = (convert instanceof TextView) ? (TextView) convert : new TextView(MainActivity.this);
                ResolveInfo r = getItem(pos);
                t.setText(labelOf(r));
                t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(26));
                t.setTextColor(Ui.TXT);
                t.setGravity(Gravity.CENTER_VERTICAL);
                t.setPadding(Ui.px(12), Ui.px(10), Ui.px(12), Ui.px(10));
                Drawable d = r.loadIcon(pm);
                d.setBounds(0, 0, Ui.px(48), Ui.px(48));
                t.setCompoundDrawables(d, null, null, null);
                t.setCompoundDrawablePadding(Ui.px(18));
                return t;
            }
        });

        final Dialog dlg = Ui.dialog(this, "Elige la app para «" + LABELS[slot] + "»", lv, Ui.px(620));
        lv.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                String pkg = apps.get(pos).activityInfo.packageName;
                Store.setSlot(MainActivity.this, slot, pkg);
                dlg.dismiss();
                refreshButtons();
                if (launchAfter) launch(pkg);
            }
        });
        dlg.show();
        Toast.makeText(this, "Mantén pulsado un botón para cambiar su app", Toast.LENGTH_SHORT).show();
    }
}
