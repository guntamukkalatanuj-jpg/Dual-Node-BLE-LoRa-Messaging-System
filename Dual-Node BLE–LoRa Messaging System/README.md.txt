# Dual-Node BLE–LoRa Messaging System

Short text messages from two phones to a third phone with no cellular or Wi-Fi.
Phone → BLE → ESP32 node → BLE → Gateway → LoRa 433.92 MHz → Central Unit → BLE → Phone

## Hardware
- 4 × ESP32 DevKit V1
- 2 × SX1278 LoRa module (433 MHz) with antennas

## LoRa wiring (Gateway and Central Unit)
NSS→GPIO5, RST→GPIO14, DIO0→GPIO2, SCK→GPIO18, MISO→GPIO19, MOSI→GPIO23, VCC→3.3V, GND→GND

## How to run
1. Flash `firmware/central_unit` and `firmware/gateway`.
2. Flash `firmware/node` twice (NODE_ID 1 and 2).
3. Install the Android app from `android-app`.
4. Connect Phone 3 to Central_Unit, send from the node phones.

## Limitations
- 17-character messages, one-way, range of tens to a few hundred metres at 433 MHz / 10 mW

## Status
Prototype. See `docs/` for the project report.