package com.github.juliarn.npc.event;


import com.comphenix.protocol.wrappers.EnumWrappers;
import com.github.juliarn.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerNPCInteractEvent extends PlayerEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private NPC npc;

    private Action action;

    public PlayerNPCInteractEvent(@NotNull Player who, @NotNull NPC npc, @NotNull Action action) {
        super(who);
        this.npc = npc;
        this.action = action;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @NotNull
    public NPC getNPC() {
        return npc;
    }

    @NotNull
    public Action getAction() {
        return action;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public enum Action {
        RIGHT_CLICKED,
        LEFT_CLICKED;

        public static Action fromProtocolLib(EnumWrappers.EntityUseAction protocolLibAction) {
            return protocolLibAction == EnumWrappers.EntityUseAction.ATTACK ? LEFT_CLICKED : RIGHT_CLICKED;
        }

    }

}
