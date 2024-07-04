/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.handlers.io

import gg.essential.util.Client
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.util.concurrent.TimeUnit
import kotlin.io.path.listDirectoryEntries


class DirectoryWatcher(
    private val base: Path,
    private val recursive: Boolean,
    private val batchOperationDelay: Long,
    private val batchOperationUnit: TimeUnit,
) {
    private val watchService = base.fileSystem.newWatchService()
    private val keys = mutableMapOf<WatchKey, Path>()
    private val scope = CoroutineScope(Dispatchers.Client)
    private var operationDrainer: Job? = null
    private val filesystemOperations = mutableListOf<FileSystemEvent>()
    private val delayedBatchEventListeners = mutableListOf<FileSystemEventListConsumer>()

    init {
        scope.launch(Dispatchers.IO + CoroutineName("essential-watch-service-${base.fileName}")) {
            init()
            processEvents()
        }
    }

    private fun init() {
        if (recursive) {
            processEvent(base, StandardWatchEventKinds.ENTRY_CREATE, true)
        } else {
            base.registerCMD()
        }
    }

    private fun processEvents() {
        while (true) {
            val key = watchService.take()
            for (event in key.pollEvents()) {
                val kind = event.kind()
                val name = event.context() as Path
                val base = keys[key] ?: continue
                val path = base.resolve(name)
                processEvent(path, kind, false)
            }
            key.reset()
        }
    }

    fun onBatchUpdate(event: FileSystemEventListConsumer) {
        delayedBatchEventListeners.add(event)
    }

    private fun processEvent(path: Path, kind: WatchEvent.Kind<*>, initialScan: Boolean) {
        if (recursive) {
            when (kind) {
                StandardWatchEventKinds.ENTRY_CREATE -> {
                    if (Files.isDirectory(path)) {
                        path.registerCMD()
                        path.listDirectoryEntries().forEach {
                            processEvent(it, StandardWatchEventKinds.ENTRY_CREATE, initialScan)
                        }
                    }
                }
                StandardWatchEventKinds.ENTRY_DELETE -> {
                    keys.entries.removeIf { (key, child) ->
                        if (child.startsWith(path)) {
                            key.cancel()
                            true
                        } else {
                            false
                        }
                    }
                }
            }
        }

        if (initialScan) {
            return
        }

        scope.launch {
            when (kind) {
                StandardWatchEventKinds.ENTRY_CREATE -> {
                    filesystemOperations.add(FileSystemEvent(path, FileEventType.CREATE))
                }
                StandardWatchEventKinds.ENTRY_DELETE -> {
                    filesystemOperations.add(FileSystemEvent(path, FileEventType.DELETE))
                }
                StandardWatchEventKinds.ENTRY_MODIFY -> {
                    filesystemOperations.add(FileSystemEvent(path, FileEventType.MODIFY))
                }
            }
            scheduleOperationDrainer()
        }
    }


    /**
     * Schedules an operation for [batchOperationDelay] [batchOperationUnit] in the future to drain the filesystem operation queue of any pending changes.
     */
    private fun scheduleOperationDrainer() {
        operationDrainer?.cancel()
        operationDrainer = scope.launch {
            delay(batchOperationUnit.toMillis(batchOperationDelay))

            val list = filesystemOperations.toList()
            filesystemOperations.clear()
            delayedBatchEventListeners.forEach {
                it(list)
            }
        }
    }

    private fun Path.registerCMD() = register(
        watchService,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_DELETE
    ).also { keys[it] = this }
}

data class FileSystemEvent(
    val path: Path,
    val eventType: FileEventType,
)

enum class FileEventType {
    CREATE,
    MODIFY,
    DELETE,
}

typealias FileSystemEventListConsumer = (List<FileSystemEvent>) -> Unit
