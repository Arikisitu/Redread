# 🌹 RedRead

<p align="center">
  <img src="app/src/main/res/drawable-nodpi/redread_launcher.png" width="110" alt="RedRead Logo">
</p>

<h3 align="center">Read. Discover. Repeat.</h3>

<p align="center">
  A modern Android manga reader focused on a clean reading experience,
  source management, customization, and privacy-conscious controls.
</p>

<p align="center">
  <a href="#-features">Features</a> •
  <a href="#-screenshots">Screenshots</a> •
  <a href="#-installation">Installation</a> •
  <a href="#-building">Building</a> •
  <a href="#-credits">Credits</a>
</p>

<p align="center">

![Android](https://img.shields.io/badge/Android-6.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Material](https://img.shields.io/badge/Material%20Design-3-757575?style=for-the-badge&logo=materialdesign&logoColor=white)
![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)

</p>

---

## ✨ What is RedRead?

**RedRead** is an Android manga reader designed around a simple idea:

> **A reader should stay out of the way of the story.**

It combines manga discovery, source management, library organization, reading progress, and a customizable interface in one Android application.

RedRead is built on the open-source **Futon** Android project and contains modifications, redesigns, and additional functionality developed for RedRead.

> ⚠️ RedRead is an independent project and is not affiliated with the original Futon project or its contributors.

---

# 🎨 Features

<table>
<tr>
<td width="50%">

### 📖 Manga Reader

- Clean reading interface
- Chapter navigation
- Reading progress
- History tracking
- Library organization
- Configurable reading experience

</td>

<td width="50%">

### 🔎 Discovery

- Manga source management
- Source catalog
- Source searching
- Enable / disable sources
- Source filtering
- Catalog navigation

</td>
</tr>

<tr>
<td width="50%">

### 🔐 Source Protection

- Protected source access
- Password-based unlocking
- Persistent unlock state
- Manual source locking
- Protected catalog navigation
- Separate from App Lock

</td>

<td width="50%">

### 🎨 Personalization

- Dark-focused interface
- Custom RedRead branding
- Theme customization
- Custom launcher icon
- Custom splash screen
- Android system theme support

</td>
</tr>
</table>

---

# 📱 Screenshots

<p align="center">
  <img src="docs/screenshots/home.png" width="30%" alt="RedRead Home">
  <img src="docs/screenshots/library.png" width="30%" alt="RedRead Library">
  <img src="docs/screenshots/reader.png" width="30%" alt="RedRead Reader">
</p>

<p align="center">
  <img src="docs/screenshots/catalog.png" width="30%" alt="RedRead Catalog">
  <img src="docs/screenshots/sources.png" width="30%" alt="RedRead Sources">
  <img src="docs/screenshots/settings.png" width="30%" alt="RedRead Settings">
</p>

---

# 🧭 Experience

### 🏠 Home

Keep your reading activity organized and quickly return to the manga you're following.

### 📚 Library

Your reading collection in one place, with progress and history helping you continue where you left off.

### 🔍 Catalog

Explore available manga sources and discover new titles.

### 🧩 Sources

Manage available sources from a dedicated source-management interface.

### 📖 Reader

A focused reading experience designed to keep the interface minimal while reading.

### ⚙️ Settings

Control appearance, reader behavior, source configuration, and application preferences.

---

# 🔐 Source Access

RedRead includes a dedicated protection layer for source-related functionality.

When Source Access is locked:

```text
Sources
   ↓
🔒 Locked
   ↓
Password
   ↓
🔓 Unlocked
   ↓
Source Catalog
