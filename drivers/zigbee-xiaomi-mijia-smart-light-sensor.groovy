#!include:getHeaderLicense()

#!include:getDefaultImports()
import hubitat.helper.HexUtils

metadata {
	definition (name: "Zigbee - Xiaomi Mijia Smart Light Sensor (Zigbee 3.0)", namespace: "markusl", author: "Markus Liljergren", vid: "generic-shade") {
        #!include:getDefaultMetadataCapabilitiesForZigbeeDevices()
        
        // Device Specific Capabilities
        //capability "Configuration"
        capability "Battery"
        capability "IlluminanceMeasurement"
        
        #!include:getDefaultMetadataAttributes()
        #!include:getDefaultZigbeeMetadataAttributes()
        #!include:getZigbeeBatteryMetadataAttributes()

        #!include:getZigbeeBatteryCommands()

        // Uncomment these Commands for TESTING, not needed normally:
        //command "getBattery"    // comment before release!
        //command "installed"     // just used for testing that Installed runs properly, comment before release!
        //command "sendAttribute", [[name:"Attribute*", type: "STRING", description: "Zigbee Attribute"]]
        //command "parse", [[name:"Description*", type: "STRING", description: "description"]]
        //command "configureAdditional"

        // Xiaomi Mijia Smart Light Sensor (GZCGQ01LM)
        fingerprint deviceJoinName: "Xiaomi Mijia Smart Light Sensor (GZCGQ01LM)", model: "lumi.sen_ill.mgl01", profileId: "0104", inClusters: "0000,0400,0003,0001", outClusters: "0003", manufacturer: "LUMI", endpointId: "01", deviceId: "0104"	
    }

    preferences {
        #!include:getDefaultMetadataPreferences(includeCSS=True, includeRunReset=False)
        #!include:getDefaultMetadataPreferencesForZigbeeDevices()
        #!include:getMetadataPreferencesForZigbeeDevicesWithBattery()
        input(name: "secondsMinLux", type: "number", title: addTitleDiv("Minimum Update Time"), description: addDescriptionDiv("Set the minimum number of seconds between Lux updates (5 to 3600, default: 10)"), defaultValue: "10", range: "5..3600")
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
    //cmd += zigbee.readAttribute(0x001, 0)
    
    // Specific to the Xiaomi Light Sensor
    //cmd += zigbee.readAttribute(0xFCC0, 0x0007, [mfgCode: "0x126E"])

    // This mfg-specific attribute is written to with an octet String (0x41) - this is NOT the way to send it:
    //cmd += zigbee.writeAttribute(0xFCC0, 0x0008, 0x41, "1035b63376ed5b8df8f8b4f5b2550b7c4a", [mfgCode: "0x126E"])

    logging("refresh cmd: $cmd", 1)
    return cmd
}

// Called from installed()
void installedAdditional() {
    logging("installedAdditional()", 100)
    refresh()
    resetBatteryReplacedDate()
}

ArrayList<String> parse(String description) {
    //log.debug "in parse"
    #!include:getGenericZigbeeParseHeader(loglevel=0)
    //logging("msgMap: ${msgMap}", 1)

    sendlastCheckinEvent(minimumMinutesToRepeat=55)

    // description:catchall: 0000 0006 00 00 0040 00 930E 00 00 0000 00 00 D6FDFF040101190000 | 
    // msgMap:[raw:catchall: 0000 0006 00 00 0040 00 930E 00 00 0000 00 00 D6FDFF040101190000, profileId:0000, 
    // clusterId:0006, clusterInt:6, sourceEndpoint:00, destinationEndpoint:00, options:0040, messageType:00, 
    // dni:930E, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:00, 
    // direction:00, data:[D6, FD, FF, 04, 01, 01, 19, 00, 00]]
    
    // Together: cluster: 0000 and attrId: 0005
    // description:catchall: 0104 0003 01 FF 0040 00 930E 01 00 0000 01 00  | 
    // msgMap:[raw:catchall: 0104 0003 01 FF 0040 00 930E 01 00 0000 01 00 , profileId:0104, clusterId:0003, 
    // clusterInt:3, sourceEndpoint:01, destinationEndpoint:FF, options:0040, messageType:00, dni:930E, 
    // isClusterSpecific:true, isManufacturerSpecific:false, manufacturerId:0000, command:01, direction:00, data:[]]

    // description:catchall: 0000 0013 00 00 0040 00 7361 00 00 0000 00 00 D36173BC29773CDF8CCF0484 
    // | msgMap:[raw:catchall: 0000 0013 00 00 0040 00 7361 00 00 0000 00 00 D36173BC29773CDF8CCF0484, 
    // profileId:0000, clusterId:0013, clusterInt:19, sourceEndpoint:00, destinationEndpoint:00, options:0040, 
    // messageType:00, dni:7361, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, 
    // command:00, direction:00, data:[D3, 61, 73, BC, 29, 77, 3C, DF, 8C, CF, 04, 84]]

    

    if(msgMap["clusterId"] == "8021") {
        logging("CONFIGURE CONFIRMATION - description: ${description} | parseMap:${msgMap}", 0)
        // catchall: 0000 8021 00 00 0040 00 5DF0 00 00 0000 00 00 9000
        // catchall: 0000 8021 00 00 0040 00 5DF0 00 00 0000 00 00 9100
        if(msgMap["data"] != []) {
            logging("Received BIND Confirmation with sequence number 0x${msgMap["data"][0]} (a total minimum of FOUR unique numbers expected, same number may repeat).", 100)
        }
    } else if(msgMap["clusterId"] == "8034") {
        logging("CLUSTER LEAVE REQUEST - description: ${description} | parseMap:${msgMap}", 0)
    } else if(msgMap["clusterId"] == "0013") {
        logging("Pairing event - description: ${description} | parseMap:${msgMap}", 1)
        sendZigbeeCommands(configureAdditional())
        refresh()
        // Getting this during install:
        // catchall: 0000 0013 00 00 0040 00 CE89 00 00 0000 00 00 D389CE0932773CDF8CCF0484
        // msgMap:[raw:catchall: 0000 0013 00 00 0040 00 CE89 00 00 0000 00 00 D389CE0932773CDF8CCF0484, profileId:0000, clusterId:0013, clusterInt:19, sourceEndpoint:00, destinationEndpoint:00, options:0040, messageType:00, dni:CE89, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:00, direction:00, data:[D3, 89, CE, 09, 32, 77, 3C, DF, 8C, CF, 04, 84]]
    } else if((msgMap["clusterId"] == "0000" || msgMap["clusterId"] == "0001" || msgMap["clusterId"] == "0003" || msgMap["clusterId"] == "0400") && msgMap["command"] == "07" && msgMap["data"] != [] && msgMap["data"][0] == "00") {
        logging("CONFIGURE CONFIRMATION - description:${description} | parseMap:${msgMap}", 1)
        if(msgMap["clusterId"] == "0400") {
            logging("Device confirmed LUX Report configuration ACCEPTED by the device", 100)
        } else if(msgMap["clusterId"] == "0000") {
            logging("Device confirmed BASIC Report configuration ACCEPTED by the device", 100)
        } else if(msgMap["clusterId"] == "0001") {
            logging("Device confirmed BATTERY Report configuration ACCEPTED by the device", 100)
        } else if(msgMap["clusterId"] == "0003") {
            logging("Device confirmed IDENTIFY Report configuration ACCEPTED by the device", 100)
        }
        // Configure Confirmation event Description cluster 0001:
        // catchall: 0104 0001 01 01 0040 00 5DF0 00 00 0000 07 01 00
        // msgMap:[raw:catchall: 0104 0001 01 01 0040 00 5DF0 00 00 0000 07 01 00, profileId:0104, clusterId:0001, clusterInt:1, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:5DF0, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:07, direction:01, data:[00]]

        // Configure Confirmation event Description cluster 0400:
        // catchall: 0104 0400 01 01 0040 00 5DF0 00 00 0000 07 01 00
        // msgMap:[raw:catchall: 0104 0400 01 01 0040 00 5DF0 00 00 0000 07 01 00, profileId:0104, clusterId:0400, clusterInt:1024, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:5DF0, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:07, direction:01, data:[00]]
    } else if(msgMap["cluster"] == "0000" && msgMap["attrId"] == "0005") {
        logging("Reset button pressed - description:${description} | parseMap:${msgMap}", 1)
        // The value from this command is the device model string
        setCleanModelName(newModelToSet=msgMap["value"])
        sendZigbeeCommands(configureAdditional())
        refresh()
        //sendZigbeeCommands(zigbee.readAttribute(CLUSTER_POWER, 0x0020))
        // Reset button event Description:
        // read attr - raw: 5DF00100002C050042126C756D692E73656E5F696C6C2E6D676C3031, dni: 5DF0, endpoint: 01, cluster: 0000, size: 2C, attrId: 0005, encoding: 42, command: 0A, value: 126C756D692E73656E5F696C6C2E6D676C3031
    } else if(msgMap["clusterId"] == "0006") {
        logging("Match Descriptor Request - description:${description} | parseMap:${msgMap}", 1)
        // This is usually the 0x0019 OTA Upgrade Request, safe to ignore

        // Data == data:[D5, FD, FF, 04, 01, 01, 19, 00, 00] == OTA Upgrade Request

        // catchall: 0000 0006 00 00 0040 00 F0AE 00 00 0000 00 00 D5FDFF040101190000
        // msgMap:[raw:catchall: 0000 0006 00 00 0040 00 F0AE 00 00 0000 00 00 D5FDFF040101190000, profileId:0000, clusterId:0006, clusterInt:6, sourceEndpoint:00, destinationEndpoint:00, options:0040, messageType:00, dni:F0AE, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:00, direction:00, data:[D5, FD, FF, 04, 01, 01, 19, 00, 00]]
    } else if(msgMap["clusterId"] == "0003" && msgMap["command"] == "01") {
        logging("IDENTIFY QUERY - description:${description} | parseMap:${msgMap}", 1)
        // This is responded to with a Manufacturer Specific command
        // Command: Default Response
        sendZigbeeCommands(["he raw ${device.deviceNetworkId} 1 1 0xFCC0 {04 6E 12 00 0B 03 83}"])  // 12 00 0B = the 00 is replaced with the sequence number
        // Identify Query event Description:
        // catchall: 0104 0003 01 FF 0040 00 5DF0 01 00 0000 01 00
    } else if(msgMap["cluster"] == "0400" && msgMap["attrId"] == "0000") {
        Integer rawValue = Integer.parseInt(msgMap['value'], 16)
        Integer variance = 190
        
        BigDecimal lux = rawValue > 0 ? Math.pow(10, rawValue / 10000.0) - 1.0 : 0
        BigDecimal oldLux = device.currentValue('illuminance') == null ? null : device.currentValue('illuminance')
        Integer oldRaw = oldLux == null ? null : oldLux == 0 ? 0 : Math.log10(oldLux + 1) * 10000
        lux = lux.setScale(1, BigDecimal.ROUND_HALF_UP)
        if(oldLux != null) oldLux = oldLux.setScale(1, BigDecimal.ROUND_HALF_UP)
        BigDecimal luxChange = null
        if(oldRaw == null) {
            logging("Lux: $lux (raw: $rawValue, oldRaw: $oldRawold lux: $oldLux)", 1)
        } else {
            luxChange = oldLux - lux
            luxChange = luxChange.setScale(1, BigDecimal.ROUND_HALF_UP)
            logging("Lux: $lux (raw: $rawValue, oldRaw: $oldRaw, diff: ${oldLux - lux}, lower: ${oldRaw - variance}, upper: ${oldRaw + variance}, old lux: $oldLux)", 1)
        }
        
        if(oldLux == null || rawValue < oldRaw - variance || rawValue > oldRaw + variance) {
            logging("Sending lux event (lux: $lux)", 100)
            sendEvent(name:"illuminance", value: lux, unit: "lux", isStateChange: true)
        } else {
            logging("SKIPPING lux event since change wasn't large enough (lux: $lux, change: $luxChange)", 100)
        }
        // Lux event Description:
        // read attr - raw: 5DF00104000A0000219F56, dni: 5DF0, endpoint: 01, cluster: 0400, size: 0A, attrId: 0000, encoding: 21, command: 0A, value: 9F56
    } else if(msgMap["cluster"] == "0000" && (msgMap["attrId"] == "FF01" || msgMap["attrId"] == "FF02")) {
        logging("KNOWN event (Xiaomi/Aqara specific data structure with battery data) - description:${description} | parseMap:${msgMap}", 1)
        // Xiaomi/Aqara specific data structure, contains battery info
    } else if(msgMap["cluster"] == "0001" && msgMap["attrId"] == "0020") {
        logging("Battery voltage received - description:${description} | parseMap:${msgMap}", 1)
        parseAndSendBatteryStatus(Integer.parseInt(msgMap['value'], 16) / 10.0)
        // Battery event Description:
        // read attr - raw: 5DF00100010820002020, dni: 5DF0, endpoint: 01, cluster: 0001, size: 08, attrId: 0020, encoding: 20, command: 0A, value: 20
    } else {
		log.warn "Unhandled Event PLEASE REPORT TO DEV - description:${description} | msgMap:${msgMap}"
	}
    
    #!include:getGenericZigbeeParseFooter(loglevel=0)
}

void updated() {
    logging("updated()", 10)
    configurePresence()
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
ArrayList<String> configureAdditional() {
    logging("configureAdditional()", 1)
    // List configureReporting(Integer clusterId, Integer attributeId, Integer dataType, Integer minReportTime, 
    //        Integer maxReportTime, Integer reportableChange = null, Map additionalParams=[:], 
    //        int delay = STANDARD_DELAY_INT)
    Integer msDelay = 50
    ArrayList<String> cmd = [
		"zdo bind ${device.deviceNetworkId} 0x01 0x01 0x0000 {${device.zigbeeId}} {}", "delay $msDelay",
        "zdo bind ${device.deviceNetworkId} 0x01 0x01 0x0001 {${device.zigbeeId}} {}", "delay $msDelay",
		"zdo bind ${device.deviceNetworkId} 0x01 0x01 0x0003 {${device.zigbeeId}} {}", "delay $msDelay",
		"zdo bind ${device.deviceNetworkId} 0x01 0x01 0x0400 {${device.zigbeeId}} {}", "delay $msDelay",
		"zdo send ${device.deviceNetworkId} 0x01 0x01", "delay $msDelay"
    ]
    // CLUSTER: ILLUMINANCE
    cmd += zigbee.configureReporting(0x0400, 0x0000, 0x21, (secondsMinLux == null ? 10 : secondsMinLux).intValue(), 3600, 300, [:], msDelay)
    // CLUSTER: POWER, 60 min report interval (original default 5), 3600 max report interval (original default 3600), Voltage measured: 0.1V
    cmd += zigbee.configureReporting(0x0001, 0x0020, 0x20, 3600, 3600, null, [:], msDelay)
    // CLUSTER: BASIC (Response is unreportable attribute, so no use setting this)
	//cmd += zigbee.configureReporting(0x0000, 0x0005, 0xff, 30, 3600, null, [:], msDelay)
    // CLUSTER: IDENTIFY (Response is unreportable attribute, so no use setting this)
    //cmd += zigbee.configureReporting(0x0003, 0x0000, 0xff, 0, 0, null, [:], msDelay)
    
    // Request the current lux value
	cmd += zigbeeReadAttribute(0x0400, 0x0000)
    // Request the current Battery level
    cmd += zigbeeReadAttribute(0x0001, 0x0020)

	//cmd += zigbee.writeAttribute(CLUSTER_BASIC, 0xFF29, 0x10, 0x01, [mfgCode: "0x126E"])
    logging("configure cmd=${cmd}", 1)
    return cmd
}

/*
    --------- READ ATTRIBUTE METHODS ---------
*/

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
