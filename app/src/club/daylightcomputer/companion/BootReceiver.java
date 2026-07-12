package club.daylightcomputer.companion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Puts the butler back on its rounds after a reboot. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            ShelfJobService.schedule(ctx);
            CounterJobService.schedule(ctx);
        }
    }
}
