package com.example.mhike.data.dao

import android.content.ContentValues
import android.database.Cursor
import com.example.mhike.data.db.DatabaseHelper
import com.example.mhike.data.model.Observation

class ObservationDao(private val dbh: DatabaseHelper) {

    fun listByHike(hikeId: Long): List<Observation> =
        dbh.readableDatabase.rawQuery(
            "SELECT * FROM observations WHERE hike_id=? ORDER BY observed_at DESC",
            arrayOf(hikeId.toString())
        ).use { c -> c.toList() }

    fun countForHike(hikeId: Long): Int =
        dbh.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM observations WHERE hike_id=?",
            arrayOf(hikeId.toString())
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun findById(id: Long): Observation? =
        dbh.readableDatabase.rawQuery(
            "SELECT * FROM observations WHERE id=?",
            arrayOf(id.toString())
        ).use { c -> if (c.moveToFirst()) c.toOne() else null }

    fun insert(o: Observation): Long {
        val cv = ContentValues().apply {
            put("hike_id", o.hikeId)
            put("observed_at", o.observedAt)
            put("note", o.note)
            put("comments", o.comments)
        }
        return dbh.writableDatabase.insertOrThrow("observations", null, cv)
    }

    fun update(o: Observation): Int {
        requireNotNull(o.id)
        val cv = ContentValues().apply {
            put("hike_id", o.hikeId)
            put("observed_at", o.observedAt)
            put("note", o.note)
            put("comments", o.comments)
        }
        return dbh.writableDatabase.update("observations", cv, "id=?", arrayOf(o.id.toString()))
    }

    fun delete(id: Long): Int =
        dbh.writableDatabase.delete("observations", "id=?", arrayOf(id.toString()))

    fun deleteAllForHike(hikeId: Long): Int =
        dbh.writableDatabase.delete("observations", "hike_id=?", arrayOf(hikeId.toString()))

    /* -------- Cursor helpers -------- */

    private fun Cursor.toOne(): Observation = Observation(
        id = getLong(getColumnIndexOrThrow("id")),
        hikeId = getLong(getColumnIndexOrThrow("hike_id")),
        observedAt = getLong(getColumnIndexOrThrow("observed_at")),
        note = getString(getColumnIndexOrThrow("note")),
        comments = getColumnIndex("comments").takeIf { it >= 0 }?.let { getString(it) }
    )

    private fun Cursor.toList(): List<Observation> {
        val out = mutableListOf<Observation>()
        while (moveToNext()) out.add(toOne())
        return out
    }
}
