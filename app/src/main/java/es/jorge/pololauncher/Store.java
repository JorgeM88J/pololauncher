package es.jorge.pololauncher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Datos guardados en la unidad: recordatorios, km actuales y app asignada a cada botón. */
public class Store {
    public static final TimeZone TZ = TimeZone.getTimeZone("Europe/Madrid");
    /** Para convertir km restantes en "días" y poder ordenar por urgencia (unos 20.000 km/año). */
    static final double KM_PER_DAY = 55.0;

    public static class Item {
        public String name = "";
        public int everyMonths;   // 0 = no se repite por tiempo
        public int everyKm;       // 0 = no se repite por km
        public long nextDate;     // 0 = sin fecha
        public int nextKm;        // 0 = sin km
    }

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("polo", Context.MODE_PRIVATE);
    }

    // ---------- Recordatorios ----------

    static void add(List<Item> l, String name, int months, int km) {
        Item it = new Item();
        it.name = name;
        it.everyMonths = months;
        it.everyKm = km;
        l.add(it);
    }

    /** Plantillas de serie. Los intervalos son orientativos: se pueden cambiar. Sin fechas hasta que las pongas. */
    static List<Item> defaults() {
        List<Item> l = new ArrayList<>();
        add(l, "ITV", 12, 0);
        add(l, "Seguro", 12, 0);
        add(l, "Impuesto de circulación", 12, 0);
        add(l, "Aceite y filtro", 12, 10000);
        add(l, "Distribución (correa)", 60, 90000);
        add(l, "Filtro de aire", 24, 30000);
        add(l, "Bujías", 48, 60000);
        add(l, "Líquido de frenos", 24, 0);
        add(l, "Refrigerante", 60, 0);
        add(l, "Revisión de frenos", 12, 20000);
        add(l, "Batería (revisar)", 12, 0);
        return l;
    }

    public static List<Item> load(Context c) {
        String j = sp(c).getString("reminders", null);
        if (j == null) {
            List<Item> d = defaults();
            save(c, d);
            return d;
        }
        List<Item> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(j);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Item it = new Item();
                it.name = o.optString("name", "");
                it.everyMonths = o.optInt("everyMonths", 0);
                it.everyKm = o.optInt("everyKm", 0);
                it.nextDate = o.optLong("nextDate", 0);
                it.nextKm = o.optInt("nextKm", 0);
                out.add(it);
            }
        } catch (JSONException e) {
            // datos corruptos: se devuelve lo que se haya podido leer
        }
        return out;
    }

    public static void save(Context c, List<Item> l) {
        JSONArray a = new JSONArray();
        try {
            for (Item it : l) {
                JSONObject o = new JSONObject();
                o.put("name", it.name);
                o.put("everyMonths", it.everyMonths);
                o.put("everyKm", it.everyKm);
                o.put("nextDate", it.nextDate);
                o.put("nextKm", it.nextKm);
                a.put(o);
            }
        } catch (JSONException e) {
            return;
        }
        sp(c).edit().putString("reminders", a.toString()).apply();
    }

    // ---------- Km y botones ----------

    public static int getKm(Context c) { return sp(c).getInt("km", 0); }

    public static void setKm(Context c, int km) { sp(c).edit().putInt("km", km).apply(); }

    public static String getSlot(Context c, int i) { return sp(c).getString("slot" + i, null); }

    public static void setSlot(Context c, int i, String pkg) { sp(c).edit().putString("slot" + i, pkg).apply(); }

    // ---------- Repostajes ----------

    public static class Fill {
        public long ts;
        public int km;
        public double liters;
        public double euros;
    }

    /** Devuelve los repostajes ordenados por km de menor a mayor. */
    public static List<Fill> loadFuel(Context c) {
        List<Fill> out = new ArrayList<>();
        String j = sp(c).getString("fuel", null);
        if (j != null) {
            try {
                JSONArray a = new JSONArray(j);
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    Fill f = new Fill();
                    f.ts = o.optLong("ts", 0);
                    f.km = o.optInt("km", 0);
                    f.liters = o.optDouble("liters", 0);
                    f.euros = o.optDouble("euros", 0);
                    out.add(f);
                }
            } catch (JSONException e) {
                // se devuelve lo que se haya podido leer
            }
        }
        java.util.Collections.sort(out, new java.util.Comparator<Fill>() {
            @Override
            public int compare(Fill a, Fill b) {
                return Integer.compare(a.km, b.km);
            }
        });
        return out;
    }

    public static void saveFuel(Context c, List<Fill> l) {
        JSONArray a = new JSONArray();
        try {
            for (Fill f : l) {
                JSONObject o = new JSONObject();
                o.put("ts", f.ts);
                o.put("km", f.km);
                o.put("liters", f.liters);
                o.put("euros", f.euros);
                a.put(o);
            }
        } catch (JSONException e) {
            return;
        }
        sp(c).edit().putString("fuel", a.toString()).apply();
    }

    // ---------- Fechas ----------

    public static long todayMillis() {
        Calendar c = Calendar.getInstance(TZ);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static int daysUntil(long date) {
        return (int) Math.round((date - todayMillis()) / 86400000.0);
    }

    public static String formatDate(long date) {
        if (date <= 0) return "";
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        f.setTimeZone(TZ);
        return f.format(date);
    }

    /** 0 = vacío, -1 = no válida, otro = fecha en millis. Acepta d/m/aaaa, d-m-aa, d.m.aaaa. */
    public static long parseDate(String s) {
        String t = s.trim().replace('-', '/').replace('.', '/');
        if (t.isEmpty()) return 0;
        String[] p = t.split("/");
        if (p.length != 3) return -1;
        try {
            int d = Integer.parseInt(p[0].trim());
            int m = Integer.parseInt(p[1].trim());
            int y = Integer.parseInt(p[2].trim());
            if (y < 100) y += 2000;
            Calendar c = Calendar.getInstance(TZ);
            c.clear();
            c.setLenient(false);
            c.set(y, m - 1, d);
            return c.getTimeInMillis();
        } catch (Exception e) {
            return -1;
        }
    }

    public static String formatKm(int km) {
        return String.format(new Locale("es", "ES"), "%,d", km);
    }

    // ---------- Urgencia y textos ----------

    static double dateUrgency(Item it) {
        return it.nextDate > 0 ? daysUntil(it.nextDate) : Double.POSITIVE_INFINITY;
    }

    static double kmUrgency(Item it, int curKm) {
        if (it.nextKm > 0 && curKm > 0) return (it.nextKm - curKm) / KM_PER_DAY;
        return Double.POSITIVE_INFINITY;
    }

    /** En "días": el menor entre la parte de fecha y la de km. Infinito = nada que avisar. */
    public static double urgency(Item it, int curKm) {
        return Math.min(dateUrgency(it), kmUrgency(it, curKm));
    }

    public static int color(double urgencyDays) {
        if (urgencyDays <= 30) return Ui.RED;
        if (urgencyDays <= 90) return Ui.ACC;
        return Ui.GRAY;
    }

    public static String dateText(int d) {
        if (d < 0) return "vencido hace " + (-d) + (d == -1 ? " día" : " días");
        if (d == 0) return "hoy";
        if (d == 1) return "mañana";
        if (d <= 120) return "en " + d + " días";
        return "en " + (d / 30) + " meses";
    }

    public static String kmText(int left) {
        if (left < 0) return "pasado " + formatKm(-left) + " km";
        return "faltan " + formatKm(left) + " km";
    }

    /** Un solo texto, el de la parte más urgente (para la pantalla de inicio). */
    public static String shortStatus(Item it, int curKm) {
        double du = dateUrgency(it);
        double ku = kmUrgency(it, curKm);
        if (du <= ku && du != Double.POSITIVE_INFINITY) return dateText(daysUntil(it.nextDate));
        if (ku != Double.POSITIVE_INFINITY) return kmText(it.nextKm - curKm);
        return "";
    }

    /** Texto completo (fecha y km) para la pantalla de recordatorios. */
    public static String fullStatus(Item it, int curKm) {
        StringBuilder sb = new StringBuilder();
        if (it.nextDate > 0) {
            sb.append(formatDate(it.nextDate)).append(" · ").append(dateText(daysUntil(it.nextDate)));
        }
        if (it.nextKm > 0) {
            if (sb.length() > 0) sb.append("   |   ");
            sb.append("a los ").append(formatKm(it.nextKm)).append(" km");
            if (curKm > 0) sb.append(" · ").append(kmText(it.nextKm - curKm));
        }
        if (sb.length() == 0) sb.append("Sin fecha ni km: toca para ponerlos");
        return sb.toString();
    }

    /** Marca como hecho: calcula la próxima fecha/km sumando el intervalo. Devuelve false si faltan los km actuales. */
    public static boolean markDone(Context c, Item it) {
        int km = getKm(c);
        if (it.everyKm > 0 && km <= 0) return false;
        if (it.everyMonths > 0) {
            Calendar cal = Calendar.getInstance(TZ);
            cal.setTimeInMillis(todayMillis());
            cal.add(Calendar.MONTH, it.everyMonths);
            it.nextDate = cal.getTimeInMillis();
        }
        if (it.everyKm > 0) it.nextKm = km + it.everyKm;
        return true;
    }
}
