/**
 *  Copyright 2020 Markus Liljergren
 *
 *  Code Version: v0.9.0
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/* Inspired by a driver from shin4299 which can be found here:
   https://github.com/shin4299/XiaomiSJ/blob/master/devicetypes/shinjjang/xiaomi-curtain-b1.src/xiaomi-curtain-b1.groovy
*/

// BEGIN:getDefaultImports()
/** Default Imports */
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
// Used for MD5 calculations
import java.security.MessageDigest
// END:  getDefaultImports()


metadata {
	definition (name: "Zigbee - Aqara Smart Curtain Motor", namespace: "markusl", author: "Markus Liljergren", vid: "generic-shade", importUrl: "https://raw.githubusercontent.com/markus-li/Hubitat/development/drivers/expanded/zigbee-aqara-smart-curtain-motor-expanded.groovy") {
        capability "Actuator"
        capability "Light"
		capability "Switch"
		capability "Sensor"
        capability "WindowShade"
        capability "Battery"

        // BEGIN:getDefaultMetadataCapabilities()
        // Default Capabilities
        capability "Refresh"
        capability "Configuration"
        // END:  getDefaultMetadataCapabilities()
        
        // BEGIN:getDefaultMetadataAttributes()
        // Default Attributes
        attribute   "driver", "string"
        // END:  getDefaultMetadataAttributes()
        attribute "lastCheckin", "String"
        attribute "battery2", "Number"
        attribute "batteryLastReplaced", "String"
        // BEGIN:getDefaultMetadataCommands()
        // Default Commands
        command "reboot"
        // END:  getDefaultMetadataCommands()
        command "stop"
        command "altStop"
        command "altClose"
        command "altOpen"
        command "clearPosition"
        command "reverseCurtain"
        command "clearPosition2"
        command "reverseCurtain2"
        
        command "enableAutoClose"
        command "disableAutoClose"

        // Fingerprint for Aqara Smart Curtain Motor (ZNCLDJ11LM)
		fingerprint endpointId: "01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0003, 0102, 000D, 0013, 0001", outClusters: "0003, 000A", manufacturer: "LUMI", model: "lumi.curtain.hagl04", deviceJoinName: "Xiaomi Curtain B1"
        fingerprint profileId: "0104", inClusters: "0000,0004,0003,0005,000A,0102,000D,0013,0006,0001,0406", outClusters: "0019,000A,000D,0102,0013,0006,0001,0406", manufacturer: "LUMI", model: "lumi.curtain"
        
	}

	simulator {
	}
    
    preferences {
        // BEGIN:getDefaultMetadataPreferences()
        // Default Preferences
        generate_preferences(configuration_model_debug())
        // END:  getDefaultMetadataPreferences()
        input name: "mode", type: "bool", title: "Curtain Direction", description: "Reverse Mode ON", required: true, displayDuringSetup: true
        input name: "onlySetPosition", type: "bool", title: "Use only Set Position", defaultValue: false, required: true, displayDuringSetup: true
        if(getDeviceDataByName('model') != "lumi.curtain") {
            //Battery Voltage Range
            input name: "voltsmin", type: "decimal", title: "Min Volts (0% battery = ___ volts). Default = 2.8 Volts", description: ""
            input name: "voltsmax", type: "decimal", title: "Max Volts (100% battery = ___ volts). Default = 3.05 Volts", description: ""
        }

	}
}

