/*
 * Copyright (c) 2026 Digitaliseringsdirektoratet (Digdir)
 *
 * NEW IN THE eudi-wallet-no FORK: this file is not part of the upstream EUDI proximity
 * verifier by the European Commission.
 *
 * Contents: NFC device engagement (ISO/IEC 18013-5), independent of any particular credential
 * or visual theme.
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

package eu.europa.ec.euidi.verifier.core.nfc

import android.app.Activity
import android.app.Application
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.android.identity.android.mdoc.deviceretrieval.VerificationHelper
import com.android.identity.android.mdoc.transport.DataTransportOptions
import eu.europa.ec.eudi.verifier.core.transfer.TransferManager
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethod
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.multipaz.mdoc.role.MdocRole
import org.multipaz.util.UUID
import android.widget.Toast
import java.lang.ref.WeakReference

/**
 * Keeps the NFC reader mode used for ISO/IEC 18013-5 NFC device engagement.
 *
 * Reader mode is on whenever the app is in the foreground. That also turns off card emulation on
 * this device, so wallet apps installed on the tablet cannot answer a phone held against it (they
 * would otherwise make Android show "choose an app").
 *
 * A screen that can start a verification registers itself with [enable]. When a wallet is found,
 * the NFC engagement and handover (ISO/IEC 18013-5, 8.2.2.1) are done right away, while the phone
 * is surely still in the field: by the time the transfer screen is ready the NFC link is often
 * gone (TagLostException). What the wallet answers is kept in a [PendingNfcEngagement] and played
 * back to the transfer manager when [eu.europa.ec.euidi.verifier.core.controller.AndroidTransferController] takes it over.
 *
 * This is a singleton: each screen gets its own [eu.europa.ec.euidi.verifier.core.controller.TransferController], and all of them have to see
 * the same pending tag.
 */
internal class AndroidNfcEngagement(context: Context) : Application.ActivityLifecycleCallbacks {

    private val appContext: Context = context.applicationContext

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var resumedActivity: WeakReference<Activity>? = null
    private var onTagDiscovered: (() -> Unit)? = null
    private var readerModeAnnounced = false

    @Volatile
    private var pending: PendingNfcEngagement? = null

    init {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
    }

    /**
     * Calls [onTagDiscovered] once, when a wallet is held against the device. Returns false when
     * the device has no NFC or it is turned off.
     */
    fun enable(onTagDiscovered: () -> Unit): Boolean {
        if (nfcAdapter?.isEnabled != true) return false
        this.onTagDiscovered = onTagDiscovered
        // A screen that starts listening will not take over an older engagement: drop it, or it
        // would block every new tap
        dropPending()
        // Make sure reader mode is on, also when it was lost without the activity pausing
        resumedActivity?.get()?.let { activity -> mainHandler.post { startReaderMode(activity) } }
        return true
    }

    /** Stops waiting for a wallet. Reader mode itself stays on while the app is open. */
    fun disable() {
        onTagDiscovered = null
    }

    /** Hands over the engagement done after [enable], at most once. */
    fun takePendingEngagement(): PendingNfcEngagement? = pending.also { pending = null }

    /** Stops waiting for a wallet and drops an engagement nobody took over. */
    fun reset() {
        onTagDiscovered = null
        dropPending()
    }

    private fun dropPending() {
        pending?.let { runCatching { it.helper.disconnect() } }
        pending = null
    }

