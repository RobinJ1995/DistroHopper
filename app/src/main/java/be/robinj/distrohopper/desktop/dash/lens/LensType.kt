package be.robinj.distrohopper.desktop.dash.lens

/**
 * How expensive a lens is to search, which drives how the search runner
 * schedules it (see home/SearchLoader):
 *
 * - [LOCAL]   in-memory, effectively instant (InstalledApps). Runs on every
 *             keystroke with no debounce so installed apps appear immediately.
 * - [IO]      touches local storage / content providers (LocalFiles). Debounced.
 * - [NETWORK] hits a remote API, possibly downloading per-result assets
 *             (DuckDuckGo, FDroid, GitHub, GooglePlayStore). Debounced. The
 *             default for any lens that doesn't say otherwise.
 */
enum class LensType {
    LOCAL,
    IO,
    NETWORK,
}
