#!include:getHeaderLicense()

/* Inspired by a driver from shin4299 which can be found here:
   https://github.com/shin4299/XiaomiSJ/blob/master/devicetypes/shinjjang/xiaomi-curtain-b1.src/xiaomi-curtain-b1.groovy
*/

#!include:getDefaultImports()
import hubitat.helper.HexUtils

metadata {
	definition (name: "Zigbee - Aqara Smart Curtain Motor", namespace: "markusl", author: "Markus Liljergren", vid: "generic-shade") {
        //capability "Actuator"
        //capability "Light"
		//capability "Switch"
		capability "Sensor"
        capability "WindowShade"
        capability "Battery"

        #!include:getDefaultMetadataCapabilities()
        
        #!include:getDefaultMetadataAttributes()
        attribute "lastCheckin", "String"
        attribute "battery2", "Number"
        attribute "batteryLastReplaced", "String"
        //#include:getDefaultMetadataCommands()
        command "stop"
        //command "altStop"
        //command "altClose"
        //command "altOpen"
        //command "clearPosition"
        //command "reverseCurtain"
        //command "clearPosition2"
        //command "reverseCurtain2"
        
        //command "autoCloseEnable"
        //command "autoCloseDisable"

        command "manualOpenEnable"
        command "manualOpenDisable"

        command "curtainOriginalDirection"
        command "curtainReverseDirection"

        command "calibrationMode"

        command "getPosition"
        command "getPositionAlt"

        //command "sendAttribute", [[name:"Attribute*", type: "STRING", description: "Zigbee Attribute"]]

        // Fingerprint for Xiaomi Aqara Smart Curtain Motor (ZNCLDJ11LM)
        fingerprint profileId: "0104", inClusters: "0000,0004,0003,0005,000A,0102,000D,0013,0006,0001,0406", outClusters: "0019,000A,000D,0102,0013,0006,0001,0406", manufacturer: "LUMI", model: "lumi.curtain"
        
        // Fingerprint for Xiaomi Aqara B1 Smart Curtain Motor (ZNCLDJ12LM)
		fingerprint endpointId: "01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0003, 0102, 000D, 0013, 0001", outClusters: "0003, 000A", manufacturer: "LUMI", model: "lumi.curtain.hagl04", deviceJoinName: "Xiaomi Curtain B1"
        
	}

	simulator {
	}
    
    preferences {
        #!include:getDefaultMetadataPreferences()
        //input name: "mode", type: "bool", title: "Curtain Direction", description: "Reverse Mode ON", required: true, displayDuringSetup: true
        //input name: "onlySetPosition", type: "bool", title: "Use only Set Position", defaultValue: false, required: true, displayDuringSetup: true
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
// https://github.com/zigbeer/zcl-id/blob/master/definitions/cluster_defs.json
// https://github.com/zigbeer/zcl-id/blob/master/definitions/common.json
// https://github.com/TedTolboom/com.xiaomi-mi-zigbee/blob/master/drivers/curtain.hagl04/device.js

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
    //log.debug "in parse"
    #!include:getGenericZigbeeParseHeader()
    
    if (msgMap["profileId"] == "0104" && msgMap["clusterId"] == "000A") {
		logging("Xiaomi Curtain Present Event", 10)
	} else if (msgMap["profileId"] == "0104") {
        // This is probably just a heartbeat event...
        logging("Unhandled KNOWN 0104 event (heartbeat?)- description:${description} | parseMap:${msgMap}", 0)
        logging("RAW: ${msgMap["attrId"]}", 0)
        // catchall: 0104 000A 01 01 0040 00 63A1 00 00 0000 00 00 0000, parseMap:[raw:catchall: 0104 000A 01 01 0040 00 63A1 00 00 0000 00 00 0000, profileId:0104, clusterId:000A, clusterInt:10, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:63A1, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:00, direction:00, data:[00, 00]]
    } else if (msgMap["cluster"] == "0000" && msgMap["attrId"] == "0404") {
        if(msgMap["command"] == "0A") {
            if(msgMap["value"] == "00") {
                //sendEvent(name:"commandValue", value: msgMap["value"])
                // The position event that comes after this one is a real position
                logging("Unhandled KNOWN 0A command event with Value 00 - description:${description} | parseMap:${msgMap}", 10)
            } else {
                logging("Unhandled KNOWN 0A command event - description:${description} | parseMap:${msgMap}", 10)
            }
        } else {
            // Received after sending open/close/setposition commands
            logging("Unhandled KNOWN event - description:${description} | parseMap:${msgMap}", 10)
            //read attr - raw: 63A10100000804042000, dni: 63A1, endpoint: 01, cluster: 0000, size: 08, attrId: 0404, encoding: 20, command: 0A, value: 00, parseMap:[raw:63A10100000804042000, dni:63A1, endpoint:01, cluster:0000, size:08, attrId:0404, encoding:20, command:0A, value:00, clusterInt:0, attrInt:1028]
        }
    } else if (msgMap["cluster"] == "0000" && msgMap["attrId"] == "0005") {
        logging("Unhandled KNOWN event (pressed button) - description:${description} | parseMap:${msgMap}", 0)
        // read attr - raw: 63A1010000200500420C6C756D692E6375727461696E, dni: 63A1, endpoint: 01, cluster: 0000, size: 20, attrId: 0005, encoding: 42, command: 0A, value: 0C6C756D692E6375727461696E, parseMap:[raw:63A1010000200500420C6C756D692E6375727461696E, dni:63A1, endpoint:01, cluster:0000, size:20, attrId:0005, encoding:42, command:0A, value:lumi.curtain, clusterInt:0, attrInt:5]
    } else if (msgMap["cluster"] == "0000" && msgMap["attrId"] == "0007") {
        logging("Unhandled KNOWN event (BASIC_ATTR_POWER_SOURCE) - description:${description} | parseMap:${msgMap}", 0)
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
        logging("KNOWN event (probably battery) - description:${description} | parseMap:${msgMap}", 0)
        // TODO: Test this, I don't have the battery version...
        // 1C (file separator??) is missing in the beginning of the value after doing this encoding...
        if(getDeviceDataByName('model') != "lumi.curtain") {
            sendEvent(parseBattery(msgMap["value"].getBytes().encodeHex().toString().toUpperCase()))
        }
        //read attr - raw: 63A10100004001FF421C03281E05210F00642000082120110727000000000000000009210002, dni: 63A1, endpoint: 01, cluster: 0000, size: 40, attrId: FF01, encoding: 42, command: 0A, value: 1C03281E05210F00642000082120110727000000000000000009210002, parseMap:[raw:63A10100004001FF421C03281E05210F00642000082120110727000000000000000009210002, dni:63A1, endpoint:01, cluster:0000, size:40, attrId:FF01, encoding:42, command:0A, value:(!d ! '	!, clusterInt:0, attrInt:65281]
    } else if (msgMap["cluster"] == "000D" && msgMap["attrId"] == "0055") {
        logging("cluster 000D", 1)
		if (msgMap["size"] == "16" || msgMap["size"] == "1C" || msgMap["size"] == "10") {
            // This is sent just after sending a command to open/close and just after the curtain is done moving
			long theValue = Long.parseLong(msgMap["value"], 16)
			BigDecimal floatValue = Float.intBitsToFloat(theValue.intValue());
			logging("GETTING POSITION: long => ${theValue}, BigDecimal => ${floatValue}", 10)
			curtainPosition = floatValue.intValue()
            // Original 100%:
            // msgMap: [raw:C49701000D1C5500390000C84200F02300000000, dni:C497, endpoint:01, cluster:000D, size:1C, attrId:0055, encoding:39, command:0A, value:42C80000, clusterInt:13, attrInt:85, additionalAttrs:[[value:00000000, encoding:23, attrId:F000, consumedBytes:7, attrInt:61440]]]
            // Original 0%:
            // msgMap: [raw:C49701000D1C5500390000000000F02300000000, dni:C497, endpoint:01, cluster:000D, size:1C, attrId:0055, encoding:39, command:0A, value:00000000, clusterInt:13, attrInt:85, additionalAttrs:[[value:00000000, encoding:23, attrId:F000, consumedBytes:7, attrInt:61440]]]

            // B1 100%:
            // msgMap: [raw:E85F01000D1C5500390000004000F02300000200, dni:E85F, endpoint:01, cluster:000D, size:1C, attrId:0055, encoding:39, command:0A, value:40000000, clusterInt:13, attrInt:85, additionalAttrs:[[value:00020000, encoding:23, attrId:F000, consumedBytes:7, attrInt:61440]]]
            // B1 60% ????:
            // msgMap: [raw:E85F01000D1C5500390000004000F02300000200, dni:E85F, endpoint:01, cluster:000D, size:1C, attrId:0055, encoding:39, command:0A, value:40000000, clusterInt:13, attrInt:85, additionalAttrs:[[value:00020000, encoding:23, attrId:F000, consumedBytes:7, attrInt:61440]]]
            // B1 0%
            // msgMap: [raw:E85F01000D1C5500390000000000F02300000200, dni:E85F, endpoint:01, cluster:000D, size:1C, attrId:0055, encoding:39, command:0A, value:00000000, clusterInt:13, attrInt:85, additionalAttrs:[[value:00020000, encoding:23, attrId:F000, consumedBytes:7, attrInt:61440]]]
            // Only send position events when the curtain is done moving
            //if(device.currentValue('commandValue') == "00") {
            //    sendEvent(name:"commandValue", value: "-1")
                
            //}
            if(getDeviceDataByName('model') == "lumi.curtain") {
                positionEvent(curtainPosition)
                //sendHubCommand(new hubitat.device.HubAction(zigbee.readAttribute(CLUSTER_WINDOW_COVERING, 0x0008)[0]))
            } else {
                hubitat.device.HubMultiAction allActions = new hubitat.device.HubMultiAction()
                allActions.add(new hubitat.device.HubAction(zigbee.readAttribute(0x0013, 0x0055)[0], hubitat.device.Protocol.ZIGBEE))
                //allActions.add(new hubitat.device.HubAction("delay 1000"))
                //allActions.add(new hubitat.device.HubAction(zigbee.readAttribute(CLUSTER_WINDOW_COVERING, 0x0008)[0], hubitat.device.Protocol.ZIGBEE))
                sendHubCommand(allActions)
            }
		} else if (msgMap["size"] == "28" && msgMap["value"] == "00000000") {
			logging("done…", 1)
			cmds += zigbee.readAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE)
		}
	} else if (msgMap["clusterId"] == "0001" && msgMap["attrId"] == "0021") {
        if(getDeviceDataByName('model') != "lumi.curtain") {
            def bat = msgMap["value"]
            long value = Long.parseLong(bat, 16)/2
            logging("Battery: ${value}%, ${bat}", 10)
            sendEvent(name:"battery", value: value)
        }

	} else {
		log.warn "Unhandled Event - description:${description} | msgMap:${msgMap}"
	}
    logging("PARSE END-----------------------", 1)
    #!include:getGenericZigbeeParseFooter()
}

