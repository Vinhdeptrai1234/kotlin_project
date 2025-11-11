package com.example.mhike.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import com.example.mhike.data.db.DatabaseHelper
import com.example.mhike.data.model.User
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UserDao(private val dbh: DatabaseHelper) {

    fun create(fullName: String, email: String, plain: String, avatar: String?): Result<Long> =
        runCatching {
            val emailNorm = email.trim().lowercase()
            require(!emailExists(emailNorm)) { "Email already exists" }
            val cv = ContentValues().apply {
                put("full_name", fullName.trim())
                put("email", emailNorm)
                put("avatar", avatar)
                put("password_hash", hashPassword(plain))
            }
            dbh.writableDatabase.insertOrThrow("users", null, cv)
        }

    fun findById(id: Long): User? =
        dbh.readableDatabase.rawQuery(
            "SELECT id, full_name, email, avatar, password_hash FROM users WHERE id=? LIMIT 1",
            arrayOf(id.toString())
        ).use { c ->
            if (!c.moveToFirst()) return null
            User(
                id = c.getLong(0),
                fullName = c.getString(1),
                email = c.getString(2),
                avatar = c.getString(3),
                passwordHash = c.getString(4)
            )
        }

    fun findByEmail(email: String): User? =
        dbh.readableDatabase.rawQuery(
            "SELECT id, full_name, email, avatar, password_hash FROM users WHERE email=? LIMIT 1",
            arrayOf(email)
        ).use { c ->
            if (!c.moveToFirst()) return null
            User(
                id = c.getLong(0),
                fullName = c.getString(1),
                email = c.getString(2),
                avatar = c.getString(3),
                passwordHash = c.getString(4)
            )
        }


    fun updateAvatar(userId: Long, uri: String?): Result<Int> =
        runCatching {
            val cv = ContentValues().apply { put("avatar", uri) }
            dbh.writableDatabase.update("users", cv, "id=?", arrayOf(userId.toString()))
        }

    fun login(email: String, plain: String): Result<User> = runCatching {
        val emailNorm = email.trim().lowercase()
        dbh.readableDatabase.rawQuery(
            "SELECT id, full_name, email, avatar, password_hash FROM users WHERE email=?",
            arrayOf(emailNorm)
        ).use { c ->
            require(c.moveToFirst()) { "Wrong email or password" }
            val hash = c.getString(4)
            require(verifyPassword(plain, hash)) { "Wrong email or password" }
            User(c.getLong(0), c.getString(1), c.getString(2), hash, c.getString(3))
        }
    }

    fun emailExists(email: String): Boolean {
        val db = dbh.readableDatabase
        db.rawQuery("SELECT 1 FROM users WHERE email = ? LIMIT 1", arrayOf(email)).use { c ->
            return c.moveToFirst()
        }
    }

    // ===== PBKDF2 =====
    private fun hashPassword(plain: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iter = 120_000
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(plain.toCharArray(), salt, iter, 256)
        val hash = skf.generateSecret(spec).encoded

        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        // GHÉP CHUỖI CÓ DẤU $ RÕ RÀNG
        return buildString {
            append("pbkdf2"); append('$')
            append(iter);     append('$')
            append(saltB64);  append('$')
            append(hashB64)
        }
    }

    private fun verifyPassword(plain: String, packed: String?): Boolean {
        if (packed.isNullOrBlank()) return false
        val parts = packed.split('$')
        if (parts.size != 4 || parts[0] != "pbkdf2") return false

        val iter  = parts[1].toIntOrNull() ?: return false
        val salt  = Base64.decode(parts[2], Base64.NO_WRAP)
        val expect = Base64.decode(parts[3], Base64.NO_WRAP)

        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(plain.toCharArray(), salt, iter, expect.size * 8)
        val hash = skf.generateSecret(spec).encoded

        // constant-time compare
        var diff = 0
        for (i in hash.indices) diff = diff or (hash[i].toInt() xor expect[i].toInt())
        return diff == 0
    }
}
