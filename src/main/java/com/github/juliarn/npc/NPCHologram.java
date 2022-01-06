package com.github.juliarn.npc;

import com.github.unldenis.hologram.Hologram;
import com.github.unldenis.hologram.line.AbstractLine;
import com.github.unldenis.hologram.placeholder.Placeholders;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;

public class NPCHologram extends Hologram {

  /**
   * {@inheritDoc}
   */
  @Deprecated
  @ApiStatus.Internal
  public NPCHologram(@NotNull Plugin plugin, @NotNull Location location,
      @Nullable Placeholders placeholders, @NotNull Collection<Player> seeingPlayers,
      @NotNull Object... lines) {
    super(plugin, location, placeholders, seeingPlayers, lines);
  }

  @Override
  protected void show(@NotNull Player player) {
    for(AbstractLine<?> line: this.lines) {
      line.show(player);
    }
  }

  @Override
  protected void hide(@NotNull Player player) {
    for(AbstractLine<?> line: this.lines) {
      line.hide(player);
    }
  }

}
