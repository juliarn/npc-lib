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

package com.github.juliarn.npclib.bukkit.protocol;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.github.juliarn.npclib.api.profile.Profile;
import com.github.juliarn.npclib.api.profile.ProfileProperty;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for GameProfiles when working with ProtocolLib, as the new authlib v7 types are not properly supported.
 */
final class ProtocolLibProfileFactory {

  private static final MethodHandle CONSTRUCT_PROFILE_MODERN; // (UUID, String, Multimap<String, Property>): Object
  private static final MethodHandle CONSTRUCT_PROFILE_PROPERTY_MODERN; // (String, String, String): Object

  static {
    MethodHandle constructGameProfileModern;
    MethodHandle constructGameProfilePropertyModern;
    try {
      Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
      Class<?> gameProfilePropertyClass = Class.forName("com.mojang.authlib.properties.Property");
      Class<?> gameProfilePropertyMapClass = Class.forName("com.mojang.authlib.properties.PropertyMap");

      // resolve constructor for constructing a profile property
      MethodHandles.Lookup lookup = MethodHandles.publicLookup(); // everything should be public api
      MethodHandle constructProfileProperty = lookup.findConstructor(
        gameProfilePropertyClass,
        MethodType.methodType(void.class, String.class, String.class, String.class));
      MethodType propertyAsObject = constructProfileProperty.type().changeReturnType(Object.class);
      constructGameProfilePropertyModern = constructProfileProperty.asType(propertyAsObject);

      // resolve constructor for constructing a game profile
      MethodHandle constructGameProfile = lookup.findConstructor(
        gameProfileClass,
        MethodType.methodType(void.class, UUID.class, String.class, gameProfilePropertyMapClass));
      MethodHandle constructGamePropertiesMap = lookup.findConstructor(
        gameProfilePropertyMapClass,
        MethodType.methodType(void.class, Multimap.class));
      MethodHandle constructFull = MethodHandles.filterArguments(constructGameProfile, 2, constructGamePropertiesMap);
      MethodType constructFullAsObject = constructFull.type().changeReturnType(Object.class);
      constructGameProfileModern = constructFull.asType(constructFullAsObject);
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException ignored) {
      constructGameProfileModern = null;
      constructGameProfilePropertyModern = null;
    }

    CONSTRUCT_PROFILE_MODERN = constructGameProfileModern;
    CONSTRUCT_PROFILE_PROPERTY_MODERN = constructGameProfilePropertyModern;
  }

  private ProtocolLibProfileFactory() {
    throw new UnsupportedOperationException();
  }

  /**
   * Wraps the given resolved profile into a ProtocolLib {@link WrappedGameProfile}.
   *
   * @param profile the profile to wrap.
   * @return the given profile wrapped into a {@link WrappedGameProfile}.
   * @throws NullPointerException     if the given profile is null.
   * @throws IllegalArgumentException if the given profile is invalid.
   * @throws IllegalStateException    if the profile construction failed for some reason.
   */
  public static @NotNull WrappedGameProfile wrapProfile(@NotNull Profile.Resolved profile) {
    if (CONSTRUCT_PROFILE_MODERN != null) {
      // constructs a modern (authlib >= v7) profile using the authlib.GameProfile constructor
      try {
        Multimap<String, Object> properties = HashMultimap.create();
        for (ProfileProperty prop : profile.properties()) {
          Object property = CONSTRUCT_PROFILE_PROPERTY_MODERN.invokeExact(prop.name(), prop.value(), prop.signature());
          properties.put(prop.name(), property);
        }
        Object gameProfile = CONSTRUCT_PROFILE_MODERN.invokeExact(profile.uniqueId(), profile.name(), properties);
        return WrappedGameProfile.fromHandle(gameProfile);
      } catch (Throwable thrown) {
        throw new IllegalStateException("Unable to wrap resolved profile into game profile", thrown);
      }
    } else {
      // use the wrapped profile offered by ProtocolLib
      WrappedGameProfile wrappedGameProfile = new WrappedGameProfile(profile.uniqueId(), profile.name());
      for (ProfileProperty prop : profile.properties()) {
        WrappedSignedProperty wrapped = new WrappedSignedProperty(prop.name(), prop.value(), prop.signature());
        wrappedGameProfile.getProperties().put(prop.name(), wrapped);
      }
      return wrappedGameProfile;
    }
  }
}
