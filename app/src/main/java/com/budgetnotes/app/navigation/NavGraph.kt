package com.budgetnotes.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.budgetnotes.app.ui.root.RootScaffold

@Composable
fun BudgetNotesNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    RootScaffold(navController = navController)
}
