/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.http.entity.ContentType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArraySet

private val htmlMimeTypes = setOf(ContentType.TEXT_HTML.mimeType, ContentType.APPLICATION_XHTML_XML.mimeType)

private fun String.isHtmlMimeType() = htmlMimeTypes.contains(this)

private fun Element.toSpongeUri(attributeKey: String): SpongeUri? {
    return try {
        attr(attributeKey)?.toSpongeUri()
    } catch (ignored: Exception) {
        null
    }
}

fun Throwable.rootMessage(): String = ExceptionUtils.getRootCauseMessage(this)

class Sponge(private val spongeService: SpongeService, private val spongeInput: SpongeInput) {
    private val requestContext = newFixedThreadPoolContext(spongeInput.concurrentRequests, "request")
    private val downloadContext = newFixedThreadPoolContext(spongeInput.concurrentDownloads, "download")
    private val spongeUris = CopyOnWriteArraySet<SpongeUri>()
    private val rootHost = spongeInput.spongeUri.toUri().host

    fun execute() = runBlocking {
        visit(spongeInput.spongeUri, setOf())
    }

    private suspend fun visit(spongeUri: SpongeUri, parents: Set<SpongeUri>) {
        try {
            if (spongeUris.add(spongeUri)) {
                if (spongeUris.size > spongeInput.maximumUris) return

                visit(spongeUri)
            }

            if (parents.size < spongeInput.maximumDepth) {
                visit(spongeUri.children, parents + spongeUri)
            }
        } catch (e: Exception) {
            System.err.println("⚠ Processing failed for $spongeUri: ${e.rootMessage()}")
        }
    }

    private suspend fun visit(spongeUri: SpongeUri) {
        if (isDownloadableByExtension(spongeUri)) {
            download(spongeUri)
        } else {
            val response = spongeService.request(spongeUri)
            val mimeType = ContentType.parse(response.contentType()).mimeType

            if (spongeInput.mimeTypes.contains(mimeType)) {
                download(spongeUri)
            } else if (mimeType.isHtmlMimeType()) {
                val document = Jsoup.parse(response.body(), response.url().toExternalForm())

                spongeUri.children = getChildren(document, spongeUri)
            }
        }
    }

    private suspend fun download(spongeUri: SpongeUri) {
        val path = getDownloadPath(spongeUri)

        if (Files.exists(path)) {
            println("∃ $path")
        } else {
            withContext(downloadContext) { spongeService.download(spongeUri, path) }
        }
    }

    private suspend fun visit(spongeUris: Set<SpongeUri>, parents: Set<SpongeUri>) {
        spongeUris.minus(parents)
            .map { GlobalScope.async(requestContext) { visit(it, parents) } }
            .awaitAll()
    }

    private fun getChildren(document: Document, parent: SpongeUri) =
        (getHrefChildren(document, parent) + getImgChildren(document, parent)).toSet()

    private fun getHrefChildren(document: Document, parent: SpongeUri) =
        getChildren(document, parent, "a[href]", "abs:href")

    private fun getImgChildren(document: Document, parent: SpongeUri) =
        getChildren(document, parent, "img[src]", "abs:src")

    private fun getChildren(
        document: Document,
        parent: SpongeUri,
        cssQuery: String,
        attributeKey: String
    ): Sequence<SpongeUri> {
        return document.select(cssQuery)
            .asSequence()
            .mapNotNull { it.toSpongeUri(attributeKey) }
            .distinct()
            .filter { isVisitable(it, parent) }
    }

    private fun isVisitable(spongeUri: SpongeUri, parent: SpongeUri) =
        spongeUri != parent && isHostVisitable(spongeUri.toUri().host)

    private fun isHostVisitable(host: String): Boolean {
        return host == rootHost ||
            spongeInput.includeSubdomains && host.endsWith(rootHost)
    }

    private fun isDownloadableByExtension(spongeUri: SpongeUri): Boolean {
        val extension = FilenameUtils.getExtension(spongeUri.toUri().path)

        return spongeInput.fileExtensions.contains(extension)
    }

    private fun getDownloadPath(spongeUri: SpongeUri): Path {
        val uri = spongeUri.toUri()

        return spongeInput.outputDirectory
            .resolve(uri.host)
            .resolve(FilenameUtils.getPath(uri.path))
            .resolve(FilenameUtils.getName(uri.path))
            .toAbsolutePath()
    }
}