// BEGIN:getDeviceInfoFunction()
String getDeviceInfoByName(infoName) { 
    // DO NOT EDIT: This is generated from the metadata!
    // TODO: Figure out how to get this from Hubitat instead of generating this?
    Map deviceInfo = ['name': 'Zigbee - Aqara Smart Curtain Motor', 'namespace': 'markusl', 'author': 'Markus Liljergren', 'vid': 'generic-shade', 'importUrl': 'https://raw.githubusercontent.com/markus-li/Hubitat/development/drivers/expanded/zigbee-aqara-smart-curtain-motor-expanded.groovy']
    //logging("deviceInfo[${infoName}] = ${deviceInfo[infoName]}", 1)
    return(deviceInfo[infoName])
}
// END:  getDeviceInfoFunction()


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
    // 0x115F
    cmds += zigbee.readAttribute(CLUSTER_BASIC, 0x0401, [mfgCode: "0x115F"])
    cmds += zigbee.readAttribute(CLUSTER_BASIC, 0xFF27, [mfgCode: "0x115F"])
    cmds += zigbee.readAttribute(CLUSTER_BASIC, 0xFF28, [mfgCode: "0x115F"])
    cmds += zigbee.readAttribute(CLUSTER_BASIC, 0xFF29, [mfgCode: "0x115F"])
    //  read attr - raw: 0759010000180104420700000100000000, dni: 0759, endpoint: 01, cluster: 0000, size: 18, 
    // attrId: 0401, encoding: 42, command: 01, value: 0700 000100000000
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
    log.debug "in parse"
    // BEGIN:getGenericZigbeeParseHeader()
    // parse() Generic Zigbee-device header BEGINS here
    logging("Parsing: ${description}", 10)
    def events = []
    def msgMap = zigbee.parseDescriptionAsMap(description)
    logging("msgMap: ${msgMap}", 10)
    // parse() Generic header ENDS here
    // END:  getGenericZigbeeParseHeader()
    
    if (msgMap["profileId"] == "0104") {
        logging("Unhandled KNOWN 0104 event - description:${description} | parseMap:${msgMap}", 10)
        logging("RAW: ${msgMap["attrId"]}", 10)
        // catchall: 0104 000A 01 01 0040 00 63A1 00 00 0000 00 00 0000, parseMap:[raw:catchall: 0104 000A 01 01 0040 00 63A1 00 00 0000 00 00 0000, profileId:0104, clusterId:000A, clusterInt:10, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:63A1, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:00, direction:00, data:[00, 00]]
    } else if (msgMap["cluster"] == "0000" && msgMap["attrId"] == "0404") {
        logging("Unhandled KNOWN event - description:${description} | parseMap:${msgMap}", 10)
        //read attr - raw: 63A10100000804042000, dni: 63A1, endpoint: 01, cluster: 0000, size: 08, attrId: 0404, encoding: 20, command: 0A, value: 00, parseMap:[raw:63A10100000804042000, dni:63A1, endpoint:01, cluster:0000, size:08, attrId:0404, encoding:20, command:0A, value:00, clusterInt:0, attrInt:1028]
    } else if (msgMap["cluster"] == "0000" && msgMap["attrId"] == "0005") {
        logging("Unhandled KNOWN event (pressed button) - description:${description} | parseMap:${msgMap}", 10)
        // read attr - raw: 63A1010000200500420C6C756D692E6375727461696E, dni: 63A1, endpoint: 01, cluster: 0000, size: 20, attrId: 0005, encoding: 42, command: 0A, value: 0C6C756D692E6375727461696E, parseMap:[raw:63A1010000200500420C6C756D692E6375727461696E, dni:63A1, endpoint:01, cluster:0000, size:20, attrId:0005, encoding:42, command:0A, value:lumi.curtain, clusterInt:0, attrInt:5]
    } else if (msgMap["cluster"] == "0000" && msgMap["attrId"] == "0007") {
        logging("Unhandled KNOWN event (BASIC_ATTR_POWER_SOURCE) - description:${description} | parseMap:${msgMap}", 10)
        // Answer to zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE)
        //read attr - raw: 63A10100000A07003001, dni: 63A1, endpoint: 01, cluster: 0000, size: 0A, attrId: 0007, encoding: 30, command: 01, value: 01, parseMap:[raw:63A10100000A07003001, dni:63A1, endpoint:01, cluster:0000, size:0A, attrId:0007, encoding:30, command:01, value:01, clusterInt:0, attrInt:7]
    } else if (msgMap["cluster"] == "0102" && msgMap["attrId"] == "0008") {
        logging("Position event (after pressing stop) - description:${description} | parseMap:${msgMap}", 10)
        long theValue = Long.parseLong(msgMap["value"], 16)
        curtainPosition = theValue.intValue()
        logging("GETTING POSITION from cluster 0102: int => ${curtainPosition}", 10)
        positionEvent(curtainPosition)
        //read attr - raw: 63A1010102080800204E, dni: 63A1, endpoint: 01, cluster: 0102, size: 08, attrId: 0008, encoding: 20, command: 0A, value: 4E
        //read attr - raw: 63A1010102080800203B, dni: 63A1, endpoint: 01, cluster: 0102, size: 08, attrId: 0008, encoding: 20, command: 0A, value: 3B | parseMap:[raw:63A1010102080800203B, dni:63A1, endpoint:01, cluster:0102, size:08, attrId:0008, encoding:20, command:0A, value:3B, clusterInt:258, attrInt:8]
    } else if (msgMap["cluster"] == "0000" && (msgMap["attrId"] == "FF01" || msgMap["attrId"] == "FF02")) {
        // This is probably the battery event, like in other Xiaomi devices... it can also be FF02
        logging("KNOWN event (probably battery) - description:${description} | parseMap:${msgMap}", 10)
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
			logging("GETTING POSITION: long => ${theValue}, BigDecimal => ${floatValue}", 10)
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
            logging("Battery: ${value}%, ${bat}", 10)
            sendEvent(name:"battery", value: value)
        }

	} else if (msgMap["clusterId"] == "000A") {
		logging("Xiaomi Curtain Present Event", 10)
	} else {
		log.warn "Unhandled Event - description:${description} | parseMap:${msgMap}"
	}
    logging("-----------------------", 1)
    // BEGIN:getGenericZigbeeParseFooter()
    // parse() Generic Zigbee-device footer BEGINS here
    
    return events
    // parse() Generic footer ENDS here
    // END:  getGenericZigbeeParseFooter()
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
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF28, 0x10, 0x01, [mfgCode: "0x115F"])
    cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0x0401, 0x21, 0x0100000000070007, [mfgCode: "0x115F"])
    logging("cmd=${cmd}", 1)
    return cmd
}

