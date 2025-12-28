package org.example.wfserial

interface StorageProvider {
    fun save(data: String)
    fun load(): String?
}

expect fun getStorageProvider(): StorageProvider
