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

package eu.europa.ec.euidi.verifier.domain.config

import eu.europa.ec.euidi.verifier.core.controller.PlatformController
import eu.europa.ec.euidi.verifier.core.controller.model.BuildType
import eu.europa.ec.euidi.verifier.core.controller.model.FlavorType
import eu.europa.ec.euidi.verifier.domain.config.model.AttestationType
import eu.europa.ec.euidi.verifier.domain.config.model.ClaimItem
import eu.europa.ec.euidi.verifier.domain.config.model.DocumentMode
import eu.europa.ec.euidi.verifier.domain.config.model.Logger
import eu.europa.ec.euidi.verifier.domain.config.model.SupportedDocuments
import eudiverifier.verifierapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

internal interface ConfigProvider {

    val buildType: BuildType

    val flavorType: FlavorType

    val appVersion: String

    val supportedDocuments: SupportedDocuments

    val logger: Logger

    fun getDocumentModes(attestationType: AttestationType): List<DocumentMode>

    suspend fun getCertificates(): List<String>
}

internal class ConfigProviderImpl(private val platformController: PlatformController) : ConfigProvider {

    override val buildType: BuildType
        get() = platformController.buildType

    override val flavorType: FlavorType
        get() = platformController.flavorType

    override val appVersion: String
        get() = "${platformController.appVersion}-${flavorType.name}"

    override fun getDocumentModes(attestationType: AttestationType): List<DocumentMode> {
        return when (attestationType) {
            AttestationType.Pid -> listOf(DocumentMode.FULL, DocumentMode.CUSTOM)
            AttestationType.Mdl -> listOf(DocumentMode.FULL, DocumentMode.CUSTOM)
            AttestationType.EmployeeId -> listOf(DocumentMode.FULL, DocumentMode.CUSTOM)
            AttestationType.Drikkebong -> listOf(DocumentMode.FULL)
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getCertificates(): List<String> = listOf(
        Res.readBytes("files/certs/pidissuerca02_cz.pem").decodeToString(),
        Res.readBytes("files/certs/pidissuerca02_ee.pem").decodeToString(),
        Res.readBytes("files/certs/pidissuerca02_eu.pem").decodeToString(),
        Res.readBytes("files/certs/pidissuerca02_lu.pem").decodeToString(),
        Res.readBytes("files/certs/pidissuerca02_nl.pem").decodeToString(),
        Res.readBytes("files/certs/pidissuerca02_pt.pem").decodeToString(),
        Res.readBytes("files/certs/pidissuerca02_ut.pem").decodeToString(),
        // Bevisporten (dealdag) in eidas2sandkasse test: EAA Provider CA 2 and its root
        Res.readBytes("files/certs/eidas2sandkasse_eaa_provider_ca2_test.pem").decodeToString(),
        Res.readBytes("files/certs/eidas2sandkasse_root_ca2_test.pem").decodeToString()
    )

    override val supportedDocuments = SupportedDocuments(
        mapOf(
            AttestationType.Drikkebong to listOf(
                ClaimItem("navn"),
                ClaimItem("epostadresse"),
            ),
        )
    )

    override val logger: Logger
        get() = when (buildType) {
            BuildType.DEBUG -> Logger.LEVEL_DEBUG
            BuildType.RELEASE -> Logger.OFF
        }
}