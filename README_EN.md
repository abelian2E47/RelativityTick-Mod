# RelativityTick

RelativityTick is a Fabric mod that allows you to control the game tick rate of selected regions, with each region consisting of one or more Minecraft chunks. You can freeze regions, advance them tick by tick, run them at a custom rate, and give different regions independent time progression.

## Features

- Create and manage regions by chunk.
- Pause entity ticks, block entity ticks, random ticks, and scheduled ticks in selected regions.
- Step through region ticks for debugging redstone, farms, and entity behavior.
- Run regions at custom rates to speed up or slow down local gameplay.
- Display region status, region TPS, running rate, and tick processing time.
- Set a maximum tick processing time for each region.

## Getting Started

### 1. Select Chunks

In-game, assign a key to **Toggle region selection** in the Controls menu. The key is unbound by default. You can also use `/regionManager chunk select`.

1. Press the selection key or execute the command to enable chunk selection mode.
2. Left-click to select the chunk you are currently standing in.
3. Right-click to deselect the current chunk.
4. Press the selection key again to disable selection mode.

Selected chunks are shown with boundary lines. Selection mode only records chunk positions; it does not create a region automatically.

### 2. Create a Region

```text
/regionManager create <region_id>
```

For example:

```text
/regionManager create test
```

This creates a region named `test` from all currently selected chunks. Region IDs must be unique.

### 3. Take Over

```text
/regionTick takeover <region_id>
```

After a region is taken over, it is frozen by default and no longer advances with the normal world tick.

You can also run the command without an ID while standing inside a region:

```text
/regionTick takeover
```

Running the command again releases the region and returns it to normal world tick processing.

### 4. Run a Region

Setting a region rate takes over the region and puts it into a running state:

```text
/regionTick rate <region_id> <rate>
```

For example, to run a region at the default rate:

```text
/regionTick rate test 20
```

The rate is the number of ticks the region attempts to process per second. Common examples:

```text
/regionTick rate test 10
/regionTick rate test 20
/regionTick rate test 100
```

Higher rates advance gameplay in the region faster. The actual rate is still affected by the MSPT and TickDuration limits.

## Region Control

### Take Over/Release

```text
/regionTick takeover [region_id]
```

Toggles the region takeover state:

- An unreleased region is taken over and frozen.
- A taken-over region is released and returned to normal world ticking.

### Freeze/Resume

```text
/regionTick freeze [region_id]
```

Toggles the region between its frozen and running states:

- An unreleased region is taken over and frozen.
- A running region is frozen.
- A frozen region resumes running.

### Stepping

```text
/regionTick step [region_id] [ticks]
```

Adds the specified number of pending ticks to a frozen region. If the number of ticks is omitted, one tick is added:

```text
/regionTick step test
/regionTick step test 20
```

The steps are processed during the server tick loop, and entity states in the region are synchronized with clients.

### Dash Stepping

```text
/regionTick dash [region_id] [ticks]
```

Immediately executes the specified number of region ticks without waiting for subsequent server ticks:

```text
/regionTick dash test 100
```

### Tick Time Limit

```text
/regionManager parameter tickDurationLimit <region_id> <milliseconds>
```

For example:

```text
/regionManager parameter tickDurationLimit test 10
```

This limit prevents a single region from using too much time during a server tick. The mod automatically slows a region down when it approaches the limit.

### Status

View all regions:

```text
/regionTick status
```

View a specific region:

```text
/regionTick status <region_id>
```

The status output includes:

- Region time.
- Current state: Released, Frozen, Running, or Stepping.
- Number of chunks.
- Region TPS and target rate.
- Tick processing time and its limit.
- Number of pending steps.

## Chunk Management

### Add a Chunk

```text
/regionManager chunk add <region_id>
```

Adds the chunk you are currently standing in to the specified region.

If chunks are currently selected in the client, all selected chunks are added to the region instead.

### Add Selected Chunks

```text
/regionManager chunk select
```

Enters chunk selection mode. After selecting chunks, run:

```text
/regionManager chunk add <region_id>
```

### Remove a Chunk

```text
/regionManager chunk remove
```

Removes the chunk you are currently standing in from its region.

## Configuration

View the current configuration:

```text
/relativityTick
```

### Maximum MSPT

```text
/relativityTick maxMspt <milliseconds>
```

For example:

```text
/relativityTick maxMspt 45
```

The default value is `45 ms`. This value limits how much time region ticks may use during a server tick.

### Chunk Ticking

```text
/relativityTick chunkTick enabled <true|false>
```

Chunk ticking is enabled by default.
