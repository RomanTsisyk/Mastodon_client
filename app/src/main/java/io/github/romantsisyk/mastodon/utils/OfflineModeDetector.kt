package io.github.romantsisyk.mastodon.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineModeDetector @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val coroutineScope: CoroutineScope
) {
    private val offlineMode = MutableStateFlow(false)

    init {
        Timber.d("Initializing OfflineModeDetector")
        observeNetworkStatus()
    }

    private fun observeNetworkStatus() {
        coroutineScope.launch {
            networkMonitor.networkStatus
                .map { it == NetworkStatus.Unavailable }
                .distinctUntilChanged()
                .collectLatest { isOffline ->
                    Timber.d("Setting offline mode: $isOffline")
                    offlineMode.value = isOffline
                }
        }
    }
}