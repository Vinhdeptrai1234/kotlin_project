// app/src/main/java/com/example/mhike/data/db/DatabaseHelper.kt
package com.example.mhike.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

private const val DB_NAME = "mhike.db"
// Tăng version để kích hoạt onUpgrade
private const val DB_VERSION = 3
private const val TAG = "MHikeDB"

class DatabaseHelper(private val ctx: Context) :
    SQLiteOpenHelper(ctx, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.execSQL("PRAGMA foreign_keys=ON;")
        Log.d(TAG, "onConfigure -> foreign_keys=ON; path=${ctx.getDatabasePath(DB_NAME).absolutePath}")
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "onCreate -> seeding from assets/sql/schema_seed.sql ...")
        execSqlScript(db, "sql/schema_seed.sql")
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
            val tables = buildList { while (c.moveToNext()) add(c.getString(0)) }
            Log.d(TAG, "onCreate -> tables=$tables")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "onUpgrade from $oldVersion to $newVersion")
        // v2: đảm bảo có bảng users + user_id cho hikes (nếu DB cũ chưa có)
        if (oldVersion < 2) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS users (
                  id            INTEGER PRIMARY KEY AUTOINCREMENT,
                  full_name     TEXT NOT NULL,
                  email         TEXT NOT NULL UNIQUE,
                  avatar        TEXT,
                  password_hash TEXT NOT NULL DEFAULT '',
                  created_at    INTEGER NOT NULL DEFAULT (CAST(strftime('%s','now') AS INTEGER) * 1000),
                  updated_at    INTEGER NOT NULL DEFAULT (CAST(strftime('%s','now') AS INTEGER) * 1000)
                )
            """.trimIndent())
            try { db.execSQL("ALTER TABLE hikes ADD COLUMN user_id INTEGER NOT NULL DEFAULT 1") } catch (_: Exception) {}
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_hikes_user ON hikes(user_id)")
        }
        // v3: thêm cover_image cho hikes + thêm password_hash nếu thiếu
        if (oldVersion < 3) {
            try { db.execSQL("ALTER TABLE hikes ADD COLUMN cover_image TEXT") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE users ADD COLUMN password_hash TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
        }
    }

    private fun execSqlScript(db: SQLiteDatabase, assetPath: String) {
        ctx.assets.open(assetPath).use { input ->
            BufferedReader(InputStreamReader(input)).use { br ->
                val sb = StringBuilder()
                var inTrigger = false
                fun flush() {
                    val stmt = sb.toString().trim()
                    if (stmt.isNotEmpty()) db.execSQL(stmt)
                    sb.setLength(0)
                }
                br.lineSequence().forEach { raw ->
                    val line = raw.trim()
                    if (line.isEmpty() || line.startsWith("--")) return@forEach
                    val lower = line.lowercase(Locale.ROOT)
                    if (lower.startsWith("create trigger")) inTrigger = true
                    sb.append(line).append(' ')
                    if (inTrigger) {
                        if (lower.endsWith("end;")) { flush(); inTrigger = false }
                    } else if (line.endsWith(";")) flush()
                }
                if (sb.isNotEmpty()) flush()
            }
        }
    }
}
