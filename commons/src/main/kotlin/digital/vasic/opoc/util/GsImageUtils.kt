/*#######################################################
 *
 * SPDX-FileCopyrightText: 2016-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2016-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * Migrated to Kotlin 2025 by Milos Vasic
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 #########################################################*/
package digital.vasic.opoc.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.util.Base64
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import digital.vasic.opoc.wrapper.GsCallback
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

/**
 * Image and bitmap utilities for Android applications.
 *
 * Provides functionality for:
 * - Drawable to Bitmap conversion
 * - Bitmap loading and scaling
 * - Image file operations
 * - Drawable tinting
 * - Text on images
 * - WebView to Bitmap conversion
 * - Base64 encoding
 * - Image sharing
 *
 * Extracted from GsContextUtils as part of modularization.
 * Depends on: GsResourceUtils, GsStorageUtils
 */
@Suppress("unused", "MemberVisibilityCanBePrivate", "DEPRECATION")
object GsImageUtils {

    //########################
    //## Drawable/Bitmap Conversion
    //########################

    /**
     * Load an image into an [ImageView] and apply a color filter.
     *
     * @param imageView ImageView to set image on
     * @param drawableResId Drawable resource id
     * @param colorResId Color resource id for filter
     */
    @JvmStatic
    fun setDrawableWithColorToImageView(
        imageView: ImageView,
        @DrawableRes drawableResId: Int,
        @ColorRes colorResId: Int
    ) {
        imageView.setImageResource(drawableResId)
        imageView.setColorFilter(ContextCompat.getColor(imageView.context, colorResId))
    }

