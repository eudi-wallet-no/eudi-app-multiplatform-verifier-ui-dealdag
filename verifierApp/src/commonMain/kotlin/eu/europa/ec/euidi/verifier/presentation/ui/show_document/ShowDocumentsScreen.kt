/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.euidi.verifier.presentation.ui.show_document

import androidx.compose.foundation.background
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import eu.europa.ec.euidi.verifier.presentation.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.euidi.verifier.presentation.model.ReceivedDocsHolder
import eu.europa.ec.euidi.verifier.presentation.navigation.getFromPreviousBackStack
import eu.europa.ec.euidi.verifier.presentation.ui.home.CHEERS_ILLUSTRATION_ASPECT_RATIO
import eu.europa.ec.euidi.verifier.presentation.ui.home.CheersIllustration
import eu.europa.ec.euidi.verifier.presentation.ui.home.FestCheckBadge
import eu.europa.ec.euidi.verifier.presentation.ui.home.FestCrossBadge
import eu.europa.ec.euidi.verifier.presentation.ui.home.NO_BONG_ILLUSTRATION_ASPECT_RATIO
import eu.europa.ec.euidi.verifier.presentation.ui.home.NoBongIllustration
import eu.europa.ec.euidi.verifier.presentation.ui.home.FestColors
import eu.europa.ec.euidi.verifier.presentation.ui.home.FestGoldButton
import eu.europa.ec.euidi.verifier.presentation.ui.home.FestOrnaments
import eu.europa.ec.euidi.verifier.presentation.utils.Constants
import eudiverifier.verifierapp.generated.resources.Res
import eudiverifier.verifierapp.generated.resources.drikkebong_accepted_title
import eudiverifier.verifierapp.generated.resources.drikkebong_back_to_start
import eudiverifier.verifierapp.generated.resources.drikkebong_rejected_invalid
import eudiverifier.verifierapp.generated.resources.drikkebong_none_left_subtitle
import eudiverifier.verifierapp.generated.resources.drikkebong_none_left_title
import eudiverifier.verifierapp.generated.resources.drikkebong_rejected_not_trusted
import eudiverifier.verifierapp.generated.resources.drikkebong_rejected_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Result screen after a wallet has shared its drikkebong, in the same festive design as the
 * welcome screen: "Drikkebong akseptert. Party on!" or why it was not accepted.
 */
@Composable
internal fun ShowDocumentsScreen(
    navController: NavController,
    viewModel: ShowDocumentsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val verdict = state.verdict

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

        if (verdict != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(0.5f))

                if (verdict == DrikkebongVerdict.ACCEPTED) {
                    FestCheckBadge(modifier = Modifier.size(160.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(Res.string.drikkebong_accepted_title),
                        color = FestColors.DarkGreen,
                        fontSize = 40.sp,
                        lineHeight = 48.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    FestCrossBadge(modifier = Modifier.size(160.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(
                            if (verdict == DrikkebongVerdict.MISSING) Res.string.drikkebong_none_left_title
                            else Res.string.drikkebong_rejected_title
                        ),
                        color = FestColors.Red,
                        fontSize = 36.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(verdict.reason()),
                        color = FestColors.DarkGreen,
                        fontSize = 20.sp,
                        lineHeight = 30.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 480.dp),
                    )
                }

                Spacer(modifier = Modifier.weight(0.6f))

                when (verdict) {
                    DrikkebongVerdict.ACCEPTED -> CheersIllustration(
                        modifier = Modifier
                            .widthIn(max = 480.dp)
                            .fillMaxWidth()
                            .aspectRatio(CHEERS_ILLUSTRATION_ASPECT_RATIO)
                    )

                    DrikkebongVerdict.MISSING -> NoBongIllustration(
                        modifier = Modifier
                            .widthIn(max = 480.dp)
                            .fillMaxWidth()
                            .aspectRatio(NO_BONG_ILLUSTRATION_ASPECT_RATIO)
                    )

                    else -> Unit
                }

                Spacer(modifier = Modifier.weight(1f))

                FestGoldButton(
                    text = stringResource(Res.string.drikkebong_back_to_start),
                    enabled = true,
                    onClick = {
                        viewModel.setEvent(ShowDocumentViewModelContract.Event.OnDoneClick)
                    },
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = { viewModel.setEvent(ShowDocumentViewModelContract.Event.OnBackClick) }
    )

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ShowDocumentViewModelContract.Effect.Navigation ->
                    handleNavigationEffect(navController, effect)
            }
        }
    }

    OneTimeLaunchedEffect {
        navController
            .getFromPreviousBackStack<ReceivedDocsHolder>(Constants.RECEIVED_DOCUMENTS)
            ?.let { docsHolder ->
                viewModel.setEvent(
                    ShowDocumentViewModelContract.Event.Init(
                        items = docsHolder.items,
                    )
                )
            }
    }
}

private fun DrikkebongVerdict.reason() = when (this) {
    DrikkebongVerdict.ACCEPTED,
    DrikkebongVerdict.MISSING -> Res.string.drikkebong_none_left_subtitle

    DrikkebongVerdict.INVALID -> Res.string.drikkebong_rejected_invalid
    DrikkebongVerdict.NOT_TRUSTED -> Res.string.drikkebong_rejected_not_trusted
}

private fun handleNavigationEffect(
    navController: NavController,
    navigationEffect: ShowDocumentViewModelContract.Effect.Navigation
) {
    when (navigationEffect) {
        is ShowDocumentViewModelContract.Effect.Navigation.PopTo -> {
            navController.popBackStack(
                route = navigationEffect.route,
                inclusive = navigationEffect.inclusive
            )
        }
    }
}
