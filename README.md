# Club Companion

The [Daylight Computer Club](https://daylightcomputer.club)'s own dish: a
small native app for the DC-1 that watches the shelf so you don't have to.

It is three friends in one trench coat:

- **The butler** — checks the shelf a few times a day. A friend brings a
  dish → a quiet notification. A dish gets better → an update note. A dish
  is pulled → a recall note with what to do. One-tap installs: it downloads
  the dish and hands it to Android's own installer (Android still asks you
  to confirm — the Companion never installs anything silently).
- **The inspector-at-home** — looks at what's actually installed and
  enabled on the tablet and explains in plain words where dishes might
  fight (two accessibility services in a line, two overlays on one
  screen), with a button to the exact Settings page where you choose.
- **The reporter** — when a dish misbehaves, one tap builds a structured
  report written to be pasted into the cook's Claude session, and hands it
  to the share sheet. Trouble travels back to the kitchen carrying exactly
  what the cook's tools need.

Privacy is structural: everything the Companion sees stays on the tablet.
No accounts, no analytics, no server of its own — its only conversation is
GET-ing the public shelf.

## Building

`./build.sh` — no Gradle; plain `aapt2` + `javac` + `d8` against the
Android SDK (same pattern as dc1-keys). Signs with the club key from the
club repo (`signing/dcc.keystore`, password public by design — see the
club's signing/README).

## Where it fits

The design lives in the club repo's MECHANICS.md ("Move 3 — the Club
Companion"). This is v0: butler + first collisions + reporter. v1 grows
richer collision detection and the invite-link identity.
