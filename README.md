# NPC-Lib

Asynchronous, high-performance Minecraft NPC library for 1.8-1.18 servers.

This Library does only support the latest patch release of a supported version (for example 1.13.2).
Issues with older patch versions (for example 1.13.1) won't be fixed.

## Requirements

This library can only be used on spigot servers higher or on version 1.8.8. The
plugin [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) is required on your server.

## How to use

Add the repository and dependency to your plugin:

Maven

```xml

<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<!-- latest stable release -->
<dependency>
    <groupId>com.github.juliarn</groupId>
    <artifactId>npc-lib</artifactId>
    <version>2.6-RELEASE</version>
</dependency>
<!-- latest build -->
<dependency>
    <groupId>com.github.juliarn</groupId>
    <artifactId>npc-lib</artifactId>
    <version>development-SNAPSHOT</version>
</dependency>
```

Gradle

```groovy
maven {
    name 'jitpack.io'
    url 'https://jitpack.io'
}

// latest stable release
compile group: 'com.github.juliarn', name: 'npc-lib', version: '2.6-RELEASE'
// latest build
compile group: 'com.github.juliarn', name: 'npc-lib', version: 'development-SNAPSHOT'
```

Add ProtocolLib as dependency to your plugin.yml. It could look like this:

```yml
name: Hub
version: 1.0-SNAPSHOT
api-version: "1.13"
depend: [ProtocolLib]
author: juliarn
main: com.github.juliarn.server.hub.ServerHub
```

Now you're all set! You can start by creating an instance of the
[NPCPool](https://github.com/juliarn/NPC-Lib/blob/development/src/main/java/com/github/juliarn/npc/NPCPool.java)
and the
[NPC.Builder](https://github.com/juliarn/NPC-Lib/blob/development/src/main/java/com/github/juliarn/npc/NPC.java).

### Example usage

```java
public class ExampleNPCs implements Listener {

  /**
   * The NPC pool managing all NPCs.
   */
  private final NPCPool npcPool;

  private final Random random;

  /**
   * @param plugin The plugin which uses the lib
   */
  public ExampleNPCs(Plugin plugin) {
    this.npcPool = NPCPool.builder(plugin)
      .spawnDistance(60)
      .actionDistance(30)
      .tabListRemoveTicks(20)
      .build();
    this.random = new Random();

    // we are handling NPC events here
    Bukkit.getPluginManager().registerEvents(this, plugin);

    // making the NPCs randomly sneak and unsneak every second
    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      for (NPC npc : this.npcPool.getNPCs()) {
        npc.metadata()
          .queue(MetadataModifier.EntityMetadata.SNEAKING, this.random.nextBoolean())
          // sending the change to all players
          .send();
      }
    }, 20L, 20L);
  }

  /**
   * Appends a new NPC to the pool.
   *
   * @param location       The location the NPC will be spawned at
   * @param excludedPlayer A player which will not see the NPC
   */
  public void appendNPC(Location location, Player excludedPlayer) {
    // building the NPC
    NPC npc = NPC.builder()
      .profile(this.createProfile())
      .location(location)
      .imitatePlayer(false)
      .lookAtPlayer(false)
      // appending it to the NPC pool
      .build(this.npcPool);

    if (excludedPlayer != null) {
      // adding the excluded player which will not see the NPC
      npc.addExcludedPlayer(excludedPlayer);
    }
  }

  /**
   * Creates an example profile for NPCs.
   *
   * @return The new profile
   */
  public Profile createProfile() {
    Profile profile = new Profile(UUID.fromString("fdef0011-1c58-40c8-bfef-0bdcb1495938"));
    // Synchronously completing the profile, as we only created the profile with a UUID.
    // Through this, the name and textures will be filled in.
    profile.complete();

    // we want to keep the textures, but change the name and UUID.
    profile.setName("Notch");
    // with a UUID like this, the NPC can play LabyMod emotes!
    profile.setUniqueId(new UUID(this.random.nextLong(), 0));

    return profile;
  }

  /**
   * Doing something when a NPC is shown for a certain player.
   * Alternatively, {@link NPC.Builder#spawnCustomizer(SpawnCustomizer)} can be used.
   *
   * @param event The event instance
   */
  @EventHandler
  public void handleNPCShow(PlayerNPCShowEvent event) {
    NPC npc = event.getNPC();

    // sending the data only to the player from the event
    event.send(
      // making the NPC swing its main arm
      npc.animation()
        .queue(AnimationModifier.EntityAnimation.SWING_MAIN_ARM),
      // making the NPC play the LabyMod "dab" emote.
      // all ids can be found here: https://docs.labymod.net/pages/server/labymod/emote_api/
      npc.labymod()
        .queue(LabyModModifier.LabyModAction.EMOTE, 3),
      // equipping the NPC with a diamond chestplate
      npc.equipment()
        .queue(EquipmentModifier.CHEST, new ItemStack(Material.DIAMOND_CHESTPLATE, 1)),
      // enabling the skin layers and letting the NPC sneak
      npc.metadata()
        .queue(MetadataModifier.EntityMetadata.SKIN_LAYERS, true)
        .queue(MetadataModifier.EntityMetadata.SNEAKING, true));
  }

  /**
   * Doing something when a NPC is hidden for a certain player.
   *
   * @param event The event instance
   */
  @EventHandler
  public void handleNPCHide(PlayerNPCHideEvent event) {
    Player player = event.getPlayer();
    NPC npc = event.getNPC();

    // if the player has been excluded from seeing the NPC, removing him from the excluded players
    if (event.getReason() == PlayerNPCHideEvent.Reason.EXCLUDED) {
      npc.removeExcludedPlayer(player);
    }
  }

  /**
   * Doing something when a player interacts with a NPC.
   *
   * @param event The event instance
   */
  @EventHandler
  public void handleNPCInteract(PlayerNPCInteractEvent event) {
    Player player = event.getPlayer();
    NPC npc = event.getNPC();

    // checking if the player hit the NPC
    if (event.getUseAction() == PlayerNPCInteractEvent.EntityUseAction.ATTACK) {
      player.sendMessage("Why are you hitting me? :(");
      // making the NPC look at the player
      npc.rotation()
        .queueLookAt(player.getEyeLocation())
        // sending the change only to the player who interacted with the NPC
        .send(player);
    }
  }
}
```
