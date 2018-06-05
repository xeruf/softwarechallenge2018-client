import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	application
	kotlin("jvm") version "1.2.41"
	id("com.github.johnrengelman.shadow") version "2.0.3"
}

application {
	mainClassName = "TurnExtractor"
}

java.sourceSets.getByName("main") {
	java.srcDir("src")
	resources.srcDir("resources")
}

repositories {
	jcenter()
	maven { setUrl("http://dist.wso2.org/maven2/") }
}

dependencies {
	compile(fileTree("../client/lib"))
	compile("xerus.util", "javafx")
	compile("xerus.util", "utilities")
	compile("jargs", "jargs", "1.0")
}


tasks.withType<KotlinCompile> {
	kotlinOptions {
		jvmTarget = "1.8"
		freeCompilerArgs = listOf("-Xno-param-assertions", "-Xno-call-assertions")
	}
}