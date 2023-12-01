import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.internal.TaskOutputCachingState
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.Writer
import java.util.Arrays
import java.util.Scanner

val c = properties["c"] as String? ?: "3"
val client = "Jumper$c"
val clientParams = listOf("-c", client)

version = properties["j"]?.toString() ?: "${Scanner(Runtime.getRuntime().exec("git rev-list --count HEAD").inputStream).next()}-${properties["n"]?.toString()
		?: Scanner(Runtime.getRuntime().exec("git rev-parse --short HEAD").inputStream).next().substring(0, 4)}-$c"
println("Version: $version")

plugins {
	kotlin("jvm") version "1.3.61"
	id("com.github.johnrengelman.shadow") version "2.0.3"
	id("com.dorongold.task-tree") version "1.3"
	application
}

repositories { jcenter() }

dependencies {
	compile("xerus.util", "kotlin")
	compile(kotlin("reflect"))
	compile(fileTree("lib"))
}

java.sourceSets.getByName("main").java.srcDir("src")
java.sourceSets.getByName("main").resources.srcDir("resources")

val javaArgs = listOf("-Dfile.encoding=UTF-8"
		, "-XX:NewRatio=1"
		, "-ms1500000000", "-mx1500000000"
		, "-XX:MaxGCPauseMillis=80", "-XX:GCPauseIntervalMillis=1000"
		, "-XX:TargetSurvivorRatio=90"
)

val cms = listOf("-XX:+UseConcMarkSweepGC"
		, "-XX:-UseParNewGC"
		, "-XX:CMSInitiatingOccupancyFraction=80", "-XX:+UseCMSInitiatingOccupancyOnly"
		, "-XX:+ScavengeBeforeFullGC", "-XX:+CMSScavengeBeforeRemark")

val gcDebugParams = listOf(
		"-XX:+PrintGCDetails", "-XX:+PrintGCTimeStamps"
		, "-XX:+PrintPromotionFailure", "-noverify"
)

application {
	applicationName = "Jumper 1"
	mainClassName = "xerus.softwarechallenge.StarterKt"
	applicationDefaultJvmArgs = javaArgs + cms + (if (properties["nogc"] == null) gcDebugParams else emptyList())
}

tasks {
	
	val MAIN = "_main"
	val clients = file("../clients").apply { mkdirs() }
	val jumper = "Jumper-$version"
	
	"scripts"(Exec::class) {
		val script = clients.resolve("start-client.sh")
		doFirst {
			script.bufferedWriter().run {
				write("""
					#!/usr/bin/env bash
					if [ $1 ] && [ -f $1 ]
					then client=$(dirname "${'$'}{BASH_SOURCE[0]}")/$1
					args=2
					else
					client=$(dirname "${'$'}{BASH_SOURCE[0]}")/$jumper.jar
					args=1
					fi
					java ${(javaArgs + cms + (if (properties["nogc"] == null) gcDebugParams else emptyList())).joinToString(" ")} -jar ${'$'}client "${'$'}{@:${'$'}args}"
				""".trimIndent())
				close()
			}
		}
		commandLine("chmod", "777", script)
	}
	
	withType<KotlinCompile> {
		kotlinOptions {
			jvmTarget = "1.8"
			freeCompilerArgs = listOf("-Xno-param-assertions", "-Xno-call-assertions")
		}
	}
	
	"run"(JavaExec::class) {
		group = MAIN
		args = listOf("-d", "2")
	}
	
	"shadowJar"(ShadowJar::class) {
		baseName = "Jumper"
		classifier = ""
		destinationDir = clients
		from(java.sourceSets.getByName("main").output)
		dependsOn("classes")
	}
	
	getByName("processResources").dependsOn("writeResources")
	
	"writeResources" {
		doFirst {
			sync {
				from("src/xerus/softwarechallenge")
				into("resources/sources")
			}
			file("resources/activeclient").writeText(client)
			file("resources/version").writeText(version as String)
		}
	}
	
	"zip"(Zip::class) {
		dependsOn("jar")
		from(clients)
		include("$jumper.jar", "start-client.sh")
		archiveName = "$jumper.zip"
		destinationDir = file("..")
		doFirst {
			file("..").listFiles { _, name -> name.matches(Regex("Jumper.*.zip")) }.forEach { it.delete() }
		}
	}
	
	"clean"(Delete::class) {
		delete.addAll(arrayOf("out", "games", "../clients/games", "../testserver/logs", "../testserver/starters",
				"../softwarechallenge-tools/out", "../softwarechallenge-tools/build", "../softwarechallenge-tools/.gradle"))
	}
	
	tasks.replace("jar").apply {
		group = MAIN
		dependsOn("shadowJar", "scripts")
	}
	
}

