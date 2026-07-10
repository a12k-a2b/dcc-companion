package club.daylightcomputer.companion;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The inspector-at-home: looks at what is actually installed and enabled
 * on THIS tablet and explains, in plain words, where dishes might fight.
 * Everything it learns stays on the tablet.
 */
public class Inspector {

    public static class Finding {
        public final String title;
        public final String explanation;
        public final String settingsAction; // deep link, or null

        Finding(String title, String explanation, String settingsAction) {
            this.title = title;
            this.explanation = explanation;
            this.settingsAction = settingsAction;
        }
    }

    /** Which club dishes are installed, as "pkg -> versionName". */
    public static String installedVersion(Context ctx, String pkg) {
        if (pkg == null || pkg.isEmpty()) return null;
        try {
            PackageInfo pi = ctx.getPackageManager().getPackageInfo(pkg, 0);
            return pi.versionName == null ? "?" : pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static List<Finding> inspect(Context ctx, List<Catalog.Dish> shelf) {
        List<Finding> findings = new ArrayList<>();
        PackageManager pm = ctx.getPackageManager();

        // 1. Accessibility services: more than one enabled means they form
        // a chain — key events pass through them in order, and two services
        // both grabbing the same keys is the classic club collision
        // (Daylight Keys listens to the volume keys this way).
        String enabled = Settings.Secure.getString(ctx.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        List<String> services = new ArrayList<>();
        if (!TextUtils.isEmpty(enabled)) {
            for (String comp : enabled.split(":")) {
                String appLabel = labelFor(pm, comp.split("/")[0]);
                if (appLabel != null && !services.contains(appLabel)) services.add(appLabel);
            }
        }
        if (services.size() > 1) {
            findings.add(new Finding(
                    "Two helpers are listening to your keys and screen",
                    TextUtils.join(" and ", services) + " both run as accessibility "
                    + "services. They form a line: whichever is higher in the list "
                    + "sees your key presses first, and may keep them from the other. "
                    + "If one of them stops responding to keys, this order is why. "
                    + "You can reorder or switch one off in Settings.",
                    Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }

        // 2. Overlays: two club dishes that both draw over other apps can
        // cover each other's corners and buttons.
        List<String> overlayDishes = new ArrayList<>();
        for (Catalog.Dish d : shelf) {
            if (d.pkg == null) continue;
            try {
                PackageInfo pi = pm.getPackageInfo(d.pkg, PackageManager.GET_PERMISSIONS);
                if (pi.requestedPermissions == null) continue;
                for (String p : pi.requestedPermissions) {
                    if ("android.permission.SYSTEM_ALERT_WINDOW".equals(p)) {
                        overlayDishes.add(d.name);
                        break;
                    }
                }
            } catch (PackageManager.NameNotFoundException ignored) { }
        }
        if (overlayDishes.size() > 1) {
            findings.add(new Finding(
                    "Two dishes draw on top of your screen",
                    TextUtils.join(" and ", overlayDishes) + " can both draw over "
                    + "other apps. If a corner, button, or panel from one seems to "
                    + "disappear, the other may be drawing on top of it. Each app's "
                    + "overlay switch is in Settings if you need to choose.",
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
        }

        return findings;
    }

    private static String labelFor(PackageManager pm, String pkg) {
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }
}
