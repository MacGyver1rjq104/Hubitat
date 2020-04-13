#!include:getHeaderLicense(driverVersionSpecial="v0.9.0")

/* Inspired by a driver from shin4299 which can be found here:
   https://github.com/shin4299/XiaomiSJ/blob/master/devicetypes/shinjjang/xiaomi-curtain-b1.src/xiaomi-curtain-b1.groovy
*/

#!include:getDefaultImports()

metadata {
	definition (name: "Zigbee - Aqara Smart Curtain Motor", namespace: "markusl", author: "Markus Liljergren", vid: "generic-shade") {
        capability "Actuator"
        capability "Light"
		capability "Switch"
		capability "Sensor"
        capability "WindowShade"
        capability "Battery"

        #!include:getDefaultMetadataCapabilities()
        
        #!include:getDefaultMetadataAttributes()
        attribute "lastCheckin", "String"
        attribute "battery2", "Number"
        attribute "batteryLastReplaced", "String"
        #!include:getDefaultMetadataCommands()
        command "stop"
        command "altStop"
        command "altClose"
        command "altOpen"
        command "clearPosition"
        command "reverseCurtain"

        // Fingerprint for Aqara Smart Curtain Motor (ZNCLDJ11LM)
		fingerprint endpointId: "01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0003, 0102, 000D, 0013, 0001", outClusters: "0003, 000A", manufacturer: "LUMI", model: "lumi.curtain.hagl04", deviceJoinName: "Xiaomi Curtain B1"
        fingerprint profileId: "0104", inClusters: "0000,0004,0003,0005,000A,0102,000D,0013,0006,0001,0406", outClusters: "0019,000A,000D,0102,0013,0006,0001,0406", manufacturer: "LUMI", model: "lumi.curtain"
        
	}

	simulator {
	}
    
    preferences {
        #!include:getDefaultMetadataPreferences()
        input name: "mode", type: "bool", title: "Curtain Direction", description: "Reverse Mode ON", required: true, displayDuringSetup: true
        input name: "onlySetPosition", type: "bool", title: "Use only Set Position", defaultValue: false, required: true, displayDuringSetup: true
        if(getDeviceDataByName('model') != "lumi.curtain") {
            //Battery Voltage Range
            input name: "voltsmin", type: "decimal", title: "Min Volts (0% battery = ___ volts). Default = 2.8 Volts", description: ""
            input name: "voltsmax", type: "decimal", title: "Max Volts (100% battery = ___ volts). Default = 3.05 Volts", description: ""
        }

	}
}

#!include:getDeviceInfoFunction()

/* These functions are unique to each driver */
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

def refresh() {
    logging("refresh() model='${getDeviceDataByName('model')}'", 10)
    // http://ftp1.digi.com/support/images/APP_NOTE_XBee_ZigBee_Device_Profile.pdf
    // https://docs.hubitat.com/index.php?title=Zigbee_Object
    // https://docs.smartthings.com/en/latest/ref-docs/zigbee-ref.html
    // https://www.nxp.com/docs/en/user-guide/JN-UG-3115.pdf

    def cmds = []
    cmds += zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE)
    cmds += zigbee.readAttribute(CLUSTER_POWER, POWER_ATTR_BATTERY_PERCENTAGE_REMAINING)
    cmds += zigbee.readAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE)
    return cmds
}

