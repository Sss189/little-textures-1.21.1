# Little Textures - Minecraft 1.21.1

当前版本：**0.1.0-alpha.1**（alpha）。

**[下载可安装 JAR](https://github.com/Sss189/little-textures-1.21.1/releases/tag/v0.1.0-alpha.1)** | **[全部版本](https://github.com/Sss189/little-textures-1.21.1/releases)** | **[简要使用说明 PDF](docs/quick-start.pdf)**

启用含连接纹理规则的材质包后自动生效，支持普通方块与 LittleTiles；测试时只启用一个连接纹理引擎。

将 Release 中的 JAR 放入客户端 `mods` 并重启游戏。兼容基准：Minecraft 1.21.1、NeoForge 21.1.256、Java 21、LT pre233、CreativeCore 2.13.50。

源码构建：设置 Java 21 的 `JAVA_HOME`，把 `libs/README.md` 中的外部依赖放入 `libs`，执行 `gradlew.bat build`（Windows）或 `./gradlew build`。首次构建需要联网下载 Gradle/Maven 依赖。

构建验证与游戏内验收范围见 `docs/VALIDATION.md`。旧记录中的 `LT_WORKSPACE` / `GAME_DIR` 是本地路径占位；测试用第三方材质包、光影及游戏模型不包含在仓库中。

---

# Little Textures

面向 Minecraft **1.21.1 / NeoForge** 的独立客户端纹理模组。项目位置：`LT_WORKSPACE\little-textures`。

自行读取 OptiFine 格式的纹理规则，不依赖 Continuity、Connector 或 Fabric API。
LittleTiles 和 CreativeCore 是可选集成对象。没有 LT 时，仅处理普通方块。

## 第一版范围

- `repeat`、`random`、47 格 `ctm`、`vertical`。
- `matchBlocks`（含方块状态筛选）、`matchTiles`、`faces`、`connect=block/tile/state`、`weights`、`symmetry`、`innerSeams`、现代 `biomes` 名称。
- 普通方块通过 NeoForge 的模型数据获取世界位置；当前 Sodium 的 NeoForge 模型通道也调用这些方法。
- LT 在 CreativeCore 裁切前选择纹理，以世界方块坐标计算 repeat，保留 LT 的几何、颜色、材质状态和渲染层。
- 将没有 `cullface` 的模型面按方向提供给 LT 裁切；不修改 LT 的遮挡判定。
- CTM 贴图加入方块图集，UV 跟随主贴图变换；资源别名供 Iris 查找对应的 `_n` / `_s` 贴图及元数据。
- 编辑时合并刷新邻近 26 个位置；通过 LT 的队列请求清除缓存，资源重载后请求重新生成已加载的小方块。

这是开发中的 alpha。编译、规则测试和资源检查不能替代游戏中的渲染验收。
独立 `_e` 发光贴图叠加渲染尚未实现；PBR 发光参数继续由 Iris / 光影解释。
彩色 GI、PTGI、体素反射和移动结构的光影效果需要分别验证。
Sodium 切片中的生物群系查询通过一个可选适配器获取，其他不提供生物群系的视图会跳过带 `biomes` 的规则。
不支持 overlay / ctm_compact 等额外方法。遇到未支持的属性会明确记录并跳过整条规则。
自定义 Fabric 模型的 `emitBlockQuads` 输出暂未接入；普通方块路径保留这些模型原有行为并跳过纹理处理。

## 使用你们的素材

当前 1.21.1 的 `INCEPTION texture V1.5.3 1.21.1 alpha4.zip` 静态检查结果：

- 115 条规则：102 repeat、10 random、3 ctm，全部通过解析。
- 1,337 个不同贴图引用；其中 1,253 个有法线贴图，1,236 个有反光贴图。
- 兼容三条规则使用的小写 `matchtiles`，属性重复且冲突时拒绝应用。
- 原材质包缺少 `minecraft:textures/blocks/pure_color/nothing.png`。模组为这个明确的透明占位路径提供内置透明纹理；不对其他缺失贴图自动替换。
- 原材质包和游戏配置没有改动。

1.12 的 `INCEPTION Genesis version` / `Genesis` 存档用于对照旧行为。
它使用 85 repeat、6 random、19 ctm、1 vertical。
旧数字 ID、metadata、旧模型和生物群系名字需要迁移到 1.21.1；不能直接将整个旧整合包当作新版本资源使用。

## 构建

开发模板来自 [NeoForge 官方 MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle)。
固定 ModDevGradle 2.0.148、NeoForge 21.1.256、Java 21、Gradle 9.2.1。

`libs` 放入用于编译的精确版本：

- `CreativeCore_NEOFORGE_v2.13.50_mc1.21.1.jar`
- `LittleTiles_BETA_v1.6.0-pre233_mc1.21.1.jar`

它们作为外部开发依赖使用，不会打进本模组 jar。

```powershell
.\tools\build.ps1 -Audit
```

`tools/build.ps1` 使用父目录的共享 `.gradle-user` 缓存，优先使用本机已有的 Java 21。
其他机器需设置 Java 21 的 JAVA_HOME，并调整或移除 gradle.properties 中的本机工具链路径。
产物位于 `build/libs/lttextures-neoforge-1.21.1-0.1.0-alpha.1.jar`。

```powershell
.\gradlew.bat runClient
```

运行目录为独立的 `run`，不使用原来的游戏存档。
验证时仅启用一种连接纹理引擎；此模组与 Continuity / NeoContinuity 的组合尚未验证。

## 验收

详细场景见 [docs/ACCEPTANCE.md](docs/ACCEPTANCE.md)。
已执行的检查及其范围见 [docs/VALIDATION.md](docs/VALIDATION.md)。
先比较关闭光影时的普通方块与 LT，再使用实际的 `iterationRP Alpha 0.4.2` 验证 PBR 和阴影。
