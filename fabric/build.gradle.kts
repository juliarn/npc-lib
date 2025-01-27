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
  alias(libs.plugins.fabricLoom)
}

configurations {
  // custom configuration for later dependency resolution
  create("runtimeImpl") {
    configurations.getByName("api").extendsFrom(this)
  }
}

dependencies {
  minecraft(libs.minecraft)
  modImplementation(libs.fabricLoader)
  mappings(loom.officialMojangMappings())

  modImplementation(platform(libs.fabricApiBom))
  modImplementation(libs.fabricApiNetworkingV1)

  "runtimeImpl"(projects.npcLibApi)
  "runtimeImpl"(projects.npcLibCommon)

  implementation(libs.geantyref)
}

tasks.withType<Jar> {
  dependsOn(":npc-lib-api:jar")
  dependsOn(":npc-lib-common:jar")
  from(configurations.getByName("runtimeImpl").map { if (it.isDirectory) it else zipTree(it) })
}

tasks.withType<JavaCompile> {
  options.release.set(21)
}

tasks.withType<ProcessResources> {
  val props = mapOf("version" to project.version)
  inputs.properties(props)
  filesMatching("fabric.mod.json") {
    expand(props)
  }
}

loom {
  accessWidenerPath.set(project.file("src/main/resources/npc_lib.accesswidener"))
}
