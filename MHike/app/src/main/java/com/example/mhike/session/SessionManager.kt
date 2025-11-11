package com.example.mhike.session

import android.content.Context
import androidx.core.content.edit

class SessionManager(ctx: Context) {
    private val sp = ctx.getSharedPreferences("session", Context.MODE_PRIVATE)
    fun getUserId(): Long? = sp.getLong("uid", -1L).let { if (it > 0) it else null }
    fun setUserId(id: Long) { sp.edit { putLong("uid", id) } }
    fun clear() { sp.edit { remove("uid") } }
}


