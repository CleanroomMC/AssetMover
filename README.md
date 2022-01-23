# AssetMover

***Allows acquiring of vanilla/mod assets at runtime instead of including them in builds potentially violating licenses.***

### Features:

1. Downloading Minecraft assets, CurseForge mods and jars from specified URLs.
2. Locating these in `.minecraft/assetmover` folder resource pack that is hidden in-game.
3. Dev environment compatibility.

### For Developers:

1. Add CleanroomMC's repository and query for AssetMover's maven entry:

```
repositories {
    maven {
        url "https://maven.cleanroommc.com"
    }
}

dependencies {
    deobfCompile ("com.cleanroommc:assetmover:0.2")
}
```

2. Use `AssetMoverAPI`. Make sure you use these methods before `FMLInitializationEvent` is fired, anything later would be too late.
