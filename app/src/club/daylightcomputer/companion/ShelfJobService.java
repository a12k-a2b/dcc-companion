package club.daylightcomputer.companion;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The butler's rounds: every few hours (only when the tablet is online),
 * quietly fetch the shelf and compare it with the last shelf this tablet
 * saw. New dish → a warm notification. New version → an update note.
 * Dish gone → a recall note, with what to do. Then remember the new shelf.
 */
public class ShelfJobService extends JobService {

    private static final int JOB_ID = 1;
    private static final String CHANNEL = "club";
    private Thread work;

    public static void schedule(Context ctx) {
        JobScheduler js = (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js.getPendingJob(JOB_ID) != null) return;
        js.schedule(new JobInfo.Builder(JOB_ID,
                new ComponentName(ctx, ShelfJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(6 * 60 * 60 * 1000)  // a few rounds a day is plenty
                .setPersisted(true)
                .build());
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        work = new Thread(() -> {
            try {
                checkShelf(this);
            } catch (Exception ignored) {
                // Offline or the club is unreachable — next rounds will catch up.
            }
            jobFinished(params, false);
        });
        work.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (work != null) work.interrupt();
        return true;
    }

    static void checkShelf(Context ctx) throws Exception {
        String freshJson = new String(Catalog.get(Catalog.SHELF_URL), "UTF-8");
        List<Catalog.Dish> fresh = Catalog.parse(freshJson);
        String oldJson = Catalog.loadSnapshot(ctx);
        Catalog.saveSnapshot(ctx, freshJson);
        if (oldJson == null) return; // first look at the shelf — nothing to compare

        Map<String, Catalog.Dish> before = new HashMap<>();
        for (Catalog.Dish d : Catalog.parse(oldJson)) before.put(d.id, d);

        for (Catalog.Dish d : fresh) {
            Catalog.Dish prev = before.remove(d.id);
            if (prev == null) {
                notify(ctx, d.id.hashCode(),
                        d.author + " brought a dish: " + d.name,
                        d.tagline + " — tap to see it on the shelf.");
            } else if (!prev.version.equals(d.version)) {
                notify(ctx, d.id.hashCode(),
                        d.name + " got better",
                        d.author + " updated it to " + d.version
                        + ". Tap to get the new version.");
            }
        }

        // Anything left in `before` has been pulled from the shelf.
        for (Catalog.Dish gone : before.values()) {
            boolean installed = Inspector.installedVersion(ctx, gone.pkg) != null;
            notify(ctx, gone.id.hashCode(),
                    gone.name + " was pulled from the shelf",
                    installed
                        ? "You have it installed. To remove it: Settings → Apps → "
                          + gone.name + " → Uninstall. The club site has the details."
                        : "You don't have it installed — nothing to do.");
        }
    }

    private static void notify(Context ctx, int id, String title, String text) {
        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL,
                "Club news", NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        nm.notify(id, new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
                .setContentTitle(title)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build());
    }
}
