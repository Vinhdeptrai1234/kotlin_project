package com.example.mhike

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mhike.data.dao.HikeDao
import com.example.mhike.data.dao.ObservationDao
import com.example.mhike.data.db.DatabaseHelper
import com.example.mhike.data.model.Hike
import com.example.mhike.data.repo.HikeRepository
import com.example.mhike.data.repo.ObservationRepository
import com.example.mhike.ui.AddHikeFlow
import com.example.mhike.ui.EditHikeFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.mhike.ui.AddObservationFlow
import com.example.mhike.ui.EditObservationFlow
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import com.example.mhike.data.dao.UserDao
import com.example.mhike.session.SessionManager
import com.example.mhike.ui.AuthFlow
import com.example.mhike.ui.ProfileScreen
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.TrendingUp


private const val TAG = "MHikeApp"

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbh = DatabaseHelper(this)
        val db = dbh.writableDatabase
        Log.d(TAG, "Opened SQLite OK; pageSize=${db.pageSize}")

        val hikeRepo = HikeRepository(HikeDao(dbh))
        val obsRepo = ObservationRepository(ObservationDao(dbh))
        val userDao = UserDao(dbh)
        val session = SessionManager(this)

        setContent {
            com.example.mhike.ui.theme.VibrantTheme {
                val top = MaterialTheme.colorScheme.secondary
                val bottom = MaterialTheme.colorScheme.primary

                var currentUserId by remember { mutableStateOf(session.getUserId()) }

                val rootSnack = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                fun show(msg: String) = scope.launch { rootSnack.showSnackbar(msg) }

                Surface {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(top.copy(alpha = .20f), bottom.copy(alpha = .12f))
                                )
                            )
                    ) {
                        if (currentUserId == null) {
                            AuthFlow(
                                userDao = userDao,
                                session = session,
                                onLoggedIn = { currentUserId = session.getUserId() },
                                onMessage = ::show
                            )
                        } else {
                            HikeApp(
                                repo = hikeRepo,
                                obsRepo = obsRepo,
                                userDao = userDao,
                                session = session,
                                currentUserId = currentUserId!!,
                                onLoggedOut = { currentUserId = null }
                            )
                        }

                        SnackbarHost(
                            hostState = rootSnack,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}

/** --------- UI LAYER ---------- */

private sealed interface Screen {
    object List : Screen
    object Add : Screen
    object SearchBasic : Screen
    object SearchAdvanced : Screen

    data class Detail(val id: Long) : Screen
    data class Edit(val id: Long) : Screen
    // ⬇️ thêm 2 màn cho Observation
    data class AddObs(val hikeId: Long) : Screen
    data class EditObs(val hikeId: Long, val obsId: Long) : Screen
}

private sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val throwable: Throwable) : UiState<Nothing>
}


// đặt ở file MainActivity.kt, bên ngoài mọi class/func, ví dụ ngay dưới UiState
private enum class MainTab { Home, Create, Profile }


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun HikeApp(
    repo: HikeRepository,
    obsRepo: ObservationRepository,
    userDao: UserDao,
    session: SessionManager,
    currentUserId: Long,
    onLoggedOut: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var screen by remember { mutableStateOf<Screen>(Screen.List) }
    var hikes by remember { mutableStateOf<List<Hike>>(emptyList()) }
    var pendingDelete by remember { mutableStateOf<Hike?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }


    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun toast(msg: String) = scope.launch { snackbar.showSnackbar(msg) }

    var loading by remember { mutableStateOf(true) }
    fun refresh() {
        loading = true
        runCatching { repo.dao.listByUser(currentUserId) }
            .onSuccess { hikes = it; loading = false }
            .onFailure { toast("Load failed: ${it.message}"); loading = false }
    }
    LaunchedEffect(currentUserId) { refresh() }

    val title = when {
        tab == MainTab.Profile -> "Profile"
        tab == MainTab.Create -> "Create Hike"
        else -> when (screen) {
            is Screen.List -> "Hikes (${hikes.size})"
            is Screen.Add -> "Add Hike"
            is Screen.Detail -> "Hike Detail"
            is Screen.Edit -> "Edit Hike"
            is Screen.AddObs -> "Add Observation"
            is Screen.EditObs -> "Edit Observation"
            is Screen.SearchBasic -> "Search"
            is Screen.SearchAdvanced -> "Advanced Search"
        }
    }

    // Xác nhận xóa 1 hike
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Filled.Delete, null) },
            title = { Text("Delete hike?") },
            text  = { Text("Remove \"${target.name}\" and its observations?") },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = requireNotNull(target.id)
                    pendingDelete = null
                    repo.delete(id)
                        .onSuccess { toast("Deleted"); refresh() }
                        .onFailure { e -> toast("Delete failed: ${e.message}") }
                }) { Text("Delete") }
            }
        )
    }