def reverseCurtain2() {
    logging("reverseCurtain2()", 1)
	def cmd = []
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF28, 0x10, 0x00, [mfgCode: "0x115F"])
    // xiaomi mfg: 0x115F
    //cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0x0401, 0x21, 0x1100, [mfgCode: "0x115F"])
    //cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0x0401, 0x21, 0x1100, [mfgCode: "0x115F"])
    //cmd += zigbee.command(CLUSTER_BASIC, 0x0401, [mfgCode: "0x115F"], "0x0100000101070007")
    //cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0x0401, 0x21, 0x0100000101070007, [mfgCode: "0x115F"])
    // read attr - raw: 0759010000180104420700000100000000, dni: 0759, endpoint: 01, cluster: 0000, 
    // size: 18, attrId: 0401, encoding: 42, command: 01, value: 0700000100000000
    //                                                           0100000101070007
    //                                                           0700000100000000
    // [he wattr 0x0759 0x01 0x0000 0x0401 0x21 {0100000101070007} {115F}, delay 2000, 
    //  he cmd 0x0759 0x01 0x0000 0x401 {0x0100000101070007} {115F}, delay 2000]
    logging("cmd=${cmd}", 10)
    //def hubAction = new hubitat.device.HubAction("he wattr 0x0759 0x01 0x0000 0x0401 0x42 {0x0100000101070007} {115F}", hubitat.device.Protocol.ZIGBEE)
    //def hubAction = new hubitat.device.HubAction("he wattr 0x0759 0x01 0x0000 0x0401 0x42 {0100000101070007} {115F}", hubitat.device.Protocol.ZIGBEE)
    //def hubAction = new hubitat.device.HubAction("he wattr 0x0759 0x01 0x0000 0x0401 0x21 {7777} {115F}", hubitat.device.Protocol.ZIGBEE)
    //logging("hubAction=${hubAction}", 10)
    //sendHubCommand(hubAction)
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
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF27, 0x10, 0x00, [mfgCode: "0x115F"])
    logging("cmd=${cmd}", 1)
    return cmd
}

def clearPosition2() {
    logging("clearPosition2()", 1)
    def cmd = []
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF27, 0x10, 0x01, [mfgCode: "0x115F"])
    logging("cmd=${cmd}", 1)
    return cmd
}

def enableAutoClose() {
    logging("enableAutoClose()", 1)
    def cmd = []
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF29, 0x10, 0x01, [mfgCode: "0x115F"])
    logging("cmd=${cmd}", 1)
    return cmd
}

def disableAutoClose() {
    logging("disableAutoClose()", 1)
    def cmd = []
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF29, 0x10, 0x00, [mfgCode: "0x115F"])
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

// BEGIN:getDefaultFunctions(driverVersionSpecial="v0.9.0")
/* Default Driver Methods go here */
private String getDriverVersion() {
    //comment = ""
    //if(comment != "") state.comment = comment
    String version = "v0.9.0"
    logging("getDriverVersion() = ${version}", 100)
    sendEvent(name: "driver", value: version)
    updateDataValue('driver', version)
    return version
}
// END:  getDefaultFunctions(driverVersionSpecial="v0.9.0")


// BEGIN:getLoggingFunction()
/* Logging function included in all drivers */
private boolean logging(message, level) {
    boolean didLogging = false
    Integer logLevelLocal = (logLevel != null ? logLevel.toInteger() : 0)
    if(!isDeveloperHub()) {
        logLevelLocal = 0
        if (infoLogging == true) {
            logLevelLocal = 100
        }
        if (debugLogging == true) {
            logLevelLocal = 1
        }
    }
    if (logLevelLocal != "0"){
        switch (logLevelLocal) {
        case -1: // Insanely verbose
            if (level >= 0 && level < 100) {
                log.debug "$message"
                didLogging = true
            } else if (level == 100) {
                log.info "$message"
                didLogging = true
            }
        break
        case 1: // Very verbose
            if (level >= 1 && level < 99) {
                log.debug "$message"
                didLogging = true
            } else if (level == 100) {
                log.info "$message"
                didLogging = true
            }
        break
        case 10: // A little less
            if (level >= 10 && level < 99) {
                log.debug "$message"
                didLogging = true
            } else if (level == 100) {
                log.info "$message"
                didLogging = true
            }
        break
        case 50: // Rather chatty
            if (level >= 50 ) {
                log.debug "$message"
                didLogging = true
            }
        break
        case 99: // Only parsing reports
            if (level >= 99 ) {
                log.debug "$message"
                didLogging = true
            }
        break
        
        case 100: // Only special debug messages, eg IR and RF codes
            if (level == 100 ) {
                log.info "$message"
                didLogging = true
            }
        break
        }
    }
    return didLogging
}
// END:  getLoggingFunction()


/**
 * ALL DEBUG METHODS (helpers-all-debug)
 *
 * Helper Debug functions included in all drivers/apps
 */
