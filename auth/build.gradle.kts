import com.android.build.gradle.internal.dsl.TestOptions
import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

apply(plugin = "kotlin-android")
apply(plugin = "kotlin-android-extensions")
apply(plugin = "kotlin-kapt")

android {

    lintOptions {
        disable("UnusedQuantity")
        disable("UnknownNullness")  // TODO fix in future PR
        disable("TypographyQuotes") // Straight versus directional quotes
    }

    testOptions {
        unitTests(closureOf<TestOptions.UnitTestOptions> {
            isIncludeAndroidResources = true
        })
    }
}


dependencies {
    implementation(Libs.material)
    implementation(Libs.browser)
    implementation(Libs.constraintlayout)
    implementation(Libs.annotation)
    implementation(Libs.appcompat)
    implementation(Libs.library)
    implementation(Libs.vvalidator)
    implementation(Libs.lifecycle_extensions)
    annotationProcessor(Libs.lifecycle_compiler)

    api(Libs.firebase_auth)
    api(Libs.play_services_auth)

    compileOnly(Libs.facebook_login)
    implementation(Libs.cardview) // Needed to override Facebook
    compileOnly(Libs.twitter_core) { isTransitive = true }

    testImplementation(Libs.junit)
    testImplementation(Libs.truth)
    testImplementation(Libs.mockito_android)
    testImplementation(Libs.robolectric)
    testImplementation(Libs.facebook_login)
    testImplementation(Libs.twitter_core) { isTransitive = true }
}
