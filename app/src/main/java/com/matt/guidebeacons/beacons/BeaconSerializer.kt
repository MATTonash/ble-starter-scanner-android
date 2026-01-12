package com.matt.guidebeacons.beacons

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

// see: https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#handwritten-composite-serializer
object BeaconSerializer: KSerializer<Beacon> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("matt.guidebeacons.beacon") {
        element<String>("name")
        element<Int>("calibrationRSSI")
        element<Double>("x")
        element<Double>("y")
        element<Int>("buzzerSensitivity")
        element<String>("beaconType")
    }

    override fun serialize(encoder: Encoder, value: Beacon) {
        val coords = value.getCoordinates()
        val x = coords[0]
        val y = coords[1]
        
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.toString())
            encodeIntElement(descriptor, 1, value.getCalibrationRSSI())
            encodeDoubleElement(descriptor, 2, x)
            encodeDoubleElement(descriptor, 3, y)
            encodeIntElement(descriptor, 4, value.getBuzzerSensitivity())
            encodeStringElement(descriptor, 5, value.getBeaconType().name)
        }
    }

    override fun deserialize(decoder: Decoder): Beacon {
        var buzzerSensitivity: Int = 0
        var beaconTypeString: String = BeaconType.DEFAULT.name

        val result = decoder.decodeStructure(descriptor) {
            var name: String = "NAME"
            var calibrationRSSI: Int = -1
            var x: Double = 0.0
            var y: Double = 0.0

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> name = decodeStringElement(descriptor, 0)
                    1 -> calibrationRSSI = decodeIntElement(descriptor, 1)
                    2 -> x = decodeDoubleElement(descriptor, 2)
                    3 -> y = decodeDoubleElement(descriptor, 3)
                    4 -> buzzerSensitivity = decodeIntElement(descriptor, 4)
                    5 -> beaconTypeString = decodeStringElement(descriptor, 5)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            Beacon(name, calibrationRSSI, x, y)
        }

        val beaconType = BeaconType.entries.firstOrNull { it.name == beaconTypeString } ?: BeaconType.DEFAULT
        result.setBeaconType(beaconType)
        result.setBuzzerSensitivity(buzzerSensitivity)

        return result
    }
}
