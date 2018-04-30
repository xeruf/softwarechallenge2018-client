import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.internal.TaskOutputCachingState
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.Writer
import java.util.Arrays

version = file("src/xerus/softwaerchallenge/logic2018/Jumper1.kt")

plugins {
	kotlin("jvm") version "1.2.41"
	id("com.github.johnrengelman.shadow") version "2.0.3"
	application
}

repositories { jcenter() }

dependencies {
	compile("xerus.util", "kotlin")
	compile(kotlin("stdlib"))
	compile(fileTree("lib"))
}

java.sourceSets.getByName("main").java.srcDir("src")

val args = listOf("-Dfile.encoding=UTF-8"
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
		, "-XX:+PrintPromotionFailure"
)

application {
	applicationName = "Jumper 1"
	mainClassName = "xerus.softwarechallenge.StarterKt"
	applicationDefaultJvmArgs = args + cms + gcDebugParams + "-Xverify:none"
}

tasks {
	
	val MAIN = "_main"
	val jumper = "Jumper-$version"
	
	"scripts" {
		doFirst {
			file("../start-client.sh").bufferedWriter().run {
				write("#!/bin/sh\n")
				write("java ")
				write((args + cms + gcDebugParams).joinToString(" "))
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
	
	tasks.replace("jar").apply {
		group = MAIN
		dependsOn("shadowJar", "scripts")
		doFirst {
			file("..").listFiles { _, name -> name.startsWith("Jumper") && name.endsWith("jar") && name != "$jumper.jar" }
					.forEach {
						it.renameTo(file("../Archiv/Jumper/${it.name}").also { it.delete() })
					}
		}
	}
	
}

println("JavaVersion: ${JavaVersion.current()}")
