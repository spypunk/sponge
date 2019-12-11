/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import org.apache.commons.io.FilenameUtils
import org.apache.http.entity.ContentType
import org.jsoup.Connection
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files

class Sponge(private val spongeService: SpongeService, private val spongeInput: SpongeInput) {
    private val urisChildren = mutableMapOf<String, Set<String>>()
    private val failedUris = mutableSetOf<String>()

    fun execute() {
        Files.createDirectories(spongeInput.outputDirectory)

        visitUri()

        urisChildren.clear()
        failedUris.clear()
    }

    private fun visitUri(uri: String = spongeInput.uri.toString(), depth: Int = 0) {
        if (failedUris.contains(uri)) {
            return
        }

        try {
            val response = spongeService.connect(uri)
            val mimeType = ContentType.parse(response.contentType()).mimeType

            if (mimeType.isHtmlMimeType()) {
                visitChildren(uri, depth, response)
            } else if (spongeInput.mimeTypes.contains(mimeType)) {
                visitFile(uri, response)
            }
        } catch (e: IOException) {
            System.err.println("⚠ Processing failed for $uri: ${e.message}")

            failedUris.add(uri)
        }
    }

    private fun visitChildren(uri: String, depth: Int, response: Connection.Response) {
        urisChildren.computeIfAbsent(uri) {
            println("﹫ $uri")

            getChildren(response)
        }

        if (depth < spongeInput.maxDepth) {
            urisChildren.getValue(uri)
                    .forEach { visitUri(it, depth + 1) }
        }
    }

    private fun getChildren(response: Connection.Response): Set<String> {
        return response.parse().select("a[href]").asSequence()
                .map { it.attr("abs:href") }
                .filterNot { it.isNullOrEmpty() }
                .map { it.toOptionalUri() }
                .filterNotNull()
                .distinct()
                .filter(this::hasValidHost)
                .map { it.toString() }
                .toSet()
    }

    private fun hasValidHost(uri: URI): Boolean {
        val normalizedHost = uri.normalizedHost() ?: return false

        return normalizedHost == spongeInput.normalizedHost
                || spongeInput.includeSubdomains && normalizedHost.endsWith(spongeInput.normalizedHost)
    }

    private fun visitFile(uri: String, response: Connection.Response) {
        val fileName = FilenameUtils.getName(uri)
        val filePath = spongeInput.outputDirectory.resolve(fileName).toAbsolutePath()

        if (!Files.exists(filePath)) {
            spongeService.download(response, filePath)
        }
    }

    private fun String.isHtmlMimeType() = ContentType.TEXT_HTML.mimeType == this
            || ContentType.APPLICATION_XHTML_XML.mimeType == this

    private fun String.toOptionalUri(): URI? {
        return try {
            toUri()
        } catch (e: URISyntaxException) {
            handleToUriException(e)
        } catch (e: MalformedURLException) {
            handleToUriException(e)
        }
    }

    private fun String.handleToUriException(e: Exception): Nothing? {
        System.err.println("⚠ URI parsing failed for $this: ${e.message}")
        return null
    }
}
