# Changelog

## 3.0.1 - 2026-10-31

### Bug Fix

- Null minecraft home for server side *[commit by [@Rongmario](https://github.com/Rongmario) in [4dd0cae](https://github.com/CleanroomMC/AssetMover/commit/4dd0cae4db10da3b03e609774290fd9ef89f70a3)]*

**Full Changelog**: https://github.com/CleanroomMC/AssetMover/compare/3.0.0...3.0.1

## 3.0.0 - 2026-10-29

> [!IMPORTANT]
> This is a new major version (2.x → 3.x).

### Feature

- Fire MoveAssetEvent.Server during pre-initialization on the dedicated server *[commit by [@Rongmario](https://github.com/Rongmario) in [e79c59f](https://github.com/CleanroomMC/AssetMover/commit/e79c59f791d0ee0fc17e984847b29900e6ed9339)]*
- **api**: Split MoveAssetEvent into client and server events *[commit by [@Rongmario](https://github.com/Rongmario) in [9e51720](https://github.com/CleanroomMC/AssetMover/commit/9e517202607d5fc0208c4d89d02217a0072ec56e)]*
- Report move progress and size the pool from available cores *[commit by [@Rongmario](https://github.com/Rongmario) in [e491642](https://github.com/CleanroomMC/AssetMover/commit/e491642057e21e6e1b2232a6cd1f60f5d601b57c)]*
- Per-source download timeouts and versioned user agent *[commit by [@Rongmario](https://github.com/Rongmario) in [0c42724](https://github.com/CleanroomMC/AssetMover/commit/0c42724a2ed2c83e5350d306835ce1c5c0861a9f)]*
- **api**: Generalise GitHub sources into GitHost *[commit by [@Rongmario](https://github.com/Rongmario) in [dd24ed0](https://github.com/CleanroomMC/AssetMover/commit/dd24ed0a922d9a4f17039152b05839ff19582402)]*
- **api**: Event-based 3.0 API with parallel downloads *[commit by [@Rongmario](https://github.com/Rongmario) in [77b229a](https://github.com/CleanroomMC/AssetMover/commit/77b229aa2e98ead3a0bbe6c88e51e9a36d2aaccc)]*

### Bug Fix

- Pop the progress bar when moving is interrupted *[commit by [@Rongmario](https://github.com/Rongmario) in [51a8aa5](https://github.com/CleanroomMC/AssetMover/commit/51a8aa51a00859e3cdc5935b71d382084e692437)]*
- Name sources readably in progress and logs *[commit by [@Rongmario](https://github.com/Rongmario) in [19af04e](https://github.com/CleanroomMC/AssetMover/commit/19af04e9b26b950c16b8a6dc93c622ec8d5bf4f6)]*
- Register the 2.x listener during construction and point the icon at its moved path *[commit by [@Rongmario](https://github.com/Rongmario) in [40ee1aa](https://github.com/CleanroomMC/AssetMover/commit/40ee1aaa013e962022d9b351426d5629c87f8625)]*
- **build**: Limit token replacement to the main source set *[commit by [@Rongmario](https://github.com/Rongmario) in [9287e1b](https://github.com/CleanroomMC/AssetMover/commit/9287e1b528b2c93eac4c68ff733ee2c712f908bf)]*
- **api**: Resolve CurseForge files through curse.cleanroommc.com *[commit by [@Rongmario](https://github.com/Rongmario) in [a3f81b5](https://github.com/CleanroomMC/AssetMover/commit/a3f81b57bd99e963237620b4794bb666f71c1e4d)]*

### Documentation

- Document every API parameter *[commit by [@Rongmario](https://github.com/Rongmario) in [60ee021](https://github.com/CleanroomMC/AssetMover/commit/60ee02115b4f6df6e9e34d80742c5d319b76a3b7)]*

### Testing

- Cover archive extraction, sharing, skipping, checksums and timeouts *[commit by [@Rongmario](https://github.com/Rongmario) in [2cf5e40](https://github.com/CleanroomMC/AssetMover/commit/2cf5e402380f466843f69ee42020dc3efeccb084)]*

### Build and Dependencies

- **deps**: Update conventions to 1.4.4 *[commit by [@Rongmario](https://github.com/Rongmario) in [d3b475b](https://github.com/CleanroomMC/AssetMover/commit/d3b475bb8c9184ce504d375a24723c953f4419a4)]*
- Migrate to Cleanroom's Conventions 1.4.2 *[commit by [@Rongmario](https://github.com/Rongmario) in [72bcf50](https://github.com/CleanroomMC/AssetMover/commit/72bcf5082186889e48c3f97a1ae1df6c11f92351)]*
- **deps**: Update gradle wrapper to 9.8.1 *[commit by [@Rongmario](https://github.com/Rongmario) in [4801fe8](https://github.com/CleanroomMC/AssetMover/commit/4801fe8e3f65c36210d645431d355a2f850388f2)]*

### Other

- Finalized v3 API! *[commit by [@Rongmario](https://github.com/Rongmario) in [0c412f9](https://github.com/CleanroomMC/AssetMover/commit/0c412f95cb488b74aebe9c8754cf6a6d862a8116)]*
- Update buildscript *[commit by [@Rongmario](https://github.com/Rongmario) in [f8e7090](https://github.com/CleanroomMC/AssetMover/commit/f8e709043967b0144d1c4eed3a1d983f8f85df6e)]*
- Fix version json file path *[commit by [@jbredwards](https://github.com/jbredwards) in [af7fc8b](https://github.com/CleanroomMC/AssetMover/commit/af7fc8b5dc3aba00fc0b219c8e94b4ef72493fcd)]*
- Fixes #14 *[commit by [@jbredwards](https://github.com/jbredwards) in [ba2373d](https://github.com/CleanroomMC/AssetMover/commit/ba2373d4e8528449f9138770dcc34158354990d8), issue by [@juraj-hrivnak](https://github.com/juraj-hrivnak)]*
- Update README code block to show v2.5 *[commit by [@Rongmario](https://github.com/Rongmario) in [1dad932](https://github.com/CleanroomMC/AssetMover/commit/1dad9326e4635a26d6706aa128438f7e63f262e2)]*

### First-time Contributors

- **[@jbredwards](https://github.com/jbredwards) made their first contribution!**

**Full Changelog**: https://github.com/CleanroomMC/AssetMover/compare/2.5...3.0.0

## 2.5 - 2023-03-10

### Other

- 2.5 - set maps to null rather than clearing them first *[commit by [@Rongmario](https://github.com/Rongmario) in [b4ce37f](https://github.com/CleanroomMC/AssetMover/commit/b4ce37f90f04abdd040655e5fd741bfd9fcc4bc3)]*
- Clarify in README *[commit by [@Rongmario](https://github.com/Rongmario) in [ec60903](https://github.com/CleanroomMC/AssetMover/commit/ec60903bdc211ded6ac91c0b4646bfcf0206ef41)]*

**Full Changelog**: https://github.com/CleanroomMC/AssetMover/compare/2.4...2.5

## 2.4 - 2023-03-09

### Other

- Bump to 2.4 - Resolves #1, Fixes #10 *[commit by [@Rongmario](https://github.com/Rongmario) in [c5fa3ff](https://github.com/CleanroomMC/AssetMover/commit/c5fa3ffcd2b0276277f9b58e5dbc97006298764c), issue by [@ChAoSUnItY](https://github.com/ChAoSUnItY), [@jbredwards](https://github.com/jbredwards)]*
- Appropriating error logging when user is using Java 8u51, resolves #8 *[commit by [@Rongmario](https://github.com/Rongmario) in [314f545](https://github.com/CleanroomMC/AssetMover/commit/314f5454451aff579e56087e5bf7ac1449ade09e), issue by [@SlayerTheChikken](https://github.com/SlayerTheChikken)]*
- Validate and enforce calls before or during FMLConstructionEvent *[commit by [@Rongmario](https://github.com/Rongmario) in [1e20063](https://github.com/CleanroomMC/AssetMover/commit/1e20063784d0d6469b06552d7e0e3a235919cc3e)]*
- Fatal logging for when the version json could not be retrieved *[commit by [@Rongmario](https://github.com/Rongmario) in [3b1fd71](https://github.com/CleanroomMC/AssetMover/commit/3b1fd711ae0703aaf8cd54d7f811f1e6b549d8a8)]*

**Full Changelog**: https://github.com/CleanroomMC/AssetMover/compare/2.3...2.4

## 2.3 - 2023-02-23

### Other

- Update AssetMoverHelper.java *[commit by [@KuroNeko87](https://github.com/KuroNeko87) in [9082004](https://github.com/CleanroomMC/AssetMover/commit/9082004fc7dfc56148a97245dabab61655f7d09a)]*
- Bump to 2.3 *[commit by [@Rongmario](https://github.com/Rongmario) in [93e7b42](https://github.com/CleanroomMC/AssetMover/commit/93e7b422b99d621c882f8e077cdfd02b2c1d91bb)]*

### First-time Contributors

- **[@KuroNeko87](https://github.com/KuroNeko87) made their first contribution!**

**Full Changelog**: https://github.com/CleanroomMC/AssetMover/compare/2.2...2.3

## 2.2 - 2023-02-17

### Other

- Bump to 2.2 *[commit by [@Rongmario](https://github.com/Rongmario) in [76a0fe6](https://github.com/CleanroomMC/AssetMover/commit/76a0fe655164eada903d2c05f3fc99dcf75a4479)]*
- Fixes #5 *[commit by [@Rongmario](https://github.com/Rongmario) in [7cc30e1](https://github.com/CleanroomMC/AssetMover/commit/7cc30e15754466827d87a63626c01f48145b7829), issue by [@jbredwards](https://github.com/jbredwards)]*

**Full Changelog**: https://github.com/CleanroomMC/AssetMover/compare/2.1...2.2

## 2.1 - 2022-11-23

### Other

- Bump to 2.1 *[commit by [@Rongmario](https://github.com/Rongmario) in [76308e6](https://github.com/CleanroomMC/AssetMover/commit/76308e6c742372941c90664a449b79242271a833)]*
- Grab client JsonObject first before querying for url string *[commit by [@Rongmario](https://github.com/Rongmario) in [69f59f9](https://github.com/CleanroomMC/AssetMover/commit/69f59f957a14ba5ae23fda30ab87f1ec6a8c900b)]*

**Full Changelog**: https://github.com/CleanroomMC/AssetMover/compare/2.0...2.1

## 2.0 - 2022-11-20

> [!IMPORTANT]
> This is a new major version (1.x → 2.x).

### Other

- Bump to 2.0 *[commit by [@Rongmario](https://github.com/Rongmario) in [90f36e0](https://github.com/CleanroomMC/AssetMover/commit/90f36e0bd8d4d630cc80c3d789200d2d7cfc03d4)]*
- Allow partial asset updates *[commit by [@Rongmario](https://github.com/Rongmario) in [d1e2f20](https://github.com/CleanroomMC/AssetMover/commit/d1e2f2011eae1fb21653ca6777d19f3659cd6bed)]*
- Implement off-thread downloader + more logging *[commit by [@Rongmario](https://github.com/Rongmario) in [231406d](https://github.com/CleanroomMC/AssetMover/commit/231406d935c64c089a2ff622ee5ec9076cc3c017)]*
- Fixed typos *[commit by [@Rongmario](https://github.com/Rongmario) in [3068d2a](https://github.com/CleanroomMC/AssetMover/commit/3068d2a0fc9d58f2ff88dd63178b18dd01b0d182)]*
- Utilize logger more *[commit by [@Rongmario](https://github.com/Rongmario) in [5e820c3](https://github.com/CleanroomMC/AssetMover/commit/5e820c3558b25bcac0e90445fbf87dfa7148e3af)]*
- More logging + error handling *[commit by [@Rongmario](https://github.com/Rongmario) in [cec7ab7](https://github.com/CleanroomMC/AssetMover/commit/cec7ab76d83b0851d4c2b036aeef7c577b08e4a1)]*
- Working fallback to client jar to find remaining assets *[commit by [@Rongmario](https://github.com/Rongmario) in [920ac8f](https://github.com/CleanroomMC/AssetMover/commit/920ac8f8963c85c770c1ffdb55aa828a17dd946c)]*
- Now attempts to extract minecraft assets locally first *[commit by [@Rongmario](https://github.com/Rongmario) in [8cecb87](https://github.com/CleanroomMC/AssetMover/commit/8cecb87c19678266bd014e4c044005e49415bce7)]*
- Update AssetMoverAPI.java *[commit by Li in [c42226e](https://github.com/CleanroomMC/AssetMover/commit/c42226ea4add261a8ab713c686b016922e0188a4)]*
- Update AssetMoverAPI.java *[commit by Li in [ff74b04](https://github.com/CleanroomMC/AssetMover/commit/ff74b04c393c2822695f4af0b1a8330ca2af7c42)]*
- Update AssetMoverAPI.java *[commit by Li in [df2d5f2](https://github.com/CleanroomMC/AssetMover/commit/df2d5f2f7b113b5971f803b058b71197ec469890)]*

**Full Changelog**: https://github.com/CleanroomMC/AssetMover/compare/1.1...2.0

## 1.1 - 2022-05-29

### Other

- Fix logo location conflict + bump to 1.1 *[commit by [@Rongmario](https://github.com/Rongmario) in [443b204](https://github.com/CleanroomMC/AssetMover/commit/443b2044c627fda95657a3d5f0dfb946d9553ed9)]*
- Fixes #2, add logging + cleanup *[commit by [@Rongmario](https://github.com/Rongmario) in [a26e725](https://github.com/CleanroomMC/AssetMover/commit/a26e72502ab0da307b55ba0d8092a1fae558f279), issue by [@xemnes](https://github.com/xemnes)]*
- Change FG version *[commit by [@Rongmario](https://github.com/Rongmario) in [7c220d1](https://github.com/CleanroomMC/AssetMover/commit/7c220d1039441c8a0dd4376e24ee8d1b31f9e5ec)]*

**Full Changelog**: https://github.com/CleanroomMC/AssetMover/compare/1.0...1.1

## 1.0 - 2022-01-23

> [!IMPORTANT]
> This is a new major version (0.x → 1.x).

### Other

- Bump to 1.0 *[commit by [@Rongmario](https://github.com/Rongmario) in [6bfd8a8](https://github.com/CleanroomMC/AssetMover/commit/6bfd8a8b32cd2292c8724a2f3a0069f93dfc654f)]*
- Added dev instructions in README *[commit by [@Rongmario](https://github.com/Rongmario) in [e6b59e6](https://github.com/CleanroomMC/AssetMover/commit/e6b59e6aabb9f2d104b6baa96795725e2febd694)]*
- Fix fully qualified name *[commit by [@Rongmario](https://github.com/Rongmario) in [4700514](https://github.com/CleanroomMC/AssetMover/commit/47005148bee294c4e67728648118de231779ddcc)]*
- Move to new template + work on cleanroom maven *[commit by [@Rongmario](https://github.com/Rongmario) in [f0e1e9b](https://github.com/CleanroomMC/AssetMover/commit/f0e1e9bd004c5af13c3eda915e5739a141ba07f8)]*
- LGPLv3 *[commit by [@Rongmario](https://github.com/Rongmario) in [93c6947](https://github.com/CleanroomMC/AssetMover/commit/93c69476c0db8ed013e87ef5d732fe0016a4afcd)]*
- Update README.md *[commit by [@Rongmario](https://github.com/Rongmario) in [41db25e](https://github.com/CleanroomMC/AssetMover/commit/41db25ecb77f51117ff187eb0ff0957747216826)]*
- Fixed overly-zealous duplicates checks *[commit by [@Rongmario](https://github.com/Rongmario) in [0b7bb2d](https://github.com/CleanroomMC/AssetMover/commit/0b7bb2dc0069692fcfb4a50c78de8b26e19d193c)]*

**Full Changelog**: https://github.com/CleanroomMC/AssetMover/compare/0.2...1.0

## 0.2 - 2022-01-04

### Other

- Fix CurseForge stupidness, marked as volatile - bumped to 0.2 *[commit by [@Rongmario](https://github.com/Rongmario) in [b51a3f2](https://github.com/CleanroomMC/AssetMover/commit/b51a3f24bb2bb6486e279a77c7fc14635347606c)]*
- Refactored again... now supports urls, curseforge mods directly too *[commit by [@Rongmario](https://github.com/Rongmario) in [dd5b833](https://github.com/CleanroomMC/AssetMover/commit/dd5b833dd93e742a77808f9bfd0529da80819054)]*

**Full Changelog**: https://github.com/CleanroomMC/AssetMover/compare/0.1...0.2

## 0.1 - 2022-01-03

### Other

- Mark RequestURLAssets as WIP + bump to 0.1 *[commit by [@Rongmario](https://github.com/Rongmario) in [b11a6d7](https://github.com/CleanroomMC/AssetMover/commit/b11a6d7ecd99ae2f02050db969bd34fbd21e0c2d)]*
- Now works outside dev env. Via a hidden internal resource pack. *[commit by [@Rongmario](https://github.com/Rongmario) in [447ad69](https://github.com/CleanroomMC/AssetMover/commit/447ad69d8043aa37ed2eb0889a2b6303846936e9)]*
- Commit before breakfast - solved an oversight *[commit by [@Rongmario](https://github.com/Rongmario) in [8ec4c44](https://github.com/CleanroomMC/AssetMover/commit/8ec4c44c57fcadb332aa70684905df2ded751013)]*
- Replaced mkdirs call for ZipFiles *[commit by [@Rongmario](https://github.com/Rongmario) in [b6c9f46](https://github.com/CleanroomMC/AssetMover/commit/b6c9f461f4ce199f13b64dd19ae3be80b71af697)]*
- Removed map from enum + clear some dupe code *[commit by [@Rongmario](https://github.com/Rongmario) in [5ec5662](https://github.com/CleanroomMC/AssetMover/commit/5ec566245dbfa7c65db1a6b54d6fb37994e98de0)]*
- Works! Even in dev environment! (Minecraft assets only atm) *[commit by [@Rongmario](https://github.com/Rongmario) in [64ed03c](https://github.com/CleanroomMC/AssetMover/commit/64ed03c879bce62953d7e8a54bf01572d6335bc5)]*
- Fix typo *[commit by [@Rongmario](https://github.com/Rongmario) in [1ea633e](https://github.com/CleanroomMC/AssetMover/commit/1ea633ec9d45d35afd5d2dc63309eedd8126932c)]*
- Turn `MinecraftVersion` into an enum *[commit by [@Rongmario](https://github.com/Rongmario) in [b342eb8](https://github.com/CleanroomMC/AssetMover/commit/b342eb8f5bbbcb93c3d5fc7669a894d3ca666826)]*
- Very experimental *[commit by [@Rongmario](https://github.com/Rongmario) in [ad7872f](https://github.com/CleanroomMC/AssetMover/commit/ad7872f72de3af75405548a3275a9c072c87b871)]*
- Begin work *[commit by [@Rongmario](https://github.com/Rongmario) in [047b33a](https://github.com/CleanroomMC/AssetMover/commit/047b33a39e60dc2a2aac0ef7241d208435551e5f)]*
- Initial commit *[commit by [@Rongmario](https://github.com/Rongmario) in [919b227](https://github.com/CleanroomMC/AssetMover/commit/919b227247295855eed727881baf7da334aef3c2)]*