def reboot() {
    logging('reboot() is NOT implemented for this device', 1)
    // Ignore
}
// description:read attr - raw: 05470100000A07003001, dni: 0547, endpoint: 01, cluster: 0000, size: 0A, attrId: 0007, encoding: 30, command: 01, value: 01, parseMap:[raw:05470100000A07003001, dni:0547, endpoint:01, cluster:0000, size:0A, attrId:0007, encoding:30, command:01, value:01, clusterInt:0, attrInt:7]
// Closed curtain: read attr - raw: 054701000D1055003900000000, dni: 0547, endpoint: 01, cluster: 000D, size: 10, attrId: 0055, encoding: 39, command: 01, value: 00000000
// Partially open: msgMap: [raw:054701000D1C5500390000C84200F02300000000, dni:0547, endpoint:01, cluster:000D, size:1C, attrId:0055, encoding:39, command:0A, value:42C80000, clusterInt:13, attrInt:85, additionalAttrs:[[value:00000000, encoding:23, attrId:F000, consumedBytes:7, attrInt:61440]]]
// 0104 000A 01 01 0040 00 0547 00 00 0000 00 00 0000, profileId:0104, clusterId:000A, clusterInt:10, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:0547, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:00, direction:00, data:[00, 00]]
// Fully open: 
def parse(description) {
    #!include:getGenericZigbeeParseHeader()
    
    if (msgMap["profileId"] == "0104") {
        logging("Unhandled KNOWN 0104 event - description:${description} | parseMap:${msgMap}", 1)
        logging("RAW: ${msgMap["attrId"]}", 1)
        // catchall: 0104 000A 01 01 0040 00 63A1 00 00 0000 00 00 0000, parseMap:[raw:catchall: 0104 000A 01 01 0040 00 63A1 00 00 0000 00 00 0000, profileId:0104, clusterId:000A, clusterInt:10, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:63A1, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:00, direction:00, data:[00, 00]]
    } else if (msgMap["cluster"] == "0000" && msgMap["attrId"] == "0404") {
        logging("Unhandled KNOWN event - description:${description} | parseMap:${msgMap}", 1)
        //read attr - raw: 63A10100000804042000, dni: 63A1, endpoint: 01, cluster: 0000, size: 08, attrId: 0404, encoding: 20, command: 0A, value: 00, parseMap:[raw:63A10100000804042000, dni:63A1, endpoint:01, cluster:0000, size:08, attrId:0404, encoding:20, command:0A, value:00, clusterInt:0, attrInt:1028]
    } else if (msgMap["cluster"] == "0000" && msgMap["attrId"] == "0005") {
        logging("Unhandled KNOWN event (pressed button) - description:${description} | parseMap:${msgMap}", 1)
        // read attr - raw: 63A1010000200500420C6C756D692E6375727461696E, dni: 63A1, endpoint: 01, cluster: 0000, size: 20, attrId: 0005, encoding: 42, command: 0A, value: 0C6C756D692E6375727461696E, parseMap:[raw:63A1010000200500420C6C756D692E6375727461696E, dni:63A1, endpoint:01, cluster:0000, size:20, attrId:0005, encoding:42, command:0A, value:lumi.curtain, clusterInt:0, attrInt:5]
    } else if (msgMap["cluster"] == "0000" && msgMap["attrId"] == "0007") {
        logging("Unhandled KNOWN event (BASIC_ATTR_POWER_SOURCE) - description:${description} | parseMap:${msgMap}", 1)
        // Answer to zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE)
        //read attr - raw: 63A10100000A07003001, dni: 63A1, endpoint: 01, cluster: 0000, size: 0A, attrId: 0007, encoding: 30, command: 01, value: 01, parseMap:[raw:63A10100000A07003001, dni:63A1, endpoint:01, cluster:0000, size:0A, attrId:0007, encoding:30, command:01, value:01, clusterInt:0, attrInt:7]
    } else if (msgMap["cluster"] == "0102" && msgMap["attrId"] == "0008") {
        logging("Position event (after pressing stop) - description:${description} | parseMap:${msgMap}", 1)
        long theValue = Long.parseLong(msgMap["value"], 16)
        curtainPosition = theValue.intValue()
        logging("GETTING POSITION from cluster 0102: int => ${curtainPosition}", 1)
        positionEvent(curtainPosition)
        //read attr - raw: 63A1010102080800204E, dni: 63A1, endpoint: 01, cluster: 0102, size: 08, attrId: 0008, encoding: 20, command: 0A, value: 4E
        //read attr - raw: 63A1010102080800203B, dni: 63A1, endpoint: 01, cluster: 0102, size: 08, attrId: 0008, encoding: 20, command: 0A, value: 3B | parseMap:[raw:63A1010102080800203B, dni:63A1, endpoint:01, cluster:0102, size:08, attrId:0008, encoding:20, command:0A, value:3B, clusterInt:258, attrInt:8]
    } else if (msgMap["cluster"] == "0000" && (msgMap["attrId"] == "FF01" || msgMap["attrId"] == "FF02")) {
        // This is probably the battery event, like in other Xiaomi devices... it can also be FF02
        logging("KNOWN event (probably battery) - description:${description} | parseMap:${msgMap}", 1)
        // TODO: Test this, I don't have the battery version...
        // 1C (file separator??) is missing in the beginning of the value after doing this encoding...
        if(getDeviceDataByName('model') != "lumi.curtain") {
            sendEvent(parseBattery(msgMap["value"].getBytes().encodeHex().toString().toUpperCase()))
        }
        //read attr - raw: 63A10100004001FF421C03281E05210F00642000082120110727000000000000000009210002, dni: 63A1, endpoint: 01, cluster: 0000, size: 40, attrId: FF01, encoding: 42, command: 0A, value: 1C03281E05210F00642000082120110727000000000000000009210002, parseMap:[raw:63A10100004001FF421C03281E05210F00642000082120110727000000000000000009210002, dni:63A1, endpoint:01, cluster:0000, size:40, attrId:FF01, encoding:42, command:0A, value:(!d ! '	!, clusterInt:0, attrInt:65281]
    } else if (msgMap["cluster"] == "000D" && msgMap["attrId"] == "0055") {
        logging("cluster 000D", 1)
		if (msgMap["size"] == "16" || msgMap["size"] == "1C" || msgMap["size"] == "10") {
			long theValue = Long.parseLong(msgMap["value"], 16)
			BigDecimal floatValue = Float.intBitsToFloat(theValue.intValue());
			logging("GETTING POSITION: long => ${theValue}, BigDecimal => ${floatValue}", 1)
			curtainPosition = floatValue.intValue()
			positionEvent(curtainPosition)
		} else if (msgMap["size"] == "28" && msgMap["value"] == "00000000") {
			logging("done…", 1)
			sendHubCommand(zigbee.readAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE))                
		}
	} else if (msgMap["clusterId"] == "0001" && msgMap["attrId"] == "0021") {
        if(getDeviceDataByName('model') != "lumi.curtain") {
            def bat = msgMap["value"]
            long value = Long.parseLong(bat, 16)/2
            logging("Battery: ${value}%, ${bat}", 1)
            sendEvent(name:"battery", value: value)
        }

	} else if (msgMap["clusterId"] == "000A") {
		logging("Xiaomi Curtain Present Event", 1)
	} else {
		log.warn "Unhandled Event - description:${description} | parseMap:${msgMap}"
	}
    logging("-----------------------", 1)
    #!include:getGenericZigbeeParseFooter()
}

