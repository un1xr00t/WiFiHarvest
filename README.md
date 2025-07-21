# 📶 WiFiHarvest v1.0
A comprehensive Android wardriving application for scanning, mapping, and analyzing nearby Wi-Fi networks with real-time GPS tagging, interactive maps, and beautiful analytics dashboard.

WiFiHarvest leverages Android's `WifiManager` and `FusedLocationProviderClient` to discover Wi-Fi access points, geocode their physical addresses, and visualize network data through interactive charts and live mapping. All results sync seamlessly across tabs using modern Android architecture with `LiveData` and `ViewModel`.

---

## 🚀 Features

### 📡 **Advanced WiFi Scanning**
- Real-time Wi-Fi network discovery with SSID, BSSID, and signal strength
- Smart Android throttling management (respects 4 scans per 2-minute limit)
- 10-second scan intervals for rapid network discovery
- GPS-tagged coordinates with 100-meter accuracy filtering
- Intelligent location clustering to prevent duplicate entries
- Background scanning service with persistent notifications

### 🗺️ **Interactive Mapping**
- Live Google Maps integration with real-time pin dropping
- Color-coded markers based on signal strength (red=poor, green=excellent)
- Detailed info windows showing complete network information
- Multiple map types (Normal, Satellite, Hybrid, Terrain)
- Auto-camera movement following scanning progress
- Floating action buttons for map controls and centering

### 📊 **Beautiful Analytics Dashboard**
- Interactive signal strength distribution charts
- Security type analysis with intelligent SSID-based detection
- Discovery timeline visualization showing hourly network trends
- Top networks leaderboard with gold/silver/bronze rankings
- Real-time statistics cards with coverage metrics
- Comprehensive overview including total networks, unique SSIDs, and hidden networks

### 💾 **Data Management**
- **CSV Import/Export** with signal strength support
- User-selectable save locations via system file picker
- Native Android share sheet integration for easy data sharing
- Backward compatibility with legacy CSV formats
- Auto-save functionality with intelligent cleanup
- **Format**: `SSID, BSSID, Latitude, Longitude, Signal, Physical Address`

### 🌍 **Location Services**
- Automatic address geocoding using Google's reverse geocoding API
- Smart caching system to reduce API calls and improve performance
- Fallback address resolution with multiple quality tiers
- Rate-limited geocoding to respect API limits
- Physical address display alongside GPS coordinates

### 🎨 **Modern UI/UX**
- Stunning dark theme with neon green accent colors
- Material Design components with glassmorphism effects
- Smooth animations and responsive layouts
- Color-coded signal strength indicators throughout the app
- Professional network information display with visual signal bars

---

## 📱 App Structure

### **Live Feed Tab**
- Dynamic list of discovered networks with real-time updates
- Green-themed network information display
- Load CSV files from previous scans
- Export options with "Save to Device" and "Share" functionality
- Debug tools for development and troubleshooting

### **Map Tab**
- Interactive Google Maps with live network pin dropping
- Signal strength color coding on markers
- Comprehensive network details in info windows
- Map type switching and location centering controls

### **Analytics Tab**
- Beautiful data visualization with interactive charts
- Network discovery insights and signal strength analysis
- Security type distribution and coverage statistics
- Top performing networks leaderboard

---

## 🏗️ Architecture Highlights

- **MVVM Architecture** with `SharedWifiViewModel` for cross-fragment data sync
- **Foreground Service** for reliable background scanning
- **Coroutines-based** asynchronous operations for smooth performance
- **Singleton ViewModel** pattern ensuring consistent data across app components
- **Modern Android APIs** including `FusedLocationProviderClient` and `WifiManager`
- **Geocoding Pipeline** with smart caching and rate limiting
- **MPAndroidChart** integration for beautiful data visualization
- **File Provider** setup for secure file sharing

---

## 🔧 Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/un1xr00t/WiFiHarvest.git
   cd WiFiHarvest
   ```

2. **Open in Android Studio**
   - Android Studio Hedgehog (2023.1.1) or newer
   - JDK 17
   - Gradle 8.6+
   - AGP 8.3.0

3. **Configure Google Maps API**
   
   **Option A: Debug builds (recommended)**
   Create `local.properties` in project root:
   ```properties
   MAPS_API_KEY=YOUR_API_KEY_HERE
   ```
   
   **Option B: Release builds**
   Create `app/src/main/res/values/google_maps_api.xml`:
   ```xml
   <resources>
       <string name="google_maps_key">YOUR_API_KEY_HERE</string>
   </resources>
   ```

4. **Device Requirements**
   - Android 5.0 (API 21) or higher
   - Location services enabled
   - Wi-Fi capability
   - Internet connection for maps and geocoding

5. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   # or use Android Studio's Run button
   ```

---

## ⚙️ Permissions

The app requires the following permissions for full functionality:

**WiFi Scanning:**
- `android.permission.ACCESS_WIFI_STATE`
- `android.permission.CHANGE_WIFI_STATE`

**Location Services:**
- `android.permission.ACCESS_FINE_LOCATION`
- `android.permission.ACCESS_COARSE_LOCATION`
- `android.permission.ACCESS_BACKGROUND_LOCATION`