// Xác nhận xóa tất cả
    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            icon = { Icon(Icons.Filled.DeleteForever, null) },
            title = { Text("Delete all hikes?") },
            text  = { Text("This will remove all hikes and observations for this user.") },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    repo.deleteAll()
                        .onSuccess { toast("All deleted"); refresh() }
                        .onFailure { e -> toast("Delete all failed: ${e.message}") }
                }) { Text("Delete all") }
            }
        )
    }


    Scaffold(
        topBar = {
            Box {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                )
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    title = { Text(title) },
                    navigationIcon = {
                        if (tab == MainTab.Home && screen !is Screen.List) {
                            IconButton(onClick = { screen = Screen.List }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (tab == MainTab.Home && screen is Screen.List && hikes.isNotEmpty()) {
                            IconButton(onClick = { confirmDeleteAll = true }) {
                                Icon(Icons.Filled.DeleteForever, contentDescription = "Delete All")
                            }
                        }
                        if (tab == MainTab.Home && screen is Screen.List) {
                            IconButton(onClick = { screen = Screen.Add }) {
                                Icon(Icons.Filled.Add, contentDescription = "Add")
                            }
                            IconButton(onClick = { screen = Screen.SearchBasic }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { screen = Screen.SearchAdvanced }) {
                                Icon(Icons.Filled.Tune, contentDescription = "Advanced")
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MainTab.Home,
                    onClick = { tab = MainTab.Home; screen = Screen.List },
                    icon = { Icon(Icons.Filled.Home, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = tab == MainTab.Create,
                    onClick = { tab = MainTab.Create },
                    icon = { Icon(Icons.Filled.AddCircle, null) },
                    label = { Text("Create") }
                )
                NavigationBarItem(
                    selected = tab == MainTab.Profile,
                    onClick = { tab = MainTab.Profile },
                    icon = { Icon(Icons.Filled.Person, null) },
                    label = { Text("Profile") }
                )
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            when (tab) {
                MainTab.Home -> {
                    when (val s = screen) {
                        Screen.List -> HikeList(
                            items = hikes,
                            onDetail = { screen = Screen.Detail(it.id!!) },
                            onEdit = { screen = Screen.Edit(it.id!!) },
                            onDelete = { pendingDelete = it }

                        )
                        Screen.Add -> AddHikeFlow(
                            dao = repo.dao,
                            currentUserId = currentUserId,
                            onSaved = {
                                toast("Saved")
                                screen = Screen.List
                                refresh()
                            },
                            onCancel = { screen = Screen.List }
                        )
                        Screen.SearchBasic -> SearchBasicScreen(
                            repo = repo,
                            onOpenDetail = { id -> screen = Screen.Detail(id) },
                            onBack = { screen = Screen.List }
                        )
                        Screen.SearchAdvanced -> SearchAdvancedScreen(
                            repo = repo,
                            onOpenDetail = { id -> screen = Screen.Detail(id) },
                            onBack = { screen = Screen.List }
                        )
                        is Screen.Edit -> {
                            val current = remember(s.id) { repo.get(s.id).getOrNull() }
                            if (current == null) {
                                Text("Not found")
                            } else {
                                EditHikeFlow(
                                    initial = current,
                                    dao = repo.dao,
                                    onSaved = {
                                        toast("Updated")
                                        screen = Screen.List
                                        refresh()
                                    },
                                    onCancel = { screen = Screen.List }
                                )
                            }
                        }
                        is Screen.Detail -> {
                            val current = remember(s.id) { repo.get(s.id).getOrNull() }
                            if (current == null) {
                                Text("Not found")
                            } else {
                                var observations by remember { mutableStateOf(emptyList<com.example.mhike.data.model.Observation>()) }
                                LaunchedEffect(s.id) {
                                    obsRepo.listByHike(s.id)
                                        .onSuccess { observations = it }
                                }
                                HikeDetail(
                                    hike = current,
                                    observations = observations,
                                    onAddObservation = { screen = Screen.AddObs(current.id!!) },
                                    onEditObservation = { obsId -> screen = Screen.EditObs(current.id!!, obsId) },
                                    onDeleteObservation = { obsId ->
                                        obsRepo.delete(obsId)
                                            .onSuccess {
                                                obsRepo.listByHike(current.id!!).onSuccess { observations = it }
                                            }
                                    },
                                    onEdit = { screen = Screen.Edit(current.id!!) },
                                    onDelete = { pendingDelete = current }
                                    ,
                                    onBack = { screen = Screen.List }
                                )
                            }
                        }
                        is Screen.AddObs -> AddObservationFlow(
                            hikeId = s.hikeId,
                            repo = obsRepo,
                            onSaved = { screen = Screen.Detail(s.hikeId) },
                            onCancel = { screen = Screen.Detail(s.hikeId) }
                        )
                        is Screen.EditObs -> {
                            val ob = remember(s.obsId) { obsRepo.get(s.obsId).getOrNull() }
                            if (ob == null) {
                                Text("Not found")
                            } else {
                                EditObservationFlow(
                                    initial = ob,
                                    repo = obsRepo,
                                    onSaved = { screen = Screen.Detail(s.hikeId) },
                                    onCancel = { screen = Screen.Detail(s.hikeId) }
                                )
                            }
                        }
                    }
                }
                MainTab.Create -> {
                    AddHikeFlow(
                        dao = repo.dao,
                        currentUserId = currentUserId,
                        onSaved = {
                            toast("Saved")
                            tab = MainTab.Home
                            screen = Screen.List
                            refresh()
                        },
                        onCancel = { tab = MainTab.Home }
                    )
                }
                MainTab.Profile -> {
                    var me by remember(currentUserId) { mutableStateOf(userDao.findById(currentUserId)!!) }
                    ProfileScreen(
                        user = me,
                        onLogout = {
                            session.clear()
                            onLoggedOut()
                        },
                        onChangeAvatar = { uri ->
                            userDao.updateAvatar(currentUserId, uri).onSuccess {
                                me = userDao.findById(currentUserId)!!
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun SearchBasicScreen(
    repo: HikeRepository,
    onOpenDetail: (Long) -> Unit,
    onBack: () -> Unit
) {
    var q by rememberSaveable { mutableStateOf("") }
    var simulateErr by rememberSaveable { mutableStateOf(false) }
    var state by remember { mutableStateOf<UiState<Hike?>>(UiState.Idle) }
    val scope = rememberCoroutineScope()

    fun doSearch() {
        state = UiState.Loading
        scope.launch {
            repo.searchBasicByName(q, simulateErr)
                .onSuccess { state = UiState.Success(it) }
                .onFailure { state = UiState.Error(it) }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Search (basic)", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = q, onValueChange = { q = it },
            label = { Text("Name contains…") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = simulateErr, onCheckedChange = { simulateErr = it })
            Text("Simulate DB error")
        }
        Button(onClick = ::doSearch, enabled = q.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("Search")
        }

        when (val s = state) {
            UiState.Idle -> {}
            UiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Error -> Text("DB error: ${s.throwable.message}", color = MaterialTheme.colorScheme.error)
            is UiState.Success -> {
                val h = s.data
                if (h == null) {
                    // N14_search_no_result.png
                    Text("No result", style = MaterialTheme.typography.titleMedium)
                } else {
                    // N12_search_basic.png
                    ElevatedCard(modifier = Modifier.clickable { onOpenDetail(requireNotNull(h.id)) }) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(h.name, style = MaterialTheme.typography.titleMedium)
                            Text("${h.location} • ${h.lengthKm} km • ${h.difficulty}")
                            Text("Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(h.hikeDateEpoch))}")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchAdvancedScreen(
    repo: HikeRepository,
    onOpenDetail: (Long) -> Unit,
    onBack: () -> Unit
) {
    var prefix by rememberSaveable { mutableStateOf("") }
    var fromDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var toDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var minLen by rememberSaveable { mutableStateOf("") }
    var maxLen by rememberSaveable { mutableStateOf("") }
    var state by remember { mutableStateOf<UiState<List<Hike>>>(UiState.Idle) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    fun pickDate(current: Long?, onPicked: (Long?) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = current ?: System.currentTimeMillis() }
        DatePickerDialog(
            ctx,
            { _, y, m, d ->
                // Set về 00:00:00 cho "from", và 23:59:59.999 cho "to" (xử lý bên dưới)
                val c = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                onPicked(c.timeInMillis)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun doSearch() {
        state = UiState.Loading
        val from = fromDate
        val to   = toDate?.let { it + (24L * 60 * 60 * 1000 - 1) } // inclusive đến cuối ngày
        val min  = minLen.toDoubleOrNull()
        val max  = maxLen.toDoubleOrNull()
        scope.launch {
            repo.searchAdvanced(prefix.ifBlank { null }, from, to, min, max)
                .onSuccess { state = UiState.Success(it) }
                .onFailure { state = UiState.Error(it) }
        }
    }

    val df = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Search (advanced)", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = prefix, onValueChange = { prefix = it },
            label = { Text("Name prefix") }, modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = fromDate?.let { df.format(Date(it)) } ?: "",
                onValueChange = {}, readOnly = true,
                label = { Text("From date") },
                trailingIcon = { TextButton(onClick = { pickDate(fromDate) { fromDate = it } }) { Text("Pick") } },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = toDate?.let { df.format(Date(it)) } ?: "",
                onValueChange = {}, readOnly = true,
                label = { Text("To date") },
                trailingIcon = { TextButton(onClick = { pickDate(toDate) { toDate = it } }) { Text("Pick") } },
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = minLen, onValueChange = { minLen = it },
                label = { Text("Min length (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = maxLen, onValueChange = { maxLen = it },
                label = { Text("Max length (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        Button(onClick = ::doSearch, modifier = Modifier.fillMaxWidth()) { Text("Apply filters") }

        when (val s = state) {
            UiState.Idle -> {}
            UiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Error -> Text("DB error: ${s.throwable.message}", color = MaterialTheme.colorScheme.error)
            is UiState.Success -> {
                val results = s.data
                if (results.isEmpty()) {
                    // N14_search_no_result.png (case advanced)
                    Text("No results with current filters.", style = MaterialTheme.typography.titleMedium)
                } else {
                    // N13_search_advanced.png
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(results) { h ->
                            ElevatedCard(modifier = Modifier.clickable { onOpenDetail(requireNotNull(h.id)) }) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(h.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${h.location} • ${h.lengthKm} km • ${h.difficulty}")
                                    Text("Date: ${df.format(Date(h.hikeDateEpoch))}")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}


@Composable
private fun HikeList(
    items: List<Hike>,
    onDetail: (Hike) -> Unit,
    onEdit: (Hike) -> Unit,
    onDelete: (Hike) -> Unit
) {

    if (items.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Landscape, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(8.dp))
            Text("No hikes yet", style = MaterialTheme.typography.titleMedium)
            Text("Tap the + button to create your first hike.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items) { h ->
            ElevatedCard(
                onClick = { onDetail(h) },
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // ẢNH BÌA TRONG LIST
                    h.coverImage?.takeIf { it.isNotBlank() }?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Terrain, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(h.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        DifficultyPill(h.difficulty)
                    }
                    Text("${h.location} • ${h.lengthKm} km", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { onDetail(h) }) { Icon(Icons.Filled.Info, null) }
                        IconButton(onClick = { onEdit(h) }) { Icon(Icons.Filled.Edit, null) }
                        IconButton(onClick = { onDelete(h) }) { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyPill(d: String) {
    val color = when (d) {
        "Easy" -> Color(0xFF10B981)
        "Moderate" -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    AssistChip(
        onClick = {},
        label = { Text(d) },
        leadingIcon = {
            when (d) {
                "Easy" -> Icon(Icons.Filled.SentimentSatisfied, null)
                "Moderate" -> Icon(Icons.Filled.DirectionsWalk, null)
                else -> Icon(Icons.Filled.Whatshot, null)
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = .12f),
            labelColor = color, leadingIconContentColor = color
        )
    )
}

@Composable
private fun HikeDetail(
    hike: Hike,
    observations: List<com.example.mhike.data.model.Observation>,
    onAddObservation: () -> Unit,
    onEditObservation: (Long) -> Unit,
    onDeleteObservation: (Long) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val dfDate = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
    val dfTime = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        // Thanh nút dùng lại cho cả dọc/ngang
        @Composable
        fun ActionBar() {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.ArrowBack, null); Spacer(Modifier.width(6.dp)); Text("Back")
                }
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(6.dp)); Text("Edit Hike")
                }
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Delete, null); Spacer(Modifier.width(6.dp)); Text("Delete")
                }
            }
        }

        if (!isWide) {
            // ======= DỌC (điện thoại) – 1 LazyColumn cuộn tất cả =======
            Scaffold(
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        icon = { Icon(Icons.Filled.Add, null) },
                        text = { Text("Add Observation") },
                        onClick = onAddObservation,
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                },
                bottomBar = { ActionBar() }
            ) { inner ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Landscape, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Hike Detail", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.weight(1f))
                            DifficultyPill(hike.difficulty)
                        }
                    }
                    // Card thông tin hike
                    // Card thông tin hike (có ảnh, spacing rộng hơn + Divider)
                    item {
                        ElevatedCard(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp) // nới spacing
                            ) {
                                // ẢNH BÌA (nếu có)
                                hike.coverImage?.takeIf { it.isNotBlank() }?.let { uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Divider()
                                }


                                InfoRow(Icons.Filled.Badge, "Name", hike.name)
                                InfoRow(Icons.Filled.Place, "Location", hike.location)
                                InfoRow(Icons.Filled.Event, "Date", dfDate.format(Date(hike.hikeDateEpoch)))
                                InfoRow(Icons.Filled.LocalParking, "Parking", if (hike.parking) "Yes" else "No")
                                InfoRow(Icons.Filled.Straighten, "Length", "${hike.lengthKm} km")
                                InfoRow(Icons.Filled.FitnessCenter, "Difficulty", hike.difficulty)

                                hike.description?.takeIf { it.isNotBlank() }?.let {
                                    Divider()
                                    InfoRow(Icons.Filled.Notes, "Description", it)
                                }
                                hike.elevationGainM?.let {
                                    InfoRow(Icons.AutoMirrored.Filled.TrendingUp, "Elevation gain", "$it m")
                                }
                                hike.maxGroupSize?.let {
                                    InfoRow(Icons.Filled.Groups, "Max group", "$it")
                                }
                            }
                        }
                    }

                    // Tiêu đề + danh sách observations
                    item { Text("Observations", style = MaterialTheme.typography.titleMedium) }
                    if (observations.isEmpty()) {
                        item { ElevatedCard { Text("No observations yet.", Modifier.padding(16.dp)) } }
                    } else {
                        items(observations, key = { it.id ?: it.hashCode().toLong() }) { ob ->
                            ElevatedCard {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    InfoRow(Icons.Filled.Schedule, "Time", dfTime.format(Date(ob.observedAt)))
                                    ob.note?.takeIf { it.isNotBlank() }?.let {
                                        InfoRow(Icons.Filled.StickyNote2, "Note", it)
                                    }
                                    InfoRow(Icons.Filled.ChatBubble, "Comments", ob.comments ?: "-")
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { onEditObservation(requireNotNull(ob.id)) }) {
                                            Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(6.dp)); Text("Edit")
                                        }
                                        TextButton(onClick = { onDeleteObservation(requireNotNull(ob.id)) }) {
                                            Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                                            Spacer(Modifier.width(6.dp)); Text("Delete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // chừa đáy để FAB/BottomBar không che item cuối
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        } else {
            // ======= NGANG / RỘNG (tablet) – 2 cột, mỗi cột là 1 LazyColumn có chiều cao hữu hạn =======
            Scaffold(
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        icon = { Icon(Icons.Filled.Add, null) },
                        text = { Text("Add Observation") },
                        onClick = onAddObservation,
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                },
                bottomBar = { ActionBar() }
            ) { inner ->
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Trái: thông tin hike
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Landscape, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Hike Detail", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.weight(1f))
                                DifficultyPill(hike.difficulty)
                            }
                        }
                        item {
                            ElevatedCard {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    InfoRow(Icons.Filled.Badge, "Name", hike.name)
                                    InfoRow(Icons.Filled.Place, "Location", hike.location)
                                    InfoRow(Icons.Filled.Event, "Date", dfDate.format(Date(hike.hikeDateEpoch)))
                                    InfoRow(Icons.Filled.LocalParking, "Parking", if (hike.parking) "Yes" else "No")
                                    InfoRow(Icons.Filled.Straighten, "Length", "${hike.lengthKm} km")
                                    InfoRow(Icons.Filled.FitnessCenter, "Difficulty", hike.difficulty)
                                    hike.description?.takeIf { it.isNotBlank() }?.let {
                                        InfoRow(Icons.Filled.Notes, "Description", it)
                                    }
                                    hike.elevationGainM?.let {
                                        InfoRow(Icons.Filled.TrendingUp, "Elevation gain", "$it m")
                                    }
                                    hike.maxGroupSize?.let {
                                        InfoRow(Icons.Filled.Groups, "Max group", "$it")
                                    }
                                }
                            }
                        }
                    }

                    // Phải: danh sách observations
                    LazyColumn(
                        modifier = Modifier
                            .weight(1.4f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Text("Observations", style = MaterialTheme.typography.titleMedium) }
                        if (observations.isEmpty()) {
                            item { ElevatedCard { Text("No observations yet.", Modifier.padding(16.dp)) } }
                        } else {
                            items(observations, key = { it.id ?: it.hashCode().toLong() }) { ob ->
                                ElevatedCard {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        InfoRow(Icons.Filled.Schedule, "Time", dfTime.format(Date(ob.observedAt)))
                                        ob.note?.takeIf { it.isNotBlank() }?.let {
                                            InfoRow(Icons.Filled.StickyNote2, "Note", it)
                                        }
                                        InfoRow(Icons.Filled.ChatBubble, "Comments", ob.comments ?: "-")
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = { onEditObservation(requireNotNull(ob.id)) }) {
                                                Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(6.dp)); Text("Edit")
                                            }
                                            TextButton(onClick = { onDeleteObservation(requireNotNull(ob.id)) }) {
                                                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                                                Spacer(Modifier.width(6.dp)); Text("Delete")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}


