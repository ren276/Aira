package com.aira.health.presentation.supplementary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.domain.model.AuthState
import com.aira.health.domain.model.StravaConnectionState
import com.aira.health.domain.repository.StravaRepository
import com.aira.health.domain.repository.UserRepository
import com.aira.health.domain.usecase.ExecuteLocalResetUseCase
import com.aira.health.domain.usecase.LocalResetResult
import com.aira.health.presentation.theme.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val name: String = "Guest",
    val email: String = "No email linked",
    val authLabel: String = "Guest mode",
    val stravaConnected: Boolean = false,
    val stravaReconnectRequired: Boolean = false,
    val stravaStatusLabel: String = "Not connected",
    val disconnectInProgress: Boolean = false,
    val disconnectErrorMessage: String? = null,
    val resetInProgress: Boolean = false,
    val resetBlocked: Boolean = false,
    val resetStatusMessage: String? = null,
    val overrideConfirmationRequired: Boolean = false
)

private data class DisconnectActionState(
    val inProgress: Boolean = false,
    val errorMessage: String? = null
)

private data class ResetActionState(
    val inProgress: Boolean = false,
    val blocked: Boolean = false,
    val statusMessage: String? = null,
    val overrideConfirmationRequired: Boolean = false
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val stravaRepository: StravaRepository,
    private val executeLocalResetUseCase: ExecuteLocalResetUseCase
) : ViewModel() {

    private val disconnectActionState = MutableStateFlow(DisconnectActionState())
    private val resetActionState = MutableStateFlow(ResetActionState())

    val uiState: StateFlow<AccountUiState> = combine(
        userRepository.observeAuthState(),
        stravaRepository.observeConnectionState().catch { emit(StravaConnectionState()) },
        disconnectActionState,
        resetActionState
    ) { authState, stravaState, disconnectAction, resetAction ->
        val base = when (authState) {
            is AuthState.Authenticated -> {
                val name = authState.session.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: authState.session.email
                        ?.substringBefore('@')
                        ?.takeIf { it.isNotBlank() }
                    ?: "Signed-in user"
                AccountUiState(
                    name = name,
                    email = authState.session.email ?: "No email linked",
                    authLabel = "Firebase account"
                )
            }

            AuthState.Guest -> AccountUiState()
            AuthState.Loading -> AccountUiState(name = "Loading profile", authLabel = "Checking auth")
            is AuthState.Error -> AccountUiState(name = "Profile error", authLabel = authState.message)
        }

        val stravaStatusLabel = when {
            disconnectAction.inProgress -> "Disconnecting..."
            stravaState.reconnectRequired -> "Reconnect required"
            stravaState.isConnected -> "Connected"
            else -> "Not connected"
        }

        base.copy(
            stravaConnected = stravaState.isConnected,
            stravaReconnectRequired = stravaState.reconnectRequired,
            stravaStatusLabel = stravaStatusLabel,
            disconnectInProgress = disconnectAction.inProgress,
            disconnectErrorMessage = disconnectAction.errorMessage,
            resetInProgress = resetAction.inProgress,
            resetBlocked = resetAction.blocked,
            resetStatusMessage = resetAction.statusMessage,
            overrideConfirmationRequired = resetAction.overrideConfirmationRequired
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AccountUiState(name = "Loading profile", authLabel = "Checking auth")
        )

    fun signOut() {
        viewModelScope.launch {
            userRepository.signOut()
        }
    }

    fun disconnectStrava() {
        if (disconnectActionState.value.inProgress) return
        disconnectActionState.update { it.copy(inProgress = true, errorMessage = null) }

        viewModelScope.launch {
            val result = stravaRepository.disconnect()
            disconnectActionState.update {
                if (result.isSuccess) {
                    it.copy(inProgress = false, errorMessage = null)
                } else {
                    it.copy(
                        inProgress = false,
                        errorMessage = result.exceptionOrNull()?.message
                            ?: "Unable to disconnect Strava"
                    )
                }
            }
        }
    }

    fun resetLocalData() {
        if (resetActionState.value.inProgress) return
        resetActionState.update {
            it.copy(
                inProgress = true,
                blocked = false,
                statusMessage = "Final continuity upload in progress...",
                overrideConfirmationRequired = false
            )
        }

        viewModelScope.launch {
            when (val result = executeLocalResetUseCase(allowIrreversibleOverride = false)) {
                LocalResetResult.Completed -> {
                    resetActionState.value = ResetActionState(
                        statusMessage = "Local data was reset after continuity upload."
                    )
                }

                is LocalResetResult.Blocked -> {
                    resetActionState.value = ResetActionState(
                        blocked = true,
                        statusMessage = "Reset blocked: ${result.reason}. Retry upload or explicitly confirm irreversible reset.",
                        overrideConfirmationRequired = false
                    )
                }
            }
        }
    }

    fun requestIrreversibleOverrideConfirmation() {
        val current = resetActionState.value
        if (!current.blocked || current.inProgress) return
        resetActionState.update {
            it.copy(
                statusMessage = "Irreversible override armed. Confirm to wipe local data without continuity upload.",
                overrideConfirmationRequired = true
            )
        }
    }

    fun confirmIrreversibleReset() {
        val current = resetActionState.value
        if (!current.overrideConfirmationRequired || current.inProgress) return

        resetActionState.update {
            it.copy(inProgress = true, statusMessage = "Executing irreversible local reset...")
        }

        viewModelScope.launch {
            when (val result = executeLocalResetUseCase(allowIrreversibleOverride = true)) {
                LocalResetResult.Completed -> {
                    resetActionState.value = ResetActionState(
                        statusMessage = "Irreversible local reset completed."
                    )
                }

                is LocalResetResult.Blocked -> {
                    resetActionState.value = ResetActionState(
                        blocked = true,
                        statusMessage = "Reset still blocked: ${result.reason}",
                        overrideConfirmationRequired = false
                    )
                }
            }
        }
    }
}

@Composable
fun AccountScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.dominant),
        contentPadding = PaddingValues(top = 48.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Theme.colors.surfaceContainerLow)
                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Theme.colors.accent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Text(
                        text = state.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Theme.colors.onSurfaceVariant
                    )

                    Text(
                        text = state.authLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Theme.colors.accent
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Strava",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )

                    Text(
                        text = state.stravaStatusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Theme.colors.onSurfaceVariant
                    )

                    state.disconnectErrorMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFB4AB)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = viewModel::disconnectStrava,
                        enabled = state.stravaConnected && !state.disconnectInProgress
                    ) {
                        Text(if (state.disconnectInProgress) "Disconnecting..." else "Disconnect Strava")
                    }

                    Button(onClick = viewModel::signOut) {
                        Text("Sign out")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Local Reset",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )

                    Text(
                        text = "A final continuity upload is required before wipe. If upload fails, reset stays blocked unless you explicitly confirm irreversible override.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Theme.colors.onSurfaceVariant
                    )

                    state.resetStatusMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.resetBlocked) Color(0xFFFFB4AB) else Theme.colors.accent
                        )
                    }

                    Button(
                        onClick = viewModel::resetLocalData,
                        enabled = !state.resetInProgress
                    ) {
                        Text(if (state.resetInProgress) "Processing..." else "Reset Local Data")
                    }

                    if (state.resetBlocked) {
                        Button(
                            onClick = viewModel::requestIrreversibleOverrideConfirmation,
                            enabled = !state.overrideConfirmationRequired && !state.resetInProgress
                        ) {
                            Text("Proceed Without Upload")
                        }
                    }

                    if (state.overrideConfirmationRequired) {
                        Button(
                            onClick = viewModel::confirmIrreversibleReset,
                            enabled = !state.resetInProgress
                        ) {
                            Text("Confirm Irreversible Wipe")
                        }
                    }
                }
            }
        }
    }
}
