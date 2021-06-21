package com.github.juliarn.npc.profile;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A wrapper for a game profile which can be completed.
 */
public class Profile implements Cloneable {

  private static final ThreadLocal<Gson> GSON = ThreadLocal
      .withInitial(() -> new GsonBuilder().serializeNulls().create());

  private static final String UUID_REQUEST_URL = "https://api.mojang.com/users/profiles/minecraft/%s";
  private static final String TEXTURES_REQUEST_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=%b";

  private static final Pattern UNIQUE_ID_PATTERN = Pattern
      .compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");
  private static final Type PROPERTY_LIST_TYPE = TypeToken
      .getParameterized(Set.class, Property.class).getType();

  private String name;
  private UUID uniqueId;
  private Collection<Property> properties;

  /**
   * Creates a new profile.
   *
   * @param uniqueId The unique id of the profile.
   */
  public Profile(@NotNull UUID uniqueId) {
    this(uniqueId, null);
  }

  /**
   * Creates a new profile.
   *
   * @param uniqueId   The unique id of the profile.
   * @param properties The properties of the profile.
   */
  public Profile(@NotNull UUID uniqueId, Collection<Property> properties) {
    this(uniqueId, null, properties);
  }

  /**
   * Creates a new profile.
   *
   * @param name The name of the profile.
   */
  public Profile(@NotNull String name) {
    this(name, null);
  }

  /**
   * Creates a new profile.
   *
   * @param name       The name of the profile.
   * @param properties The properties of the profile.
   */
  public Profile(@NotNull String name, Collection<Property> properties) {
    this(null, name, properties);
  }

  /**
   * Creates a new profile. Either {@code uniqueId} or {@code name} must be non-null.
   *
   * @param uniqueId   The unique id of the profile.
   * @param name       The name of the profile.
   * @param properties The properties of the profile.
   */
  public Profile(UUID uniqueId, String name, Collection<Property> properties) {
    Preconditions
        .checkArgument(name != null || uniqueId != null, "Either name or uniqueId must be given!");

    this.uniqueId = uniqueId;
    this.name = name;
    this.properties = properties;
  }

  /**
   * Checks if this profile is complete. Complete does not mean, that the profile has textures.
   *
   * @return if this profile is complete (has unique id and name)
   */
  public boolean isComplete() {
    return this.uniqueId != null && this.name != null;
  }

  /**
   * Checks if this profile has textures. That does not mean, that this profile has a name and
   * unique id.
   *
   * @return if this profile has textures.
   * @since 2.5-SNAPSHOT
   */
  public boolean hasTextures() {
    return this.getProperty("textures").isPresent();
  }

  /**
   * Checks if this profile has properties.
   *
   * @return if this profile has properties
   */
  public boolean hasProperties() {
    return this.properties != null && !this.properties.isEmpty();
  }

  /**
   * Fills this profiles with all missing attributes
   *
   * @return if the profile was successfully completed
   */
  public boolean complete() {
    return this.complete(true);
  }

  /**
   * Fills this profiles with all missing attributes
   *
   * @param propertiesAndName if properties and name should be filled for this profile
   * @return if the profile was successfully completed
   */
  public boolean complete(boolean propertiesAndName) {
    if (this.isComplete() && this.hasProperties()) {
      return true;
    }

    if (this.uniqueId == null) {
      JsonElement identifierElement = this.makeRequest(String.format(UUID_REQUEST_URL, this.name));
      if (identifierElement == null || !identifierElement.isJsonObject()) {
        return false;
      }

      JsonObject jsonObject = identifierElement.getAsJsonObject();
      if (jsonObject.has("id")) {
        this.uniqueId = UUID.fromString(
            UNIQUE_ID_PATTERN.matcher(jsonObject.get("id").getAsString())
                .replaceAll("$1-$2-$3-$4-$5"));
      } else {
        return false;
      }
    }

    if ((this.name == null || this.properties == null) && propertiesAndName) {
      JsonElement profileElement = this.makeRequest(
          String.format(TEXTURES_REQUEST_URL, this.uniqueId.toString().replace("-", ""), false));
      if (profileElement == null || !profileElement.isJsonObject()) {
        return false;
      }

      JsonObject object = profileElement.getAsJsonObject();
      if (object.has("name") && object.has("properties")) {
        this.name = this.name == null ? object.get("name").getAsString() : this.name;
        this.getProperties()
            .addAll(GSON.get().fromJson(object.get("properties"), PROPERTY_LIST_TYPE));
      } else {
        return false;
      }
    }

    return true;
  }

