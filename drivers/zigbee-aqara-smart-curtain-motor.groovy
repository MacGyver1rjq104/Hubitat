#!include:getHeaderLicense()

/* 
    Inspired by a driver from shin4299 which can be found here:
    https://github.com/shin4299/XiaomiSJ/blob/master/devicetypes/shinjjang/xiaomi-curtain-b1.src/xiaomi-curtain-b1.groovy
*/

#!include:getDefaultImports()
import hubitat.helper.HexUtils

metadata {
	definition (name: "Zigbee - Aqara Smart Curtain Motor", namespace: "markusl", author: "Markus Liljergren", vid: "generic-shade") {
        #!include:getDefaultMetadataCapabilitiesForZigbeeDevices()
        
        // Device Specific Capabilities
        capability "Refresh"
        capability "Battery"
        capability "PowerSource"
        capability "WindowShade"
        
        // These 4 capabilities are included to be compatible with integrations like Alexa:
        capability "Actuator"
        capability "Switch"
        capability "Light"
        capability "SwitchLevel"

        #!include:getDefaultMetadataAttributes()
        #!include:getDefaultZigbeeMetadataAttributes()
        
        command "stop"
        command "manualOpenEnable"
        command "manualOpenDisable"
        command "curtainOriginalDirection"
        command "curtainReverseDirection"
        command "trackDiscoveryMode"

        // Uncomment these Commands for TESTING, not needed normally:
        //command "getBattery"    // comment before release!
        //command "installed"     // just used for testing that Installed runs properly, comment before release!
        //command "sendAttribute", [[name:"Attribute*", type: "STRING", description: "Zigbee Attribute"]]
        //command "parse", [[name:"Description*", type: "STRING", description: "description"]]

        // Aqara Smart Curtain Motor (ZNCLDJ11LM)
        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0004,0003,0005,000A,0102,000D,0013,0006,0001,0406", outClusters: "0019,000A,000D,0102,0013,0006,0001,0406", manufacturer: "LUMI", model: "lumi.curtain"
        
        // Aqara B1 Smart Curtain Motor (ZNCLDJ12LM)
		fingerprint endpointId: "01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0003, 0102, 000D, 0013, 0001", outClusters: "0003, 000A", manufacturer: "LUMI", model: "lumi.curtain.hagl04", deviceJoinName: "Xiaomi Curtain B1"
	}

    preferences {
        #!include:getDefaultMetadataPreferences(includeCSS=True, includeRunReset=False)
        #!include:getDefaultMetadataPreferencesForZigbeeDevices()
	}
}

#!include:getDeviceInfoFunction()

/* These functions are unique to each driver */
// https://github.com/zigbeer/zcl-id/blob/master/definitions/cluster_defs.json
// https://github.com/zigbeer/zcl-id/blob/master/definitions/common.json

ArrayList<String> refresh() {
    logging("refresh() model='${getDeviceDataByName('model')}'", 10)
    // http://ftp1.digi.com/support/images/APP_NOTE_XBee_ZigBee_Device_Profile.pdf
    // https://docs.hubitat.com/index.php?title=Zigbee_Object
    // https://docs.smartthings.com/en/latest/ref-docs/zigbee-ref.html
    // https://www.nxp.com/docs/en/user-guide/JN-UG-3115.pdf

    getDriverVersion()
    configurePresence()
    setLogsOffTask(noLogWarning=true)

    ArrayList<String> cmd = []
    cmd += getPosition()
    cmd += zigbee.readAttribute(CLUSTER_BASIC, 0xFF01, [mfgCode: "0x115F"])
    //cmd += zigbee.readAttribute(CLUSTER_BASIC, 0xFF02, [mfgCode: "0x115F"])
    if(getDeviceDataByName('model') != "lumi.curtain") { 
        cmd += getBattery()
    }
    logging("refresh cmd: $cmd", 1)
    sendZigbeeCommands(cmd)
    
}

// Called from initialize()
void initializeAdditional() {
    logging("initializeAdditional()", 100)
    setCleanModelName()
    updateDataValue("endpointId", "01")
    makeSchedule()
    getDriverVersion()
}

