plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "ptp.burp.extensions"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly("net.portswigger.burp.extensions:montoya-api:2024.12")
    implementation("org.jfree:jfreechart:1.5.3")
}

tasks {
    shadowJar {
        archiveBaseName.set("TickTockEnum")
        archiveClassifier.set("")
        archiveVersion.set("")
        dependencies {
            include(dependency("org.jfree:jfreechart"))
        }
    }
}
