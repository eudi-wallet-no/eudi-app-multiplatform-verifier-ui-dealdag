/*
 * Copyright (c) 2026 Digitaliseringsdirektoratet (Digdir)
 *
 * NEW IN THE eudi-wallet-no FORK: this file is not part of the upstream EUDI proximity
 * verifier by the European Commission.
 *
 * Contents: the demo's rule for whether a shared credential is accepted.
 *
 * As a work based on the upstream project, it is licensed like the rest of it:
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

import eu.europa.ec.euidi.verifier.domain.config.model.AttestationType
import eu.europa.ec.euidi.verifier.presentation.model.ReceivedDocumentUi

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
