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

package eu.europa.ec.euidi.verifier.core.controller

import android.content.Context
import android.util.Base64
import com.android.identity.android.mdoc.deviceretrieval.VerificationHelper
import eu.europa.ec.eudi.verifier.core.EudiVerifier
import eu.europa.ec.eudi.verifier.core.EudiVerifierConfig
import eu.europa.ec.eudi.verifier.core.request.DeviceRequest
import eu.europa.ec.eudi.verifier.core.request.DocRequest
import eu.europa.ec.eudi.verifier.core.transfer.TransferConfig
import eu.europa.ec.eudi.verifier.core.transfer.TransferEvent
import eu.europa.ec.eudi.verifier.core.transfer.TransferManager
import eu.europa.ec.euidi.verifier.core.extension.flattenedClaims
import eu.europa.ec.euidi.verifier.core.provider.ResourceProvider
import eu.europa.ec.euidi.verifier.core.utils.Constants
import eu.europa.ec.euidi.verifier.domain.config.model.ClaimItem
import eu.europa.ec.euidi.verifier.domain.config.model.Logger
import eu.europa.ec.euidi.verifier.domain.model.DocumentValidityDomain
import eu.europa.ec.euidi.verifier.domain.model.ReceivedDocumentDomain
import eu.europa.ec.euidi.verifier.domain.model.ReceivedDocumentsDomain
import eu.europa.ec.euidi.verifier.presentation.model.RequestedDocumentUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethod
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.util.UUID
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class AndroidTransferController(
    private val context: Context,
    private val resourceProvider: ResourceProvider,
    private val nfcEngagement: AndroidNfcEngagement,
) : TransferController {

    private var transferManager: TransferManager? = null
    private lateinit var eudiVerifier: EudiVerifier
    private var listener: TransferEvent.Listener? = null
    private var scope: CoroutineScope? = null

    private val _statuses = MutableSharedFlow<TransferStatus>(
        replay = 1,
        extraBufferCapacity = 64,
    )
    private val statuses: SharedFlow<TransferStatus> = _statuses.asSharedFlow()

    override fun initializeVerifier(certificates: List<String>, logger: Logger) {
        if (::eudiVerifier.isInitialized.not()) {

            eudiVerifier = EudiVerifier(
                context = context,
                config = EudiVerifierConfig {
                    configureLogging(level = logger.level)
                }
            ) {

                val x509Certificates = certificates.mapNotNull {
                    pemToX509Certificate(it).getOrNull()
                }

                trustedCertificates(
                    certificatesProvided = x509Certificates
                )
            }
        }
    }

    override fun initializeTransferManager(
        bleCentralClientMode: Boolean,
        blePeripheralServerMode: Boolean,
        useL2Cap: Boolean,
        clearBleCache: Boolean
    ) {
        val connectionMethods = listOf<MdocConnectionMethod>(
            MdocConnectionMethodBle(
                supportsPeripheralServerMode = blePeripheralServerMode,
                supportsCentralClientMode = bleCentralClientMode,
                peripheralServerModeUuid = UUID.randomUUID(),
                centralClientModeUuid = UUID.randomUUID()
            )
        )

        transferManager = eudiVerifier.createTransferManager {
            addEngagementMethod(TransferConfig.EngagementMethod.QR, connectionMethods)
        }

        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun startEngagement(qrCode: String) {
        if (qrCode == Constants.Generic.NFC_ENGAGEMENT) {
            startNfcEngagement()
        } else {
            _statuses.tryEmit(TransferStatus.Info("Les engagement frå QR-koden"))
            try {
                transferManager?.startQRDeviceEngagement(qrCode)
            } catch (t: Throwable) {
                // A malformed QR code must not crash the app
                _statuses.tryEmit(
                    TransferStatus.Error("Ugyldig QR-kode: ${t::class.simpleName}: ${t.localizedMessage}")
                )
            }
        }
    }

    override fun enableNfcEngagement(onTagDiscovered: () -> Unit): Boolean =
        nfcEngagement.enable(onTagDiscovered)

    override fun disableNfcEngagement() {
        nfcEngagement.disable()
    }

    /**
     * NFC device engagement (ISO/IEC 18013-5, 8.2.2.1). verifier-core 0.1.0 does not implement it
     * (`enableNFCDeviceEngagement` only logs "Not implemented yet"), so we give its transfer
     * manager a multipaz [VerificationHelper] and let that read the engagement from the tag.
     * The transfer manager's own listener then connects over BLE and reports events as for QR.
     */
    private fun startNfcEngagement() {
        val manager = transferManager ?: return
        val engagement = nfcEngagement.takePendingEngagement()
        if (engagement == null) {
            nfcEngagement.showStatus(NFC_TAG_LOST_MESSAGE)
            _statuses.tryEmit(TransferStatus.Error(NFC_TAG_LOST_MESSAGE))
            return
        }
        _statuses.tryEmit(
            TransferStatus.Info("Engagement lese over NFC (${engagement.tagTechs})")
        )
        try {
            manager.takeOverNfcEngagement(engagement)
        } catch (t: Throwable) {
            nfcEngagement.showStatus("NFC-feil: ${t.localizedMessage}")
            _statuses.tryEmit(
                TransferStatus.Error(
                    "${t::class.simpleName}: ${t.localizedMessage ?: resourceProvider.genericErrorMessage()}"
                )
            )
        }
    }

    /**
     * Puts the [VerificationHelper] that already did the NFC engagement into the transfer manager
     * (verifier-core 0.1.0 has no NFC support of its own), and plays back what the wallet answered
     * to the manager's own listener, which then connects over BLE and reports events as for QR.
     */
    private fun TransferManager.takeOverNfcEngagement(engagement: PendingNfcEngagement) {
        val managerClass = this::class.java
        val listener = managerClass.getDeclaredField("responseListener")
            .apply { isAccessible = true }
            .get(this) as VerificationHelper.Listener

        managerClass.getDeclaredField("verificationHelper")
            .apply { isAccessible = true }
            .set(this, engagement.helper)

        engagement.listener.attach(listener)
    }

    override fun sendRequest(
        requestedDocs: List<RequestedDocumentUi>,
        retainData: Boolean,
    ): Flow<TransferStatus> {
        requireNotNull(transferManager) {
            "TransferManager is not initialized. Call initializeTransferManager() before sendRequest()."
        }

        val request = requestedDocs
            .map { it.transformToDocRequest(retainData) }
            .toDeviceRequest()

        val eventsListener = object : TransferEvent.Listener {
            override fun onEvent(event: TransferEvent) {
                with(_statuses) {
                    when (event) {
                        is TransferEvent.Connecting -> {
                            tryEmit(TransferStatus.Connecting)
                        }

                        is TransferEvent.Connected -> {
                            tryEmit(TransferStatus.Connected)
                            tryEmit(TransferStatus.Info("Ber om: ${requestedDocs.describe()}"))
                            transferManager?.sendRequest(request)
                        }

                        is TransferEvent.DeviceEngagementCompleted -> {
                            tryEmit(TransferStatus.DeviceEngagementCompleted)
                        }

                        is TransferEvent.Disconnected -> {
                            tryEmit(TransferStatus.Disconnected)
                        }

                        is TransferEvent.Error -> {
                            tryEmit(
                                TransferStatus.Error(
                                    "${event.error::class.simpleName}: " +
                                            (event.error.localizedMessage
                                                ?: resourceProvider.genericErrorMessage())
                                )
                            )
                        }

                        is TransferEvent.RequestSent -> {
                            tryEmit(TransferStatus.RequestSent)
                        }

                        is TransferEvent.ResponseReceived -> {

                            scope?.launch {
                                try {
                                    val receivedDocuments = coroutineScope {
                                        event.response.deviceResponse.documents
                                            .zip(event.response.documentsClaims)
                                            .map { (parserDoc, claimsDoc) ->
                                                async {
                                                    val trusted = eudiVerifier
                                                        .isDocumentTrusted(
                                                            document = parserDoc,
                                                            atTime = Clock.System.now()
                                                        ).isTrusted

                                                    val validity = event.response.documentsValidity
                                                        .find { it.docType == claimsDoc.docType }

                                                    ReceivedDocumentDomain(
                                                        isTrusted = trusted,
                                                        docType = claimsDoc.docType,
                                                        claims = claimsDoc.flattenedClaims(
                                                            resourceProvider
                                                        ),
                                                        validity = DocumentValidityDomain(
                                                            isDeviceSignatureValid = validity?.isDeviceSignatureValid,
                                                            isIssuerSignatureValid = validity?.isIssuerSignatureValid,
                                                            isDataIntegrityIntact = validity?.isDataIntegrityIntact,
                                                            signed = validity?.msoValidity?.signed,
                                                            validFrom = validity?.msoValidity?.validFrom,
                                                            validUntil = validity?.msoValidity?.validUntil,
                                                        )
                                                    )
                                                }
                                            }
                                            .awaitAll()
                                    }

                                    tryEmit(
                                        TransferStatus.Info(
                                            "Mottok ${receivedDocuments.size} dokument " +
                                                    "(status ${event.response.deviceResponse.status})"
                                        )
                                    )
                                    receivedDocuments.forEach { doc ->
                                        tryEmit(TransferStatus.Info(doc.describe()))
                                    }
                                    tryEmit(
                                        TransferStatus.OnResponseReceived(
                                            receivedDocs = ReceivedDocumentsDomain(documents = receivedDocuments)
                                        )
                                    )

                                } catch (t: Throwable) {
                                    tryEmit(
                                        TransferStatus.Error(
                                            t.localizedMessage
                                                ?: resourceProvider.genericErrorMessage()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        listener?.let { transferManager?.removeListener(it) }
        listener = eventsListener
        transferManager?.addListener(eventsListener)
        return statuses
    }

    override fun stopConnection() {
        nfcEngagement.reset()
        listener?.let { transferManager?.removeListener(it) }
        listener = null
        transferManager?.stopSession()
        transferManager = null
        scope?.cancel()
        scope = null
    }

    private fun List<RequestedDocumentUi>.describe(): String =
        joinToString { doc ->
            "${doc.documentType.docType} (${doc.claims.joinToString { it.label }})"
        }

    private fun ReceivedDocumentDomain.describe(): String {
        fun Boolean?.yesNo() = when (this) {
            true -> "ok"
            false -> "FEIL"
            null -> "ukjend"
        }
        return "$docType: utferdarsignatur ${validity.isIssuerSignatureValid.yesNo()}, " +
                "einingssignatur ${validity.isDeviceSignatureValid.yesNo()}, " +
                "dataintegritet ${validity.isDataIntegrityIntact.yesNo()}, " +
                "tillit ${if (isTrusted) "ok" else "IKKJE STOLT PÅ"}, " +
                "${claims.size} claim"
    }

    private companion object {
        const val NFC_TAG_LOST_MESSAGE = "Fekk ikkje kontakt med lommeboka over NFC. Prøv igjen."
    }

    private fun pemToX509Certificate(pem: String): Result<X509Certificate> {
        return runCatching {
            val base64 = pem
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replace("\\s+".toRegex(), "")

            val der = Base64.decode(base64, Base64.DEFAULT)
            val cf = CertificateFactory.getInstance("X.509")

            cf.generateCertificate(ByteArrayInputStream(der)) as X509Certificate
        }
    }

    private fun RequestedDocumentUi.transformToDocRequest(
        retainData: Boolean
    ): DocRequest {
        val requestedClaims: Map<String, Boolean> = this
            .claims
            .associate { claimItem: ClaimItem ->
                claimItem.label to retainData
            }

        return DocRequest(
            docType = this.documentType.docType,
            itemsRequest = mapOf(
                this.documentType.namespace to requestedClaims
            ),
            readerAuthCertificate = null
        )
    }

    private fun List<DocRequest>.toDeviceRequest(): DeviceRequest =
        DeviceRequest(
            docRequests = this
        )
}