  /**
   * Makes a request to the given url, accepting only application/json.
   *
   * @param apiUrl The api url to make the request to.
   * @return The json element parsed from the result stream of the site.
   * @since 2.5-SNAPSHOT
   */
  protected @Nullable JsonElement makeRequest(@NotNull String apiUrl) {
    try {
      HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
      connection.setReadTimeout(5000);
      connection.setConnectTimeout(5000);
      connection.setUseCaches(true);
      connection.connect();

      if (connection.getResponseCode() == 200) {
        try (Reader reader = new InputStreamReader(connection.getInputStream(),
            StandardCharsets.UTF_8)) {
          return JsonParser.parseReader(reader);
        }
      }
      return null;
    } catch (IOException exception) {
      exception.printStackTrace();
      return null;
    }
  }

  /**
   * Checks if this profile has a unique id.
   *
   * @return if this profile has a unique id.
   * @since 2.5-SNAPSHOT
   */
  public boolean hasUniqueId() {
    return this.uniqueId != null;
  }

  /**
   * Get the unique id of this profile. May be null when this profile was created using a name and
   * is not complete. Is never null when {@link #hasUniqueId()} is {@code true}.
   *
   * @return the unique id of this profile.
   */
  public UUID getUniqueId() {
    return this.uniqueId;
  }

  /**
   * Sets the unique of this profile. To re-request the profile textures/uuid of this profile, make
   * sure the properties are clear.
   *
   * @param uniqueId the new unique of this profile.
   * @return the same profile instance, for chaining.
   */
  @NotNull
  public Profile setUniqueId(UUID uniqueId) {
    this.uniqueId = uniqueId;
    return this;
  }

  /**
   * Check if this profile has a name.
   *
   * @return if this profile has a name.
   * @since 2.5-SNAPSHOT
   */
  public boolean hasName() {
    return this.name != null;
  }

  /**
   * Get the name of this profile. May be null when this profile was created using a unique id and
   * is not complete. Is never null when {@link #hasName()} ()} is {@code true}.
   *
   * @return the name of this profile.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Sets the name of this profile. To re-request the profile textures/uuid of this profile, make
   * sure the properties are clear.
   *
   * @param name the new name of this profile.
   * @return the same profile instance, for chaining.
   */
  @NotNull
  public Profile setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Gets the properties of this profile.
   *
   * @return the properties of this profile.
   */
  @NotNull
  public Collection<Property> getProperties() {
    if (this.properties == null) {
      this.properties = ConcurrentHashMap.newKeySet();
    }
    return this.properties;
  }

  /**
   * Sets the properties of this profile.
   *
   * @param properties The new properties of this profile.
   */
  public void setProperties(Collection<Property> properties) {
    this.properties = properties;
  }

  /**
   * Adds the given {@code property} to this profile.
   *
   * @param property the property to add.
   * @return the same profile instance, for chaining.
   * @since 2.5-SNAPSHOT
   */
  @NotNull
  public Profile setProperty(@NotNull Property property) {
    this.getProperties().add(property);
    return this;
  }

  /**
   * Get a specific property by its name.
   *
   * @param name the name of the property.
   * @return the property.
   * @since 2.5-SNAPSHOT
   */
  public @NotNull Optional<Property> getProperty(@NotNull String name) {
    return this.getProperties().stream().filter(property -> property.getName().equals(name))
        .findFirst();
  }

