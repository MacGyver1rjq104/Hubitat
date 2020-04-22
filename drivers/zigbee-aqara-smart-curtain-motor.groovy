#!include:getHeaderLicense()

/* Inspired by a driver from shin4299 which can be found here:
   https://github.com/shin4299/XiaomiSJ/blob/master/devicetypes/shinjjang/xiaomi-curtain-b1.src/xiaomi-curtain-b1.groovy
*/

#!include:getDefaultImports()
import hubitat.helper.HexUtils

metadata {
	definition (name: "Zigbee - Aqara Smart Curtain Motor", namespace: "markusl", author: "Markus Liljergren", vid: "generic-shade") {
        capability "Initialize"
        capability "Sensor"
        capability "Battery"
        capability "PowerSource"
        capability "WindowShade"
        capability "Refresh"
        
        // These 4 capabilities are included to be compatible with integrations like Alexa:
        capability "Actuator"
        capability "Switch"
        capability "Light"
        capability "SwitchLevel"

        //#include:getDefaultMetadataCapabilities()
        #!include:getDefaultMetadataAttributes()
        //#include:getDefaultMetadataCommands()
        command "stop"
        //command "installed"  // just used for testing that Installed runs properly

        command "manualOpenEnable"
        command "manualOpenDisable"

        command "curtainOriginalDirection"
        command "curtainReverseDirection"

        command "trackDiscoveryMode"

        //command "getPosition"
        //command "getPositionAlt"
        //command "getBattery"

        // For testing:
        //command "sendAttribute", [[name:"Attribute*", type: "STRING", description: "Zigbee Attribute"]]

        // Fingerprint for Xiaomi Aqara Smart Curtain Motor (ZNCLDJ11LM)
        fingerprint profileId: "0104", inClusters: "0000,0004,0003,0005,000A,0102,000D,0013,0006,0001,0406", outClusters: "0019,000A,000D,0102,0013,0006,0001,0406", manufacturer: "LUMI", model: "lumi.curtain"
        
        // Fingerprint for Xiaomi Aqara B1 Smart Curtain Motor (ZNCLDJ12LM)
		fingerprint endpointId: "01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0003, 0102, 000D, 0013, 0001", outClusters: "0003, 000A", manufacturer: "LUMI", model: "lumi.curtain.hagl04", deviceJoinName: "Xiaomi Curtain B1"
	}

    preferences {
        #!include:getDefaultMetadataPreferences()
	}
}

#!include:getDeviceInfoFunction()

/* These functions are unique to each driver */

// These get-methods work as static defines
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

// https://github.com/zigbeer/zcl-id/blob/master/definitions/cluster_defs.json
// https://github.com/zigbeer/zcl-id/blob/master/definitions/common.json
// https://github.com/TedTolboom/com.xiaomi-mi-zigbee/blob/master/drivers/curtain.hagl04/device.js

def refresh() {
    logging("refresh() model='${getDeviceDataByName('model')}'", 10)
    // http://ftp1.digi.com/support/images/APP_NOTE_XBee_ZigBee_Device_Profile.pdf
    // https://docs.hubitat.com/index.php?title=Zigbee_Object
    // https://docs.smartthings.com/en/latest/ref-docs/zigbee-ref.html
    // https://www.nxp.com/docs/en/user-guide/JN-UG-3115.pdf

    def cmd = []
    cmd += zigbee.readAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE)
    if(getDeviceDataByName('model') != "lumi.curtain") { 
        cmd += zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE)
        cmd += zigbee.readAttribute(CLUSTER_POWER, 0x0021)
    }
    logging("refresh cmd: $cmd", 1)
    return cmd
}

// called from initialize()
void initializeAdditional() {
    logging("initializeAdditional()", 100)
    cleanModelName()
    makeSchedule()
}

// called from installed()
void installedAdditional() {
    logging("installedAdditional()", 100)
    cleanModelName()
    sendEvent(name:"windowShade", value: 'unknown')
    sendEvent(name:"switch", value: 'off')
    sendEvent(name:"level", value: 0)
    //sendEvent(name:"position", value: null)     // This set it to the string "null" in current versions of HE (2.2.0 and earlier)
}

