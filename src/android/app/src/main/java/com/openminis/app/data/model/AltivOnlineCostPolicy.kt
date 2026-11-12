package com.openminis.app.data.model

/**
 * Conservative, explicit zero-price routing.
 *
 * A *provider free tier* is not the same as a model with zero billing cost.
 * Only OpenRouter model IDs ending in ":free" are recognized for this
 * strict mode. This is an eligibility hint, not a guarantee of quota or
 * service availability. No paid/unknown model can be selected as fallback.
 */
internal object AltivOnlineCostPolicy {
    fun isExplicitlyFree(entry: ModelEntry, instance: ProviderInstance?): Boolean =
        instance != null && instance.isEnabled &&
            instance.providerType == ProviderType.openRouter &&
            entry.model.id.trim().endsWith(":free", ignoreCase = true) &&
            !entry.isHidden && !entry.isUnavailableFromProvider

    fun eligibleEntries(
        entries: List<ModelEntry>,
        instances: List<ProviderInstance>,
    ): List<ModelEntry> {
        val byId = instances.associateBy { it.id }
        return entries.filter { isExplicitlyFree(it, byId[it.providerInstanceId]) }
    }

    fun isGroupFreeOnly(
        group: ModelGroup,
        entries: List<ModelEntry>,
        instances: List<ProviderInstance>,
    ): Boolean {
        if (group.memberEntryIds.isEmpty()) return false
        val eligible = eligibleEntries(entries, instances).map { it.id }.toSet()
        // A partial free group is deliberately not free-only: a dynamic
        // fallback could otherwise route to an unapproved paid member.
        return group.memberEntryIds.all { it in eligible }
    }

    fun modelLabel(entry: ModelEntry, instance: ProviderInstance?): String =
        if (isExplicitlyFree(entry, instance)) "Grátis identificado (:free)"
        else "Preço/cota não verificados — consulte o provedor"
}
