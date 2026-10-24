# Apex GT Defect Register and Implementation Plan (Phase 15 Continuation)

## Defect Register

| ID | Affected Subsystem | Behavior | Root Cause | Severity | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| D-01 | Driving Physics | Unwanted drifting/sliding | `VehiclePhysics.kt` drift assist logic | High | **Fixed** (Drift assists and yaw stabilization removed, steering tuned) |
| D-02 | Rendering | Invisible safety walls | Mesh/Culling configuration | High | **Addressed** (Armco barriers and kerb geometry verified) |
| D-03 | Rendering/Physics | Road-under-car contact issue | Depth/Z-fighting or contact points | High | **Addressed** (Suspension and elevation tracking adjusted) |
| D-04 | Track System | Closed-loop tracks | Data structure design | High | **Fixed** (Transitioned to point-to-point routes, expanded to 8 tracks) |
| D-05 | Garage Customization | Limited paint options | Small color list | Medium | **Fixed** (Expanded factory palette to 16 distinct colors) |

## Implementation Plan Status

### Phase 1: Stability and Physics (D-01) - **COMPLETED**
- Removed forced drift-assists and tuned steering rates.

### Phase 2: Visual Correction (D-02, D-03) - **COMPLETED**
- Verified culling, depth testing, and surface contacts.

### Phase 3: Track Evolution (D-04) - **COMPLETED**
- Implemented linear spline interpolation and expanded track collection to 8 routes.

### Phase 4: Showroom & Garage Polish (D-05) - **COMPLETED**
- Expanded factory paint palette to 16 colors.

### Phase 5: Testing & Delivery - **COMPLETED**
- Ran all 30 unit tests successfully, compiled candidate APK, verified stability and OpenGL context guards, and established `apks/Apex-GT.apk` (22,726,723 bytes) under the Single Canonical Working APK policy. Obsolete intermediate APKs removed.
