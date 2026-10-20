# Ban Dream

**我的世界 Forge 1.20.1 封禁管理模组**

- 模组 ID：`bandream`
- 作者：`nins77`
- Forge：`1.20.1-47.4.20`（兼容 Forge 47.x）
- 指令：`/bbban`
- 权限：管理员（权限等级 3）

Ban Dream 用于服务器和整合包统一封禁物品、生物、结构、附魔、维度、Buff 与物品 NBT。
规则保存在独立的 JSON 文件中，修改后可热重载，不依赖世界存档，适合随整合包分发。

## 功能

- 物品封禁支持全局或指定维度。
- 物品封禁可选 JEI 显示或隐藏。
- 物品封禁不删除或修改 ItemStack、物品 ID 和 NBT；只有实际使用时才阻止使用、交互和物品自身事件，并在客户端显示 `999ban` 图标。
- 生物与结构支持全局或指定维度封禁。
- 方块支持全局或指定维度封禁；原方块不替换，客户端方块物品图标显示为 `bakaban`。
- 被封禁方块禁止玩家交互、破坏、内部 tick、随机 tick、邻居更新、红石和流体调度，并拒绝漏斗、投掷器和其他 Forge 能力管道输入/输出物品。
- 附魔与 Buff 支持全局或指定维度封禁；已存在的违规附魔/效果会被移除。
- 维度封禁会阻止传送、传送门、命令跨维度进入；已在其中的玩家会被送回可用维度。
- NBT 封禁支持原版 NBT 路径语法和 SNBT 值匹配，可指定物品或使用 `*` 匹配任意物品。
- `/bbban list <类型>` 查看封禁清单；物品和方块默认打开分页 GUI，其他类型输出到聊天框。
- `/bbban list <类型> book` 生成成书。
- 物品与方块清单不会刷屏，而是打开一个 5 行、可翻页、只读的箱子式 GUI。
- GUI 中的图标无法取出或放入；被封禁物品显示 999ban 图标，被封禁方块物品显示 bakaban 图标。
- 清单中的物品和方块图标可左键打开管理面板，设置禁止或解封。
- 封禁棒左键方块/生物打开左侧封禁选项，右键打开右下角全局解封确认框。
- 潜行左键原版木桶可批量封禁桶内物品；潜行右键木桶可一键全局解封桶内物品。
- 玩家白名单：白名单玩家不受物品使用限制、附魔/Buff 清理和维度封禁影响。
- 封禁指令可省略物品 ID，直接封禁主手物品；可省略范围，默认全局封禁。
- 规则按类型和 ID 建立索引，物品、生物、结构、附魔、Buff、方块查询不再逐条扫描整张封禁表。
- 没有对应类型规则时会快速跳过检查；不再周期性扫描玩家背包。
- JEI 为可选依赖；安装 JEI 时，标记为隐藏的物品会从 JEI 中移除。
- Curios 为可选联动；物品封禁不会移除饰品栏物品。
- 未安装本模组的客户端可以连接服务器并接受服务端封禁；物品/方块 GUI、JEI 同步和封禁棒需要客户端安装本模组。

## 构建

需要 JDK 17。项目使用 Gradle 8.8 Wrapper：

```powershell
$env:JAVA_HOME = '你的 JDK 17 目录'
.\gradlew.bat build
```

产物位于 `build/libs/ban_dream-1.3.0.jar`。

Curios 使用 `5.10.0+1.20.1` 的 API 作为 `compileOnly` 依赖，运行时不要求安装。

## 多平台开发环境

仓库包含相互独立的平台工程，避免不同版本的 Gradle 和映射工具冲突：

| 平台 | 目录 | Java | Gradle |
| --- | --- | --- | --- |
| Forge 1.12.2 | `platforms/forge-1.12.2` | 25 / 8 | 9.6.1 |
| Cleanroom 1.12.2 | `platforms/cleanroom-1.12.2` | 25 / 8 | 9.8.1 |
| NeoForge 1.20.1 | `platforms/neoforge-1.20.1` | 17 | 8.14.2 |
| NeoForge 1.21.1 | `platforms/neoforge-1.21.1` | 21 | 8.14.2 |

Forge 1.12.2 使用提供的 `ForgeDevEnv` 模板，MixinBooter 固定为 `10.7`。
Cleanroom 当前使用 `0.7.9-alpha`。NeoForge 工程依赖官方
`maven.neoforged.net`，构建时需要保证该域名可正常访问。

详见 [`platforms/README.md`](platforms/README.md)。

架构说明和构建记录放在工作区文档中，不进入 JAR：

- [`docs/architecture.md`](docs/architecture.md)
- [`docs/build-notes.md`](docs/build-notes.md)
- [`docs/performance.md`](docs/performance.md)

## 指令

所有指令均需要管理员权限。

### 封禁物品

```text
/bbban ban item [物品ID] [global|in <维度ID>] [jei show|hide] [trigger inventory|use]
```

示例：