String configuration_model_debug() {
    if(!isDeveloperHub()) {
        if(!isDriver()) {
            app.removeSetting("logLevel")
            app.updateSetting("logLevel", "0")
        }
        return '''
<configuration>
<Value type="bool" index="debugLogging" label="Enable debug logging" description="" value="false" submitOnChange="true" setting_type="preference" fw="">
<Help></Help>
</Value>
<Value type="bool" index="infoLogging" label="Enable descriptionText logging" description="" value="true" submitOnChange="true" setting_type="preference" fw="">
<Help></Help>
</Value>
</configuration>
'''
    } else {
        if(!isDriver()) {
            app.removeSetting("debugLogging")
            app.updateSetting("debugLogging", "false")
            app.removeSetting("infoLogging")
            app.updateSetting("infoLogging", "false")
        }
        return '''
<configuration>
<Value type="list" index="logLevel" label="Debug Log Level" description="Under normal operations, set this to None. Only needed for debugging. Auto-disabled after 30 minutes." value="100" submitOnChange="true" setting_type="preference" fw="">
<Help>
</Help>
    <Item label="None" value="0" />
    <Item label="Insanely Verbose" value="-1" />
    <Item label="Very Verbose" value="1" />
    <Item label="Verbose" value="10" />
    <Item label="Reports+Status" value="50" />
    <Item label="Reports" value="99" />
    // BEGIN:getSpecialDebugEntry()
    <Item label="descriptionText" value="100" />
    // END:  getSpecialDebugEntry()
</Value>
</configuration>
'''
    }
}

/**
 *   --END-- ALL DEBUG METHODS (helpers-all-debug)
 */

/**
 * ALL DEFAULT METHODS (helpers-all-default)
 *
 * Helper functions included in all drivers/apps
 */

boolean isDriver() {
    try {
        // If this fails, this is not a driver...
        getDeviceDataByName('_unimportant')
        logging("This IS a driver!", 0)
        return true
    } catch (MissingMethodException e) {
        logging("This is NOT a driver!", 0)
        return false
    }
}

void deviceCommand(cmd) {
    def jsonSlurper = new JsonSlurper()
    cmd = jsonSlurper.parseText(cmd)
    logging("deviceCommand: ${cmd}", 0)
    r = this."${cmd['cmd']}"(*cmd['args'])
    logging("deviceCommand return: ${r}", 0)
    updateDataValue('appReturn', JsonOutput.toJson(r))
}

/*
	initialize

	Purpose: initialize the driver/app
	Note: also called from updated()
    This is called when the hub starts, DON'T declare it with return as void,
    that seems like it makes it to not run? Since testing require hub reboots
    and this works, this is not conclusive...
*/
// Call order: installed() -> configure() -> updated() -> initialize()
def initialize() {
    logging("initialize()", 100)
	unschedule("updatePresence")
    // disable debug logs after 30 min, unless override is in place
	if (logLevel != "0" && logLevel != "100") {
        if(runReset != "DEBUG") {
            log.warn "Debug logging will be disabled in 30 minutes..."
        } else {
            log.warn "Debug logging will NOT BE AUTOMATICALLY DISABLED!"
        }
        runIn(1800, "logsOff")
    }
    if(isDriver()) {
        if(!isDeveloperHub()) {
            device.removeSetting("logLevel")
            device.updateSetting("logLevel", "0")
        } else {
            device.removeSetting("debugLogging")
            device.updateSetting("debugLogging", "false")
            device.removeSetting("infoLogging")
            device.updateSetting("infoLogging", "false")
        }
    }
    try {
        // In case we have some more to run specific to this driver/app
        initializeAdditional()
    } catch (MissingMethodException e) {
        // ignore
    }
    refresh()
}

/**
 * Automatically disable debug logging after 30 mins.
 *
 * Note: scheduled in Initialize()
 */
void logsOff() {
    if(runReset != "DEBUG") {
        log.warn "Debug logging disabled..."
        // Setting logLevel to "0" doesn't seem to work, it disables logs, but does not update the UI...
        //device.updateSetting("logLevel",[value:"0",type:"string"])
        //app.updateSetting("logLevel",[value:"0",type:"list"])
        // Not sure which ones are needed, so doing all... This works!
        if(isDriver()) {
            device.clearSetting("logLevel")
            device.removeSetting("logLevel")
            device.updateSetting("logLevel", "0")
            state?.settings?.remove("logLevel")
            device.clearSetting("debugLogging")
            device.removeSetting("debugLogging")
            device.updateSetting("debugLogging", "false")
            state?.settings?.remove("debugLogging")
            
        } else {
            //app.clearSetting("logLevel")
            // To be able to update the setting, it has to be removed first, clear does NOT work, at least for Apps
            app.removeSetting("logLevel")
            app.updateSetting("logLevel", "0")
            app.removeSetting("debugLogging")
            app.updateSetting("debugLogging", "false")
        }
    } else {
        log.warn "OVERRIDE: Disabling Debug logging will not execute with 'DEBUG' set..."
        if (logLevel != "0" && logLevel != "100") runIn(1800, "logsOff")
    }
}

boolean isDeveloperHub() {
    return generateMD5(location.hub.zigbeeId as String) == "125fceabd0413141e34bb859cd15e067_disabled"
}

def getEnvironmentObject() {
    if(isDriver()) {
        return device
    } else {
        return app
    }
}

private def getFilteredDeviceDriverName() {
    def deviceDriverName = getDeviceInfoByName('name')
    if(deviceDriverName.toLowerCase().endsWith(' (parent)')) {
        deviceDriverName = deviceDriverName.substring(0, deviceDriverName.length()-9)
    }
    return deviceDriverName
}

private def getFilteredDeviceDisplayName() {
    def deviceDisplayName = device.displayName.replace(' (parent)', '').replace(' (Parent)', '')
    return deviceDisplayName
}

