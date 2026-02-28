import java.util.Locale

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

if (!file(".git").exists()) {
    val errorText = """
        
        =====================[ ERROR ]=====================
         The Zero project directory is not a properly cloned Git repository.
         
         In order to build Zero from source you must clone
         the Zero repository using Git, not download a code
         zip from GitHub.
         
         Built Zero jars are available for download at
         https://github.com/oneachina/Zero
         
         See https://github.com/oneachina/Zero/blob/HEAD/CONTRIBUTING.md
         for further information on building and modifying Zero.
        ===================================================
    """.trimIndent()
    error(errorText)
}

rootProject.name = "zero"
for (name in listOf("zero-api", "zero-server")) {
    val projName = name.lowercase(Locale.ENGLISH)
    include(projName)
    findProject(":$projName")!!.projectDir = file(name)
}

optionalInclude("test-plugin")

fun optionalInclude(name: String, op: (ProjectDescriptor.() -> Unit)? = null) {
    val settingsFile = file("$name.settings.gradle.kts")
    if (settingsFile.exists()) {
        apply(from = settingsFile)
        findProject(":$name")?.let { op?.invoke(it) }
    } else {
        settingsFile.writeText(
            """
            // Uncomment to enable the '$name' project
            // include(":$name")

            """.trimIndent()
        )
    }
}
