plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.7.1"
}

group = "me.kongkiat"
version = "25.4.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IU", "2025.1.4.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.database")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes = """
            <ul>
              <li><b>New:</b> Added context menu integration — right-click inside <code>@Query</code> or <code>@NativeQuery</code> annotations and select <b>"Format SQL Query"</b> directly from the editor.</li>
              <li><b>New:</b> Added support for formatting standalone <code>.sql</code> files — OctoQuery now works seamlessly in SQL editors as well.</li>
              <li>Improved detection for <code>@Query</code> and <code>@NativeQuery</code> blocks.</li>
              <li>Enhanced DTO constructor formatting (<code>SELECT new ...</code>) for cleaner indentation.</li>
              <li>Integrated background listener — triggers automatic SQL reformat on save and after document commit.</li>
              <li>Manual action shortcut: <kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>L</kbd> (Format JPA Query).</li>
              <li>Fixed inconsistent indentation when using IntelliJ’s built-in formatter.</li>
            </ul>
            <p><b>OctoQuery</b> now supports both annotation-based and standalone SQL formatting — accessible via shortcuts, menus, or right-click context actions. Faster, smarter, and more consistent than ever.</p>
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    register<Delete>("cleanSandbox") {
        delete("build/idea-sandbox")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
