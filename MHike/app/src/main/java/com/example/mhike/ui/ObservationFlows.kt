@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.mhike.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mhike.data.model.Observation
import com.example.mhike.data.repo.ObservationRepository
import java.time.*
import java.time.format.DateTimeFormatter

// ✨ KHÔNG còn OBS_TYPES (dropdown) vì DB không có cột "type"

data class ObservationForm(
    var note: String = "",                 // REQUIRED (NOT NULL)
    var comments: String = "",             // optional
    var observedAt: Long? = System.currentTimeMillis()   // default NOW
)

data class ObsErrors(
    var note: String? = null,
    var observedAt: String? = null
)

private fun validate(f: ObservationForm): Pair<Boolean, ObsErrors> {
    val e = ObsErrors()
    if (f.note.isBlank()) e.note = "Note is required"
    if (f.observedAt == null) e.observedAt = "Pick date & time"
    val ok = listOf(e.note, e.observedAt).all { it == null }
    return ok to e
}

/* ---------- ADD ---------- */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddObservationFlow(
    hikeId: Long,
    repo: ObservationRepository,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    var form by remember { mutableStateOf(ObservationForm()) }
    var errs by remember { mutableStateOf(ObsErrors()) }
    var step by remember { mutableStateOf(1) }

    if (step == 1) {
        ObservationFormUI(
            form = form,
            errs = errs,
            onChange = { form = it },
            onNext = {
                val (ok, e) = validate(form)
                if (ok) { errs = ObsErrors(); step = 2 } else errs = e
            },
            onCancel = onCancel
        )
    } else {
        ObservationConfirm(
            form = form,
            onBack = { step = 1 },
            onSave = {
                val obs = Observation(
                    hikeId = hikeId,
                    observedAt = requireNotNull(form.observedAt),
                    note = form.note.trim(),
                    comments = form.comments.ifBlank { null }
                )
                repo.add(obs)
                    .onSuccess { onSaved() }
                    .onFailure { /* có thể show Snackbar ở trên */ }
            }
        )
    }
}

/* ---------- EDIT ---------- */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EditObservationFlow(
    initial: Observation,
    repo: ObservationRepository,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val initForm = remember(initial) {
        ObservationForm(
            note = initial.note,
            comments = initial.comments ?: "",
            observedAt = initial.observedAt
        )
    }
    var form by remember { mutableStateOf(initForm) }
    var errs by remember { mutableStateOf(ObsErrors()) }
    var step by remember { mutableStateOf(1) }

    if (step == 1) {
        ObservationFormUI(
            form = form,
            errs = errs,
            onChange = { form = it },
            onNext = {
                val (ok, e) = validate(form)
                if (ok) { errs = ObsErrors(); step = 2 } else errs = e
            },
            onCancel = onCancel
        )
    } else {
        ObservationConfirm(
            form = form,
            onBack = { step = 1 },
            onSave = {
                val updated = initial.copy(
                    note = form.note.trim(),
                    comments = form.comments.ifBlank { null },
                    observedAt = requireNotNull(form.observedAt)
                )
                repo.update(updated)
                    .onSuccess { onSaved() }
                    .onFailure { /* show error nếu cần */ }
            }
        )
    }
}

/* ---------- SHARED UI ---------- */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ObservationFormUI(
    form: ObservationForm,
    errs: ObsErrors,
    onChange: (ObservationForm) -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val base = Instant.ofEpochMilli(form.observedAt ?: System.currentTimeMillis())
        .atZone(ZoneId.systemDefault()).toLocalDateTime()

    val openDateTime = {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                TimePickerDialog(
                    context,
                    { _, hh, mm ->
                        val dt = LocalDateTime.of(y, m + 1, d, hh, mm)
                        onChange(form.copy(observedAt = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                    },
                    base.hour, base.minute, true
                ).show()
            },
            base.year, base.monthValue - 1, base.dayOfMonth
        ).show()
    }

    val timeText = remember(form.observedAt) {
        form.observedAt?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                .toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        } ?: ""
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Add Observation", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)

        OutlinedTextField(
            value = form.note,
            onValueChange = { onChange(form.copy(note = it)) },
            label = { Text("Note *") },
            leadingIcon = { Icon(Icons.Filled.StickyNote2, null) },
            isError = errs.note != null,
            supportingText = { errs.note?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = form.comments,
            onValueChange = { onChange(form.copy(comments = it)) },
            label = { Text("Comments (optional)") },
            leadingIcon = { Icon(Icons.Filled.ChatBubble, null) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = timeText, onValueChange = {}, readOnly = true,
            label = { Text("Observed at *") },
            leadingIcon = { Icon(Icons.Filled.Schedule, null) },
            trailingIcon = {
                IconButton(onClick = openDateTime) { Icon(Icons.Filled.EditCalendar, contentDescription = "Pick") }
            },
            isError = errs.observedAt != null,
            supportingText = { errs.observedAt?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Close, null); Spacer(Modifier.width(6.dp)); Text("Cancel")
            }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CheckCircle, null); Spacer(Modifier.width(6.dp)); Text("Confirm")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ObservationConfirm(
    form: ObservationForm,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val dt = remember(form.observedAt) {
        form.observedAt?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                .toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        } ?: "(not set)"
    }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Review Observation", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        ElevatedCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ListItem(
                    headlineContent = { Text(dt) },
                    supportingContent = { Text("Observed at") },
                    leadingContent = { Icon(Icons.Filled.Schedule, null) }
                )
                ListItem(
                    headlineContent = { Text(form.note) },
                    supportingContent = { Text("Note") },
                    leadingContent = { Icon(Icons.Filled.StickyNote2, null) }
                )
                if (form.comments.isNotBlank())
                    ListItem(
                        headlineContent = { Text(form.comments) },
                        supportingContent = { Text("Comments") },
                        leadingContent = { Icon(Icons.Filled.ChatBubble, null) }
                    )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.ArrowBack, null); Spacer(Modifier.width(6.dp)); Text("Back")
            }
            Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Save, null); Spacer(Modifier.width(6.dp)); Text("Save")
            }
        }
    }
}

