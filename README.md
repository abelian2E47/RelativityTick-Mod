# RelativityTick

RelativityTick 是一个 Fabric 模组，用于对指定区域（由一个或多个 Minecraft 区块组成）单独控制游戏 Tick。你可以冻结区域、逐 Tick 执行、以自定义速率运行区域，或让不同区域拥有相互独立的时间进度。

## 功能

- 以区块为单位创建和管理区域。
- 暂停指定区域的实体、方块实体、随机刻和计划刻处理。
- 逐步执行区域 Tick，方便调试红石、农场和实体行为。
- 以自定义速率运行区域，例如让某个区域加速或减速。
- 显示区域状态、区域 TPS、运行速率和 Tick 处理耗时。
- 设置区域优先级和单次 Tick 处理时间上限。

## 入门

### 1. 选择区块

进入游戏后，在控制设置中为 **Toggle region selection** 设置一个按键。该按键默认未绑定。或者使用/regionManager chunk select

1. 按下选择模式按键或者执行命令，开启区块选择模式。
2. 左键选择当前所在区块。
3. 右键取消选择当前所在区块。
4. 再次按下选择模式按键，关闭选择模式。

选择的区块会显示边界线。选择模式只记录区块位置，不会自动创建区域。

### 2. 创建区域

```text
/regionManager create <区域ID>
```

例如：

```text
/regionManager create test
```

该命令会使用当前客户端选择的所有区块创建名为 `test` 的区域。区域 ID 不能重复。

### 3. 接管区域

```text
/regionTick takeover <区域ID>
```

接管后，区域默认进入冻结状态，其 Tick 不再随世界正常推进。

也可以在当前所在区域执行：

```text
/regionTick takeover
```

再次执行该命令会释放区域，使其恢复由世界正常处理。

### 4. 运行区域

设置区域速率会接管区域并使其进入运行状态：

```text
/regionTick rate <区域ID> <速率>
```

例如，以默认速率运行：

```text
/regionTick rate test 20
```

速率是区域每秒尝试执行的 Tick 数。常用示例：

```text
/regionTick rate test 10
/regionTick rate test 20
/regionTick rate test 100
```

速率越高，区域内的游戏时间推进越快；实际速度仍会受到 MSPT 和 TickDuration 限制影响。

## 区域控制

### 接管/释放

```text
/regionTick takeover [区域ID]
```

切换区域的接管状态：

- 未接管区域：接管并冻结。
- 已接管区域：释放并恢复正常世界 Tick。

### 冻结/运行

```text
/regionTick freeze [区域ID]
```

切换区域的冻结/运行状态：

- 未接管区域：接管并冻结。
- 运行中的区域：冻结。
- 已冻结区域：恢复运行。

### 步进

```text
/regionTick step [区域ID] [ticks]
```

在冻结区域中加入待执行的 Tick 数。省略参数时执行 1 Tick：

```text
/regionTick step test
/regionTick step test 20
```

步进会在服务器 Tick 循环中执行，并通过客户端同步区域实体状态。

### Dash 步进

```text
/regionTick dash [区域ID] [ticks]
```

立即执行指定数量的区域 Tick，不需要等待后续服务器 Tick：

```text
/regionTick dash test 100
```

### Tick 时间上限

```text
/regionManager parameter tickDurationLimit <区域ID> <milliseconds>
```

例如：

```text
/regionManager parameter tickDurationLimit test 10
```

该限制用于避免单个区域占用过多服务器 Tick 时间。区域接近限制时，模组会自动降低其处理速度。

### 状态

查看所有区域：

```text
/regionTick status
```

查看指定区域：

```text
/regionTick status <区域ID>
```

状态信息包括：

- 区域时间。
- 当前状态：Released、Frozen、Running 或 Stepping。
- 区块数量。
- 优先级。
- 区域 TPS 和目标速率。
- Tick 处理耗时与限制。
- 等待执行的步进数量。

## 区块管理

### 添加区块

```text
/regionManager chunk add <区域ID>
```

将执行者当前所在区块添加到指定区域。

如果当前存在客户端区块选择，则会把所有选中的区块添加到该区域。

### 添加选中区块

```text
/regionManager chunk select
```

进入区块选择模式。完成选择后执行：

```text
/regionManager chunk add <区域ID>
```

### 移除区块

```text
/regionManager chunk remove
```

将执行者当前所在区块从所属区域移除。

## 配置

查看配置：

```text
/relativityTick
```

### 最大 MSPT

```text
/relativityTick maxMspt <milliseconds>
```

例如：

```text
/relativityTick maxMspt 45
```

默认值为 `45 ms`。该值用于限制区域 Tick 对服务器单 Tick 时间的占用。

### 区块 Tick

```text
/relativityTick chunkTick enabled <true|false>
```

默认启用。