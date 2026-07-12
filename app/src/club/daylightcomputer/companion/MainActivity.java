package club.daylightcomputer.companion;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * The Companion's single screen: the shelf (with one-tap installs and
 * update notes), the inspector's getting-along report, and the reporter.
 * Design follows the club's rules for the DC-1's reflective monochrome
 * screen: pure grayscale, real borders, no animation, big touch targets.
 */
public class MainActivity extends Activity {

    private static final int INK = Color.BLACK;
    private static final int PAPER = Color.WHITE;
    private static final int FAINT = Color.rgb(102, 102, 102);

    private LinearLayout page;
    private List<Catalog.Dish> shelf = new ArrayList<>();
    private List<Counter.Dish> counter = new ArrayList<>();
    private List<Inspector.Finding> findings = new ArrayList<>();
    private boolean offline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // First-run duties (permission, the butler's rounds) live in
        // ClubActivity — the front room. This is the staff room.
        ShelfJobService.schedule(this);

        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(PAPER);
        page.setPadding(dp(20), dp(20), dp(20), dp(40));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(PAPER);
        scroll.addView(page);
        setContentView(scroll);

        render(); // header + "looking at the shelf…" immediately
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Coming back from an install or Settings — look again.
        if (!shelf.isEmpty()) refresh();
    }

    private void refresh() {
        new Thread(() -> {
            List<Catalog.Dish> fresh;
            boolean off = false;
            try {
                String json = new String(Catalog.get(Catalog.SHELF_URL), "UTF-8");
                Catalog.saveSnapshot(this, json);
                fresh = Catalog.parse(json);
            } catch (Exception e) {
                off = true;
                try {
                    String cached = Catalog.loadSnapshot(this);
                    fresh = cached == null ? new ArrayList<>() : Catalog.parse(cached);
                } catch (Exception e2) {
                    fresh = new ArrayList<>();
                }
            }
            List<Counter.Dish> tasting;
            try {
                tasting = Counter.fetch(this);
            } catch (Exception e) {
                tasting = new ArrayList<>();
            }
            final List<Catalog.Dish> result = fresh;
            final List<Counter.Dish> onCounter = tasting;
            final boolean isOff = off;
            runOnUiThread(() -> {
                shelf = result;
                counter = onCounter;
                offline = isOff;
                findings = Inspector.inspect(this, shelf);
                render();
            });
        }).start();
    }

    /* ---------------- the page ---------------- */

    private void render() {
        page.removeAllViews();

        page.addView(label("DAYLIGHT COMPUTER CLUB"));
        TextView title = text("Club Companion", 26, true);
        title.setTypeface(Typeface.SERIF, Typeface.BOLD);
        page.addView(title);
        page.addView(muted("It watches the shelf so you don't have to."));

        if (offline) {
            TextView off = muted("Offline — showing the shelf from the last look.");
            off.setPadding(0, dp(8), 0, 0);
            page.addView(off);
        }
        gap(16);

        // The owner's tasting counter — only on a tablet that paired one.
        if (!counter.isEmpty()) {
            page.addView(label("ON THE COUNTER — JUST YOU"));
            for (Counter.Dish d : counter) {
                LinearLayout card = card();
                card.addView(text(d.name, 18, true));
                if (d.note != null && !d.note.isEmpty()) card.addView(muted(d.note));
                card.addView(muted("resting since " +
                        (d.added.length() >= 10 ? d.added.substring(0, 10) : d.added)));
                card.addView(button("Taste it →", v -> startActivity(
                        new Intent(this, DishWebActivity.class).putExtra("url", d.url))));
                page.addView(card);
            }
            gap(20);
        }

        page.addView(label("THE SHELF"));
        if (shelf.isEmpty()) {
            page.addView(muted(offline
                    ? "Haven't seen the shelf yet — connect once and come back."
                    : "Looking at the shelf…"));
        }
        for (Catalog.Dish d : shelf) {
            page.addView("apk".equals(d.type) ? apkCard(d) : pwaCard(d));
        }
        gap(20);

        page.addView(label("GETTING ALONG"));
        if (findings.isEmpty()) {
            LinearLayout card = card();
            card.addView(text("No collisions — your dishes are getting along ☀", 16, false));
            page.addView(card);
        }
        for (Inspector.Finding f : findings) {
            LinearLayout card = card();
            card.addView(text(f.title, 17, true));
            card.addView(muted(f.explanation));
            if (f.settingsAction != null) {
                card.addView(button("Open the right Settings page →", v -> {
                    try { startActivity(new Intent(f.settingsAction)); }
                    catch (Exception e) { toast("Couldn't open Settings here."); }
                }));
            }
            page.addView(card);
        }
        gap(20);

        page.addView(button("Check the shelf again", v -> { toast("Looking…"); refresh(); }));
        page.addView(button("Report trouble to a cook", v -> Reporter.send(this, shelf, findings)));
        page.addView(button("Open the club in Chrome →", v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Catalog.CLUB)))));

        TextView foot = muted("A potluck, not a store. The Companion never installs "
                + "anything without you tapping yes, and nothing it sees leaves this tablet.");
        foot.setPadding(0, dp(16), 0, 0);
        page.addView(foot);
    }

    private View apkCard(Catalog.Dish d) {
        LinearLayout card = card();
        card.addView(text(d.name, 18, true));
        card.addView(muted(d.tagline));
        card.addView(muted("by " + d.author + " · " + d.version
                + (d.size != null && !d.size.isEmpty() ? " · " + d.size : "")));

        String installed = Inspector.installedVersion(this, d.pkg);

        if (installed == null) {
            card.addView(button("Get it — one tap ⬇", v -> install(d, (Button) v)));
        } else if (d.version.startsWith(installed)) {
            card.addView(muted("On your tablet ✓ (" + installed + ")"));
        } else {
            card.addView(muted("On your tablet: " + installed));
            card.addView(button("Update to " + d.version + " ⬆", v -> install(d, (Button) v)));
        }
        return card;
    }

    private View pwaCard(Catalog.Dish d) {
        LinearLayout card = card();
        card.addView(text(d.name, 18, true));
        card.addView(muted(d.tagline + " (a web dish — lives in your browser)"));
        card.addView(button("Open it →", v -> startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse(
                        d.url.startsWith("http") ? d.url : Catalog.CLUB)))));
        return card;
    }

    private void install(Catalog.Dish d, Button b) {
        b.setEnabled(false);
        b.setText("Fetching " + d.name + "…");
        new Thread(() -> {
            try {
                Installer.install(this, d);
                runOnUiThread(() -> b.setText("Handing it to Android…"));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    b.setEnabled(true);
                    b.setText("Get it — one tap ⬇");
                    toast("Couldn't fetch the dish — are you online?");
                });
            }
        }).start();
    }

    /* ---------------- grayscale building blocks ---------------- */

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable border = new GradientDrawable();
        border.setColor(PAPER);
        border.setStroke(dp(2), INK);
        c.setBackground(border);
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        c.setLayoutParams(lp);
        return c;
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(INK);
        if (bold) t.setTypeface(Typeface.SERIF, Typeface.BOLD);
        return t;
    }

    private TextView muted(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(14);
        t.setTextColor(FAINT);
        t.setPadding(0, dp(4), 0, 0);
        return t;
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setTextColor(INK);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.12f);
        t.setPadding(0, dp(8), 0, dp(2));
        return t;
    }

    private Button button(String s, View.OnClickListener onClick) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(PAPER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(INK);
        b.setBackground(bg);
        b.setMinHeight(dp(48));
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        b.setLayoutParams(lp);
        return b;
    }

    private void gap(int dps) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dps)));
        page.addView(v);
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private int dp(int d) {
        return Math.round(d * getResources().getDisplayMetrics().density);
    }
}
