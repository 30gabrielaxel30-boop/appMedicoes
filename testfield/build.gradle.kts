// Aplicativo auxiliar usado apenas nos testes instrumentados: fornece campos de texto
// comuns e protegidos em OUTRO pacote, para validar o teclado como em um app real.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.keyhistory.testfield"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.keyhistory.testfield"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
