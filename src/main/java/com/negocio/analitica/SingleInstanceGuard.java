package com.negocio.analitica;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Prevents two local AKRON application processes from operating on the same
 * business installation at the same time. The operating system releases the
 * lock when the process exits or the channel is closed.
 */
public final class SingleInstanceGuard implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    private SingleInstanceGuard(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    public static SingleInstanceGuard acquire(Path lockPath) {
        if (lockPath == null) {
            throw new IllegalArgumentException("La ruta del bloqueo es obligatoria.");
        }

        FileChannel channel = null;
        try {
            Path absolutePath = lockPath.toAbsolutePath();
            Path parent = absolutePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            channel = FileChannel.open(absolutePath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock = channel.tryLock();
            if (lock == null) {
                channel.close();
                throw new IllegalStateException(
                        "AKRON ya está abierto en este computador. Cierra la otra instancia antes de continuar.");
            }
            return new SingleInstanceGuard(channel, lock);
        } catch (OverlappingFileLockException error) {
            closeQuietly(channel);
            throw new IllegalStateException(
                    "AKRON ya está abierto en este computador. Cierra la otra instancia antes de continuar.", error);
        } catch (IOException error) {
            closeQuietly(channel);
            throw new IllegalStateException(
                    "No se pudo reservar el acceso exclusivo a AKRON: " + error.getMessage(), error);
        }
    }

    @Override
    public void close() {
        try {
            if (lock.isValid()) {
                lock.release();
            }
            channel.close();
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo liberar el bloqueo de AKRON.", error);
        }
    }

    private static void closeQuietly(FileChannel channel) {
        if (channel == null) return;
        try {
            channel.close();
        } catch (IOException ignored) {
            // Preserve the original lock-acquisition error.
        }
    }
}
