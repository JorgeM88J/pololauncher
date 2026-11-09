package es.jorge.pololauncher;

import java.util.Calendar;
import java.util.Locale;

/** Amanecer y anochecer calculados offline con latitud/longitud (algoritmo del Almanac). */
public class Sun {
    static final double ZENITH = 90.833;

    /** Devuelve {amanecer, anochecer} como "HH:mm" en la zona horaria del Calendar. */
    public static String[] times(Calendar day, double lat, double lon) {
        int n = day.get(Calendar.DAY_OF_YEAR);
        double off = day.getTimeZone().getOffset(day.getTimeInMillis()) / 3600000.0;
        return new String[]{fmt(calc(n, lat, lon, true), off), fmt(calc(n, lat, lon, false), off)};
    }

    static double rad(double d) { return d * Math.PI / 180.0; }

    static double deg(double r) { return r * 180.0 / Math.PI; }

    static double norm(double v, double m) {
        v = v % m;
        if (v < 0) v += m;
        return v;
    }

    static double calc(int n, double lat, double lon, boolean rising) {
        double lngHour = lon / 15.0;
        double t = n + ((rising ? 6 : 18) - lngHour) / 24.0;
        double M = 0.9856 * t - 3.289;
        double L = norm(M + 1.916 * Math.sin(rad(M)) + 0.020 * Math.sin(rad(2 * M)) + 282.634, 360);
        double RA = norm(deg(Math.atan(0.91764 * Math.tan(rad(L)))), 360);
        RA += (Math.floor(L / 90.0) * 90.0) - (Math.floor(RA / 90.0) * 90.0);
        RA /= 15.0;
        double sinDec = 0.39782 * Math.sin(rad(L));
        double cosDec = Math.cos(Math.asin(sinDec));
        double cosH = (Math.cos(rad(ZENITH)) - sinDec * Math.sin(rad(lat))) / (cosDec * Math.cos(rad(lat)));
        if (cosH > 1 || cosH < -1) return Double.NaN;
        double H = rising ? 360 - deg(Math.acos(cosH)) : deg(Math.acos(cosH));
        H /= 15.0;
        double T = H + RA - 0.06571 * t - 6.622;
        return norm(T - lngHour, 24);
    }

    static String fmt(double ut, double off) {
        if (Double.isNaN(ut)) return "--:--";
        double local = norm(ut + off, 24);
        int min = (int) Math.round(local * 60) % 1440;
        return String.format(Locale.US, "%02d:%02d", min / 60, min % 60);
    }
}
