#include <SPI.h>
#include <LoRa.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>

#define LORA_SS    5
#define LORA_RST   14
#define LORA_DIO0  2

#define QUEUE_SIZE 10
struct QueuedMessage {
  uint8_t nodeId;
  char text[18];
};
QueuedMessage msgQueue[QUEUE_SIZE];
volatile int queueHead = 0;
volatile int queueTail = 0;

void enqueueMsg(uint8_t id, const char *txt) {
  int nextHead = (queueHead + 1) % QUEUE_SIZE;
  if (nextHead != queueTail) {
    msgQueue[queueHead].nodeId = id;
    memset(msgQueue[queueHead].text, 0, 18);
    strncpy(msgQueue[queueHead].text, txt, 17);
    queueHead = nextHead;
  }
}

bool dequeueMsg(QueuedMessage &msg) {
  if (queueHead == queueTail) return false;
  msg = msgQueue[queueTail];
  queueTail = (queueTail + 1) % QUEUE_SIZE;
  return true;
}

int16_t lastSeq[3] = {-1, -1, -1};   // index = node ID (1 or 2)

BLEScan *pBLEScan;

class AdvertisedDeviceCallbacks : public BLEAdvertisedDeviceCallbacks {
  void onResult(BLEAdvertisedDevice advertisedDevice) {
    if (!advertisedDevice.haveManufacturerData()) return;

    auto mfg = advertisedDevice.getManufacturerData();
    size_t len = mfg.length();
    if (len < 6) return;

    const uint8_t *p = (const uint8_t *)mfg.c_str();
    if (p[0] != 0xFF || p[1] != 0xFF || p[2] != 0xAA) return;   // not our packet

    uint8_t nodeId = p[3];
    uint8_t seq = p[4];
    if (nodeId != 1 && nodeId != 2) return;
    if (seq == lastSeq[nodeId]) return;          // duplicate
    lastSeq[nodeId] = seq;

    char txt[18] = {0};
    size_t n = len - 5;
    if (n > 17) n = 17;
    memcpy(txt, p + 5, n);

    Serial.printf("BLE RX from node %d seq %d: %s\n", nodeId, seq, txt);
    enqueueMsg(nodeId, txt);
  }
};

void setup() {
  Serial.begin(115200);
  delay(300);

  LoRa.setPins(LORA_SS, LORA_RST, LORA_DIO0);
  if (!LoRa.begin(433.92E6)) {
    Serial.println("Starting LoRa failed! Check wiring.");
    while (1) delay(1000);
  }
  LoRa.setTxPower(10);
  LoRa.enableCrc();
  LoRa.setSyncWord(0xF3);
  Serial.println("LoRa ready");

  BLEDevice::init("");
  pBLEScan = BLEDevice::getScan();
  pBLEScan->setAdvertisedDeviceCallbacks(new AdvertisedDeviceCallbacks(), true); // true = allow duplicates
  pBLEScan->setActiveScan(false);   // passive: no scan requests sent to the nodes
  pBLEScan->setInterval(100);
  pBLEScan->setWindow(99);
}

void loop() {
  QueuedMessage msg;
  while (dequeueMsg(msg)) {
    char loraPacket[32];
    snprintf(loraPacket, sizeof(loraPacket), "N,%d,%s", msg.nodeId, msg.text);

    Serial.print("BLE RX then LoRa TX -> ");
    Serial.println(loraPacket);

    LoRa.beginPacket();
    LoRa.print(loraPacket);
    LoRa.endPacket();

    delay(200);
  }

  pBLEScan->start(1, false);
  pBLEScan->clearResults();
}