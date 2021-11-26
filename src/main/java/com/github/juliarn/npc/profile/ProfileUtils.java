package com.github.juliarn.npc.profile;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Small utility class around profiles.
 *
 * @since 2.7-SNAPSHOT
 */
public final class ProfileUtils {

  private ProfileUtils() {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a random name which is exactly 16 chars long and only contains alphabetic and numeric
   * chars. The created name conforms to the Mojang naming convention as (for example) described
   * <a href="https://help.minecraft.net/hc/en-us/articles/4408950195341-Minecraft-Java-Edition-Username-VS-Gamertag-FAQ">here</a>.
   *
   * @return a randomly created minecraft name.
   */
  public static @NotNull String randomName() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }

  /**
   * Converts the given {@link Profile} to a protocol lib wrapper copying the name, unique id and
   * profile properties.
   *
   * @param profile the profile to convert.
   * @return the created protocol lib wrapper of the profile.
   */
  public static @NotNull WrappedGameProfile profileToWrapper(@NotNull Profile profile) {
    WrappedGameProfile wrapped = new WrappedGameProfile(profile.getUniqueId(), profile.getName());
    profile.getProperties().forEach(prop -> wrapped.getProperties().put(
        prop.getName(),
        new WrappedSignedProperty(prop.getName(), prop.getValue(), prop.getSignature())));
    return wrapped;
  }
}
