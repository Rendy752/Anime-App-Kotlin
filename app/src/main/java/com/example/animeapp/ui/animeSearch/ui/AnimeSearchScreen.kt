package com.example.animeapp.ui.animeSearch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.animeapp.R
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeSearchScreen(navController: NavController) {
    val viewModel: AnimeSearchViewModel = hiltViewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.title_search)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.applyFilters(viewModel.queryState.value) }) {
                Icon(
                    painterResource(id = R.drawable.ic_refresh_blue_24dp),
                    contentDescription = stringResource(id = R.string.refresh_button)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FilterSection(viewModel)
            HorizontalDivider()
            Column(modifier = Modifier.weight(1f)) {
                ResultsSection(navController, viewModel)
            }
            LimitAndPaginationSection(viewModel)
        }
    }
}