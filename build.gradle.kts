import com.android.build.gradle.BaseExtension

tasks.withType<Wrapper> {
    gradleVersion = Versions.gradleLatestVersion
    distributionType = Wrapper.DistributionType.ALL
}

buildscript {
    repositories {
        google()
        jcenter()
        mavenLocal()
    }

    dependencies {
        classpath(Config.Plugins.android)
        classpath(Config.Plugins.kotlin)
        classpath(Config.Plugins.google)
    }
}

plugins {
    id("com.gradle.build-scan") version "2.4.1"
    id("de.fayard.buildSrcVersions") version "0.4.2"
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    publishAlways()
}

subprojects { parent!!.path.takeIf { it != rootProject.path }?.let { evaluationDependsOn(it) } }

allprojects {
    repositories {
        google()
        jcenter()
        mavenLocal()
    }
}


subprojects {
    val isAndroidModule = project.name in Config.androidModules
    val isSample = project.name in Config.sampleModules

    if (isAndroidModule) apply(plugin = "com.android.library")
    else if (isSample) apply(plugin = "com.android.application")


    if (isSample || isAndroidModule) {
        configure<BaseExtension> {
            compileSdkVersion(28)

            defaultConfig {
                minSdkVersion(16)
                targetSdkVersion(28)

                versionName = "5.1.0"
                versionCode = 1

//                resourcePrefix("fui_")
//                vectorDrawables.useSupportLibrary = true
            }

            lintOptions {
                disable(
                        "ObsoleteLintCustomCheck", // ButterKnife will fix this in v9.0
                        "IconExpectedSize",
                        "InvalidPackage", // Firestore uses GRPC which makes lint mad
                        "NewerVersionAvailable", "GradleDependency", // For reproducible builds
                        "SelectableText", "SyntheticAccessor" // We almost never care about this
                )

                isCheckAllWarnings = true
                isWarningsAsErrors = true
                isAbortOnError = true
            }
        }
    }

}