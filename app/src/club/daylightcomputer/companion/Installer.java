package club.daylightcomputer.companion;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

import java.io.OutputStream;

/**
 * One-tap install: downloads a dish from the shelf and hands it to
 * Android's own installer through a PackageInstaller session. Android
 * still shows its confirmation screen — the Companion never installs
 * anything silently. Because every club dish is signed with the club
 * key, updates land cleanly over the installed version.
 */
public class Installer {

    public static final String ACTION_RESULT =
            "club.daylightcomputer.companion.INSTALL_RESULT";

    /** Blocking — call off the main thread. */
    public static void install(Context ctx, Catalog.Dish dish) throws Exception {
        byte[] apk = Catalog.get(dish.apkUrl);

        PackageInstaller installer = ctx.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(dish.pkg);

        int sessionId = installer.createSession(params);
        try (PackageInstaller.Session session = installer.openSession(sessionId)) {
            try (OutputStream out = session.openWrite(dish.id + ".apk", 0, apk.length)) {
                out.write(apk);
                session.fsync(out);
            }
            Intent result = new Intent(ACTION_RESULT)
                    .setPackage(ctx.getPackageName())
                    .putExtra("dish", dish.name);
            PendingIntent pending = PendingIntent.getBroadcast(
                    ctx, sessionId, result,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            session.commit(pending.getIntentSender());
        }
    }
}
