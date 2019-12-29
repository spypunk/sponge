/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
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
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

private val htmlMimeTypes = setOf(ContentType.TEXT_HTML.mimeType, ContentType.APPLICATION_XHTML_XML.mimeType)

class Sponge(private val spongeService: SpongeService, private val spongeInput: SpongeInput) {
    private val requestContext = newFixedThreadPoolContext(spongeInput.concurrentRequests, "request")
    private val downloadContext = newFixedThreadPoolContext(spongeInput.concurrentDownloads, "download")
    private val spongeUris = CopyOnWriteArraySet<SpongeUri>()
    private val spongeUrisChildren = ConcurrentHashMap<SpongeUri, Set<SpongeUri>>()
    private val rootHost = spongeInput.spongeUri.toUri().host

    fun execute() = runBlocking {
        visit(spongeInput.spongeUri, setOf())
    }

    private suspend fun visit(spongeUri: SpongeUri, parents: Set<SpongeUri>) {
        try {
            if (spongeUris.add(spongeUri)) {
                visit(spongeUri)
            }

            if (parents.size < spongeInput.maxDepth && spongeUrisChildren.containsKey(spongeUri)) {
                visit(spongeUrisChildren.getValue(spongeUri), parents + spongeUri)
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
                val children = getChildren(spongeUri, response)

                if (children.isNotEmpty()) spongeUrisChildren[spongeUri] = children
            }
        }
    }

    private suspend fun download(spongeUri: SpongeUri) {
        withContext(downloadContext) { spongeService.download(spongeUri, getDownloadPath(spongeUri)) }
    }

    private suspend fun visit(spongeUris: Set<SpongeUri>, parents: Set<SpongeUri>) {
        spongeUris.filterNot(parents::contains)
            .map { GlobalScope.async(requestContext) { visit(it, parents) } }
            .awaitAll()
    }

    private fun getChildren(parent: SpongeUri, response: Connection.Response): Set<SpongeUri> {
        return Jsoup.parse(response.body(), response.url().toExternalForm())
            .let { getHrefChildren(it, parent) + getImgChildren(it, parent) }
    }

    private fun getHrefChildren(document: Document, parent: SpongeUri) =
        getChildren(document, parent, "a[href]", "abs:href")

    private fun getImgChildren(document: Document, parent: SpongeUri) =
        getChildren(document, parent, "img[src]", "abs:src")

    private fun getChildren(
        document: Document,
        parent: SpongeUri,
        cssQuery: String,
        attributeKey: String
    ): Set<SpongeUri> {
        return document.select(cssQuery)
            .mapNotNull { it.attr(attributeKey)?.toSpongeUriOrNull() }
            .filter { it != parent && isVisitable(it) }
            .toSet()
    }

    private fun isVisitable(spongeUri: SpongeUri): Boolean {
        return spongeUri.toUri().host
            .let {
                it == rootHost ||
                    spongeInput.includeSubdomains && it.endsWith(rootHost)
            }
    }

    private fun isDownloadableByExtension(spongeUri: SpongeUri): Boolean {
        return FilenameUtils.getExtension(spongeUri.toUri().path)
            .let { spongeInput.fileExtensions.contains(it) }
    }

    private fun getDownloadPath(spongeUri: SpongeUri): Path {
        return spongeUri.toUri()
            .let {
                spongeInput.outputDirectory
                    .resolve(it.host)
                    .resolve(FilenameUtils.getPath(it.path))
                    .resolve(FilenameUtils.getName(it.path))
                    .toAbsolutePath()
            }
    }
}

private fun String.isHtmlMimeType() = htmlMimeTypes.contains(this)

private fun String.toSpongeUriOrNull(): SpongeUri? {
    return try {
        toSpongeUri()
    } catch (ignored: Exception) {
        null
    }
}

fun Throwable.rootMessage(): String = ExceptionUtils.getRootCauseMessage(this)
