plugins {
	id("dev.yumi.gradle.licenser").version("2.1.+")
	alias(libs.plugins.fabric.loom)
	`maven-publish`
	checkstyle
}

val mod_version: String by project
val maven_group: String by project
val mod_id: String by project
val mod_license: String by project

base.archivesName = mod_id
version = mod_version
group = maven_group

repositories {
	// Add repositories to retrieve artifacts from in here.
	// You should only use this when depending on other mods because
	// Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
	// See https://docs.gradle.org/current/userguide/declaring_repositories.html
	// for more information about repositories.

	mavenCentral()

	maven {
		name = "ParchmentMC"
		url = uri("https://maven.parchmentmc.org")
	}

	maven {
		name = "TerraformersMC"
		url = uri("https://maven.terraformersmc.com/")
	}
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft(libs.minecraft)
	mappings(loom.officialMojangMappings())
	modImplementation(libs.fabric.loader)

	// Fabric API. This is technically optional, but you probably want it anyway.
	modImplementation(libs.fabric.api)
}

loom {
	runtimeOnlyLog4j = true

	// Split sources is best practice in modern Minecraft versions
	splitEnvironmentSourceSets()

	mods {
		register(mod_id) {
			sourceSet(sourceSets["main"])
			sourceSet(sourceSets["client"])
			sourceSet(sourceSets["test"])
		}
	}

	sourceSets {
		register("testmod") {
			compileClasspath += sourceSets["main"].compileClasspath
			runtimeClasspath += sourceSets["main"].runtimeClasspath
		}

		register("testmodClient") {
			compileClasspath += sourceSets["main"].compileClasspath
			runtimeClasspath += sourceSets["main"].runtimeClasspath
			compileClasspath += sourceSets["client"].compileClasspath
			runtimeClasspath += sourceSets["client"].runtimeClasspath
		}

		getByName("test") {
			compileClasspath += sourceSets["testmodClient"].compileClasspath
			runtimeClasspath += sourceSets["testmodClient"].runtimeClasspath
		}
	}

	runs {
		create("testmodClient") {
			client()
			ideConfigGenerated(project.rootProject == project)
			name = "Testmod Client"
			source(sourceSets["testmodClient"])
		}
		create("testmodServer") {
			server()
			ideConfigGenerated(project.rootProject == project)
			name = "Testmod Server"
			source(sourceSets["testmod"])
		}
	}
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	// If this mod is a library, it should generate javadocs.
	// This line generates javadocs for the mod.
	// withJavadocJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks {
	withType<AbstractArchiveTask> {
		from("LICENSE") {
			rename { "${it}_${mod_id}"}
		}

		from("COPYING") {
			rename { "${it}_${mod_id}"}
		}

		from("COPYING.LESSER") {
			rename { "${it}_${mod_id}"}
		}
	}

	val expandProps = mapOf(
		"maven_group" to maven_group,
		"mod_id" to mod_id,
		"mod_version" to mod_version,
		"mod_license" to mod_license
	)

	processResources {
		inputs.property("version", version)

		filesMatching(listOf("fabric.mod.json", "*.mixins.json")) {
			expand(expandProps)
		}
	}

	javadoc {
		(options as StandardJavadocDocletOptions)
			.tags(
				"apiNote:a:API Note:",
				"implSpec:a:Implementation Requirements:",
				"implNote:a:Implementation Note:"
			)
	}

	withType<JavaCompile> {
		options.release.set(21)
	}

	build {
//		dependsOn(tasks.applyLicenses)
	}
}

license {
	rule(file("LHEADER"))
	exclude("**/*.json")
	exclude("**/*.fsh")
	exclude("**/*.vsh")
	exclude("**/*.gsh")
	exclude("**/*.tsh")
	exclude("**/*.csh")
}

apply("gradle/package-info.gradle")

// configure the maven publication
publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			artifactId = base.archivesName.get()
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}
