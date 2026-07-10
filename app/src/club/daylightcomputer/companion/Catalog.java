package club.daylightcomputer.companion;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * The shelf, as the Companion sees it: fetches apps.json from the club,
 * remembers the last copy it saw (so the butler can spot new dishes,
 * updates, and recalls), and answers "is this dish on this tablet?".
 */
public class Catalog {

    public static final String CLUB = "https://daylightcomputer.club/";
    public static final String SHELF_URL = CLUB + "apps.json";
    private static final String PREFS = "companion";
    private static final String KEY_SNAPSHOT = "shelf-snapshot";

    public static class Dish {
        public String id, name, tagline, author, version, updated, type;
        public String pkg;      // apk dishes only
        public String apkUrl;   // apk dishes only, absolute
        public String size;     // apk dishes only, human words
        public String url;      // pwa dishes only

        static Dish from(JSONObject o) {
            Dish d = new Dish();
            d.id = o.optString("id");
            d.name = o.optString("name");
            d.tagline = o.optString("tagline");
            d.author = o.optString("author");
            d.version = o.optString("version");
            d.updated = o.optString("updated");
            d.type = o.optString("type");
            d.url = o.optString("url");
            JSONObject apk = o.optJSONObject("apk");
            if (apk != null) {
                d.pkg = apk.optString("package");
                d.size = apk.optString("size");
                String file = apk.optString("file");
                d.apkUrl = file.isEmpty() ? null : CLUB + file;
            }
            return d;
        }
    }

    /** Fetch the live shelf. Blocking — call off the main thread. */
    public static List<Dish> fetch() throws Exception {
        return parse(new String(get(SHELF_URL), "UTF-8"));
    }

    public static List<Dish> parse(String json) throws Exception {
        List<Dish> dishes = new ArrayList<>();
        JSONArray apps = new JSONObject(json).getJSONArray("apps");
        for (int i = 0; i < apps.length(); i++) {
            dishes.add(Dish.from(apps.getJSONObject(i)));
        }
        return dishes;
    }

    /** Raw bytes from the club — used for the shelf and for dish files. */
    public static byte[] get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        try (InputStream in = c.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        } finally {
            c.disconnect();
        }
    }

    /** The last shelf this tablet saw, or null on first run. */
    public static String loadSnapshot(Context ctx) {
        return prefs(ctx).getString(KEY_SNAPSHOT, null);
    }

    public static void saveSnapshot(Context ctx, String json) {
        prefs(ctx).edit().putString(KEY_SNAPSHOT, json).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
