# PCRemote 📱💻

A powerful Android application that allows you to control your PC remotely with real-time input, screen streaming, and advanced gesture support.

---

## 🚀 Overview

PCRemote is a Kotlin-based Android app that connects to a desktop server and enables full remote control of a PC over a local network.

It features:

* Real-time input transmission using a custom binary protocol
* Live screen streaming
* Authentication and license system
* Advanced gesture-based controls

---

## ✨ Features

### 🖱️ Remote Control

* Touchpad-style mouse control
* Multi-touch gestures (scroll, zoom, shortcuts)
* Keyboard input + key combinations
* Media control (volume, playback)

### 🖥️ Screen Streaming

* Real-time PC screen streaming
* Optimized decoding (RGB_565)
* Resolution-aware rendering

### 🔐 Authentication & Licensing

* Login & registration system
* License activation & validation
* Premium feature unlocking

### ⚡ Networking

* TCP socket communication
* Custom binary protocol for low latency
* Background coroutine processing

---

## 🛠️ Tech Stack

* **Kotlin**
* **Jetpack Compose**
* **MVVM Architecture**
* **Coroutines + Flow**
* **Socket Programming (TCP)**
* **Gson / JSON APIs**
* **OkHttp (API calls)**

---

## 📱 Screenshots

<p align="center">
  <img src="screenshots/connect.png" width="250"/>
  <img src="screenshots/main.png" width="250"/>
  <img src="screenshots/remote.png" width="250"/>
</p>

---

## 🧠 Architecture

The app follows an MVVM-inspired structure:

* **View (UI):** Compose screens (Connect, Remote, Keyboard, Media)
* **ViewModel:** Manages state, networking, and business logic
* **Data Layer:** Auth, License, Socket, Streaming managers

---

## 📂 Key Components

* `MainViewModel` → central state & connection logic
* `SocketManager` → handles TCP communication
* `BinaryProtocol` → defines command packets
* `ScreenStreamManager` → real-time frame streaming
* `AuthManager` → authentication system
* `LicenseManager` → license validation

---

## ⚙️ Setup

```bash
git clone https://github.com/YOUR_USERNAME/PCRemote.git
```

1. Open in Android Studio
2. Run on device/emulator
3. Enter PC IP address and connect

---

## 🚧 Future Improvements

* 🔒 End-to-end encryption
* 📡 Internet-based connection (not just LAN)
* 📂 File transfer support
* 🖥️ Desktop server UI improvements
* ☁️ Cloud sync

---

## 💼 Portfolio Value

This project demonstrates:

* Real-time networking
* Custom protocol design
* Advanced UI/UX with gestures
* State management with Flow
* Full-stack thinking (client + server)

---

## 👨‍💻 Author

**TheGaemisYash**