  /**
   * Clears the properties of this profile.
   *
   * @since 2.5-SNAPSHOT
   */
  public void clearProperties() {
    this.getProperties().clear();
  }

  /**
   * Get the properties of this profile as a protocol lib wrapper. This is not recommended to use as
   * it creates a copy of all properties and requires protocol lib as a dependency. Use {@link
   * #getProperties()} instead.
   *
   * @return the properties of this profile as a protocol lib wrapper.
   * @deprecated Use {@link #getProperties()} instead.
   */
  @NotNull
  @Deprecated
  @ApiStatus.ScheduledForRemoval
  public Collection<WrappedSignedProperty> getWrappedProperties() {
    return this.getProperties().stream().map(Property::asWrapped).collect(Collectors.toList());
  }

  /**
   * Converts this profile to a protocol lib wrapper. This method requires protocol lib a dependency
   * of your project and is not the point of this class. It will be removed in a further release.
   *
   * @return this profile as a protocol lib wrapper.
   * @deprecated No longer supported for public use, convert it yourself when needed.
   */
  @NotNull
  @Deprecated
  @ApiStatus.ScheduledForRemoval
  public WrappedGameProfile asWrapped() {
    return this.asWrapped(true);
  }

  /**
   * Converts this profile to a protocol lib wrapper. This method requires protocol lib a dependency
   * of your project and is not the point of this class. It will be removed in a further release.
   *
   * @param withProperties If the properties of this wrapper should get copied.
   * @return this profile as a protocol lib wrapper.
   * @deprecated No longer supported for public use, convert it yourself when needed.
   */
  @NotNull
  @Deprecated
  @ApiStatus.ScheduledForRemoval
  public WrappedGameProfile asWrapped(boolean withProperties) {
    WrappedGameProfile profile = new WrappedGameProfile(this.getUniqueId(), this.getName());

    if (withProperties) {
      this.getProperties().forEach(
          property -> profile.getProperties().put(property.getName(), property.asWrapped()));
    }

    return profile;
  }

  /**
   * Creates a clone of this profile.
   *
   * @return the cloned profile.
   */
  @Override
  public Profile clone() {
    try {
      return (Profile) super.clone();
    } catch (CloneNotSupportedException exception) {
      return new Profile(this.uniqueId, this.name,
          this.properties == null ? null : new HashSet<>(this.properties));
    }
  }

  /**
   * A property a profile can contain. A property must be immutable.
   */
  public static class Property {

    private final String name;
    private final String value;
    private final String signature;

    /**
     * Creates a new profile property object.
     *
     * @param name      The name of the profile property.
     * @param value     The value of the property.
     * @param signature The signature of the property or null if the property is not signed.
     */
    public Property(@NotNull String name, @NotNull String value, @Nullable String signature) {
      this.name = name;
      this.value = value;
      this.signature = signature;
    }

    /**
     * Get the name of this property.
     *
     * @return the name of this property.
     */
    @NotNull
    public String getName() {
      return this.name;
    }

    /**
     * The value of this property.
     *
     * @return the value of this property.
     */
    @NotNull
    public String getValue() {
      return this.value;
    }

    /**
     * Get the signature of this profile. It might be null, but must never be null when {@link
     * #isSigned()} is {@code true}.
     *
     * @return the signature of this profile.
     */
    @Nullable
    public String getSignature() {
      return this.signature;
    }

    /**
     * Get if this property has a signature.
     *
     * @return if this property has a signature.
     */
    public boolean isSigned() {
      return this.signature != null;
    }

    /**
     * Converts this property to a protocol lib wrapper. This method is no longer supported for
     * public use and will be removed in a further release as its requiring protocol lib as a
     * project dependency which is not the point of this wrapper class.
     *
     * @return this property to a protocol lib wrapper
     * @deprecated No longer supported for public use, convert it yourself when needed.
     */
    @NotNull
    @Deprecated
    @ApiStatus.Internal
    public WrappedSignedProperty asWrapped() {
      return new WrappedSignedProperty(this.getName(), this.getValue(), this.getSignature());
    }
  }
}
