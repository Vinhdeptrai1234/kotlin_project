@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.mhike.ui


import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mhike.data.dao.HikeDao
import com.example.mhike.data.model.Hike
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage

private val DIFFICULTIES = listOf("Easy","Moderate","Hard")

data class HikeForm(
    var name: String = "",
    var location: String = "",
    var date: LocalDate? = null,
    var parking: Boolean = false,
    var lengthKm: String = "",
    var difficulty: String = "",
    var description: String = "",
    var elevationGainM: String = "",
    var maxGroupSize: String = "",
    var coverImage: String? = null
)
data class FieldErrors(
    var name: String? = null,
    var location: String? = null,
    var date: String? = null,
    var lengthKm: String? = null,
    var difficulty: String? = null,
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddHikeFlow(
    dao: HikeDao,
    currentUserId: Long,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    var form by remember { mutableStateOf(HikeForm()) }
    var errs by remember { mutableStateOf(FieldErrors()) }
    var step by remember { mutableStateOf(1) } // 1=form, 2=confirm

    if (step == 1) {
        AddHikeForm(
            form = form,
            errs = errs,
            onChange = { form = it },
            onNext = {
                val (ok, e) = validate(form)
                if (ok) { errs = FieldErrors(); step = 2 } else errs = e
            },
            onCancel = onCancel
        )
    } else {
        ConfirmHike(form = form,
            onBack = { step = 1 },
            onSave = {
                val epoch = form.date!!.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val hike = Hike(
                    userId = currentUserId,
                    name = form.name.trim(),
                    location = form.location.trim(),
                    hikeDateEpoch = epoch,
                    parking = form.parking,
                    lengthKm = form.lengthKm.trim().toDouble(),
                    difficulty = form.difficulty,
                    description = form.description.ifBlank { null },
                    elevationGainM = form.elevationGainM.toIntOrNull(),
                    maxGroupSize = form.maxGroupSize.toIntOrNull(),
                    coverImage = form.coverImage
                )
                dao.insert(hike)
                onSaved()
            }
        )
    }
}


fun validate(f: HikeForm): Pair<Boolean, FieldErrors> {
    val e = FieldErrors()
    if (f.name.isBlank()) e.name = "Name is required"
    if (f.location.isBlank()) e.location = "Location is required"
    if (f.date == null) e.date = "Pick a date"
    val len = f.lengthKm.toDoubleOrNull()
    if (len == null || len <= 0.0) e.lengthKm = "Length must be a positive number"
    if (f.difficulty.isBlank()) e.difficulty = "Select difficulty"
    val ok = listOf(e.name,e.location,e.date,e.lengthKm,e.difficulty).all { it == null }
    return ok to e
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddHikeForm(
    form: HikeForm,
    errs: FieldErrors,
    onChange: (HikeForm) -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()

    val openDatePicker = {
        val d = form.date ?: LocalDate.now()
        DatePickerDialog(
            context,
            { _, y, m, day -> onChange(form.copy(date = LocalDate.of(y, m + 1, day))) },
            d.year, d.monthValue - 1, d.dayOfMonth
        ).show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header đẹp
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Terrain,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text("Plan a New Hike", style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Fill basic info then confirm",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .8f))
                }
            }
        }

        // Name
        OutlinedTextField(
            value = form.name,
            onValueChange = { onChange(form.copy(name = it)) },
            label = { Text("Name *") },
            leadingIcon = { Icon(Icons.Filled.Badge, null) },
            isError = errs.name != null,
            supportingText = { errs.name?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        // Location
        OutlinedTextField(
            value = form.location,
            onValueChange = { onChange(form.copy(location = it)) },
            label = { Text("Location *") },
            leadingIcon = { Icon(Icons.Filled.Place, null) },
            isError = errs.location != null,
            supportingText = { errs.location?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        // Date
        OutlinedTextField(
            value = form.date?.toString() ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Date (yyyy-MM-dd) *") },
            leadingIcon = { Icon(Icons.Filled.Event, null) },
            trailingIcon = {
                IconButton(onClick = openDatePicker) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick date")
                }
            },
            isError = errs.date != null,
            supportingText = { errs.date?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        // Parking
        ElevatedCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalParking, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Parking available *", style = MaterialTheme.typography.bodyLarge)
                }
                Switch(checked = form.parking, onCheckedChange = { onChange(form.copy(parking = it)) })
            }
        }

        // Length
        OutlinedTextField(
            value = form.lengthKm,
            onValueChange = { onChange(form.copy(lengthKm = it)) },
            label = { Text("Length (km) *") },
            leadingIcon = { Icon(Icons.Filled.Straighten, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = errs.lengthKm != null,
            supportingText = { errs.lengthKm?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        // Difficulty bằng chip (chọn 1)
        Text("Difficulty *", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DIFFICULTIES.forEach { d ->
                val selected = form.difficulty == d
                FilterChip(
                    selected = selected,
                    onClick = { onChange(form.copy(difficulty = d)) },
                    label = { Text(d) },
                    leadingIcon = {
                        when (d) {
                            "Easy" -> Icon(Icons.Filled.SentimentSatisfied, null)
                            "Moderate" -> Icon(Icons.Filled.DirectionsWalk, null)
                            else -> Icon(Icons.Filled.Whatshot, null)
                        }
                    }
                )
            }
        }
        if (errs.difficulty != null) Text(errs.difficulty!!, color = MaterialTheme.colorScheme.error)

        // Optional fields
        OutlinedTextField(
            value = form.description,
            onValueChange = { onChange(form.copy(description = it)) },
            label = { Text("Description (optional)") },
            leadingIcon = { Icon(Icons.Filled.Notes, null) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.elevationGainM,
            onValueChange = { onChange(form.copy(elevationGainM = it)) },
            label = { Text("Elevation gain (m, optional)") },
            leadingIcon = { Icon(Icons.Filled.TrendingUp, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.maxGroupSize,
            onValueChange = { onChange(form.copy(maxGroupSize = it)) },
            label = { Text("Max group size (optional)") },
            leadingIcon = { Icon(Icons.Filled.Groups, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        CoverImagePicker(
            value = form.coverImage,
            onChange = { onChange(form.copy(coverImage = it)) }
        )


        // Actions
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Close, null); Spacer(Modifier.width(6.dp)); Text("Cancel")
            }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CheckCircle, null); Spacer(Modifier.width(6.dp)); Text("Confirm")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun ConfirmHike(
    form: HikeForm,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.ArrowBack, null); Spacer(Modifier.width(6.dp)); Text("Cancel")
                }
                Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Save, null); Spacer(Modifier.width(6.dp)); Text("Save")
                }
            }
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Review & Save", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)

            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    // PREVIEW ẢNH BÌA (nếu có)
                    form.coverImage?.takeIf { it.isNotBlank() }?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    ListItem(headlineContent = { Text(form.name) }, supportingContent = { Text("Name") }, leadingContent = { Icon(Icons.Filled.Badge, null) })
                    ListItem(headlineContent = { Text(form.location) }, supportingContent = { Text("Location") }, leadingContent = { Icon(Icons.Filled.Place, null) })
                    ListItem(headlineContent = { Text("${form.date}") }, supportingContent = { Text("Date") }, leadingContent = { Icon(Icons.Filled.Event, null) })
                    ListItem(headlineContent = { Text(if (form.parking) "Yes" else "No") }, supportingContent = { Text("Parking") }, leadingContent = { Icon(Icons.Filled.LocalParking, null) })
                    ListItem(headlineContent = { Text("${form.lengthKm} km") }, supportingContent = { Text("Length") }, leadingContent = { Icon(Icons.Filled.Straighten, null) })
                    ListItem(headlineContent = { Text(form.difficulty) }, supportingContent = { Text("Difficulty") }, leadingContent = { Icon(Icons.Filled.FitnessCenter, null) })
                    if (form.description.isNotBlank()) ListItem(headlineContent = { Text(form.description) }, supportingContent = { Text("Description") }, leadingContent = { Icon(Icons.Filled.Notes, null) })
                    if (form.elevationGainM.isNotBlank()) ListItem(headlineContent = { Text("${form.elevationGainM} m") }, supportingContent = { Text("Elevation gain") }, leadingContent = { Icon(Icons.Filled.TrendingUp, null) })
                    if (form.maxGroupSize.isNotBlank()) ListItem(headlineContent = { Text(form.maxGroupSize) }, supportingContent = { Text("Max group size") }, leadingContent = { Icon(Icons.Filled.Groups, null) })
                }
            }

            // chừa khoảng cuối để tránh nội dung dính vào bottomBar
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun CoverImagePicker(
    value: String?,
    onChange: (String?) -> Unit
) {
    val ctx = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    ctx.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {}
                onChange(uri.toString())
            }
        }
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Cover image (optional)", style = MaterialTheme.typography.titleSmall)
        ElevatedCard {
            if (value.isNullOrBlank()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Image, null)
                    Spacer(Modifier.width(8.dp))
                }
            } else {
                AsyncImage( // coil-compose
                    model = value,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickImage.launch(arrayOf("image/*")) }) {
                Icon(Icons.Filled.Upload, null); Spacer(Modifier.width(6.dp)); Text("Choose")
            }
            if (!value.isNullOrBlank()) {
                TextButton(onClick = { onChange(null) }) {
                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp)); Text("Remove")
                }
            }
        }
    }
}

