package com.github.juliarn.npc.profile;


import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

public class ProfileBuilder {

    private UUID uniqueId;

    private String name;

    private Collection<Profile.Property> profileProperties;

    private boolean complete = false;

    public ProfileBuilder(@NotNull UUID uniqueId, @NotNull String name) {
        this.uniqueId = uniqueId;
        this.name = name;
    }

    public ProfileBuilder(@NotNull UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public ProfileBuilder(@NotNull String name) {
        this.name = name;
    }

    public ProfileBuilder profileProperties(@NotNull Collection<Profile.Property> profileProperties) {
        this.profileProperties = profileProperties;
        return this;
    }

    public ProfileBuilder complete(boolean complete) {
        this.complete = complete;
        return this;
    }

    @NotNull
    public Profile build() {
        if (this.name == null && this.uniqueId == null) {
            throw new IllegalStateException("Either name or uniqueId has to be given!");
        }

        Profile profile = new Profile(this.uniqueId, this.name, this.profileProperties);
        if (this.complete) {
            profile.complete();
        }

        return profile;
    }

}
