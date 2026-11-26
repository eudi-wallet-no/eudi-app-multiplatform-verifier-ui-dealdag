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

import androidx.lifecycle.viewModelScope
import eu.europa.ec.euidi.verifier.domain.config.model.AttestationType
import eu.europa.ec.euidi.verifier.domain.interactor.ShowDocumentsInteractor
import eu.europa.ec.euidi.verifier.presentation.architecture.MviViewModel
import eu.europa.ec.euidi.verifier.presentation.architecture.UiEffect
import eu.europa.ec.euidi.verifier.presentation.architecture.UiEvent
import eu.europa.ec.euidi.verifier.presentation.architecture.UiState
import eu.europa.ec.euidi.verifier.presentation.model.ReceivedDocumentUi
import eu.europa.ec.euidi.verifier.presentation.navigation.NavItem
import eu.europa.ec.euidi.verifier.presentation.ui.show_document.model.DocumentUi
import kotlinx.coroutines.launch

internal sealed interface ShowDocumentViewModelContract {
    data class State(
        val isLoading: Boolean = false,
        val items: List<DocumentUi> = emptyList(),
        val screenTitle: String = "",
        val verdict: DrikkebongVerdict? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class Init(
            val items: List<ReceivedDocumentUi>
        ) : Event

        data object OnDoneClick : Event
        data object OnBackClick : Event
    }

    sealed interface Effect : UiEffect {
        sealed interface Navigation : Effect {
            data class PopTo(
                val route: NavItem,
                val inclusive: Boolean,
            ) : Navigation
        }
    }
}

internal class ShowDocumentsViewModel(
    private val interactor: ShowDocumentsInteractor
) : MviViewModel<ShowDocumentViewModelContract.Event, ShowDocumentViewModelContract.State, ShowDocumentViewModelContract.Effect>() {

    override fun createInitialState(): ShowDocumentViewModelContract.State =
        ShowDocumentViewModelContract.State()

    override fun handleEvent(event: ShowDocumentViewModelContract.Event) {
        when (event) {
            is ShowDocumentViewModelContract.Event.Init -> {
                viewModelScope.launch {
                    setState {
                        copy(
                            isLoading = true
                        )
                    }

                    val title = interactor.getScreenTitle()
                    val transformedItems = interactor.transformToUiItems(
                        items = event.items
                    )

                    setState {
                        copy(
                            screenTitle = title,
                            items = transformedItems,
                            verdict = drikkebongVerdict(event.items),
                            isLoading = false
                        )
                    }
                }
            }

            is ShowDocumentViewModelContract.Event.OnDoneClick -> {
                popTo(
                    route = NavItem.Home,
                    inclusive = false
                )
            }

            is ShowDocumentViewModelContract.Event.OnBackClick -> {
                popTo(
                    route = NavItem.Home,
                    inclusive = false
                )
            }
        }
    }

    private fun popTo(route: NavItem, inclusive: Boolean) {
        setEffect {
            ShowDocumentViewModelContract.Effect.Navigation.PopTo(
                route = route,
                inclusive = inclusive,
            )
        }
    }
}

/** Whether the bar should accept the drikkebong that was shared, and if not, why. */
internal enum class DrikkebongVerdict {
    ACCEPTED,
    MISSING,
    INVALID,
    NOT_TRUSTED,
}

/**
 * A drikkebong is accepted when one was received, its issuer and device signatures and data
 * integrity check out (ISO/IEC 18013-5, 9.3), and its issuer chains to a trusted certificate.
 */
internal fun drikkebongVerdict(items: List<ReceivedDocumentUi>): DrikkebongVerdict {
    val bonger = items.filter { it.documentType == AttestationType.Drikkebong }
    if (bonger.isEmpty()) return DrikkebongVerdict.MISSING
    val valid = bonger.filter {
        with(it.documentValidity) {
            isIssuerSignatureValid && isDeviceSignatureValid && isDataIntegrityIntact
        }
    }
    if (valid.isEmpty()) return DrikkebongVerdict.INVALID
    if (valid.none { it.isTrusted }) return DrikkebongVerdict.NOT_TRUSTED
    return DrikkebongVerdict.ACCEPTED
}