def generate_preferences(configuration_model) {
    def configuration = new XmlSlurper().parseText(configuration_model)
   
    configuration.Value.each {
        if(it.@hidden != "true" && it.@disabled != "true") {
            switch(it.@type) {   
                case "number":
                    input("${it.@index}", "number",
                        title:"${addTitleDiv(it.@label)}" + "${it.Help}",
                        description: makeTextItalic(it.@description),
                        range: "${it.@min}..${it.@max}",
                        defaultValue: "${it.@value}",
                        submitOnChange: it.@submitOnChange == "true",
                        displayDuringSetup: "${it.@displayDuringSetup}")
                    break
                case "list":
                    def items = []
                    it.Item.each { items << ["${it.@value}":"${it.@label}"] }
                    input("${it.@index}", "enum",
                        title:"${addTitleDiv(it.@label)}" + "${it.Help}",
                        description: makeTextItalic(it.@description),
                        defaultValue: "${it.@value}",
                        submitOnChange: it.@submitOnChange == "true",
                        displayDuringSetup: "${it.@displayDuringSetup}",
                        options: items)
                    break
                case "password":
                    input("${it.@index}", "password",
                            title:"${addTitleDiv(it.@label)}" + "${it.Help}",
                            description: makeTextItalic(it.@description),
                            submitOnChange: it.@submitOnChange == "true",
                            displayDuringSetup: "${it.@displayDuringSetup}")
                    break
                case "decimal":
                    input("${it.@index}", "decimal",
                            title:"${addTitleDiv(it.@label)}" + "${it.Help}",
                            description: makeTextItalic(it.@description),
                            range: "${it.@min}..${it.@max}",
                            defaultValue: "${it.@value}",
                            submitOnChange: it.@submitOnChange == "true",
                            displayDuringSetup: "${it.@displayDuringSetup}")
                    break
                case "bool":
                    input("${it.@index}", "bool",
                            title:"${addTitleDiv(it.@label)}" + "${it.Help}",
                            description: makeTextItalic(it.@description),
                            defaultValue: "${it.@value}",
                            submitOnChange: it.@submitOnChange == "true",
                            displayDuringSetup: "${it.@displayDuringSetup}")
                    break
            }
        }
    }
}

/*
    General Mathematical and Number Methods
*/
BigDecimal round2(BigDecimal number, Integer scale) {
    Integer pow = 10;
    for (Integer i = 1; i < scale; i++)
        pow *= 10;
    BigDecimal tmp = number * pow;
    return ( (Float) ( (Integer) ((tmp - (Integer) tmp) >= 0.5f ? tmp + 1 : tmp) ) ) / pow;
}

String generateMD5(String s) {
    if(s != null) {
        return MessageDigest.getInstance("MD5").digest(s.bytes).encodeHex().toString()
    } else {
        return "null"
    }
}

Integer extractInt(String input) {
  return input.replaceAll("[^0-9]", "").toInteger()
}

/**
 * --END-- ALL DEFAULT METHODS (helpers-all-default)
 */

/**
 * DRIVER METADATA METHODS (helpers-driver-metadata)
 *
 * These methods are to be used in (and/or with) the metadata section of drivers and
 * is also what contains the CSS handling and styling.
 */

// These methods can be executed in both the NORMAL driver scope as well
// as the Metadata scope.
private Map getMetaConfig() {
    // This method can ALSO be executed in the Metadata Scope
    def metaConfig = getDataValue('metaConfig')
    if(metaConfig == null) {
        metaConfig = [:]
    } else {
        metaConfig = parseJson(metaConfig)
    }
    return metaConfig
}

boolean isCSSDisabled(Map metaConfig=null) {
    if(metaConfig==null) metaConfig = getMetaConfig()
    boolean disableCSS = false
    if(metaConfig.containsKey("disableCSS")) disableCSS = metaConfig["disableCSS"]
    return disableCSS
}

// These methods are used to set which elements to hide. 
// They have to be executed in the NORMAL driver scope.
private void saveMetaConfig(Map metaConfig) {
    updateDataValue('metaConfig', JsonOutput.toJson(metaConfig))
}

private Map setSomethingToHide(String type, List something, Map metaConfig=null) {
    if(metaConfig==null) metaConfig = getMetaConfig()
    def oldData = []
    something = something.unique()
    if(!metaConfig.containsKey("hide")) {
        metaConfig["hide"] = [type:something]
    } else {
        //logging("setSomethingToHide 1 else: something: '$something', type:'$type' (${metaConfig["hide"]}) containsKey:${metaConfig["hide"].containsKey(type)}", 1)
        if(metaConfig["hide"].containsKey(type)) {
            //logging("setSomethingToHide 1 hasKey else: something: '$something', type:'$type' (${metaConfig["hide"]}) containsKey:${metaConfig["hide"].containsKey(type)}", 1)
            metaConfig["hide"][type].addAll(something)
        } else {
            //logging("setSomethingToHide 1 noKey else: something: '$something', type:'$type' (${metaConfig["hide"]}) containsKey:${metaConfig["hide"].containsKey(type)}", 1)
            metaConfig["hide"][type] = something
        }
        //metaConfig["hide"]["$type"] = oldData
        //logging("setSomethingToHide 2 else: something: '$something', type:'$type' (${metaConfig["hide"]}) containsKey:${metaConfig["hide"].containsKey(type)}", 1)
    }
    saveMetaConfig(metaConfig)
    logging("setSomethingToHide() = ${metaConfig}", 1)
    return metaConfig
}

