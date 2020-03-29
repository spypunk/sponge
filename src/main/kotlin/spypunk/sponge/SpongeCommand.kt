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
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.types.restrictTo
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import java.util.regex.Pattern
import kotlin.system.exitProcess

private val mimeTypePattern = Pattern.compile("^[-\\w.]+/[-\\w.]+\$")
private val version = ConfigurationProperties
    .fromResource("sponge.properties")[Key("version", stringType)]

class SpongeCommand : CliktCommand(name = "sponge", printHelpOnEmptyArgs = true) {
    private val spongeUri by option("-u", "--uri", help = "URI (example: https://www.google.com)")
        .convert { SpongeUri(it) }
        .required()

    private val outputDirectory by option("-o", "--output", help = "Output directory where files are downloaded")
        .path()
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
        .default(DEFAULT_MAXIMUM_DEPTH)

    private val maximumUris by option("-m", "--max-uris", help = "Maximum uris to visit")
        .int()
        .restrictTo(1)
        .default(DEFAULT_MAXIMUM_URIS)

    private val includeSubdomains by option("-s", "--include-subdomains", help = "Include subdomains")
        .flag()

    private val concurrentRequests by option("-R", "--concurrent-requests", help = "Concurrent requests")
        .int()
        .restrictTo(1)
        .default(DEFAULT_CONCURRENT_REQUESTS)

    private val concurrentDownloads by option("-D", "--concurrent-downloads", help = "Concurrent downloads")
        .int()
        .restrictTo(1)
        .default(DEFAULT_CONCURRENT_DOWNLOADS)

    private val referrer by option("-r", "--referrer", help = "Referrer")
        .default(DEFAULT_REFERRER)

    private val userAgent by option("-U", "--user-agent", help = "User agent")
        .default(DEFAULT_USER_AGENT)

    private val overwriteExistingFiles by option("-O", "--overwrite", help = "Overwrite existing files")
        .flag(default = DEFAULT_OVERWRITE_EXISTING_FILES)

    init {
        versionOption(names = setOf("-v", "--version"), version = version) { it }

        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        if (mimeTypes.isEmpty() && fileExtensions.isEmpty()) {
            throw UsageError("At least one mime type or one file extension is required")
        }

        try {
            val spongeServiceConfig = SpongeServiceConfig(referrer, userAgent)
            val spongeService = SpongeService(spongeServiceConfig)

            val spongeConfig = SpongeConfig(
                spongeUri,
                outputDirectory,
                mimeTypes.toSet(),
                fileExtensions.toSet(),
                maximumDepth,
                maximumUris,
                includeSubdomains,
                concurrentRequests,
                concurrentDownloads,
                overwriteExistingFiles
            )

            Sponge(spongeService, spongeConfig).execute()
        } catch (t: Throwable) {
            System.err.println("Unexpected error encountered: : ${t.rootMessage}")

            exitProcess(1)
        }
    }
}
