package com.vltv.play

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Environment

object DownloadHelper {

    private const val PREFS_NAME = "vltv_prefs"
    private const val KEY_DM_ID_PREFIX = "dm_id_"          // dm_id_<logicalId>
    private const val KEY_DL_STATE_PREFIX = "dl_state_"    // dl_state_<logicalId>

    const val STATE_BAIXAR = "BAIXAR"
    const val STATE_BAIXANDO = "BAIXANDO"
    const val STATE_BAIXADO = "BAIXADO"

    /**
     * logicalId = id lógico do item (movie streamId ou episodeId)
     * type = "movie" ou "series" (só para você diferenciar depois se quiser)
     */
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

    /**
     * Registrar esse receiver uma vez, por exemplo no Application ou na Activity principal.
     * Ele converte status do DownloadManager -> STATE_BAIXADO / BAIXANDO / BAIXAR.
     */
    fun registerReceiver(context: Context) {
        context.registerReceiver(
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id == -1L) return

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val all = prefs.all

            val logicalId = all.keys
                .firstOrNull { key ->
                    key.startsWith(KEY_DM_ID_PREFIX) &&
                            prefs.getLong(key, -2L) == id
                }
                ?.removePrefix(KEY_DM_ID_PREFIX)
                ?: return

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(id)
            val cursor: Cursor = dm.query(query) ?: return

            cursor.use {
                if (!it.moveToFirst()) return

                val statusIdx = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIdx == -1) return

                when (it.getInt(statusIdx)) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        setDownloadState(context, logicalId, STATE_BAIXADO)
                    }
                    DownloadManager.STATUS_FAILED -> {
                        setDownloadState(context, logicalId, STATE_BAIXAR)
                    }
                    DownloadManager.STATUS_RUNNING,
                    DownloadManager.STATUS_PENDING,
                    DownloadManager.STATUS_PAUSED -> {
                        setDownloadState(context, logicalId, STATE_BAIXANDO)
                    }
                }
            }
        }
    }
}
