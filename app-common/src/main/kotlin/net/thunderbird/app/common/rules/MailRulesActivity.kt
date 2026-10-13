package net.thunderbird.app.common.rules

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fsck.k9.ui.base.BaseActivity
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.app.common.R
import net.thunderbird.components.ui.bolt.atom.button.ButtonFilled
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleLarge
import net.thunderbird.components.ui.bolt.molecule.input.SelectInput
import net.thunderbird.components.ui.bolt.molecule.input.TextInput
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MailRulesActivity : BaseActivity() {
    private val rulesViewModel: MailRulesViewModel by viewModel()
    private val themeProvider: FeatureThemeProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            themeProvider.WithTheme {
                val state by rulesViewModel.state.collectAsStateWithLifecycle()
                MailRulesScreen(state, rulesViewModel::event, ::finish)
            }
        }
    }
}

@Composable
private fun MailRulesScreen(state: MailRulesState, onEvent: (MailRulesEvent) -> Unit, onBack: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(BoltTheme.spacings.double)
                .semantics { testTagsAsResourceId = true },
            verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
        ) {
            ButtonText(text = stringResource(R.string.mail_rules_back), onClick = onBack)
            TextTitleLarge(text = stringResource(R.string.mail_rules_title))
            TextBodyMedium(text = stringResource(R.string.mail_rules_description))
            val selected = state.snapshot.accounts.firstOrNull { it.uuid == state.snapshot.selectedUuid }
            if (selected != null) {
                SelectInput(
                    options = state.snapshot.accounts.toImmutableList(),
                    selectedOption = selected,
                    onOptionChange = { onEvent(MailRulesEvent.SelectAccount(it.uuid)) },
                    optionToStringTransformation = { it.label },
                    label = stringResource(R.string.mail_rules_account),
                    contentPadding = PaddingValues(0.dp),
                )
                MailRuleForm(state, onEvent)
            } else if (!state.busy) {
                TextBodyMedium(text = stringResource(R.string.mail_rules_no_accounts))
            }
            MailRuleStatus(state)
            if (state.snapshot.rules.isNotEmpty()) {
                TextTitleLarge(text = stringResource(R.string.mail_rules_active))
            }
            for (rule in state.snapshot.rules) {
                TextBodyMedium(
                    text = stringResource(
                        R.string.mail_rules_rule,
                        rule.sender,
                        rule.folderName ?: stringResource(R.string.mail_rules_missing_folder),
                    ),
                )
                ButtonText(
                    text = stringResource(R.string.mail_rules_remove),
                    onClick = { onEvent(MailRulesEvent.Remove(rule.sender)) },
                    enabled = !state.busy,
                )
            }
            TextBodyMedium(text = stringResource(R.string.mail_rules_footer))
        }
    }
}

@Composable
private fun MailRuleForm(state: MailRulesState, onEvent: (MailRulesEvent) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    TextInput(
        text = state.sender,
        onTextChange = { onEvent(MailRulesEvent.SenderChanged(it)) },
        label = stringResource(R.string.mail_rules_sender),
        isEnabled = !state.busy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
        contentPadding = PaddingValues(0.dp),
    )
    TextInput(
        text = state.folder,
        onTextChange = { onEvent(MailRulesEvent.FolderChanged(it)) },
        label = stringResource(R.string.mail_rules_folder),
        isEnabled = !state.busy,
        contentPadding = PaddingValues(0.dp),
    )
    ButtonFilled(
        text = stringResource(R.string.mail_rules_save),
        onClick = {
            keyboard?.hide()
            onEvent(MailRulesEvent.Save)
        },
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MailRuleStatus(state: MailRulesState) {
    if (state.busy) TextBodyMedium(text = stringResource(R.string.mail_rules_busy))
    state.error?.let {
        val messageId = when (it) {
            MailRulesError.INPUT -> R.string.mail_rules_invalid
            MailRulesError.FOLDER_CONFLICT -> R.string.mail_rules_folder_conflict
            MailRulesError.OPERATION -> R.string.mail_rules_error
        }
        TextBodyMedium(text = stringResource(messageId), color = BoltTheme.colors.error)
    }
    state.result?.let {
        TextBodyMedium(text = stringResource(R.string.mail_rules_result, it.moved, it.pending))
    }
    if (state.removed) TextBodyMedium(text = stringResource(R.string.mail_rules_removed))
}