    private fun startReaderMode(activity: Activity) {
        if (nfcAdapter?.isEnabled != true) return
        try {
            nfcAdapter.enableReaderMode(
                activity,
                { tag -> handleTag(tag) },
                NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                // Android's frequent presence checks can collide with the handover exchange and
                // end it with TagLostException, so check far less often than the default
                Bundle().apply {
                    putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, PRESENCE_CHECK_DELAY_MS)
                }
            )
            if (!readerModeAnnounced) {
                readerModeAnnounced = true
                showStatus("NFC-lesar aktiv: hald telefonen mot baksida av nettbrettet")
            }
        } catch (t: Throwable) {
            showStatus("NFC-lesar starta ikkje: ${t.message}")
        }
    }

    private fun handleTag(tag: Tag) {
        val techs = tag.techList.joinToString { it.substringAfterLast('.') }
        val callback = onTagDiscovered
        if (callback == null || pending != null) {
            showStatus("NFC: ingen skjerm ventar på ei lommebok no. Gå til framsida for å starte på nytt.")
            return
        }
        if (!tag.techList.contains(android.nfc.tech.IsoDep::class.java.name)) {
            showStatus("NFC: fann $techs, men det er ikkje ei lommebok (manglar IsoDep)")
            return
        }
        showStatus("NFC: fann lommebok, koplar til ...")
        onTagDiscovered = null

        val listener = BufferingListener(mainHandler)
        val engagement = PendingNfcEngagement(
            helper = VerificationHelper.Builder(
                appContext,
                listener,
                ContextCompat.getMainExecutor(appContext)
            )
                .setDataTransportOptions(DataTransportOptions.Builder().build())
                .setNegotiatedHandoverConnectionMethods(readerConnectionMethods())
                .build(),
            listener = listener,
            tagTechs = techs,
        )
        pending = engagement

        // Do the engagement now, while the phone is in the field, but on a thread of its own: if
        // it hangs (phone moved away mid-exchange), the NFC callback thread must stay free, or
        // Android delivers no more taps until the app restarts
        Thread({
            try {
                engagement.helper.nfcProcessOnTagDiscovered(tag)
            } catch (t: Throwable) {
                listener.onError(t)
            }
        }, "nfc-engagement").apply { isDaemon = true }.start()
        mainHandler.post(callback)
    }

    /** Short on-screen status, so NFC problems can be seen without a debugger. */
    fun showStatus(message: String) {
        mainHandler.post {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        resumedActivity = WeakReference(activity)
        // Android only turns on reader mode for an activity it sees as resumed, which it does not
        // yet while these callbacks run, so wait until onResume has finished.
        mainHandler.post {
            if (resumedActivity?.get() === activity) startReaderMode(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumedActivity?.get() === activity) {
            nfcAdapter?.disableReaderMode(activity)
            resumedActivity = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    /** BLE in both roles, one method per role, as the reader offers them in negotiated handover. */
    private fun readerConnectionMethods(): List<MdocConnectionMethod> =
        MdocConnectionMethod.disambiguate(
            listOf(
                MdocConnectionMethodBle(
                    supportsPeripheralServerMode = true,
                    supportsCentralClientMode = true,
                    peripheralServerModeUuid = UUID.randomUUID(),
                    centralClientModeUuid = UUID.randomUUID()
                )
            ),
            MdocRole.MDOC_READER
        )

    private companion object {
        const val PRESENCE_CHECK_DELAY_MS = 1000
    }
}

/** An NFC engagement done as soon as the wallet was found, waiting to be taken over. */
internal class PendingNfcEngagement(
    val helper: VerificationHelper,
    val listener: BufferingListener,
    val tagTechs: String,
)

/**
 * Keeps [VerificationHelper] events until [attach] gives it the transfer manager's own listener,
 * then plays them back in order and forwards everything after that.
 */
internal class BufferingListener(private val handler: Handler) : VerificationHelper.Listener {

    private val buffered = mutableListOf<(VerificationHelper.Listener) -> Unit>()
    private var target: VerificationHelper.Listener? = null

    fun attach(listener: VerificationHelper.Listener) {
        val events = synchronized(this) {
            target = listener
            buffered.toList().also { buffered.clear() }
        }
        handler.post { events.forEach { it(listener) } }
    }

    private fun dispatch(event: (VerificationHelper.Listener) -> Unit) {
        val current = synchronized(this) {
            target ?: run { buffered += event; null }
        }
        current?.let { listener -> handler.post { event(listener) } }
    }

    override fun onReaderEngagementReady(readerEngagement: ByteArray) =
        dispatch { it.onReaderEngagementReady(readerEngagement) }

    override fun onDeviceEngagementReceived(connectionMethods: List<MdocConnectionMethod>) =
        dispatch { it.onDeviceEngagementReceived(connectionMethods) }

    override fun onMoveIntoNfcField() = dispatch { it.onMoveIntoNfcField() }

    override fun onDeviceConnected() = dispatch { it.onDeviceConnected() }

    override fun onDeviceDisconnected(transportSpecificTermination: Boolean) =
        dispatch { it.onDeviceDisconnected(transportSpecificTermination) }

    override fun onResponseReceived(deviceResponseBytes: ByteArray) =
        dispatch { it.onResponseReceived(deviceResponseBytes) }

    override fun onError(error: Throwable) = dispatch { it.onError(error) }
}

/**
 * Puts the [VerificationHelper] that already did the NFC engagement into the transfer manager
 * (verifier-core 0.1.0 has no NFC support of its own), and plays back what the wallet answered
 * to the manager's own listener, which then connects over BLE and reports events as for QR.
 */
internal fun TransferManager.takeOverNfcEngagement(engagement: PendingNfcEngagement) {
    val managerClass = this::class.java
    val listener = managerClass.getDeclaredField("responseListener")
        .apply { isAccessible = true }
        .get(this) as VerificationHelper.Listener

    managerClass.getDeclaredField("verificationHelper")
        .apply { isAccessible = true }
        .set(this, engagement.helper)

    engagement.listener.attach(listener)
}