void cleanModelName() {
    // Clean the model name
    String model = getDeviceDataByName('model')
    String newModel = model.replaceAll("[^A-Za-z0-9.\\-]", "")
    logging("old model = $model, new model=$newModel", 1)
    updateDataValue('model', newModel)
}

void makeSchedule() {
    logging("makeSchedule()", 100)
    // https://www.freeformatter.com/cron-expression-generator-quartz.html
    if(getDeviceDataByName('model') != "lumi.curtain") {
        schedule("16 3 2/12 * * ? *", 'getBattery')
    } else {
        unschedule('getBattery')
    }
}

def parse(description) {
    //log.debug "in parse"
    #!include:getGenericZigbeeParseHeader()
    
    if (msgMap["profileId"] == "0104" && msgMap["clusterId"] == "000A") {
		logging("Xiaomi Curtain Present Event", 1)
	} else if (msgMap["profileId"] == "0104") {
        // This is probably just a heartbeat event...
        logging("Unhandled KNOWN 0104 event (heartbeat?)- description:${description} | parseMap:${msgMap}", 0)
        logging("RAW: ${msgMap["attrId"]}", 0)
        // catchall: 0104 000A 01 01 0040 00 63A1 00 00 0000 00 00 0000, parseMap:[raw:catchall: 0104 000A 01 01 0040 00 63A1 00 00 0000 00 00 0000, profileId:0104, clusterId:000A, clusterInt:10, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:63A1, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:00, direction:00, data:[00, 00]]
    } else if (msgMap["cluster"] == "0000" && msgMap["attrId"] == "0404") {
        if(msgMap["command"] == "0A") {
            if(msgMap["value"] == "00" && getDeviceDataByName('model') == "lumi.curtain") {
                // The position event that comes after this one is a real position
                logging("HANDLED KNOWN 0A command event with Value 00 - description:${description} | parseMap:${msgMap}", 1)
                logging("Sending request for the actual position...", 1)
                hubitat.device.HubMultiAction allActions = new hubitat.device.HubMultiAction()
                allActions.add(new hubitat.device.HubAction(zigbee.readAttribute(CLUSTER_WINDOW_POSITION, 0x0055)[0], hubitat.device.Protocol.ZIGBEE))
                sendHubCommand(allActions)
            } else {
                logging("Unhandled KNOWN 0A command event - description:${description} | parseMap:${msgMap}", 0)
            }
        } else {
            // Received after sending open/close/setposition commands
            logging("Unhandled KNOWN event - description:${description} | parseMap:${msgMap}", 0)
            //read attr - raw: 63A10100000804042000, dni: 63A1, endpoint: 01, cluster: 0000, size: 08, attrId: 0404, encoding: 20, command: 0A, value: 00, parseMap:[raw:63A10100000804042000, dni:63A1, endpoint:01, cluster:0000, size:08, attrId:0404, encoding:20, command:0A, value:00, clusterInt:0, attrInt:1028]
        }
    } else if (msgMap["clusterId"] == "0013" && msgMap["command"] == "00") {
        logging("Unhandled KNOWN event - description:${description} | parseMap:${msgMap}", 0)
        // read attr - raw: 63A1010000200500420C6C756D692E6375727461696E, dni: 63A1, endpoint: 01, cluster: 0000, size: 20, attrId: 0005, encoding: 42, command: 0A, value: 0C6C756D692E6375727461696E, parseMap:[raw:63A1010000200500420C6C756D692E6375727461696E, dni:63A1, endpoint:01, cluster:0000, size:20, attrId:0005, encoding:42, command:0A, value:lumi.curtain, clusterInt:0, attrInt:5]
    } else if (msgMap["cluster"] == "0000" && msgMap["attrId"] == "0005") {
        logging("Unhandled KNOWN event (pressed button) - description:${description} | parseMap:${msgMap}", 0)
        // read attr - raw: 63A1010000200500420C6C756D692E6375727461696E, dni: 63A1, endpoint: 01, cluster: 0000, size: 20, attrId: 0005, encoding: 42, command: 0A, value: 0C6C756D692E6375727461696E, parseMap:[raw:63A1010000200500420C6C756D692E6375727461696E, dni:63A1, endpoint:01, cluster:0000, size:20, attrId:0005, encoding:42, command:0A, value:lumi.curtain, clusterInt:0, attrInt:5]
    } else if (msgMap["cluster"] == "0000" && msgMap["attrId"] == "0007") {
        logging("Handled KNOWN event (BASIC_ATTR_POWER_SOURCE) - description:${description} | parseMap:${msgMap}", 1)
        if(msgMap["value"] == "03") {
            sendEvent(name:"powerSource", value: "battery")
        } else if(msgMap["value"] == "04") {
            sendEvent(name:"powerSource", value: "dc")
        } else {
            sendEvent(name:"powerSource", value: "unknown")
        }
        // Answer to zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE)
        //read attr - raw: 63A10100000A07003001, dni: 63A1, endpoint: 01, cluster: 0000, size: 0A, attrId: 0007, encoding: 30, command: 01, value: 01, parseMap:[raw:63A10100000A07003001, dni:63A1, endpoint:01, cluster:0000, size:0A, attrId:0007, encoding:30, command:01, value:01, clusterInt:0, attrInt:7]
    } else if (msgMap["cluster"] == "0102" && msgMap["attrId"] == "0008") {
        logging("Position event (after pressing stop) - description:${description} | parseMap:${msgMap}", 0)
        long theValue = Long.parseLong(msgMap["value"], 16)
        curtainPosition = theValue.intValue()
        logging("GETTING POSITION from cluster 0102: int => ${curtainPosition}", 1)
        positionEvent(curtainPosition)
        //read attr - raw: 63A1010102080800204E, dni: 63A1, endpoint: 01, cluster: 0102, size: 08, attrId: 0008, encoding: 20, command: 0A, value: 4E
        //read attr - raw: 63A1010102080800203B, dni: 63A1, endpoint: 01, cluster: 0102, size: 08, attrId: 0008, encoding: 20, command: 0A, value: 3B | parseMap:[raw:63A1010102080800203B, dni:63A1, endpoint:01, cluster:0102, size:08, attrId:0008, encoding:20, command:0A, value:3B, clusterInt:258, attrInt:8]
    } else if (msgMap["cluster"] == "0000" && (msgMap["attrId"] == "FF01" || msgMap["attrId"] == "FF02")) {
        logging("KNOWN event (probably battery, but not used) - description:${description} | parseMap:${msgMap}", 0)
    } else if (msgMap["cluster"] == "000D" && msgMap["attrId"] == "0055") {
        logging("cluster 000D", 1)
		if (msgMap["size"] == "16" || msgMap["size"] == "1C" || msgMap["size"] == "10") {
            // This is sent just after sending a command to open/close and just after the curtain is done moving
			long theValue = Long.parseLong(msgMap["value"], 16)
			BigDecimal floatValue = Float.intBitsToFloat(theValue.intValue());
			logging("GOT POSITION DATA (might not be the actual position): long => ${theValue}, BigDecimal => ${floatValue}", 1)
			curtainPosition = floatValue.intValue()
            if(getDeviceDataByName('model') != "lumi.curtain" && msgMap["command"] == "0A" && curtainPosition == 0) {
                logging("Sending a request for the actual position...", 1)
                hubitat.device.HubMultiAction allActions = new hubitat.device.HubMultiAction()
                allActions.add(new hubitat.device.HubAction(zigbee.readAttribute(CLUSTER_WINDOW_POSITION, 0x0055)[0], hubitat.device.Protocol.ZIGBEE))
                //allActions.add(new hubitat.device.HubAction("delay 1000"))
                //allActions.add(new hubitat.device.HubAction(zigbee.readAttribute(CLUSTER_WINDOW_COVERING, 0x0008)[0], hubitat.device.Protocol.ZIGBEE))
                sendHubCommand(allActions)
            } else {
                logging("SETTING POSITION: long => ${theValue}, BigDecimal => ${floatValue}", 1)
                positionEvent(curtainPosition)
                //sendHubCommand(new hubitat.device.HubAction(zigbee.readAttribute(CLUSTER_WINDOW_COVERING, 0x0008)[0]))
            }
		} else if (msgMap["size"] == "28" && msgMap["value"] == "00000000") {
			logging("done…", 0)
			cmds += zigbee.readAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE)
		}
	} else if (msgMap["cluster"] == "0001" && msgMap["attrId"] == "0021") {
        if(getDeviceDataByName('model') != "lumi.curtain") {
            def bat = msgMap["value"]
            long value = Long.parseLong(bat, 16)/2
            logging("Battery: ${value}%, ${bat}", 1)
            sendEvent(name:"battery", value: value)
        }

	} else {
		log.warn "Unhandled Event - description:${description} | msgMap:${msgMap}"
	}
    
    #!include:getGenericZigbeeParseFooter()
}

