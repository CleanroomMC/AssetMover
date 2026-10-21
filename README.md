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
