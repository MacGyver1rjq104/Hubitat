/**
 * ZIGBEE GENERIC METHODS (helpers-zigbee-generic)
 *
 * Helper functions included in all Zigbee drivers
 */

/* --------- STATIC DEFINES --------- */
private getCLUSTER_BASIC() { 0x0000 }
private getCLUSTER_POWER() { 0x0001 }
private getCLUSTER_WINDOW_COVERING() { 0x0102 }
private getCLUSTER_WINDOW_POSITION() { 0x000d }
private getCLUSTER_ON_OFF() { 0x0006 }
private getBASIC_ATTR_POWER_SOURCE() { 0x0007 }
private getPOWER_ATTR_BATTERY_PERCENTAGE_REMAINING() { 0x0021 }
private getPOSITION_ATTR_VALUE() { 0x0055 }
private getCOMMAND_OPEN() { 0x00 }
private getCOMMAND_CLOSE() { 0x01 }
private getCOMMAND_PAUSE() { 0x02 }
private getENCODING_SIZE() { 0x39 }


/* --------- GENERIC METHODS --------- */
void updateNeededSettings() {
    // Ignore, included for compatinility with the driver framework
}

// Used as a workaround to replace an incorrect endpoint
ArrayList<String> zigbeeCommand(Integer cluster, Integer command, Map additionalParams, int delay = 2000, String... payload) {
    ArrayList<String> cmd = zigbee.command(cluster, command, additionalParams, delay, payload)
    cmd[0] = cmd[0].replace('0xnull', '0x01')
    logging("zigbeeCommand() cmd=${cmd}", 0)
    return cmd
}

// Used as a workaround to replace an incorrect endpoint
ArrayList<String> zigbeeCommand(Integer cluster, Integer command, String... payload) {
    ArrayList<String> cmd = zigbee.command(cluster, command, payload)
    cmd[0] = cmd[0].replace('0xnull', '0x01')
    logging("zigbeeCommand() cmd=${cmd}", 0)
    return cmd
}

// Used as a workaround to replace an incorrect endpoint
ArrayList<String> zigbeeWriteAttribute(Integer cluster, Integer attributeId, Integer dataType, Integer value, Map additionalParams = [:], int delay = 2000) {
    ArrayList<String> cmd = zigbee.writeAttribute(cluster, attributeId, dataType, value, additionalParams, delay)
    cmd[0] = cmd[0].replace('0xnull', '0x01')
    logging("zigbeeWriteAttribute() cmd=${cmd}", 0)
    return cmd
}

// Used as a workaround to replace an incorrect endpoint
ArrayList<String> zigbeeReadAttribute(Integer cluster, Integer attributeId, Map additionalParams = [:], int delay = 2000) {
    ArrayList<String> cmd = zigbee.readAttribute(cluster, attributeId, additionalParams, delay)
    cmd[0] = cmd[0].replace('0xnull', '0x01')
    logging("zigbeeReadAttribute() cmd=${cmd}", 0)
    return cmd
}

ArrayList<String> zigbeeWriteLongAttribute(Integer cluster, Integer attributeId, Integer dataType, Long value, Map additionalParams = [:], int delay = 2000) {
    logging("zigbeeWriteLongAttribute()", 1)
    String mfgCode = ""
    if(additionalParams.containsKey("mfgCode")) {
        mfgCode = " {${HexUtils.integerToHexString(HexUtils.hexStringToInt(additionalParams.get("mfgCode")), 2)}}"
    }
    String wattrArgs = "0x${device.deviceNetworkId} 0x01 0x${HexUtils.integerToHexString(cluster, 2)} " + 
                       "0x${HexUtils.integerToHexString(attributeId, 2)} " + 
                       "0x${HexUtils.integerToHexString(dataType, 1)} " + 
                       "{${Long.toHexString(value)}}" + 
                       "$mfgCode"
    ArrayList<String> cmd = ["he wattr $wattrArgs", "delay $delay"]
    
    logging("zigbeeWriteLongAttribute cmd=$cmd", 1)
    return cmd
}

void sendZigbeeCommand(String cmd) {
    logging("sendZigbeeCommand(cmd=$cmd)", 1)
    sendZigbeeCommands([cmd])
}

void sendZigbeeCommands(ArrayList<String> cmd) {
    logging("sendZigbeeCommands(cmd=$cmd)", 1)
    hubitat.device.HubMultiAction allActions = new hubitat.device.HubMultiAction()
    cmd.each {
        if(it.startsWith('delay')) {
            allActions.add(new hubitat.device.HubAction(it))
        } else {
            allActions.add(new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE))
        }
    }
    sendHubCommand(allActions)
}

