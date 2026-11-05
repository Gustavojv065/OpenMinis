package com.openminis.app.commercial

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Non-sensitive, in-process status for the Settings account card.
 * Refresh/access tokens are encrypted in AltivCommercialSessionStore.
 */
internal object AltivCommercialAccountState {
    val subscription = MutableStateFlow<AltivSubscription?>(null)
    val signedIn = MutableStateFlow(false)
    val logoutRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun signedOut() {
        subscription.value = null
        signedIn.value = false
        logoutRequests.tryEmit(Unit)
    }
}
