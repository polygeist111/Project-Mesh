package com.greybox.projectmesh.messaging.data.entities

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

//Use this to encode files not just images
//Needs to be tested sometime
class FileEncoder {//Made by Craig. Encodes via base64.
    @OptIn(ExperimentalEncodingApi::class)
    fun encodebase64(ctxt: Context, inputuri: Uri): String?{
        val encodedstrm: InputStream?=ctxt.contentResolver.openInputStream(inputuri)
        val bytes = encodedstrm?.readBytes()
        encodedstrm?.close()
        return if(bytes!=null){
            Base64.encode(bytes)
        } else {
            "Cannot encode file"
        }
    }
    @OptIn(ExperimentalEncodingApi::class)//Made by Craig
    fun decodeBase64(inputbase64:String, output: File): File{//Decodes to a file. Uses base64
        val decodedfilebytes = Base64.decode(inputbase64)
        val decodedstrm = FileOutputStream(output)
        decodedstrm.write(decodedfilebytes)
        decodedstrm.close()
        return output
    }
}