package com.prismspace.container.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.prismspace.container.core.BootstrapTrace
import com.prismspace.container.core.NativeCore
import com.prismspace.container.service.PrismEngineService
import com.prismspace.container.ui.navigation.PrismSpaceNavGraph
import com.prismspace.container.ui.theme.BackgroundDark
import com.prismspace.container.ui.theme.PrismSpaceTheme
import com.prismspace.container.ui.viewmodel.PrismSpaceViewModel
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Main Activity for Prism Space UI
 */
class PrismSpaceActivity : ComponentActivity() {

    private val bootstrapTriggered = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PrismEngineService.startService(this)

        setContent {
            PrismSpaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    val navController = rememberNavController()
                    val viewModel: PrismSpaceViewModel = viewModel()

                    PrismSpaceNavGraph(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }

        window.decorView.post {
            if (bootstrapTriggered.compareAndSet(false, true)) {
                BootstrapTrace.trace("first_activity_post_bootstrap_trigger")
                NativeCore.ensureBootstrapped()
            }
        }
    }
}
