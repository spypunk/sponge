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
import org.apache.http.entity.ContentType
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private val htmlMimeTypes = setOf(ContentType.TEXT_HTML.mimeType, ContentType.APPLICATION_XHTML_XML.mimeType)
private val ignoredSpongeUriMetadata = SpongeUriMetadata()

private data class SpongeUriMetadata(val downloadPath: Path? = null, val children: Set<SpongeUri> = setOf())

class Sponge(private val spongeService: SpongeService, private val spongeInput: SpongeInput) {
    private val requestContext = newFixedThreadPoolContext(spongeInput.concurrentRequests, "request")
    private val downloadContext = newFixedThreadPoolContext(spongeInput.concurrentDownloads, "download")
    private val spongeUriMetadatas = ConcurrentHashMap<SpongeUri, SpongeUriMetadata>()
    private val rootHost = spongeInput.spongeUri.toUri().host

    fun execute() = runBlocking { visitSpongeUri(spongeInput.spongeUri) }

    private suspend fun visitSpongeUri(spongeUri: SpongeUri, parents: Set<SpongeUri> = setOf()) {
        val spongeUriMetadata = spongeUriMetadatas.computeIfAbsent(spongeUri, this::getSpongeUriMetadata)

        if (spongeUriMetadata.downloadPath != null) {
            download(spongeUri, spongeUriMetadata.downloadPath)
        } else if (parents.size < spongeInput.maxDepth) {
            visitSpongeUris(spongeUriMetadata.children, parents + spongeUri)
        }
    }

    private fun getSpongeUriMetadata(spongeUri: SpongeUri): SpongeUriMetadata {
        return try {
            val response = spongeService.request(spongeUri)
            val mimeType = ContentType.parse(response.contentType()).mimeType

            when {
                isDownloadable(spongeUri, mimeType) -> SpongeUriMetadata(downloadPath = getDownloadPath(spongeUri))
                mimeType.isHtmlMimeType() -> SpongeUriMetadata(children = getChildren(spongeUri, response))
                else -> ignoredSpongeUriMetadata
            }
        } catch (e: Exception) {
            System.err.println("⚠ Processing failed for $spongeUri: ${e.javaClass.name} - ${e.message}")

            ignoredSpongeUriMetadata
        }
    }

    private fun getDownloadPath(spongeUri: SpongeUri): Path {
        val uri = spongeUri.toUri()

        return spongeInput.outputDirectory
            .resolve(uri.host)
            .resolve(FilenameUtils.getPath(uri.path))
            .resolve(FilenameUtils.getName(uri.path))
            .toAbsolutePath()
    }

    private suspend fun visitSpongeUris(spongeUris: Set<SpongeUri>, parents: Set<SpongeUri>) {
        spongeUris.asSequence()
            .filterNot(parents::contains)
            .map { GlobalScope.async(requestContext) { visitSpongeUri(it, parents) } }
            .toList()
            .awaitAll()
    }

    private fun getChildren(spongeUri: SpongeUri, response: Connection.Response): Set<SpongeUri> {
        println("↺ $spongeUri")

        val document = Jsoup.parse(response.body(), response.url().toExternalForm())
        val links = getLinks(document) + getImageLinks(document)

        return links.distinct()
            .mapNotNull(String::toSpongeUriOrNull)
            .filterNot(spongeUri::equals)
            .filter(this::isVisitable)
            .toSet()
    }

    private fun getLinks(document: Document) = getAttributeValues(document, "a[href]", "abs:href")

    private fun getImageLinks(document: Document) = getAttributeValues(document, "img[src]", "abs:src")

    private fun getAttributeValues(document: Document, cssQuery: String, attributeKey: String): Sequence<String> {
        return document.select(cssQuery).asSequence()
            .mapNotNull { it.attr(attributeKey) }
            .filterNot(String::isEmpty)
    }

    private fun isVisitable(spongeUri: SpongeUri): Boolean {
        val host = spongeUri.toUri().host

        return host == rootHost ||
            spongeInput.includeSubdomains && host.endsWith(rootHost)
    }

    private fun isDownloadable(spongeUri: SpongeUri, mimeType: String): Boolean {
        val extension = FilenameUtils.getExtension(spongeUri.toUri().path)

        return spongeInput.fileExtensions.contains(extension) || spongeInput.mimeTypes.contains(mimeType)
    }

    private suspend fun download(spongeUri: SpongeUri, path: Path) {
        spongeUriMetadatas[spongeUri] = ignoredSpongeUriMetadata

        withContext(downloadContext) { spongeService.download(spongeUri, path) }
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
