package com.vrock.navigationexample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.vrock.navigation.ui.view.NavigationController
import com.vrock.navigation.ui.view.NavigationLaunchConfig
import com.vrock.navigation.ui.view.NavigationRoutePoints
import com.vrock.navigation.ui.view.NavigationFragment

class EmbeddedNavigationActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = IntentCompat.getSerializableExtra(intent, CONFIG, NavigationLaunchConfig::class.java)
        val points = IntentCompat.getSerializableExtra(intent, POINTS, NavigationRoutePoints::class.java)
        if (config == null || points == null) {
            finish()
            return
        }

        val container = FragmentContainerView(this).apply { id = android.R.id.custom }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(Button(context).apply {
                setText(R.string.close_fragment_trip)
                setOnClickListener { endEmbeddedTrip() }
            }, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            addView(container, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val padding = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(padding.left, padding.top, padding.right, padding.bottom)
            insets
        }
        setContentView(root)

        supportFragmentManager.setFragmentResultListener(NavigationFragment.managedEndResultKey, this) { _, _ ->
            finish()
        }
        val fragment = supportFragmentManager.findFragmentById(container.id) as? NavigationFragment
        if (fragment == null) {
            supportFragmentManager.beginTransaction()
                .add(container.id, NavigationFragment.newInstance(config, points).apply { manageSystemBars = true })
                .commitNow()
        } else {
            fragment.manageSystemBars = true
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (config.appearance.uiConfig.allowBackToEndTrip) endEmbeddedTrip()
            }
        })
    }

    private fun endEmbeddedTrip() {
        if (!NavigationController.stopNavigation()) finish()
    }

    companion object {
        private const val CONFIG = "com.vrock.navigationexample.embedded_config"
        private const val POINTS = "com.vrock.navigationexample.embedded_points"

        fun createIntent(context: Context, config: NavigationLaunchConfig, points: NavigationRoutePoints): Intent =
            Intent(context, EmbeddedNavigationActivity::class.java)
                .putExtra(CONFIG, config)
                .putExtra(POINTS, points.copy(waypoints = ArrayList(points.waypoints)))
    }
}
