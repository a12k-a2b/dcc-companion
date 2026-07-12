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
 * The counter watcher: while the tablet is charging (battery-kind — the
 * dev loop happens at a desk anyway), glance at the owner's tasting
 * counter every quarter hour. A dish newly set down, or set down again
 * fresh, gets a soft knock: tap to taste it full-screen in the Clubhouse.
 * Does nothing at all on tablets that never paired a counter.
 */
public class CounterJobService extends JobService {

    private static final int JOB_ID = 2;
    private static final String CHANNEL = "counter";
    private Thread work;

    public static void schedule(Context ctx) {
        JobScheduler js = (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js.getPendingJob(JOB_ID) != null) return;
        js.schedule(new JobInfo.Builder(JOB_ID,
                new ComponentName(ctx, CounterJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresCharging(true)
                .setPeriodic(15 * 60 * 1000)  // JobScheduler's floor; fine for a counter
                .setPersisted(true)
                .build());
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        work = new Thread(() -> {
            try {
                checkCounter(this);
            } catch (Exception ignored) {
                // Offline, or no counter paired — nothing to do.
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

    /** Compare the live counter to the last one seen; knock for anything new. */
    static void checkCounter(Context ctx) throws Exception {
        String url = Counter.configUrl(ctx);
        if (url == null) return;

        String freshJson = new String(Catalog.get(url), "UTF-8");
        List<Counter.Dish> fresh = Counter.parse(freshJson);
        String oldJson = Counter.loadSnapshot(ctx);
        Counter.saveSnapshot(ctx, freshJson);
        if (oldJson == null) return; // first look — start quiet

        Map<String, String> before = new HashMap<>();
        for (Counter.Dish d : Counter.parse(oldJson)) before.put(d.id, d.added);

        for (Counter.Dish d : fresh) {
            String prevAdded = before.get(d.id);
            boolean isNew = prevAdded == null;
            boolean freshened = prevAdded != null && !prevAdded.equals(d.added);
            if (isNew || freshened) {
                notify(ctx, ("counter-" + d.id).hashCode(),
                        isNew ? "On your counter: " + d.name
                              : d.name + " is fresh again",
                        (d.note != null && !d.note.isEmpty() ? d.note + " — " : "")
                                + "tap to taste it.",
                        d.url);
            }
        }
    }

    private static void notify(Context ctx, int id, String title, String text, String url) {
        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL,
                "The tasting counter", NotificationManager.IMPORTANCE_DEFAULT));
        Intent taste = new Intent(ctx, DishWebActivity.class)
                .putExtra("url", url)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent open = PendingIntent.getActivity(ctx, id, taste,
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