String setCleanModelName(String newModelToSet=null) {
    // Clean the model name
    String model = newModelToSet != null ? newModelToSet : getDeviceDataByName('model')
    String newModel = model.replaceAll("[^A-Za-z0-9.\\-_]", "")
    logging("dirty model = $model, cleaned model=$newModel", 1)
    updateDataValue('model', newModel)
    return newModel
}

boolean isValidDate(String dateFormat, String dateString) {
    // TODO: Replace this with something NOT using try catch?
    try {
        Date.parse(dateFormat, dateString)
    } catch (e) {
        return false
    }
    return true
}

void sendlastCheckinEvent(Integer minimumMinutesToRepeat=55) {
    if (lastCheckinEnable == true || lastCheckinEnable == null) {
        String lastCheckinVal = device.currentValue('lastCheckin')
        if(lastCheckinVal == null || isValidDate('yyyy-MM-dd HH:mm:ss', lastCheckinVal) == false || now() >= Date.parse('yyyy-MM-dd HH:mm:ss', lastCheckinVal).getTime() + (minimumMinutesToRepeat * 60 * 1000)) {
		    sendEvent(name: "lastCheckin", value: new Date().format('yyyy-MM-dd HH:mm:ss'))
            logging("Updated lastCheckin", 1)
        } else {
            logging("Not updating lastCheckin since at least $minimumMinutesToRepeat minute(s) has not yet passed since last checkin.", 0)
        }
	}
    if (lastCheckinEpochEnable == true) {
		if(device.currentValue('lastCheckinEpoch') == null || now() >= device.currentValue('lastCheckinEpoch').toLong() + (minimumMinutesToRepeat * 60 * 1000)) {
		    sendEvent(name: "lastCheckinEpoch", value: now())
            logging("Updated lastCheckinEpoch", 1)
        } else {
            logging("Not updating lastCheckinEpoch since at least $minimumMinutesToRepeat minute(s) has not yet passed since last checkin.", 0)
        }
	}
}

void checkPresence() {
    Long lastCheckinTime = null
    String lastCheckinVal = device.currentValue('lastCheckin')
    if ((lastCheckinEnable == true || lastCheckinEnable == null) && isValidDate('yyyy-MM-dd HH:mm:ss', lastCheckinVal) == true) {
        lastCheckinTime = Date.parse('yyyy-MM-dd HH:mm:ss', lastCheckinVal).getTime()
    } else if (lastCheckinEpochEnable == true && device.currentValue('lastCheckinEpoch') != null) {
        lastCheckinTime = device.currentValue('lastCheckinEpoch').toLong()
    }
    if(lastCheckinTime != null && lastCheckinTime >= now() - (3 * 60 * 60 * 1000)) {
        // There was an event within the last 3 hours, all is well
        sendEvent(name: "presence", value: "present")
    } else {
        sendEvent(name: "presence", value: "not present")
        log.warn("No event seen from the device for over 3 hours! Something is not right...")
    }
}

void resetBatteryReplacedDate(boolean forced=true) {
    if(forced == true || device.currentValue('batteryLastReplaced') == null) {
        sendEvent(name: "batteryLastReplaced", value: new Date().format('yyyy-MM-dd HH:mm:ss'))
    }
}

void parseAndSendBatteryStatus(BigDecimal vCurrent) {
    BigDecimal vMin = vMinSetting == null ? 2.5 : vMinSetting
    BigDecimal vMax = vMaxSetting == null ? 3.0 : vMaxSetting
    
    BigDecimal bat = 0
    if(vMax - vMin > 0) {
        bat = ((vCurrent - vMin) / (vMax - vMin)) * 100.0
    } else {
        bat = 100
    }
    bat = bat.setScale(1, BigDecimal.ROUND_HALF_UP)
    bat = bat > 100 ? 100 : bat
    
    vCurrent = vCurrent.setScale(3, BigDecimal.ROUND_HALF_UP)

    logging("Battery event: $bat% (V = $vCurrent)", 1)
    sendEvent(name:"battery", value: bat, unit: "%", isStateChange: false)
}

