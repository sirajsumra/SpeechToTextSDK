package com.openinfotech.speechtotextsdk

/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.Manifest
import android.R
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.view.*
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import java.util.*


class SpeechActivity : AppCompatActivity(), MessageDialogFragment.Listener {
    override fun onMessageDialogDismissed() {

    }
    /*private var mSpeechService: SpeechService? = null
    private var mVoiceRecorder: VoiceRecorder? = null
    private val mVoiceCallback: VoiceRecorder.Callback = object : VoiceRecorder.Callback() {
        override fun onVoiceStart() {
            showStatus(true)
            mSpeechService?.startRecognizing(mVoiceRecorder!!.sampleRate)
        }

        override fun onVoice(data: ByteArray?, size: Int) {
            mSpeechService?.recognize(data, size)
        }

        override fun onVoiceEnd() {
            showStatus(false)
            mSpeechService?.finishRecognizing()
        }
    }

    // Resource caches
    private var mColorHearing = 0
    private var mColorNotHearing = 0

    // View references
    private var mStatus: TextView? = null
    private var mText: TextView? = null
    private var mAdapter: ResultAdapter? = null
    private var mRecyclerView: RecyclerView? = null
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            mSpeechService = SpeechService.from(binder)
            mSpeechService.addListener(mSpeechServiceListener)
            mStatus!!.visibility = View.VISIBLE
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mSpeechService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val resources = resources
        val theme = theme
        mColorHearing = ResourcesCompat.getColor(resources, R.color.status_hearing, theme)
        mColorNotHearing = ResourcesCompat.getColor(resources, R.color.status_not_hearing, theme)
        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)
        mStatus = findViewById<View>(R.id.status) as TextView
        mText = findViewById<View>(R.id.text) as TextView
        mRecyclerView = findViewById<View>(R.id.recycler_view) as RecyclerView
        mRecyclerView.setLayoutManager(LinearLayoutManager(this))
        val results = savedInstanceState?.getStringArrayList(STATE_RESULTS)
        mAdapter = ResultAdapter(results)
        mRecyclerView.setAdapter(mAdapter)
    }

    override fun onStart() {
        super.onStart()

        // Prepare Cloud Speech API
        bindService(Intent(this, SpeechService::class.java), mServiceConnection, BIND_AUTO_CREATE)

        // Start listening to voices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceRecorder()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECORD_AUDIO
            )
        ) {
            showPermissionMessageDialog()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    override fun onStop() {
        // Stop listening to voice
        stopVoiceRecorder()

        // Stop Cloud Speech API
        mSpeechService?.removeListener(mSpeechServiceListener)
        unbindService(mServiceConnection)
        mSpeechService = null
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mAdapter != null) {
            outState.putStringArrayList(STATE_RESULTS, mAdapter!!.results)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (permissions.size == 1 && grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecorder()
            } else {
                showPermissionMessageDialog()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder!!.stop()
        }
        mVoiceRecorder = VoiceRecorder(mVoiceCallback)
        mVoiceRecorder!!.start()
    }

    private fun stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder!!.stop()
            mVoiceRecorder = null
        }
    }

    private fun showPermissionMessageDialog() {
        MessageDialogFragment
            .newInstance("permission_message")
            .show(supportFragmentManager, FRAGMENT_MESSAGE_DIALOG)
    }

    private fun showStatus(hearingVoice: Boolean) {
        runOnUiThread { mStatus!!.setTextColor(if (hearingVoice) mColorHearing else mColorNotHearing) }
    }

    override fun onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_RECORD_AUDIO_PERMISSION
        )
    }

    private val mSpeechServiceListener: SpeechService.Listener = object : MessageDialogFragment.Listener() {
        fun onSpeechRecognized(text: String?, isFinal: Boolean) {
            if (isFinal) {
                mVoiceRecorder!!.dismiss()
            }
            if (mText != null && !TextUtils.isEmpty(text)) {
                runOnUiThread {
                    if (isFinal) {
                        mText?.text = null
                        mAdapter!!.addResult(text)
                        mRecyclerView?.smoothScrollToPosition(0)
                    } else {
                        mText!!.text = text
                    }
                }
            }
        }

        override fun onMessageDialogDismissed() {
            TODO("Not yet implemented")
        }
    }

    private class ViewHolder internal constructor(inflater: LayoutInflater, parent: ViewGroup?) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.item_result, parent, false)) {
        var text: TextView

        init {
            text = itemView.findViewById(R.id.text)
        }
    }

    private class ResultAdapter internal constructor(results: ArrayList<String?>?) :
        RecyclerView.Adapter<ViewHolder?>() {
        val results = ArrayList<String?>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text.text = results[position]
        }

        fun addResult(result: String?) {
            results.add(0, result)
            notifyItemInserted(0)
        }

        init {
            results?.addAll(results)
        }

        override fun getItemCount(): Int {
            return results.size
        }
    }

    companion object {
        private const val FRAGMENT_MESSAGE_DIALOG = "message_dialog"
        private const val STATE_RESULTS = "results"
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 1
    }*/
}