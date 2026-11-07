#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define NODE_ID   2
#define NODE_NAME "Node_B2_Input"   // Node B: 2 and "Node_B2_Input"

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

// Core 2.x uses std::string, core 3.x uses Arduino String for advert data
#if defined(ESP_ARDUINO_VERSION_MAJOR) && ESP_ARDUINO_VERSION_MAJOR >= 3
typedef String BleStr;
#else
typedef std::string BleStr;
#endif

volatile bool newMessageAvailable = false;
volatile bool connected = false;
volatile bool needReadvertise = false;
char messageBuffer[18] = {0};
uint8_t messageSeq = 1;
BLEAdvertising *pAdvertising;

// Connectable advert: name + service UUID, so the phone can find/connect
void startConnectableAdv() {
  pAdvertising->stop();
  pAdvertising->setAdvertisementType(ADV_TYPE_IND);
  pAdvertising->setMinInterval(0x50);   // 50 ms
  pAdvertising->setMaxInterval(0xA0);   // 100 ms
  BLEAdvertisementData a;
  a.setFlags(0x06);
  a.setCompleteServices(BLEUUID(SERVICE_UUID));
  pAdvertising->setAdvertisementData(a);
  pAdvertising->start();
}

class ServerCB : public BLEServerCallbacks {
  void onConnect(BLEServer *s, esp_ble_gatts_cb_param_t *param) {
    connected = true;
    Serial.println("Phone connected");
    // 20-40 ms interval, no latency, 4 s supervision timeout (survives collisions)
    s->updateConnParams(param->connect.remote_bda, 0x10, 0x20, 0, 400);
  }
  void onDisconnect(BLEServer *s) {
    connected = false;
    needReadvertise = true;
    Serial.println("Phone disconnected");
  }
};

class RXCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *c) {
    auto v = c->getValue();
    size_t n = v.length();
    const char *s = v.c_str();
    size_t st = 0;
    while (st < n && isspace((unsigned char)s[st])) st++;
    while (n > st && isspace((unsigned char)s[n - 1])) n--;
    size_t len = n - st;
    if (len == 0) return;
    if (len > 17) len = 17;
    memset(messageBuffer, 0, sizeof(messageBuffer));
    memcpy(messageBuffer, s + st, len);
    if (++messageSeq == 0) messageSeq = 1;   // never 0
    Serial.print("Got from phone: "); Serial.println(messageBuffer);
    newMessageAvailable = true;
  }
};

void setup() {
  Serial.begin(115200);
  messageSeq = (uint8_t)(esp_random() % 200) + 1;   // random start so a reboot isn't ignored

  BLEDevice::init(NODE_NAME);
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new ServerCB());
  BLEService *pService = pServer->createService(SERVICE_UUID);

  BLECharacteristic *tx = pService->createCharacteristic(
      CHARACTERISTIC_UUID_TX, BLECharacteristic::PROPERTY_NOTIFY);
  tx->addDescriptor(new BLE2902());

  BLECharacteristic *rx = pService->createCharacteristic(
      CHARACTERISTIC_UUID_RX,
      BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);
  rx->setCallbacks(new RXCallbacks());
  pService->start();

  pAdvertising = BLEDevice::getAdvertising();
  BLEAdvertisementData sr;
  sr.setName(NODE_NAME);
  pAdvertising->setScanResponseData(sr);
  pAdvertising->setScanResponse(true);
  startConnectableAdv();
  Serial.println("Node BLE ready");
}

void loop() {
  if (needReadvertise) {
    needReadvertise = false;
    delay(300);
    startConnectableAdv();
    Serial.println("Re-advertising");
  }

  if (newMessageAvailable) {
    newMessageAvailable = false;
    Serial.print("Node local TX: "); Serial.println(messageBuffer);

    BleStr m;
    m += (char)0xFF; m += (char)0xFF;   // company ID
    m += (char)0xAA;                    // marker
    m += (char)NODE_ID;
    m += (char)messageSeq;
    m += messageBuffer;

    pAdvertising->stop();
    pAdvertising->setAdvertisementType(ADV_TYPE_NONCONN_IND);  // gentle on the live connection
    pAdvertising->setMinInterval(0xA0);   // 100 ms
    pAdvertising->setMaxInterval(0xF0);   // 150 ms
    BLEAdvertisementData ad;
    ad.setFlags(0x06);
    ad.setManufacturerData(m);
    pAdvertising->setAdvertisementData(ad);
    pAdvertising->start();

    delay(3000);   // Gateway scans in ~1 s windows

    pAdvertising->stop();
    if (!connected) startConnectableAdv();   // if phone is connected, stay quiet
  }
  delay(50);
}