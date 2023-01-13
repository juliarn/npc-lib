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

package com.github.juliarn.npclib.api.profile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class DefaultCachedProfileResolver implements ProfileResolver.Cached {

  private static final long ENTRY_KEEP_ALIVE_TIME = TimeUnit.HOURS.toMillis(3);

  private final ProfileResolver delegate;

  // using HashMap here might lead to inconsistent result views, but that doesn't matter in the context
  private final Map<String, CacheEntry<UUID>> nameToUniqueIdCache = new HashMap<>();
  private final Map<UUID, CacheEntry<Profile.Resolved>> uuidToProfileCache = new HashMap<>();

  public DefaultCachedProfileResolver(@NotNull ProfileResolver delegate) {
    this.delegate = delegate;
  }

  @Override
  public @NotNull CompletableFuture<Profile.Resolved> resolveProfile(@NotNull Profile profile) {
    // check if we can get the profile instantly from the cache
    Profile.Resolved cached = this.fromCache(profile);
    if (cached != null) {
      return CompletableFuture.completedFuture(cached);
    }

    // try to complete using the delegate resolver
    return this.delegate.resolveProfile(profile).whenComplete((resolvedProfile, exception) -> {
      // don't do anything if the operation wasn't successful
      if (exception == null && resolvedProfile != null) {
        // cache the result, override possible values which were previously inserted to reset the keep alive time
        this.nameToUniqueIdCache.put(
          resolvedProfile.name(),
          new CacheEntry<>(resolvedProfile.uniqueId(), ENTRY_KEEP_ALIVE_TIME));
        this.uuidToProfileCache.put(
          resolvedProfile.uniqueId(),
          new CacheEntry<>(resolvedProfile, ENTRY_KEEP_ALIVE_TIME));
      }
    });
  }

  @Override
  public @Nullable Profile.Resolved fromCache(@NotNull String name) {
    UUID cachedUniqueId = findCacheEntry(this.nameToUniqueIdCache, name);
    return cachedUniqueId == null ? null : this.fromCache(cachedUniqueId);
  }

  @Override
  public @Nullable Profile.Resolved fromCache(@NotNull UUID uniqueId) {
    return findCacheEntry(this.uuidToProfileCache, uniqueId);
  }

  @Override
  public @Nullable Profile.Resolved fromCache(@NotNull Profile profile) {
    UUID profileId = profile.uniqueId();
    if (profileId != null) {
      // check if we can get the resolved profile from the cache by the profile id
      Profile.Resolved cached = this.fromCache(profileId);
      if (cached != null) {
        return cached;
      }
    }

    String name = profile.name();
    if (name != null) {
      // check if we can get the resolved profile from the cache by the profile name
      return this.fromCache(name);
    }

    // unable to resolve with any possible method
    return null;
  }

  private static @Nullable <K, V> V findCacheEntry(@NotNull Map<K, CacheEntry<V>> cache, @NotNull K key) {
    // check if an entry is associated with the given key
    CacheEntry<V> entry = cache.get(key);
    if (entry == null) {
      return null;
    }

    // check if the entry is outdated
    if (entry.timeoutTime <= System.currentTimeMillis()) {
      cache.remove(key);
      return null;
    }

    // all fine
    return entry.value;
  }

  private static final class CacheEntry<T> {

    private final T value;
    private final long timeoutTime;

    public CacheEntry(@Nullable T value, long keepMillis) {
      this.value = value;
      this.timeoutTime = System.currentTimeMillis() + keepMillis;
    }
  }
}
