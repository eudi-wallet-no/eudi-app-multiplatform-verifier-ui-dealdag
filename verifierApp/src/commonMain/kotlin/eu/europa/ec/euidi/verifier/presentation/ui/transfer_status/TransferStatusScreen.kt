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

package eu.europa.ec.euidi.verifier.presentation.ui.transfer_status

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.bluetooth.BLUETOOTH_ADVERTISE
import dev.icerock.moko.permissions.bluetooth.BLUETOOTH_CONNECT
import dev.icerock.moko.permissions.bluetooth.BLUETOOTH_LE
import dev.icerock.moko.permissions.bluetooth.BLUETOOTH_SCAN
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.location.COARSE_LOCATION
import dev.icerock.moko.permissions.location.LOCATION
import eu.europa.ec.euidi.verifier.core.extension.arePermissionsGranted
import eu.europa.ec.euidi.verifier.core.extension.provideAll
import eu.europa.ec.euidi.verifier.presentation.component.ErrorInfo
import eu.europa.ec.euidi.verifier.presentation.component.content.ContentScreen
import eu.europa.ec.euidi.verifier.presentation.component.content.ScreenNavigateAction
import eu.europa.ec.euidi.verifier.presentation.component.content.ToolbarConfig
import eu.europa.ec.euidi.verifier.presentation.component.fest.FestColors
import eu.europa.ec.euidi.verifier.presentation.component.fest.FestGoldButton
import eu.europa.ec.euidi.verifier.presentation.component.fest.FestOrnaments
import eu.europa.ec.euidi.verifier.presentation.component.loader.LoadingIndicator
import eu.europa.ec.euidi.verifier.presentation.component.preview.PreviewTheme
import eu.europa.ec.euidi.verifier.presentation.component.preview.ThemeModePreviews
import eu.europa.ec.euidi.verifier.presentation.component.utils.LifecycleEffect
import eu.europa.ec.euidi.verifier.presentation.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.euidi.verifier.presentation.component.utils.SIZE_SMALL
import eu.europa.ec.euidi.verifier.presentation.component.utils.SPACING_MEDIUM
import eu.europa.ec.euidi.verifier.presentation.component.utils.SPACING_SMALL
import eu.europa.ec.euidi.verifier.presentation.component.wrap.ButtonConfig
import eu.europa.ec.euidi.verifier.presentation.component.wrap.ButtonType
import eu.europa.ec.euidi.verifier.presentation.component.wrap.StickyBottomConfig
import eu.europa.ec.euidi.verifier.presentation.component.wrap.StickyBottomType
import eu.europa.ec.euidi.verifier.presentation.component.wrap.WrapCard
import eu.europa.ec.euidi.verifier.presentation.component.wrap.WrapStickyBottomContent
import eu.europa.ec.euidi.verifier.presentation.model.ReceivedDocsHolder
import eu.europa.ec.euidi.verifier.presentation.model.RequestedDocsHolder
import eu.europa.ec.euidi.verifier.presentation.navigation.NavItem
import eu.europa.ec.euidi.verifier.presentation.navigation.getFromPreviousBackStack
import eu.europa.ec.euidi.verifier.presentation.navigation.saveToCurrentBackStack
import eu.europa.ec.euidi.verifier.presentation.ui.transfer_status.TransferStatusViewModelContract.Event.PermissionReceived
import eu.europa.ec.euidi.verifier.presentation.utils.Constants
import eudiverifier.verifierapp.generated.resources.Res
import eudiverifier.verifierapp.generated.resources.generic_cancel
import eudiverifier.verifierapp.generated.resources.transfer_back
import eudiverifier.verifierapp.generated.resources.transfer_cancel
import eudiverifier.verifierapp.generated.resources.transfer_failed_subtitle
import eudiverifier.verifierapp.generated.resources.transfer_failed_title
import eudiverifier.verifierapp.generated.resources.transfer_log_title
import eudiverifier.verifierapp.generated.resources.transfer_status_screen_no_permissions
import eudiverifier.verifierapp.generated.resources.transfer_status_screen_request_status
import eudiverifier.verifierapp.generated.resources.transfer_status_screen_title
import eudiverifier.verifierapp.generated.resources.transfer_waiting_subtitle
import eudiverifier.verifierapp.generated.resources.transfer_waiting_title
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import eu.europa.ec.euidi.verifier.core.utils.Constants as CoreConstants

@Composable
internal fun TransferStatusScreen(
    navController: NavController,
    viewModel: TransferStatusViewModel
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

        Content(
            state = state,
            effectFlow = viewModel.effect,
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(
                    navController = navController,
                    navigationEffect = navigationEffect
                )
            },
            onEvent = { event -> viewModel.setEvent(event) }
        )

        LifecycleEffect(
            lifecycleOwner = LocalLifecycleOwner.current,
            lifecycleEvent = Lifecycle.Event.ON_RESUME
        ) {
            viewModel.setEvent(
                TransferStatusViewModelContract.Event.RequestPermissions
            )
        }

        OneTimeLaunchedEffect {
            val requestedDocs = navController.getFromPreviousBackStack<RequestedDocsHolder>(
                key = Constants.REQUESTED_DOCUMENTS
            )
            requestedDocs?.let {
                viewModel.setEvent(TransferStatusViewModelContract.Event.Init(requestedDocs.items))
            }
        }
    }
}

