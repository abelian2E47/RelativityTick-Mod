# RelativityTick

RelativityTick is a Fabric mod that allows you to control the game tick rate of selected regions, with each region consisting of one or more Minecraft chunks. You can freeze regions, advance them tick by tick, run them at a custom rate, and give different regions independent time progression.

## Features

- Create and manage regions by chunk.
- Pause entity ticks, block entity ticks, random ticks, and scheduled ticks in selected regions.
- Step through region ticks for debugging redstone, farms, and entity behavior.
- Dash through region ticks: run as many as the tick budget allows right away, carrying the rest over to later ticks.
- Sprint: fill the region budget with dash on every server tick to advance a region as fast as the budget allows.
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

Executes the specified number of region ticks as fast as possible within the same server tick:

```text
/regionTick dash test 100
```

Dash is subject to the region tick time limit and the MSPT limit as well. When a dash fills the budget and gets cut short, the remaining steps are remembered and keep running at the same pace (filling the budget on every later server tick) until they are done; the command feedback reports how many steps were taken and how many are left.

### Sprinting

```text
/regionTick sprint [region_id] [ticks]
```

Fills the region budget / global MSPT budget with the dash primitive on every server tick:

- With `ticks`: sprints for that many server ticks, then automatically restores the state the region had before the sprint (running stays running, frozen goes back to frozen).
- Without `ticks`: sprints continuously until the same command is executed again to stop it.

```text
/regionTick sprint test 100
/regionTick sprint test
```

A sprinting region does not advance by its rate (the status shows the rate as "Unlimited"), its state shows as "Sprinting", and its chunk borders are drawn in orange-red. Starting a sprint takes the region over and cancels any pending steps; `step`/`dash` fail while sprinting, and `freeze`, `rate` and takeover (release) all interrupt it.

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

View the region you are currently in (same as `step` and `dash`: without a region id the command resolves the region at the executor's position):

```text
/regionTick status
```

View a specific region:

```text
/regionTick status <region_id>
```

View all regions:

```text
/regionTick status all
```

The status output includes:

- Region time (RegionTime, the start time plus the steps taken).
- Current state: Released, Frozen, Running, Stepping, or Sprinting.
- Number of chunks.
- Region TPS and target rate (the rate shows as "Unlimited" while sprinting). Region TPS is measured: it is the real steps per second, weighted by wall-clock elapsed time (not "steps per gt × 20"), so it stays accurate when the server lags or its tick rate changes; until one second of samples has accumulated it shows "measuring" instead of reporting the target rate as if it were measured.
- Tick processing time and its limit (the duration is averaged over wall-clock time as well, so a single fat gt does not skew it).
- Number of pending steps (pending dash steps are counted as well).

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
