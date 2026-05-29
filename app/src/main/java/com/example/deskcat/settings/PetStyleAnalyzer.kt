package com.example.deskcat.settings

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object PetStyleAnalyzer {

    private val CAT_KEYWORDS = setOf("cat", "kitten", "feline", "tabby", "persian", "siamese")
    private val DOG_KEYWORDS = setOf("dog", "puppy", "canine", "labrador", "poodle", "husky", "bulldog", "terrier", "retriever")
    private val RABBIT_KEYWORDS = setOf("rabbit", "bunny", "hare")

    suspend fun analyze(bitmap: Bitmap): Pair<PetStyle, String?> {
        val labels = runLabeling(bitmap)
        val topLabel = labels.firstOrNull()?.text
        val style = labels
            .sortedByDescending { it.confidence }
            .firstNotNullOfOrNull { label ->
                val lower = label.text.lowercase()
                when {
                    CAT_KEYWORDS.any { lower.contains(it) } -> PetStyle.Cat
                    DOG_KEYWORDS.any { lower.contains(it) } -> PetStyle.Dog
                    RABBIT_KEYWORDS.any { lower.contains(it) } -> PetStyle.Rabbit
                    else -> null
                }
            } ?: PetStyle.Default

        return style to topLabel
    }

    private suspend fun runLabeling(bitmap: Bitmap): List<com.google.mlkit.vision.label.ImageLabel> =
        suspendCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            val labeler = ImageLabeling.getClient(
                ImageLabelerOptions.Builder().setConfidenceThreshold(0.5f).build()
            )
            labeler.process(image)
                .addOnSuccessListener { labels -> cont.resume(labels) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
