package com.matt.guidebeacons.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UppercaseSerializer: KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("UppcercaseSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value.uppercase())
    }

    override fun deserialize(decoder: Decoder): String {
        return decoder.decodeString().uppercase()
    }
}