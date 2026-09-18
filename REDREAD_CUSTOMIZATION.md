# RedRead customization

App name/tagline:
- `app/src/main/res/values/strings.xml`
- `app_name`: RedRead
- `app_tagline`: Read. Discover. Repeat.

Splash:
- `app/src/main/res/drawable/redread_splash.xml`
- Android 12+ splash themes point to it.
- Replace this vector later with the final splash logo.

Launcher icon:
- `app/src/main/AndroidManifest.xml` uses `@mipmap/ic_launcher`.
- Adaptive icon foreground: `app/src/main/res/drawable/redread_launcher_foreground.xml`.
- Legacy fallback: `app/src/main/res/drawable-nodpi/redread_launcher.png`.

Default RedRead palette:
- Background: #0D0D0D
- Burgundy: #5A0F18
- Accent red: #F0443E
- Cream: #FFF1DC
- Muted beige: #BFA99A

Default color resources:
- `app/src/main/res/values/colors_futon.xml`
- `app/src/main/res/values-night/colors_futon.xml`
- `app/src/main/res/values/colors_themed.xml`
- `app/src/main/res/values-night/colors_themed.xml`

The existing internal `futon_*` and `totoro_*` resource names are retained for compatibility and are not user-facing.

Developer credit:
- `ARIK` is shown subtly on the main Settings screen.

Source Access:
- Fixed master password: `Arik`
- Existing SourceAccessManager + AppSettings.isSourceAccessUnlocked
- App Lock remains separate.