def positionEvent(curtainPosition) {
	def windowShadeStatus = ""
	if(mode == true) {
        curtainPosition = 100 - curtainPosition
	}
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
	sendEvent(name:"windowShade", value: windowShadeStatus)
	sendEvent(name:"position", value: curtainPosition)
	sendEvent(name:"switch", value: (windowShadeStatus == "closed" ? "off" : "on"))
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

def close() {
    logging("close()", 1)
	setPosition(0)    
}

def open() {
    logging("open()", 1)
	setPosition(100)    
}

def altOpen() {
    logging("altOpen()", 1)
	def cmd = []
	cmd += zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_OPEN)
    return cmd  
}

//reverseCurtain

def reverseCurtain() {
    logging("reverseCurtain()", 1)
	def cmd = []
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF28, 0x10, 0x01)
    logging("cmd=${cmd}", 1)
    return cmd
}

def altClose() {
    logging("altClose()", 1)
	def cmd = []
	cmd += zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_CLOSE)
    logging("cmd=${cmd}", 1)
    return cmd  
}

def on() {
    logging("on()", 1)
	open()
}

def off() {
    logging("off()", 1)
	close()
}

def stop() {
    logging("stop()", 1)
    def cmd = []
	cmd += zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_PAUSE)
    //cmd += zigbee.readAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE)
    return cmd
}

def altStop() {
    logging("altStop()", 1)
    def cmd = []
	cmd += zigbee.command(CLUSTER_ON_OFF, COMMAND_PAUSE)
    return cmd
}

def clearPosition() {
    logging("clearPosition()", 1)
    def cmd = []
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF27, 0x10, 0x00)
    logging("cmd=${cmd}", 1)
    return cmd
}

def setPosition(position) {
    if (position == null) {position = 0}
    position = position as Integer
    logging("setPosition(position: ${position})", 1)
    Integer  currentPosition = device.currentValue("position")
    if (position > currentPosition) {
        sendEvent(name: "windowShade", value: "opening")
    } else if (position < currentLevel) {
        sendEvent(name: "windowShade", value: "closing")
    }
    if((onlySetPosition == false || onlySetPosition == null) && position == 100) {
        if(mode == true){
            logging("Command: Close", 1)
        } else {
            logging("Command: Open", 1)
        }
        logging("cluster: ${CLUSTER_ON_OFF}, command: ${COMMAND_OPEN}", 1)
        cmd = zigbee.command(CLUSTER_ON_OFF, COMMAND_CLOSE)
        logging("cmd=${cmd}", 1)
        return cmd
    } else if ((onlySetPosition == false || onlySetPosition == null) && position < 1) {
        if(mode == true){
            logging("Command: Open", 1)
        } else {
            logging("Command: Close", 1)
        }
        logging("cluster: ${CLUSTER_ON_OFF}, command: ${COMMAND_CLOSE}", 1)

        cmd = zigbee.command(CLUSTER_ON_OFF, COMMAND_OPEN)
        logging("cmd=${cmd}", 1)
        return cmd
    } else {
        if(mode == true){
            position = (100 - position) as Integer
        }
        logging("Set Position: ${position}%", 1)
        //logging("zigbee.writeAttribute(getCLUSTER_WINDOW_POSITION()=${CLUSTER_WINDOW_POSITION}, getPOSITION_ATTR_VALUE()=${POSITION_ATTR_VALUE}, getENCODING_SIZE()=${ENCODING_SIZE}, position=${Float.floatToIntBits(position)})", 1)
        return zigbee.writeAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE, ENCODING_SIZE, Float.floatToIntBits(position))
    }
}

/*
    -----------------------------------------------------------------------------
    Everything below here are LIBRARY includes and should NOT be edited manually!
    -----------------------------------------------------------------------------
    --- Nothings to edit here, move along! --------------------------------------
    -----------------------------------------------------------------------------
*/

#!include:getDefaultFunctions(driverVersionSpecial="v0.9.0")

#!include:getLoggingFunction()

#!include:getHelperFunctions('all-default')

#!include:getHelperFunctions('driver-metadata')

#!include:getHelperFunctions('styling')

#!include:getHelperFunctions('driver-default')
