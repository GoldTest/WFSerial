package org.example.wfserial

import java.io.File

class DesktopStorageProvider : StorageProvider {
    private val folderPath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "WFSerial"
    private val filePath = folderPath + File.separator + "data.json"

    init {
        val folder = File(folderPath)
        if (!folder.exists()) {
            folder.mkdirs()
        }
    }

    override fun save(data: String) {
        File(filePath).writeText(data)
    }

    override fun load(): String? {
        val file = File(filePath)
        return if (file.exists()) file.readText() else null
    }
}

actual fun getStorageProvider(): StorageProvider = DesktopStorageProvider()
