import com.fphoenixcorneae.plugin.Android
import com.fphoenixcorneae.plugin.Deps

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("com.github.dcendents.android-maven")
    id("com.fphoenixcorneae.plugin")
}
group = "com.github.FPhoenixCorneaE"

android {
    compileSdkVersion(Android.compileSdkVersion)
    buildToolsVersion(Android.buildToolsVersion)

    defaultConfig {
        minSdkVersion(Android.minSdkVersion)
        targetSdkVersion(Android.targetSdkVersion)
        versionCode = Android.versionCode
        versionName = Android.versionName

        setConsumerProguardFiles(listOf("consumer-rules.pro"))
    }

    buildTypes {
        getByName(Deps.BuildType.Release) {
            // 执行proguard混淆
            isMinifyEnabled = false
            // Zipalign优化
            isZipAlignEnabled = true
            // 移除无用的resource文件
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName(Deps.BuildType.Debug) {
            // 执行proguard混淆
            isMinifyEnabled = false
            // Zipalign优化
            isZipAlignEnabled = true
            // 移除无用的resource文件
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    sourceSets {
        val main by getting
        main.java.srcDirs("src/main/kotlin")
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    dexOptions {
        jumboMode = true
    }

    lintOptions {
        isCheckReleaseBuilds = false
        isAbortOnError = false
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    // kotlin
    compileOnly(Deps.Kotlin.stdlib)
    compileOnly(Deps.AndroidX.coreKtx)
    compileOnly(Deps.AndroidX.appcompat)
    compileOnly(Deps.FPhoenixCorneaE.commonUtil)
}

// 添加以下配置，否则上传后的jar包看不到注释-------------------------------------------------------------

// 指定编码
tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}
// 打包源码
task("sourcesJar", Jar::class) {
    from("src/main/kotlin")
    classifier = "sources"
}
task("javadoc", Javadoc::class) {
    isFailOnError = false
    source = fileTree(mapOf("dir" to "src/main/kotlin", "include" to listOf("*.*")))
    classpath += project.files(android.bootClasspath.joinToString(File.pathSeparator))
    classpath += configurations.compile
}
// 制作文档(Javadoc)
task("javadocJar", Jar::class) {
    classifier = "javadoc"
    val javadoc = tasks.getByName("javadoc") as Javadoc
    from(javadoc.destinationDir)
}
artifacts {
    val sourcesJar = tasks.getByName("sourcesJar")
    val javadocJar = tasks.getByName("javadocJar")
    archives(sourcesJar)
    archives(javadocJar)
}