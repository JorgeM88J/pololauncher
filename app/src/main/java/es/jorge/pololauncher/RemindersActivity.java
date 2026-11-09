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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Pantalla para ver, añadir, editar y marcar como hechos los recordatorios (ITV, aceite, distribución...). */
public class RemindersActivity extends Activity {

    LinearLayout list;
    TextView kmBtn;
    List<Store.Item> items;

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
        head.addView(Ui.tv(this, "Recordatorios", 40, Ui.TXT, true), Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView add = Ui.button(this, "+ Añadir", Ui.ACC, Ui.DARK);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { editDialog(null); }
        });
        head.addView(add);
        TextView back = Ui.button(this, "Volver", Ui.CARD, Ui.TXT);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finish(); }
        });
        head.addView(back, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 16, 0, 0, 0));
        root.addView(head, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));

        LinearLayout kmRow = new LinearLayout(this);
        kmRow.setOrientation(LinearLayout.HORIZONTAL);
        kmRow.setGravity(Gravity.CENTER_VERTICAL);
        kmRow.addView(Ui.tv(this, "Km actuales del coche:", 24, Ui.GRAY, false));
        kmBtn = Ui.button(this, "", Ui.CARD, Ui.ACC);
        kmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { kmDialog(); }
        });
        kmRow.addView(kmBtn, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 16, 0, 0, 0));
        kmRow.addView(Ui.tv(this, "(ponlos de vez en cuando para que avise por km)", 18, Ui.GRAY, false),
                Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 16, 0, 0, 0));
        root.addView(kmRow, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 14, 0, 14));

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
        refresh();
    }

    void refresh() {
        items = Store.load(this);
        final int km = Store.getKm(this);
        kmBtn.setText(km > 0 ? Store.formatKm(km) + " km" : "sin poner");

        // Orden: lo más urgente arriba; lo que no tiene fecha ni km, al final.
        List<Store.Item> sorted = new ArrayList<>(items);
        Collections.sort(sorted, new Comparator<Store.Item>() {
            @Override
            public int compare(Store.Item a, Store.Item c) {
                return Double.compare(Store.urgency(a, km), Store.urgency(c, km));
            }
        });

        list.removeAllViews();
        for (final Store.Item it : sorted) {
            double u = Store.urgency(it, km);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(Ui.round(Ui.CARD, 18));
            row.setPadding(Ui.px(24), Ui.px(16), Ui.px(20), Ui.px(16));
            row.setClickable(true);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { editDialog(it); }
            });

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(Ui.tv(this, it.name, 28, Ui.TXT, true));
            int color = (u == Double.POSITIVE_INFINITY) ? Ui.GRAY : Store.color(u);
            col.addView(Ui.tv(this, Store.fullStatus(it, km), 22, color, false),
                    Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 6, 0, 0));
            String rep = repeatText(it);
            if (!rep.isEmpty()) {
                col.addView(Ui.tv(this, rep, 17, Ui.GRAY, false),
                        Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 4, 0, 0));
            }
            row.addView(col, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            if (it.everyMonths > 0 || it.everyKm > 0) {
                TextView done = Ui.button(this, "Hecho", Ui.ACC, Ui.DARK);
                done.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!Store.markDone(RemindersActivity.this, it)) {
                            Toast.makeText(RemindersActivity.this, "Pon primero los km actuales del coche", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Store.save(RemindersActivity.this, items);
                        refresh();
                    }
                });
                row.addView(done);
            }
            list.addView(row, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 0, 10));
        }
    }

    String repeatText(Store.Item it) {
        StringBuilder sb = new StringBuilder();
        if (it.everyMonths > 0) sb.append("cada ").append(it.everyMonths).append(" meses");
        if (it.everyKm > 0) {
            if (sb.length() > 0) sb.append(" o ");
            else sb.append("cada ");
            sb.append(Store.formatKm(it.everyKm)).append(" km");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ Diálogos

    EditText field(String hint, String value, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(26));
        e.setTextColor(Ui.TXT);
        e.setHintTextColor(Ui.GRAY);
        e.setBackground(Ui.round(Ui.BG, 12));
        e.setPadding(Ui.px(18), Ui.px(14), Ui.px(18), Ui.px(14));
        return e;
    }

    void addLabeled(LinearLayout parent, String label, EditText e) {
        parent.addView(Ui.tv(this, label, 18, Ui.GRAY, false),
                Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 12, 0, 6));
        parent.addView(e, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
    }

    static String num(int v) { return v > 0 ? String.valueOf(v) : ""; }

    static int parseInt(String s) {
        String t = s.trim().replace(".", "").replace(" ", "");
        if (t.isEmpty()) return 0;
        try {
            return Math.max(0, Integer.parseInt(t));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    void kmDialog() {
        final EditText e = field("p. ej. 135000", num(Store.getKm(this)), InputType.TYPE_CLASS_NUMBER);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(e, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.END);
        TextView cancel = Ui.button(this, "Cancelar", Ui.BG, Ui.TXT);
        TextView save = Ui.button(this, "Guardar", Ui.ACC, Ui.DARK);
        btns.addView(cancel);
        btns.addView(save, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 14, 0, 0, 0));
        box.addView(btns, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 18, 0, 0));
        final Dialog d = Ui.dialog(this, "Km actuales (cuentakilómetros)", box, 0);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { d.dismiss(); }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int km = parseInt(e.getText().toString());
                if (km < 0) {
                    Toast.makeText(RemindersActivity.this, "Número no válido", Toast.LENGTH_SHORT).show();
                    return;
                }
                Store.setKm(RemindersActivity.this, km);
                d.dismiss();
                refresh();
            }
        });
        d.show();
    }

    /** it == null: crear uno nuevo. */
    void editDialog(final Store.Item it) {
        final boolean isNew = (it == null);
        final Store.Item cur = isNew ? new Store.Item() : it;

        final EditText name = field("p. ej. Neumáticos", cur.name, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final EditText date = field("dd/mm/aaaa", Store.formatDate(cur.nextDate), InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE);
        final EditText nextKm = field("p. ej. 145000", num(cur.nextKm), InputType.TYPE_CLASS_NUMBER);
        final EditText months = field("p. ej. 12 (vacío = no se repite)", num(cur.everyMonths), InputType.TYPE_CLASS_NUMBER);
        final EditText everyKm = field("p. ej. 10000 (vacío = no se repite)", num(cur.everyKm), InputType.TYPE_CLASS_NUMBER);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        addLabeled(form, "Nombre", name);
        addLabeled(form, "Próxima fecha", date);
        addLabeled(form, "Próximos km (a qué km toca)", nextKm);
        addLabeled(form, "Se repite cada… (meses)", months);
        addLabeled(form, "Se repite cada… (km)", everyKm);
        ScrollView sv = new ScrollView(this);
        sv.addView(form, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(sv, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.CENTER_VERTICAL);
        TextView del = Ui.button(this, "Borrar", Ui.BG, Ui.RED);
        del.setVisibility(isNew ? View.GONE : View.VISIBLE);
        btns.addView(del);
        btns.addView(new View(this), Ui.lp(0, 1, 1f));
        TextView cancel = Ui.button(this, "Cancelar", Ui.BG, Ui.TXT);
        TextView save = Ui.button(this, "Guardar", Ui.ACC, Ui.DARK);
        btns.addView(cancel);
        btns.addView(save, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 14, 0, 0, 0));
        content.addView(btns, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 14, 0, 0));

        final Dialog d = Ui.dialog(this, isNew ? "Nuevo recordatorio" : cur.name, content, Ui.px(640));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { d.dismiss(); }
        });
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                items.remove(cur);
                Store.save(RemindersActivity.this, items);
                d.dismiss();
                refresh();
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String n = name.getText().toString().trim();
                long dt = Store.parseDate(date.getText().toString());
                int nk = parseInt(nextKm.getText().toString());
                int em = parseInt(months.getText().toString());
                int ek = parseInt(everyKm.getText().toString());
                if (n.isEmpty()) {
                    Toast.makeText(RemindersActivity.this, "Ponle un nombre", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (dt < 0) {
                    Toast.makeText(RemindersActivity.this, "Fecha no válida. Usa dd/mm/aaaa, por ejemplo 15/03/2027", Toast.LENGTH_LONG).show();
                    return;
                }
                if (nk < 0 || em < 0 || ek < 0) {
                    Toast.makeText(RemindersActivity.this, "Revisa los números", Toast.LENGTH_SHORT).show();
                    return;
                }
                cur.name = n;
                cur.nextDate = dt;
                cur.nextKm = nk;
                cur.everyMonths = em;
                cur.everyKm = ek;
                if (isNew) items.add(cur);
                Store.save(RemindersActivity.this, items);
                d.dismiss();
                refresh();
            }
        });
        d.show();
    }
}
