# NextYear
Hand-drawn plant journal: mood sheet, year grid of doodles, haptics, fullscreen.

Build: push to GitHub -> Actions "Build APK" -> download artifact `NextYear-debug`.
Local: `gradle assembleDebug` (Gradle 8.7, JDK 17, Android SDK 34).

## Widget
"NextYear Clock": 3x2, borderless, 28dp corners, digits made of tiny hand-drawn plants.
Add it with the clock+ button (bottom right of the app) or long-press home screen > Widgets.
Ticks every minute; allow "Alarms & reminders" for NextYear on Android 12+ for on-the-minute updates.