def getPosition() {
    logging("getPosition()", 1)
	def cmd = []
	cmd += zigbee.readAttribute(CLUSTER_WINDOW_POSITION, 0x0055)
    logging("cmd: $cmd", 1)
    return cmd 
}

def getBattery() {
    logging("getBattery()", 100)
	def cmd = []
    cmd += zigbee.readAttribute(CLUSTER_POWER, 0x0021)
    cmd += zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE)
	logging("cmd: $cmd", 1)
    return cmd 
}

def positionEvent(curtainPosition) {
	def windowShadeStatus = ""
	if(curtainPosition <= 2) curtainPosition = 0
    if(curtainPosition >= 98) curtainPosition = 100
    if (curtainPosition == 100) {
        logging("Fully Open", 1)
        windowShadeStatus = "open"
    } else if (curtainPosition > 0) {
        logging(curtainPosition + '% Partially Open', 1)
        windowShadeStatus = "partially open"
    } else {
        logging("Closed", 1)
        windowShadeStatus = "closed"
    }
    logging("device.currentValue('position') = ${device.currentValue('position')}, curtainPosition = $curtainPosition", 1)
    if(device.currentValue('position') == null || 
        curtainPosition < device.currentValue('position') - 1 || 
        curtainPosition > device.currentValue('position') + 1) {
        
        logging("CHANGING device.currentValue('position') = ${device.currentValue('position')}, curtainPosition = $curtainPosition", 1)
        sendEvent(name:"windowShade", value: windowShadeStatus)
        sendEvent(name:"position", value: curtainPosition)
        // For Alexa:
        sendEvent(name:"level", value: curtainPosition)
        if(windowShadeStatus == "closed") {
            sendEvent(name:"switch", value: 'off')
        } else {
            sendEvent(name:"switch", value: 'on')
        }
    }
}

