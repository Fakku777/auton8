plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
}

base {
    archivesName = properties["archives_base_name"] as String
    version = properties["mod_version"] as String
    group = properties["maven_group"] as String
}

repositories {
    mavenCentral()
    maven {
        name = "fabricmc"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
}

dependencies {
    // Fabric
    minecraft("com.mojang:minecraft:${properties["minecraft_version"] as String}")
    mappings("net.fabricmc:yarn:${properties["yarn_mappings"] as String}:v2")
    modImplementation("net.fabricmc:fabric-loader:${properties["loader_version"] as String}")


    // HTTP Client for Minescript API communication
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    include("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    include("com.squareup.okhttp3:okhttp-sse:4.12.0")
    
    // Docker Java API for container management
    implementation("com.github.docker-java:docker-java-core:3.3.4")
    include("com.github.docker-java:docker-java-core:3.3.4")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.3.4")
    include("com.github.docker-java:docker-java-transport-httpclient5:3.3.4")

    modLocalRuntime(files("libs/baritone.jar"))
    modLocalRuntime(files("libs/nether-pathfinder-1.4.1.jar"))


    // Meteor
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.132.0+1.21.8")
    modImplementation("meteordevelopment:meteor-client:${properties["minecraft_version"] as String}-SNAPSHOT")
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to project.property("minecraft_version"),
        )

        inputs.properties(propertyMap)

        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        inputs.property("archivesName", project.base.archivesName.get())

        from("LICENSE") {
            rename { "${it}_${inputs.properties["archivesName"]}" }
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }
}
