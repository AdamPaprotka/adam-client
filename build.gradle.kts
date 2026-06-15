plugins {
    id("fabric-loom") version "1.9.2"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://repo.maven.apache.org/maven2")
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("adam-client") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.4")

    // ✔ Correct Yarn for 1.21.4 (IMPORTANT: must match MC version)
    mappings("net.fabricmc:yarn:1.21.4+build.1:v2")

    modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")

    // ImGui
    modImplementation("io.github.spair:imgui-java-binding:1.86.11")
    modImplementation("io.github.spair:imgui-java-lwjgl3:1.86.11")
    runtimeOnly("io.github.spair:imgui-java-natives-windows:1.86.11")
}

tasks.processResources {
    val modVersion = project.version.toString()
    inputs.property("version", modVersion)

    filesMatching("fabric.mod.json") {
        expand("version" to modVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.getByName("client").output)
    from("LICENSE") {
        rename { "${it}_${project.name}" }
    }
}