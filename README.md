# GamesHub Android

An Android mobile app for GamesHub, a Steam-style game platform built with Kotlin.

## Tech Stack

- Kotlin
- Android SDK (min SDK 24, target SDK 36)
- Retrofit2 + Moshi (API calls)
- Coil (image loading)
- ExoPlayer / Media3 (video/trailer playback)
- WebView (HTML game descriptions)
- ViewPager2 (banner carousel)
- StompProtocolAndroid (WebSocket chat)
- Stripe Android SDK (payments)
- WebSocket + RxJava (real-time messaging)

## Features

- 🎮 Steam-style game store
- 🎠 Banner carousel with auto-scroll
- 🏷️ Genre filter with pagination
- 🔍 Real-time game search
- 🎬 Game trailer playback (HLS)
- 📸 Screenshot gallery
- 🌐 Detailed HTML game descriptions (WebView)
- 🛒 Shopping cart & Stripe payment
- 👥 Friend system
- 💬 Real-time private & group chat (WebSocket)
- 🖥️ Community servers
- 👤 User profile with avatar

## Project Structure

app/
├── adapters/
│   ├── BannerAdapter
│   ├── GameAdapter
│   ├── NewReleaseGameAdapter
│   ├── ScreenshotAdapter
│   └── SearchGameAdapter
├── fragment/
│   ├── CommunityFragment
│   ├── FriendsFragment
│   ├── GamesFragment
│   ├── ProfileFragment
│   └── SearchFragment
├── models/
│   ├── CommunityServer
│   ├── Friendship
│   ├── Game
│   ├── LoginRequest
│   ├── LoginResponse
│   ├── Message
│   ├── Order
│   ├── SignupRequest
│   └── User
├── network/
│   ├── ApiService
│   ├── CartManager
│   ├── RetrofitInstance
│   ├── TokenManager
│   └── WebSocketManager
├── ui.theme/
├── CartActivity
├── ChatActivity
├── GameDetailsActivity
├── GroupChatActivity
├── LoginActivity
└── MainActivity


## Setup

### Prerequisites
- Android Studio
- Android device or emulator (API 24+)

### Configuration

1. Open `RetrofitInstance.kt` and set your backend URL:
```kotlin
private const val BASE_URL = "http://YOUR_IP:8080/"
```

2. Add to `local.properties`:
IMGBB_API_KEY=your_imgbb_key
STRIPE_PUBLISHABLE_KEY=your_stripe_publishable_key


### Run
1. Open project in Android Studio
2. Connect device or start emulator
3. Click Run ▶️

## Screenshots

> _(Add screenshots here)_

## Related

- [GamesHub Backend](link-to-backend-repo)

