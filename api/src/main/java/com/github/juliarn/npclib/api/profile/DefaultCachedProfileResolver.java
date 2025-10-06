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

package com.github.juliarn.npclib.api.profile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class DefaultCachedProfileResolver implements ProfileResolver.Cached {

  private static final long DEFAULT_CACHE_TIME = TimeUnit.HOURS.toMillis(5);
  private static final long CACHE_TIME_MS = Long.getLong("npc_lib.profile-cache.keep-ms", DEFAULT_CACHE_TIME);
  private static final long CACHE_TIME_NS = TimeUnit.MILLISECONDS.toNanos(CACHE_TIME_MS);

  private final ProfileResolver delegate;

  // using HashMap here might lead to inconsistent result views, but that doesn't matter in the context
  private final Map<String, CacheEntry<UUID>> nameToUniqueIdCache = new HashMap<>();
  private final Map<UUID, CacheEntry<Profile.Resolved>> uuidToProfileCache = new HashMap<>();

  public DefaultCachedProfileResolver(@NotNull ProfileResolver delegate) {
    this.delegate = delegate;
  }

  private static @Nullable <K, V> V findCacheEntry(@NotNull Map<K, CacheEntry<V>> cache, @NotNull K key) {
    CacheEntry<V> entry = cache.get(key);
    if (entry == null) {
      return null;
    }

    if (System.nanoTime() > entry.timeoutTime) {
      cache.remove(key, entry);
      return null;
    }

    return entry.value;
  }

  @Override
  public @NotNull CompletableFuture<Profile.Resolved> resolveProfile(@NotNull Profile profile) {
    // check if we can get the profile instantly from the cache
    Profile.Resolved cached = this.fromCache(profile);
    if (cached != null) {
      return CompletableFuture.completedFuture(cached);
    }

    // try to complete using the delegate resolver
    return this.delegate.resolveProfile(profile).thenApply((resolvedProfile) -> {
      CacheEntry<UUID> nameCacheEntry = new CacheEntry<>(resolvedProfile.uniqueId(), CACHE_TIME_NS);
      this.nameToUniqueIdCache.put(resolvedProfile.name(), nameCacheEntry);

      CacheEntry<Profile.Resolved> profileCacheEntry = new CacheEntry<>(resolvedProfile, CACHE_TIME_NS);
      this.uuidToProfileCache.put(resolvedProfile.uniqueId(), profileCacheEntry);

      return resolvedProfile;
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
      Profile.Resolved cached = this.fromCache(profileId);
      if (cached != null) {
        return cached;
      }
    }

    String name = profile.name();
    return name != null ? this.fromCache(name) : null;
  }

  private static final class CacheEntry<T> {

    private final T value;
    private final long timeoutTime;

    public CacheEntry(@Nullable T value, long cacheTimeNs) {
      this.value = value;
      this.timeoutTime = System.nanoTime() + cacheTimeNs;
    }
  }
}
