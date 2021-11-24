package com.github.juliarn.npc.profile;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public final class ProfileUtils {

  private ProfileUtils() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull String randomName() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }

  public static @NotNull WrappedGameProfile profileToWrapper(@NotNull Profile profile) {
    WrappedGameProfile wrapped = new WrappedGameProfile(profile.getUniqueId(), profile.getName());
    profile.getProperties().forEach(prop -> wrapped.getProperties().put(
        prop.getName(),
        new WrappedSignedProperty(prop.getName(), prop.getValue(), prop.getSignature())));
    return wrapped;
  }
}
