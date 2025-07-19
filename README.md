# 📶 WiFiHarvest

An Android app for scanning and logging nearby Wi-Fi networks with real-time GPS tagging and live map integration.

WiFiHarvest uses `WifiManager` to gather SSID and BSSID data from surrounding access points and pins their exact physical locations on a live map using `FusedLocationProviderClient`. All results are shown in a dynamic feed and synced across tabs using `LiveData` and `ViewModel`.

---

## 📱 Features

- 📡 Scans for nearby Wi-Fi SSIDs and BSSIDs  
- 🌍 Captures GPS coordinates (lat/lng) per scan  
- 🗺️ Displays access points on a Google Map with live pin drops  
- 🧠 Shared ViewModel for real-time data sync across fragments  
- 🔁 Live feed auto-updates as new networks are discovered  
- 🚫 Filters out duplicate BSSIDs to avoid noisy logs  
- ⏱ Rescans every 5 seconds while active  
- 🎛 Manual Start/Stop scan control  
- 🔒 Runtime permissions for fine location  

---

## 🧪 Architecture Highlights

- **Fragment-to-Fragment sync** via `SharedWifiViewModel`  
- **Coroutines-based scanner** runs every 5 seconds  
- **FusedLocationProviderClient** for accurate GPS fixes  
- **MapFragment** observes `LiveData` and drops markers live  
- **Custom `WifiScanListener` interface** for pin callbacks  
- **RecyclerView adapter** keeps the feed in sync  

---

## 🔧 Setup Instructions

1. **Clone the repo**
   ```bash
   git clone https://github.com/YOUR_USERNAME/WiFiHarvest.git
   cd WiFiHarvest
   ```

2. **Open in Android Studio**

3. **Ensure you have:**
   - Android Studio Hedgehog or newer  
   - JDK 17  
   - Gradle 8.6  
   - AGP 8.3.0  

4. **Add your Google Maps API key**  
   Create this file (not committed to Git):
   ```
   app/src/main/res/values/google_maps_api.xml
   ```
   With:
   ```xml
   <resources>
       <string name="google_maps_key">YOUR_API_KEY</string>
   </resources>
   ```

5. **Connect a device or emulator** with:
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

## 📸 Screenshots

> _(Drop screenshots here)_  
> Example:  
> ![WiFiHarvest UI](screenshots/live-feed.jpg)  
> ![Map Pins](screenshots/map-pins.jpg)  

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

- Export Wi-Fi logs to CSV or JSON  
- Background scanning service  
- Heatmap overlay for RSSI strength  
- Wardriving mode with auto-logging  
- Threat scoring (e.g. open networks or weak encryption)  
- MAC vendor resolution  

---

## 🛡 Disclaimer

This tool is for **educational use and authorized network analysis** only.  
Do not use it to collect data on networks you are not permitted to scan.

---

## 🧑‍💻 Author

Built by [Your Name or Alias]  
MIT License
