package com.example.sihscrap.ai

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Native GGUF (v2 / v3) Binary Parser and Memory-Mapper for Android.
 * 
 * Safely inspects and memory-maps multi-gigabyte GGUF models (like Qwen2.5-VL-7B)
 * on 64-bit Android (Snapdragon 8 Elite) without hitting ART's 512 MB Java heap ceiling.
 */
object GgufModelReader {
    private const val TAG = "GgufModelReader"
    private const val GGUF_MAGIC = 0x46554747 // "GGUF" in little-endian

    // GGUF Metadata Value Types
    private const val GGUF_TYPE_UINT8 = 0
    private const val GGUF_TYPE_INT8 = 1
    private const val GGUF_TYPE_UINT16 = 2
    private const val GGUF_TYPE_INT16 = 3
    private const val GGUF_TYPE_UINT32 = 4
    private const val GGUF_TYPE_INT32 = 5
    private const val GGUF_TYPE_FLOAT32 = 6
    private const val GGUF_TYPE_BOOL = 7
    private const val GGUF_TYPE_STRING = 8
    private const val GGUF_TYPE_ARRAY = 9
    private const val GGUF_TYPE_UINT64 = 10
    private const val GGUF_TYPE_INT64 = 11
    private const val GGUF_TYPE_FLOAT64 = 12

    data class GgufModelInfo(
        val fileName: String,
        val filePath: String,
        val fileSizeBytes: Long,
        val fileSizeGb: Double,
        val version: Int,
        val tensorCount: Long,
        val architecture: String,
        val modelName: String,
        val isVisionCapable: Boolean,
        val isValidGguf: Boolean
    )

    /**
     * Inspects the binary GGUF header of a model file on disk.
     */
    fun parseHeader(file: File): GgufModelInfo? {
        if (!file.exists() || file.length() < 16) return null

        try {
            RandomAccessFile(file, "r").use { raf ->
                val channel = raf.channel
                // Read first 64KB containing header and key-value metadata
                val headerBytes = 64 * 1024
                val buffer = ByteBuffer.allocate(minOf(headerBytes, file.length().toInt()))
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                channel.read(buffer)
                buffer.flip()

                val magic = buffer.int
                if (magic != GGUF_MAGIC) {
                    Log.d(TAG, "${file.name} is not a valid GGUF file (magic: 0x${Integer.toHexString(magic)})")
                    return null
                }

                val version = buffer.int
                val tensorCount = buffer.long
                val metadataKvCount = buffer.long

                var architecture = "unknown"
                var modelName = file.nameWithoutExtension

                // Parse initial key-value pairs
                val maxPairsToRead = minOf(metadataKvCount, 128L).toInt()
                for (i in 0 until maxPairsToRead) {
                    if (buffer.remaining() < 12) break
                    val keyLen = buffer.long.toInt()
                    if (keyLen <= 0 || keyLen > buffer.remaining()) break

                    val keyBytes = ByteArray(keyLen)
                    buffer.get(keyBytes)
                    val key = String(keyBytes, Charsets.UTF_8)

                    if (buffer.remaining() < 4) break
                    val valueType = buffer.int

                    when (valueType) {
                        GGUF_TYPE_UINT8, GGUF_TYPE_INT8, GGUF_TYPE_BOOL -> if (buffer.remaining() >= 1) buffer.get()
                        GGUF_TYPE_UINT16, GGUF_TYPE_INT16 -> if (buffer.remaining() >= 2) buffer.short
                        GGUF_TYPE_UINT32, GGUF_TYPE_INT32, GGUF_TYPE_FLOAT32 -> if (buffer.remaining() >= 4) buffer.int
                        GGUF_TYPE_UINT64, GGUF_TYPE_INT64, GGUF_TYPE_FLOAT64 -> if (buffer.remaining() >= 8) buffer.long
                        GGUF_TYPE_STRING -> {
                            if (buffer.remaining() >= 8) {
                                val strLen = buffer.long.toInt()
                                if (strLen in 1..buffer.remaining()) {
                                    val strBytes = ByteArray(strLen)
                                    buffer.get(strBytes)
                                    val strVal = String(strBytes, Charsets.UTF_8)
                                    if (key == "general.architecture") architecture = strVal
                                    if (key == "general.name") modelName = strVal
                                }
                            }
                        }
                        else -> break // Skip complex arrays to stay within buffer
                    }
                }

                val isVision = architecture.contains("vl", ignoreCase = true) ||
                        file.name.contains("vl", ignoreCase = true) ||
                        architecture.contains("qwen2vl", ignoreCase = true)

                val sizeGb = file.length() / (1024.0 * 1024.0 * 1024.0)

                return GgufModelInfo(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSizeBytes = file.length(),
                    fileSizeGb = sizeGb,
                    version = version,
                    tensorCount = tensorCount,
                    architecture = architecture,
                    modelName = modelName,
                    isVisionCapable = isVision,
                    isValidGguf = true
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing GGUF header for ${file.name}: ${e.message}")
            return null
        }
    }

    /**
     * Memory-maps a multi-gigabyte GGUF file in 1 GB chunks.
     * Uses Linux kernel mmap via FileChannel.map, avoiding ART Java heap limits.
     */
    fun memoryMapGguf(file: File): List<ByteBuffer> {
        val mappedBuffers = mutableListOf<ByteBuffer>()
        try {
            RandomAccessFile(file, "r").use { raf ->
                val channel = raf.channel
                val totalLength = file.length()
                val chunkSize = 1024L * 1024L * 1024L // 1 GB chunks

                var offset = 0L
                while (offset < totalLength) {
                    val remaining = totalLength - offset
                    val sizeToMap = minOf(chunkSize, remaining)
                    val buffer = channel.map(FileChannel.MapMode.READ_ONLY, offset, sizeToMap)
                    // Touch first page of chunk to warm up kernel page tables
                    buffer.get(0)
                    mappedBuffers.add(buffer)
                    offset += sizeToMap
                }
            }
            Log.i(TAG, "Successfully mapped ${file.name} (${String.format("%.2f", file.length() / (1024.0 * 1024.0 * 1024.0))} GB) in ${mappedBuffers.size} native chunks.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to memory-map ${file.name}: ${e.message}", e)
        }
        return mappedBuffers
    }
}
