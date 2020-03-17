/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import com.github.michaelbull.retry.ContinueRetrying
import com.github.michaelbull.retry.RetryFailure
import com.github.michaelbull.retry.RetryInstruction
import com.github.michaelbull.retry.StopRetrying
import com.github.michaelbull.retry.policy.RetryPolicy
import com.github.michaelbull.retry.policy.constantDelay
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.input.CountingInputStream
import org.apache.http.HttpHeaders
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.system.measureNanoTime

private const val MAX_RETRIES = 3
private const val RETRY_DELAY_MS = 1000L
private const val DEFAULT_REQUEST_TIMEOUT = 30_000
private const val ENCODING = "gzip, deflate"
private const val NS_TO_S = 1_000_000_000.0
private const val KB_TO_B = 1_000.0

private val retryPolicy: RetryPolicy<Throwable> = {
    if (reason is IOException) ContinueRetrying else StopRetrying
}

private val policy: suspend RetryFailure<Throwable>.() -> RetryInstruction =
    retryPolicy + limitAttempts(MAX_RETRIES) + constantDelay(RETRY_DELAY_MS)

private fun request(connection: Connection) = runBlocking {
    retry(policy) { connection.execute() }
}

private fun download(inputStream: InputStream, path: Path) {
    Files.createDirectories(path.parent)

    val countingInputStream = CountingInputStream(inputStream)

    val duration = countingInputStream.use {
        measureNanoTime { Files.copy(countingInputStream, path, StandardCopyOption.REPLACE_EXISTING) }
    }

    val size = countingInputStream.byteCount
    val humanSize = FileUtils.byteCountToDisplaySize(size)
    val speed = NS_TO_S * size / (KB_TO_B * duration)
    val humanSpeed = "%.2f kB/s".format(speed)

    println("↓ $path [$humanSize] [$humanSpeed]")
}

class SpongeService(private val spongeServiceConfig: SpongeServiceConfig) {
    fun request(spongeUri: SpongeUri, timeout: Int = DEFAULT_REQUEST_TIMEOUT): Connection.Response {
        val connection = Jsoup.connect(spongeUri.uri)
            .timeout(timeout)
            .header(HttpHeaders.ACCEPT_ENCODING, ENCODING)
            .referrer(spongeServiceConfig.referrer)
            .userAgent(spongeServiceConfig.userAgent)
            .maxBodySize(0)
            .ignoreHttpErrors(true)
            .ignoreContentType(true)
            .followRedirects(true)

        return request(connection)
    }

    fun download(spongeUri: SpongeUri) {
        val path = getDownloadPath(spongeUri)

        if (!spongeServiceConfig.overwriteExistingFiles && Files.exists(path)) {
            println("∃ $path")
        } else {
            request(spongeUri, 0).bodyStream()
                .use { download(it, path) }
        }
    }

    private fun getDownloadPath(spongeUri: SpongeUri): Path {
        return spongeServiceConfig.outputDirectory.resolve(spongeUri.host)
            .resolve(FilenameUtils.getPath(spongeUri.path))
            .resolve(FilenameUtils.getName(spongeUri.path))
            .toAbsolutePath()
    }
}