private fun handleNavigationEffect(
    navController: NavController,
    navigationEffect: TransferStatusViewModelContract.Effect.Navigation
) {
    when (navigationEffect) {
        is TransferStatusViewModelContract.Effect.Navigation.GoBack -> navController.popBackStack()

        is TransferStatusViewModelContract.Effect.Navigation.RetryOverNfc -> {
            navController.popBackStack()
            navController.navigate(NavItem.TransferStatus(qrCode = CoreConstants.Generic.NFC_ENGAGEMENT))
        }

        is TransferStatusViewModelContract.Effect.Navigation.NavigateToShowDocumentsScreen -> {
            navController.saveToCurrentBackStack<ReceivedDocsHolder>(
                key = Constants.RECEIVED_DOCUMENTS,
                value = ReceivedDocsHolder(
                    items = navigationEffect.receivedDocuments,
                )
            )
            navController.navigate(NavItem.ShowDocuments)
        }
    }
}

@Composable
private fun StickyBottomSection(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = modifier
    ) {
        WrapStickyBottomContent(
            stickyBottomModifier = Modifier.fillMaxWidth(),
            stickyBottomConfig = StickyBottomConfig(
                type = StickyBottomType.OneButton(
                    config = ButtonConfig(
                        type = ButtonType.SECONDARY,
                        enabled = enabled,
                        onClick = onClick,
                        content = {
                            Text(
                                text = stringResource(Res.string.generic_cancel)
                            )
                        }
                    )
                )
            )
        )
    }
}

@Composable
private fun Content(
    state: TransferStatusViewModelContract.State,
    effectFlow: Flow<TransferStatusViewModelContract.Effect>,
    onNavigationRequested: (TransferStatusViewModelContract.Effect.Navigation) -> Unit,
    onEvent: (TransferStatusViewModelContract.Event) -> Unit,
) {

    val permissionsControllerFactory: PermissionsControllerFactory =
        rememberPermissionsControllerFactory()

    val permissionsController: PermissionsController = remember(permissionsControllerFactory) {
        permissionsControllerFactory.createPermissionsController()
    }

    BindEffect(permissionsController)

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = { onEvent(TransferStatusViewModelContract.Event.OnBackClick) }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(110.dp))

        Text(
            text = stringResource(
                if (state.failed) Res.string.transfer_failed_title
                else Res.string.transfer_waiting_title
            ),
            color = if (state.failed) FestColors.Red else FestColors.DarkGreen,
            fontSize = 40.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.hasPermissions == false) {
            Text(
                text = stringResource(Res.string.transfer_status_screen_no_permissions),
                color = FestColors.Red,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onEvent(TransferStatusViewModelContract.Event.OpenAppSettings) }
                    .padding(12.dp),
            )
        } else {
            if (state.failed) {
                Text(
                    text = stringResource(Res.string.transfer_failed_subtitle),
                    color = FestColors.DarkGreen,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 480.dp),
                )
            } else {
                Text(
                    text = stringResource(Res.string.transfer_waiting_subtitle),
                    color = FestColors.DarkGreen,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 480.dp),
                )
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(
                    color = FestColors.Gold,
                    strokeWidth = 6.dp,
                    modifier = Modifier.size(64.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        StatusLog(
            lines = listOfNotNull(state.requestedDocTypes) + state.statusLog,
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        FestGoldButton(
            text = stringResource(
                if (state.failed) Res.string.transfer_back else Res.string.transfer_cancel
            ),
            enabled = true,
            onClick = { onEvent(TransferStatusViewModelContract.Event.OnCancelClick) },
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    LaunchedEffect(Unit) {
        effectFlow.collect { effect ->
            when (effect) {
                is TransferStatusViewModelContract.Effect.Navigation -> {
                    onNavigationRequested(effect)
                }

                is TransferStatusViewModelContract.Effect.RequestPermissions -> {

                    val permissions = buildList {
                        add(Permission.BLUETOOTH_LE)
                        add(Permission.BLUETOOTH_SCAN)
                        add(Permission.BLUETOOTH_CONNECT)
                        add(Permission.BLUETOOTH_ADVERTISE)
                        add(Permission.COARSE_LOCATION)
                        add(Permission.LOCATION)
                    }

                    if (permissionsController.arePermissionsGranted(*permissions.toTypedArray())) {
                        onEvent(PermissionReceived(denied = false))
                    } else {
                        permissionsController.provideAll(*permissions.toTypedArray()).fold(
                            onSuccess = {
                                onEvent(PermissionReceived(denied = false))
                            },
                            onFailure = {
                                onEvent(PermissionReceived(denied = true))
                            }
                        )
                    }
                }

                is TransferStatusViewModelContract.Effect.PermissionsGranted -> {
                    onEvent(TransferStatusViewModelContract.Event.StartProximity)
                }

                is TransferStatusViewModelContract.Effect.PermissionsRevoked -> {
                    onEvent(TransferStatusViewModelContract.Event.StopProximity)
                }

                is TransferStatusViewModelContract.Effect.OpenAppSettings -> {
                    permissionsController.openAppSettings()
                }
            }
        }
    }
}

/** Every status message so far, newest at the bottom, scrolled into view as they arrive. */
@Composable
private fun StatusLog(
    lines: List<String>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(FestColors.PaleGold.copy(alpha = 0.45f))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(Res.string.transfer_log_title),
            color = FestColors.DarkGreen,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(lines) { line ->
                Text(
                    text = line,
                    color = FestColors.DarkGreen,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
