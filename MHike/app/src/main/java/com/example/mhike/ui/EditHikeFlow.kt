@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mhike.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import com.example.mhike.data.dao.HikeDao
import com.example.mhike.data.model.Hike
import java.time.Instant
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EditHikeFlow(
    initial: Hike,
    dao: HikeDao,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    // Map Hike -> HikeForm
    val initForm = remember(initial) {
        HikeForm(
            name = initial.name,
            location = initial.location,
            date = Instant.ofEpochMilli(initial.hikeDateEpoch)
                .atZone(ZoneId.systemDefault()).toLocalDate(),
            parking = initial.parking,
            lengthKm = initial.lengthKm.toString(),
            difficulty = initial.difficulty,
            description = initial.description ?: "",
            elevationGainM = initial.elevationGainM?.toString() ?: "",
            maxGroupSize = initial.maxGroupSize?.toString() ?: "",
            coverImage = initial.coverImage // NEW
        )
    }

    var form by remember { mutableStateOf(initForm) }
    var errs by remember { mutableStateOf(FieldErrors()) }
    var step by remember { mutableStateOf(1) } // 1=form, 2=confirm

    if (step == 1) {
        // Dùng AddHikeForm hiện có
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
        ConfirmHike(
            form = form,
            onBack = { step = 1 },
            onSave = {
                val epoch = form.date!!.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val updated = initial.copy(
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
                dao.update(updated)
                onSaved()
            }
        )
    }
}
