package es.jorge.pololauncher;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

/** Registro de repostajes: km, litros y euros. Calcula el consumo de depósito lleno a depósito lleno. */
public class FuelActivity extends Activity {

    static final Locale ES = new Locale("es", "ES");

    final TextView[] statVal = new TextView[3];
    LinearLayout list;
    EditText kmE, litE, eurE;
    List<Store.Fill> fills;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);
        getWindow().setBackgroundDrawable(new ColorDrawable(Ui.BG));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.px(44), Ui.px(28), Ui.px(44), Ui.px(24));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(Ui.tv(this, "Repostajes", 40, Ui.TXT, true), Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView back = Ui.button(this, "Volver", Ui.CARD, Ui.TXT);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finish(); }
        });
        head.addView(back);
        root.addView(head, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));

        // Tres tarjetas con el consumo
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        String[] titles = {"ÚLTIMO DEPÓSITO", "MEDIA", "COSTE CADA 100 KM"};
        for (int i = 0; i < 3; i++) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(Ui.round(Ui.CARD, 18));
            card.setPadding(Ui.px(22), Ui.px(16), Ui.px(22), Ui.px(16));
            card.addView(Ui.tv(this, titles[i], 16, Ui.GRAY, true));
            statVal[i] = Ui.tv(this, "--", 38, i == 0 ? Ui.ACC : Ui.TXT, true);
            card.addView(statVal[i], Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 6, 0, 0));
            stats.addView(card, Ui.lpm(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, i == 0 ? 0 : 8, 0, i == 2 ? 0 : 8, 0));
        }
        root.addView(stats, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 16, 0, 0));

        // Formulario
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.HORIZONTAL);
        form.setGravity(Gravity.BOTTOM);
        form.setBackground(Ui.round(Ui.CARD, 18));
        form.setPadding(Ui.px(22), Ui.px(14), Ui.px(22), Ui.px(18));
        kmE = field("km del cuentakilómetros", InputType.TYPE_CLASS_NUMBER);
        litE = field("litros", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        eurE = field("euros totales", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addCol(form, "Km", kmE, 1.3f);
        addCol(form, "Litros", litE, 1f);
        addCol(form, "Euros", eurE, 1f);
        TextView save = Ui.button(this, "Guardar", Ui.ACC, Ui.DARK);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { save(); }
        });
        form.addView(save, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 8, 0, 0, 0));
        root.addView(form, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 14, 0, 0));
        root.addView(Ui.tv(this, "Llena siempre el depósito hasta el tope para que el consumo salga bien.", 18, Ui.GRAY, false),
                Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4, 8, 0, 8));

        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sv, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int km = Store.getKm(this);
        if (kmE.getText().length() == 0 && km > 0) kmE.setText(String.valueOf(km));
        refresh();
    }

    EditText field(String hint, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(26));
        e.setTextColor(Ui.TXT);
        e.setHintTextColor(Ui.GRAY);
        e.setBackground(Ui.round(Ui.BG, 12));
        e.setPadding(Ui.px(16), Ui.px(12), Ui.px(16), Ui.px(12));
        return e;
    }

    void addCol(LinearLayout parent, String label, EditText e, float weight) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(Ui.tv(this, label, 17, Ui.GRAY, false), Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 0, 6));
        col.addView(e, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        parent.addView(col, Ui.lpm(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight, 0, 0, 8, 0));
    }

    static double parseD(String s) {
        String t = s.trim().replace(',', '.');
        if (t.isEmpty()) return 0;
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    void save() {
        int km = RemindersActivity.parseInt(kmE.getText().toString());
        double l = parseD(litE.getText().toString());
        double e = parseD(eurE.getText().toString());
        if (km <= 0) {
            Toast.makeText(this, "Pon los km del cuentakilómetros", Toast.LENGTH_SHORT).show();
            return;
        }
        if (l <= 0 || e <= 0) {
            Toast.makeText(this, "Pon los litros y los euros", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!fills.isEmpty()) {
            int max = fills.get(fills.size() - 1).km;
            if (km <= max) {
                Toast.makeText(this, "Los km tienen que ser mayores que los del último repostaje (" + Store.formatKm(max) + ")", Toast.LENGTH_LONG).show();
                return;
            }
        }
        Store.Fill f = new Store.Fill();
        f.ts = System.currentTimeMillis();
        f.km = km;
        f.liters = l;
        f.euros = e;
        fills.add(f);
        Store.saveFuel(this, fills);
        if (km > Store.getKm(this)) Store.setKm(this, km);
        litE.setText("");
        eurE.setText("");
        Toast.makeText(this, fills.size() == 1 ? "Guardado: el primero solo sirve de punto de partida" : "Guardado", Toast.LENGTH_SHORT).show();
        refresh();
    }

    void refresh() {
        fills = Store.loadFuel(this);
        int n = fills.size();
        if (n >= 2) {
            Store.Fill last = fills.get(n - 1);
            Store.Fill prev = fills.get(n - 2);
            double lastCons = last.liters / (last.km - prev.km) * 100.0;
            double totalL = 0, totalE = 0;
            for (int i = 1; i < n; i++) {
                totalL += fills.get(i).liters;
                totalE += fills.get(i).euros;
            }
            double dist = last.km - fills.get(0).km;
            statVal[0].setText(String.format(ES, "%.1f l/100", lastCons));
            statVal[1].setText(String.format(ES, "%.1f l/100", totalL / dist * 100.0));
            statVal[2].setText(String.format(ES, "%.1f €", totalE / dist * 100.0));
        } else {
            for (TextView t : statVal) t.setText("--");
        }

        list.removeAllViews();
        if (n < 2) {
            list.addView(Ui.tv(this, n == 0
                            ? "Mete tu primer repostaje (sirve de punto de partida) y el siguiente ya calcula el consumo."
                            : "Ya tienes el punto de partida. En el próximo repostaje sale el consumo.",
                    22, Ui.GRAY, false));
        }
        for (int i = n - 1; i >= 0 && i >= n - 40; i--) {
            final Store.Fill f = fills.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(Ui.round(Ui.CARD, 18));
            row.setPadding(Ui.px(24), Ui.px(14), Ui.px(24), Ui.px(14));
            row.setClickable(true);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { confirmDelete(f); }
            });

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(Ui.tv(this, Store.formatDate(f.ts) + "  ·  " + Store.formatKm(f.km) + " km", 26, Ui.TXT, true));
            col.addView(Ui.tv(this, String.format(ES, "%.1f l  ·  %.2f €  ·  %.3f €/l", f.liters, f.euros, f.euros / f.liters), 20, Ui.GRAY, false),
                    Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 4, 0, 0));
            row.addView(col, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            String cons = (i == 0) ? "punto de partida"
                    : String.format(ES, "%.1f l/100", f.liters / (f.km - fills.get(i - 1).km) * 100.0);
            row.addView(Ui.tv(this, cons, i == 0 ? 20 : 28, i == 0 ? Ui.GRAY : Ui.ACC, i != 0));
            list.addView(row, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 0, 10));
        }
    }

    void confirmDelete(final Store.Fill f) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(Ui.tv(this, Store.formatKm(f.km) + " km · " + String.format(ES, "%.1f l", f.liters), 24, Ui.GRAY, false));
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.END);
        TextView cancel = Ui.button(this, "Cancelar", Ui.BG, Ui.TXT);
        TextView del = Ui.button(this, "Borrar", Ui.BG, Ui.RED);
        btns.addView(cancel);
        btns.addView(del, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 14, 0, 0, 0));
        box.addView(btns, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 20, 0, 0));
        final Dialog d = Ui.dialog(this, "¿Borrar este repostaje?", box, 0);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { d.dismiss(); }
        });
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fills.remove(f);
                Store.saveFuel(FuelActivity.this, fills);
                d.dismiss();
                refresh();
            }
        });
        d.show();
    }
}
