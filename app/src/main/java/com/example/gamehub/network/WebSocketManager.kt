package com.example.gamehub.network

import android.util.Log
import com.google.gson.Gson
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

object WebSocketManager {
    private const val WS_URL = "ws://192.168.0.61:8080/ws"

    private var stompClient: StompClient? = null
    private val gson = Gson()

    private var privateMessageCallback: ((String) -> Unit)? = null
    private var messageDisposable: io.reactivex.disposables.Disposable? = null
    private var friendRequestDisposable: io.reactivex.disposables.Disposable? = null

    fun connect() {
        if (stompClient?.isConnected == true) {
            Log.d("GameHub", "WebSocket already connected")
            return
        }

        val token = TokenManager.getToken() ?: return

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL)

        val headers = mutableListOf<ua.naiksoftware.stomp.dto.StompHeader>()
        headers.add(ua.naiksoftware.stomp.dto.StompHeader("Authorization", token))

        stompClient?.connect(headers)

        stompClient?.lifecycle()?.subscribe { event ->
            when (event.type) {
                LifecycleEvent.Type.OPENED -> {
                    subscribePrivateMessagesInternal()

                    Log.d("GameHub", "WebSocket connected")
                }
                LifecycleEvent.Type.ERROR -> Log.e("GameHub", "WebSocket error: ${event.exception}")
                LifecycleEvent.Type.CLOSED -> Log.d("GameHub", "WebSocket closed")
                else -> {}
            }
        }
    }

    private fun subscribePrivateMessagesInternal() {
        if (messageDisposable?.isDisposed == false) {
            Log.d("GameHub", "Already subscribed to private messages")
            return
        }
        val topic = stompClient?.topic("/user/queue/messages")

        Log.d("GameHub", "stompClient = $stompClient")
        Log.d("GameHub", "topic = $topic")

        messageDisposable = topic?.subscribe { message ->

            Log.d(
                "GameHub",
                "Received WS message: ${message.payload}"
            )

            privateMessageCallback?.invoke(
                message.payload
            )
        }

        Log.d("GameHub", "messageDisposable = $messageDisposable")

    }
    fun subscribePrivateMessages(onMessage: (String) -> Unit) {
        privateMessageCallback = onMessage

        if (stompClient?.isConnected == true) {
            subscribePrivateMessagesInternal()
        } else {
            connect()
        }
    }

    fun sendPrivateMessage(senderId: String, receiverId: String, content: String) {
        val messageJson = gson.toJson(
            mapOf(
                "senderId" to senderId,
                "receiverId" to receiverId,
                "content" to content
            )
        )
        stompClient
            ?.send("/app/chat.private", messageJson)
            ?.subscribe(
                {
                    Log.d("GameHub", "Message sent successfully")
                },
                { error ->
                    Log.e("GameHub", "Failed to send message", error)
                }
            )
    }

    fun subscribeFriendRequests(onFriendRequest: (String) -> Unit) {
        friendRequestDisposable?.dispose()
        friendRequestDisposable = stompClient?.topic("/user/queue/friend-request")?.subscribe { message ->
            onFriendRequest(message.payload)
        }
    }

    fun ensureConnected() {
        if (stompClient?.isConnected != true) {
            Log.d("GameHub", "WebSocket not connected, reconnecting...")
            connect()
        }
    }

    fun disconnect() {
        stompClient?.disconnect()
    }
}