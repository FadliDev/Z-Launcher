# Aturan R8 untuk Z Launcher.
# Saat Tahap B (JNI / game host) ditambahkan, jaga kelas yang dipanggil dari native:
# -keep class com.sally.zlauncher.host.** { *; }
-keepattributes SourceFile,LineNumberTable
