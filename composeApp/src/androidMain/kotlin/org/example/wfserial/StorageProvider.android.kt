package org.example.wfserial

import android.content.Context
import java.io.File

class AndroidStorageProvider(private val context: Context) : StorageProvider {
    private val filePath = context.filesDir.absolutePath + File.separator + "data.json"

    override fun save(data: String) {
        File(filePath).writeText(data)
    }

    override fun load(): String? {
        val file = File(filePath)
        return if (file.exists()) file.readText() else null
    }
}

// Note: In Android, we need a way to pass context. 
// For simplicity in this demo, we'll use a static reference or initialize it in MainActivity.
object AndroidStorage {
    lateinit var context: Context
}

actual fun getStorageProvider(): StorageProvider = AndroidStorageProvider(AndroidStorage.context)
