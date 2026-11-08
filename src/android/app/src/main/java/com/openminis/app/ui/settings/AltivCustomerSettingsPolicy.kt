package com.openminis.app.ui.settings

/**
 * Commercial ALTIV settings stay focused on the customer's own models,
 * repositories and integrations. Upstream About/Feedback and third-party
 * contact destinations are not presented as commercial support channels.
 * The original AboutScreen and OSS notices remain in the source and build.
 */
internal object AltivCustomerSettingsPolicy {
    fun isAltiv(flavor: String): Boolean = flavor == "altiv"
    fun showAboutAndFeedback(flavor: String): Boolean = !isAltiv(flavor)
}
