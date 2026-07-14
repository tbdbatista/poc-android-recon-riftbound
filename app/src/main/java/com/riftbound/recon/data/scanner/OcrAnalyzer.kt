package com.riftbound.recon.data.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

class OcrAnalyzer(
    private val onLinesDetected: (List<OcrLine>) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val linesList = mutableListOf<OcrLine>()
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val rect = line.boundingBox
                            if (rect != null) {
                                linesList.add(
                                    OcrLine(
                                        text = line.text,
                                        left = rect.left,
                                        top = rect.top,
                                        right = rect.right,
                                        bottom = rect.bottom
                                    )
                                )
                            }
                        }
                    }
                    onLinesDetected(linesList)
                }
                .addOnFailureListener {
                    // Ignore failures in frame processing
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
