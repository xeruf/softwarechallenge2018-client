import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Arrays

version = "1.2.0"

plugins {
    java
    application
    kotlin("jvm") version "1.2.21"
    id("com.github.johnrengelman.shadow") version "2.0.1"
}

repositories { jcenter() }

dependencies {
    compile("xerus.util", "kotlin")
    compile("xerus.util", "utilities")
    compile(files(file("lib").listFiles()))
}

java.sourceSets.getByName("main").java.srcDir("src")

application {
    applicationName = "Jumper 1"
    mainClassName = "xerus.softwarechallenge.Starter"
}

val MAIN = "_main"
tasks {

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    "run"(JavaExec::class) {
        group = MAIN
        args = listOf("-d", "2")
    }

    "shadowJar"(ShadowJar::class) {
        baseName = "Jumper"
        classifier = null
        isZip64 = true
        from(java.sourceSets.getByName("main").output)
        dependsOn("clean", "classes")
    }

    "classes" {
        mustRunAfter("clean")
    }

    val jumper = "Jumper-$version"

    "zip"(Zip::class) {
        dependsOn("jar")
        from("../$jumper.jar")
        archiveName = "$jumper.zip"
        destinationDir = file("..")
    }

    tasks.replace("jar", Copy::class.java).apply {
        group = MAIN
        dependsOn("shadowJar")
        from("build/libs")
        into("..")
        doFirst {
            val old = file("..").listFiles { _, name -> name.startsWith("Jumper") }
            old.forEach { file ->
                file.copyTo(file("../Archiv/${file.name}"), true)
                file.delete()
            }
        }
    }

}