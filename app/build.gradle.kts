plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.exemplo.ocrflutuante"; compileSdk = 35
 defaultConfig { applicationId = "com.exemplo.ocrflutuante"; minSdk = 26; targetSdk = 34; versionCode = 1; versionName = "1.0" } }
dependencies { implementation("com.google.mlkit:text-recognition:16.0.1") }