/*
 * This file is part of npc-lib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 Julian M., Pasqual K. and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  alias(libs.plugins.spotless)
  alias(libs.plugins.shadow) apply false
}

defaultTasks("build", "shadowJar")

allprojects {
  version = "2.0.0-SNAPSHOT"
  group = "com.github.juliarn"

  repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
  }
}

subprojects {
  // apply all plugins only to subprojects
  apply(plugin = "checkstyle")
  apply(plugin = "java-library")
  apply(plugin = "maven-publish")
  apply(plugin = "com.diffplug.spotless")
  apply(plugin = "com.github.johnrengelman.shadow")

  dependencies {
    "compileOnly"(rootProject.libs.annotations)
  }

  configurations.all {
    // unsure why but every project loves them, and they literally have an import for every letter I type - beware
    exclude("org.checkerframework", "checker-qual")
  }

  tasks.withType<Jar> {
    from(rootProject.file("license.txt"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }

  tasks.withType<ShadowJar> {
    archiveClassifier.set(null as String?)
  }

  tasks.withType<JavaCompile>().configureEach {
    // options
    options.release.set(8)
    options.encoding = "UTF-8"
    options.isIncremental = true
    // we are aware that those are there, but we only do that if there is no other way we can use - so please keep the terminal clean!
    options.compilerArgs = mutableListOf("-Xlint:-deprecation,-unchecked")
  }

  extensions.configure<JavaPluginExtension> {
    disableAutoTargetJvm()
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
  }

  tasks.withType<Checkstyle> {
    maxErrors = 0
    maxWarnings = 0
    configFile = rootProject.file("checkstyle.xml")
  }

  extensions.configure<CheckstyleExtension> {
    toolVersion = "10.3.4"
  }

  extensions.configure<SpotlessExtension> {
    java {
      licenseHeaderFile(rootProject.file("license_header.txt"))
    }
  }

  tasks.withType<Javadoc> {
    val options = options as? StandardJavadocDocletOptions ?: return@withType

    // options
    options.encoding = "UTF-8"
    options.memberLevel = JavadocMemberLevel.PRIVATE
    options.addStringOption("-html5")
    options.addBooleanOption("Xdoclint:-missing", true)
  }

  extensions.configure<PublishingExtension> {
    publications.apply {
      create("maven", MavenPublication::class.java).apply {
        from(components.getByName("java"))
      }
    }
  }
}
