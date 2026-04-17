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
    val disconnectErrorMessage: String? = null
)

private data class DisconnectActionState(
    val inProgress: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val stravaRepository: StravaRepository
) : ViewModel() {

    private val disconnectActionState = MutableStateFlow(DisconnectActionState())

    val uiState: StateFlow<AccountUiState> = combine(
        userRepository.observeAuthState(),
        stravaRepository.observeConnectionState().catch { emit(StravaConnectionState()) },
        disconnectActionState
    ) { authState, stravaState, disconnectAction ->
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
            disconnectErrorMessage = disconnectAction.errorMessage
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
                }
            }
        }
    }
}
