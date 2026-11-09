package es.jorge.pololauncher;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Ayudas para construir la interfaz por código. Todas las medidas están pensadas para 1280x720 y se escalan. */
public class Ui {
    public static final int BG = 0xFF0E1116;
    public static final int CARD = 0xFF171B22;
    public static final int TXT = 0xFFE8EAED;
    public static final int GRAY = 0xFF9AA3AF;
    public static final int ACC = 0xFFF2A33A;
    public static final int RED = 0xFFFF6B6B;
    public static final int DARK = 0xFF1A1204;

    static float s = 1f;

    public static void init(Context c) {
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        float w = Math.max(dm.widthPixels, dm.heightPixels);
        float h = Math.min(dm.widthPixels, dm.heightPixels);
        s = Math.min(w / 1280f, h / 720f);
    }

    public static int px(float v) { return Math.round(v * s); }

    public static TextView tv(Context c, String text, float size, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * s);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setIncludeFontPadding(false);
        return v;
    }

    public static GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius * s);
        return g;
    }

    public static LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    public static LinearLayout.LayoutParams lpm(int w, int h, float weight, float l, float t, float r, float b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, weight);
        p.setMargins(px(l), px(t), px(r), px(b));
        return p;
    }

    /** Botón de texto redondeado. */
    public static TextView button(Context c, String text, int bg, int fg) {
        TextView b = tv(c, text, 24, fg, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(px(26), px(16), px(26), px(16));
        b.setBackground(round(bg, 14));
        b.setClickable(true);
        return b;
    }

    /**
     * Diálogo oscuro sin título del sistema. heightPx > 0 fija la altura (el contenido se estira); si no, se ajusta al contenido.
     */
    public static Dialog dialog(Context c, String title, View content, int heightPx) {
        Dialog d = new Dialog(c, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(round(CARD, 24));
        box.setPadding(px(28), px(24), px(28), px(24));
        TextView t = tv(c, title, 30, TXT, true);
        box.addView(t, lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 0, 16));
        if (heightPx > 0) {
            box.addView(content, lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        } else {
            box.addView(content, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        }
        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0x00000000));
            w.setLayout(px(780), heightPx > 0 ? heightPx : ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return d;
    }
}
