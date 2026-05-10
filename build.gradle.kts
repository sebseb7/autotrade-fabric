import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
	id("dev.isxander.modstitch.base") version "0.8.4"
	id("com.modrinth.minotaur") version "2.+"
}

repositories {
	mavenCentral()
	maven("https://maven.fabricmc.net/")
	maven("https://api.modrinth.com/maven")
	maven("https://maven.terraformersmc.com/releases/")
	maven("https://jitpack.io")
}

val minecraft = findProperty("deps.minecraft")?.toString()
	?: error("deps.minecraft must be set by the active Stonecutter target (see versions/*/gradle.properties).")

val javaLanguageVersion = when (minecraft) {
	"1.21.10", "1.21.11" -> 21
	"26.1.2" -> 25
	else -> error("Add Java toolchain mapping for Minecraft $minecraft in build.gradle.kts.")
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(javaLanguageVersion))
	}
}

val rawModVersion = findProperty("mod_version")?.toString() ?: error("mod_version")
val modReleaseVersion = if (rawModVersion.endsWith("-dev")) {
	"$rawModVersion.${DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss").format(LocalDateTime.now())}"
} else {
	rawModVersion
}

modstitch {
	minecraftVersion = minecraft

	metadata {
		modId = findProperty("mod_id")?.toString() ?: error("mod_id")
		modName = findProperty("mod_name")?.toString() ?: error("mod_name")
		modVersion = modReleaseVersion
		modGroup = findProperty("mod_group")?.toString() ?: error("mod_group")
		modAuthor = findProperty("author")?.toString() ?: error("author")

		replacementProperties.put("mod_author", findProperty("author")?.toString() ?: error("author"))
		replacementProperties.put("mod_issue_tracker", "https://github.com/sebseb7/autotrade-fabric/issues")
		replacementProperties.put("mod_sources", "https://github.com/sebseb7/autotrade-fabric")
		replacementProperties.put("mod_homepage", "https://modrinth.com/mod/autotrade-fabric")
		replacementProperties.put("malilib_version", findProperty("malilib_version")?.toString() ?: error("malilib_version"))
		replacementProperties.put(
			"fabric_api_dependency",
			when (minecraft) {
				"26.1.2" -> ">=0.145.0"
				"1.21.11" -> ">=0.140.0"
				"1.21.10" -> ">=0.136.0"
				else -> error("fabric_api_dependency mapping for $minecraft")
			},
		)
		replacementProperties.put(
			"minecraft_dependency",
			">=" + (findProperty("minecraft_version_min")?.toString() ?: minecraft),
		)
	}

	loom {
		fabricLoaderVersion = findProperty("fabric_loader_version")?.toString()
			?: error("fabric_loader_version")

		configureLoom {
		}
	}
}

val loader = project.name.substringAfterLast("-")
stonecutter {
	consts(
		"fabric" to loader.equals("fabric", ignoreCase = true),
		"neoforge" to loader.equals("neoforge", ignoreCase = true),
		"forge" to loader.equals("forge", ignoreCase = true),
		"vanilla" to loader.equals("vanilla", ignoreCase = true),
		"mc26" to (minecraft == "26.1.2"),
		"npcSplit" to (minecraft == "26.1.2" || minecraft == "1.21.11"),
		"npcFlat" to (minecraft == "1.21.10"),
	)
}

dependencies {
	val fabricApi = findProperty("fabric_api_version")?.toString() ?: error("fabric_api_version")
	val malilib = findProperty("malilib_version")?.toString() ?: error("malilib_version")
	val modMenu = findProperty("mod_menu_version")?.toString() ?: error("mod_menu_version")

	modstitchModImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApi")
	modstitchModImplementation("maven.modrinth:malilib:$malilib")
	modstitchModCompileOnly("com.terraformersmc:modmenu:$modMenu")
}

group = findProperty("mod_group")?.toString() ?: error("mod_group")
base {
	archivesName.set("${findProperty("mod_file_name")}-$minecraft")
}

version = modReleaseVersion

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
}

modrinth {
	token.set(System.getenv("MODRINTH_TOKEN"))
	syncBodyFrom.set(rootProject.file("README.md").readText())
	projectId.set("C1naQCmt")
	uploadFile.set(tasks.jar)
	gameVersions.addAll(listOf("26.1.2", "1.21.10", "1.21.11"))
	loaders.add("fabric")
}
