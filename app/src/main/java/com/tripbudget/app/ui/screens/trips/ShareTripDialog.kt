package com.tripbudget.app.ui.screens.trips

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.ui.components.PrimaryCoastalButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareTripDialog(
    trip: TripEntity,
    onPrepareCode: (TripEntity, (Result<String>) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var code by remember { mutableStateOf(trip.inviteCode.orEmpty()) }
    var loading by remember { mutableStateOf(code.isBlank()) }
    var error by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(trip.id) {
        if (code.isNotBlank()) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        onPrepareCode(trip) { result ->
            loading = false
            result.fold(
                onSuccess = { code = it },
                onFailure = { error = it.message ?: "Could not create code" },
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Share trip",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Friends sign in, tap Join trip, and enter this code.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            when {
                loading -> {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator()
                    Text("Preparing code…", style = MaterialTheme.typography.bodySmall)
                }
                error != null -> {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    Text(
                        code,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                        ),
                        textAlign = TextAlign.Center,
                        color = Color(0xFF0D8B8A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                    PrimaryCoastalButton(
                        text = if (copied) "Copied" else "Copy code",
                        onClick = {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Trip invite", code))
                            copied = true
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    )
                }
            }
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    }
}