def getPosition() {
    logging("getPosition()", 1)
	def cmd = []
	cmd += zigbee.readAttribute(0x0013, 0x0055)
    cmd += zigbee.readAttribute(CLUSTER_POWER, 0x0021)
    cmd += zigbee.readAttribute(CLUSTER_WINDOW_POSITION, 0x0055)
    logging("cmd: $cmd", 1)
    return cmd 
}

def getPositionAlt() {
    logging("getPositionAlt()", 1)
	def cmd = []
	cmd += zigbee.readAttribute(0x0013, 0x0055, [mfgCode: "0x115F"])
    cmd += zigbee.readAttribute(CLUSTER_POWER, 0x0021, [mfgCode: "0x115F"])
    cmd += zigbee.readAttribute(CLUSTER_WINDOW_POSITION, 0x0055, [mfgCode: "0x115F"])
    logging("cmd: $cmd", 1)
    return cmd 
}

def positionEvent(curtainPosition) {
	def windowShadeStatus = ""
	//if(mode == true) {
    //    curtainPosition = 100 - curtainPosition
	//}
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
    if(device.currentValue('position') != curtainPosition) {
        logging("CHANGING device.currentValue('position') = ${device.currentValue('position')}, curtainPosition = $curtainPosition", 1)
        sendEvent(name:"windowShade", value: windowShadeStatus)
        sendEvent(name:"position", value: curtainPosition)
    }
	//sendEvent(name:"switch", value: (windowShadeStatus == "closed" ? "off" : "on"))
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

ArrayList close() {
    logging("close()", 1)
	return setPosition(0)    
}

ArrayList open() {
    logging("open()", 1)
	return setPosition(100)    
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
    //cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0x0401, 0x21, 0x0100000000070007, [mfgCode: "0x115F"])
    logging("cmd=${cmd}", 1)
    return cmd
}



String hexToASCII(String hexValue)
{
    StringBuilder output = new StringBuilder("")
    for (int i = 0; i < hexValue.length(); i += 2)
    {
        String str = hexValue.substring(i, i + 2)
        output.append((char) Integer.parseInt(str, 16) + 30)
        logging("${Integer.parseInt(str, 16)}", 10)
    }
    logging("${output.toString()}", 10)
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

def sendAttribute(String attribute) {
    attribute = attribute.replace(' ', '')
    logging("sendAttribute(attribute=$attribute) (0x${Long.toHexString(Long.decode("0x$attribute"))})", 1)
    def cmd = []
    cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, Long.decode("0x$attribute"), [mfgCode: "0x115F"])
    logging("cmd=${cmd}, size=${cmd.size()}", 10)
    return cmd
}

ArrayList manualOpenEnable() {
    logging("manualOpenEnable()", 1)
    ArrayList cmd = []
    if(getDeviceDataByName('model') == "lumi.curtain") {
        cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700080000040012, [mfgCode: "0x115F"])
    } else {
        cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF29, 0x10, 0x00, [mfgCode: "0x115F"])
    }
    logging("cmd=${cmd}", 1)
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
    logging("cmd=${cmd}", 1)
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
    logging("cmd=${cmd}", 1)
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
    logging("cmd=${cmd}", 1)
    return cmd
}

