package com.example.mhike.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mhike.data.dao.UserDao
import com.example.mhike.data.model.User
import com.example.mhike.session.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers


@Composable
fun LoginScreen(userDao: UserDao, onSuccess: (Long) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sign in", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = email.isNotBlank(),
            onClick = {
                val u = userDao.findByEmail(email.trim())
                if (u != null) onSuccess(u.id!!) else { /* TODO: snackbar "User not found" */ }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue") }
    }
}

@Composable
fun ProfileScreen(
    user: User,
    onLogout: () -> Unit,
    onChangeAvatar: (String?) -> Unit
) {
    val ctx = LocalContext.current
    val pick = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            onChangeAvatar(uri.toString())
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.titleLarge)
        if (!user.avatar.isNullOrBlank()) {
            AsyncImage(
                model = user.avatar,
                contentDescription = null,
                modifier = Modifier.size(96.dp).clip(CircleShape)
            )
        } else {
            Icon(Icons.Filled.AccountCircle, null, modifier = Modifier.size(96.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pick.launch(arrayOf("image/*")) }) {
                Text("Change avatar")
            }
            TextButton(onClick = onLogout) { Text("Logout") }
        }
        Text(user.fullName)
        Text(user.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Màn hình đầy đủ: Sign in / Sign up với password, lưu session */
@Composable
fun AuthFlow(
    userDao: UserDao,
    session: SessionManager,
    onLoggedIn: () -> Unit,
    onMessage: (String) -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }

    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(.25f),
                        MaterialTheme.colorScheme.secondary.copy(.25f)
                    )
                )
            )
            .padding(20.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.align(Alignment.Center),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Terrain, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isLogin) "Welcome back" else "Create account",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                var name by remember { mutableStateOf("") }
                var email by remember { mutableStateOf("") }
                var pass by remember { mutableStateOf("") }
                var showPass by remember { mutableStateOf(false) }
                var err by remember { mutableStateOf<String?>(null) }
                fun resetErr() { err = null }

                if (!isLogin) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it; resetErr() },
                        label = { Text("Full name") },
                        leadingIcon = { Icon(Icons.Filled.Badge, null) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = email, onValueChange = { email = it; resetErr() },
                    label = { Text("Email") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Email, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pass, onValueChange = { pass = it; resetErr() },
                    label = { Text("Password") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(
                                if (showPass) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility, null
                            )
                        }
                    },
                    visualTransformation = if (showPass) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error)

                Button(
                    onClick = {
                        if (loading) return@Button
                        loading = true
                        // chạy nền để tránh ANR
                        scope.launch {
                            if (isLogin) {
                                val res = withContext(Dispatchers.Default) {
                                    userDao.login(email.trim(), pass)
                                }
                                res.onSuccess { u ->
                                    session.setUserId(requireNotNull(u.id)) // LƯU Ý: dùng setUserId
                                    onMessage("Signed in as ${u.fullName}")
                                    onLoggedIn()
                                }.onFailure { e ->
                                    onMessage(e.message ?: "Login failed")
                                }
                            } else {
                                if (name.isBlank()) {
                                    onMessage("Please enter full name")
                                } else {
                                    val created = withContext(Dispatchers.Default) {
                                        userDao.create(name.trim(), email.trim(), pass, null)
                                    }
                                    if (created.isSuccess) {
                                        // auto-login
                                        val me = withContext(Dispatchers.Default) {
                                            userDao.login(email.trim(), pass)
                                        }
                                        me.onSuccess { u ->
                                            session.setUserId(requireNotNull(u.id)) // LƯU Ý
                                            onMessage("Account created")
                                            onLoggedIn()
                                        }.onFailure { e ->
                                            onMessage(e.message ?: "Auto login failed")
                                        }
                                    } else {
                                        onMessage(created.exceptionOrNull()?.message ?: "Sign up failed")
                                    }
                                }
                            }
                            loading = false
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (loading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(if (isLogin) Icons.Filled.Login else Icons.Filled.PersonAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isLogin) "Sign in" else "Sign up")
                }


                TextButton(onClick = { isLogin = !isLogin; err = null }) {
                    Text(
                        if (isLogin) "New here? Create an account"
                        else "Have an account? Sign in"
                    )
                }
            }
        }
    }
}