// Called from installed()
void installedAdditional() {
    logging("installedAdditional()", 100)
    setCleanModelName()
    sendEvent(name:"windowShade", value: 'unknown')
    sendEvent(name:"switch", value: 'off')
    sendEvent(name:"level", value: 0)
    //sendEvent(name:"position", value: null)     // This set it to the string "null" in current versions of HE (2.2.0 and earlier)
}

void makeSchedule() {
    logging("makeSchedule()", 100)
    // https://www.freeformatter.com/cron-expression-generator-quartz.html
    if(getDeviceDataByName('model') != "lumi.curtain") {
        Random rnd = new Random()
        schedule("${rnd.nextInt(59)} ${rnd.nextInt(59)} 5/12 * * ? *", 'getBattery')
    } else {
        unschedule('getBattery')
    }
}

ArrayList<String> parse(String description) {
    #!include:getGenericZigbeeParseHeader(loglevel=1)
    //logging("msgMap: ${msgMap}", 1)

    if(msgMap["profileId"] == "0104" && msgMap["clusterId"] == "000A") {
		logging("Xiaomi Curtain Present Event", 1)
        sendlastCheckinEvent(minimumMinutesToRepeat=60)
	} else if(msgMap["profileId"] == "0104") {
        // TODO: Check if this is just a remnant and that we don't just catch this in the clause above?
        // This is probably just a heartbeat event...
        logging("Unhandled KNOWN 0104 event (heartbeat?)- description:${description} | parseMap:${msgMap}", 0)
        logging("RAW: ${msgMap["attrId"]}", 0)
        // Heartbeat event Description:
        // catchall: 0104 000A 01 01 0040 00 63A1 00 00 0000 00 00 0000

        // parseMap:[raw:catchall: 0104 000A 01 01 0040 00 63A1 00 00 0000 00 00 0000, profileId:0104, clusterId:000A, clusterInt:10, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:63A1, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:00, direction:00, data:[00, 00]]
    } else if(msgMap["cluster"] == "0000" && msgMap["attrId"] == "0404") {
        if(msgMap["command"] == "0A") {
            if(msgMap["value"] == "00" && getDeviceDataByName('model') == "lumi.curtain") {
                // The position event that comes after this one is a real position
                logging("HANDLED KNOWN 0A command event with Value 00 - description:${description} | parseMap:${msgMap}", 1)
                logging("Sending request for the actual position...", 1)
                cmd += zigbee.readAttribute(CLUSTER_WINDOW_POSITION, 0x0055)
            } else {
                logging("Unhandled KNOWN 0A command event - description:${description} | parseMap:${msgMap}", 0)
            }
        } else {
            // Received after sending open/close/setposition commands
            logging("Unhandled KNOWN event - description:${description} | parseMap:${msgMap}", 0)
            //read attr - raw: 63A10100000804042000, dni: 63A1, endpoint: 01, cluster: 0000, size: 08, attrId: 0404, encoding: 20, command: 0A, value: 00, parseMap:[raw:63A10100000804042000, dni:63A1, endpoint:01, cluster:0000, size:08, attrId:0404, encoding:20, command:0A, value:00, clusterInt:0, attrInt:1028]
        }
    } else if(msgMap["clusterId"] == "0013" && msgMap["command"] == "00") {
        logging("Unhandled KNOWN event - description:${description} | parseMap:${msgMap}", 0)
        // Event Description:
        // read attr - raw: 63A1010000200500420C6C756D692E6375727461696E, dni: 63A1, endpoint: 01, cluster: 0000, size: 20, attrId: 0005, encoding: 42, command: 0A, value: 0C6C756D692E6375727461696E
    } else if(msgMap["cluster"] == "0000" && msgMap["attrId"] == "0005") {
        logging("Reset button pressed - description:${description} | parseMap:${msgMap}", 1)
        // The value from this command is the device model string
        setCleanModelName(newModelToSet=msgMap["value"])
        refresh()
    } else if(msgMap["cluster"] == "0000" && msgMap["attrId"] == "0006") {
        logging("Got a date - description:${description} | parseMap:${msgMap}", 1)
        // Sends a date, maybe product release date since it is the same on different devices?
        
        // This is sent when entering Track Discovery Mode

        // Original Curtain Description:
        // read attr - raw: 25D80100001C0600420A30382D31332D32303138, dni: 25D8, endpoint: 01, cluster: 0000, size: 1C, attrId: 0006, encoding: 42, command: 0A, value: 0A30382D31332D32303138
        // msgMap:[raw:25D80100001C0600420A30382D31332D32303138, dni:25D8, endpoint:01, cluster:0000, size:1C, attrId:0006, encoding:42, command:0A, value:08-13-2018, clusterInt:0, attrInt:6]
    } else if(msgMap["cluster"] == "0000" && msgMap["attrId"] == "0007") {
        logging("Handled KNOWN event (BASIC_ATTR_POWER_SOURCE) - description:${description} | parseMap:${msgMap}", 1)
        if(msgMap["value"] == "03") {
            sendEvent(name:"powerSource", value: "battery")
        } else if(msgMap["value"] == "04") {
            sendEvent(name:"powerSource", value: "dc")
        } else {
            sendEvent(name:"powerSource", value: "unknown")
        }
        // Description received for zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE):
        // read attr - raw: 63A10100000A07003001, dni: 63A1, endpoint: 01, cluster: 0000, size: 0A, attrId: 0007, encoding: 30, command: 01, value: 01
    } else if(msgMap["cluster"] == "0102" && msgMap["attrId"] == "0008") {
        logging("Position event (after pressing stop) - description:${description} | parseMap:${msgMap}", 0)
        Long theValue = Long.parseLong(msgMap["value"], 16)
        curtainPosition = theValue.intValue()
        logging("GETTING POSITION from cluster 0102: int => ${curtainPosition}", 1)
        positionEvent(curtainPosition)
        // Position event Descriptions:
        //read attr - raw: 63A1010102080800204E, dni: 63A1, endpoint: 01, cluster: 0102, size: 08, attrId: 0008, encoding: 20, command: 0A, value: 4E
        //read attr - raw: 63A1010102080800203B, dni: 63A1, endpoint: 01, cluster: 0102, size: 08, attrId: 0008, encoding: 20, command: 0A, value: 3B
    } else if(msgMap["cluster"] == "0000" && (msgMap["attrId"] == "FF01" || msgMap["attrId"] == "FF02")) {
        if(msgMap["encoding"] == "42") {
            // First redo the parsing using a different encoding:
            msgMap = zigbee.parseDescriptionAsMap(description.replace('encoding: 42', 'encoding: 41'))
            msgMap["encoding"] = "42"
            msgMap["value"] = parseXiaomiStruct(msgMap["value"], isFCC0=false)
        }
        logging("KNOWN event (Xiaomi/Aqara specific data structure) - description:${description} | parseMap:${msgMap}", 0)
        // Xiaomi/Aqara specific data structure, contains data we probably don't need
        // FF01 event Description from Original Curtain:
        // read attr - raw: A5C50100004001FF421C03281F05212B00642058082120110727000000000000000009210304, dni: A5C5, endpoint: 01, cluster: 0000, size: 40, attrId: FF01, encoding: 42, command: 0A, value: 1C03281F05212B00642058082120110727000000000000000009210304
        
        // read attr - raw: 25D80100004001FF421C03281E05212F00642064082120110727000000000000000009210104, dni: 25D8, endpoint: 01, cluster: 0000, size: 40, attrId: FF01, encoding: 42, command: 0A, value: 1C03281E05212F00642064082120110727000000000000000009210104
        // parseMap:[raw:25D80100004001FF421C03281E05212F00642064082120110727000000000000000009210104, dni:25D8, endpoint:01, cluster:0000, size:40, attrId:FF01, encoding:41, command:0A, value:[raw:[deviceTemperature:1E, RSSI_dB:002F, curtainPosition:64, unknown3:1120, unknown2:0000000000000000, unknown4:0401], deviceTemperature:30, RSSI_dB:47, curtainPosition:100, unknown3:4384, unknown2:0, unknown4:1025], clusterInt:0, attrInt:65281]
    } else if(msgMap["cluster"] == "000D" && msgMap["attrId"] == "0055") {
        logging("cluster 000D", 1)
		if(msgMap["size"] == "16" || msgMap["size"] == "1C" || msgMap["size"] == "10") {
            // This is sent just after sending a command to open/close and just after the curtain is done moving
			Long theValue = Long.parseLong(msgMap["value"], 16)
			BigDecimal floatValue = Float.intBitsToFloat(theValue.intValue());
			logging("GOT POSITION DATA: long => ${theValue}, BigDecimal => ${floatValue}", 1)
			curtainPosition = floatValue.intValue()
            if(getDeviceDataByName('model') != "lumi.curtain" && msgMap["command"] == "0A" && curtainPosition == 0) {
                logging("Sending a request for the actual position...", 1)
                cmd += zigbee.readAttribute(CLUSTER_WINDOW_POSITION, 0x0055)
            } else {
                logging("SETTING POSITION: long => ${theValue}, BigDecimal => ${floatValue}", 1)
                positionEvent(curtainPosition)
            }
		} else if(msgMap["size"] == "28" && msgMap["value"] == "00000000") {
			logging("Requesting Position", 1)
			cmd += zigbee.readAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE)
		}
	} else if(msgMap["cluster"] == "0001" && msgMap["attrId"] == "0021") {
        if(getDeviceDataByName('model') != "lumi.curtain") {
            def bat = msgMap["value"]
            Long value = Long.parseLong(bat, 16)/2
            logging("Battery: ${value}%, ${bat}", 1)
            sendEvent(name:"battery", value: value)
        }

	} else {
		log.warn "Unhandled Event - description:${description} | msgMap:${msgMap}"
	}
    
    #!include:getGenericZigbeeParseFooter(loglevel=0)
}