```text
/bbban ban item
/bbban ban item global
/bbban ban item jei hide trigger inventory
/bbban ban item minecraft:tnt global jei hide
/bbban ban item minecraft:diamond_sword in minecraft:the_nether trigger use
```

不写 `物品ID` 时封禁主手物品。不写 `global` 或 `in <维度ID>` 时默认全局封禁。
`jei` 与 `trigger` 可以只写其中一个。若两个都写，顺序为 `jei ... trigger ...`。
`trigger` 参数保留用于兼容旧配置，但当前所有物品封禁都只禁止使用和交互，不会删除物品。

### 封禁生物、结构、附魔与 Buff

```text
/bbban ban entity <实体ID> [global|in <维度ID>]
/bbban ban structure <结构ID> [global|in <维度ID>]
/bbban ban enchantment <附魔ID> [global|in <维度ID>]
/bbban ban effect <效果ID> [global|in <维度ID>]
```

省略范围时默认全局。

示例：

```text
/bbban ban entity minecraft:creeper global
/bbban ban structure minecraft:village_plains in minecraft:overworld
/bbban ban enchantment minecraft:mending global
/bbban ban effect minecraft:blindness in minecraft:the_nether
```

结构封禁作用于新生成的结构。已经完整生成在世界中的结构不会被回滚。

### 封禁维度

```text
/bbban ban dimension <维度ID>
```

示例：

```text
/bbban ban dimension minecraft:the_end
```

### 封禁方块

```text
/bbban ban block <方块ID> [global|in <维度ID>]
```

方块封禁采用原位 Mixin 行为重定向，不替换 BlockState，也不移动原方块或方块实体。
方块世界外观保持不变；方块物品在客户端显示为 `bakaban`。方块被禁止交互、破坏、
内部 tick、随机 tick、邻居更新、红石和流体调度，并拒绝 Forge 能力物品输入/输出。
解封只删除规则，原方块和容器内容自然继续可用，不需要快照恢复。

```text
/bbban ban block minecraft:barrel in minecraft:overworld
/bbban ban block minecraft:bedrock global
```

### 封禁 NBT

```text
/bbban ban nbt <物品ID|*> <NBT路径> <SNBT值|*> [global|in <维度ID>] [trigger inventory|use]
```

NBT 路径使用原版 NBT Path 语法，例如 `CustomModelData`、`display.Name`、
`Items[0].tag.foo`。值使用 SNBT；`*` 表示只要路径存在就匹配。
省略范围时默认全局。

示例：

```text
/bbban ban nbt minecraft:stick CustomModelData 777 global
/bbban ban nbt * display.Name '"服务器禁用"' global
/bbban ban nbt minecraft:diamond_sword "Items[0].tag.banned" "*" global trigger use
```

### 封禁棒

```text
/bbban stick
```

管理员可通过该指令获得 `bandream:ban_stick`，它也会出现在创造模式“工具与实用物品”页。

- 左键方块：在左侧打开选项，可选择全局禁止或当前维度禁止。
- 左键非玩家生物：在左侧打开选项，可选择全局禁止或当前维度禁止。
- 潜行左键原版木桶：在左侧打开物品封禁选项，可选择全局/当前维度，以及直接禁止或禁止使用；一次写入桶内全部物品。
- 右键方块或非玩家生物：在右下角弹出全局解封确认框。
- 潜行右键原版木桶：立即全局解封桶内全部物品，不弹出确认框。

左键木桶使用点击瞬间的物品快照；确认选项前再修改桶内物品，不会改变已记录的目标。

### 解除封禁

```text
/bbban unban item <物品ID> [global|in <维度ID>]
/bbban unban entity <实体ID> [global|in <维度ID>]
/bbban unban structure <结构ID> [global|in <维度ID>]
/bbban unban enchantment <附魔ID> [global|in <维度ID>]
/bbban unban effect <效果ID> [global|in <维度ID>]
/bbban unban block <方块ID> [global|in <维度ID>]
/bbban unban dimension <维度ID>
/bbban unban nbt <物品ID|*> [path <NBT路径> [value <SNBT值>]]
```

不写 `global` / `in <维度ID>` 时会删除该 ID 的所有范围记录。

### 查看清单

```text
/bbban list item
/bbban list block
/bbban list entity [book]
/bbban list structure [book]
/bbban list enchantment [book]
/bbban list effect [book]
/bbban list dimension [book]
/bbban list nbt [book]
```

物品和方块清单默认打开分页 GUI，加 `book` 会生成成书。其他类型直接输出到聊天框，
加 `book` 会生成成书。方块 GUI 使用方块物品图标；没有物品形式的方块使用屏障图标，
鼠标悬停可查看封禁维度；物品图标还会显示处理方式和 JEI 状态。

左键物品或方块图标会打开管理面板：

- 方块可选择“禁止（全局）”或“解封”。
- 物品可选择“禁止（全局）”或“解封”。
- 解封会移除该 ID 的全局和维度规则。