    /**
     * Get a [Bitmap] out of a [Drawable].
     * Handles VectorDrawable, AdaptiveIconDrawable, and BitmapDrawable.
     *
     * @param drawable Drawable to convert
     * @return Bitmap or null if conversion failed
     */
    @JvmStatic
    fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) return null

        var bitmap: Bitmap? = null
        when {
            drawable is VectorDrawableCompat ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && drawable is VectorDrawable) ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) -> {

                val wrappedDrawable = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    DrawableCompat.wrap(drawable).mutate()
                } else {
                    drawable
                }

                bitmap = Bitmap.createBitmap(
                    wrappedDrawable.intrinsicWidth,
                    wrappedDrawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                wrappedDrawable.setBounds(0, 0, canvas.width, canvas.height)
                wrappedDrawable.draw(canvas)
            }
            drawable is BitmapDrawable -> {
                bitmap = drawable.bitmap
            }
        }
        return bitmap
    }

    /**
     * Get a [Bitmap] out of a [DrawableRes].
     *
     * @param context Android context
     * @param drawableId Drawable resource id
     * @return Bitmap or null if conversion failed
     */
    @JvmStatic
    fun drawableToBitmap(context: Context, @DrawableRes drawableId: Int): Bitmap? {
        return try {
            drawableToBitmap(ContextCompat.getDrawable(context, drawableId))
        } catch (e: Exception) {
            null
        }
    }

    //########################
    //## Bitmap Loading from Filesystem
    //########################

    /**
     * Get a [Bitmap] from a given imagePath on the filesystem.
     * Specifying a maxDimen is recommended (< 2000) to avoid OutOfMemoryError.
     *
     * @param imagePath File path to image
     * @param maxDimen Max dimension (width or height) for the bitmap
     * @return Loaded bitmap
     */
    @JvmStatic
    fun loadImageFromFilesystem(imagePath: File, maxDimen: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath.absolutePath, options)
        options.inSampleSize = calculateInSampleSize(options, maxDimen)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(imagePath.absolutePath, options)
    }

    /**
     * Calculate the scaling factor so the bitmap is maximal as big as the maxDimen.
     *
     * @param options Bitmap options that contain the current dimensions
     * @param maxDimen Max size of the Bitmap (width or height)
     * @return The scaling factor that needs to be applied to the bitmap
     */
    @JvmStatic
    fun calculateInSampleSize(options: BitmapFactory.Options, maxDimen: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (Math.max(height, width) > maxDimen) {
            inSampleSize = Math.round(1f * Math.max(height, width) / maxDimen)
        }
        return inSampleSize
    }

    //########################
    //## Bitmap Scaling
    //########################

    /**
     * Scale the bitmap so both dimensions are lower or equal to maxDimen.
     * This keeps the aspect ratio.
     *
     * @param bitmap Bitmap to scale
     * @param maxDimen Maximum dimension
     * @return Scaled bitmap
     */
    @JvmStatic
    fun scaleBitmap(bitmap: Bitmap, maxDimen: Int): Bitmap {
        val picSize = Math.min(bitmap.height, bitmap.width)
        val scale = 1f * maxDimen / picSize
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    //########################
    //## Image Writing to File
    //########################

    /**
     * Write the given [Bitmap] to filesystem.
     *
     * @param targetFile The file to be written to
     * @param image Android Bitmap
     * @param okCallback Callback with success status
     * @param a0quality Quality (0-100), defaults to 70
     */
    @JvmStatic
    fun writeImageToFile(
        targetFile: File,
        image: Bitmap,
        okCallback: GsCallback.a1<Boolean>?,
        vararg a0quality: Int
    ) {
        val quality = if (a0quality.isNotEmpty() && a0quality[0] in 0..100) {
            a0quality[0]
        } else {
            70
        }

        val lc = targetFile.absolutePath.lowercase(Locale.ROOT)
        val format = when {
            lc.endsWith(".webp") -> Bitmap.CompressFormat.WEBP
            lc.endsWith(".png") -> Bitmap.CompressFormat.PNG
            else -> Bitmap.CompressFormat.JPEG
        }

        GsStorageUtils.writeFile(null, targetFile, false) { isOk, outputStream ->
            val success = isOk && outputStream != null && image.compress(format, quality, outputStream)
            okCallback?.callback(success)
            try {
                image.recycle()
            } catch (ignored: Exception) {
            }
        }
    }

    //########################
    //## Drawing Text on Drawable
    //########################

    /**
     * Draw text in the center of the given [DrawableRes].
     * This may be useful for e.g. badge counts.
     *
     * @param context Android context
     * @param drawableRes Drawable resource id
     * @param text Text to draw
     * @param textSize Text size in sp
     * @return Bitmap with text drawn on it
     */
    @JvmStatic
    fun drawTextOnDrawable(
        context: Context,
        @DrawableRes drawableRes: Int,
        text: String,
        textSize: Int
    ): Bitmap {
        val resources = context.resources
        val scale = resources.displayMetrics.density
        var bitmap = drawableToBitmap(context, drawableRes)!!

        bitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = GsResourceUtils.rgb(61, 61, 61)
        paint.textSize = (textSize * scale)
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE)

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val x = (bitmap.width - bounds.width()) / 2
        val y = (bitmap.height + bounds.height()) / 2
        canvas.drawText(text, x.toFloat(), y.toFloat(), paint)

        return bitmap
    }

    //########################
    //## Drawable Tinting
    //########################

    /**
     * Load [Drawable] by given [DrawableRes] and apply a color.
     *
     * @param context Android context
     * @param drawableRes Drawable resource id
     * @param color Color to tint with
     * @return Tinted drawable
     */
    @JvmStatic
    fun tintDrawable(
        context: Context,
        @DrawableRes drawableRes: Int,
        @ColorInt color: Int
    ): Drawable? {
        return tintDrawable(GsResourceUtils.rdrawable(context, drawableRes), color)
    }

    /**
     * Tint a [Drawable] with given color.
     *
     * @param drawable Drawable to tint (nullable)
     * @param color Color to tint with
     * @return Tinted drawable or null
     */
    @JvmStatic
    fun tintDrawable(drawable: Drawable?, @ColorInt color: Int): Drawable? {
        if (drawable == null) return null

        val wrappedDrawable = DrawableCompat.wrap(drawable)
        DrawableCompat.setTint(wrappedDrawable.mutate(), color)
        return wrappedDrawable
    }

    //########################
    //## WebView to Bitmap
    //########################

    /**
     * Create a picture out of [WebView]'s whole content.
     *
     * @param webView The WebView to get contents from
     * @param a0fullpage If true, capture full page instead of visible area
     * @return A Bitmap or null if failed
     */
    @JvmStatic
    fun getBitmapFromWebView(webView: WebView, vararg a0fullpage: Boolean): Bitmap? {
        return try {
            // Measure WebView's content
            if (a0fullpage.isNotEmpty() && a0fullpage[0]) {
                val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                    View.MeasureSpec.UNSPECIFIED,
                    View.MeasureSpec.UNSPECIFIED
                )
                val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                webView.measure(widthMeasureSpec, heightMeasureSpec)
                webView.layout(0, 0, webView.measuredWidth, webView.measuredHeight)
            }

            // Build drawing cache
            webView.buildDrawingCache()

            // Create bitmap and draw WebView's content on it
            val bitmap = Bitmap.createBitmap(
                webView.measuredWidth,
                webView.measuredHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.drawBitmap(bitmap, 0f, bitmap.height.toFloat(), Paint())

            webView.draw(canvas)
            webView.destroyDrawingCache()

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            null
        }
    }

    //########################
    //## Image to Base64
    //########################

    /**
     * Convert bitmap to Base64 string.
     *
     * @param bitmap Bitmap to convert
     * @param format Compression format (JPEG, PNG, WEBP)
     * @param quality Compression quality (0-100)
     * @return Base64 encoded string
     */
    @JvmStatic
    fun imageToBase64(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(format, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            .replace(Regex("\\s+"), "")
    }

    //########################
    //## Image Sharing
    //########################

    /**
     * Share an image via system share dialog.
     *
     * @param context Android context
     * @param bitmap Image to share
     * @param okCallback Callback with success status
     * @param quality Quality of the exported image (0-100)
     */
    @JvmStatic
    fun shareImage(
        context: Context,
        bitmap: Bitmap?,
        okCallback: GsCallback.a1<Boolean>?,
        vararg quality: Int
    ) {
        try {
            val file = File(context.cacheDir, GsFileUtils.getFilenameWithTimestamp())
            if (bitmap != null) {
                writeImageToFile(file, bitmap, { ok ->
                    if (ok) {
                        GsStorageUtils.shareStream(context, file, GsFileUtils.getMimeType(file))
                    }
                    okCallback?.callback(ok)
                }, *quality)
                return
            }
        } catch (ignored: Exception) {
        }
        okCallback?.callback(false)
    }
}
