## Chunk 8: Final Integration & Testing

### Task 14: Integration Testing

- [ ] **Step 1: Run full application**

```bash
cd zfh-virtual-device-backend
mvn clean package -DskipTests
java -jar target/zfh-virtual-device-backend-1.0.0.jar
```

- [ ] **Step 2: End-to-end test**

Prerequisites: `jq` must be installed (`sudo apt-get install jq` on Ubuntu/Debian)

```bash
# 1. Login and extract token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.token')

# Verify token was extracted
if [ "$TOKEN" = "null" ] || [ -z "$TOKEN" ]; then
    echo "Failed to get token"
    exit 1
fi

# 2. Create gateway
# 2. Create gateway and extract ID
GW_RESPONSE=$(curl -s -X POST http://localhost:8080/api/gateways \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "测试网关",
    "communicationAddress": "GW001",
    "protocol": "MQTT",
    "commMode": "CLIENT",
    "mqttBroker": "tcp://localhost:1883",
    "mqttClientId": "gw-001"
  }')
GW_ID=$(echo $GW_RESPONSE | jq -r '.data.id')

# 3. Create meter and extract ID
METER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/meters \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "测试电表",
    "meterType": "ELECTRIC",
    "communicationAddress": "METER001",
    "protocol": "MQTT",
    "autoReport": true,
    "reportInterval": 5
  }')
METER_ID=$(echo $METER_RESPONSE | jq -r '.data.id')

# 4. Start meter
curl -X POST "http://localhost:8080/api/meters/$METER_ID/start" \
  -H "Authorization: Bearer $TOKEN"

# 5. Check logs after 10 seconds
sleep 10
curl "http://localhost:8080/api/logs?deviceId=$METER_ID" \
  -H "Authorization: Bearer $TOKEN"

# 6. Test WebSocket connection (in a separate terminal)
# Use browser console with SockJS+STOMP (see Chunk 6 test instructions)
```

- [ ] **Step 3: Commit final version**

```bash
git add .
git commit -m "feat: complete backend MVP implementation"
```

---

**Plan complete and saved to `docs/superpowers/plans/2026-04-18-virtual-device-backend.md`. Ready to execute?**

**Next Steps:**
1. Review and approve this plan
2. Execute using subagent-driven-development or executing-plans skill
3. Then create frontend implementation plan
