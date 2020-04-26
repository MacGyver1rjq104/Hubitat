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

void sendlastCheckinEvent(Integer minimumMinutesToRepeat=55) {
    if (lastCheckinEnable == true) {
        if(device.currentValue('lastCheckin') == null || now() >= Date.parse('yyyy-MM-dd HH:mm:ss', device.currentValue('lastCheckin')).getTime() + (minimumMinutesToRepeat * 60 * 1000)) {
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
    if (lastCheckinEnable == true && device.currentValue('lastCheckin') != null) {
        lastCheckinTime = Date.parse('yyyy-MM-dd HH:mm:ss', device.currentValue('lastCheckin')).getTime()
    } else if (lastCheckinEpochEnable == true && device.currentValue('lastCheckinEpoch') != null) {
        lastCheckinTime = device.currentValue('lastCheckinEpoch').toLong()
    }
    if(lastCheckinTime != null && lastCheckinTime >= now() - (3 * 60 * 60 * 1000)) {
        // There was an event within the last 3 hours, all is well
        sendEvent(name: "presence", value: "present")
    } else {
        sendEvent(name: "presence", value: "not present")
    }
}

void resetBatteryReplacedDate() {
    sendEvent(name: "batteryLastReplaced", value: new Date().format('yyyy-MM-dd HH:mm:ss'))
}

void parseAndSendBatteryStatus(BigDecimal vCurrent) {
    BigDecimal vMin = vMinSetting == null ? 2.6 : vMinSetting
    BigDecimal vMax = vMaxSetting == null ? 3.1 : vMinSetting

    BigDecimal bat = 0
    if(vMax - vMin > 0) {
        bat = ((vCurrent - vMin) / (vMax - vMin)) * 100.0
    } else {
        bat = 100
    }
    bat = bat.setScale(1, BigDecimal.ROUND_HALF_UP)
    bat = bat > 100 ? 100 : bat

    logging("Battery event: $bat% (V = $vCurrent)", 1)
    sendEvent(name:"battery", value: bat, unit: "%", isStateChange: false)
}

 /**
 * --END-- ZIGBEE GENERIC METHODS (helpers-zigbee-generic)
 */