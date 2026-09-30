<div align="center">

# 🎵 Musik

**Pemutar musik lokal untuk Android. Minimalis, cepat, dan punya lirik yang ikut bergulir.**

![Android](https://img.shields.io/badge/Android-11%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Media3](https://img.shields.io/badge/Media3-ExoPlayer-FF6F00)
![Build](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF?logo=githubactions&logoColor=white)

</div>

---

## ✨ Fitur

| | |
|---|---|
| 🎨 **Material 3 minimalis** | Antarmuka bersih tanpa elemen berlebihan |
| 🌗 **Terang / Gelap AMOLED** | Ganti tema dengan satu ketukan: ikuti sistem → terang → hitam murni (`#000000`) |
| 📂 **Musik lokal** | Membaca semua lagu di perangkat lewat MediaStore |
| 📑 **Playlist** | Buat, hapus, dan atur playlist sendiri, tersimpan di perangkat |
| 🗂️ **Pengelompokan** | Telusuri berdasarkan **Album**, **Artis**, atau **Genre** |
| 📝 **Lirik tersinkron** | Diambil dari [LRCLIB](https://lrclib.net), bergulir otomatis, ketuk baris untuk melompat |
| 🧩 **Widget** | Widget layar utama dengan tombol prev, play/pause, dan next |
| 🔔 **Latar belakang** | Tetap memutar saat layar mati, lengkap dengan kontrol notifikasi |

## 🖼️ Tampilan

> Tambahkan tangkapan layar di folder `docs/` lalu tautkan di sini.

```
docs/
├── library.png
├── lyrics.png
└── widget.png
```

## 🚀 Cara Build

### Opsi 1: GitHub Actions (tanpa install apa pun)

1. Push proyek ini ke repo GitHub Anda.
2. Buka tab **Actions** → workflow **Build APK** (jalan otomatis, atau klik *Run workflow*).
3. Setelah selesai, unduh **`Musik-debug-apk`** dari bagian *Artifacts*.
4. Ekstrak, salin APK ke HP, lalu install (izinkan sumber tidak dikenal).

### Opsi 2: Android Studio

1. Buka folder proyek di Android Studio (Koala atau lebih baru).
2. Tunggu Gradle sync selesai.
3. Sambungkan perangkat atau emulator (Android 11+), lalu klik **Run ▶**.

## 🧱 Arsitektur

```
app/src/main/
├── java/com/example/musik/
│   ├── MainActivity.kt   # Tema, UI Compose, layar Sedang Diputar
│   ├── Service.kt        # PlaybackService (Media3) + PlayerWidget
│   └── Data.kt           # Song, MediaStore, Playlists, klien LRCLIB
└── res/
    ├── layout/widget_player.xml
    └── xml/widget_info.xml
```

| Lapisan | Teknologi |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Pemutaran | Media3 ExoPlayer + `MediaSessionService` |
| Data lagu | `MediaStore.Audio` |
| Playlist | `SharedPreferences` (JSON) |
| Lirik | LRCLIB REST API (format LRC) |
| Widget | `AppWidgetProvider` + `RemoteViews` |

## 🔐 Izin

| Izin | Kegunaan |
|---|---|
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | Membaca file musik |
| `INTERNET` | Mengambil lirik |
| `POST_NOTIFICATIONS` | Notifikasi kontrol pemutar |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Pemutaran di latar belakang |

## ℹ️ Catatan

- Minimal **Android 11 (API 30)**, karena info genre baru tersedia dari versi itu.
- Lirik hanya tampil jika LRCLIB memiliki data yang cocok dengan judul, artis, album, dan durasi lagu.
- APK dari workflow adalah build **debug**. Untuk rilis, tambahkan keystore lewat GitHub Secrets.

## 🗺️ Rencana

- [ ] Cover album
- [ ] Shuffle & repeat
- [ ] Cache lirik offline
- [ ] Pencarian lagu
- [ ] Antrean pemutaran
- [ ] Sleep timer
- [ ] Equalizer

## 🙏 Kredit

- [LRCLIB](https://lrclib.net) untuk API lirik tersinkron yang gratis dan terbuka
- [AndroidX Media3](https://developer.android.com/media/media3) dan [Jetpack Compose](https://developer.android.com/jetpack/compose)

---

<div align="center">Dibuat dengan ❤️ dan Kotlin</div>
