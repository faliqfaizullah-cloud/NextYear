# NextYear
Hand-drawn plant journal: mood sheet, year grid of doodles, haptics, fullscreen.

Build: push to GitHub -> Actions "Build APK" -> download artifact `NextYear-debug`.
Local: `gradle assembleDebug` (Gradle 8.7, JDK 17, Android SDK 34).

## Widget
"NextYear Clock": 3x2, borderless, 28dp corners, digits made of tiny hand-drawn plants.
Add it with the clock+ button (bottom right of the app) or long-press home screen > Widgets.
Ticks every minute; allow "Alarms & reminders" for NextYear on Android 12+ for on-the-minute updates.

## Camera
Tap the photo card on Today to open the camera (long-press to pick from gallery).
Flow: viewfinder (close, flash, 1x/2x, scribble shutter, flip) -> review card (X retake, check keep).

## More widgets
Year of growth (4x4), Day (2x2: #day, mood flower, weekday, date), Camera (1x1). All 28dp, borderless.
Add them from the clock+ button in the app (bottom right) or the launcher widget list.

## Photo pile
Every kept photo is saved to filesDir/photos. On Today they form a stack (newest on top): swipe left/right to browse,
tap the stack to take another, long-press for gallery / delete.

## Day widget (swipe)
2x2 stack: swipe up/down for today, yesterday, and 30 days back. Tap today's card -> camera; tap an older card -> opens that day.
Photos and notes are stored per day. In the app, use the arrows beside the "Today" label to move between days.
Widget panels use a 28dp layout background; content is drawn at its own aspect and centered, so it never stretches.
