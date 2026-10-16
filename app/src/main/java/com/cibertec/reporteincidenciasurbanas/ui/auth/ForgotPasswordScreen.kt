package com.cibertec.reporteincidenciasurbanas.ui.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.ui.components.LoadingDialog
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlue
import com.cibertec.reporteincidenciasurbanas.ui.theme.ReporteIncidenciasUrbanasTheme
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import com.cibertec.reporteincidenciasurbanas.viewmodel.AuthViewModel

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.resetPasswordState) {
        when (val state = uiState.resetPasswordState) {
            is Resource.Success -> {
                errorMessage = null
                showSuccessDialog = true
                viewModel.resetStates()
            }
            is Resource.Error -> {
                errorMessage = state.message
                snackbarHostState.showSnackbar(state.message)
            }
            else -> {}
        }
    }

    ForgotPasswordContent(
        isLoading = uiState.resetPasswordState is Resource.Loading,
        showSuccessDialog = showSuccessDialog,
        externalError = errorMessage,
        snackbarHostState = snackbarHostState,
        onSendResetEmail = { email ->
            errorMessage = null
            viewModel.sendPasswordReset(email)
        },
        onDismissSuccessDialog = {
            showSuccessDialog = false
            onNavigateBack()
        },
        onNavigateBack = onNavigateBack,
        onClearError = { errorMessage = null }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordContent(
    isLoading: Boolean = false,
    showSuccessDialog: Boolean = false,
    externalError: String? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSendResetEmail: (String) -> Unit,
    onDismissSuccessDialog: () -> Unit,
    onNavigateBack: () -> Unit,
    onClearError: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    val displayError = validationError ?: externalError

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.forgot_password_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            val invalidEmailMsg = stringResource(R.string.login_error_invalid_email)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.forgot_password_headline),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.forgot_password_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    validationError = null
                    onClearError()
                },
                label = { Text(stringResource(R.string.login_email_label)) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = displayError != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (displayError != null) {
                Text(
                    text = displayError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                        validationError = invalidEmailMsg
                        return@Button
                    }
                    onSendResetEmail(email.trim())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    text = stringResource(R.string.forgot_password_btn_send),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (isLoading) {
        LoadingDialog(message = stringResource(R.string.forgot_password_loading_msg))
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = onDismissSuccessDialog,
            title = { Text(stringResource(R.string.forgot_password_dialog_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.forgot_password_dialog_msg)) },
            confirmButton = {
                TextButton(onClick = onDismissSuccessDialog) {
                    Text(stringResource(R.string.btn_understand), color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ForgotPasswordScreenPreview() {
    ReporteIncidenciasUrbanasTheme {
        ForgotPasswordContent(
            onSendResetEmail = {},
            onDismissSuccessDialog = {},
            onNavigateBack = {}
        )
    }
}

