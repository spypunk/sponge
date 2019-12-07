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
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import java.io.File
import java.net.URI

class SpongeCommand : CliktCommand(name = "sponge") {
    private val uri: URI by option("-u", "--uri", help = "URI (example: https://www.google.com)")
            .convert { URI(it) }
            .required()

    private val outputDirectory: File by option("-o", "--output", help = "Output directory where files are downloaded")
            .file()
            .required()

    private val mimeTypes: List<String> by option("-t", "--mime-type", help = "Mime types to download")
            .multiple()
            .validate {
                require(it.isNotEmpty()) { "At least one mime type is required" }
            }

    private val depth: Int by option("-d", "--depth", help = "Search depth")
            .int()
            .restrictTo(1)
            .default(1)

    override fun run() = Sponge(uri, outputDirectory, mimeTypes.toSet(), depth).execute()
}
