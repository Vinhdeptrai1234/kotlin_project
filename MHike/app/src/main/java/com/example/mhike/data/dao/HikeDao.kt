package com.example.mhike.data.dao

import android.database.Cursor
import com.example.mhike.data.db.DatabaseHelper
import com.example.mhike.data.model.Hike
import android.content.ContentValues

class HikeDao(private val dbh: DatabaseHelper) {
    fun count(): Int =
        dbh.readableDatabase.rawQuery("SELECT COUNT(*) FROM hikes", null)
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun listByUser(userId: Long): List<Hike> =
        dbh.readableDatabase.rawQuery(
            "SELECT * FROM hikes WHERE user_id=? ORDER BY hike_date DESC",
            arrayOf(userId.toString())
        ).use { it.toHikes() }


    fun listAll(): List<Hike> =
        dbh.readableDatabase.rawQuery(
            "SELECT * FROM hikes ORDER BY hike_date DESC", null
        ).use { c -> c.toHikes() }

    fun findById(id: Long): Hike? =
        dbh.readableDatabase.rawQuery(
            "SELECT * FROM hikes WHERE id=?",
            arrayOf(id.toString())
        ).use { c -> if (c.moveToFirst()) c.toHike() else null }

            fun insert(h: Hike): Long {
        val cv = ContentValues().apply {
            put("user_id", h.userId)
            put("name", h.name)
            put("location", h.location)
            put("hike_date", h.hikeDateEpoch)
            put("parking", if (h.parking) 1 else 0)
            put("length_km", h.lengthKm)
            put("difficulty", h.difficulty)
            put("description", h.description)
            put("elevation_gain_m", h.elevationGainM)
            put("max_group_size", h.maxGroupSize)
            put("cover_image", h.coverImage)
            put("created_at", System.currentTimeMillis())
            put("updated_at", System.currentTimeMillis())
        }
        return dbh.writableDatabase.insertOrThrow("hikes", null, cv)
    }

    fun delete(id: Long): Int =
        dbh.writableDatabase.delete("hikes", "id=?", arrayOf(id.toString()))

    fun deleteAll(): Int =
        dbh.writableDatabase.delete("hikes", null, null)

    fun update(h: Hike): Int {
        requireNotNull(h.id)
        val cv = ContentValues().apply {
            put("user_id", h.userId)
            put("name", h.name)
            put("location", h.location)
            put("hike_date", h.hikeDateEpoch)
            put("parking", if (h.parking) 1 else 0)
            put("length_km", h.lengthKm)
            put("difficulty", h.difficulty)
            put("description", h.description)
            put("elevation_gain_m", h.elevationGainM)
            put("max_group_size", h.maxGroupSize)
            put("cover_image", h.coverImage)
            put("updated_at", System.currentTimeMillis())
        }
        return dbh.writableDatabase.update("hikes", cv, "id=?", arrayOf(h.id.toString()))
    }

    // 1. Tìm 1 bản ghi đầu tiên theo tên (LIKE, case-insensitive)
    fun searchFirstByNameLike(q: String): Hike? =
        dbh.readableDatabase.rawQuery(
            """
        SELECT * FROM hikes
        WHERE name LIKE ? COLLATE NOCASE
        ORDER BY hike_date DESC
        LIMIT 1
        """.trimIndent(),
            arrayOf("%${q.trim()}%")
        ).use { c -> if (c.moveToFirst()) c.toHike() else null }

    // 2. Lọc nâng cao: prefix + date range + length range
    fun searchAdvanced(
        prefix: String?,
        fromEpoch: Long?,   // inclusive
        toEpoch: Long?,     // inclusive
        minLen: Double?,
        maxLen: Double?
    ): List<Hike> {
        val where = mutableListOf<String>()
        val args  = mutableListOf<String>()

        if (!prefix.isNullOrBlank()) { where += "name LIKE ? COLLATE NOCASE"; args += "${prefix.trim()}%" }
        if (fromEpoch != null)      { where += "hike_date >= ?";              args += fromEpoch.toString() }
        if (toEpoch != null)        { where += "hike_date <= ?";              args += toEpoch.toString() }
        if (minLen != null)         { where += "length_km >= ?";              args += minLen.toString() }
        if (maxLen != null)         { where += "length_km <= ?";              args += maxLen.toString() }

        val sql = "SELECT * FROM hikes WHERE ${if (where.isEmpty()) "1" else where.joinToString(" AND ")} ORDER BY hike_date DESC"
        return dbh.readableDatabase.rawQuery(sql, args.toTypedArray()).use { it.toHikes() }
    }

    // 3. Dùng cho N15: cố tình tạo lỗi SQLite
    fun simulateDbError() {
        // Table giả không tồn tại -> ném SQLiteException
        dbh.readableDatabase.rawQuery("SELECT * FROM __no_table__", null).use { /* never reached */ }
    }

    // helper: convert 1 dòng Cursor -> Hike
    private fun Cursor.toHike(): Hike = Hike(
        id = getLong(getColumnIndexOrThrow("id")),
        userId = getLong(getColumnIndexOrThrow("user_id")),
        name = getString(getColumnIndexOrThrow("name")),
        location = getString(getColumnIndexOrThrow("location")),
        hikeDateEpoch = getLong(getColumnIndexOrThrow("hike_date")),
        parking = getInt(getColumnIndexOrThrow("parking")) == 1,
        lengthKm = getDouble(getColumnIndexOrThrow("length_km")),
        difficulty = getString(getColumnIndexOrThrow("difficulty")),
        description = getString(getColumnIndexOrThrow("description")),
        elevationGainM = getColumnIndex("elevation_gain_m").takeIf { it >= 0 }?.let { getInt(it) },
        maxGroupSize   = getColumnIndex("max_group_size").takeIf { it >= 0 }?.let { getInt(it) },
        coverImage     = getColumnIndex("cover_image").takeIf { it >= 0 }?.let { getString(it) }
    )

    /* (không bắt buộc nhưng nên) viết lại toHikes() dùng toHike() cho gọn */
    private fun Cursor.toHikes(): List<Hike> {
        val out = mutableListOf<Hike>()
        while (moveToNext()) out.add(toHike())
        return out
    }
}


