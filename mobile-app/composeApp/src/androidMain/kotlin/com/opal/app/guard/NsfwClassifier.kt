package com.opal.app.guard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Ekran rasmini "open_nsfw" TFLite modeli bilan baholaydi (faqat qurilmada).
 *
 * Model: Yahoo open_nsfw (BSD-2-Clause), 224x224, BGR, mean [104,117,123].
 * Chiqish: [sfw, nsfw] — biz ikkinchisini qaytaramiz.
 *
 * Bitta nusxa, bitta background oqim. Bitmap hech qayerda saqlanmaydi.
 */
internal class NsfwClassifier(context: Context) : AutoCloseable {

    private val interpreter: Interpreter
    private val input: ByteBuffer
    private val output = Array(1) { FloatArray(2) }
    private val pixels = IntArray(SIZE * SIZE)
    private val scaled = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(scaled)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val dst = Rect(0, 0, SIZE, SIZE)

    init {
        val model = context.assets.openFd(MODEL).use { afd ->
            FileInputStream(afd.fileDescriptor).use { fis ->
                fis.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.startOffset,
                    afd.declaredLength
                )
            }
        }
        interpreter = Interpreter(
            model,
            Interpreter.Options().apply { setNumThreads(2) }
        )
        input = ByteBuffer.allocateDirect(SIZE * SIZE * 3 * 4).order(ByteOrder.nativeOrder())
    }

    /** @return 0f..1f — nomaqbul kontent ehtimoli. */
    fun score(bitmap: Bitmap): Float {
        canvas.drawBitmap(bitmap, null, dst, paint)
        scaled.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
        input.rewind()
        var i = 0
        while (i < pixels.size) {
            val c = pixels[i++]
            val r = (c shr 16 and 0xFF) - 123
            val g = (c shr 8 and 0xFF) - 117
            val b = (c and 0xFF) - 104
            input.putFloat(b.toFloat())
            input.putFloat(g.toFloat())
            input.putFloat(r.toFloat())
        }
        interpreter.run(input, output)
        return output[0][1].coerceIn(0f, 1f)
    }

    override fun close() {
        runCatching { interpreter.close() }
        runCatching { scaled.recycle() }
    }

    companion object {
        private const val SIZE = 224
        private const val MODEL = "nsfw.tflite"
    }
}
