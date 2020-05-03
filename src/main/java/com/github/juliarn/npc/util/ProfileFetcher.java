package com.github.juliarn.npc.util;

import com.github.derklaro.requestbuilder.RequestBuilder;
import com.github.derklaro.requestbuilder.method.RequestMethod;
import com.github.derklaro.requestbuilder.result.RequestResult;
import com.github.derklaro.requestbuilder.result.http.StatusCode;
import com.github.derklaro.requestbuilder.types.MimeTypes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class ProfileFetcher {

    private ProfileFetcher() {
        throw new UnsupportedOperationException();
    }

    private static final String TEXTURES_REQUEST_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=%b";

    private static final Map<UUID, Profile> CACHE = new ConcurrentHashMap<>();

    private static final ThreadLocal<Gson> GSON = ThreadLocal.withInitial(
            () -> new GsonBuilder().serializeNulls().create()
    );

    @Nullable
    public static Profile getProfile(@NotNull UUID uniqueID) {
        return getProfile(uniqueID, true);
    }

    @Nullable
    public static Profile getProfile(@NotNull UUID uniqueID, boolean secure) {
        return getProfile(uniqueID, secure, true);
    }

    @Nullable
    public static Profile getProfile(@NotNull UUID uniqueID, boolean secure, boolean useCache) {
        if (useCache && CACHE.containsKey(uniqueID)) {
            return CACHE.get(uniqueID);
        }

        RequestBuilder builder = RequestBuilder
                .newBuilder(String.format(TEXTURES_REQUEST_URL, uniqueID.toString().replace("-", ""), !secure))
                .setConnectTimeout(10, TimeUnit.SECONDS)
                .setRequestMethod(RequestMethod.GET)
                .enableRedirectFollow()
                .accepts(MimeTypes.getMimeType("json"));

        try (RequestResult requestResult = builder.fireAndForget()) {
            if (requestResult.getStatus() != StatusCode.OK) {
                return null;
            }

            Profile profile = GSON.get().fromJson(requestResult.getResultAsString(), Profile.class);
            if (profile == null || !useCache) {
                return profile;
            }

            CACHE.put(uniqueID, profile);
            return profile;
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

}
