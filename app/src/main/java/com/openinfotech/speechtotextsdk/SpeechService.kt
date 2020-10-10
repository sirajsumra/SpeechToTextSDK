package com.openinfotech.speechtotextsdk


/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.R
import android.app.Service
import android.content.Intent
import android.os.AsyncTask
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import com.google.auth.Credentials
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.*
import com.google.protobuf.ByteString
import io.grpc.*
import io.grpc.ClientInterceptors.CheckedForwardingClientCall
import io.grpc.internal.DnsNameResolverProvider
import io.grpc.stub.StreamObserver
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import java.util.concurrent.TimeUnit


class SpeechService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
      return null
    }
    /*interface Listener {
        *//**
         * Called when a new piece of text was recognized by the Speech API.
         *
         * @param text    The text.
         * @param isFinal `true` when the API finished processing audio.
         *//*
        fun onSpeechRecognized(text: String?, isFinal: Boolean)
    }

    private val mBinder: SpeechBinder = SpeechBinder()
    private val mListeners = ArrayList<Listener>()

    @Volatile
    private var mAccessTokenTask: AccessTokenTask? = null
    private var mApi: SpeechGrpc.SpeechStub? = null
    private val mResponseObserver: StreamObserver<StreamingRecognizeResponse> =
        object : StreamObserver<StreamingRecognizeResponse> {
            override fun onNext(response: StreamingRecognizeResponse) {
                var text: String? = null
                var isFinal = false
                if (response.resultsCount > 0) {
                    val result = response.getResults(0)
                    isFinal = result.isFinal
                    if (result.alternativesCount > 0) {
                        val alternative = result.getAlternatives(0)
                        text = alternative.transcript
                    }
                }
                if (text != null) {
                    for (listener in mListeners) {
                        listener.onSpeechRecognized(text, isFinal)
                    }
                }
            }

            override fun onError(t: Throwable) {
                Log.e(TAG, "Error calling the API.", t)
            }

            override fun onCompleted() {
                Log.i(TAG, "API completed.")
            }
        }
    private val mFileResponseObserver: StreamObserver<RecognizeResponse> =
        object : StreamObserver<RecognizeResponse> {
            override fun onNext(response: RecognizeResponse) {
                var text: String? = null
                if (response.resultsCount > 0) {
                    val result = response.getResults(0)
                    if (result.alternativesCount > 0) {
                        val alternative = result.getAlternatives(0)
                        text = alternative.transcript
                    }
                }
                if (text != null) {
                    for (listener in mListeners) {
                        listener.onSpeechRecognized(text, true)
                    }
                }
            }

            override fun onError(t: Throwable) {
                Log.e(TAG, "Error calling the API.", t)
            }

            override fun onCompleted() {
                Log.i(TAG, "API completed.")
            }
        }
    private var mRequestObserver: StreamObserver<StreamingRecognizeRequest>? = null
    override fun onCreate() {
        super.onCreate()
        mHandler = Handler()
        fetchAccessToken()
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler!!.removeCallbacks(mFetchAccessTokenRunnable)
        mHandler = null
        // Release the gRPC channel.
        if (mApi != null) {
            val channel = mApi.getChannel() as ManagedChannel
            if (channel != null && !channel.isShutdown) {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Error shutting down the gRPC channel.", e)
                }
            }
            mApi = null
        }
    }

    private fun fetchAccessToken() {
        if (mAccessTokenTask != null) {
            return
        }
        mAccessTokenTask = AccessTokenTask()
        mAccessTokenTask!!.execute()
    }

    private val defaultLanguageCode: String
        private get() {
            val locale = Locale.getDefault()
            val language = StringBuilder(locale.language)
            val country = locale.country
            if (!TextUtils.isEmpty(country)) {
                language.append("-")
                language.append(country)
            }
            return language.toString()
        }

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    fun addListener(listener: Listener) {
        mListeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        mListeners.remove(listener)
    }

    *//**
     * Starts recognizing speech audio.
     *
     * @param sampleRate The sample rate of the audio.
     *//*
    fun startRecognizing(sampleRate: Int) {
        if (mApi == null) {
            Log.w(TAG, "API not ready. Ignoring the request.")
            return
        }
        // Configure the API
        mRequestObserver = mApi.streamingRecognize(mResponseObserver)
        mRequestObserver!!.onNext(
            StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(
                    StreamingRecognitionConfig.newBuilder()
                        .setConfig(
                            RecognitionConfig.newBuilder()
                                .setLanguageCode(defaultLanguageCode)
                                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                .setSampleRateHertz(sampleRate)
                                .build()
                        )
                        .setInterimResults(true)
                        .setSingleUtterance(true)
                        .build()
                )
                .build()
        )
    }

    *//**
     * Recognizes the speech audio. This method should be called every time a chunk of byte buffer
     * is ready.
     *
     * @param data The audio data.
     * @param size The number of elements that are actually relevant in the `data`.
     *//*
    fun recognize(data: ByteArray?, size: Int) {
        if (mRequestObserver == null) {
            return
        }
        // Call the streaming recognition API
        mRequestObserver!!.onNext(
            StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, 0, size))
                .build()
        )
    }

    *//**
     * Finishes recognizing speech audio.
     *//*
    fun finishRecognizing() {
        if (mRequestObserver == null) {
            return
        }
        mRequestObserver!!.onCompleted()
        mRequestObserver = null
    }

    *//**
     * Recognize all data from the specified [InputStream].
     *
     * @param stream The audio data.
     *//*
    fun recognizeInputStream(stream: InputStream?) {
        try {
            mApi.recognize(
                RecognizeRequest.newBuilder()
                    .setConfig(
                        RecognitionConfig.newBuilder()
                            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                            .setLanguageCode("en-US")
                            .setSampleRateHertz(16000)
                            .build()
                    )
                    .setAudio(
                        RecognitionAudio.newBuilder()
                            .setContent(ByteString.readFrom(stream))
                            .build()
                    )
                    .build(),
                mFileResponseObserver
            )
        } catch (e: IOException) {
            Log.e(TAG, "Error loading the input", e)
        }
    }

    private inner class SpeechBinder : Binder() {
        val service: SpeechService
            get() = this@SpeechService
    }

    private val mFetchAccessTokenRunnable = Runnable { fetchAccessToken() }

    private inner class AccessTokenTask :
        AsyncTask<Void?, Void?, AccessToken?>() {
        protected override fun doInBackground(vararg voids: Void): AccessToken? {
            val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
            val tokenValue = prefs.getString(PREF_ACCESS_TOKEN_VALUE, null)
            val expirationTime = prefs.getLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, -1)

            // Check if the current token is still valid for a while
            if (tokenValue != null && expirationTime > 0) {
                if (expirationTime
                    > System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TOLERANCE
                ) {
                    return AccessToken(tokenValue, Date(expirationTime))
                }
            }

            // ***** WARNING *****
            // In this sample, we load the credential from a JSON file stored in a raw resource
            // folder of this client app. You should never do this in your app. Instead, store
            // the file in your server and obtain an access token from there.
            // *******************
            val stream = resources.openRawResource(R.raw.credential)
            try {
                val credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(SCOPE)
                val token = credentials.refreshAccessToken()
                prefs.edit()
                    .putString(PREF_ACCESS_TOKEN_VALUE, token.tokenValue)
                    .putLong(
                        PREF_ACCESS_TOKEN_EXPIRATION_TIME,
                        token.expirationTime.time
                    )
                    .apply()
                return token
            } catch (e: IOException) {
                Log.e(TAG, "Failed to obtain access token.", e)
            }
            return null
        }

        override fun onPostExecute(accessToken: AccessToken?) {
            mAccessTokenTask = null
            val channel: ManagedChannel = OkHttpChannelProvider()
                .builderForAddress(HOSTNAME, PORT)
                .nameResolverFactory(DnsNameResolverProvider())
                .intercept(
                    GoogleCredentialsInterceptor(
                        GoogleCredentials(accessToken)
                            .createScoped(SCOPE)
                    )
                )
                .build()
            mApi = SpeechGrpc.newStub(channel)

            // Schedule access token refresh before it expires
            if (mHandler != null) {
                mHandler!!.postDelayed(
                    mFetchAccessTokenRunnable,
                    Math.max(
                        accessToken!!.expirationTime.time
                                - System.currentTimeMillis()
                                - ACCESS_TOKEN_FETCH_MARGIN,
                        ACCESS_TOKEN_EXPIRATION_TOLERANCE.toLong()
                    )
                )
            }
        }
    }

    *//**
     * Authenticates the gRPC channel using the specified [GoogleCredentials].
     *//*
    private class GoogleCredentialsInterceptor internal constructor(private val mCredentials: Credentials) :
        ClientInterceptor {
        private var mCached: Metadata? = null
        private var mLastMetadata: Map<String, List<String>>? = null
        override fun <ReqT, RespT> interceptCall(
            method: MethodDescriptor<ReqT, RespT>, callOptions: CallOptions,
            next: Channel
        ): ClientCall<ReqT, RespT> {
            return object : CheckedForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)
            ) {
                @Throws(StatusException::class)
                override fun checkedStart(responseListener: Listener<RespT>, headers: Metadata) {
                    var cachedSaved: Metadata?
                    val uri = serviceUri(next, method)
                    synchronized(this) {
                        val latestMetadata =
                            getRequestMetadata(uri)
                        if (mLastMetadata == null || mLastMetadata !== latestMetadata) {
                            mLastMetadata = latestMetadata
                            mCached =
                                toHeaders(
                                    mLastMetadata
                                )
                        }
                        cachedSaved = mCached
                    }
                    headers.merge(cachedSaved)
                    delegate().start(responseListener, headers)
                }
            }
        }

        *//**
         * Generate a JWT-specific service URI. The URI is simply an identifier with enough
         * information for a service to know that the JWT was intended for it. The URI will
         * commonly be verified with a simple string equality check.
         *//*
        @Throws(StatusException::class)
        private fun serviceUri(channel: Channel, method: MethodDescriptor<*, *>): URI {
            val authority = channel.authority()
                ?: throw Status.UNAUTHENTICATED
                    .withDescription("Channel has no authority")
                    .asException()
            // Always use HTTPS, by definition.
            val scheme = "https"
            val defaultPort = 443
            val path = "/" + MethodDescriptor.extractFullServiceName(method.fullMethodName)
            var uri: URI
            uri = try {
                URI(scheme, authority, path, null, null)
            } catch (e: URISyntaxException) {
                throw Status.UNAUTHENTICATED
                    .withDescription("Unable to construct service URI for auth")
                    .withCause(e).asException()
            }
            // The default port must not be present. Alternative ports should be present.
            if (uri.port == defaultPort) {
                uri = removePort(uri)
            }
            return uri
        }

        @Throws(StatusException::class)
        private fun removePort(uri: URI): URI {
            return try {
                URI(
                    uri.scheme, uri.userInfo, uri.host, -1 *//* port *//*,
                    uri.path, uri.query, uri.fragment
                )
            } catch (e: URISyntaxException) {
                throw Status.UNAUTHENTICATED
                    .withDescription("Unable to construct service URI after removing port")
                    .withCause(e).asException()
            }
        }

        @Throws(StatusException::class)
        private fun getRequestMetadata(uri: URI): Map<String, List<String>> {
            return try {
                mCredentials.getRequestMetadata(uri)
            } catch (e: IOException) {
                throw Status.UNAUTHENTICATED.withCause(e).asException()
            }
        }

        companion object {
            private fun toHeaders(metadata: Map<String, List<String>>?): Metadata {
                val headers = Metadata()
                if (metadata != null) {
                    for (key in metadata.keys) {
                        val headerKey = Metadata.Key.of(
                            key, Metadata.ASCII_STRING_MARSHALLER
                        )
                        for (value in metadata[key]!!) {
                            headers.put(headerKey, value)
                        }
                    }
                }
                return headers
            }
        }
    }

    companion object {
        private const val TAG = "SpeechService"
        private const val PREFS = "SpeechService"
        private const val PREF_ACCESS_TOKEN_VALUE = "access_token_value"
        private const val PREF_ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time"

        *//** We reuse an access token if its expiration time is longer than this.  *//*
        private const val ACCESS_TOKEN_EXPIRATION_TOLERANCE = 30 * 60 * 1000 // thirty minutes

        *//** We refresh the current access token before it expires.  *//*
        private const val ACCESS_TOKEN_FETCH_MARGIN = 60 * 1000 // one minute
        val SCOPE = listOf("https://www.googleapis.com/auth/cloud-platform")
        private const val HOSTNAME = "speech.googleapis.com"
        private const val PORT = 443
        private var mHandler: Handler? = null
        fun from(binder: IBinder): SpeechService {
            return (binder as SpeechBinder).service
        }
    }*/
}