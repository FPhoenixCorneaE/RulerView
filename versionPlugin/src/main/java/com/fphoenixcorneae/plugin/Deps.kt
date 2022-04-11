package com.fphoenixcorneae.plugin

object Deps {

    object FPhoenixCorneaE {
        const val commonUtil = "com.github.FPhoenixCorneaE:CommonUtil:1.0.7"
    }

    object BuildType {
        const val Debug = "debug"
        const val Release = "release"
    }

    object AndroidX {
        const val appcompat = "androidx.appcompat:appcompat:1.2.0"
        const val material = "com.google.android.material:material:1.3.0"
        const val coreKtx = "androidx.core:core-ktx:1.3.2"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.0.4"
    }

    object Kotlin {
        const val testJunit = "org.jetbrains.kotlin:kotlin-test-junit:1.6.10"
        const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1"
        const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.1"
    }

    object Log {
        /** logger */
        const val logger = "com.orhanobut:logger:2.2.0"
    }

    object Test {
        const val junit = "junit:junit:4.12"
        const val junitExt = "androidx.test.ext:junit:1.1.2"
        const val espresso = "androidx.test.espresso:espresso-core:3.3.0"
    }
}