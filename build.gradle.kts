plugins {
    id("com.android.application") version "9.0.1" apply false
    id("org.jetbrains.kotlin.android") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    // KSP 2.3.2 es la versión alineada con AGP 9.0.x en esta máquina.
    id("com.google.devtools.ksp") version "2.3.2" apply false
}
