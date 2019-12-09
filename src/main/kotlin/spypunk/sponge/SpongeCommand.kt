/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import java.net.URI
import java.util.regex.Pattern

class SpongeCommand(private val spongeService: SpongeService) : CliktCommand(name = "sponge") {

    private val uri by option("-u", "--uri", help = "URI (example: https://www.google.com)")
            .convert { URI(it) }
            .required()
            .validate {
                require(setOf("http", "https").contains(it.scheme)) { "Unsupported scheme: ${it.scheme}" }
                require(!it.host.isNullOrEmpty()) { "Host name cannot be empty" }
            }

    private val outputDirectory by option("-o", "--output",
            help = "Output directory where files are downloaded")
            .file()
            .required()

    private val mimeTypes by option("-t", "--mime-type", help = "Mime types to download (example: text/plain)")
            .multiple()
            .validate {
                require(it.isNotEmpty()) { "At least one mime type is required" }

                it.forEach { mimeType ->
                    require(mimeTypePattern.matcher(mimeType).matches()) { "$mimeType is not a valid mime type" }
                }
            }

    private val depth by option("-d", "--depth", help = "Search depth (default: 1)")
            .int()
            .restrictTo(1)
            .default(1)

    private val includeSubdomains by option("-s", "--include-subdomains", help = "Include subdomains")
            .flag()

    private val mimeTypePattern = Pattern.compile("^[-\\w.]+/[-\\w.]+\$")

    override fun run() {
        val spongeInput = SpongeInput(uri, outputDirectory, mimeTypes.toSet(), depth, includeSubdomains)

        Sponge(spongeService, spongeInput).execute()
    }
}
