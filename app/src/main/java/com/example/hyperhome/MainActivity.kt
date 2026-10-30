package com.example.hyperhome

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    data class AppItem(val label: String, val icon: Drawable, val intent: Intent)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        val apps = loadApps()
        val root = FrameLayout(this)

        // Home grid: 4 columns like HyperOS
        val grid = GridView(this).apply {
            numColumns = 4
            verticalSpacing = dp(18)
            setPadding(dp(16), dp(56), dp(16), dp(140))
            clipToPadding = false
            selector = ColorDrawable(Color.TRANSPARENT)
            adapter = IconAdapter(apps)
            setOnItemClickListener { _, _, i, _ -> startActivity(apps[i].intent) }
        }
        root.addView(grid, FrameLayout.LayoutParams(-1, -1))

        // Frosted dock with the first 4 apps
        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(30).toFloat()
                setColor(Color.parseColor("#55FFFFFF"))
            }
        }
        apps.take(4).forEach { app ->
            dock.addView(
                roundIcon(app.icon).also { it.setOnClickListener { startActivity(app.intent) } },
                LinearLayout.LayoutParams(0, dp(58), 1f).apply { setMargins(dp(6), 0, dp(6), 0) }
            )
        }
        root.addView(dock, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
            setMargins(dp(16), 0, dp(16), dp(28))
        })

        setContentView(root)

        // Background blur (only on Android 12+ devices that support it)
        if (Build.VERSION.SDK_INT >= 31) {
            window.setBackgroundBlurRadius(40)
        }
    }

    private fun loadApps(): List<AppItem> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(i, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map {
                AppItem(
                    it.loadLabel(packageManager).toString(),
                    it.loadIcon(packageManager),
                    packageManager.getLaunchIntentForPackage(it.activityInfo.packageName)!!
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun roundIcon(d: Drawable) = ImageView(this).apply {
        setImageDrawable(d)
        scaleType = ImageView.ScaleType.FIT_CENTER
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, o: Outline) {
                o.setRoundRect(0, 0, v.width, v.height, v.height * 0.26f)
            }
        }
    }

    inner class IconAdapter(private val items: List<AppItem>) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(p: Int) = items[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, convert: View?, parent: ViewGroup): View {
            val a = items[p]
            return LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                addView(roundIcon(a.icon), LinearLayout.LayoutParams(dp(58), dp(58)))
                addView(TextView(this@MainActivity).apply {
                    text = a.label
                    setTextColor(Color.WHITE)
                    textSize = 11f
                    maxLines = 1
                    gravity = Gravity.CENTER
                    setShadowLayer(4f, 0f, 1f, Color.BLACK)
                }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
            }
        }
    }

    @Deprecated("Launcher should ignore Back")
    override fun onBackPressed() { /* stay on home */ }
}
