# hybrid-battery-indicator-android

# android-obd-soc-display

Android app to read hybrid battery SOC (State of Charge) and engine RPM via a Wi‑Fi ELM327 OBD‑II adapter.

## 🚗 Features

- Connects to OBD‑II adapter via Wi‑Fi (IP `192.168.0.10`, port `35000`)
- Reads raw SOC using Toyota‑specific PID `01 B5`
- Reads RPM using standard PID `01 0C`
- Applies linear calibration to convert raw SOC values to realistic percentages (0–100%)
- Displays SOC and RPM in a simple Compose UI
- No XML layouts—modern Jetpack Compose setup

## 📦 Hardware & Tools

- Android phone or tablet
- Wi‑Fi OBD‑II adapter (ELM327-compatible)
- Toyota hybrid (e.g. Corolla hybrid) for data

## ⚙️ SOC Calibration

Convert raw OBD reading to percentage with linear mapping:



Raw examples:
- `72` → ~27% SOC
- `81` → ~50% SOC
- `99` → ~75% SOC

Adjust `rawMin` and `rawMax` based on your own measurements.

## 🧩 Installation & Setup

1. Clone the repo into Android Studio.
2. Ensure Compose setup and permissions for INTERNET.
3. Compile and install on device connected to the same Wi‑Fi SSID as the OBD adapter.
4. Open the app—SOC and RPM should update every second.

## 🛠 How It Works

1. Establishes TCP socket to `192.168.0.10:35000`
2. Sends initialization commands (`ATZ`, `ATE0`, `ATL0`, `ATH0`, `ATSP6`)
3. Periodically:
    - Sends `01 B5` → parses raw SOC from `41 5B XX`
    - Sends `01 0C` → parses RPM from `41 0C AA BB`
4. Calibrates SOC, shows results in UI:
    - Numeric `%` and RPM

## 🔭 Why Build This?

Android diagnostic apps like AndrOBD already exist :contentReference[oaicite:1]{index=1}, but this app is:
- **Custom-tailored** for Toyota hybrid SOC via non-standard PID
- A modern, lightweight Compose UI implementation
- Easy to extend for new PIDs or sensors

## 🔧 Next Steps

- Add custom PID support (Torque-style)
- Log SOC over time
- Implement PID discovery (like ScanYourCar) :contentReference[oaicite:2]{index=2}
- Add Bluetooth or USB support

## 📄 Dependencies

- Jetpack Compose
- Kotlin Coroutines
- Standard Android libraries

---

Feel free to fork and customize. Pull requests and issues welcome!
::contentReference[oaicite:3]{index=3}
