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

package com.github.juliarn.npclib.bukkit;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.github.juliarn.npclib.api.profile.Profile;
import com.github.juliarn.npclib.api.profile.ProfileProperty;
import com.github.juliarn.npclib.api.profile.ProfileResolver;
import io.papermc.lib.PaperLib;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public final class BukkitProfileResolver {

  private BukkitProfileResolver() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull ProfileResolver profileResolver() {
    // check if we're on paper and newer than 1.12 (when the profile API was introduced)
    if (PaperLib.isPaper() && PaperLib.isVersion(12)) {
      return PaperProfileResolver.INSTANCE;
    }

    // check if we're on spigot and newer than 1.18.2 (when the profile API was introduced)
    if (PaperLib.isSpigot() && PaperLib.isVersion(18, 2)) {
      return SpigotProfileResolver.INSTANCE;
    }

    // use fallback resolver
    return LegacyResolver.INSTANCE;
  }

  private static final class PaperProfileResolver implements ProfileResolver {

    private static final ProfileResolver INSTANCE = new PaperProfileResolver();

    @Override
    public @NotNull CompletableFuture<Profile.Resolved> resolveProfile(@NotNull Profile profile) {
      return CompletableFuture.supplyAsync(() -> {
        // create a profile from the given one and try to complete it
        PlayerProfile playerProfile = Bukkit.createProfile(profile.uniqueId(), profile.name());
        playerProfile.complete(true, true);

        // convert the profile properties to the wrapper one
        Set<ProfileProperty> properties = playerProfile.getProperties()
          .stream()
          .map(prop -> ProfileProperty.property(prop.getName(), prop.getValue(), prop.getSignature()))
          .collect(Collectors.toSet());

        // create the resolved profile
        //noinspection ConstantConditions
        return Profile.resolved(playerProfile.getName(), playerProfile.getId(), properties);
      });
    }
  }

  private static final class SpigotProfileResolver implements ProfileResolver {

    private static final ProfileResolver INSTANCE = new SpigotProfileResolver();
    private static final Pattern DATA_EXTRACT_PATTERN = Pattern.compile("^CraftPlayerTextures \\[data=(.*)]$");

    @Override
    @SuppressWarnings("deprecation") // deprecated by paper, but we only use this on spigot
    public @NotNull CompletableFuture<Profile.Resolved> resolveProfile(@NotNull Profile profile) {
      // create the profile and fill in the empty values
      org.bukkit.profile.PlayerProfile playerProfile = Bukkit.createPlayerProfile(profile.uniqueId(), profile.name());
      return playerProfile.update().thenApply(resolvedProfile -> {
        // hack to get the data from the profile
        Matcher matcher = DATA_EXTRACT_PATTERN.matcher(resolvedProfile.getTextures().toString());
        if (matcher.matches()) {
          // encode the raw texture data
          byte[] rawTextureData = matcher.group(1).getBytes(StandardCharsets.UTF_8);
          String encodedTextureData = Base64.getEncoder().encodeToString(rawTextureData);

          // create the profile from the spigot one
          ProfileProperty textureProperty = ProfileProperty.property("textures", encodedTextureData);
          Set<ProfileProperty> properties = Collections.singleton(textureProperty);

          // create the resolved profile
          //noinspection ConstantConditions
          return Profile.resolved(playerProfile.getName(), playerProfile.getUniqueId(), properties);
        }

        // unable to complete the profile
        throw new IllegalArgumentException("Profile texture input: " + resolvedProfile.getTextures() + " is invalid");
      });
    }
  }

  private static final class LegacyResolver {

    private static final ProfileResolver INSTANCE = ProfileResolver.caching(ProfileResolver.mojang());
  }
}
