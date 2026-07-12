package club.daylightcomputer.companion;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The Clubhouse's front room — and its home tab IS the shelf: the club
 * website itself, full-screen, with a thin ink bar on top. Everywhere
 * else the website is the club through any window; on a Daylight, this
 * is the club at home. The staff (butler, inspector, reporter) work one
 * door over — the STAFF button.
 */
public class ClubActivity extends Activity {

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // First-run house setup: the butler's rounds and, politely,
        // permission to knock.
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
        ShelfJobService.schedule(this);
        CounterJobService.schedule(this);
        // Opening the front door is also a fine moment to glance at the
        // counter — instant knock if something new is resting there.
        new Thread(() -> {
            try { CounterJobService.checkCounter(this); } catch (Exception ignored) { }
        }).start();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.WHITE);

        // The thin ink bar: the house name, and the staff door.
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(Color.BLACK);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = new TextView(this);
        name.setText("THE CLUBHOUSE ☀");
        name.setTextSize(13);
        name.setTextColor(Color.WHITE);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setLetterSpacing(0.12f);
        name.setPadding(dp(16), dp(12), dp(8), dp(12));
        bar.addView(name, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView staff = new TextView(this);
        staff.setText("STAFF →");
        staff.setTextSize(13);
        staff.setTextColor(Color.BLACK);
        staff.setTypeface(Typeface.DEFAULT_BOLD);
        staff.setLetterSpacing(0.1f);
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(Color.WHITE);
        staff.setBackground(pill);
        staff.setGravity(Gravity.CENTER);
        staff.setPadding(dp(14), dp(10), dp(14), dp(10));
        staff.setMinimumHeight(dp(48));
        staff.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.setMargins(0, dp(6), dp(10), dp(6));
        staff.setLayoutParams(sp);
        bar.addView(staff);

        page.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient());
        // The wizard's APK downloads belong to a real browser (one-tap
        // installs live in the staff room anyway).
        web.setDownloadListener((dlUrl, ua, cd, mime, len) -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse(dlUrl)));
            } catch (Exception ignored) { }
        });
        page.addView(web, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(page);
        web.loadUrl(Catalog.CLUB);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    private int dp(int d) {
        return Math.round(d * getResources().getDisplayMetrics().density);
    }
}
