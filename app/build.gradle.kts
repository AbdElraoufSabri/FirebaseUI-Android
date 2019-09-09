// NOTE: this project uses Gradle Kotlin DSL. More common build.gradle instructions can be found in
// the main README.

android {
    defaultConfig {
        multiDexEnabled = true
    }

    lintOptions {
        disable("ResourceName", "MissingTranslation")
    }
}

dependencies {

    implementation(project(":auth"))
    implementation(project(":auth-github"))

    implementation(Libs.material)
    implementation(Libs.multidex)
    implementation(Libs.facebook_login)
    // Needed to override Facebook
    implementation(Libs.cardview)
    implementation(Libs.recyclerview)
    implementation(Libs.twitter_core) { isTransitive = true }

    implementation(Libs.glide)
    annotationProcessor(Libs.com_github_bumptech_glide_compiler)

    // Used for FirestorePagingActivity
    implementation(Libs.paging_runtime)
    implementation(Libs.browser)

    // The following dependencies are not required to use the Firebase UI materialProgress.
    // They are used to make some aspects of the demo app implementation simpler for
    // demonstrative purposes, and you may find them useful in your own apps; YMMV.
    implementation(Libs.easypermissions)
    implementation(Libs.butterknife)
    implementation(Libs.constraintlayout)
    annotationProcessor(Libs.butterknife_compiler)
    debugImplementation(Libs.leakcanary_android)
    debugImplementation(Libs.leakcanary_support_fragment)
    releaseImplementation(Libs.leakcanary_android_no_op)
    testImplementation(Libs.leakcanary_android_no_op)
}

apply(plugin = "com.google.gms.google-services")
