/**
 * Copyright © 2019-2023 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import com.github.michaelbull.retry.ContinueRetrying
import com.github.michaelbull.retry.StopRetrying
import com.github.michaelbull.retry.policy.RetryPolicy
import com.github.michaelbull.retry.policy.constantDelay
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import kotlinx.coroutines.runBlocking
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

private val retryPolicy: RetryPolicy<Throwable> = {
    if (reason is IOException) {
        ContinueRetrying
    } else {
        StopRetrying
    }
}

private val policy = retryPolicy + limitAttempts(MAX_RETRIES) + constantDelay(RETRY_DELAY_MS)

private fun download(inputStream: InputStream, path: Path): SpongeDownload {
    return CountingInputStream(inputStream).use {
        Files.createDirectories(path.parent)

        val duration = measureNanoTime {
            Files.copy(it, path, StandardCopyOption.REPLACE_EXISTING)
        }

        SpongeDownload(it.byteCount, duration)
    }
}

class SpongeService(private val spongeServiceConfig: SpongeServiceConfig) {
    fun request(spongeURI: SpongeURI, timeout: Int = DEFAULT_REQUEST_TIMEOUT): Connection.Response {
        val connection = Jsoup.connect(spongeURI.uri)
            .timeout(timeout)
            .header(HttpHeaders.ACCEPT_ENCODING, ENCODING)
            .referrer(spongeServiceConfig.referrer)
            .userAgent(spongeServiceConfig.userAgent)
            .maxBodySize(0)
            .ignoreHttpErrors(true)
            .ignoreContentType(true)
            .followRedirects(true)

        return runBlocking {
            retry(policy) { connection.execute() }
        }
    }

    fun download(spongeURI: SpongeURI, path: Path): SpongeDownload {
        return request(spongeURI).bodyStream()
            .use { download(it, path) }
    }
}
