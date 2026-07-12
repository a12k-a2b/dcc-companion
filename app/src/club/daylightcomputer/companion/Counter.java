package club.daylightcomputer.companion;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * The tasting counter — the owner's personal feed of dishes resting
 * before the shelf. Entirely optional: it only exists if a counter.conf
 * file has been paired onto this tablet (over USB, by the owner's own
 * Mac — `daylight pair`). The URL inside is a private capability link;
 * it never appears in this app's source, and friends' Clubhouses simply
 * have no counter. Everything read here stays on the tablet, same as
 * the shelf.
 */
public class Counter {

    private static final String PREFS = "companion";
    private static final String KEY_SNAPSHOT = "counter-snapshot";

    public static class Dish {
        public String id, name, url, note, added;

        static Dish from(JSONObject o) {
            Dish d = new Dish();
            d.id = o.optString("id");
            d.name = o.optString("name");
            d.url = o.optString("url");
            d.note = o.optString("note");
            d.added = o.optString("added");
            return d;
        }
    }

    /** The paired counter URL, or null if this tablet has no counter. */
    public static String configUrl(Context ctx) {
        try {
            File f = new File(ctx.getExternalFilesDir(null), "counter.conf");
            if (!f.exists()) return null;
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line = r.readLine();
                if (line == null) return null;
                line = line.trim();
                return line.startsWith("https://") ? line : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /** Fetch the live counter. Blocking — call off the main thread. */
    public static List<Dish> fetch(Context ctx) throws Exception {
        String url = configUrl(ctx);
        if (url == null) return new ArrayList<>();
        return parse(new String(Catalog.get(url), "UTF-8"));
    }

    public static List<Dish> parse(String json) throws Exception {
        List<Dish> dishes = new ArrayList<>();
        JSONArray arr = new JSONObject(json).getJSONArray("dishes");
        for (int i = 0; i < arr.length(); i++) {
            dishes.add(Dish.from(arr.getJSONObject(i)));
        }
        return dishes;
    }

    public static String loadSnapshot(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_SNAPSHOT, null);
    }

    public static void saveSnapshot(Context ctx, String json) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_SNAPSHOT, json).apply();
    }
}