private Map clearTypeToHide(String type, Map metaConfig=null) {
    if(metaConfig==null) metaConfig = getMetaConfig()
    if(!metaConfig.containsKey("hide")) {
        metaConfig["hide"] = [(type):[]]
    } else {
        metaConfig["hide"][(type)] = []
    }
    saveMetaConfig(metaConfig)
    logging("clearTypeToHide() = ${metaConfig}", 1)
    return metaConfig
}

Map clearThingsToHide(Map metaConfig=null) {
    metaConfig = setSomethingToHide("other", [], metaConfig=metaConfig)
    metaConfig["hide"] = [:]
    saveMetaConfig(metaConfig)
    logging("clearThingsToHide() = ${metaConfig}", 1)
    return metaConfig
}

Map setDisableCSS(boolean value, Map metaConfig=null) {
    if(metaConfig==null) metaConfig = getMetaConfig()
    metaConfig["disableCSS"] = value
    saveMetaConfig(metaConfig)
    logging("setDisableCSS(value = $value) = ${metaConfig}", 1)
    return metaConfig
}

Map setStateCommentInCSS(String stateComment, Map metaConfig=null) {
    if(metaConfig==null) metaConfig = getMetaConfig()
    metaConfig["stateComment"] = stateComment
    saveMetaConfig(metaConfig)
    logging("setStateCommentInCSS(stateComment = $stateComment) = ${metaConfig}", 1)
    return metaConfig
}

Map setCommandsToHide(List commands, Map metaConfig=null) {
    metaConfig = setSomethingToHide("command", commands, metaConfig=metaConfig)
    logging("setCommandsToHide(${commands})", 1)
    return metaConfig
}

Map clearCommandsToHide(Map metaConfig=null) {
    metaConfig = clearTypeToHide("command", metaConfig=metaConfig)
    logging("clearCommandsToHide(metaConfig=${metaConfig})", 1)
    return metaConfig
}

Map setStateVariablesToHide(List stateVariables, Map metaConfig=null) {
    metaConfig = setSomethingToHide("stateVariable", stateVariables, metaConfig=metaConfig)
    logging("setStateVariablesToHide(${stateVariables})", 1)
    return metaConfig
}

Map clearStateVariablesToHide(Map metaConfig=null) {
    metaConfig = clearTypeToHide("stateVariable", metaConfig=metaConfig)
    logging("clearStateVariablesToHide(metaConfig=${metaConfig})", 1)
    return metaConfig
}

Map setCurrentStatesToHide(List currentStates, Map metaConfig=null) {
    metaConfig = setSomethingToHide("currentState", currentStates, metaConfig=metaConfig)
    logging("setCurrentStatesToHide(${currentStates})", 1)
    return metaConfig
}

Map clearCurrentStatesToHide(Map metaConfig=null) {
    metaConfig = clearTypeToHide("currentState", metaConfig=metaConfig)
    logging("clearCurrentStatesToHide(metaConfig=${metaConfig})", 1)
    return metaConfig
}

Map setDatasToHide(List datas, Map metaConfig=null) {
    metaConfig = setSomethingToHide("data", datas, metaConfig=metaConfig)
    logging("setDatasToHide(${datas})", 1)
    return metaConfig
}

Map clearDatasToHide(Map metaConfig=null) {
    metaConfig = clearTypeToHide("data", metaConfig=metaConfig)
    logging("clearDatasToHide(metaConfig=${metaConfig})", 1)
    return metaConfig
}

Map setPreferencesToHide(List preferences, Map metaConfig=null) {
    metaConfig = setSomethingToHide("preference", preferences, metaConfig=metaConfig)
    logging("setPreferencesToHide(${preferences})", 1)
    return metaConfig
}

Map clearPreferencesToHide(Map metaConfig=null) {
    metaConfig = clearTypeToHide("preference", metaConfig=metaConfig)
    logging("clearPreferencesToHide(metaConfig=${metaConfig})", 1)
    return metaConfig
}

// These methods are for executing inside the metadata section of a driver.
def metaDataExporter() {
    //log.debug "getEXECUTOR_TYPE = ${getEXECUTOR_TYPE()}"
    List filteredPrefs = getPreferences()['sections']['input'].name[0]
    //log.debug "filteredPrefs = ${filteredPrefs}"
    if(filteredPrefs != []) updateDataValue('preferences', "${filteredPrefs}".replaceAll("\\s",""))
}

// These methods are used to add CSS to the driver page
// This can be used for, among other things, to hide Commands
// They HAVE to be run in getDriverCSS() or getDriverCSSWrapper()!

