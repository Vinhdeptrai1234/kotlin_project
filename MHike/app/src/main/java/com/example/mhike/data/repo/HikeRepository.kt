package com.example.mhike.data.repo

import android.util.Log
import com.example.mhike.data.dao.HikeDao
import com.example.mhike.data.model.Hike

private const val TAG = "HikeRepo"

class HikeRepository(val dao: HikeDao) {

    fun getAll(): Result<List<Hike>> = runCatching {
        val list = dao.listAll()
        Log.i(TAG, "getAll -> ok size=${list.size}")
        list
    }.onFailure { Log.e(TAG, "getAll -> error", it) }

    fun get(id: Long): Result<Hike> = runCatching {
        dao.findById(id) ?: error("Hike $id not found")
    }.onSuccess { Log.i(TAG, "get($id) -> ok") }
        .onFailure { Log.e(TAG, "get($id) -> error", it) }

    fun getAllByUser(userId: Long) = runCatching { dao.listByUser(userId) }


    fun add(h: Hike): Result<Long> = runCatching {
        val rowId = dao.insert(h)
        Log.i(TAG, "add -> ok id=$rowId")
        rowId
    }.onFailure { Log.e(TAG, "add -> error", it) }

    fun update(h: Hike): Result<Int> = runCatching {
        val n = dao.update(h)
        Log.i(TAG, "update(${h.id}) -> ok rows=$n")
        n
    }.onFailure { Log.e(TAG, "update(${h.id}) -> error", it) }

    fun delete(id: Long): Result<Int> = runCatching {
        val n = dao.delete(id)
        Log.i(TAG, "delete($id) -> ok rows=$n")
        n
    }.onFailure { Log.e(TAG, "delete($id) -> error", it) }

    fun deleteAll(): Result<Int> = runCatching {
        val n = dao.deleteAll()
        Log.w(TAG, "deleteAll -> ok rows=$n")
        n
    }.onFailure { Log.e(TAG, "deleteAll -> error", it) }

    fun searchBasicByName(name: String, simulateError: Boolean = false): Result<Hike?> = runCatching {
        if (simulateError) dao.simulateDbError()  // ném lỗi giả lập
        dao.searchFirstByNameLike(name)
    }.onSuccess { Log.i(TAG, "searchBasicByName('$name') -> ${if (it == null) "none" else "1 result"}") }
        .onFailure { Log.e(TAG, "searchBasicByName('$name') -> error", it) }

    fun searchAdvanced(
        prefix: String?,
        fromEpoch: Long?,
        toEpoch: Long?,
        minLen: Double?,
        maxLen: Double?
    ): Result<List<Hike>> = runCatching {
        dao.searchAdvanced(prefix, fromEpoch, toEpoch, minLen, maxLen)
    }.onSuccess { Log.i(TAG, "searchAdvanced -> ${it.size} results") }
        .onFailure { Log.e(TAG, "searchAdvanced -> error", it) }

}
