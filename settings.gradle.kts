pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		maven("https://maven.isxander.dev/releases/")
		maven("https://maven.fabricmc.net/")
		maven("https://maven.neoforged.net/releases/")
		maven("https://maven.kikugie.dev/releases")
		maven("https://maven.kikugie.dev/snapshots")
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.6+"
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
	kotlinController = true
	centralScript = "build.gradle.kts"

	create(rootProject) {
		fun mc(mcVersion: String, name: String = mcVersion, loaders: Iterable<String>) =
			loaders.forEach { vers("$name-$it", mcVersion) }

		mc("26.1.2", loaders = listOf("fabric"))
		mc("1.21.10", loaders = listOf("fabric"))
		mc("1.21.11", loaders = listOf("fabric"))

		vcsVersion = "26.1.2-fabric"
	}
}

rootProject.name = "autotrade-fabric"
