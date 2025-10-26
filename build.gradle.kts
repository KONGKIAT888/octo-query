plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.7.1"
}

group = "me.kongkiat"
version = "25.4.1"

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
            <h3>25.4.1</h3>
            <ul>
              <li>Added automatic SQL reformat after IntelliJ reformat (<kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>L</kbd>)</li>
              <li>Improved detection for <code>@Query</code> and <code>@NativeQuery</code> blocks</li>
              <li>Enhanced DTO constructor formatting (<code>SELECT new ...</code>) for cleaner indentation</li>
              <li>Integrated background listener — triggers reformat on save and after document commit</li>
              <li>Added manual action shortcut: <kbd>Alt</kbd> + <kbd>Shift</kbd> + <kbd>F</kbd> (Format JPA Query)</li>
              <li>Fixed inconsistent indentation when using IntelliJ’s built-in formatter</li>
            </ul>
            <p><b>OctoQuery</b> now seamlessly formats your SQL inside <code>@Query</code> annotations — automatically, elegantly, and consistently.</p>
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