### 重载配置

```text
/bbban reload
```

### 玩家白名单

```text
/bbban whitelist add <玩家名|UUID>
/bbban whitelist remove <玩家名|UUID>
/bbban whitelist list
```

白名单玩家不受以下规则影响：

- 封禁物品的使用和交互限制。
- 封禁附魔与 Buff 的移除。
- 被禁维度中的强制传送和跨维度拦截。

生物生成、结构生成等世界级规则仍会执行。在线或缓存中有玩家档案时会同时保存名称和 UUID；
无法查询档案时可以只写玩家名，之后按名称匹配。

## 配置文件

首次启动后配置文件位于：

```text
config/bandream/bans.json
```

整合包可直接预置这一文件。配置格式示例见
[`examples/config/bandream/bans.json`](examples/config/bandream/bans.json)。

顶层结构：

```json
{
  "version": 1,
  "playerWhitelist": [
    {
      "name": "Steve",
      "uuid": "8667ba71-b85a-4004-af54-457a9734eed7"
    }
  ],
  "bans": [
    {
      "type": "ITEM",
      "id": "minecraft:tnt",
      "scope": "GLOBAL",
      "jeiVisible": false,
      "trigger": "INVENTORY"
    }
  ]
}
```

`bans` 数组内的字段说明：

| 字段 | 说明 |
| --- | --- |
| `type` | `ITEM`、`ENTITY`、`STRUCTURE`、`ENCHANTMENT`、`DIMENSION`、`EFFECT`、`BLOCK`、`NBT` |
| `id` | 注册名；NBT 类型可使用 `*` 表示任意物品 |
| `scope` | `GLOBAL` 或 `DIMENSION` |
| `dimension` | `DIMENSION` 范围时填写维度 ID |
| `jeiVisible` | 仅物品使用；`false` 时从 JEI 隐藏 |
| `trigger` | `INVENTORY` 或 `USE` |
| `path` | 仅 NBT 使用；原版 NBT 路径 |
| `value` | 仅 NBT 使用；SNBT 值，`*` 表示仅检查路径存在 |

顶层 `playerWhitelist` 是玩家白名单数组，每项可填写 `name`、`uuid` 或两者。

JSON 中 `type`、`scope`、`trigger` 不区分大小写。修改文件后执行 `/bbban reload`，
或重启服务器。

## 行为说明

- 物品封禁不会删除或修改主背包、快捷栏、盔甲、副手、光标、容器和饰品栏中的 ItemStack。
- 被封禁物品显示 `999ban` 图标；被封禁方块物品显示 `bakaban` 图标，解封后恢复原图标。
- 方块保持原 BlockState、方块实体和世界外观；旧的 `bakaban` 方块只用于旧存档迁移。
- `bakaban` 物品最大堆叠数为 1。
- 方块解封只删除规则，不需要恢复方块或容器快照。
- 被封禁方块拒绝漏斗、投掷器和 Forge 能力管道的物品输入/输出；规则可全局生效，
  也可限定维度。
- `/bbban list block` 打开方块封禁分页页面；同一方块的多个维度会合并显示在同一个
  图标下，悬浮提示列出被封禁维度。
- `/bbban list item` 或 `/bbban list block` 中左键图标可管理封禁或解封，执行后会
  重新打开对应清单页面。
- 附魔封禁会从玩家物品中移除违规附魔；Buff 封禁会移除玩家与生物的违规效果。
- 生物封禁同时拦截新生成和从存档加载的实体，并在指令生效时清理已加载实体。
- 结构封禁在结构放置阶段取消，因此不会破坏周围地形。
- 维度封禁防止把玩家困在不可用维度中；若所有其他维度也被封禁，则不会强制传送。
- 配置中的未知模组 ID 会保留，便于在缺少可选模组的整合包环境中安全加载。
- 物品规则查询使用 `类型 -> ID -> 规则列表` 索引；没有相关规则时直接退出。
- 物品使用通过 `ItemStack` 的交互、使用、释放、完成使用、tick 和攻击入口拦截；不进行背包扫描。
- 方块 tick、随机 tick、邻居更新、红石和流体调度使用 Mixin 入口检查，不扫描世界方块。
- 附魔、Buff 和 Curios 不再依赖周期性全背包扫描。
- 木桶批量封禁通过一次配置写入完成，避免为每个物品重复保存 JSON。
- 封禁棒选项按玩家隔离，超过 60 秒未选择会自动失效。
- 所有 `/bbban` 子指令都继承根指令的权限等级 3 要求。

## Curios 兼容

[Curios](https://github.com/TheIllusiveC4/Curios) 是可选联动，不安装时不会影响
Ban Dream 启动或加载。安装 Curios 后：

- 物品封禁不会移除饰品栏中的物品。
- 饰品栏中的物品仍会参与已有的附魔清理。

该兼容只使用 Curios API 编译，运行时不要求 Curios 一定存在。

## 许可

MIT，作者 nins77。
