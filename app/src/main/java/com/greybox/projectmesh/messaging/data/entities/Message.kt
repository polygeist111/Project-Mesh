package com.greybox.projectmesh.messaging.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI
/*object URISerializable : KSerializer<URI> {//This makes the URI serializable, can be used in JSON
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("URI", PrimitiveKind.STRING)
    override fun serialize(enc: Encoder, vals: URI) {
        enc.encodeString(vals.toString())
    }
    override fun deserialize(dec: Decoder): URI {
        return URI.create(dec.decodeString())
    }
}*/
//@Serializable
@Entity(tableName = "message")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "dateReceived") val dateReceived: Long,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "sender") val sender: String,
    @ColumnInfo(name = "chat") val chat: String,
    //@ColumnInfo(name= "file")  @Serializable(with=URISerializable::class) val file: URI?
)