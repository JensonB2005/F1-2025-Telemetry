# F1 2025 Telemetry Viewer

An Android app that receives **F1 25 (EA/Codemasters) UDP telemetry** and shows
every available data point **live**, lets you **record and replay** whole
sessions, and **analyses your driving** — flagging mistakes and suggesting setup
changes.

> Works with the F1 25 UDP format (packet format 2025) and is backward-compatible
> with F1 22/23/24 formats, since the per-car block sizes are derived from each
> packet rather than hard-coded.

## Getting the APK

You don't need Android Studio. Every push builds an installable APK on GitHub
Actions:

1. Open the **Actions** tab → **Build APK** → latest run → download the
   **`F1-Telemetry-Viewer-APK`** artifact, **or** grab it from the
   **`latest-apk`** release on the Releases page.
2. Copy the `.apk` to your Android phone and open it. Allow "Install unknown
   apps" when prompted.

Minimum Android 8.0 (API 26).

## Setting up the game

On the same Wi-Fi network as your phone:

**F1 25 → Settings → Telemetry Settings**
- UDP Telemetry: **On**
- UDP Broadcast Mode: **Off** (then set IP) or **On** (sends to whole subnet)
- UDP IP Address: your **phone's** IP address (Broadcast Off)
- UDP Port: **20777** (the app default)
- UDP Send Rate: **20–60 Hz**
- UDP Format: **2025**

In the app open the **Connect** tab and tap **Start listening**.

## What you get

| Tab | Contents |
|-----|----------|
| **Dash** | Gear / speed / RPM / rev lights, throttle & brake bars, DRS, live lap & last/best times, sector splits, deltas, position, tyre, fuel, ERS, weather & temps. |
| **Traces** | Per-lap charts of speed (current vs your best lap), throttle/brake/steering, gear, lateral & longitudinal g-force, and RPM — plotted against lap distance. |
| **Car** | Four-corner tyre surface/core temps, pressures and wear; brake temps & damage; power unit (ERS store/deploy/harvest, engine temp, fuel); full damage breakdown. |
| **Analysis** | Detected **mistakes** (lock-ups, mid-corner throttle corrections, coasting, early upshifts, track-limit/invalid laps, biggest time loss vs best lap) and **setup suggestions** (tyre temps/pressures, wear balance, brake temps, understeer/oversteer tendency, damage, fuel), plus your current setup and a live event feed. |
| **Connect** | Live connection + port, session recording, and replay of saved sessions with 0.5×–4× speed. |

## Recording & replay

- On the **Connect** tab, start live telemetry, then tap **Record session**.
  Packets are saved to a `.f1rec` file in the app's private storage.
- Stop recording, then **Play** any saved recording to replay it packet-for-packet
  at your chosen speed. Every screen — including the analysis — works identically
  on replayed data, so you can review a session retroactively.

## How the analysis works

The analysis is a set of explainable heuristics, not a black box — each insight
points at a distance on track and says why:

- **Lock-ups**: full brake pressure with an abrupt speed collapse.
- **Throttle corrections**: throttle picked up then backed off while cornering
  (rear instability).
- **Coasting / early upshifts**: lost time from off-power zones or shifting below
  the power band.
- **Time loss**: bucketed speed comparison against your best lap to find the
  single biggest loss.
- **Setup**: tyre/brake temperature windows, front/rear temperature & wear
  balance, pressure targets, and a running understeer/oversteer estimate from
  steering input vs. lateral grip.

## Building locally (optional)

Requires the Android SDK.

```bash
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## Project layout

```
app/src/main/java/com/f1telemetry/viewer/
  telemetry/   ByteReader, packet structs, Constants, PacketParser
  net/         UDP receiver, session recorder & replayer
  data/        TelemetryRepository (state), MainViewModel, models
  analysis/    MistakeDetector, SetupAdvisor, HandlingEstimator
  ui/          Compose screens & components
```
