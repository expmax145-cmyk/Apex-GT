# Implementation Plan: Apex GT Phase 18 - Replay System & Cinematic Cameras

## 1. Analysis of Current Architecture
- **Race Loop**: Driven by `RacingSurfaceView` (renderer) and `GameViewModel` (physics update).
- **Physics**: `VehiclePhysics` handles movement, inputs, and transforms.
- **Rendering**: `RacingRenderer` takes `VehiclePhysics` and `OpponentAIController` lists to render the 3D scene.
- **State**: `GameViewModel` holds `playerVehicle`, `aiOpponents`, and race timing.

## 2. Replay System Design
- **Recording**:
    - Create `ReplayManager` class.
    - Sample player and AI transforms (position, orientation, steer, gear, throttle/brake) at a fixed rate (e.g., 20Hz).
    - Store in a memory-bounded `RingBuffer` to prevent unbounded memory growth.
- **Playback**:
    - `ReplayManager` will provide interpolated state at requested replay-time.
    - `RacingRenderer` will be decoupled from live `VehiclePhysics` during replay to render snapshots from the replay buffer.
- **Camera System**:
    - Implement `ReplayCameraDirector` to handle cinematic switching and smooth interpolation between viewpoints.
- **UI**:
    - Add `ReplayControls` UI (play, pause, timeline, camera switch) in `RacingHud`.

## 3. Implementation Steps
1.  Define `ReplaySnapshot` data class.
2.  Implement `ReplayManager` with fixed-rate sampling.
3.  Extend `GameViewModel` to handle recording lifecycle (Start, Stop, Save).
4.  Implement replay playback mode in `GameViewModel` and `RacingRenderer`.
5.  Develop `ReplayCameraDirector` for cinematic views.
6.  Build Replay UI in `RacingHud` and `RaceSummaryScreen`.
7.  Add Photo Mode trigger.
8.  Implement tests (as per Phase 18 requirements).

## 4. Verification
- Validate recording/playback using Robolectric unit tests for snapshots.
- Verify memory stability.
- Assemble candidate APK and verify functional replay playback.
