# APEX GT — APK Build Artifacts

This directory contains the official compiled Android package (`.apk`) artifact for **Apex GT**, following the **Single Canonical Working APK** policy.

## Canonical Working APK

| File | Size | MD5 Checksum | Status | Description |
| :--- | :--- | :--- | :--- | :--- |
| **`Apex-GT.apk`** | 22,726,723 bytes (~22.7 MB) | `3eeeeebbf5a3a4a7c578eef1895f6fb5` | **Canonical Verified** | Official verified working APK with grip-focused driving physics, 9 point-to-point circuits, OpenGL ES thread safety guards, and 16 showroom paints. |

### Build Specification
- **Canonical Path:** `apks/Apex-GT.apk`
- **Application ID:** `com.aistudio.apexgt.rkqmvd`
- **Main Activity:** `com.example.MainActivity`
- **Minimum SDK:** 24 (Android 7.0 Nougat)
- **Target SDK:** 36 (Android 16)
- **Compile SDK:** 36
- **Signature Scheme:** APK Signature Scheme v2 (Verified)
- **Test Status:** 30/30 Unit tests passed, zero compilation errors

---

## APK Management Policy

1. **Single Canonical Path:** `apks/Apex-GT.apk` is the sole official working APK.
2. **Pre-Build Preservation:** The existing canonical APK is inspected before building and never modified or deleted while testing a candidate.
3. **Candidate Isolation:** Candidate builds are produced in standard build directories (`app/build/outputs/apk/debug/app-debug.apk`); no phase-numbered APKs are created in `apks/`.
4. **Candidate Verification:** Candidate builds must pass all unit tests, succeed in compilation, and be verified for size and signature integrity.
5. **Atomic Safe Replacement:** Only after full verification is the canonical APK replaced via temporary swap (`.apk.tmp` -> `.apk`) to prevent corruption.
6. **Failure Recovery:** If any candidate verification or build fails, the previous canonical APK is strictly preserved.
7. **Clean Directory:** No milestone or phase-numbered APKs accumulate in `apks/`.
