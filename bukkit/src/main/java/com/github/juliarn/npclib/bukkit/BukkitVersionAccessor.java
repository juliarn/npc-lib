/*
 * This file is part of npc-lib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-present Julian M., Pasqual K. and contributors
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

package com.github.juliarn.npclib.bukkit;

import com.github.juliarn.npclib.api.PlatformVersionAccessor;
import io.papermc.paper.ServerBuildInfo;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public final class BukkitVersionAccessor implements PlatformVersionAccessor {

  /**
   * Regex for any {@code number.number.number} combination, where the last number is optional.
   */
  private static final Pattern MC_VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.?(\\d+)?");
  /**
   * Regex for the bukkit mc version representation.
   */
  private static final Pattern BUKKIT_VERSION_PATTERN = Pattern.compile("\\(MC: (\\d+)\\.(\\d+)\\.?(\\d+)?");

  /**
   * The version string that couldn't be parsed when no default instance could be constructed. {@code null} in case a
   * default instance was constructed successfully.
   */
  private static final String FAILED_PARSE_INPUT;
  /**
   * Default instance parsed from a version string. {@code null} in case the version input string couldn't be converted
   * to a SemVer version. In this case the failed parse input field is non-null and holds the parse input that failed.
   */
  private static final PlatformVersionAccessor DEFAULT_INSTANCE;

  static {
    Matcher versionMatcher;
    try {
      // try to use the modern approach via Paper ServerBuildInfo
      ServerBuildInfo buildInfo = ServerBuildInfo.buildInfo();
      versionMatcher = MC_VERSION_PATTERN.matcher(buildInfo.minecraftVersionId());
    } catch (Throwable throwable) {
      // use the legacy approach via Bukkit.getVersion()
      versionMatcher = BUKKIT_VERSION_PATTERN.matcher(Bukkit.getVersion());
    }

    if (versionMatcher.find()) {
      String major = versionMatcher.group(1);
      String minor = versionMatcher.group(2);
      String patch = versionMatcher.group(3);
      DEFAULT_INSTANCE = new BukkitVersionAccessor(
        Integer.parseInt(major),
        Integer.parseInt(minor),
        patch == null ? 0 : Integer.parseInt(patch));
      FAILED_PARSE_INPUT = null;
    } else {
      DEFAULT_INSTANCE = null;
      FAILED_PARSE_INPUT = versionMatcher.replaceAll(""); // this returns the full text when there is no match
    }
  }

  private final int major;
  private final int minor;
  private final int patch;

  public BukkitVersionAccessor(int major, int minor, int patch) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
  }

  public static boolean hasDefaultAccessor() {
    return DEFAULT_INSTANCE != null;
  }

  public static @NotNull PlatformVersionAccessor versionAccessor() {
    if (DEFAULT_INSTANCE != null) {
      return DEFAULT_INSTANCE;
    }

    throw new IllegalStateException("Version is not available as '" + FAILED_PARSE_INPUT + "' couldn't be parsed");
  }

  @Override
  public int major() {
    return this.major;
  }

  @Override
  public int minor() {
    return this.minor;
  }

  @Override
  public int patch() {
    return this.patch;
  }

  @Override
  public boolean atLeast(int major, int minor, int patch) {
    if (this.major != major) {
      return this.major > major;
    }

    if (this.minor != minor) {
      return this.minor > minor;
    }

    return this.patch >= patch;
  }
}
