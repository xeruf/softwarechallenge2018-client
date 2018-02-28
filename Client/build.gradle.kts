import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.internal.TaskOutputCachingState
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Arrays

version = KotlinVersion(1, 5).toString()

plugins {
    kotlin("jvm") version "1.2.21"
    id("com.github.johnrengelman.shadow") version "2.0.1"
    application
}

repositories { jcenter() }

dependencies {
    compile("xerus.util", "kotlin")
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
        destinationDir = file("..")
        from(java.sourceSets.getByName("main").output)
        dependsOn("clean", "classes")
    }

    "classes" {
        mustRunAfter("clean")
    }

    val jumper = "Jumper-$version"

    "zip"(Zip::class) {
        dependsOn("jar")
        from("..")
        include("$jumper.jar", "start*.sh")
        archiveName = "$jumper.zip"
        destinationDir = file("..")
        doFirst {
            file("..").listFiles { _, name -> name.matches(Regex("Jumper.*.zip")) }.forEach { it.delete() }
        }
    }

    tasks.replace("jar").apply {
        group = MAIN
        dependsOn("shadowJar")
        doFirst {
            val old = file("..").listFiles { _, name -> name.startsWith("Jumper") && name.endsWith("jar") && name != "$jumper.jar" }
            old.forEach { file ->
                file.copyTo(file("../Archiv/${file.name}"), true)
                file.delete()
            }
        }
    }

}

/*
-XX:+UseG1GC

 -Dfile.encoding=UTF-8 \
     -server \
     -XX:MaxGCPauseMillis=100 \
     -XX:GCPauseIntervalMillis=2050 \
     -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled \
     -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 \
     -XX:+ScavengeBeforeFullGC -XX:+CMSScavengeBeforeRemark \
*/