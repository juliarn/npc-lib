package com.github.realpanamo.npc;


import org.bukkit.Bukkit;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionUtil {

    private static int minecraftVersion = -1;

    static {
        Pattern versionPattern = Pattern.compile("\\(MC: (\\d)\\.(\\d+)\\.?(\\d+?)?\\)");
        Matcher matcher = versionPattern.matcher(Bukkit.getVersion());

        if (matcher.find()) {
            MatchResult matchResult = matcher.toMatchResult();

            try {
                VersionUtil.minecraftVersion = Integer.parseInt(matchResult.group(2), 10);
            } catch (Exception exception) {
                Bukkit.getLogger().warning("Failed to parse version of this server!");
            }
        }
    }

    public static int getMinecraftVersion() {
        return minecraftVersion;
    }

}
