/*
 * Copyright 2019 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.client

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.core.util.Pair
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.R
import com.hippo.util.ExceptionUtils
import com.hippo.util.IoThreadPoolExecutor
import com.hippo.yorozuya.FileUtils
import com.hippo.yorozuya.IOUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class EhTagDatabase(private val name: String, source: BufferedSource) {
    private val tags: ByteArray

    init {
        var tmp: Array<String?>
        val buffer = StringBuilder()
        source.readInt()
        for (i in source.readUtf8().split("\n".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()) {
            tmp = i.split("\r".toRegex(), limit = 2).toTypedArray()
            buffer.append(tmp[0])
            buffer.append("\r")
            try {
                buffer.append(String(Base64.decode(tmp[1], Base64.DEFAULT), StandardCharsets.UTF_8))
            } catch (e: Exception) {
                buffer.append(tmp[1])
            }
            buffer.append("\n")
        }
        val b = buffer.toString().toByteArray(StandardCharsets.UTF_8)
        val totalBytes = b.size
        tags = ByteArray(totalBytes)
        System.arraycopy(b, 0, tags, 0, totalBytes)
    }

    fun getTranslation(tag: String): String? {
        return search(tags, tag.toByteArray(StandardCharsets.UTF_8))
    }

    private fun search(tags: ByteArray, tag: ByteArray): String? {
        var low = 0
        var high = tags.size
        while (low < high) {
            var start = (low + high) / 2
            // Look for the starting '\n'
            while (start > -1 && tags[start] != '\n'.code.toByte()) {
                start--
            }
            start++

            // Look for the middle '\r'.
            var middle = 1
            while (tags[start + middle] != '\r'.code.toByte()) {
                middle++
            }

            // Look for the ending '\n'
            var end = middle + 1
            while (tags[start + end] != '\n'.code.toByte()) {
                end++
            }
            var compare: Int
            var tagIndex = 0
            var curIndex = start
            while (true) {
                val tagByte = tag[tagIndex].toInt() and 0xff
                val curByte = tags[curIndex].toInt() and 0xff
                compare = tagByte - curByte
                if (compare != 0) {
                    break
                }
                tagIndex++
                curIndex++
                if (tagIndex == tag.size && curIndex == start + middle) {
                    break
                }
                if (tagIndex == tag.size) {
                    compare = -1
                    break
                }
                if (curIndex == start + middle) {
                    compare = 1
                    break
                }
            }
            if (compare < 0) {
                high = start - 1
            } else if (compare > 0) {
                low = start + end + 1
            } else {
                return String(tags, start + middle + 1, end - middle - 1, StandardCharsets.UTF_8)
            }
        }
        return null
    }

    fun suggest(keyword: String): ArrayList<Pair<String, String>> {
        return searchTag(tags, keyword)
    }

    private fun searchTag(tags: ByteArray, keyword: String): ArrayList<Pair<String, String>> {
        val searchHints = ArrayList<Pair<String, String>>()
        var begin = 0
        while (begin < tags.size - 1) {
            var start = begin
            // Look for the starting '\n'
            while (tags[start] != '\n'.code.toByte()) {
                start++
            }

            // Look for the middle '\r'.
            var middle = 1
            while (tags[start + middle] != '\r'.code.toByte()) {
                middle++
            }

            // Look for the ending '\n'
            var end = middle + 1
            while (tags[start + end] != '\n'.code.toByte()) {
                end++
            }
            begin = start + end
            val hintBytes = ByteArray(end - middle - 1)
            System.arraycopy(tags, start + middle + 1, hintBytes, 0, end - middle - 1)
            val hint = String(hintBytes, StandardCharsets.UTF_8)
            val tagBytes = ByteArray(middle)
            System.arraycopy(tags, start + 1, tagBytes, 0, middle)
            val tag = String(tagBytes, StandardCharsets.UTF_8)
            val index = tag.indexOf(':')
            var keywordMatches: Boolean
            keywordMatches = if (index == -1 || index >= tag.length - 1 || keyword.length > 2) {
                containsIgnoreSpace(tag, keyword)
            } else {
                containsIgnoreSpace(tag.substring(index + 1), keyword)
            }
            if (keywordMatches || containsIgnoreSpace(hint, keyword)) {
                val pair = Pair(hint, tag)
                if (!searchHints.contains(pair)) {
                    searchHints.add(pair)
                }
            }
            if (searchHints.size > 20) {
                break
            }
        }
        return searchHints
    }

    private fun containsIgnoreSpace(text: String, key: String): Boolean {
        return text.replace(" ", "").contains(key.replace(" ", ""))
    }

    companion object {
        private val NAMESPACE_TO_PREFIX: MutableMap<String, String> = HashMap()

        // TODO more lock for different language
        private val lock: Lock = ReentrantLock()

        @Volatile
        private var instance: EhTagDatabase? = null

        init {
            NAMESPACE_TO_PREFIX["artist"] = "a:"
            NAMESPACE_TO_PREFIX["cosplayer"] = "cos:"
            NAMESPACE_TO_PREFIX["character"] = "c:"
            NAMESPACE_TO_PREFIX["female"] = "f:"
            NAMESPACE_TO_PREFIX["group"] = "g:"
            NAMESPACE_TO_PREFIX["language"] = "l:"
            NAMESPACE_TO_PREFIX["male"] = "m:"
            NAMESPACE_TO_PREFIX["mixed"] = "x:"
            NAMESPACE_TO_PREFIX["other"] = "o:"
            NAMESPACE_TO_PREFIX["parody"] = "p:"
            NAMESPACE_TO_PREFIX["reclass"] = "r:"
        }

        @JvmStatic
        fun getInstance(context: Context): EhTagDatabase? {
            return if (isPossible(context)) {
                instance
            } else {
                instance = null
                null
            }
        }

        @JvmStatic
        fun namespaceToPrefix(namespace: String): String? {
            return NAMESPACE_TO_PREFIX[namespace]
        }

        private fun getMetadata(context: Context): Array<String>? {
            val metadata = context.resources.getStringArray(R.array.tag_translation_metadata)
            return if (metadata.size == 4) {
                metadata
            } else {
                null
            }
        }

        @JvmStatic
        fun isPossible(context: Context): Boolean {
            return getMetadata(context) != null
        }

        private fun getFileContent(file: File, length: Int): ByteArray? {
            try {
                file.source().buffer().use { source ->
                    val content = ByteArray(length)
                    source.readFully(content)
                    return content
                }
            } catch (e: IOException) {
                return null
            }
        }

        private fun getFileSha1(file: File): ByteArray? {
            try {
                FileInputStream(file).use { `is` ->
                    val digest = MessageDigest.getInstance("SHA-1")
                    var n: Int
                    val buffer = ByteArray(4 * 1024)
                    while (`is`.read(buffer).also { n = it } != -1) {
                        digest.update(buffer, 0, n)
                    }
                    return digest.digest()
                }
            } catch (e: IOException) {
                return null
            } catch (e: NoSuchAlgorithmException) {
                return null
            }
        }

        private fun equals(b1: ByteArray?, b2: ByteArray?): Boolean {
            if (b1 == null && b2 == null) {
                return true
            }
            if (b1 == null || b2 == null) {
                return false
            }
            if (b1.size != b2.size) {
                return false
            }
            for (i in b1.indices) {
                if (b1[i] != b2[i]) {
                    return false
                }
            }
            return true
        }

        private fun checkData(sha1File: File, dataFile: File): Boolean {
            val s1 = getFileContent(sha1File, 20) ?: return false
            val s2 = getFileSha1(dataFile) ?: return false
            return equals(s1, s2)
        }

        private fun save(client: OkHttpClient, url: String, file: File): Boolean {
            val request: Request = Request.Builder().url(url).build()
            val call = client.newCall(request)
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        return false
                    }
                    val body = response.body ?: return false
                    body.byteStream()
                        .use { `is` -> FileOutputStream(file).use { os -> IOUtils.copy(`is`, os) } }
                    return true
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                ExceptionUtils.throwIfFatal(t)
                return false
            }
        }

        @JvmStatic
        fun update(context: Context) {
            val urls = getMetadata(context)
            if (urls == null || urls.size != 4) {
                // Clear tags if it's not possible
                instance = null
                return
            }
            val sha1Name = urls[0]
            val sha1Url = urls[1]
            val dataName = urls[2]
            val dataUrl = urls[3]

            // Clear tags if name if different
            val tmp = instance
            if (tmp != null && tmp.name != dataName) {
                instance = null
            }
            IoThreadPoolExecutor.getInstance().execute {
                if (!lock.tryLock()) {
                    return@execute
                }
                try {
                    val dir = AppConfig.getFilesDir("tag-translations") ?: return@execute

                    // Check current sha1 and current data
                    val sha1File = File(dir, sha1Name)
                    val dataFile = File(dir, dataName)

                    // from assets get current data and sha1
                    if(instance == null && !dataFile.exists()){
                        try{
                            sha1File.writeBytes(context.applicationContext.assets.open("tag-translations-zh-rCN.sha1").readBytes())
                            dataFile.writeBytes(context.applicationContext.assets.open("tag-translations-zh-rCN").readBytes())
                        }catch (e: IOException) {
                            Log.e("EhTagDatabase", "update: ${e.message}", )
                        }
                    }

                    if (!checkData(sha1File, dataFile)) {
                        FileUtils.delete(sha1File)
                        FileUtils.delete(dataFile)
                    }

                    // Read current EhTagDatabase
                    if (instance == null && dataFile.exists()) {
                        try {
                            dataFile.source().buffer()
                                .use { source -> instance = EhTagDatabase(dataName, source) }
                        } catch (e: IOException) {
                            FileUtils.delete(sha1File)
                            FileUtils.delete(dataFile)
                        }
                    }
                    val client = EhApplication.getOkHttpClient(context)

                    // Save new sha1
                    val tempSha1File = File(dir, "$sha1Name.tmp")
                    if (!save(client, sha1Url, tempSha1File)) {
                        FileUtils.delete(tempSha1File)
                        return@execute
                    }

                    // Check new sha1 and current data
                    if (checkData(tempSha1File, dataFile)) {
                        // The data is the same
                        FileUtils.delete(tempSha1File)
                        return@execute
                    }

                    // Save new data
                    val tempDataFile = File(dir, "$dataName.tmp")
                    if (!save(client, dataUrl, tempDataFile)) {
                        FileUtils.delete(tempDataFile)
                        return@execute
                    }

                    // Check new sha1 and new data
                    if (!checkData(tempSha1File, tempDataFile)) {
                        FileUtils.delete(tempSha1File)
                        FileUtils.delete(tempDataFile)
                        return@execute
                    }

                    // Replace current sha1 and current data with new sha1 and new data
                    FileUtils.delete(sha1File)
                    FileUtils.delete(dataFile)
                    tempSha1File.renameTo(sha1File)
                    tempDataFile.renameTo(dataFile)

                    // Read new EhTagDatabase
                    try {
                        dataFile.source().buffer()
                            .use { source -> instance = EhTagDatabase(dataName, source) }
                    } catch (e: IOException) {
                        // Ignore
                    }
                } finally {
                    lock.unlock()
                }
            }
        }
    }
}