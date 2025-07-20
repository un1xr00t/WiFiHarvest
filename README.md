# 📶 WiFiHarvest

An Android app for scanning and logging nearby Wi-Fi networks with **real-time GPS** and **physical address tagging**, including **live map pins**, **dynamic feed updates**, and **CSV export**.

WiFiHarvest uses `WifiManager` to gather SSID/BSSID info from surrounding access points and maps them to exact real-world street addresses using `FusedLocationProviderClient`. Everything is synced across tabs via `LiveData`, and all logs are exportable in a tap.

---

## 📱 Features

- 📡 Scans for nearby Wi-Fi SSIDs and BSSIDs  
- 🌐 Uses fresh GPS data per scan — no stale coordinates  
- 🏠 Converts GPS to street address (reverse geocoding)  
- 🗺️ Drops real-time map pins for each unique access point  
- 🔁 Live feed updates as new networks are discovered  
- 📊 CSV export includes SSID, BSSID, GPS, and Address  
- 🧠 Shared ViewModel for data sync across fragments  
- 🚫 Filters duplicate BSSIDs across scan cycles  
- ⏱ Scans every 3 seconds until manually stopped  
- 🔒 Fine location permissions w/ runtime handling  
- 🎛 Manual Start/Stop scan buttons  

---

## 🧪 Architecture Highlights

- `SharedWifiViewModel` stores all Wi-Fi networks  
- Coroutine-based `WifiScanner` runs every 3 seconds  
- `getFreshLocation()` uses `suspendCoroutine` for live GPS  
- `LocationHelper` resolves GPS to street addresses  
- MapFragment observes LiveData and updates markers  
- LiveFeedFragment uses `WifiLogAdapter` with dynamic address display  
- ExportHelper creates and shares .CSV with all fields  
- Logcat shows scan status and debug data (e.g. BSSID count, GPS, address)

---

## 🔧 Setup Instructions

1. **Clone the repo**
   ```bash
   git clone https://github.com/YOUR_USERNAME/WiFiHarvest.git
   cd WiFiHarvest
   ```

2. **Open in Android Studio**

3. **Make sure you have:**
   - Android Studio Hedgehog or newer  
   - JDK 17  
   - Gradle 8.6  
   - AGP 8.3.0  

4. **Set up your Google Maps API Key**  
   Create this file:
   ```
   app/src/main/res/values/google_maps_api.xml
   ```
   With:
   ```xml
   <resources>
       <string name="google_maps_key">YOUR_API_KEY</string>
   </resources>
   ```

5. **Run on a device/emulator** with:
   - Location services enabled  
   - Wi-Fi turned on  
   - Internet connection for map tiles  

---

## ⚙️ Required Permissions

In `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```
---

## 📦 Dependencies

- `androidx.core:core-ktx`  
- `androidx.appcompat:appcompat`  
- `androidx.lifecycle:lifecycle-viewmodel`  
- `androidx.recyclerview:recyclerview`  
- `com.google.android.material:material`  
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`  
- `com.google.android.gms:play-services-location`  
- `com.google.android.gms:play-services-maps`  

---

## 🚀 Future Enhancements (PRs welcome!)

- 🔄 Background scanning service  
- 🌈 Heatmap overlay for signal strength  
- 🚗 Wardriving mode with auto-logging  
- ⚠️ Threat scoring (e.g. open networks, weak encryption)  
- 🧬 MAC vendor resolution  

---

## 🛡 Disclaimer

This tool is for **educational use and authorized network analysis** only.  
Do not use it to collect data on networks you are not permitted to scan.

---

## 🧑‍💻 Author

Built by [Your Name or Alias]  
MIT License
****