void positionEvent(Integer curtainPosition) {
	String windowShadeStatus = ""
	if(curtainPosition <= 2) curtainPosition = 0
    if(curtainPosition >= 98) curtainPosition = 100
    if(curtainPosition == 100) {
        logging("Fully Open", 1)
        windowShadeStatus = "open"
    } else if(curtainPosition > 0) {
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

void updated() {
    logging("updated()", 10)
    try {
        // Also run initialize(), if it exists...
        initialize()
    } catch (MissingMethodException e) {
        // ignore
    }
}

/*
    --------- WRITE ATTRIBUTE METHODS ---------
*/
ArrayList<String> open() {
    logging("open()", 1)
	return setPosition(100)    
}

ArrayList<String> on() {
    logging("on()", 1)
	return open()
}

ArrayList<String> close() {
    logging("close()", 1)
	return setPosition(0)    
}

ArrayList<String> off() {
    logging("off()", 1)
	return close()
}

ArrayList<String> reverseCurtain() {
    logging("reverseCurtain()", 1)
	ArrayList<String> cmd = []
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF28, 0x10, 0x01, [mfgCode: "0x115F"])
    logging("cmd=${cmd}", 1)
    return cmd
}

ArrayList<String> manualOpenEnable() {
    logging("manualOpenEnable()", 1)
    ArrayList<String> cmd = []
    if(getDeviceDataByName('model') == "lumi.curtain") {
        cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700080000040012, [mfgCode: "0x115F"])
    } else {
        cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF29, 0x10, 0x00, [mfgCode: "0x115F"])
    }
    logging("manualOpenEnable cmd=${cmd}", 0)
    return cmd
}