/* Example usage:
r += getCSSForCommandsToHide(["off", "refresh"])
r += getCSSForStateVariablesToHide(["alertMessage", "mac", "dni", "oldLabel"])
r += getCSSForCurrentStatesToHide(["templateData", "tuyaMCU", "needUpdate"])
r += getCSSForDatasToHide(["preferences", "appReturn"])
r += getCSSToChangeCommandTitle("configure", "Run Configure2")
r += getCSSForPreferencesToHide(["numSwitches", "deviceTemplateInput"])
r += getCSSForPreferenceHiding('<none>', overrideIndex=getPreferenceIndex('<none>', returnMax=true) + 1)
r += getCSSForHidingLastPreference()
r += '''
form[action*="preference"]::before {
    color: green;
    content: "Hi, this is my content"
}
form[action*="preference"] div.mdl-grid div.mdl-cell:nth-of-type(2) {
    color: green;
}
form[action*="preference"] div[for^=preferences] {
    color: blue;
}
h3, h4, .property-label {
    font-weight: bold;
}
'''
*/

String getDriverCSSWrapper() {
    Map metaConfig = getMetaConfig()
    boolean disableCSS = isCSSDisabled(metaConfig=metaConfig)
    String defaultCSS = '''
    /* This is part of the CSS for replacing a Command Title */
    div.mdl-card__title div.mdl-grid div.mdl-grid .mdl-cell p::after {
        visibility: visible;
        position: absolute;
        left: 50%;
        transform: translate(-50%, 0%);
        width: calc(100% - 20px);
        padding-left: 5px;
        padding-right: 5px;
        margin-top: 0px;
    }
    /* This is general CSS Styling for the Driver page */
    h3, h4, .property-label {
        font-weight: bold;
    }
    .preference-title {
        font-weight: bold;
    }
    .preference-description {
        font-style: italic;
    }
    '''
    String r = "<style>"
    
    if(disableCSS == false) {
        r += "$defaultCSS "
        try{
            // We always need to hide this element when we use CSS
            r += " ${getCSSForHidingLastPreference()} "
            
            if(disableCSS == false) {
                if(metaConfig.containsKey("hide")) {
                    if(metaConfig["hide"].containsKey("command")) {
                        r += getCSSForCommandsToHide(metaConfig["hide"]["command"])
                    }
                    if(metaConfig["hide"].containsKey("stateVariable")) {
                        r += getCSSForStateVariablesToHide(metaConfig["hide"]["stateVariable"])
                    }
                    if(metaConfig["hide"].containsKey("currentState")) {
                        r += getCSSForCurrentStatesToHide(metaConfig["hide"]["currentState"])
                    }
                    if(metaConfig["hide"].containsKey("data")) {
                        r += getCSSForDatasToHide(metaConfig["hide"]["data"])
                    }
                    if(metaConfig["hide"].containsKey("preference")) {
                        r += getCSSForPreferencesToHide(metaConfig["hide"]["preference"])
                    }
                }
                if(metaConfig.containsKey("stateComment")) {
                    r += "div#stateComment:after { content: \"${metaConfig["stateComment"]}\" }"
                }
                r += " ${getDriverCSS()} "
            }
        }catch(MissingMethodException e) {
            if(!e.toString().contains("getDriverCSS()")) {
                log.warn "getDriverCSS() Error: $e"
            }
        } catch(e) {
            log.warn "getDriverCSS() Error: $e"
        }
    }
    r += " </style>"
    return r
}

Integer getCommandIndex(String cmd) {
    List commands = device.getSupportedCommands().unique()
    Integer i = commands.findIndexOf{ "$it" == cmd}+1
    //log.debug "getCommandIndex: Seeing these commands: '${commands}', index=$i}"
    return i
}

String getCSSForCommandHiding(String cmdToHide) {
    Integer i = getCommandIndex(cmdToHide)
    String r = ""
    if(i > 0) {
        r = "div.mdl-card__title div.mdl-grid div.mdl-grid .mdl-cell:nth-of-type($i){display: none;}"
    }
    return r
}

String getCSSForCommandsToHide(List commands) {
    String r = ""
    commands.each {
        r += getCSSForCommandHiding(it)
    }
    return r
}

String getCSSToChangeCommandTitle(String cmd, String newTitle) {
    Integer i = getCommandIndex(cmd)
    String r = ""
    if(i > 0) {
        r += "div.mdl-card__title div.mdl-grid div.mdl-grid .mdl-cell:nth-of-type($i) p {visibility: hidden;}"
        r += "div.mdl-card__title div.mdl-grid div.mdl-grid .mdl-cell:nth-of-type($i) p::after {content: '$newTitle';}"
    }
    return r
}

Integer getStateVariableIndex(String stateVariable) {
    def stateVariables = state.keySet()
    Integer i = stateVariables.findIndexOf{ "$it" == stateVariable}+1
    //log.debug "getStateVariableIndex: Seeing these State Variables: '${stateVariables}', index=$i}"
    return i
}

String getCSSForStateVariableHiding(String stateVariableToHide) {
    Integer i = getStateVariableIndex(stateVariableToHide)
    String r = ""
    if(i > 0) {
        r = "ul#statev li.property-value:nth-of-type($i){display: none;}"
    }
    return r
}

String getCSSForStateVariablesToHide(List stateVariables) {
    String r = ""
    stateVariables.each {
        r += getCSSForStateVariableHiding(it)
    }
    return r
}

