package com.openminis.app.commercial

/** Display hint only. Premium backend routes MUST check entitlement themselves. */
internal data class AltivSubscription(
    val active: Boolean,
    val plan: String,
    val status: String,
    val expiresAt: String?,
    val checkedAt: String?,
)

internal object AltivSubscriptionPolicy {
    fun allowsHostedServices(subscription: AltivSubscription?): Boolean =
        subscription?.active == true && subscription.status == "active"

    fun safeStatus(subscription: AltivSubscription?): String = when {
        subscription == null -> "not_found"
        subscription.status == "suspended" -> "suspended"
        subscription.status == "cancelled" -> "cancelled"
        subscription.status == "pending" -> "pending"
        allowsHostedServices(subscription) -> "active"
        else -> "expired"
    }
}
