package com.github.juliarn.npc.profile;


import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

public class ProfileBuilder {

    private UUID uniqueId;

    private String name;

    private Collection<Profile.Property> profileProperties = new HashSet<>();

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
        Profile profile = new Profile(this.uniqueId, this.name, this.profileProperties);

        if (this.complete) {
            profile.complete();
        }

        return profile;
    }

}
