object Config {

   val androidModules = listOf("auth", "auth-github")
    val sampleModules = listOf("app")

    object Plugins {
        const val android = "com.android.tools.build:gradle:3.5.0"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.50"
        const val google = "com.google.gms:google-services:4.3.1"
    }

    object Dependencies {
        object Kotlin {
            const val jvm = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.50"
        }

        object Support {

            const val mediaCompat = "androidx.media:media:1.1.0"
        }

        object Arch {

            const val runtime = "androidx.lifecycle:lifecycle-runtime:2.1.0"
            const val viewModel = "androidx.lifecycle:lifecycle-viewmodel:2.1.0"

        }

        object Firebase {
        }

        object PlayServices {
        }


        object Provider {

        }

        object Misc {

        }

        object Test {
        }
    }
}
