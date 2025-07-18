```markdown
# WiFiHarvest

An Android app for scanning and logging nearby Wi-Fi networks along with their GPS coordinates.

WiFiHarvest uses `WifiManager` to gather SSID and BSSID information from surrounding access points and tags each scan result with real-world latitude and longitude using the `FusedLocationProviderClient`. This data is then displayed in a live scrollable list within the app.

---

## 📱 Features

- 📡 Scans for visible Wi-Fi SSIDs and BSSIDs
- 🌍 Logs GPS coordinates (lat/lng) for each result
- 🧠 Filters out duplicate results by BSSID and location proximity
- 🗂 Displays live log feed with SSID, BSSID, and coordinates
- ⏱ Rescans every 5 seconds until manually stopped
- 🛑 Manual Start/Stop buttons
- 🛡 Runtime permission request for location access

---

## 🔧 Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/YOUR_USERNAME/WiFiHarvest.git
   cd WiFiHarvest
   ```

2. Open in Android Studio

3. Ensure you have:
   - Android Studio Hedgehog or newer
   - Gradle 8.6
   - AGP 8.3.0
   - JDK 17

4. Connect an Android device or emulator with:
   - Location services enabled
   - Wi-Fi turned on

---

## ⚙️ Permissions Required

In `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
```

---

## 📸 Screenshots

> _(Drop screenshots here if you have them — like the one you posted earlier)_  
> Example:
> ![WiFiHarvest Demo](screenshots/main-ui.jpg)

---

## 📦 Dependencies

- `androidx.core:core-ktx`
- `androidx.appcompat:appcompat`
- `com.google.android.material:material`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`
- `com.squareup.okhttp3:okhttp`
- `com.google.android.gms:play-services-location`

---

## 📍 Future Enhancements (PRs welcome!)

- Export logs to CSV or JSON
- Background scanning service
- Signal strength mapping
- Custom map overlay view
- Wardriving drive mode with auto-logging

---

## 🛡 Disclaimer

This tool is for **educational and lawful network reconnaissance** only. Do not use it to scan networks you are not authorized to analyze.

---

## 🧑‍💻 Author

Built by [Your Name]  
MIT License
```
