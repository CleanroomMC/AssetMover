# AssetMover

***Allows acquiring of vanilla/mod assets at runtime instead of including them in builds potentially violating licenses.***

### Features:

1. Downloading assets from Minecraft (Java and Bedrock), CurseForge, Modrinth, GitHub, Maven repositories and URLs.
2. Locating these in `.minecraft/assetmover` folder resource pack that is hidden in-game.
3. Dev environment compatibility.

### For Developers:

1. Add CleanroomMC's repository and query for AssetMover's maven entry:

```
repositories {
    maven {
        url 'https://maven.cleanroommc.com'
    }
}

dependencies {
    implementation 'com.cleanroommc:assetmover:3.0.0'
}
```

2. Register a listener on `MinecraftForge.EVENT_BUS` no later than `FMLConstructionEvent`, and queue assets through `MoveAssetEvent`:

```java
@SubscribeEvent
public void onMoveAssets(MoveAssetEvent event) {
    AssetMoverAPI api = event.getAssetMover();
    api.fromJava("1.20.1")
        .moveAs("assets/minecraft/textures/block/dirt.png", "assets/mymod/textures/block/dirt.png");
    api.fromModSite(ModSite.CURSEFORGE, "238222", "4593548")
        .move("assets/jei/lang/en_us.lang", "assets/jei/lang/de_de.lang");
}
```

### Timeouts

Each download gives up after 30 seconds without a connection or without data. Override it per source with a system property, in seconds:

| Property                                  | Source                                  |
|-------------------------------------------|-----------------------------------------|
| `cleanroom.asset-mover.java.timeout`      | `fromJava`                              |
| `cleanroom.asset-mover.bedrock.timeout`   | `fromBedrock`                           |
| `cleanroom.asset-mover.mod-site.timeout`  | `fromModSite`                           |
| `cleanroom.asset-mover.git.timeout`       | `fromGit`, `fromGitRelease`             |
| `cleanroom.asset-mover.maven.timeout`     | `fromMaven`, `fromMavenCentral`         |
| `cleanroom.asset-mover.url.timeout`       | `fromUrl`, `fromUrlFile`                |

Downloads run on `max(4, cores * 2)` threads, which `cleanroom.asset-mover.threads` overrides.
