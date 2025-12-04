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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.text.Html
import android.text.InputFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.WebView
import android.widget.ScrollView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.snackbar.Snackbar
import digital.vasic.opoc.wrapper.GsCallback
import java.lang.ref.WeakReference
import java.util.Calendar
import java.util.Locale

/**
 * UI utilities for Android applications.
 *
 * Provides functionality for:
 * - Theme and color management
 * - Keyboard operations
 * - Toasts and Snackbars
 * - Dialog operations
 * - Menu operations
 * - View animations
 * - Screen and display settings
 * - Clipboard operations
 * - Vibration/haptic feedback
 * - Input filters
 * - WebView operations
 * - Miscellaneous UI helpers
 *
 * Extracted from GsContextUtils as part of modularization.
 * Depends on: GsResourceUtils, GsImageUtils
 */
@Suppress("unused", "MemberVisibilityCanBePrivate", "DEPRECATION")
object GsUiUtils {

    private const val BLINK_ANIMATOR_TAG = -1206813720

    //########################
    //## UI Theme & Colors
    //########################

    /**
     * Get current primary color from theme.
     *
     * @param context Android context
     * @return Primary color
     */
    @JvmStatic
    @ColorInt
    fun getCurrentPrimaryColor(context: Context): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(
            GsResourceUtils.getResId(context, GsResourceUtils.ResType.ATTR, "colorPrimary"),
            typedValue,
            true
        )
        return typedValue.data
    }

    /**
     * Get current primary dark color from theme.
     *
     * @param context Android context
     * @return Primary dark color
     */
    @JvmStatic
    @ColorInt
    fun getCurrentPrimaryDarkColor(context: Context): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(
            GsResourceUtils.getResId(context, GsResourceUtils.ResType.ATTR, "colorPrimaryDark"),
            typedValue,
            true
        )
        return typedValue.data
    }

    /**
     * Get current accent color from theme.
     *
     * @param context Android context
     * @return Accent color
     */
    @JvmStatic
    @ColorInt
    fun getCurrentAccentColor(context: Context): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(
            GsResourceUtils.getResId(context, GsResourceUtils.ResType.ATTR, "colorAccent"),
            typedValue,
            true
        )
        return typedValue.data
    }

    /**
     * Get activity background color.
     *
     * @param activity Activity context
     * @return Background color
     */
    @JvmStatic
    @ColorInt
    fun getActivityBackgroundColor(activity: Activity): Int {
        val array = activity.theme.obtainStyledAttributes(
            intArrayOf(android.R.attr.colorBackground)
        )
        val c = array.getColor(0, 0xFF0000)
        array.recycle()
        return c
    }

    /**
     * Get list divider color (adaptive to theme).
     *
     * @param activity Activity context
     * @return Divider color
     */
    @JvmStatic
    @ColorInt
    fun getListDividerColor(activity: Activity?): Int {
        val forBlackBg = "#d1d1d1"
        val forWhiteBg = "#3d3d3d"
        var isWhiteBg = true
        try {
            isWhiteBg = GsResourceUtils.shouldColorOnTopBeLight(getActivityBackgroundColor(activity!!))
        } catch (ignored: Exception) {
        }
        return Color.parseColor(if (isWhiteBg) forWhiteBg else forBlackBg)
    }

    /**
     * Set activity background color.
     *
     * @param activity Activity context
     * @param color Color to set (nullable)
     */
    @JvmStatic
    fun setActivityBackgroundColor(activity: Activity, @ColorInt color: Int?) {
        if (color != null) {
            try {
                val contentView = activity.findViewById<View>(android.R.id.content)
                (contentView as? android.view.ViewGroup)?.getChildAt(0)?.setBackgroundColor(color)
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * Set activity navigation bar background color (API 21+).
     *
     * @param context Activity context
     * @param color Color to set (nullable)
     */
    @JvmStatic
    fun setActivityNavigationBarBackgroundColor(context: Activity?, @ColorInt color: Int?) {
        if (context != null && color != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val window = context.window
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    window.navigationBarColor = color
                }
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * Set status bar color (API 21+).
     *
     * @param context Activity context
     * @param color Color value (or resource id if fromRes is true)
     * @param fromRes Whether color is a resource id
     */
    @JvmStatic
    fun setStatusbarColor(context: Activity, color: Int, vararg fromRes: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val actualColor = if (fromRes.isNotEmpty() && fromRes[0]) {
                ContextCompat.getColor(context, color)
            } else {
                color
            }
            context.window.statusBarColor = actualColor
        }
    }

    /**
     * Apply day-night theme.
     *
     * @param pref Theme preference: "light", "dark", "system", "auto", "autocompat"
     */
    @JvmStatic
    @SuppressLint("WrongConstant")
    fun applyDayNightTheme(pref: String) {
        val prefLight = pref.contains("light") || ("autocompat" == pref && isCurrentHourOfDayBetween(9, 17))
        val prefDark = pref.contains("dark") || ("autocompat" == pref && !isCurrentHourOfDayBetween(9, 17))

        when {
            prefLight -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            prefDark -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "system" == pref -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "auto" == pref -> @Suppress("DEPRECATION") AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO)
        }
    }

    /**
     * Check if current hour is between begin and end.
     * Useful for time-based light/dark mode.
     *
     * @param begin Begin hour (0-23)
     * @param end End hour (0-23)
     * @return True if current hour is in range
     */
    @JvmStatic
    fun isCurrentHourOfDayBetween(begin: Int, end: Int): Boolean {
        val safeBegin = if (begin >= 23 || begin < 0) 0 else begin
        val safeEnd = if (end >= 23 || end < 0) 0 else end
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return h in safeBegin..safeEnd
    }

    /**
     * Check if dark mode is currently enabled.
     *
     * @param context Android context
     * @return True if dark theme/mode is enabled
     */
    @JvmStatic
    fun isDarkModeEnabled(context: Context): Boolean {
        return when (val state = AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> {
                when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                    Configuration.UI_MODE_NIGHT_YES -> true
                    Configuration.UI_MODE_NIGHT_NO -> false
                    else -> false
                }
            }
        }
    }

    //########################
    //## Keyboard Operations
    //########################

    /**
     * Show or hide soft keyboard.
     *
     * @param activity Activity context
     * @param show Whether to show or hide
     * @param view Optional view to focus (uses current focus if not provided)
     */
    @JvmStatic
    fun showSoftKeyboard(activity: Activity?, show: Boolean, vararg view: View) {
        if (activity == null) return

        val win = activity.window ?: return
        var focus = if (view.isNotEmpty()) view[0] else activity.currentFocus
        if (focus == null) {
            focus = win.decorView
        }

        if (focus != null) {
            val ctrl = WindowInsetsControllerCompat(win, focus)
            if (show) {
                focus.requestFocus()
                ctrl.show(WindowInsetsCompat.Type.ime())
            } else {
                focus.clearFocus()
                ctrl.hide(WindowInsetsCompat.Type.ime())
            }
        }
    }

    //########################
    //## Toast & Snackbar
    //########################

    /**
     * Show a Snackbar.
     *
     * @param context Activity context
     * @param stringResId String resource id
     * @param showLong Whether to show for long duration
     * @return Created Snackbar
     */
    @JvmStatic
    fun showSnackBar(context: Activity, @StringRes stringResId: Int, showLong: Boolean): Snackbar {
        val s = Snackbar.make(
            context.findViewById(android.R.id.content),
            stringResId,
            if (showLong) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
        )
        s.show()
        return s
    }

    /**
     * Show a Snackbar with action.
     *
     * @param context Activity context
     * @param stringResId Message string resource id
     * @param showLong Whether to show for long duration
     * @param actionResId Action button text resource id
     * @param listener Action button click listener
     */
    @JvmStatic
    fun showSnackBar(
        context: Activity,
        @StringRes stringResId: Int,
        showLong: Boolean,
        @StringRes actionResId: Int,
        listener: View.OnClickListener
    ) {
        Snackbar.make(
            context.findViewById(android.R.id.content),
            stringResId,
            if (showLong) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
        )
            .setAction(actionResId, listener)
            .show()
    }

    //########################
    //## Dialog Operations
    //########################

    /**
     * Show dialog with HTML text view.
     *
     * @param context Activity context
     * @param resTitleId Title resource id
     * @param html HTML content
     */
    @JvmStatic
    fun showDialogWithHtmlTextView(context: Activity, @StringRes resTitleId: Int, html: String) {
        showDialogWithHtmlTextView(context, resTitleId, html, true, null)
    }

    /**
     * Show dialog with HTML or plain text view.
     *
     * @param context Activity context
     * @param resTitleId Title resource id (0 for no title)
     * @param text Content text
     * @param isHtml Whether content is HTML
     * @param dismissedListener Listener for dialog dismiss event
     */
    @JvmStatic
    fun showDialogWithHtmlTextView(
        context: Activity,
        @StringRes resTitleId: Int,
        text: String,
        isHtml: Boolean,
        dismissedListener: DialogInterface.OnDismissListener?
    ) {
        val scroll = ScrollView(context)
        val textView = AppCompatTextView(context)
        val padding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 16f,
            context.resources.displayMetrics
        ).toInt()

        scroll.setPadding(padding, 0, padding, 0)
        scroll.addView(textView)
        textView.movementMethod = LinkMovementMethod()
        textView.text = if (isHtml) SpannableString(Html.fromHtml(text)) else text
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f)

        val dialog = AlertDialog.Builder(context)
            .setPositiveButton(android.R.string.ok, null)
            .setOnDismissListener(dismissedListener)
            .setView(scroll)

        if (resTitleId != 0) {
            dialog.setTitle(resTitleId)
        }
        dialogFullWidth(dialog.show(), true, false)
    }

    /**
     * Show dialog with raw file in WebView.
     *
     * @param context Activity context
     * @param fileInRaw Raw resource file name
     * @param resTitleId Title resource id
     */
    @JvmStatic
    fun showDialogWithRawFileInWebView(context: Activity, fileInRaw: String, @StringRes resTitleId: Int) {
        val wv = WebView(context)
        wv.loadUrl("file:///android_res/raw/$fileInRaw")
        val dialog = AlertDialog.Builder(context)
            .setPositiveButton(android.R.string.ok, null)
            .setTitle(resTitleId)
            .setView(wv)
        dialogFullWidth(dialog.show(), true, false)
    }

    /**
     * Configure dialog to be full width and/or show keyboard.
     *
     * @param dialog Dialog (get via dialog.show())
     * @param fullWidth Whether to make dialog full width
     * @param showKeyboard Whether to show keyboard
     */
    @JvmStatic
    fun dialogFullWidth(dialog: AlertDialog?, fullWidth: Boolean, showKeyboard: Boolean) {
        try {
            val w = dialog?.window
            if (w != null) {
                if (fullWidth) {
                    w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
                }
                if (showKeyboard) {
                    w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
            }
        } catch (ignored: Exception) {
        }
    }

    //########################
    //## Menu Operations
    //########################

    /**
     * Try to tint all menu items with given color.
     *
     * @param menu Menu to tint
     * @param recurse Whether to recurse into submenus
     * @param iconColor Color to tint with
     */
    @JvmStatic
    fun tintMenuItems(menu: Menu?, recurse: Boolean, @ColorInt iconColor: Int) {
        if (menu == null) return

        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            try {
                GsImageUtils.tintDrawable(item.icon, iconColor)
                if (item.hasSubMenu() && recurse) {
                    tintMenuItems(item.subMenu, recurse, iconColor)
                }
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * Try to make icons in Toolbar/ActionBar SubMenus visible.
     * This may not work on some devices or future Android updates.
     *
     * @param menu Menu to modify
     * @param visible Whether icons should be visible
     */
    @JvmStatic
    fun setSubMenuIconsVisibility(menu: Menu, visible: Boolean) {
        if (androidx.core.text.TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL) {
            return
        }
        if (menu.javaClass.simpleName == "MenuBuilder") {
            try {
                @SuppressLint("PrivateApi")
                val m = menu.javaClass.getDeclaredMethod("setOptionalIconsVisible", Boolean::class.javaPrimitiveType)
                m.isAccessible = true
                m.invoke(menu, visible)
            } catch (ignored: Exception) {
                android.util.Log.d(
                    "GsUiUtils",
                    "Error: 'setSubMenuIconsVisibility' not supported on this device"
                )
            }
        }
    }

    //########################
    //## View Operations
    //########################

    /**
     * A simple Runnable which does a touch event on a view.
     * This pops up e.g. the keyboard on an EditText.
     *
     * Example: Handler().postDelayed(DoTouchView(editView), 200)
     */
    class DoTouchView(private var view: View?) : Runnable {
        override fun run() {
            view?.let { v ->
                v.dispatchTouchEvent(
                    MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_DOWN, 0f, 0f, 0
                    )
                )
                v.dispatchTouchEvent(
                    MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_UP, 0f, 0f, 0
                    )
                )
                view = null
            }
        }
    }

    //########################
    //## Animation
    //########################

    /**
     * Blink view (fade out and in).
     *
     * @param view View to blink
     */
    @JvmStatic
    fun blinkView(view: View?) {
        if (view == null) return

        val animator = ObjectAnimator
            .ofFloat(view, View.ALPHA, 0.2f, 1.0f)
            .setDuration(500L)

        view.setTag(BLINK_ANIMATOR_TAG, WeakReference(animator))

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.alpha = 1.0f
                view.setTag(BLINK_ANIMATOR_TAG, null)
            }
        })

        animator.start()
    }

    /**
     * Stop blinking view.
     *
     * @param view View to stop blinking
     */
    @JvmStatic
    fun stopBlinking(view: View?) {
        if (view == null) return

        val tagRef = view.getTag(BLINK_ANIMATOR_TAG)
        if (tagRef is WeakReference<*>) {
            val tag = tagRef.get()
            if (tag is ObjectAnimator && tag.isRunning) {
                tag.cancel()
            }
        }
    }

    /**
     * Fade one view in and another out.
     *
     * @param inView View to fade in
     * @param outView View to fade out
     * @param animate Whether to animate the transition
     * @return True if state changed, false if already in correct state
     */
    @JvmStatic
    fun fadeInOut(inView: View, outView: View, animate: Boolean): Boolean {
        // Do nothing if we are already in the correct state
        if (inView.visibility == View.VISIBLE && outView.visibility == View.INVISIBLE) {
            return false
        }

        if (animate) {
            outView.animate()
                .alpha(0f)
                .setDuration(400)
                .withEndAction { outView.visibility = View.INVISIBLE }

            inView.alpha = 0f
            inView.visibility = View.VISIBLE
            inView.animate()
                .alpha(1f)
                .setDuration(400)
        } else {
            outView.visibility = View.INVISIBLE
            inView.visibility = View.VISIBLE
        }

        return true
    }

    //########################
    //## Screen & Display
    //########################

    /**
     * Toggle status bar visibility.
     * If no parameter, toggles current state.
     *
     * @param context Activity context
     * @param optionalForceVisible Force visible state
     */
    @JvmStatic
    fun toggleStatusbarVisibility(context: Activity, vararg optionalForceVisible: Boolean) {
        val attrs = context.window.attributes
        val flag = WindowManager.LayoutParams.FLAG_FULLSCREEN

        when {
            optionalForceVisible.isEmpty() -> {
                attrs.flags = attrs.flags xor flag
            }
            optionalForceVisible.size == 1 && optionalForceVisible[0] -> {
                attrs.flags = attrs.flags and flag.inv()
            }
            else -> {
                attrs.flags = attrs.flags or flag
            }
        }
        context.window.attributes = attrs
    }

    /**
     * Detect if activity is in splitscreen/multiwindow mode.
     *
     * @param activity Activity context
     * @return True if in split screen mode
     */
    @JvmStatic
    fun isInSplitScreenMode(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.isInMultiWindowMode
        } else {
            false
        }
    }

    /**
     * Set keep screen on flag.
     *
     * @param activity Activity context
     * @param keepOn Whether to keep screen on
     */
    @JvmStatic
    fun setKeepScreenOn(activity: Activity, keepOn: Boolean) {
        if (keepOn) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * Set window aspect ratio based on orientation.
     *
     * @param window Window to modify
     * @param displayMetrics Display metrics
     * @param portraitWidthRatio Width ratio in portrait (0.0-1.0)
     * @param portraitHeightRatio Height ratio in portrait (0.0-1.0)
     * @param landscapeWidthRatio Width ratio in landscape (0.0-1.0)
     * @param landscapeHeightRatio Height ratio in landscape (0.0-1.0)
     */
    @JvmStatic
    fun windowAspectRatio(
        window: Window?,
        displayMetrics: DisplayMetrics,
        portraitWidthRatio: Float,
        portraitHeightRatio: Float,
        landscapeWidthRatio: Float,
        landscapeHeightRatio: Float
    ) {
        if (window == null) return

        val params = window.attributes
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        if (width < height) { // Portrait
            params.width = (width * portraitWidthRatio).toInt()
            params.height = (height * portraitHeightRatio).toInt()
        } else { // Landscape
            params.width = (width * landscapeWidthRatio).toInt()
            params.height = (height * landscapeHeightRatio).toInt()
        }
        window.attributes = params
    }

    /**
     * Check if device is in portrait orientation.
     *
     * @param context Android context
     * @return True if in portrait orientation
     */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun isDeviceOrientationPortrait(context: Context): Boolean {
        val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.orientation
        return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
    }

    /**
     * Cycle through screen rotation settings (landscape -> portrait -> sensor -> default).
     * Shows a toast with the new setting.
     *
     * @param context Activity context
     */
    @JvmStatic
    @SuppressLint("SwitchIntDef")
    fun nextScreenRotationSetting(context: Activity) {
        val (nextOrientation, textKey) = when (context.requestedOrientation) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT to "Portrait"
            }
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR to "Sensor"
            }
            ActivityInfo.SCREEN_ORIENTATION_SENSOR -> {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED to "Default"
            }
            else -> {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE to "Landscape"
            }
        }

        val resId = GsResourceUtils.getResId(context, GsResourceUtils.ResType.STRING, textKey)
        val text = if (resId != 0) context.getString(resId) else textKey
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        context.requestedOrientation = nextOrientation
    }

    //########################
    //## Clipboard
    //########################

    /**
     * Replace (primary) clipboard contents with given text.
     *
     * @param context Android context
     * @param text Text to set
     * @return True if successful
     */
    @JvmStatic
    fun setClipboard(context: Context, text: CharSequence): Boolean {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(context.packageName, text)
            cm.setPrimaryClip(clip)
            true
        } catch (ignored: Exception) {
            false
        }
    }

    /**
     * Get clipboard contents (failsafe).
     *
     * @param context Android context
     * @return List of clipboard text items
     */
    @JvmStatic
    fun getClipboard(context: Context): List<String> {
        val clipper = mutableListOf<String>()
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

        if (cm != null && cm.hasPrimaryClip()) {
            val data = cm.primaryClip
            if (data != null) {
                for (i in 0 until data.itemCount) {
                    val item = data.getItemAt(i)
                    val text = item.coerceToText(context).toString()
                    if (text.isNotEmpty()) {
                        clipper.add(text)
                    }
                }
            }
        }
        return clipper
    }

    //########################
    //## Vibration
    //########################

    /**
     * Vibrate device one time by given amount of time (default 50ms).
     * Requires <uses-permission android:name="android.permission.VIBRATE" /> in AndroidManifest.
     *
     * @param context Android context
     * @param ms Duration in milliseconds
     */
    @JvmStatic
    @SuppressLint("MissingPermission")
    fun vibrate(context: Context, vararg ms: Int) {
        val msValue = if (ms.isNotEmpty()) ms[0] else 50
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        if (vibrator == null) {
            return
        } else if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(msValue.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(msValue.toLong())
        }
    }

    //########################
    //## Input Filters
    //########################

    /**
     * Create an InputFilter for filenames (removes disallowed characters).
     *
     * @return InputFilter for filename validation
     */
    @JvmStatic
    fun makeFilenameInputFilter(): InputFilter {
        return InputFilter { filterSrc, filterStart, filterEnd, _, _, _ ->
            if (filterSrc != null && filterSrc.isNotEmpty()) {
                val newInput = filterSrc.subSequence(filterStart, filterEnd).toString().replace(" ", "")
                val newInputFiltered = GsFileUtils.getFilteredFilenameWithoutDisallowedChars(newInput)
                if (newInput != newInputFiltered) {
                    return@InputFilter ""
                }
            }
            null
        }
    }

    //########################
    //## WebView Operations
    //########################

    /**
     * Print a WebView's contents (can also create PDF).
     *
     * @param webview WebView to print
     * @param jobName Name of the job (affects PDF name)
     * @param landscape Whether to use landscape orientation
     * @return PrintJob or null
     */
    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun print(webview: WebView, jobName: String, vararg landscape: Boolean): PrintJob? {
        val context = webview.context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val printAdapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                webview.createPrintDocumentAdapter(jobName)
            } else {
                @Suppress("DEPRECATION")
                webview.createPrintDocumentAdapter()
            }

            val attrib = PrintAttributes.Builder()
            if (landscape.isNotEmpty() && landscape[0]) {
                attrib.setMediaSize(PrintAttributes.MediaSize("ISO_A4", "android", 11690, 8270))
                attrib.setMinMargins(PrintAttributes.Margins(0, 0, 0, 0))
            }

            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            return try {
                printManager?.print(jobName, printAdapter, attrib.build())
            } catch (ignored: Exception) {
                null
            }
        } else {
            android.util.Log.e("GsUiUtils", "ERROR: Method called on too low Android API version")
        }
        return null
    }

    /**
     * Create PDF from WebView (convenience method for print).
     *
     * @param webview WebView to convert to PDF
     * @param jobName PDF job name
     * @return PrintJob or null
     */
    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun createPdf(webview: WebView, jobName: String): PrintJob? {
        return print(webview, jobName)
    }

    //########################
    //## Miscellaneous UI
    //########################

    /**
     * Remove activity from history (won't show up in recents).
     * Call before finish() / System.exit().
     *
     * @param activity Activity context
     */
    @JvmStatic
    fun removeActivityFromHistory(activity: Context) {
        try {
            val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (am != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val tasks = am.appTasks
                if (tasks != null && tasks.isNotEmpty()) {
                    tasks[0].setExcludeFromRecents(true)
                }
            }
        } catch (ignored: Exception) {
        }
    }
}
