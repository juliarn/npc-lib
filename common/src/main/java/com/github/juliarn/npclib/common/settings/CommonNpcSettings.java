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

package com.github.juliarn.npclib.common.settings;

import com.github.juliarn.npclib.api.flag.NpcFlag;
import com.github.juliarn.npclib.api.settings.NpcProfileResolver;
import com.github.juliarn.npclib.api.settings.NpcSettings;
import com.github.juliarn.npclib.api.settings.NpcTrackingRule;
import com.github.juliarn.npclib.common.flag.CommonNpcFlaggedObject;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class CommonNpcSettings<P> extends CommonNpcFlaggedObject implements NpcSettings<P> {

  protected final NpcTrackingRule<P> trackingRule;
  protected final NpcProfileResolver<P> profileResolver;

  public CommonNpcSettings(
    @NotNull Map<NpcFlag<?>, Optional<?>> flags,
    @NotNull NpcTrackingRule<P> trackingRule,
    @NotNull NpcProfileResolver<P> profileResolver
  ) {
    super(flags);
    this.trackingRule = trackingRule;
    this.profileResolver = profileResolver;
  }

  @Override
  public @NotNull NpcTrackingRule<P> trackingRule() {
    return this.trackingRule;
  }

  @Override
  public @NotNull NpcProfileResolver<P> profileResolver() {
    return this.profileResolver;
  }
}
