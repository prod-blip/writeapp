package com.atulpandey.clearwrite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atulpandey.clearwrite.data.DataStoreClearWriteLocalStore
import com.atulpandey.clearwrite.data.StoredClearWriteState
import com.atulpandey.clearwrite.data.ThemePreference
import com.atulpandey.clearwrite.billing.PlayBillingManager
import com.atulpandey.clearwrite.ai.AiRewriteRepository
import com.atulpandey.clearwrite.theme.ClearWriteTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      val applicationContext = LocalContext.current.applicationContext
      val localStore = remember(applicationContext) { DataStoreClearWriteLocalStore(applicationContext) }
      val billingManager = remember(applicationContext) { PlayBillingManager(applicationContext) }
      val aiRewriteRepository =
        remember(billingManager) {
          AiRewriteRepository(
            purchaseTokenProvider = { billingManager.activePurchaseToken },
            reviewerTokenProvider = { billingManager.activeReviewerToken },
          )
        }
      DisposableEffect(billingManager) {
        onDispose { billingManager.close() }
      }
      val storedState by
        localStore.state.collectAsStateWithLifecycle(initialValue = StoredClearWriteState())
      val followsSystemDarkTheme = isSystemInDarkTheme()
      val useDarkTheme =
        when (storedState.themePreference) {
          ThemePreference.SYSTEM -> followsSystemDarkTheme
          ThemePreference.LIGHT -> false
          ThemePreference.DARK -> true
        }
      SideEffect {
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
          statusBarStyle =
            SystemBarStyle.auto(transparent, transparent) { useDarkTheme },
          navigationBarStyle =
            SystemBarStyle.auto(transparent, transparent) { useDarkTheme },
        )
      }

      ClearWriteTheme(darkTheme = useDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(
            localStore = localStore,
            billingManager = billingManager,
            aiRewriteRepository = aiRewriteRepository,
          )
        }
      }
    }
  }
}
