#include <SPI.h>
#include <LoRa.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define LORA_SS    5
#define LORA_RST   14
#define LORA_DIO0  2

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

BLECharacteristic *pTxCharacteristic;
volatile bool deviceConnected = false;
bool loraReady = false;
unsigned long lastLoraRetry = 0;

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer *s, esp_ble_gatts_cb_param_t *param) {
    deviceConnected = true;
    Serial.println("Phone 3 connected.");
    s->updateConnParams(param->connect.remote_bda, 0x10, 0x20, 0, 400);
  }
  void onDisconnect(BLEServer *s) {
    deviceConnected = false;
    Serial.println("Phone 3 disconnected. Restarting advertising...");
    delay(300);
    BLEDevice::startAdvertising();
  }
};

uint8_t readLoRaVersion() {
  pinMode(LORA_RST, OUTPUT);
  digitalWrite(LORA_RST, LOW);  delay(10);
  digitalWrite(LORA_RST, HIGH); delay(10);
  pinMode(LORA_SS, OUTPUT);
  digitalWrite(LORA_SS, HIGH);
  SPI.begin(18, 19, 23, LORA_SS);
  SPI.beginTransaction(SPISettings(1000000, MSBFIRST, SPI_MODE0));
  digitalWrite(LORA_SS, LOW);
  SPI.transfer(0x42 & 0x7F);
  uint8_t v = SPI.transfer(0x00);
  digitalWrite(LORA_SS, HIGH);
  SPI.endTransaction();
  return v;
}

bool startLoRa() {
  uint8_t v = readLoRaVersion();
  Serial.printf("LoRa version register: 0x%02X (should be 0x12)\n", v);

  LoRa.setPins(LORA_SS, LORA_RST, LORA_DIO0);
  if (!LoRa.begin(433.92E6)) {
    Serial.println("LoRa init FAILED. Check wiring, 3.3V supply and module type. Retrying in 5 s...");
    return false;
  }
  LoRa.enableCrc();
  LoRa.setSyncWord(0xF3);
  LoRa.receive();                 // continuous receive mode
  Serial.println("LoRa ready");
  return true;
}

void setup() {
  Serial.begin(115200);
  delay(300);

  BLEDevice::init("Central_Unit");
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new ServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);
  pTxCharacteristic = pService->createCharacteristic(
      CHARACTERISTIC_UUID_TX, BLECharacteristic::PROPERTY_NOTIFY);
  pTxCharacteristic->addDescriptor(new BLE2902());
  pService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  BLEDevice::startAdvertising();
  Serial.println("Central Unit BLE ready (advertising as Central_Unit).");

  loraReady = startLoRa();
  lastLoraRetry = millis();
}

void loop() {
  if (!loraReady) {
    if (millis() - lastLoraRetry > 5000) {
      lastLoraRetry = millis();
      loraReady = startLoRa();
    }
    delay(10);
    return;
  }

  int packetSize = LoRa.parsePacket();
  if (packetSize) {
    String loraPayload = "";
    while (LoRa.available()) loraPayload += (char)LoRa.read();

    Serial.print("LoRa RX: "); Serial.print(loraPayload);
    Serial.print(" | RSSI: "); Serial.print(LoRa.packetRssi());
    Serial.print(" dBm | SNR: "); Serial.println(LoRa.packetSnr());

    if (loraPayload.startsWith("N,")) {
      int firstComma = loraPayload.indexOf(',');
      int secondComma = loraPayload.indexOf(',', firstComma + 1);
      if (firstComma != -1 && secondComma != -1) {
        String nodeIdStr = loraPayload.substring(firstComma + 1, secondComma);
        String msgText = loraPayload.substring(secondComma + 1);
        String phoneNotification = "N" + nodeIdStr + ":" + msgText;   // max 20 chars

        if (deviceConnected) {
          pTxCharacteristic->setValue(phoneNotification.c_str());
          pTxCharacteristic->notify();
          Serial.print("Notified Phone 3: "); Serial.println(phoneNotification);
        } else {
          Serial.println("Phone 3 not connected; notification skipped.");
        }
      }
    }
  }
  delay(10);
}