ArrayList calibrationMode() {
    logging("calibrationMode()", 1)
    ArrayList cmd = []
    if(getDeviceDataByName('model') == "lumi.curtain") {
        cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700010000040012, [mfgCode: "0x115F"])
    } else {
        cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF27, 0x10, 0x00, [mfgCode: "0x115F"])
    }
    logging("cmd=${cmd}", 1)
    return cmd
}

def reverseCurtain2() {
    logging("reverseCurtain2()", 1)
	def cmd = []
	//cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF28, 0x10, 0x00, [mfgCode: "0x115F"])
    // xiaomi mfg: 0x115F
    //cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0x0401, 0x21, 0x3100, [mfgCode: "0x115F"])
    
    // Works to make value empty
    //cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0000, [mfgCode: "0x115F"])
    
    // Works to make scrambled characters:
    //cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x1010, [mfgCode: "0x115F"])
    
    // If opening by hand is disabled??? and I change direction to original (works as expected except opening by hand!)
    //cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700020000040012, [mfgCode: "0x115F"])
    // If opening by hand is enabled and I change direction to reverse
    //cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700020001040012, [mfgCode: "0x115F"])

    // If opening by hand is disabled??? and I change direction to original (works as expected except opening by hand!)
    //cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700020000040112, [mfgCode: "0x115F"])


    // If direction is reverse and I change opening by hand to off
    cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700080001040112, [mfgCode: "0x115F"])
    // If direction is reverse and I change opening by hand to on
    //cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700080001040012, [mfgCode: "0x115F"])
    
    // If direction is original and I change opening by hand to off (this works and this disables opening by hand)
    //cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700080000040112, [mfgCode: "0x115F"])
    
    // If direction is original and I change opening by hand to on (this works and this enables opening by hand)
    //cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700080000040012, [mfgCode: "0x115F"])
    
    // If opening by hand is disabled and I change direction to reverse
    //cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700080001040112, [mfgCode: "0x115F"])

    //cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700020001040012, [mfgCode: "0x115F"])
    
    //cmd += zigbee.command(CLUSTER_BASIC, 0x0401, [mfgCode: "0x115F"], "0x0100000101070007")
    //cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0x0401, 0x21, 0x0100000101070007, [mfgCode: "0x115F"])
    // read attr - raw: 0759010000180104420700000100000000, dni: 0759, endpoint: 01, cluster: 0000, 
    // size: 18, attrId: 0401, encoding: 42, command: 01, value: 0700000100000000
    //                                                           0100000101070007
    //                                                           0700000100000000
    // [he wattr 0x0759 0x01 0x0000 0x0401 0x21 {0100000101070007} {115F}, delay 2000, 
    //  he cmd 0x0759 0x01 0x0000 0x401 {0x0100000101070007} {115F}, delay 2000]
    logging("cmd=${cmd}, size=${cmd.size()}", 10)
    //test(cmd)
    //zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700020001040012, [mfgCode: "0x115F"])
    
    //def hubAction = new hubitat.device.HubAction("he wattr 0x0759 0x01 0x0000 0x0401 0x42 {0x0100000101070007} {115F}", hubitat.device.Protocol.ZIGBEE)
    //def hubAction = new hubitat.device.HubAction("he wattr 0x0759 0x01 0x0000 0x0401 0x42 {0100000101070007} {115F}", hubitat.device.Protocol.ZIGBEE)
    //def hubAction = new hubitat.device.HubAction("he wattr 0x60FD 0x01 0x0000 0x0401 0x42 {7777} {115F}", hubitat.device.Protocol.ZIGBEE)
    //def hubAction = new hubitat.device.HubAction("he wattr 0x60FD 0x01 0x0000 0x0401 0x42 {7777} {115F}", hubitat.device.Protocol.ZIGBEE)
    //def hubAction = new hubitat.device.HubAction("he wattr 0x60FD 0x01 0x0000 0x0401 0x21 {00112233} {115F}", hubitat.device.Protocol.ZIGBEE)
    //def hubAction = new hubitat.device.HubAction("he wattr 0x60FD 0x01 0x0000 0x0401 0x21 {7777} {115F}", hubitat.device.Protocol.ZIGBEE)
    
    //Testing:
    //def hubAction = new hubitat.device.HubAction("he wattr 0x60FD 0x01 0x0000 0x0401 0x21 {${hexToASCII('0100000101070007')}} {115F}", hubitat.device.Protocol.ZIGBEE)

    // Use the values from here:
    // https://github.com/Koenkk/zigbee2mqtt/issues/1639#issuecomment-565374656

    // raw: 60FD010000180104420700000001000000, dni: 60FD, endpoint: 01, cluster: 0000, size: 18, attrId: 0401, encoding: 42, command: 01, value: 0700000001000000
    //0700000001001400
    //0700000001000000
    //0700020000040012
    //0700020001040012
    //def hubAction = new hubitat.device.HubAction("he wattr 0x60FD 0x01 0x0000 0x0401 0x42 {0700020000040012} {115F}", hubitat.device.Protocol.ZIGBEE)

    // Same as the above that works:
    //def hubAction = new hubitat.device.HubAction("he wattr 0x60FD 0x01 0x0000 0x0401 0x42 {0000} {115F}", hubitat.device.Protocol.ZIGBEE)
    //def hubAction = new hubitat.device.HubAction("he wattr 0x60FD 0x01 0x0000 0x0401 0x42 {1010} {115F}", hubitat.device.Protocol.ZIGBEE)
    
    //
    //logging("hubAction=${hubAction}", 10)
    //sendHubCommand(hubAction)
    return cmd
}

