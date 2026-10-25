# 0.1.0-alpha.1 验证记录

## 已完成

| 检查 | 结果 | 证明的范围 |
|---|---|---|
| Java 21 / NeoForge 21.1.256 构建 | 通过 | 源码编译、资源打包和 jar 生成 |
| JUnit 规则测试 | 9 项通过 | CTM 的 256 个邻居组合、47 个索引、对角限制、repeat 的六个朝向和负坐标、random、vertical、解析及路径处理 |
| 实际 1.21.1 INCEPTION 包检查 | 115 条通过 | 102 repeat、10 random、3 ctm；1,337 个不同贴图引用；一个明确的透明占位由内置资源提供 |
| NeoForge + LT / CreativeCore 客户端 | 通过 | 加载并激活 115 条规则；普通玻璃按邻居选择 0、3、26；LT 在裁切前换图并保留裁切范围 |
| LT + Sodium 0.8.13-beta.2 + Iris 1.8.14-beta.1 | 通过 | 真实客户端中执行上述断言；Sodium 的 NeoForgeModelAccess 返回正确 CTM；生物群系适配器应用到 LevelSlice |
| Iris PBR 加载 | 通过 | 光影配置选中 iterationRP Alpha 0.4.2；使用 Iris 自身 AtlasPBRLoader 实际解码一个 CTM 材质的 NORMAL 和 SPECULAR 贴图 |
| 无 LT / CreativeCore，仅 NeoForge + Sodium / Iris | 通过 | 模组可独立加载，普通方块 CTM、Sodium 模型接口及 Iris PBR 加载仍通过 |

顶点检查确认换图仅改变 UV；位置、法线、光照数据、朝向、颜色索引和 AO / shade 标志保留。
测试的 LT 盒子范围是 0.25 到 0.75，裁切输出保留正确的 CTM 26 号贴图。

## 尚未完成

- 没有进入真实测试世界完成各方向、混合材质、连续编辑、透明排序和斜面的一整套视觉验收。
- 没有逐一验证材质包所有贴图的视觉外观。规则解析通过不等于每条规则都匹配到了期望的现代模型。
- Iris 的实际 PBR 图片解码通过，不能代替光影中法线、反射、阴影、彩色 GI 和 PTGI 的场景验收。
- `_e` 独立发光叠加还未实现。
- 自定义 Fabric 模型输出暂时保留原行为，未提供本引擎的纹理处理。
- 当前材质包自身有 red_sand 方块状态 JSON 语法 / 旧属性及 magma 动画帧警告。这些不是模组生成的文件，未修改原包。
- LT 对可选 Sable 功能的缺类日志、原模组开发环境 refmap 提示不代表这些功能已验证。

## 复现

开发运行目录为 `LT_WORKSPACE\little-textures\run`。复制的材质包、光影和 Sodium / Iris 只存在于这个测试目录。
原整合包的配置、mod 列表和存档未修改。

```powershell
.\tools\build.ps1 -Audit
.\tools\smoke-test.ps1
```

独立检查可使用 `gradlew runClient -Psmoke -PwithoutLT`。
smoke 模式只由显式 JVM 开关启用，在标题界面执行断言并退出，不打开存档。
Gradle 的客户端进程退出码可能为零，即使断言失败；`smoke-test.ps1` 会额外检查 PASS / FAIL 标记。

原始运行记录保存在 `build/smoke-vanilla-console.log`、`build/smoke-iris-runtime.log`、
`build/smoke-without-lt-runtime.log` 和 `run/logs/latest.log`。