**Background Service:**
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_LOCATION`
- `android.permission.WAKE_LOCK`

**Data Management:**
- `android.permission.READ_EXTERNAL_STORAGE`
- `android.permission.WRITE_EXTERNAL_STORAGE`
- `android.permission.INTERNET`

**Network State:**
- `android.permission.ACCESS_NETWORK_STATE`

---

## 📦 Dependencies

### Core Android
- `androidx.core:core-ktx:1.12.0`
- `androidx.appcompat:appcompat:1.6.1`
- `androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2`
- `androidx.navigation:navigation-fragment-ktx:2.7.7`
- `com.google.android.material:material:1.11.0`

### Networking & Location
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3`
- `com.google.android.gms:play-services-location:21.0.1`
- `com.google.android.gms:play-services-maps:18.2.0`
- `com.squareup.okhttp3:okhttp:4.12.0`

### Data Visualization
- `com.github.PhilJay:MPAndroidChart:v3.1.0`

### Utilities
- `com.google.code.gson:gson:2.10.1`
- `androidx.annotation:annotation:1.7.1`

---

## 📸 Screenshots

<img width="1344" height="2992" alt="1000000551" src="https://github.com/user-attachments/assets/f5f900c6-3e6d-4bdf-93dd-c1a3a59442fa" />
<img width="1344" height="2992" alt="1000000550" src="https://github.com/user-attachments/assets/6337ff9a-3838-4969-926f-08c2a3d963f6" />
![1000000553](https://github.com/user-attachments/assets/b3abbb7b-94e6-4d67-a82b-fbb4ce2d1259)

---

## 🎯 Usage Guide

### **Basic Wardriving Workflow:**
1. **Grant Permissions** - Allow location and WiFi access when prompted
2. **Start Scanning** - Tap "Start Scanning" in the Live Feed tab
3. **Monitor Progress** - Watch networks appear in real-time with geocoded addresses
4. **View on Map** - Switch to Map tab to see network pins with signal strength colors
5. **Analyze Data** - Check Analytics tab for insights and statistics
6. **Export Results** - Use Export button to save or share your findings

### **CSV Data Format:**
```csv
SSID,BSSID,Latitude,Longitude,Signal,Physical address
"MyNetwork",AA:BB:CC:DD:EE:FF,42.987654,-87.123456,-45,"456 Oak Avenue, Springfield, IL"
```

---

## 🔮 Roadmap & Future Enhancements

### **Planned Features:**
- [ ] **Advanced Security Analysis** - WPA/WEP detection and vulnerability assessment
- [ ] **MAC Vendor Resolution** - Identify device manufacturers from BSSID
- [ ] **Heatmap Overlays** - Visual signal strength mapping
- [ ] **Export Formats** - KML, GPX, and JSON export options
- [ ] **Filtering & Search** - Advanced network filtering capabilities
- [ ] **Session Management** - Save and load scanning sessions
- [ ] **Statistics Enhancement** - More detailed analytics and insights
- [ ] **Offline Mode** - Cache maps for offline wardriving

### **Performance Optimizations:**
- [ ] **Database Integration** - SQLite for large dataset management
- [ ] **Background Sync** - Cloud storage integration
- [ ] **Battery Optimization** - Enhanced power management
- [ ] **Memory Management** - Improved handling of large network lists

---

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit your changes** (`git commit -m 'Add amazing feature'`)
4. **Push to branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

### **Areas for Contribution:**
- 🐛 Bug fixes and stability improvements
- 🎨 UI/UX enhancements
- 📊 New analytics features
- 🔧 Performance optimizations
- 📚 Documentation improvements

---

## 🛡️ Legal & Ethical Use

**⚠️ IMPORTANT DISCLAIMER:**

This application is designed for **educational purposes, authorized network analysis, and legitimate security research only**. Users are responsible for ensuring compliance with:

- Local and federal laws regarding network scanning
- Corporate policies and terms of service
- Ethical hacking guidelines and responsible disclosure practices

**DO NOT:**
- Scan networks without proper authorization
- Attempt to access or compromise discovered networks
- Use this tool for malicious purposes or illegal activities
- Violate privacy or data protection regulations

**WiFiHarvest is a passive scanning tool** - it only observes publicly broadcast network information and does not attempt to connect to or compromise any networks.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🧑‍💻 Author & Acknowledgments

**Built with ❤️ by the WiFiHarvest Team**

### **Special Thanks:**
- **MPAndroidChart** library for beautiful data visualization
- **Google Maps API** for interactive mapping capabilities
- **Android Open Source Project** for the robust platform
- **Community Contributors** for bug reports and feature suggestions

---

## 📞 Support & Contact

- 🐛 **Bug Reports**: [GitHub Issues](https://github.com/un1xr00t/WiFiHarvest/issues)
- 💡 **Feature Requests**: [GitHub Discussions](https://github.com/un1xr00t/WiFiHarvest/discussions)
- 📧 **Security Issues**: Please report responsibly via private channels

---

**⭐ If you find WiFiHarvest useful, please star the repository and share it with the community!**

*Happy Wardriving! 📡🚗💨*
