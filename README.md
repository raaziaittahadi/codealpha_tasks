# codealpha_tasks
**📱 Task 1: AI Language Translator (Android Application)**
This is a modern Android application designed to provide seamless text translation between multiple languages, developed using industry-standard tools and practices:
Programming Language & Architecture: Built using Kotlin, the application strictly follows the MVVM (Model-View-ViewModel) clean architecture pattern. This ensures a modular, highly testable code structure and smooth application performance.
API Integration (Translation): Real-time, accurate translation capabilities are powered by integrating the LibreTranslate REST API.
Local Database (Room DB): To ensure a seamless user experience, an Android Room Database is implemented locally. This securely stores every translation request, allowing users to access their Translation History offline without an active internet connection.
Text-to-Speech (TTS): The application utilizes Android’s native TTS engine. When a user clicks the speaker icon, the app reads out the translated text aloud with the correct native pronunciation.
User Interface (UI): Designed according to Material 3 guidelines, the application features a polished layout including a quick-action Swap Button with smooth transitions and a native Light and Dark Mode toggle.

 ✨ Key Features
🌐 Advanced Translation:** Powered by LibreTranslate REST API integration for accurate results.

🗣️ Multi-Language Support:** English, Urdu, Hindi, Arabic, French, Spanish, Chinese, and German.

🔄 Swap Languages:** One-tap language swap with smooth animation and instant configuration reset.

📜 Translation History:** Saved locally using Android **Room Database** so users can access past lookups offline.

🔊 Text-to-Speech (TTS):** Built-in Android TTS engine to speak out translated text with correct pronunciation.

🌗 Material 3 UI:** Beautiful, modern interface with native Light and Dark mode UI toggle.


🔍 **Task 2: Live Object Tracking System (Computer Vision)**
This is an advanced Artificial Intelligence and Computer Vision application built to dynamically detect, classify, and track physical entities via a live webcam feed.
Core Algorithm (YOLOv8): The project is powered by the state-of-the-art YOLOv8 (You Only Look Once) machine learning algorithm developed by Ultralytics. This model is globally recognized for its exceptional speed and high accuracy in real-time edge computing.
Real-Time Processing: Operating on Python 3.11, the script captures video feed frame-by-frame via OpenCV and executes localized, low-latency inference directly on the machine.
Detection & Bounding Boxes: As soon as an item (such as a person, phone, laptop, or vehicle) enters the frame, the model instantly overlays a dynamic Bounding Box around it. Each box is paired with an object Label and a Confidence Score (the percentage indicating the model's accuracy level).
Optimization: The script incorporates environment and framework optimizations to prevent video lagging, ensuring stable frame rates (FPS) and smooth visual tracking on local hardware.

Key Features
⚡ Live Tracking:* Instantly detects persons, vehicles, electronic appliances, and daily common objects via a live webcam feed.

🤖 State-of-the-Art ML:* Utilizes *YOLOv8 (You Only Look Once)* algorithm from Ultralytics for real-time edge processing.

📦 Bounding Boxes & Labels:* Dynamic visual boxes that track objects smoothly across frames with real-time confidence scores.

⚙️ Hardware Optimized:* Configured using Python 3.11 with localized DLL and framework optimizations for stable frame rates.
