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

import com.github.juliarn.npclib.api.util.Util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class MojangProfileResolver implements ProfileResolver {

  public static final MojangProfileResolver INSTANCE = new MojangProfileResolver();

  private static final int DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);

  private static final Type PROFILE_PROPERTIES_TYPE = TypeToken
    .getParameterized(Set.class, ProfileProperty.class)
    .getType();
  private static final Gson GSON = new GsonBuilder()
    .disableJdkUnsafe()
    .disableHtmlEscaping()
    .registerTypeAdapter(ProfileProperty.class, new ProfilePropertyTypeAdapter())
    .create();

  private static final Pattern UUID_NO_DASH_PATTERN = Pattern.compile("-", Pattern.LITERAL);
  private static final Pattern UUID_DASHER_PATTERN = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");

  private static final String NAME_TO_UUID_ENDPOINT = "https://api.mojang.com/users/profiles/minecraft/%s";
  private static final String UUID_TO_PROFILE_ENDPOINT = "https://sessionserver.mojang.com/session/minecraft/profile/%s";

  @Override
  public @NotNull CompletableFuture<Profile.Resolved> resolveProfile(@NotNull Profile profile) {
    return CompletableFuture.supplyAsync(Util.callableToSupplier(() -> {
      // check if we need to resolve the uuid of the profile
      UUID uniqueId = profile.uniqueId();
      if (uniqueId == null) {
        // this will give us either a valid object or throw an exception
        JsonObject responseData = makeRequest(String.format(NAME_TO_UUID_ENDPOINT, profile.name()));
        String rawUniqueId = responseData.get("id").getAsString();

        // insert dashes into the unique id string we get to parse it
        String dashedId = UUID_DASHER_PATTERN.matcher(rawUniqueId).replaceAll("$1-$2-$3-$4-$5");
        uniqueId = UUID.fromString(dashedId);
      }

      // now as the unique id is present we can send the request to get the all the other information about the profile
      String profileId = UUID_NO_DASH_PATTERN.matcher(uniqueId.toString()).replaceAll("");
      JsonObject responseData = makeRequest(String.format(UUID_TO_PROFILE_ENDPOINT, profileId));

      // get the name of the player
      String name = responseData.get("name").getAsString();
      Set<ProfileProperty> properties = GSON.fromJson(responseData.get("properties"), PROFILE_PROPERTIES_TYPE);

      // create the profile from the received data
      return Profile.resolved(name, uniqueId, properties);
    }));
  }

  private static @NotNull JsonObject makeRequest(@NotNull String endpoint) throws IOException {
    HttpURLConnection connection = createBaseConnection(endpoint);

    // little hack - we cannot just follow redirects as some endpoints (for example CF workers)
    // are setting a cookie and redirect us, we need to keep that cookie for the next request
    // so we re-request the site when we were redirected
    int redirectCount = 0;
    do {
      connection.connect();

      // check for a redirect
      int status = connection.getResponseCode();
      boolean redirect = status == HttpURLConnection.HTTP_MOVED_TEMP
        || status == HttpURLConnection.HTTP_MOVED_PERM
        || status == HttpURLConnection.HTTP_SEE_OTHER;

      if (redirect) {
        // get the cookies and the target endpoint
        String cookies = connection.getHeaderField("Set-Cookie");
        String redirectTarget = connection.getHeaderField("Location");

        // retry the request
        connection = createBaseConnection(redirectTarget);
        connection.setRequestProperty("Cookie", cookies);
      } else {
        // we are connected successfully
        if (status == HttpURLConnection.HTTP_OK) {
          // parse the incoming data
          try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
          }
        } else {
          // rate limit, invalid name/uuid etc.
          throw new IllegalArgumentException("Unable to fetch data, server responded with " + status);
        }
      }
    } while (redirectCount++ < 10);

    // too many redirects
    throw new IllegalStateException("Endpoint request redirected more than 10 times!");
  }

  private static @NotNull HttpURLConnection createBaseConnection(@NotNull String endpoint) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();

    // default properties
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Connection", "close");
    connection.setRequestProperty("Accept", "application/json");
    connection.setRequestProperty("User-Agent", "juliarn/npc-lib2");

    // ensure that the request will not take forever
    connection.setReadTimeout(DEFAULT_TIMEOUT);
    connection.setConnectTimeout(DEFAULT_TIMEOUT);

    // ensure that these are 'true' even if the defaults changed
    connection.setUseCaches(true);
    connection.setInstanceFollowRedirects(true);

    return connection;
  }

  private static final class ProfilePropertyTypeAdapter extends TypeAdapter<ProfileProperty> {

    @Override
    public void write(@NotNull JsonWriter out, @Nullable ProfileProperty property) throws IOException {
      if (property != null) {
        out
          .beginObject()
          .name("name").value(property.name())
          .name("value").value(property.value())
          .name("signature").value(property.signature())
          .endObject();
      }
    }

    @Override
    public @Nullable ProfileProperty read(@NotNull JsonReader in) throws IOException {
      // early break if the value is null
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }

      // the values we might find
      String name = null;
      String value = null;
      String signature = null;

      // begin the next object and read it until it's over
      in.beginObject();
      while (in.peek() != JsonToken.END_OBJECT) {
        String fieldName = in.nextName();

        // check if we know the field
        switch (fieldName.toLowerCase()) {
          case "name":
            name = in.nextString();
            break;
          case "value":
            value = in.nextString();
            break;
          case "signature":
            // normally should not be included, just to be sure
            if (in.peek() == JsonToken.NULL) {
              in.nextNull();
            } else {
              signature = in.nextString();
            }
            break;
          default:
            // unknown value ¯\_(ツ)_/¯
            in.skipValue();
            break;
        }
      }

      // finish reading
      in.endObject();

      // ensure that all values are present to create the property object
      return name != null && value != null ? ProfileProperty.property(name, value, signature) : null;
    }
  }
}