Map unpackStructInMap(Map msgMap, String originalEncoding="4C") {
    // This is a LIMITED implementation, it only does what is needed by any of my drivers so far
    // This is NOT optimized for speed, it is just a convenient way of doing things
    logging("unpackStructInMap()", 0)
    msgMap['encoding'] = originalEncoding
    List<String> values = msgMap['value'].split("(?<=\\G..)")
    Integer numElements = Integer.parseInt(values.take(2).reverse().join(), 16)
    values = values.drop(2)
    List r = []
    while(values != []) {
        Integer cType = Integer.parseInt(values.take(1)[0], 16)
        values = values.drop(1)
        switch(cType) {
            case 0x10:
                // BOOLEAN
                r += Integer.parseInt(values.take(1)[0], 16) != 0
                values = values.drop(1)
                break
            case 0x20:
                // UINT8
                r += Integer.parseInt(values.take(1)[0], 16)
                values = values.drop(1)
                break
            case 0x21:
                // UINT16
                r += Integer.parseInt(values.take(2).reverse().join(), 16)
                values = values.drop(2)
                break
            case 0x22:
                // UINT24
                r += Integer.parseInt(values.take(3).reverse().join(), 16)
                values = values.drop(3)
                break
            case 0x23:
                // UINT24
                r += Long.parseLong(values.take(4).reverse().join(), 16)
                values = values.drop(4)
                break
            case 0x24:
                // UINT40
                r += Long.parseLong(values.take(5).reverse().join(), 16)
                values = values.drop(5)
                break
            case 0x25:
                // UINT48
                r += Long.parseLong(values.take(6).reverse().join(), 16)
                values = values.drop(6)
                break
            case 0x26:
                // UINT56
                r += Long.parseLong(values.take(7).reverse().join(), 16)
                values = values.drop(7)
                break
            case 0x27:
                // UINT64
                r += new BigInteger(values.take(8).reverse().join(), 16)
                values = values.drop(8)
                break
            case 0x28:
                // INT8
                r += convertToSignedInt8(Integer.parseInt(values.take(1).reverse().join(), 16))
                values = values.drop(1)
                break
            case 0x29:
                // INT16 - Short forces the sign
                r += (Integer) (short) Integer.parseInt(values.take(2).reverse().join(), 16)
                values = values.drop(2)
                break
            case 0x2B:
                // INT32 - Long to Integer forces the sign
                r += (Integer) Long.parseLong(values.take(4).reverse().join(), 16)
                values = values.drop(4)
                break
            case 0x39:
                // FLOAT - Single Precision
                r += Float.intBitsToFloat(Long.valueOf(values.take(4).reverse().join(), 16).intValue())
                values = values.drop(4)
                break
            default:
                throw new Exception("The STRUCT used an unrecognized type: $cType (0x${Long.toHexString(cType)})")
        }
    }
    if(r.size() != numElements) throw new Exception("The STRUCT specifies $numElements elements, found ${r.size()}!")
    logging("split: ${r}, numElements: $numElements", 0)
    msgMap['value'] = r
    return msgMap
}

