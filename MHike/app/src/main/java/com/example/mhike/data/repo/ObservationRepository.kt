package com.example.mhike.data.repo

import android.util.Log
import com.example.mhike.data.dao.ObservationDao
import com.example.mhike.data.model.Observation

private const val TAG = "ObsRepo"

class ObservationRepository(val dao: ObservationDao) {

    fun listByHike(hikeId: Long): Result<List<Observation>> = runCatching {
        val list = dao.listByHike(hikeId)
        Log.i(TAG, "listByHike($hikeId) -> ${list.size}")
        list
    }.onFailure { Log.e(TAG, "listByHike($hikeId) -> error", it) }

    fun get(id: Long): Result<Observation> = runCatching {
        dao.findById(id) ?: error("Observation $id not found")
    }.onSuccess { Log.i(TAG, "get($id) -> ok") }
        .onFailure { Log.e(TAG, "get($id) -> error", it) }

    fun add(o: Observation): Result<Long> = runCatching {
        val id = dao.insert(o)
        Log.i(TAG, "add -> ok id=$id")
        id
    }.onFailure { Log.e(TAG, "add -> error", it) }

    fun update(o: Observation): Result<Int> = runCatching {
        val n = dao.update(o)
        Log.i(TAG, "update(${o.id}) -> rows=$n")
        n
    }.onFailure { Log.e(TAG, "update(${o.id}) -> error", it) }

    fun delete(id: Long): Result<Int> = runCatching {
        val n = dao.delete(id)
        Log.i(TAG, "delete($id) -> rows=$n")
        n
    }.onFailure { Log.e(TAG, "delete($id) -> error", it) }
}