ArrayList<String> manualOpenDisable() {
    logging("manualOpenDisable()", 1)
    ArrayList<String> cmd = []
    if(getDeviceDataByName('model') == "lumi.curtain") {
        cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700080000040112, [mfgCode: "0x115F"])
    } else {
        cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF29, 0x10, 0x01, [mfgCode: "0x115F"])
    }
    logging("manualOpenDisable cmd=${cmd}", 0)
    return cmd
}

ArrayList<String> curtainOriginalDirection() {
    logging("curtainOriginalDirection()", 1)
    ArrayList<String> cmd = []
    if(getDeviceDataByName('model') == "lumi.curtain") {
        cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700020000040012, [mfgCode: "0x115F"])
    } else {
        cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF28, 0x10, 0x00, [mfgCode: "0x115F"])
    }
    logging("curtainOriginalDirection cmd=${cmd}", 0)
    return cmd
}

ArrayList<String> curtainReverseDirection() {
    logging("curtainReverseDirection()", 1)
    ArrayList<String> cmd = []
    if(getDeviceDataByName('model') == "lumi.curtain") {
        cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700020001040012, [mfgCode: "0x115F"])
    } else {
        cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF28, 0x10, 0x01, [mfgCode: "0x115F"])
    }
    logging("curtainReverseDirection cmd=${cmd}", 0)
    return cmd
}

