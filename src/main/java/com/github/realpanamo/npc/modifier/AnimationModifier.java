package com.github.realpanamo.npc.modifier;


import com.comphenix.protocol.PacketType;
import com.github.realpanamo.npc.NPC;
import org.jetbrains.annotations.NotNull;

public class AnimationModifier extends NPCModifier {

    public AnimationModifier(@NotNull NPC npc) {
        super(npc);
    }

    public AnimationModifier play(@NotNull EntityAnimation entityAnimation) {
        super.newContainer(PacketType.Play.Server.ANIMATION).getIntegers().write(1, entityAnimation.id);
        return this;
    }

    public enum EntityAnimation {
        SWING_MAIN_ARM(0),
        TAKE_DAMAGE(1),
        LEAVE_BED(2),
        SWING_OFF_HAND(3),
        CRITICAL_EFFECT(4),
        MAGIC_CRITICAL_EFFECT(5);

        private int id;

        EntityAnimation(int id) {
            this.id = id;
        }

    }

}
