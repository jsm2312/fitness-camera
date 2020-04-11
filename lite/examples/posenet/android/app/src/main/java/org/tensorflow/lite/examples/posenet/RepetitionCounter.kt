package org.tensorflow.lite.examples.posenet

import org.tensorflow.lite.examples.posenet.lib.BodyPart
import org.tensorflow.lite.examples.posenet.lib.Person
import java.util.*

class RepetitionCounter {

    var csv = ""

    fun OnFrame(person: Person) {
        // Write frames to CSV for later analysis in Google Colab
        val now = System.currentTimeMillis()
        val y = person.keyPoints.map { kp -> if (kp.score >= 0.5) kp.position.y.toString() else "" }
        val values = y.joinToString(",")

        csv += "${now},${values}\n"
    }
}