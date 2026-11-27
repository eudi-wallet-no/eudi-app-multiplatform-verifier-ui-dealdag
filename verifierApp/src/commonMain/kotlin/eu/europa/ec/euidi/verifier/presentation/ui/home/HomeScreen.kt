/*
 * Copyright (c) 2025 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.euidi.verifier.presentation.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.euidi.verifier.presentation.component.AppIcons
import eu.europa.ec.euidi.verifier.presentation.component.fest.FEST_ILLUSTRATION_ASPECT_RATIO
import eu.europa.ec.euidi.verifier.presentation.component.fest.FestColors
import eu.europa.ec.euidi.verifier.presentation.component.fest.FestGoldButton
import eu.europa.ec.euidi.verifier.presentation.component.fest.FestIllustration
import eu.europa.ec.euidi.verifier.presentation.component.fest.FestOrnaments
import eu.europa.ec.euidi.verifier.presentation.component.utils.LifecycleEffect
import eu.europa.ec.euidi.verifier.presentation.component.wrap.WrapIconButton
import eu.europa.ec.euidi.verifier.presentation.model.RequestedDocsHolder
import eu.europa.ec.euidi.verifier.presentation.navigation.getFromCurrentBackStack
import eu.europa.ec.euidi.verifier.presentation.navigation.saveToCurrentBackStack
import eu.europa.ec.euidi.verifier.presentation.ui.home.HomeViewModelContract.Effect
import eu.europa.ec.euidi.verifier.presentation.ui.home.HomeViewModelContract.Event
import eu.europa.ec.euidi.verifier.presentation.utils.Constants
import eudiverifier.verifierapp.generated.resources.Res
import eudiverifier.verifierapp.generated.resources.home_screen_nfc_title
import eudiverifier.verifierapp.generated.resources.home_screen_sticky_bottom_text
import eudiverifier.verifierapp.generated.resources.home_screen_subtitle
import eudiverifier.verifierapp.generated.resources.home_screen_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Welcome screen for Digdir FEL Loen 2026, styled after the MinID+ Christmas design: cream
 * background, baubles, a festive illustration and one gold button that starts the verification.
 */
@Composable
internal fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FestColors.Background)
    ) {
        FestOrnaments(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp)
                .size(width = 200.dp, height = 130.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                WrapIconButton(
                    iconData = AppIcons.Menu,
                    modifier = Modifier.align(Alignment.CenterStart),
                    customTint = FestColors.DarkGreen,
                    onClick = { viewModel.setEvent(Event.OnMenuClick) },
                )
            }

            Spacer(modifier = Modifier.weight(0.6f))

            Text(
                text = stringResource(Res.string.home_screen_subtitle),
                color = FestColors.DarkGreen,
                fontSize = 32.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.home_screen_title),
                color = FestColors.Navy,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.weight(0.6f))

            Text(
                text = stringResource(Res.string.home_screen_nfc_title),
                color = FestColors.DarkGreen,
                fontSize = 20.sp,
                lineHeight = 30.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            FestIllustration(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .aspectRatio(FEST_ILLUSTRATION_ASPECT_RATIO)
            )

            Spacer(modifier = Modifier.weight(1f))

            FestGoldButton(
                text = stringResource(Res.string.home_screen_sticky_bottom_text),
                enabled = state.isStickyButtonEnabled,
                onClick = { viewModel.setEvent(Event.OnStickyButtonClicked) },
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is Effect.Navigation -> handleNavigationEffect(effect, navController)
            }
        }
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME
    ) {
        val documents = navController
            .getFromCurrentBackStack<RequestedDocsHolder>(Constants.REQUESTED_DOCUMENTS)
        viewModel.setEvent(Event.OnResume(docs = documents?.items))
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController,
) {
    when (navigationEffect) {
        is Effect.Navigation.PushScreen -> {
            navController.navigate(route = navigationEffect.route)
        }

        is Effect.Navigation.SaveDocsToBackstackAndGoTo -> {
            navController.saveToCurrentBackStack<RequestedDocsHolder>(
                key = Constants.REQUESTED_DOCUMENTS,
                value = navigationEffect.requestedDocs
            )
            navController.navigate(route = navigationEffect.screen)
        }
    }
}