Map parseXiaomiStruct(String xiaomiStruct, boolean isFCC0=false, boolean hasLength=false) {
    logging("parseXiaomiStruct()", 0)
    // https://github.com/dresden-elektronik/deconz-rest-plugin/wiki/Xiaomi-manufacturer-specific-clusters,-attributes-and-attribute-reporting
    Map tags = [
        '01': 'battery',
        '03': 'deviceTemperature',
        '04': 'unknown1',
        '05': 'RSSI_dB',
        '06': 'LQI',
        '07': 'unknown2',
        '08': 'unknown3',
        '09': 'unknown4',
        '0A': 'unknown5',
        '0B': 'unknown6',
        '0C': 'unknown6',
        '6429': 'temperature',
        '6410': 'openClose',
        '6420': 'curtainPosition',
        '65': 'humidity',
        '66': 'pressure',
        '95': 'consumption',
        '96': 'voltage',
        '9721': 'gestureCounter1',
        '9739': 'consumption',
        '9821': 'gestureCounter2',
        '9839': 'power',
        '99': 'gestureCounter3',
        '9A21': 'gestureCounter4',
        '9A20': 'unknown7',
        '9A25': 'unknown8',
        '9B': 'unknown9',
    ]
    if(isFCC0 == true) {
        tags['05'] = 'numBoots'
        tags['6410'] = 'onOff'
        tags['95'] = 'current'
    }

    List<String> values = xiaomiStruct.split("(?<=\\G..)")
    
    if(hasLength == true) values = values.drop(1)
    Map r = [:]
    r["raw"] = [:]
    String cTag = null
    String cTypeStr = null
    Integer cType = null
    String cKey = null
    while(values != []) {
        cTag = values.take(1)[0]
        values = values.drop(1)
        cTypeStr = values.take(1)[0]
        cType = Integer.parseInt(cTypeStr, 16)
        values = values.drop(1)
        if(tags.containsKey(cTag+cTypeStr)) {
            cKey = tags[cTag+cTypeStr]
        } else if(tags.containsKey(cTag)) {
            cKey = tags[cTag]
        } else {
            throw new Exception("The Xiaomi Struct used an unrecognized tag: 0x$cTag (type: 0x$cTypeStr)")
        }
        switch(cType) {
            case 0x10:
                // BOOLEAN
                r["raw"][cKey] = values.take(1)[0]
                r[cKey] = Integer.parseInt(r["raw"][cKey], 16) != 0
                values = values.drop(1)
                break
            case 0x20:
                // UINT8
                r["raw"][cKey] = values.take(1)[0]
                r[cKey] = Integer.parseInt(r["raw"][cKey], 16)
                values = values.drop(1)
                break
            case 0x21:
                // UINT16
                r["raw"][cKey] = values.take(2).reverse().join()
                r[cKey] = Integer.parseInt(r["raw"][cKey], 16)
                values = values.drop(2)
                break
            case 0x22:
                // UINT24
                r["raw"][cKey] = values.take(3).reverse().join()
                r[cKey] = Integer.parseInt(r["raw"][cKey], 16)
                values = values.drop(3)
                break
            case 0x23:
                // UINT32
                r["raw"][cKey] = values.take(4).reverse().join()
                r[cKey] = Long.parseLong(r["raw"][cKey], 16)
                values = values.drop(4)
                break
            case 0x24:
                // UINT40
                r["raw"][cKey] = values.take(5).reverse().join()
                r[cKey] = Long.parseLong(r["raw"][cKey], 16)
                values = values.drop(5)
                break
            case 0x25:
                // UINT48
                r["raw"][cKey] = values.take(6).reverse().join()
                r[cKey] = Long.parseLong(r["raw"][cKey], 16)
                values = values.drop(6)
                break
            case 0x26:
                // UINT56
                r["raw"][cKey] = values.take(7).reverse().join()
                r[cKey] = Long.parseLong(r["raw"][cKey], 16)
                values = values.drop(7)
                break
            case 0x27:
                // UINT64
                r["raw"][cKey] = values.take(8).reverse().join()
                r[cKey] = new BigInteger(r["raw"][cKey], 16)
                values = values.drop(8)
                break
            case 0x28:
                // INT8
                r["raw"][cKey] = values.take(1).reverse().join()
                r[cKey] = convertToSignedInt8(Integer.parseInt(r["raw"][cKey], 16))
                values = values.drop(1)
                break
            case 0x29:
                // INT16 - Short forces the sign
                r["raw"][cKey] = values.take(2).reverse().join()
                r[cKey] = (Integer) (short) Integer.parseInt(r["raw"][cKey], 16)
                values = values.drop(2)
                break
            case 0x2B:
                // INT32 - Long to Integer forces the sign
                r["raw"][cKey] = values.take(4).reverse().join()
                r[cKey] = (Integer) Long.parseLong(r["raw"][cKey], 16)
                values = values.drop(4)
                break
            case 0x39:
                // FLOAT - Single Precision
                r["raw"][cKey] = values.take(4).reverse().join()
                r[cKey] = parseSingleHexToFloat(r["raw"][cKey])
                values = values.drop(4)
                break
            default:
                throw new Exception("The Xiaomi Struct used an unrecognized type: 0x$cTypeStr for tag 0x$cTag with key $cKey")
        }
    }
    logging("Values: $r", 0)
    return r
}

Float parseSingleHexToFloat(String singleHex) {
    return Float.intBitsToFloat(Long.valueOf(singleHex, 16).intValue())
}

Integer convertToSignedInt8(Integer signedByte) {
    Integer sign = signedByte & (1 << 7);
    return (signedByte & 0x7f) * (sign != 0 ? -1 : 1);
}

Integer parseIntReverseHex(String hexString) {
    return Integer.parseInt(hexString.split("(?<=\\G..)").reverse().join(), 16)
}

Long parseLongReverseHex(String hexString) {
    return Long.parseLong(hexString.split("(?<=\\G..)").reverse().join(), 16)
}

String integerToHexString(BigDecimal value, Integer minBytes, boolean reverse=false) {
    return integerToHexString(value.intValue(), minBytes, reverse=reverse)
}

String integerToHexString(Integer value, Integer minBytes, boolean reverse=false) {
    if(reverse == true) {
        return HexUtils.integerToHexString(value, minBytes).split("(?<=\\G..)").reverse().join()
    } else {
        return HexUtils.integerToHexString(value, minBytes)
    }
    
}

Integer miredToKelvin(Integer mired) {
    Integer t = mired
    if(t < 153) t = 153
    if(t > 500) t = 500
    t = Math.round(1000000/t)
    if(t > 6536) t = 6536
    if(t < 2000) t = 2000
    return t
}

Integer kelvinToMired(Integer kelvin) {
    Integer t = kelvin
    if(t > 6536) t = 6536
    if(t < 2000) t = 2000
    t = Math.round(1000000/t)
    if(t < 153) t = 153
    if(t > 500) t = 500
    return t
}

void configurePresence() {
    if(presenceEnable == null || presenceEnable == true) {
        sendEvent(name: "presence", value: "present")
        Random rnd = new Random()
        schedule("${rnd.nextInt(59)} ${rnd.nextInt(59)} 1/3 * * ? *", 'checkPresence')
    } else {
        unschedule('checkPresence')
    }
}

 /**
 * --END-- ZIGBEE GENERIC METHODS (helpers-zigbee-generic)
 */