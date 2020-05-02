package com.github.juliarn.npc.utils;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;

public class Profile {

    private static final Pattern UNIQUE_ID_PATTERN = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");

    @NotNull
    public static Profile empty(@NotNull UUID uniqueID) {
        return new Profile(uniqueID.toString().replace("-", ""), null, new ArrayList<>());
    }

    public Profile(String id, String name, Collection<Property> properties) {
        this.id = id;
        this.name = name;
        this.properties = properties;
    }

    private transient UUID uniqueID;

    private final String id;

    private final String name;

    private final Collection<Property> properties;

    public UUID getUniqueID() {
        if (uniqueID == null) {
            uniqueID = UUID.fromString(UNIQUE_ID_PATTERN.matcher(this.id).replaceAll("$1-$2-$3-$4-$5"));
        }

        return uniqueID;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Collection<Property> getProperties() {
        return properties;
    }

    public void setProperties(@NotNull Collection<WrappedSignedProperty> profileProperties) {
        for (WrappedSignedProperty profileProperty : profileProperties) {
            this.properties.add(new Property(profileProperty.getName(), profileProperty.getValue(), profileProperty.getSignature()));
        }
    }

    @NotNull
    public WrappedGameProfile asWrappedWithoutProperties() {
        return new WrappedGameProfile(this.getUniqueID(), this.getName());
    }

    @NotNull
    public WrappedGameProfile asWrappedWithProperties() {
        WrappedGameProfile profile = new WrappedGameProfile(this.getUniqueID(), this.getName());
        for (Property property : this.getProperties()) {
            profile.getProperties().put(property.getName(), property.asWrapped());
        }

        return profile;
    }

    public static class Property {

        public Property(String name, String value, String signature) {
            this.name = name;
            this.value = value;
            this.signature = signature;
        }

        private final String name;

        private final String value;

        private final String signature;

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public String getSignature() {
            return signature;
        }

        public boolean isSigned() {
            return signature != null;
        }

        @NotNull
        public WrappedSignedProperty asWrapped() {
            return new WrappedSignedProperty(this.getName(), this.getValue(), this.getSignature());
        }
    }
}
