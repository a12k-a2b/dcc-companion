package club.daylightcomputer.companion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

/**
 * Hears back from Android's installer. When Android wants the human's
 * confirmation (it always does, by design), this forwards its
 * confirmation screen to the front.
 */
public class InstallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE);
        String dish = intent.getStringExtra("dish");

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) {
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(confirm);
            }
        } else if (status == PackageInstaller.STATUS_SUCCESS) {
            Toast.makeText(ctx, dish + " is on your tablet ☀", Toast.LENGTH_LONG).show();
        } else {
            String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            Toast.makeText(ctx, "Install didn't finish: "
                    + (msg == null ? "status " + status : msg), Toast.LENGTH_LONG).show();
        }
    }
}
