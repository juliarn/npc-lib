/*
 * This file is part of npc-lib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-2023 Julian M., Pasqual K. and contributors
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

        // validate that the profile id is present - when resolving by name only the id will be missing
        // see below for further details
        UUID profileId = playerProfile.getId();
        if (profileId == null) {
          throw new IllegalStateException("Could not resolve profile uuid using paper resolver");
        }

        // in case the player is not online, the complete method will not actually fill in the profile details.
        // the documentation states it will be, but it is not - even the update() method (added from the bukkit
        // api in 1.18) will not do this on a paper profile.
        // to work around this we just insert a random generated name in this case.
        // see https://github.com/PaperMC/Paper/issues/8927
        String profileName = playerProfile.getName();
        if (profileName == null) {
          String randomId = UUID.randomUUID().toString();
          profileName = randomId.replace("-", "").substring(0, 16);
        }

        // create the resolved profile
        return Profile.resolved(profileName, profileId, properties);
      });
    }
  }

  private static final class SpigotProfileResolver implements ProfileResolver {

    private static final ProfileResolver INSTANCE = new SpigotProfileResolver();

    @Override
    @SuppressWarnings("deprecation") // deprecated by paper, but we only use this on spigot
    public @NotNull CompletableFuture<Profile.Resolved> resolveProfile(@NotNull Profile profile) {
      // create the profile and fill in the empty values
      org.bukkit.profile.PlayerProfile playerProfile = Bukkit.createPlayerProfile(profile.uniqueId(), profile.name());
      return playerProfile.update().thenApplyAsync(resolvedProfile -> {
        // hack to get the data from the profile as it's not exposed directly
        //noinspection unchecked
        List<Map<String, Object>> props = (List<Map<String, Object>>) resolvedProfile.serialize().get("properties");
        if (props == null) {
          // only present if the profile has any properties, in this case there are no properties
          //noinspection ConstantConditions
          return Profile.resolved(resolvedProfile.getName(), resolvedProfile.getUniqueId());
        }

        // extract all properties of the profile
        Set<ProfileProperty> properties = new HashSet<>();
        for (Map<String, Object> entry : props) {
          ProfileProperty prop = ProfileProperty.property(
            (String) entry.get("name"),
            (String) entry.get("value"),
            (String) entry.get("signature"));
          properties.add(prop);
        }

        // build the profile from the given data
        //noinspection ConstantConditions
        return Profile.resolved(resolvedProfile.getName(), resolvedProfile.getUniqueId(), properties);
      });
    }
  }

  private static final class LegacyResolver {

    private static final ProfileResolver INSTANCE = ProfileResolver.caching(ProfileResolver.mojang());
  }
}
