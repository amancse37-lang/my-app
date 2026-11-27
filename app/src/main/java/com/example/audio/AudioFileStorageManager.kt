package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val AUDIO_STORAGE_TAG = "AudioFileStorageManager"

data class ImportedAudioFileInfo(
    val localFilePath: String,
    val displayName: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val durationSec: Double,
    val waveformPeaks: List<Double>
)

/**
 * Copies user-selected audio files (via zero-permission SAF Document Picker) into app-private
 * persistent storage (`filesDir/motionstory_audio/`), validates MIME type & 15 MB size limit,
 * extracts real audio duration (`MediaMetadataRetriever`), and extracts real normalized PCM
 * amplitude peaks (never decorative waveforms).
 */
class AudioFileStorageManager(private val context: Context) {

    private val audioDir: File by lazy {
        File(context.applicationContext.filesDir, "motionstory_audio").apply {
            if (!exists()) mkdirs()
        }
    }

    fun createPersistentWavFile(prefix: String = "tts"): File {
        val safePrefix = prefix.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(24).ifBlank { "audio" }
        val fileName = "${safePrefix}_${UUID.randomUUID().toString().take(10)}.wav"
        return File(audioDir, fileName)
    }

    fun fileExists(localFilePath: String): Boolean {
        if (localFilePath.isBlank()) return false
        return File(localFilePath).let { it.exists() && it.length() > 0L }
    }

    fun deleteLocalFile(localFilePath: String) {
        if (localFilePath.isBlank()) return
        runCatching {
            val f = File(localFilePath)
            if (f.exists() && f.canonicalPath.startsWith(audioDir.canonicalPath)) {
                f.delete()
            }
        }
    }

    /**
     * Imports an audio document URI into persistent app-private storage with strict MIME/size checks.
     */
    suspend fun importFromUri(uri: Uri, fallbackName: String = "Imported Audio"): Result<ImportedAudioFileInfo> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.applicationContext.contentResolver
                val queriedNameAndSize = queryDisplayNameAndSize(uri)
                val rawName = queriedNameAndSize.first.ifBlank { fallbackName }
                val reportedSize = queriedNameAndSize.second
                val detectedMime = inferMimeType(resolver.getType(uri), rawName)

