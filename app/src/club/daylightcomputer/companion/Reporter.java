package club.daylightcomputer.companion;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

/**
 * The reporter: when a dish misbehaves, one tap builds a structured
 * report written to be pasted into the cook's Claude session — so the
 * problem travels back to the kitchen carrying exactly what the cook's
 * tools need. It leaves the tablet only when the friend chooses to send
 * it, through the ordinary share sheet.
 */
public class Reporter {

    public static void send(Context ctx, List<Catalog.Dish> shelf,
                            List<Inspector.Finding> findings) {
        StringBuilder r = new StringBuilder();
        r.append("Hi! A dish from the Daylight Computer Club is giving me trouble ");
        r.append("on my tablet. Below is the report my Club Companion put together — ");
        r.append("please read it and help me fix the dish (its source repo is linked ");
        r.append("on its card at ").append(Catalog.CLUB).append("). If you can, ");
        r.append("reshelve the fixed version so everyone gets the update.\n\n");

        r.append("WHAT I NOTICED (the person filling this in describes it here):\n");
        r.append("> \n\n");

        r.append("--- Companion report ---\n");
        r.append("Tablet: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
         .append(", Android ").append(Build.VERSION.RELEASE)
         .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");

        r.append("\nClub dishes on this tablet:\n");
        for (Catalog.Dish d : shelf) {
            if (d.pkg == null) continue;
            String v = Inspector.installedVersion(ctx, d.pkg);
            if (v == null) continue;
            r.append("- ").append(d.name).append(" (").append(d.pkg).append(") ")
             .append("installed ").append(v)
             .append(", shelf has ").append(d.version).append("\n");
        }

        r.append("\nCollisions the inspector sees right now:\n");
        if (findings.isEmpty()) {
            r.append("- none — dishes are getting along\n");
        } else {
            for (Inspector.Finding f : findings) {
                r.append("- ").append(f.title).append(": ").append(f.explanation).append("\n");
            }
        }
        r.append("--- end of report ---\n");

        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, "Club dish trouble report")
                .putExtra(Intent.EXTRA_TEXT, r.toString());
        ctx.startActivity(Intent.createChooser(send, "Send the report to the cook"));
    }
}
