/*
 * This file is part of npc-lib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-2025 Julian M., Pasqual K. and contributors
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

plugins {
  alias(libs.plugins.shadow)
  alias(libs.plugins.fabricLoom)
}

configurations {
  val shaded = register("shaded")
  getByName("compileOnly").extendsFrom(shaded.get())
}

dependencies {
  minecraft(libs.minecraft)
  mappings(loom.officialMojangMappings())

  modImplementation(libs.geantyref)
  modImplementation(libs.fabricLoader)
  modImplementation(platform(libs.fabricApiBom))
  modImplementation(libs.fabricApiNetworkingV1)

  "shaded"(projects.npcLibApi)
  "shaded"(projects.npcLibCommon)
}

loom {
  accessWidenerPath.set(project.file("src/main/resources/npc_lib.accesswidener"))
}

tasks.shadowJar {
  exclude("META-INF/maven/**")
  configurations = setOf(project.configurations["shaded"])
}

tasks.remapJar {
  dependsOn(tasks.shadowJar)
  inputFile = tasks.shadowJar.flatMap { it.archiveFile }
}

tasks.compileJava {
  options.release = 21
}

tasks.processResources {
  val props = mapOf("version" to project.version)
  inputs.properties(props)
  filesMatching("fabric.mod.json") {
    expand(props)
  }
}