// Convert raw 4 digit integer voltage value into percentage based on minVolts/maxVolts range
private parseBattery(hexString) {
    // All credits go to veeceeoh for this battery parsing method!
    logging("Battery full string = ${hexString}", 1)
    // Moved this one byte to the left due to how the built-in parser work, needs testing!
	//def hexBattery = (hexString[8..9] + hexString[6..7])
    def hexBattery = (hexString[6..7] + hexString[4..5])
    logging("Battery parsed string = ${hexBattery}", 1)
	def rawValue = Integer.parseInt(hexBattery,16)
	def rawVolts = rawValue / 1000
	def minVolts = voltsmin ? voltsmin : 2.8
	def maxVolts = voltsmax ? voltsmax : 3.05
	def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
	def roundedPct = Math.min(100, Math.round(pct * 100))
	logging("Battery report: $rawVolts Volts ($roundedPct%), calculating level based on min/max range of $minVolts to $maxVolts", 1)
	def descText = "Battery level is $roundedPct% ($rawVolts Volts)"
	return [
		name: 'battery2',
		value: roundedPct,
		unit: "%",
		descriptionText: descText
	]
}

def updated() {
    logging("updated()", 10)
    def cmds = [] 
    try {
        // Also run initialize(), if it exists...
        initialize()
    } catch (MissingMethodException e) {
        // ignore
    }
    if (cmds != [] && cmds != null) cmds
}

