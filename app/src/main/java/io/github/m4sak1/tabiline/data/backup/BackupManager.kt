package io.github.m4sak1.tabiline.data.backup

import android.content.Context
import android.net.Uri
import io.github.m4sak1.tabiline.BuildConfig
import io.github.m4sak1.tabiline.data.repository.TabilineRepository
import io.github.m4sak1.tabiline.data.settings.SettingsRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.Instant

class BackupManager(
    private val context: Context,
    private val repository: TabilineRepository,
    private val settings: SettingsRepository,
    private val awaitReady: suspend () -> Unit = {},
) {
    private val mutex = Mutex()
    private val preferences = context.getSharedPreferences("tabiline_backup", Context.MODE_PRIVATE)
    val lastExportAt: Instant? get() = preferences.getString("last_export", null)?.let(Instant::parse)

    suspend fun <T> exclusive(block: suspend () -> T): T = mutex.withLock { block() }

    suspend fun export(uri: Uri) = exclusive {
        awaitReady()
        val now = Instant.now()
        val bytes = BackupCodec.encode(BackupDocument(now, BuildConfig.VERSION_NAME,
            repository.snapshotTrips(), settings.settings.first())).toByteArray(Charsets.UTF_8)
        (context.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot open destination"))
            .use { it.write(bytes); it.flush() }
        preferences.edit().putString("last_export", now.toString()).commit()
        now
    }

    suspend fun inspect(uri: Uri): BackupDocument {
        val bytes = ByteArrayOutputStream()
        (context.contentResolver.openInputStream(uri) ?: error("Cannot open backup")).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(bytes.size() + count <= BackupCodec.MAX_BYTES) { "ファイルが大きすぎます（最大10MB）。" }
                bytes.write(buffer, 0, count)
            }
        }
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString()
        return BackupCodec.decode(text)
    }

    suspend fun restore(document: BackupDocument, restoreSettings: Boolean) = exclusive {
        awaitReady()
        BackupCodec.validate(document)
        val previous = settings.settings.first()
        try {
            repository.replaceTrips(document.trips) {
                if (restoreSettings) settings.update(document.settings)
            }
        } catch (failure: Throwable) {
            if (restoreSettings) withContext(NonCancellable) {
                try { settings.update(previous) } catch (rollback: Throwable) { failure.addSuppressed(rollback) }
            }
            throw failure
        }
    }
}
