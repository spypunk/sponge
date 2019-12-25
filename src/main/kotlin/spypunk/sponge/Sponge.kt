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
    private val spongeUriResponses = ConcurrentHashMap<SpongeUri, SpongeUriResponse>()
    private val processedDownloads = CopyOnWriteArraySet<String>()
    private val rootHost = spongeInput.spongeUri.toUri().host

    fun execute() = runBlocking { visit(spongeInput.spongeUri) }

    private suspend fun visit(spongeUri: SpongeUri, parents: Set<SpongeUri> = setOf()) {
        try {
            val spongeUriResponse = getSpongeUriResponse(spongeUri)

            when {
                spongeUriResponse == IgnoreSpongeUriResponse -> return
                spongeUriResponse == DownloadSpongeUriResponse -> download(spongeUri)
                parents.size < spongeInput.maxDepth -> visit(spongeUriResponse.children, parents + spongeUri)
            }
        } catch (e: Exception) {
            spongeUriResponses[spongeUri] = IgnoreSpongeUriResponse

            System.err.println("⚠ Processing failed for $spongeUri: ${e.rootMessage()}")
        }
    }

    private fun getSpongeUriResponse(spongeUri: SpongeUri): SpongeUriResponse {
        return spongeUriResponses.computeIfAbsent(spongeUri) {
            if (isDownloadableByExtension(spongeUri)) {
                DownloadSpongeUriResponse
            } else {
                val response = spongeService.request(spongeUri)
                val mimeType = ContentType.parse(response.contentType()).mimeType

                getSpongeUriResponse(spongeUri, mimeType, response)
            }
        }
    }

    private fun getSpongeUriResponse(
        spongeUri: SpongeUri, mimeType: String, response: Connection.Response
    ): SpongeUriResponse {
        return when {
            spongeInput.mimeTypes.contains(mimeType) -> DownloadSpongeUriResponse
            mimeType.isHtmlMimeType() -> {
                val children = getChildren(spongeUri, response)

                if (children.isEmpty()) {
                    IgnoreSpongeUriResponse
                } else {
                    VisitSpongeUriResponse(children)
                }
            }
            else -> IgnoreSpongeUriResponse
        }
    }

    private suspend fun visit(spongeUris: Set<SpongeUri>, parents: Set<SpongeUri>) {
        spongeUris.filterNot(parents::contains)
            .map { GlobalScope.async(requestContext) { visit(it, parents) } }
            .awaitAll()
    }

    private fun getChildren(parent: SpongeUri, response: Connection.Response): Set<SpongeUri> {
        val document = Jsoup.parse(response.body(), response.url().toExternalForm())

        return getHrefChildren(document, parent) + getImgChildren(document, parent)
    }

    private fun getHrefChildren(document: Document, parent: SpongeUri) =
        getChildren(document, parent, "a[href]", "abs:href")

    private fun getImgChildren(document: Document, parent: SpongeUri) =
        getChildren(document, parent, "img[src]", "abs:src")

    private fun getChildren(
        document: Document, parent: SpongeUri,
        cssQuery: String, attributeKey: String
    ): Set<SpongeUri> {
        return document.select(cssQuery)
            .mapNotNull { it.attr(attributeKey)?.toSpongeUriOrNull() }
            .filter { it != parent && isVisitable(it) }
            .toSet()
    }

    private fun isVisitable(spongeUri: SpongeUri): Boolean {
        val host = spongeUri.toUri().host

        return host == rootHost ||
            spongeInput.includeSubdomains && host.endsWith(rootHost)
    }

    private fun isDownloadableByExtension(spongeUri: SpongeUri): Boolean {
        val extension = FilenameUtils.getExtension(spongeUri.toUri().path)

        return spongeInput.fileExtensions.contains(extension)
    }

    private suspend fun download(spongeUri: SpongeUri) {
        val path = getDownloadPath(spongeUri)

        if (!processedDownloads.add(path.toString())) return

        withContext(downloadContext) { spongeService.download(spongeUri, path) }
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

private fun String.isHtmlMimeType() = htmlMimeTypes.contains(this)

private fun String.toSpongeUriOrNull(): SpongeUri? {
    return try {
        toSpongeUri()
    } catch (ignored: Exception) {
        null
    }
}

fun Throwable.rootMessage(): String = ExceptionUtils.getRootCauseMessage(this)