def updateNeededSettings() {
    
}

ArrayList open() {
    logging("open()", 1)
	return setPosition(100)    
}

ArrayList on() {
    logging("on()", 1)
	return open()
}

ArrayList close() {
    logging("close()", 1)
	return setPosition(0)    
}

ArrayList off() {
    logging("off()", 1)
	return close()
}

ArrayList reverseCurtain() {
    logging("reverseCurtain()", 1)
	def cmd = []
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF28, 0x10, 0x01, [mfgCode: "0x115F"])
    logging("cmd=${cmd}", 1)
    return cmd
}

String hexToASCII(String hexValue) {
    StringBuilder output = new StringBuilder("")
    for (int i = 0; i < hexValue.length(); i += 2) {
        String str = hexValue.substring(i, i + 2)
        output.append((char) Integer.parseInt(str, 16) + 30)
        logging("${Integer.parseInt(str, 16)}", 10)
    }
    logging("hexToASCII: ${output.toString()}", 0)
    return output.toString()
}

ArrayList zigbeeWriteLongAttribute(Integer cluster, Integer attributeId, Integer dataType, Long value, Map additionalParams = [:], int delay = 2000) {
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
    ArrayList cmdList = ["he wattr $wattrArgs", "delay $delay"]
    
    //hubitat.device.HubMultiAction allActions = new hubitat.device.HubMultiAction()
    //allActions.add(new hubitat.device.HubAction(cmdList[0], hubitat.device.Protocol.ZIGBEE))
    //allActions.add(new hubitat.device.HubAction(cmdList[1]))
    
    //sendHubCommand(allActions)
    logging("zigbeeWriteLongAttribute cmdList=$cmdList", 1)
    return cmdList
}

/*
// Only used for debugging
def sendAttribute(String attribute) {
    attribute = attribute.replace(' ', '')
    logging("sendAttribute(attribute=$attribute) (0x${Long.toHexString(Long.decode("0x$attribute"))})", 1)
    def cmd = []
    cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, Long.decode("0x$attribute"), [mfgCode: "0x115F"])
    logging("cmd=${cmd}, size=${cmd.size()}", 10)
    return cmd
}*/

ArrayList manualOpenEnable() {
    logging("manualOpenEnable()", 1)
    ArrayList cmd = []
    if(getDeviceDataByName('model') == "lumi.curtain") {
        cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700080000040012, [mfgCode: "0x115F"])
    } else {
        cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF29, 0x10, 0x00, [mfgCode: "0x115F"])
    }
    logging("manualOpenEnable cmd=${cmd}", 0)
    return cmd
}

ArrayList manualOpenDisable() {
    logging("manualOpenDisable()", 1)
    ArrayList cmd = []
    if(getDeviceDataByName('model') == "lumi.curtain") {
        cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700080000040112, [mfgCode: "0x115F"])
    } else {
        cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF29, 0x10, 0x01, [mfgCode: "0x115F"])
    }
    logging("manualOpenDisable cmd=${cmd}", 0)
    return cmd
}

ArrayList curtainOriginalDirection() {
    logging("curtainOriginalDirection()", 1)
    ArrayList cmd = []
    if(getDeviceDataByName('model') == "lumi.curtain") {
        cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700020000040012, [mfgCode: "0x115F"])
    } else {
        cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF28, 0x10, 0x00, [mfgCode: "0x115F"])
    }
    logging("curtainOriginalDirection cmd=${cmd}", 0)
    return cmd
}