                if (reportedSize > 0L) {
                    val preCheck = AudioTimelineEngine.validateAudioImport(detectedMime, reportedSize)
                    if (preCheck is AudioValidationResult.Invalid) {
                        return@withContext Result.failure(IllegalArgumentException(preCheck.reason))
                    }
                } else if (detectedMime !in AudioTimelineEngine.ALLOWED_AUDIO_MIME_TYPES) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Unsupported audio format ($detectedMime).")
                    )
                }

                val ext = extensionForMime(detectedMime)
                val destFile = File(audioDir, "imp_${UUID.randomUUID().toString().take(12)}.$ext")
                var totalBytes = 0L

                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            totalBytes += read
                            if (totalBytes > AudioTimelineEngine.MAX_AUDIO_FILE_BYTES) {
                                destFile.delete()
                                return@withContext Result.failure(
                                    IllegalArgumentException("Audio file exceeds the 15 MB studio limit.")
                                )
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                } ?: return@withContext Result.failure(IllegalStateException("Unable to read selected audio file."))

                val validation = AudioTimelineEngine.validateAudioImport(detectedMime, totalBytes)
                if (validation is AudioValidationResult.Invalid) {
                    destFile.delete()
                    return@withContext Result.failure(IllegalArgumentException(validation.reason))
                }

                val analyzed = analyzeLocalAudioFile(
                    file = destFile,
                    displayName = rawName.substringBeforeLast('.').ifBlank { rawName },
                    mimeType = detectedMime
                )
                Result.success(analyzed)
            } catch (e: Exception) {
                Log.w(AUDIO_STORAGE_TAG, "Audio import failed", e)
                Result.failure(e)
            }
        }

    /**
     * Analyzes an existing local persistent audio file (e.g., after Android TTS WAV synthesis)
     * to extract genuine duration and genuine normalized PCM amplitude peaks.
     */
    suspend fun analyzeLocalAudioFile(
        file: File,
        displayName: String,
        mimeType: String = "audio/wav"
    ): ImportedAudioFileInfo = withContext(Dispatchers.IO) {
        val sizeBytes = file.length()
        val durationSec = extractDurationSec(file)
        val peaks = if (mimeType.contains("wav", ignoreCase = true) || file.extension.equals("wav", ignoreCase = true)) {
            extractWavPeaks(file, binCount = 40).ifEmpty { extractMediaCodecPeaks(file, binCount = 40) }
        } else {
            extractMediaCodecPeaks(file, binCount = 40)
        }
        ImportedAudioFileInfo(
            localFilePath = file.absolutePath,
            displayName = displayName.trim().take(120).ifBlank { "Studio Audio" },
            mimeType = mimeType,
            fileSizeBytes = sizeBytes,
            durationSec = durationSec,
            waveformPeaks = peaks
        )
    }

    private fun extractDurationSec(file: File): Double {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            (durationMs / 1000.0).coerceIn(0.1, 1800.0)
        } catch (e: Exception) {
            Log.w(AUDIO_STORAGE_TAG, "MediaMetadataRetriever duration read failed", e)
            estimateWavDurationFallback(file)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun estimateWavDurationFallback(file: File): Double {
        return runCatching {
            FileInputStream(file).use { fis ->
                val header = ByteArray(44)
                if (fis.read(header) < 44) return 1.0
                val byteRate = ByteBuffer.wrap(header, 28, 4).order(ByteOrder.LITTLE_ENDIAN).int
                if (byteRate <= 0) 1.0 else ((file.length() - 44).coerceAtLeast(0L).toDouble() / byteRate).coerceIn(0.1, 1800.0)
            }
        }.getOrDefault(1.0)
    }

    /**
     * Reads real 16-bit PCM amplitudes directly from a standard RIFF/WAVE file into [binCount] normalized peaks.
     * Returns emptyList if the file is not standard 16-bit PCM WAV (never fakes peaks).
     */
    private fun extractWavPeaks(file: File, binCount: Int): List<Double> {
        return runCatching {
            val totalLen = file.length()
            if (totalLen <= 44L) return emptyList()
            FileInputStream(file).use { fis ->
                val header = ByteArray(44)
                if (fis.read(header) < 44) return emptyList()
                val riff = String(header, 0, 4)
                val wave = String(header, 8, 4)
                val bitsPerSample = ByteBuffer.wrap(header, 34, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                if (riff != "RIFF" || wave != "WAVE" || bitsPerSample != 16) return emptyList()

                val dataBytes = totalLen - 44L
                val totalSamples = (dataBytes / 2L).coerceAtLeast(1L)
                val samplesPerBin = max(1L, totalSamples / binCount.toLong())
                val peaks = DoubleArray(binCount)
                val buf = ByteArray(8192)
                var sampleIndex = 0L
                while (true) {
                    val read = fis.read(buf)
                    if (read <= 1) break
                    var i = 0
                    while (i + 1 < read) {
                        val lo = buf[i].toInt() and 0xFF
                        val hi = buf[i + 1].toInt()
                        val sample = (hi shl 8) or lo
                        val norm = abs(sample.toDouble()) / 32768.0
                        val bin = ((sampleIndex / samplesPerBin).toInt()).coerceIn(0, binCount - 1)
                        if (norm > peaks[bin]) peaks[bin] = norm
                        sampleIndex++
                        i += 2
                    }
                }
                val maxPeak = peaks.maxOrNull() ?: 0.0
                if (maxPeak <= 0.001) return emptyList()
                peaks.map { (it / maxPeak).coerceIn(0.0, 1.0) }
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Decodes real PCM amplitude envelopes from MP3/M4A/AAC/OGG via Android MediaExtractor + MediaCodec
     * with a strict 3.5-second time guard so low-end devices stay responsive. Returns emptyList if
     * the device codec cannot decode PCM — never returns decorative or random waveforms.
     */
    private fun extractMediaCodecPeaks(file: File, binCount: Int): List<Double> {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            extractor.setDataSource(file.absolutePath)
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }
            if (audioTrackIndex < 0 || format == null) return emptyList()
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return emptyList()
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(100_000L)
            } else {
                5_000_000L
            }

            extractor.selectTrack(audioTrackIndex)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val binSums = DoubleArray(binCount)
            val binCounts = IntArray(binCount)
            val info = MediaCodec.BufferInfo()
            val deadlineMs = System.currentTimeMillis() + 3500L
            var inputDone = false
            var outputDone = false

            while (!outputDone && System.currentTimeMillis() < deadlineMs) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(8_000L)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                val pts = extractor.sampleTime
                                codec.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(info, 8_000L)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && info.size > 0) {
                        val progress = (info.presentationTimeUs.toDouble() / durationUs.toDouble()).coerceIn(0.0, 0.999)
                        val bin = (progress * binCount).toInt().coerceIn(0, binCount - 1)
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        val shortBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        val count = shortBuf.remaining()
                        var sumSq = 0.0
                        val stride = max(1, count / 64)
                        var sampled = 0
                        var idx = 0
                        while (idx < count) {
                            val s = shortBuf.get(idx).toDouble() / 32768.0
                            sumSq += s * s
                            sampled++
                            idx += stride
                        }
                        if (sampled > 0) {
                            val rms = sqrt(sumSq / sampled)
                            binSums[bin] = max(binSums[bin], rms)
                            binCounts[bin]++
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }

            val maxVal = binSums.maxOrNull() ?: 0.0
            if (maxVal <= 0.001) return emptyList()
            binSums.map { (it / maxVal).coerceIn(0.0, 1.0) }
        } catch (e: Exception) {
            Log.w(AUDIO_STORAGE_TAG, "MediaCodec waveform extraction skipped: ${e.message}")
            emptyList()
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun queryDisplayNameAndSize(uri: Uri): Pair<String, Long> {
        var name = ""
        var size = -1L
        runCatching {
            context.applicationContext.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = cursor.getString(nameIdx).orEmpty()
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                }
            }
        }
        return name to size
    }

    private fun inferMimeType(rawMime: String?, fileName: String): String {
        val cleanMime = rawMime?.trim()?.lowercase().orEmpty()
        if (cleanMime in AudioTimelineEngine.ALLOWED_AUDIO_MIME_TYPES) return cleanMime
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg", "oga" -> "audio/ogg"
            "webm" -> "audio/webm"
            "flac" -> "audio/flac"
            else -> cleanMime.ifBlank { "audio/mpeg" }
        }
    }

    private fun extensionForMime(mime: String): String {
        return when (mime) {
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/mp4", "audio/m4a" -> "m4a"
            "audio/aac" -> "aac"
            "audio/ogg" -> "ogg"
            "audio/webm" -> "webm"
            "audio/flac" -> "flac"
            else -> "mp3"
        }
    }
}
