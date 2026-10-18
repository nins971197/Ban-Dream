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
- 物品封禁可选两种处理方式：
  - `inventory`：进入玩家背包后立即删除。
  - `use`：物品可以留在背包中，但右键、交互、攻击或开始使用时删除并取消操作。
- 生物与结构支持全局或指定维度封禁。
- 附魔与 Buff 支持全局或指定维度封禁；已存在的违规附魔/效果会被移除。
- 维度封禁会阻止传送、传送门、命令跨维度进入；已在其中的玩家会被送回可用维度。
- NBT 封禁支持原版 NBT 路径语法和 SNBT 值匹配，可指定物品或使用 `*` 匹配任意物品。
- `/bbban list <类型>` 输出聊天框清单；`/bbban list <类型> book` 生成成书。
- 物品清单不会刷屏，而是打开一个 5 行、可翻页、只读的箱子式 GUI。
- GUI 中的物品无法取出或放入；鼠标悬停会显示封禁维度、处理方式和 JEI 状态。
- 新增封禁棒：左键生物可请求封禁其生物类型，潜行左键原版木桶可请求封禁桶内所有物品。
- 封禁棒目标会先在聊天框提示，执行 `/bbban confirm` 后才写入封禁表。
- 玩家白名单：白名单玩家不受物品删除、附魔/Buff 清理、使用限制和维度封禁影响。
- 封禁指令可省略物品 ID，直接封禁主手物品；可省略范围，默认全局封禁。
- 规则按类型和 ID 建立索引，物品、生物、结构、附魔、Buff 查询不再逐条扫描整张封禁表。
- JEI 为可选依赖；安装 JEI 时，标记为隐藏的物品会从 JEI 中移除。
- 未安装本模组的客户端可以连接服务器并接受服务端封禁；物品 GUI、JEI 同步和封禁棒需要客户端安装本模组。

## 构建

需要 JDK 17。项目使用 Gradle 8.8 Wrapper：

```powershell
$env:JAVA_HOME = '你的 JDK 17 目录'
.\gradlew.bat build
```

产物位于 `build/libs/ban_dream-1.2.0.jar`。

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

- 左键非玩家生物：取消这次攻击，在聊天框显示待封禁实体类型。
- 潜行左键原版木桶：取消挖掘，记录木桶当前所有不同物品类型，并在聊天框显示数量与预览。
- 点击聊天框中的确认按钮，或在 60 秒内输入 `/bbban confirm`。
- `/bbban cancel` 可取消待确认操作。

木桶确认使用点击瞬间的物品快照；确认前再修改桶内物品，不会改变已记录的目标。

### 解除封禁

```text
/bbban unban item <物品ID> [global|in <维度ID>]
/bbban unban entity <实体ID> [global|in <维度ID>]
/bbban unban structure <结构ID> [global|in <维度ID>]
/bbban unban enchantment <附魔ID> [global|in <维度ID>]
/bbban unban effect <效果ID> [global|in <维度ID>]
/bbban unban dimension <维度ID>
/bbban unban nbt <物品ID|*> [path <NBT路径> [value <SNBT值>]]
```

不写 `global` / `in <维度ID>` 时会删除该 ID 的所有范围记录。

### 查看清单

```text
/bbban list item
/bbban list entity [book]
/bbban list structure [book]
/bbban list enchantment [book]
/bbban list effect [book]
/bbban list dimension [book]
/bbban list nbt [book]
```

物品清单始终打开分页 GUI。其他类型直接输出到聊天框，加 `book` 会生成成书。

### 重载配置

```text
/bbban reload
```

### 确认或取消封禁棒操作

```text
/bbban confirm
/bbban cancel
```

### 玩家白名单

```text
/bbban whitelist add <玩家名|UUID>
/bbban whitelist remove <玩家名|UUID>
/bbban whitelist list
```

白名单玩家不受以下规则影响：

- 封禁物品的进入背包删除或使用/交互删除。
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
| `type` | `ITEM`、`ENTITY`、`STRUCTURE`、`ENCHANTMENT`、`DIMENSION`、`EFFECT`、`NBT` |
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

- 物品删除同时覆盖主背包、快捷栏、盔甲栏、副手和光标上的物品。
- 附魔封禁会从玩家物品中移除违规附魔；Buff 封禁会移除玩家与生物的违规效果。
- 生物封禁同时拦截新生成和从存档加载的实体，并在指令生效时清理已加载实体。
- 结构封禁在结构放置阶段取消，因此不会破坏周围地形。
- 维度封禁防止把玩家困在不可用维度中；若所有其他维度也被封禁，则不会强制传送。
- 配置中的未知模组 ID 会保留，便于在缺少可选模组的整合包环境中安全加载。
- 物品规则查询使用 `类型 -> ID -> 规则列表` 索引；没有相关规则时会跳过背包扫描和附魔检查。
- 物品删除检测逐 tick 执行，但只在存在背包删除类物品规则时扫描；附魔清理每 10 tick 检查一次。
- 木桶批量封禁通过一次配置写入完成，避免为每个物品重复保存 JSON。
- 连续点击同一目标不会重复弹出确认提示；待确认操作按玩家隔离并在 60 秒后过期。
- 所有 `/bbban` 子指令都继承根指令的权限等级 3 要求。

## 许可

MIT，作者 nins77。
