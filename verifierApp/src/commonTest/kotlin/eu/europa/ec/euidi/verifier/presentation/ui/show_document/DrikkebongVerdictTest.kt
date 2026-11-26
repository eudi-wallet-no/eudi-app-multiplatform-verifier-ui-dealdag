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

import eu.europa.ec.euidi.verifier.domain.config.model.AttestationType
import eu.europa.ec.euidi.verifier.presentation.model.ReceivedDocumentUi
import eu.europa.ec.euidi.verifier.testutil.TestData
import kotlin.test.Test
import kotlin.test.assertEquals

class DrikkebongVerdictTest {

    private val validBong = ReceivedDocumentUi(
        id = "bong",
        documentType = AttestationType.Drikkebong,
        documentValidity = TestData.validDocumentValidityUi,
        isTrusted = true,
    )

    @Test
    fun `Given a valid and trusted drikkebong When the verdict is made Then it is accepted`() {
        assertEquals(DrikkebongVerdict.ACCEPTED, drikkebongVerdict(listOf(validBong)))
    }

    @Test
    fun `Given no drikkebong When the verdict is made Then it is missing`() {
        assertEquals(DrikkebongVerdict.MISSING, drikkebongVerdict(emptyList()))
        assertEquals(
            DrikkebongVerdict.MISSING,
            drikkebongVerdict(listOf(validBong.copy(documentType = AttestationType.Pid)))
        )
    }

    @Test
    fun `Given a drikkebong with a broken signature When the verdict is made Then it is invalid`() {
        val broken = validBong.copy(
            documentValidity = TestData.validDocumentValidityUi.copy(isDeviceSignatureValid = false)
        )
        assertEquals(DrikkebongVerdict.INVALID, drikkebongVerdict(listOf(broken)))
    }

    @Test
    fun `Given a valid drikkebong from an untrusted issuer When the verdict is made Then it is not trusted`() {
        assertEquals(
            DrikkebongVerdict.NOT_TRUSTED,
            drikkebongVerdict(listOf(validBong.copy(isTrusted = false)))
        )
    }
}
