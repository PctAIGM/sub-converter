# AGENTS.md

Project conventions for AI agents working on this codebase.

## Build & Run

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK (no minification)
```

After modify , using `./gradlew assembleDebug` to build the `Debug` APK.

## Architecture

MVVM, single-activity, Jetpack Compose UI. Layers: `core/` (DI), `data/` (Room, DataStore), `domain/` (repos, services, fetchers), `server/` (raw socket HTTP), `ui/` (Compose screens + ViewModel).

## Key Patterns

- **State**: Single UiState data class collected via `StateFlow` + `collectAsStateWithLifecycle`
- **DB migrations**: Room, manual `Migration` objects. Current version: 4
- **Edit screens**: Full-screen `Scaffold` (not dialogs), toggled by enum state
- **HTTP server**: Raw `ServerSocket` (not Ktor/OkHttp), runs on `Dispatchers.IO`
- **Subscription headers**: Fetcher captures `profile-title`, `content-disposition`, `profile-web-page-url`; Server outputs them in response
- **Regex filtering**: Kotlin `Regex` matching; UI shows live preview by parsing cached YAML

## Database

Room, file: `sub_converter.db`

Tables:
- `subscription_sources` — id, name, url, website, userAgent, prefix, includeRegex, excludeRegex, cachedYaml, traffic fields, etc.
- `templates` — id, name, yamlBody, remoteUrl, isDefault
- `output_profiles` — id, name, sourceIds (comma-separated), templateId, prefix, includeRegex, excludeRegex, updateIntervalHours

When adding columns: create `Migration(N, N+1)`, register in DI container.

## Style

- No comments unless requested
- Compose-only UI (no XML layouts except manifest/resources)
- Follow existing naming: `iOSXxx` for shared UI helpers, `SmallFormField` for edit screen fields
- Theme colors and typography in `Theme.kt`, access via `MaterialTheme.colorScheme` / `MaterialTheme.typography`
