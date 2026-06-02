# AGENTS.md

Project conventions for AI agents working on this codebase.

## Build & Run

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK (no minification)
```

## Architecture

MVVM, single-activity, Jetpack Compose UI.

```
com.subconverter/
  MainActivity.kt              # Single activity, sets theme + MainScreen
  SubConverterApp.kt           # Application class, creates AppContainer
  core/AppContainer.kt         # DI: database, repos, server singletons
  data/
    Entities.kt                # Room entities (SubscriptionSourceEntity, TemplateEntity, OutputProfileEntity)
    Daos.kt                    # Room DAOs
    AppDatabase.kt             # Room DB, migrations
    settings/ServerSettingsStore.kt  # DataStore for server settings
  domain/
    Models.kt                  # Domain models (FetchResult, RenderedSubscription, etc.)
    SubscriptionFetcher.kt     # HTTP fetch, captures profile-title/profile-web-page-url headers
    SubscriptionRepository.kt  # Source CRUD + refresh (auto-resolves name from headers)
    MihomoYamlService.kt       # YAML proxy extraction, regex filtering, template rendering
    OutputRepository.kt        # Template/profile CRUD + render output YAML
    ShareLinkParser.kt         # Parse share links to mihomo proxies
    DefaultTemplates.kt        # Built-in mihomo template
    RefreshWorker.kt           # WorkManager periodic refresh
    RemoteTextFetcher.kt       # Fetch remote template text
  server/LocalHttpServer.kt    # Raw socket HTTP server, serves YAML with standard subscription headers
  ui/
    MainScreen.kt              # All screens (Sources, Outputs, Templates, Server) + edit screens
    MainViewModel.kt           # Single ViewModel, all state
    theme/Theme.kt             # Material 3 theme (light/dark), typography, shapes
```

## Key Patterns

- **State**: Single `MainUiState` data class collected via `StateFlow` + `collectAsStateWithLifecycle`
- **DB migrations**: Room, manual `Migration` objects in `AppDatabase`. Current version: 4
- **Edit screens**: Full-screen `Scaffold` (not dialogs), toggled by `EditScreen` enum state
- **HTTP server**: Raw `ServerSocket` (not Ktor/OkHttp), runs on `Dispatchers.IO`
- **Subscription headers**: Fetcher captures `profile-title`, `content-disposition`, `profile-web-page-url`; Server outputs them in response
- **Regex filtering**: `MihomoYamlService.matches()` uses Kotlin `Regex`; UI shows live preview via `extractNodeNames()` parsing `cachedYaml`

## Database

Room, file: `sub_converter.db`

Tables:
- `subscription_sources` — id, name, url, website, userAgent, prefix, includeRegex, excludeRegex, cachedYaml, traffic fields, etc.
- `templates` — id, name, yamlBody, remoteUrl, isDefault
- `output_profiles` — id, name, sourceIds (comma-separated), templateId, prefix, includeRegex, excludeRegex, updateIntervalHours

When adding columns: create `Migration(N, N+1)` in `AppDatabase`, register in `AppContainer`.

## Style

- No comments unless requested
- Compose-only UI (no XML layouts except manifest/resources)
- Follow existing naming: `iOSXxx` for shared UI helpers, `SmallFormField` for edit screen fields
- Theme colors and typography defined in `Theme.kt`, access via `MaterialTheme.colorScheme` / `MaterialTheme.typography`
