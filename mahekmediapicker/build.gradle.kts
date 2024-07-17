plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    `maven-publish`
}

android {
    namespace = "com.mahek.imagepicker"
    compileSdk = 34

    defaultConfig {
        minSdk = 27

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    publishing{
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}


publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.mahek.imagepicker"
            artifactId = "library"
            version = "1.0"

            afterEvaluate {
                from(components["release"])
            }

        }

       /* repositories {
            maven {
                name = "MahekMediaPicker"
                url = uri("https://maven.pkg.github.com/Mahekdabhi/MahekMediaPicker")

            }
        }*/

    }
}

/*afterEvaluate{
    android.libraryVariants.forEach{libraryVariant ->
        publishing.publications.create(libraryVariant.name,MavenPublication){
            from components.findByName(libraryVariant.name)

            groupId = "com.mahek.imagepicker"
            artifactId = "imagepicker"
            version = "1.0"
        }
    }
}*/
dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    api(libs.ucrop)
}