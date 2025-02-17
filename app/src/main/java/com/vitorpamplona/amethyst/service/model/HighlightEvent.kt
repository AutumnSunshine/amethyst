package com.vitorpamplona.amethyst.service.model

import androidx.compose.runtime.Immutable
import com.vitorpamplona.amethyst.model.HexKey
import com.vitorpamplona.amethyst.model.TimeUtils
import com.vitorpamplona.amethyst.model.toHexKey
import nostr.postr.Utils

@Immutable
class HighlightEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: HexKey
) : BaseTextNoteEvent(id, pubKey, createdAt, kind, tags, content, sig) {

    fun inUrl() = taggedUrls().firstOrNull()
    fun author() = taggedUsers().firstOrNull()
    fun quote() = content

    fun inPost() = taggedAddresses().firstOrNull()

    companion object {
        const val kind = 9802

        fun create(
            msg: String,
            privateKey: ByteArray,
            createdAt: Long = TimeUtils.now()
        ): PollNoteEvent {
            val pubKey = Utils.pubkeyCreate(privateKey).toHexKey()
            val tags = mutableListOf<List<String>>()
            val id = generateId(pubKey, createdAt, kind, tags, msg)
            val sig = Utils.sign(id, privateKey)
            return PollNoteEvent(id.toHexKey(), pubKey, createdAt, tags, msg, sig.toHexKey())
        }
    }
}
