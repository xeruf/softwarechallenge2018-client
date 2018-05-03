import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.internal.TaskOutputCachingState
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.Writer
import java.util.Arrays

val client = properties["c"] as String?
val clientParams = if (client != null) listOf("-c", client) else emptyList()

version = file("src/xerus/softwarechallenge/logic2018/${client ?: "Jumper2"}.kt").bufferedReader().use {
	var line: String
	do {
		line = it.readLine()
	} while (!line.contains("Jumper"))
	line.split('"')[1]
}

plugins {
	kotlin("jvm") version "1.2.41"
	id("com.github.johnrengelman.shadow") version "2.0.3"
	application
}

repositories { jcenter() }

dependencies {
	compile("xerus.util", "kotlin")
	compile(kotlin("stdlib-jdk8"))
	compile(fileTree("lib"))
}

java.sourceSets.getByName("main").java.srcDir("src")

val javaArgs = listOf("-Dfile.encoding=UTF-8"
		, "-XX:NewRatio=1"
		, "-ms600m", "-mx600m"
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
	applicationDefaultJvmArgs = javaArgs + cms + gcDebugParams
}

tasks {
	
	val MAIN = "_main"
	val jumper = "Jumper-$version"
	
	"scripts"(Exec::class) {
		doFirst {
			file("../start-client.sh").bufferedWriter().run {
				write("""
					#!/bin/sh
					client=${file("../$jumper.jar").absoluteFile}
					if [ ${'$'}# -eq 0 ]; 
					then args=0;
					else args=2;
					if [ -f ${'$'}1 ]; then client=${'$'}1; fi
					fi;
					java ${(javaArgs + cms + gcDebugParams).joinToString(" ")} -jar ${'$'}client "${'$'}{@}"
				""".trimIndent())
				close()
			}
			file("../start-new.sh").bufferedWriter().run {
				write("../start-client.sh ../$jumper.jar ${clientParams.joinToString(" ")} \"\$@\"")
				close()
			}
		}
		commandLine("chmod", "+x", "../start-client.sh")
	}
	
	withType<KotlinCompile> {
		kotlinOptions {
			jvmTarget = "1.8"
			freeCompilerArgs = listOf("-Xno-param-assertions", "-Xno-call-assertions")
		}
	}
	
	"run"(JavaExec::class) {
		group = MAIN
		args = listOf("-d", "2") + clientParams
	}
	
	"shadowJar"(ShadowJar::class) {
		baseName = "Jumper"
		classifier = ""
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
		include("$jumper.jar", "start-client.sh")
		archiveName = "$jumper.zip"
		destinationDir = file("..")
		doFirst {
			file("..").listFiles { _, name -> name.matches(Regex("Jumper.*.zip")) }.forEach { it.delete() }
		}
	}
	
	"clean"(Delete::class) {
		delete += setOf("games", "../testserver/log", "../testserver/logs", "../softwarechallenge-tools/build")
	}
	
	tasks.replace("jar").apply {
		group = MAIN
		dependsOn("shadowJar", "scripts")
		doFirst {
			file("..").listFiles { _, name -> name.startsWith("Jumper") && name.endsWith("jar") && name != "$jumper.jar" }
			//.forEach { it.renameTo(file("../Archiv/Jumper/${it.name}").also { it.delete() }) }
		}
	}
	
}

println("JavaVersion: ${JavaVersion.current()}")
println("Version: $version")