ArrayList curtainReverseDirection() {
    logging("curtainReverseDirection()", 1)
    ArrayList cmd = []
    if(getDeviceDataByName('model') == "lumi.curtain") {
        cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700020001040012, [mfgCode: "0x115F"])
    } else {
        cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF28, 0x10, 0x01, [mfgCode: "0x115F"])
    }
    logging("curtainReverseDirection cmd=${cmd}", 0)
    return cmd
}

ArrayList trackDiscoveryMode() {
    logging("trackDiscoveryMode()", 1)
    ArrayList cmd = []
    if(getDeviceDataByName('model') == "lumi.curtain") {
        cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700010000040012, [mfgCode: "0x115F"])
    } else {
        cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF27, 0x10, 0x00, [mfgCode: "0x115F"])
    }
    logging("trackDiscoveryMode cmd=${cmd}", 0)
    return cmd
}

ArrayList stop() {
    logging("stop()", 1)
    ArrayList cmd = []
	cmd += zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_PAUSE)
    logging("stop cmd=${cmd}", 0)
    return cmd
}

def enableAutoClose() {
    logging("enableAutoClose()", 1)
    def cmd = []
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF29, 0x10, 0x00, [mfgCode: "0x115F"])
    logging("enableAutoClose cmd=${cmd}", 0)
    return cmd
}

def disableAutoClose() {
    logging("disableAutoClose()", 1)
    def cmd = []
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF29, 0x10, 0x01, [mfgCode: "0x115F"])
    logging("disableAutoClose cmd=${cmd}", 0)
    return cmd
}

ArrayList setPosition(position) {
    if(position == null) {position = 0}
    if(position <= 2) position = 0
    if(position >= 98) position = 100
    ArrayList cmd = []
    position = position as Integer
    logging("setPosition(position: ${position})", 1)
    Integer currentPosition = device.currentValue("position")
    if(position > currentPosition) {
        sendEvent(name: "windowShade", value: "opening")
    } else if (position < currentPosition) {
        sendEvent(name: "windowShade", value: "closing")
    }
    if(position == 100 && getDeviceDataByName('model') == "lumi.curtain") {
        logging("Command: Open", 1)
        logging("cluster: ${CLUSTER_ON_OFF}, command: ${COMMAND_OPEN}", 0)
        cmd += zigbee.command(CLUSTER_ON_OFF, COMMAND_CLOSE)
    } else if(position < 1 && getDeviceDataByName('model') == "lumi.curtain") {
        logging("Command: Close", 1)
        logging("cluster: ${CLUSTER_ON_OFF}, command: ${COMMAND_CLOSE}", 0)
        cmd += zigbee.command(CLUSTER_ON_OFF, COMMAND_OPEN)
    } else {
        logging("Set Position: ${position}%", 1)
        //logging("zigbee.writeAttribute(getCLUSTER_WINDOW_POSITION()=${CLUSTER_WINDOW_POSITION}, getPOSITION_ATTR_VALUE()=${POSITION_ATTR_VALUE}, getENCODING_SIZE()=${ENCODING_SIZE}, position=${Float.floatToIntBits(position)})", 1)
        cmd += zigbee.writeAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE, ENCODING_SIZE, Float.floatToIntBits(position))
    }
    logging("cmd=${cmd}", 1)
    return cmd
}

ArrayList setLevel(level) {
    logging("setLevel(level: ${level})", 1)
    return setPosition(level)
}

ArrayList setLevel(level, duration) {
    logging("setLevel(level: ${level})", 1)
    return setPosition(level)
}

/*
    -----------------------------------------------------------------------------
    Everything below here are LIBRARY includes and should NOT be edited manually!
    -----------------------------------------------------------------------------
    --- Nothings to edit here, move along! --------------------------------------
    -----------------------------------------------------------------------------
*/

#!include:getDefaultFunctions()

#!include:getLoggingFunction()

#!include:getHelperFunctions('all-default')

#!include:getHelperFunctions('driver-metadata')

#!include:getHelperFunctions('styling')

#!include:getHelperFunctions('driver-default')
