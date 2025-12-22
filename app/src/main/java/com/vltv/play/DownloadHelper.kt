package com.vltv.play

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment

object DownloadHelper {

    private const val PREFS_NAME = "vltv_prefs"
    private const val KEY_DM_ID_PREFIX = "dm_id_"
    private const val KEY_DL_STATE_PREFIX = "dl_state_"

    const val STATE_BAIXAR = "BAIXAR"
    const val STATE_BAIXANDO = "BAIXANDO"
    const val STATE_BAIXADO = "BAIXADO"

    fun enqueueDownload(
        context: Context,
        url: String,
        fileName: String,
        logicalId: String,
        type: String
    ) {
        val uri = Uri.parse(url)

        val request = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setDescription("Baixando $type")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_MOVIES,
                fileName
            )

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_DM_ID_PREFIX + logicalId, downloadId)
            .putString(KEY_DL_STATE_PREFIX + logicalId, STATE_BAIXANDO)
            .apply()
    }

    fun getDownloadState(context: Context, logicalId: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DL_STATE_PREFIX + logicalId, STATE_BAIXAR) ?: STATE_BAIXAR
    }

    fun setDownloadState(context: Context, logicalId: String, state: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DL_STATE_PREFIX + logicalId, state)
            .apply()
    }

    fun registerReceiver(context: Context) {
        try {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        } catch (e: Exception) {
            // já registrado
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id == -1L) return

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val logicalId = prefs.all.keys
                .firstOrNull { key ->
                    key.startsWith(KEY_DM_ID_PREFIX) && prefs.getLong(key, -1L) == id
                }
                ?.removePrefix(KEY_DM_ID_PREFIX) ?: return

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(id)
            dm.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                    )
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL ->
                            setDownloadState(context, logicalId, STATE_BAIXADO)
                        DownloadManager.STATUS_FAILED ->
                            setDownloadState(context, logicalId, STATE_BAIXAR)
                    }
                }
            }
        }
    }
}