String getCSSForCurrentStatesToHide(List currentStates) {
    String r = ""
    currentStates.each {
        r += "ul#cstate li#cstate-$it {display: none;}"
    }
    return r
}

Integer getDataIndex(String data) {
    def datas = device.getData().keySet()
    Integer i = datas.findIndexOf{ "$it" == data}+1
    //log.debug "getDataIndex: Seeing these Data Keys: '${datas}', index=$i}"
    return i
}

String getCSSForDataHiding(String dataToHide) {
    Integer i = getDataIndex(dataToHide)
    String r = ""
    if(i > 0) {
        r = "table.property-list tr li.property-value:nth-of-type($i) {display: none;}"
    }
    return r
}

String  getCSSForDatasToHide(List datas) {
    String r = ""
    datas.each {
        r += getCSSForDataHiding(it)
    }
    return r
}

Integer getPreferenceIndex(String preference, boolean returnMax=false) {
    def filteredPrefs = getPreferences()['sections']['input'].name[0]
    //log.debug "getPreferenceIndex: Seeing these Preferences first: '${filteredPrefs}'"
    if(filteredPrefs == [] || filteredPrefs == null) {
        d = getDataValue('preferences')
        //log.debug "getPreferenceIndex: getDataValue('preferences'): '${d}'"
        if(d != null && d.length() > 2) {
            try{
                filteredPrefs = d[1..d.length()-2].tokenize(',')
            } catch(e) {
                // Do nothing
            }
        }
        

    }
    Integer i = 0
    if(returnMax == true) {
        i = filteredPrefs.size()
    } else {
        i = filteredPrefs.findIndexOf{ "$it" == preference}+1
    }
    //log.debug "getPreferenceIndex: Seeing these Preferences: '${filteredPrefs}', index=$i"
    return i
}

String getCSSForPreferenceHiding(String preferenceToHide, Integer overrideIndex=0) {
    Integer i = 0
    if(overrideIndex == 0) {
        i = getPreferenceIndex(preferenceToHide)
    } else {
        i = overrideIndex
    }
    String r = ""
    if(i > 0) {
        r = "form[action*=\"preference\"] div.mdl-grid div.mdl-cell:nth-of-type($i) {display: none;} "
    }else if(i == -1) {
        r = "form[action*=\"preference\"] div.mdl-grid div.mdl-cell:nth-last-child(2) {display: none;} "
    }
    return r
}

String getCSSForPreferencesToHide(List preferences) {
    String r = ""
    preferences.each {
        r += getCSSForPreferenceHiding(it)
    }
    return r
}

String getCSSForHidingLastPreference() {
    return getCSSForPreferenceHiding(null, overrideIndex=-1)
}

/**
 * --END-- DRIVER METADATA METHODS (helpers-driver-metadata)
 */

/**
 * STYLING (helpers-styling)
 *
 * Helper functions included in all Drivers and Apps using Styling
 */
String addTitleDiv(title) {
    return '<div class="preference-title">' + title + '</div>'
}

String addDescriptionDiv(description) {
    return '<div class="preference-description">' + description + '</div>'
}

String makeTextBold(s) {
    // DEPRECATED: Should be replaced by CSS styling!
    if(isDriver()) {
        return "<b>$s</b>"
    } else {
        return "$s"
    }
}

String makeTextItalic(s) {
    // DEPRECATED: Should be replaced by CSS styling!
    if(isDriver()) {
        return "<i>$s</i>"
    } else {
        return "$s"
    }
}

/**
 * --END-- STYLING METHODS (helpers-styling)
 */

/**
 * DRIVER DEFAULT METHODS (helpers-driver-default)
 *
 * General Methods used in ALL drivers except some CHILD drivers
 * Though some may have no effect in some drivers, they're here to
 * maintain a general structure
 */

// Since refresh, with any number of arguments, is accepted as we always have it declared anyway, 
// we use it as a wrapper
// All our "normal" refresh functions take 0 arguments, we can declare one with 1 here...
void refresh(cmd) {
    deviceCommand(cmd)
}
// Call order: installed() -> configure() -> updated() -> initialize() -> refresh()
// Calls installed() -> [configure() -> [updateNeededSettings(), updated() -> [updatedAdditional(), initialize() -> refresh() -> refreshAdditional()], installedAdditional()]
void installed() {
	logging("installed()", 100)
    
    try {
        // Used by certain types of drivers, like Tasmota Parent drivers
        installedPreConfigure()
    } catch (MissingMethodException e) {
        // ignore
    }
	configure()
    try {
        // In case we have some more to run specific to this Driver
        installedAdditional()
    } catch (MissingMethodException e) {
        // ignore
    }
}

// Call order: installed() -> configure() -> updated() -> initialize() -> refresh()
void configure() {
    logging("configure()", 100)
    if(isDriver()) {
        // Do NOT call updateNeededSettings() here!
        updated()
        try {
            // Run the getDriverVersion() command
            def newCmds = getDriverVersion()
            if (newCmds != null && newCmds != []) cmds = cmds + newCmds
        } catch (MissingMethodException e) {
            // ignore
        }
    }
}

void configureDelayed() {
    runIn(10, "configure")
    runIn(30, "refresh")
}

/**
 * --END-- DRIVER DEFAULT METHODS (helpers-driver-default)
 */
