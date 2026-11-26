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

import android.app.Activity
import android.app.Application
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference

/**
 * Keeps the NFC reader mode used for ISO/IEC 18013-5 NFC device engagement.
 *
 * The tablet polls for the wallet (which emulates an NFC tag) while the scan screen is shown. The
 * first tag found is kept as the pending tag, and reader mode stays on so the tag stays usable
 * until [AndroidTransferController] has done the engagement with it.
 *
 * This is a singleton: the scan screen and the transfer screen each get their own
 * [TransferController], and both have to see the same pending tag.
 */
internal class AndroidNfcEngagement(context: Context) : Application.ActivityLifecycleCallbacks {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var resumedActivity: WeakReference<Activity>? = null
    private var readerModeWanted = false
    private var onTagDiscovered: (() -> Unit)? = null

    @Volatile
    private var pendingTag: Tag? = null

    init {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
    }

    /**
     * Starts polling for a wallet. Returns false when the device has no NFC or it is turned off.
     */
    fun enable(onTagDiscovered: () -> Unit): Boolean {
        if (nfcAdapter?.isEnabled != true) return false
        this.onTagDiscovered = onTagDiscovered
        pendingTag = null
        readerModeWanted = true
        resumedActivity?.get()?.let(::startReaderMode)
        return true
    }

    /** Stops polling, unless a tag was found and is waiting to be engaged. */
    fun disable() {
        onTagDiscovered = null
        if (pendingTag == null) stopReaderMode()
    }

    /** Hands over the tag found by [enable], at most once. */
    fun takePendingTag(): Tag? = pendingTag.also { pendingTag = null }

    /** Stops polling and forgets any pending tag. */
    fun reset() {
        onTagDiscovered = null
        pendingTag = null
        stopReaderMode()
    }

    private fun startReaderMode(activity: Activity) {
        nfcAdapter?.enableReaderMode(
            activity,
            { tag -> handleTag(tag) },
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    private fun stopReaderMode() {
        readerModeWanted = false
        mainHandler.post {
            resumedActivity?.get()?.let { nfcAdapter?.disableReaderMode(it) }
        }
    }

    private fun handleTag(tag: Tag) {
        val callback = onTagDiscovered ?: return
        if (pendingTag != null) return
        pendingTag = tag
        onTagDiscovered = null
        mainHandler.post(callback)
    }

    override fun onActivityResumed(activity: Activity) {
        resumedActivity = WeakReference(activity)
        if (readerModeWanted) startReaderMode(activity)
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
}
