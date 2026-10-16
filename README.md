# Z Launcher

Launcher Minecraft Bedrock untuk Android. Package: `com.sally.zlauncher`.

> Hanya untuk pemilik Minecraft Bedrock yang sah. Launcher memeriksa bahwa game terpasang dari Google Play.

## Status

| Tahap | Isi | Status |
| --- | --- | --- |
| A | UI, tema, daftar versi, impor `.apk` / `.apks`, hapus versi | Selesai |
| B (awal) | Cek lisensi, ekstraksi library `arm64-v8a`, buka Minecraft resmi | Selesai |
| B (inti) | Game host: Activity + kelas Java yang diharapkan `libminecraftpe.so`, lalu `System.load` | **Belum** |
| C | Sistem mod (library C++ + hook, manifest mod) | Belum |

Versi hasil impor saat ini hanya sampai "library native siap". Menjalankan game dari versi impor
membutuhkan Tahap B (inti). Entri "Minecraft (terpasang)" sudah bisa dibuka lewat aplikasi resmi.

## Build di GitHub (tanpa komputer)

1. Buat repo kosong di GitHub, unggah isi folder ini (termasuk `.github/workflows/build.yml`).
2. Buka tab **Actions**. Build jalan otomatis setiap push, atau tekan **Run workflow**.
3. Selesai, unduh artifact `ZLauncher-apk` lalu pasang APK-nya.
4. Rilis bernomor: buat tag `v1.0.0`. Versi aplikasi mengikuti tag, dan APK masuk ke Releases.

Workflow memakai Gradle yang dipasang CI, jadi `gradlew` tidak diperlukan. APK debug ditandatangani
otomatis, tidak perlu keystore atau Firebase.

## Mengubah style

- Warna: `app/src/main/res/values/colors.xml`
- Nama aplikasi dan teks: `app/src/main/res/values/strings.xml`
- Tema: `app/src/main/res/values/themes.xml`
- Ikon: `app/src/main/res/drawable/ic_launcher_foreground.xml` (huruf Z berbentuk vektor)

## Struktur kode

```
app/src/main/java/com/sally/zlauncher/
├── MainActivity.kt          UI utama, impor, hapus, luncurkan
├── core/
│   ├── ApkImporter.kt       impor .apk/.apks, validasi paket dan arm64
│   ├── PlayStoreValidator.kt  cek Minecraft terpasang dari Google Play
│   ├── NativeRuntime.kt     ekstraksi library native (Tahap B awal)
│   ├── LaunchController.kt  aturan peluncuran
│   └── LaunchLog.kt         log sederhana
├── data/                    GameVersion, VersionStore
└── ui/VersionAdapter.kt     daftar versi
```

## Langkah berikutnya (Tahap B inti)

Kelas yang dibutuhkan `libminecraftpe.so` (misalnya `com.mojang.minecraftpe.MainActivity`) harus
tersedia di proses launcher sebelum `NativeRuntime.loadLibraries()` dipanggil. LeviLaunchroid
(Apache-2.0) menyelesaikannya lewat modul terpisah; jika kamu mengambil kode dari sana, patuhi
lisensinya (kredit dan `NOTICE`).

## Catatan

- Kode ini belum pernah dikompilasi oleh pembuatnya (lingkungan tanpa akses Maven). Jika build
  pertama di Actions gagal, kirim log error-nya dan perbaiki dari situ.
- Hanya `arm64-v8a`, minSdk 28.
