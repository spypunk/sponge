/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import java.nio.file.Paths
import java.util.regex.Pattern
import kotlin.system.exitProcess

private const val DEFAULT_REFERRER = "https://www.google.com"
private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
    "Chrome/79.0.3945.88 Safari/537.36"

private val mimeTypePattern = Pattern.compile("^[-\\w.]+/[-\\w.]+\$")
private val version = ConfigurationProperties
    .fromResource("sponge.properties")[Key("version", stringType)]

class SpongeCommand : CliktCommand(name = "sponge", printHelpOnEmptyArgs = true) {
    private val spongeUri by option("-u", "--uri", help = "URI (example: https://www.google.com)")
        .convert { it.toSpongeUri() }
        .required()

    private val outputDirectory by option("-o", "--output", help = "Output directory where files are downloaded")
        .convert { Paths.get(it) }
        .required()

    private val mimeTypes by option("-t", "--mime-type", help = "Mime types to download (example: text/plain)")
        .multiple()
        .validate {
            it.forEach { mimeType ->
                require(mimeTypePattern.matcher(mimeType).matches()) { "$mimeType is not a valid mime type" }
            }
        }

    private val fileExtensions by option("-e", "--file-extension", help = "Extensions to download (example: png)")
        .multiple()

    private val maximumDepth by option("-d", "--depth", help = "Search depth")
        .int()
        .restrictTo(1)
        .default(1)

    private val maximumUris by option("-m", "--max-uris", help = "Maximum uris to process")
        .int()
        .restrictTo(1)
        .default(Int.MAX_VALUE)

    private val includeSubdomains by option("-s", "--include-subdomains", help = "Include subdomains")
        .flag()

    private val concurrentRequests by option("-R", "--concurrent-requests", help = "Concurrent requests")
        .int()
        .restrictTo(1)
        .default(1)

    private val concurrentDownloads by option("-D", "--concurrent-downloads", help = "Concurrent downloads")
        .int()
        .restrictTo(1)
        .default(1)

    private val referrer by option("-r", "--referrer", help = "Referrer")
        .default(DEFAULT_REFERRER)

    private val userAgent by option("-U", "--user-agent", help = "User agent")
        .default(DEFAULT_USER_AGENT)

    init {
        versionOption(names = setOf("-v", "--version"), version = version) { it }

        context {
            helpFormatter = CliktHelpFormatter(showDefaultValues = true)
        }
    }

    override fun run() {
        if (mimeTypes.isEmpty() && fileExtensions.isEmpty()) {
            throw UsageError("At least one mime type or one file extension is required")
        }

        try {
            val spongeInput = SpongeInput(
                spongeUri,
                outputDirectory,
                mimeTypes.toSet(),
                fileExtensions.toSet(),
                maximumDepth,
                maximumUris,
                includeSubdomains,
                concurrentRequests,
                concurrentDownloads)

            val spongeService = SpongeService(referrer, userAgent)

            Sponge(spongeService, spongeInput).execute()
        } catch (t: Throwable) {
            System.err.println("Unexpected error encountered: : ${t.rootMessage()}")

            exitProcess(1)
        }
    }
}
