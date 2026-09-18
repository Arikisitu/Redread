# RedRead

<p align="center">
  <strong>A modern, privacy-conscious manga reader for Android.</strong>
</p>

<p align="center">
  Read. Discover. Repeat.
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#requirements">Requirements</a> •
  <a href="#building">Building</a> •
  <a href="#license">License</a>
</p>

---

## About

**RedRead** is an Android manga reader focused on a clean reading experience, source management, customization, and convenient library organization.

The project is based on the open-source **Futon** Android application and extends its foundation with RedRead-specific branding, interface customization, source-access controls, and other modifications.

> RedRead is an independent project and is not affiliated with the original Futon project or its contributors.

---

## Features

### 📚 Manga Reading

- Clean and focused manga reading experience
- Library management
- Reading history
- Progress tracking
- Chapter navigation
- Configurable reader experience

### 🔎 Discovery & Sources

- Manga source management
- Source catalog
- Source search
- Enable or disable individual sources
- Source filtering and management tools

### 🔐 Source Access Protection

RedRead includes an optional protection layer for source-related functionality.

- Protected source access
- Password-protected source unlocking
- Persistent unlock state
- Manual source locking
- Protected catalog access
- Protected direct navigation

Source Access protection is separate from the application's existing App Lock functionality.

### 🎨 Customization

- Dark-focused interface
- Custom RedRead visual identity
- Configurable color themes
- Custom launcher icon
- Custom splash screen
- Android system theme support

### ⚙️ Settings

- Reader customization
- Appearance settings
- Source management
- Application preferences
- Reading and library options

---

## Screenshots

> Screenshots will be added as the interface continues to evolve.

<!--
Add screenshots here:

![Home](screenshots/home.png)

![Reader](screenshots/reader.png)

![Sources](screenshots/sources.png)

![Settings](screenshots/settings.png)
-->

---

## Requirements

| Requirement | Version |
|---|---|
| Android | Android 6.0+ |
| Minimum SDK | 23 |
| Compile SDK | 36 |
| Language | Kotlin |
| Build System | Gradle |
| IDE | Android Studio |

---

## Building

Clone the repository and open the project in Android Studio.

Build a debug APK with:

```bash
./gradlew assembleDebug
