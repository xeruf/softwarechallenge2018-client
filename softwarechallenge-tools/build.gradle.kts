plugins {
	application
	java
	kotlin("jvm") version "1.2.41"
	id("com.github.johnrengelman.shadow") version "2.0.3"
}

application {
	mainClassName = "TurnExtractor"
}

java.sourceSets.getByName("main") {
	java.srcDirs("src")
}

repositories {
	jcenter()
	maven { setUrl("http://dist.wso2.org/maven2/") }
}

dependencies {
	compile("xerus.util", "kotlin")
	compile("xerus.util", "utilities")
	compile("jargs", "jargs", "1.0")
}
