// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

allprojects {
  //  repositories {
//        google()
//        mavenCentral()
//        maven {
//            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
//            credentials {
//                username = "mapbox"
//                password = "pk.eyJ1Ijoiam9zZXBoaW5lLXN0ZW5zZ2FhcmQiLCJhIjoiY21hY2NreWJ6MDFxMjJrcjA1YzNxa3FibCJ9.VQN_COHNfdKWyJ3sN1SwMQ"
//            }
//        }
//    }
}
