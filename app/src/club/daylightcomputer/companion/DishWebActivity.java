package club.daylightcomputer.companion;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A web dish, served full-screen inside the Clubhouse — no browser
 * chrome, no address bar, no add-to-home-screen ceremony. Used for
 * counter dishes and shelf web dishes alike. A thin ink bar up top is
 * the only furniture: the dish's address (so nothing is ever sneaky)
 * and a way back to the Clubhouse.
 *
 * Exported on purpose so the owner's Mac can put a dish on the glass
 * over adb (`daylight open`). It renders web pages; it grants nothing.
 */
public class DishWebActivity extends Activity {

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra("url");
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
            finish();
            return;
        }

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.WHITE);

        // The thin ink bar: ← back, then the dish's honest address.
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(Color.BLACK);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        TextView back = new TextView(this);
        back.setText("←");
        back.setTextSize(22);
        back.setTextColor(Color.WHITE);
        back.setTypeface(Typeface.DEFAULT_BOLD);
        back.setPadding(dp(18), dp(10), dp(18), dp(10));
        back.setMinimumWidth(dp(48));
        back.setMinimumHeight(dp(48));
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        bar.addView(back);

        TextView where = new TextView(this);
        where.setText(Uri.parse(url).getHost());
        where.setTextSize(14);
        where.setTextColor(Color.rgb(200, 200, 200));
        where.setSingleLine(true);
        bar.addView(where);

        page.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient());
        // A download inside a dish (e.g. an APK link) goes to the browser —
        // this window renders pages, it doesn't handle files.
        web.setDownloadListener((dlUrl, ua, cd, mime, len) -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(dlUrl)));
            } catch (Exception ignored) { }
        });
        page.addView(web, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(page);
        web.loadUrl(url);
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
