android {
    lintOptions {
        disable("UnknownNullness") // TODO fix in future PR
    }
}

dependencies {
    compileOnly(project(":auth")) { isTransitive = false }
    compileOnly(Libs.firebase_auth) { isTransitive = false }

    implementation(Libs.appcompat)
    implementation(Libs.browser)

    implementation(Libs.retrofit)
    implementation(Libs.converter_gson)
}