def test(Integer test) {

}

def altClose() {
    logging("altClose()", 1)
	def cmd = []
	cmd += zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_CLOSE)
    logging("cmd=${cmd}", 1)
    return cmd  
}

ArrayList on() {
    logging("on()", 1)
	return open()
}

ArrayList off() {
    logging("off()", 1)
	return close()
}

ArrayList stop() {
    logging("stop()", 1)
    ArrayList cmd = []
	cmd += zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_PAUSE)
    //cmd += zigbee.readAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE)
    logging("cmd=${cmd}", 1)
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
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF29, 0x10, 0x00, [mfgCode: "0x115F"])
    logging("cmd=${cmd}", 1)
    return cmd
}

def disableAutoClose() {
    logging("disableAutoClose()", 1)
    def cmd = []
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF29, 0x10, 0x01, [mfgCode: "0x115F"])
    logging("cmd=${cmd}", 1)
    return cmd
}

ArrayList setPosition(position) {
    if (position == null) {position = 0}
    ArrayList cmd = []
    position = position as Integer
    logging("setPosition(position: ${position})", 1)
    Integer currentPosition = device.currentValue("position")
    if (position > currentPosition) {
        sendEvent(name: "windowShade", value: "opening")
    } else if (position < currentPosition) {
        sendEvent(name: "windowShade", value: "closing")
    }
    if(position == 100 && getDeviceDataByName('model') == "lumi.curtain") {
        logging("Command: Open", 1)
        logging("cluster: ${CLUSTER_ON_OFF}, command: ${COMMAND_OPEN}", 0)
        cmd += zigbee.command(CLUSTER_ON_OFF, COMMAND_CLOSE)
    } else if (position < 1 && getDeviceDataByName('model') == "lumi.curtain") {
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

/*
    -----------------------------------------------------------------------------
    Everything below here are LIBRARY includes and should NOT be edited manually!
    -----------------------------------------------------------------------------
    --- Nothings to edit here, move along! --------------------------------------
    -----------------------------------------------------------------------------
*/

#!include:getDefaultFunctions(driverVersionSpecial="v0.9.1.MMDD")

#!include:getLoggingFunction()

#!include:getHelperFunctions('all-default')

#!include:getHelperFunctions('driver-metadata')

#!include:getHelperFunctions('styling')

#!include:getHelperFunctions('driver-default')