ArrayList<String> trackDiscoveryMode() {
    logging("trackDiscoveryMode()", 1)
    ArrayList<String> cmd = []
    if(getDeviceDataByName('model') == "lumi.curtain") {
        cmd += zigbeeWriteLongAttribute(CLUSTER_BASIC, 0x0401, 0x42, 0x0700010000040012, [mfgCode: "0x115F"])
    } else {
        cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF27, 0x10, 0x00, [mfgCode: "0x115F"])
    }
    logging("trackDiscoveryMode cmd=${cmd}", 0)
    return cmd
}

ArrayList<String> stop() {
    logging("stop()", 1)
    ArrayList<String> cmd = []
	cmd += zigbee.command(CLUSTER_WINDOW_COVERING, COMMAND_PAUSE)
    logging("stop cmd=${cmd}", 0)
    return cmd
}

ArrayList<String> enableAutoClose() {
    logging("enableAutoClose()", 1)
    ArrayList<String> cmd = []
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF29, 0x10, 0x00, [mfgCode: "0x115F"])
    logging("enableAutoClose cmd=${cmd}", 0)
    return cmd
}

ArrayList<String> disableAutoClose() {
    logging("disableAutoClose()", 1)
    ArrayList<String> cmd = []
	cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF29, 0x10, 0x01, [mfgCode: "0x115F"])
    logging("disableAutoClose cmd=${cmd}", 0)
    return cmd
}

void setPosition(position) {
    if(position == null) {position = 0}
    if(position <= 2) position = 0
    if(position >= 98) position = 100
    ArrayList<String> cmd = []
    position = position as Integer
    logging("setPosition(position: ${position})", 1)
    Integer currentPosition = device.currentValue("position")
    if(position > currentPosition) {
        sendEvent(name: "windowShade", value: "opening")
    } else if(position < currentPosition) {
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
    sendZigbeeCommands(cmd)
    //return cmd
}

ArrayList<String> setLevel(level) {
    logging("setLevel(level: ${level})", 1)
    return setPosition(level)
}

ArrayList<String> setLevel(level, duration) {
    logging("setLevel(level: ${level})", 1)
    return setPosition(level)
}


/*
    --------- READ ATTRIBUTE METHODS ---------
*/
ArrayList<String> getPosition() {
    logging("getPosition()", 1)
	ArrayList<String> cmd = []
	cmd += zigbee.readAttribute(CLUSTER_WINDOW_POSITION, POSITION_ATTR_VALUE)
    logging("cmd: $cmd", 1)
    return cmd
}

ArrayList<String> getBattery() {
    logging("getBattery()", 100)
	ArrayList<String> cmd = []
    cmd += zigbee.readAttribute(CLUSTER_POWER, POWER_ATTR_BATTERY_PERCENTAGE_REMAINING)
    cmd += zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE)
	logging("cmd: $cmd", 1)
    return cmd 
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

#!include:getHelperFunctions('zigbee-generic')

// Not using the CSS styling features in this driver, so driver-metadata can be omitted
//#include:getHelperFunctions('driver-metadata')

#!include:getHelperFunctions('styling')

#!include:getHelperFunctions('driver-default')
