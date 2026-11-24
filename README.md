# Workout Log (Android)

A digital version of a paper workout & diet journal: every box from the paper pages
(workout log, diet log, goals, body measurements, before/after photos and the 33-week
progress tracker) plus **graphs** and **PDF reports** of your progress over time.

Two versions are built from the same code:

| APK | Package | Default look |
|---|---|---|
| `apk/WorkoutLog-Mens.apk` | `com.workoutlog.app.mens` | "Iron" – dark charcoal, steel blue, ember orange, bold uppercase headings |
| `apk/WorkoutLog-Womens.apk` | `com.workoutlog.app.womens` | "Bloom" – blush, rose and plum, serif headings, rounded cards |

Both can be installed side by side, and either can switch theme (and light/dark) in Settings.

## Install on your phone
1. Copy the APK to the phone (or download it there).
2. Tap it; Android will ask to allow installing from that app ("Install unknown apps") – allow it.
3. Requires Android 8.0 or newer.

## What's in the app
- **Log** tab – pick a day (arrows / calendar / Today). Two pages, matching the paper:
  - *Workout*: workout name, warm-up / cool-down, water, sleep, weight, duration,
    exercises with 6 sets of weight × reps, cardio (time / distance / calories), notes,
    rate-your-workout 20–100 %. "Copy last" repeats your previous workout.
  - *Diet*: mood faces, energy & activity 1–5 boxes, body fat %, caffeine, fruit, veggies,
    sugar/gluten/lactose/dairy-free checkboxes, breakfast/lunch/dinner/snacks with
    product, serving, calories, protein, fat, carbs and a nameable extra column (default "Sugar"),
    automatic subtotals, totals and "vs. goal".
- **Goals** – goal, start, deadline (with days-left countdown), action plan, achievement, done.
- **Body** – before/after photos with dates, measurements (neck, chest, biceps, waist, hips,
  thigh, calf, weight, body fat + 2 custom rows) shown as a table like the paper.
- **Tracker** – any number of "item to track" grids for 33 weeks, dated from a start week.
- **Graphs** – body weight, every measurement, per-exercise heaviest set / estimated 1-rep max /
  volume, nutrition vs goal, sleep, water, mood, energy, workout rating, cardio, tracker items;
  30 days / 90 days / 1 year / all time.
- **PDF** (top-right icon) – choose a date range and sections; creates a printable report
  (summary, goals, measurements + photos, tracker, graphs, day-by-day workout & diet tables)
  that you can open, save to Downloads or share.
- **Settings** – theme, units (lbs/kg, in/cm, mi/km, oz/L), daily nutrition goals,
  custom measurement names, backup export/restore (JSON).

Data is stored privately on the phone only. Use Settings → Export backup regularly.

## Build it yourself (Linux)
```bash
# needs JDK 17+ and the Android SDK (platform 35, build-tools 35)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
./gradlew assembleMensRelease assembleWomensRelease   # APKs in app/build/outputs/apk/
./gradlew testMensDebugUnitTest                       # renders screenshots + sample PDF pages
```
Release builds are signed with the local debug key so they can be sideloaded. A build made on a
different computer has a different key, so Android will refuse to install it *over* the old one –
export a backup first, uninstall, install, restore.

Screenshots: see `docs/screenshots/`.
