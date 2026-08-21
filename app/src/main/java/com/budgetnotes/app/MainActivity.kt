package com.budgetnotes.app

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.budgetnotes.app.navigation.BudgetNotesNavGraph
import com.budgetnotes.app.ui.lock.VaultGate
import com.budgetnotes.app.ui.theme.BudgetNotesTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Release only: block screenshots / recents of vault contents.
        // Debug stays capturable for store screenshots and QA.
        val isDebuggable =
            (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isDebuggable) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
        enableEdgeToEdge()
        setContent {
            BudgetNotesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val app = application as BudgetNotesApplication
                    var unlocked by remember {
                        mutableStateOf(app.container.isUnlocked)
                    }
                    if (!unlocked) {
                        VaultGate(
                            onUnlocked = { unlocked = true },
                        )
                    } else {
                        BudgetNotesNavGraph()
                    }
                }
            }
        }
    }
}
