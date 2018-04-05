import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.internal.TaskOutputCachingState
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.Writer
import java.util.Arrays

version = KotlinVersion(1, 6).toString()

plugins {
    kotlin("jvm") version "1.2.30"
    id("com.github.johnrengelman.shadow") version "2.0.1"
    application
}

repositories { jcenter() }

dependencies {
    compile("xerus.util", "kotlin")
    compile(files(file("lib").listFiles()))
}

java.sourceSets.getByName("main").java.srcDir("src")

val args = listOf("-Dfile.encoding=UTF-8", "-XX:+ExplicitGCInvokesConcurrent"
        , "-XX:NewRatio=1"
        , "-mx800m", "-ms800m"
        , "-XX:MaxGCPauseMillis=80", "-XX:GCPauseIntervalMillis=1000"
        , "-XX:TargetSurvivorRatio=80"
)

val cms = arrayOf("-XX:+UseConcMarkSweepGC"
        , "-XX:-UseParNewGC"
        , "-XX:CMSInitiatingOccupancyFraction=80", "-XX:+UseCMSInitiatingOccupancyOnly"
        , "-XX:+ScavengeBeforeFullGC", "-XX:+CMSScavengeBeforeRemark")

val gcDebugParams = arrayOf(
        "-XX:+PrintGCDetails", "-XX:+PrintGCDateStamps"
        , "-XX:+PrintPromotionFailure"
        , "-XX:+PrintHeapAtGC"
        , "-XX:+PrintTenuringDistribution"
)

application {
    applicationName = "Jumper 1"
    mainClassName = "xerus.softwarechallenge.Starter"
    applicationDefaultJvmArgs = args + gcDebugParams + cms
}

tasks {

    val MAIN = "_main"
    val jumper = "Jumper-$version"

    "scripts" {
        doFirst {
            file("../start.sh").bufferedWriter().run {
                write("#!/bin/sh\n")
                write("java ")
                write((args + cms /*+ gcDebugParams*/).joinToString(" "))
                write(" -jar $jumper.jar \"\$@\"")
                close()
            }
        }
    }

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

    "zip"(Zip::class) {
        dependsOn("jar")
        from("..")
        include("$jumper.jar", "start.sh")
        archiveName = "$jumper.zip"
        destinationDir = file("..")
        doFirst {
            file("..").listFiles { _, name -> name.matches(Regex("Jumper.*.zip")) }.forEach { it.delete() }
        }
    }

    tasks.replace("jar").apply {
        group = MAIN
        dependsOn("shadowJar", "scripts")
        doFirst {
            val old = file("..").listFiles { _, name -> name.startsWith("Jumper") && name.endsWith("jar") && name != "$jumper.jar" }
            old.forEach { file ->
                file.copyTo(file("../Archiv/Jumper/${file.name}"), true)
                file.delete()
            }
        }
    }

}
