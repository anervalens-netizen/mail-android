package net.thunderbird.android.auth

import net.thunderbird.core.common.oauth.OAuthConfiguration
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory

// Mail fork: upstream OAuth registrations belong to Thunderbird.
// POP3/IMAP/SMTP password and app-password authentication remain available.
// Add owner-registered OAuth clients here before enabling those providers.
class TbOAuthConfigurationFactory : OAuthConfigurationFactory {
    override fun createConfigurations(): Map<List<String>, OAuthConfiguration> = emptyMap()
}
