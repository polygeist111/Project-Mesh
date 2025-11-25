package com.greybox.projectmesh.messaging.data.entities

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.db.MeshDatabase
import com.greybox.projectmesh.messaging.data.entities.Message
import com.greybox.projectmesh.messaging.network.MessageNetworkHandler
import com.greybox.projectmesh.extension.updateItem
import com.greybox.projectmesh.testing.TestDeviceService
import com.ustadmobile.meshrabiya.ext.copyToWithProgressCallback
import com.ustadmobile.meshrabiya.util.FileSerializer
import com.ustadmobile.meshrabiya.util.InetAddressSerializer
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.headersContentLength
import java.io.Closeable
import java.net.InetAddress
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicInteger
import com.greybox.projectmesh.extension.getUriNameAndSize
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLDecoder

class FileEncoder {

    @OptIn(ExperimentalEncodingApi::class)
    fun encodebase64(ctxt: Context, inputuri: Uri): String? {
        return try {
            val encodedstrm: InputStream? = ctxt.contentResolver.openInputStream(inputuri)
            val bytes = encodedstrm?.readBytes()
            encodedstrm?.close()
            encodeBytesBase64(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            "Cannot encode file"
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    internal fun encodeBytesBase64(bytes: ByteArray?): String? {
        return if (bytes != null) {
            Base64.encode(bytes)
        } else {
            "Cannot encode file"
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decodeBase64(inputbase64: String, output: File): File {
        val decodedfilebytes = Base64.decode(inputbase64)
        val decodedstrm = FileOutputStream(output)
        decodedstrm.write(decodedfilebytes)
        decodedstrm.close()
        return output
    }

    fun sendImage(imageURI: Uri?, tgtaddress: InetAddress, tgtport: Int, appctxt: Context): Boolean {
        try {
            if (imageURI != null) {
                val fp = encodebase64(appctxt, imageURI)
                if (!fp.equals("Cannot encode file")) {
                    val efp = URLEncoder.encode(fp, "UTF-8")
                    val connection =
                        URL("http://${tgtaddress.hostAddress}:${tgtport}/upload?file=$efp").openConnection() as HttpURLConnection
                    val request = "POST"
                    connection.doOutput = true
                    connection.requestMethod = request
                    connection.setChunkedStreamingMode(0)
                    val instream = appctxt.contentResolver.openInputStream(imageURI)
                    val outstream = connection.outputStream
                    val readingbuffer = ByteArray(1024)
                    var finishedreading: Int
                    while (instream?.read(readingbuffer).also { finishedreading = it!! } != -1) {
                        outstream.write(readingbuffer, 0, finishedreading)
                    }
                    outstream.close()
                    instream?.close()
                } else {
                    return false
                }
            } else {
                return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }
}
