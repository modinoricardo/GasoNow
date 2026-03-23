# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK (minify disabled)
./gradlew installDebug           # Build and install on connected device/emulator
./gradlew clean build            # Clean rebuild
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests (requires connected device)
```

## Architecture

**MVVM** with `AndroidViewModel` + `LiveData` + Kotlin Coroutines.

### Data flow
`MainActivity` (search filters + location) → `ResultsActivity` (list) via `Intent` extras.

1. `GasoNowApplication.onCreate()` initializes `RetrofitClient` (sets up OkHttp disk cache).
2. `ResultsViewModel.loadStations(SearchFilters)` drives the results screen.
3. `GasStationRepository.searchStations()` fetches from MINETUR API → filters → sorts.
4. Results are posted as `ResultsUiState` sealed class (`Loading`, `Success`, `Error`, `Empty`).

### API & Caching
- **MINETUR REST API** (no auth required): `https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/`
- Two endpoints: `EstacionesTerrestres/` (all ~12k stations) and `EstacionesTerrestres/FiltroProvincia/{id}` (by province).
- **ProvinceHelper** reverse-geocodes the user's location → province ID to pick the cheaper endpoint.
- **OkHttp disk cache**: 30 MB, forces `Cache-Control: public, max-age=1800` via network interceptor.
- **In-memory cache** in repository: 30-minute TTL keyed by `provinceId`.

### Spanish locale specifics
- API returns coordinates and prices with **comma as decimal separator** — always replace `,` → `.` before parsing.
- `ScheduleHelper` parses Spanish schedule strings (e.g. `"L-V: 08:00-20:00; S: 09:00-14:00"`, `"L-D: 24H"`) to determine open/closed status. Returns `null` for unparseable formats.
- `ProvinceHelper` normalises accented province names (e.g. `"León"` → `"leon"`) before lookup in the 52-entry map.

### Intent extras (MainActivity → ResultsActivity)
`EXTRA_USER_LAT`, `EXTRA_USER_LON`, `EXTRA_FUEL_TYPE` (enum name), `EXTRA_MAX_DISTANCE` (Int km), `EXTRA_ONLY_OPEN` (Boolean), `EXTRA_ONLY_WITH_PRICE` (Boolean), `EXTRA_SORT_BY` (enum name).

## Key Configuration

- **`local.properties`** must define `GEMINI_API_KEY`, `GMAIL_SENDER`, `GMAIL_PASSWORD`, `GMAIL_RECEIVER` — these are injected as `BuildConfig` fields.
- Min SDK 24, Target/Compile SDK 36, Java 11 source/target compatibility.
- View Binding enabled; no Jetpack Compose.
- Cleartext HTTP traffic disabled (`android:usesCleartextTraffic="false"`).
