/**
 * TASMOTA METHODS (helpers-tasmota)
 *
 * Helper functions included in all Tasmota drivers
 */

// Calls installed() -> installedPreConfigure()
void installedPreConfigure() {
    // This is run FIRST in installed()
    if(isDriver()) {
        // This is only run ONCE after install
        
        logging("Inside installedPreConfigure()", 1)
        logging("Password: ${decrypt(getDataValue('password'))}", 1)
        String pass = decrypt(getDataValue('password'))
        if(pass != null && pass != "" && pass != "[installed]") {
            device.updateSetting("password", [value: pass, type: "password"])
        }
        device.updateSetting("deviceConfig", [type: "enum", value:getDataValue('deviceConfig')])
    }
}

// Call order: installed() -> configure() -> updated() 
void updated() {
    logging("updated()", 10)
    if(isDriver()) {
        logging("before updateNeededSettings()", 10)
        updateNeededSettings()
        logging("after updateNeededSettings()", 10)
        //sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "lan", hubHardwareId: device.hub.hardwareID])
        //sendEvent(name:"needUpdate", value: device.currentValue("needUpdate"), displayed:false, isStateChange: false)
    }
    try {
        // Also run initialize(), if it exists...
        initialize()
        updatedAdditional()
    } catch (MissingMethodException e) {
        // ignore
    }
}

void refreshChildren() {
    logging("refreshChildren()", 1)
    getAction(getCommandString("Status", "0"), callback="parseConfigureChildDevices")
}

void refreshChildrenAgain() {
    logging("refreshChildrenAgain()", 1)
    refreshChildren()
}

// Call order: installed() -> configure() -> updated() -> initialize() -> refresh()
void refresh() {
	logging("refresh()", 100)
    def metaConfig = null
    if(isDriver()) {
        // Clear all old state variables, but ONLY in a driver!
        state.clear()

        // Retrieve full status from Tasmota
        getAction(getCommandString("Status", "0"), callback="parseConfigureChildDevices")
        getDriverVersion()

        updateDataValue('namespace', getDeviceInfoByName('namespace'))

        //logging("this.binding.variables = ${this.binding.variables}", 1)
        //logging("settings = ${settings}", 1)
        //logging("getDefinitionData() = ${getDefinitionData()}", 1)
        //logging("getPreferences() = ${getPreferences()}", 1)
        //logging("getSupportedCommands() = ${device.getSupportedCommands()}", 1)
        //logging("Seeing these commands: ${device.getSupportedCommands()}", 1)
        
        /*metaConfig = setCommandsToHide(["on", "hiAgain2", "on"])
        metaConfig = setStateVariablesToHide(["uptime"], metaConfig=metaConfig)
        metaConfig = setCurrentStatesToHide(["needUpdate"], metaConfig=metaConfig)
        metaConfig = setDatasToHide(["namespace"], metaConfig=metaConfig)
        metaConfig = setPreferencesToHide(["port"], metaConfig=metaConfig)*/

        // This should be the first place we access metaConfig here, so clear and reset...
        metaConfig = clearThingsToHide()
        metaConfig = setCommandsToHide([], metaConfig=metaConfig)
        metaConfig = setStateVariablesToHide(['settings', 'colorMode', 'red', 'green', 'blue', 
            'mired', 'level', 'saturation', 'mode', 'hue'], metaConfig=metaConfig)
        
        metaConfig = setCurrentStatesToHide(['needUpdate'], metaConfig=metaConfig)
        //metaConfig = setDatasToHide(['preferences', 'namespace', 'appReturn', 'metaConfig'], metaConfig=metaConfig)
        metaConfig = setDatasToHide(['namespace', 'appReturn', 'password'], metaConfig=metaConfig)
        metaConfig = setPreferencesToHide([], metaConfig=metaConfig)
    }
    try {
        // In case we have some more to run specific to this driver
        refreshAdditional(metaConfig)
    } catch (MissingMethodException e1) {
        // ignore
        try {
            // In case we have some more to run specific to this driver
            refreshAdditional()
        } catch (MissingMethodException e2) {
            // ignore
        }
    }
}

void reboot() {
	logging("reboot()", 10)
    getAction(getCommandString("Restart", "1"))
}

/*
    // Stress-testing runInstallCommands() 
    installCommands = []
    installCommands.add(["rule1", 'ON Var1#Value DO Var4 0; ENDON'])
    installCommands.add(["rule2", 'ON Var2#Value DO Var4 0; ENDON'])
    installCommands.add(["rule3", 'ON Var3#Value DO Var4 0; ENDON'])
    installCommands.add(["var1", "0"])
    installCommands.add(["var2", "0"])
    installCommands.add(["var3", "0"])
    (1..8).each {
        installCommands.add(["rule1", "+ ON Var1#Value DO Var4 $it; ENDON"])
        installCommands.add(["rule2", "+ ON Var2#Value DO Var4 $it; ENDON"])
        installCommands.add(["rule3", "+ ON Var3#Value DO Var4 $it; ENDON"])
        installCommands.add(["add1", "1"])
        installCommands.add(["add2", "1"])
        installCommands.add(["add3", "1"])
    }
    installCommands.add(["rule1", '0'])
    installCommands.add(["rule2", '0'])
    installCommands.add(["rule3", '0'])
    logging("refreshAdditional installCommands=$installCommands", 1)
    runInstallCommands(installCommands)
*/
void runInstallCommands(installCommands) {
    // Runs install commands as defined in helpers-device-configurations
    // Called from updateNeededSettings() in parent drivers
    logging("runInstallCommands(installCommands=$installCommands)", 1)
    List backlogs = []
    List rule1 = []
    List rule2 = []
    List rule3 = []
    installCommands.each {cmd->
        if(cmd[0].toLowerCase() == "rule1") {
            rule1.add([command: cmd[0], value:cmd[1]])
        } else if(cmd[0].toLowerCase() == "rule2") {
            rule2.add([command: cmd[0], value:cmd[1]])
        } else if(cmd[0].toLowerCase() == "rule3") {
            rule3.add([command: cmd[0], value:cmd[1]])
        } else {
            backlogs.add([command: cmd[0], value:cmd[1]])
        }
    }

    // Backlog inter-command delay in milliseconds
    //getAction(getCommandString("SetOption34", "20"))
    pauseExecution(100)
    // Maximum 30 commands per backlog call
    while(backlogs.size() > 0) {
        getAction(getMultiCommandString(backlogs.take(10)))
        backlogs = backlogs.drop(10)
        // If we run this too fast Tasmota can't keep up, 1000ms is enough when 20ms between commands...
        if(backlogs.size() > 0) pauseExecution(1000)
        // REALLY don't use pauseExecution often... NOT good for performance...
    }

    [rule1, rule2, rule3].each {
        //logging("rule: $it", 1)
        it.each {rule->
            // Rules can't run in backlog!
            getAction(getCommandString(rule["command"], rule["value"]))
            //logging("cmd=${rule["command"]}, value=${rule["value"]}", 1)
            pauseExecution(100)
            // REALLY don't use pauseExecution often... NOT good for performance...
        }
    }
    //getAction(getCommandString("SetOption34", "200"))
}

void updatePresence(String presence) {
    // presence - ENUM ["present", "not present"]
    logging("updatePresence(presence=$presence)", 1)
    Integer timeout = getTelePeriodValue()
    timeout += (timeout * 1.1 > 120 ? Math.round(timeout * 1.1) : 120) + 60
    String descriptionText = "No update received from the Tasmota device for ${timeout} seconds..."
    if(presence == "present") {    
        descriptionText = "Device is available"
        //log.warn "Setting as present with timeout: $timeout"
        runIn(timeout, "updatePresence", [data: "not present"])
    } else {
        log.warn "Presence time-out reached, setting device as 'not present'!"
    }
    sendEvent(name: "presence", value: presence, isStateChange: false, descriptionText: descriptionText)
}

Map parseDescriptionAsMap(description) {
    // Used by parse(description) to get descMap
	description.split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
        
        if (nameAndValue.length == 2) { 
            map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
        } else {
            map += [(nameAndValue[0].trim()):""]
        }
	}
}

private getAction(uri, callback="parse") { 
    logging("Using getAction for '${uri}'...", 0)
    httpGetAction(uri, callback=callback)
}

def parse(asyncResponse, data) {
    // Parse called by default when using asyncHTTP
    if(asyncResponse != null) {
        try{
            logging("parse(asyncResponse.getJson() = \"${asyncResponse.getJson()}\")", 100)
            parseResult(asyncResponse.getJson())
        } catch(MissingMethodException e1) {
            log.error e1
        } catch(e1) {
            try{
                logging("parse(asyncResponse.data = \"${asyncResponse.data}\", data = \"${data}\") e1=$e1", 1)
            } catch(e2) {
                logging("parse(asyncResponse.data = null, data = \"${data}\") Is the device online? e2=$e2", 1)
            }
        }
    } else {
        logging("parse(asyncResponse.data = null, data = \"${data}\")", 1)
    }
}


/*
    Methods related to configureChildDevices()

    configureChildDevices() detects which child devices to create/update and does the creation/updating
*/

void parseConfigureChildDevices(asyncResponse, data) {
    if(asyncResponse != null) {
        try{
            logging("parse(asyncResponse.getJson() 2= \"${asyncResponse.getJson()}\", data = \"${data}\")", 1)
            configureChildDevices(asyncResponse, data)
        } catch(MissingMethodException e1) {
            log.error e1
        } catch(e1) {
            try{
                logging("parse(asyncResponse.data = \"${asyncResponse.data}\", data = \"${data}\") e1=$e1", 1)
            } catch(e2) {
                logging("parse(asyncResponse.data = null, data = \"${data}\") Is the device online? e2=$e2", 1)
            }
        }
    } else {
        logging("parse(asyncResponse.data = null, data = \"${data}\")", 1)
    }
}

boolean containsKeyInSubMap(aMap, key) {
    boolean hasKey = false
    aMap.find {
        try{
            hasKey = it.value.containsKey(key)
        } catch(e) {

        }
        hasKey == true
    }
    return hasKey
}

Integer numOfKeyInSubMap(aMap, String key) {
    Integer numKeys = 0
    aMap.each {
        try{
            if(it.value.containsKey(key)) numKeys += 1
        } catch(e) {
            // Do nothing
        }
    }
    return numKeys
}

Integer numOfKeysIsMap(aMap) {
    Integer numKeys = 0
    aMap.each {
        if(it.value instanceof java.util.Map) numKeys += 1
    }
    return numKeys
}

TreeMap getKeysWithMapAndId(aMap) {
    def foundMaps = [:] as TreeMap
    aMap.each {
        if(it.value instanceof java.util.Map) {
            foundMaps[it.key] = it.value
        }
    }
    return foundMaps
}

void configureChildDevices(asyncResponse, data) {
    // This detects which child devices to create/update and does the creation/updating
    def statusMap = asyncResponse.getJson()
    logging("configureChildDevices() statusMap=$statusMap", 1)
    // Use statusMap to determine which Child Devices we should create

    // The built-in Generic Components are:
    //
    // Acceleration Sensor  - ID: 189
    // Button Controller    - ID: 1029
    // Central Scene Dimmer - ID: 912
    // Central Scene Switch - ID: 913
    // Contact Sensor       - ID: 192
    // Contact/Switch       - ID: 199
    // CT                   - ID: 198
    // Dimmer               - ID: 187
    // Metering Switch      - ID: 188
    // Motion Sensor        - ID: 197
    // RGB                  - ID: 195
    // RGBW                 - ID: 191
    // Smoke Detector       - ID: 196
    // Switch               - ID: 190
    // Temperature Sensor   - ID: 200
    // Water Sensor         - ID: 194
    

    // {"StatusSTS":{"Time":"2020-01-26T01:13:27","Uptime":"15T02:59:27","UptimeSec":1306767,
    // "Heap":26,"SleepMode":"Dynamic","Sleep":50,"LoadAvg":19,"MqttCount":0,"POWER1":"OFF",
    // "POWER2":"OFF","POWER3":"OFF","POWER4":"OFF","Wifi":{"AP":1,"SSId":"network",
    // "BSSId":"4A:11:11:12:CF:11","Channel":1,"RSSI":62,"LinkCount":37,"Downtime":"0T00:05:48"}}}

    // With a dimmer:
    // {"StatusSTS":{"Time":"2020-01-26T11:58:10","Uptime":"0T00:01:20","UptimeSec":80,"Heap":26,
    // "SleepMode":"Dynamic","Sleep":50,"LoadAvg":19,"MqttCount":0,"POWER":"OFF","Dimmer":0,
    // "Fade":"OFF","Speed":1,"LedTable":"ON","Wifi":{"AP":1,"SSId":"network",
    // "BSSId":"4A:11:11:12:D9:11","Channel":1,"RSSI":100,"LinkCount":1,"Downtime":"0T00:00:06"}}}

    // With an RGB+CW+WW light:
    // {"StatusSTS":{"Time":"2020-01-26T12:07:57","Uptime":"0T00:06:58","UptimeSec":418,"Heap":27,
    // "SleepMode":"Dynamic","Sleep":10,"LoadAvg":99,"MqttCount":0,"POWER":"ON","Dimmer":100,
    // "Color":"000000FF62","HSBColor":"0,0,0","Channel":[0,0,0,100,38],"CT":250,"Scheme":0,
    // "Fade":"ON","Speed":10,"LedTable":"ON","Wifi":{"AP":1,"SSId":"network",
    // "BSSId":"4A:11:11:12:D9:11","Channel":1,"RSSI":96,"LinkCount":1,"Downtime":"0T00:00:06"}}}
    

    // With an RGB+W light:
    // {"StatusSTS":{"Time":"2020-01-26T12:11:56","Uptime":"0T00:00:26","UptimeSec":26,"Heap":27,
    // "SleepMode":"Dynamic","Sleep":10,"LoadAvg":99,"MqttCount":0,"POWER":"ON","Dimmer":100,
    // "Color":"000000FF","HSBColor":"0,0,0","Channel":[0,0,0,100],"Scheme":0,"Fade":"ON",
    // "Speed":10,"LedTable":"ON","Wifi":{"AP":1,"SSId":"network","BSSId":"4A:11:11:12:D9:11",
    // "Channel":1,"RSSI":90,"LinkCount":1,"Downtime":"0T00:00:06"}}}

    // With an RGB light:
    // {"StatusSTS":{"Time":"2020-01-26T12:14:15","Uptime":"0T00:00:19","UptimeSec":19,"Heap":27,
    // "SleepMode":"Dynamic","Sleep":10,"LoadAvg":99,"MqttCount":0,"POWER":"ON","Dimmer":100,
    // "Color":"FFFFFF","HSBColor":"0,0,100","Channel":[100,100,100],"Scheme":0,"Fade":"ON",
    // "Speed":10,"LedTable":"ON","Wifi":{"AP":1,"SSId":"network","BSSId":"4A:11:11:12:D9:11",
    // "Channel":1,"RSSI":98,"LinkCount":1,"Downtime":"0T00:00:06"}}}

    // With CW+WW ("CT" is available):
    // {"StatusSTS":{"Time":"2020-01-26T12:16:48","Uptime":"0T00:00:17","UptimeSec":17,"Heap":28,
    // "SleepMode":"Dynamic","Sleep":10,"LoadAvg":99,"MqttCount":0,"POWER":"ON","Dimmer":100,
    // "Color":"FF62","HSBColor":"0,0,0","Channel":[100,38],"CT":250,"Fade":"ON","Speed":10,
    // "LedTable":"ON","Wifi":{"AP":1,"SSId":"network","BSSId":"4A:11:11:12:D9:11",
    // "Channel":1,"RSSI":94,"LinkCount":1,"Downtime":"0T00:00:06"}}}

    // With CW or WW (PWM1 configured on the correct pin), just the same as a normal dimmer...
    // {"StatusSTS":{"Time":"2020-01-26T12:19:51","Uptime":"0T00:01:15","UptimeSec":75,"Heap":27,
    // "SleepMode":"Dynamic","Sleep":10,"LoadAvg":99,"MqttCount":0,"POWER":"ON","Dimmer":71,
    // "Fade":"ON","Speed":10,"LedTable":"ON","Wifi":{"AP":1,"SSId":"network",
    // "BSSId":"4A:11:11:12:D9:11","Channel":1,"RSSI":88,"LinkCount":1,"Downtime":"0T00:00:25"}}}

    // Addressable RGB light (has the attribute "Width")
    // {"StatusSNS":{"Time":"2020-01-26T12:57:30","SR04":{"Distance":8.579}}}
    // {"StatusSTS":{"Time":"2020-01-26T12:57:30","Uptime":"0T00:02:14","UptimeSec":134,"Heap":21,
    // "SleepMode":"Dynamic","Sleep":10,"LoadAvg":113,"MqttCount":0,"POWER1":"ON","POWER2":"ON",
    // "Dimmer":100,"Color":"00FF00","HSBColor":"120,100,100","Channel":[0,100,0],"Scheme":13,
    // "Width":2,"Fade":"OFF","Speed":1,"LedTable":"ON","Wifi":{"AP":1,"SSId":"network",
    // "BSSId":"4A:11:11:12:D9:11","Channel":1,"RSSI":100,"Signal":-40,"LinkCount":1,
    // "Downtime":"0T00:00:09"}}}

    // {"StatusSNS":{"Time":"2020-01-26T01:24:16","BMP280":{"Temperature":23.710,"Pressure":1017.6},
    // "PressureUnit":"hPa","TempUnit":"C"}}

    // Multiple temperature sensors:
    // {"Time":"2020-01-26T17:45:30","DS18B20-1":{"Id":"000008BD38BF","Temperature":26.1},
    // "DS18B20-2":{"Id":"000008BD9714","Temperature":25.1},"DS18B20-3":{"Id":"000008C02C3A",
    // "Temperature":25.3},"TempUnit":"C"}
    
    // For DS18B20, us ID to distinguish them? Then you can't replace them...
    // For AM2301 the GPIO used is appended.
    // {"StatusSNS":{"Time":"2020-01-26T20:54:10","DS18B20-1":{"Id":"000008BD38BF","Temperature":25.8},
    // "DS18B20-2":{"Id":"000008BD9714","Temperature":24.7},"DS18B20-3":{"Id":"000008C02C3A","Temperature":24.9},
    // "AM2301-12":{"Temperature":25.1,"Humidity":66.4},"AM2301-14":{"Temperature":null,"Humidity":null},"TempUnit":"C"}}

    // D5 = GPIO14
    // D6 = GPIO12
    // D7 = GPIO13

    // Distance Sensor
    // {"StatusSNS":{"Time":"2020-01-26T13:52:19","SR04":{"Distance":11.667}}}

    // {"NAME":"ControlRGBWWCW","GPIO":[17,0,0,0,0,40,0,0,38,39,37,41,0],"FLAG":0,"BASE":18}

    // result: [Time:2020-01-30T11:30:43, DS18B20-1:[Id:000008BD38BF, Temperature:25.3], DS18B20-2:[Id:000008BD9714, Temperature:24.3], DS18B20-3:[Id:000008C02C3A, Temperature:24.4], AM2301-12:[Temperature:24.2, Humidity:68.1], AM2301-14:[Temperature:24.0, Humidity:68.1], TempUnit:C]
    // result: [Time:2020-01-30T11:31:12, DS18B20-1:[Id:000008BD38BF, Temperature:25.3], DS18B20-2:[Id:000008BD9714, Temperature:24.3], DS18B20-3:[Id:000008C02C3A, Temperature:24.4], AM2301-12:[Temperature:24.2, Humidity:68.0], AM2301-14:[Temperature:24.0, Humidity:68.1], TempUnit:C]
    // [hasEnergy:false, numTemperature:5, numHumidity:2, numPressure:0, numDistance:0, sensorMap:[AM2301-12:[Temperature:24.2, Humidity:68.1], AM2301-14:[Temperature:24.0, Humidity:68.1], DS18B20-1:[Id:000008BD38BF, Temperature:25.3], DS18B20-2:[Id:000008BD9714, Temperature:24.3], DS18B20-3:[Id:000008C02C3A, Temperature:24.4]], numSwitch:0, isDimmer:false, isAddressable:false, isRGB:false, hasCT:false]

    // SENSOR = {"Time":"2020-01-30T19:15:08","SR04":{"Distance":73.702}}

    // Switch or Metering Switch are the two most likely ones
    def deviceInfo = [:]
    deviceInfo["hasEnergy"] = false
    deviceInfo["numTemperature"] = 0
    deviceInfo["numHumidity"] = 0
    deviceInfo["numPressure"] = 0
    deviceInfo["numDistance"] = 0
    deviceInfo["numSensorGroups"] = 0
    deviceInfo["sensorMap"] = [:]
    if(statusMap.containsKey("StatusSNS")) {
        sns = statusMap["StatusSNS"]
        deviceInfo["hasEnergy"] = sns.containsKey("ENERGY")
        deviceInfo["sensorMap"] = getKeysWithMapAndId(sns)
        // Energy is the only one that doesn't belong... Just remove it...
        deviceInfo["sensorMap"].remove("ENERGY")
        deviceInfo["numSensorGroups"] = deviceInfo["sensorMap"].size()
        deviceInfo["numTemperature"] = numOfKeyInSubMap(sns, "Temperature")
        deviceInfo["numHumidity"] = numOfKeyInSubMap(sns, "Humidity")
        deviceInfo["numPressure"] = numOfKeyInSubMap(sns, "Pressure")
        deviceInfo["numDistance"] = numOfKeyInSubMap(sns, "Distance")
    }

    deviceInfo["numSwitch"] = 0
    deviceInfo["isDimmer"] = false
    deviceInfo["isAddressable"] = false
    deviceInfo["isRGB"] = false
    deviceInfo["hasCT"] = false
    deviceInfo["hasFanControl"] = false
    if(statusMap["StatusSTS"] != null) {
        sts = statusMap["StatusSTS"]
        deviceInfo["isDimmer"] = sts.containsKey("Dimmer")
        deviceInfo["isAddressable"] = sts.containsKey("Width")
        if(sts.containsKey("Color")) deviceInfo["isRGB"] = sts["Color"].length() >= 6
        deviceInfo["hasCT"] = sts.containsKey("CT")
        deviceInfo["hasFanControl"] = sts.containsKey("FanSpeed")

        if(sts["POWER"] != null) {
            // This only exist if there is ONLY one switch/bulb
            deviceInfo["numSwitch"] = 1
        } else {
            i = 1
            while(sts["POWER$i"] != null) {
                i += 1
            }
            deviceInfo["numSwitch"] = i - 1
        }
    }
    logging("Device info found: $deviceInfo", 100)

    // Create the devices, if needed

    // Switches
    def driverName = ["Tasmota - Universal Plug/Outlet (Child)", "Generic Component Switch"]
    def namespace = "tasmota"
    if(deviceInfo["numSwitch"] > 0) {
        /*if(deviceInfo["numSwitch"] > 1 && (
            deviceInfo["isDimmer"] == true || deviceInfo["isAddressable"] == true || 
            deviceInfo["isRGB"] == true || deviceInfo["hasCT"] == true)) {
                // This is supported now ;)
                // log.warn "There's more than one switch and the device is either dimmable, addressable, RGB or has CT capability. This is not fully supported yet, please report which device and settings you're using to the developer so that a solution can be found."

        }*/
        if(deviceInfo["hasEnergy"]  == true && (deviceInfo["isAddressable"] == false && deviceInfo["isRGB"] == false && deviceInfo["hasCT"] == false)) {
            if(deviceInfo["isDimmer"]) {
                // TODO: Make a Component Dimmer with Metering
                driverName = ["Tasmota - Universal Metering Dimmer (Child)", "Generic Component Dimmer"]
            } else {
                driverName = ["Tasmota - Universal Metering Plug/Outlet (Child)", 
                              "Tasmota - Universal Metering Bulb/Light (Child)",
                              "Generic Component Metering Switch"]
            }
        } else {
            if(deviceInfo["hasEnergy"] == true) {
                log.warn "This device reports Metering Capability AND has RGB, Color Temperature or is Addressable. Metering values will be ignored... This is NOT supported and may result in errors, please report it to the developer to find a solution."
            }
            if((deviceInfo["isDimmer"] == true || deviceInfo["isAddressable"] == true || 
                deviceInfo["isRGB"] == true || deviceInfo["hasCT"] == true)) {
                if(deviceInfo["isAddressable"] == false && deviceInfo["isRGB"] == false && deviceInfo["hasCT"] == false) {
                    driverName = ["Tasmota - Universal Dimmer (Child)", "Generic Component Dimmer"]
                } else if(deviceInfo["isAddressable"] == false && deviceInfo["isRGB"] == false && deviceInfo["hasCT"] == true) {
                    driverName = ["Tasmota - Universal CT/RGB/RGB+CW+WW (Child)", "Generic Component CT"]
                } else if(deviceInfo["isRGB"] == true && deviceInfo["hasCT"] == false) {
                    driverName = ["Tasmota - Universal CT/RGB/RGB+CW+WW (Child)", "Generic Component RGB"]
                } else {
                    driverName = ["Tasmota - Universal CT/RGB/RGB+CW+WW (Child)", "Generic Component RGBW"]
                }
            }
        }
        
        
        for(i in 1..deviceInfo["numSwitch"]) {
            namespace = "tasmota"
            def childId = "POWER$i"
            def childName = getChildDeviceNameRoot(keepType=true) + " ${getMinimizedDriverName(driverName[0])} ($childId)"
            def childLabel = "${getMinimizedDriverName(device.getLabel())} ($i)"
            logging("createChildDevice: POWER$i", 1)
            createChildDevice(namespace, driverName, childId, childName, childLabel)
            
            // Once the first switch is created we only support one type... At least for now...
            driverName = ["Tasmota - Universal Plug/Outlet (Child)", "Generic Component Switch"]
        }
    }
    
    // Fan Control
    if(deviceInfo["hasFanControl"] == true) {
        logging("hasFanControl", 0)
        namespace = "tasmota"
        driverName = ["Tasmota - Universal Fan Control (Child)"]
        def childId = "FAN"
        def childName = getChildDeviceNameRoot(keepType=true) + " ${getMinimizedDriverName(driverName[0])} ($childId)"
        def childLabel = "${getMinimizedDriverName(device.getLabel())} ($childId)"
        createChildDevice(namespace, driverName, childId, childName, childLabel)
    }

    // Sensors
    logging("Available in sensorMap: ${deviceInfo["sensorMap"]}, size:${deviceInfo["numSensorGroups"]}", 0)
    deviceInfo["sensorMap"].each {
        logging("sensorMap: $it.key", 0)
        namespace = "tasmota"
        driverName = ["Tasmota - Universal Multi Sensor (Child)"]
        def childId = "${it.key}"
        def childName = getChildDeviceNameRoot(keepType=true) + " ${getMinimizedDriverName(driverName[0])} ($childId)"
        def childLabel = "${getMinimizedDriverName(device.getLabel())} ($childId)"
        createChildDevice(namespace, driverName, childId, childName, childLabel)
    }
    //logging("After sensor creation...", 0)
    // Finally let the default parser have the data as well...
    parseResult(statusMap)
}

String getChildDeviceNameRoot(boolean keepType=false) {
    String childDeviceNameRoot = getDeviceInfoByName('name')
    if(childDeviceNameRoot.toLowerCase().endsWith(' (parent)')) {
        childDeviceNameRoot = childDeviceNameRoot.substring(0, childDeviceNameRoot.length()-9)
    } else if(childDeviceNameRoot.toLowerCase().endsWith(' parent')) {
        childDeviceNameRoot = childDeviceNameRoot.substring(0, childDeviceNameRoot.length()-7)
    }
    if(keepType == false && childDeviceNameRoot.toLowerCase().startsWith('tasmota - ')) {
        childDeviceNameRoot = childDeviceNameRoot.substring(10, childDeviceNameRoot.length())
    }
    return childDeviceNameRoot
}

String getMinimizedDriverName(String driverName) {
    // Remove parts we don't need from the string 
    logging("getMinimizedDriverName(driverName=$driverName)", 1)
    if(driverName.toLowerCase().endsWith(' (child)')) {
        driverName = driverName.substring(0, driverName.length()-8)
    } else if(driverName.toLowerCase().endsWith(' child')) {
        driverName = driverName.substring(0, driverName.length()-6)
    }
    if(driverName.toLowerCase().endsWith(' (parent)')) {
        driverName = driverName.substring(0, driverName.length()-9)
    } else if(driverName.toLowerCase().endsWith(' parent')) {
        driverName = driverName.substring(0, driverName.length()-7)
    }
    // Just replace all Occurrances of Parent
    driverName = driverName.replaceAll("(?i) \\(parent\\)", "").replaceAll("(?i) parent", "").replaceAll("(?i)parent", "")
    logging("driverName: $driverName", 1)

    // Remove IP as well
    driverName = driverName.replaceFirst("\\(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\)", "").replaceFirst("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", "");

    if(driverName.toLowerCase().startsWith('tasmota - ')) {
        driverName = driverName.substring(10, driverName.length())
    }
    if(driverName.toLowerCase().startsWith('universal ')) {
        driverName = driverName.substring(10, driverName.length())
    }
    driverName = driverName.replaceAll("Generic Component ", "")
    driverName = driverName.trim()
    if(driverName == '') driverName = "Device"
    
    logging("getMinimizedDriverName(driverName=$driverName) end", 1)
    return driverName
}

def getChildDeviceByActionType(String actionType) {
    return childDevices.find{it.deviceNetworkId.endsWith("-$actionType")}
}

private void createChildDevice(String namespace, List driverName, String childId, String childName, String childLabel) {
    logging("createChildDevice(namespace=$namespace, driverName=$driverName, childId=$childId, childName=$childName, childLabel=$childLabel)", 1)
    def childDevice = childDevices.find{it.deviceNetworkId.endsWith("-$childId")}
    if(!childDevice && childId.toLowerCase().startsWith("power")) {
        // If this driver was used to replace an "old" parent driver, rename the child Network ID
        logging("Looking for $childId, ending in ${childId.substring(5)}", 1)
        childDevice = childDevices.find{it.deviceNetworkId.endsWith("-${childId.substring(5)}")}
        if(childDevice) {
            logging("Setting new Network ID for $childId to '$device.id-$childId'", 1)
            childDevice.setDeviceNetworkId("$device.id-$childId")
        }
    }
    if (childDevice) {
        // The device exists, just update it
        childDevice.setName(childName)
        //Setting isComponent to false doesn't change how the device is treated...
        //childDevice.updateDataValue('isComponent', "false")
        logging("childDevice.getData(): ${childDevice.getData()}", 1)
    } else {
        logging("The child device doesn't exist, create it...", 0)
        Integer s = childName.size()
        for(i in 0..s) {
            def currentNamespace = namespace
            if(driverName[i].toLowerCase().startsWith('generic component')) {
                currentNamespace = "hubitat"
            }
            try {
                addChildDevice(currentNamespace, driverName[i], "$device.id-$childId", [name: childName, label: childLabel, isComponent: false])
                logging("Created child device '$childLabel' using driver '${driverName[i]}'...", 100)
            } catch (com.hubitat.app.exception.UnknownDeviceTypeException e) {
                if(i == s) {
                    log.error "'${driverName[i]}' driver can't be found! Did you forget to install the child driver?"
                } else {
                    log.warn "'${driverName[i]}' driver can't be found! Trying another driver: ${driverName[i+1]}..."
                }
            }
        }
    }
}

/*
    Tasmota IP Settings and Wifi status
*/
private String setDeviceNetworkId(String macOrIP, boolean isIP = false) {
    String myDNI
    if (isIP == false) {
        myDNI = macOrIP
    } else {
        logging("About to convert ${macOrIP}...", 0)
        myDNI = convertIPtoHex(macOrIP)
    }
    logging("Device Network Id should be set to ${myDNI} from ${macOrIP}", 0)
    return myDNI
}

void prepareDNI() {
    // Called from updateNeededSettings() and parse(description)
    if (useIPAsID) {
        def hexIPAddress = setDeviceNetworkId(ipAddress, true)
        if(hexIPAddress != null && state.dni != hexIPAddress) {
            state.dni = hexIPAddress
            updateDNI()
        }
    } else if (state.mac != null && state.dni != state.mac) { 
        state.dni = setDeviceNetworkId(state.mac)
        updateDNI()
    }
}

private void updateDNI() {
    // Called from:
    // prepareDNI()
    // httpGetAction(uri, callback="parse")
    // postAction(uri, data)
    if (state.dni != null && state.dni != "" && device.deviceNetworkId != state.dni) {
        logging("Device Network Id will be set to ${state.dni} from ${device.deviceNetworkId}", 0)
        device.deviceNetworkId = state.dni
    }
}

Integer getTelePeriodValue() {
    // Naming this getTelePeriod() will cause Error 500 and other unexpected behavior when
    // telePeriod isn't set to anything...
    return (telePeriod != null && telePeriod.isInteger() ? telePeriod.toInteger() : 300)
}

private String getHostAddress() {
    Integer port = 80
    if (getDeviceDataByName("port") != null) {
        port = getDeviceDataByName("port").toInteger()
    }
    if (override == true && ipAddress != null){
        // Preferences
        return "${ipAddress}:$port"
    } else if(device.currentValue("ip") != null) {
        // Current States
        return "${device.currentValue("ip")}:$port"
    } else if(getDeviceDataByName("ip") != null) {
        // Data Section
        return "${getDeviceDataByName("ip")}:$port"
    } else {
        // There really is no fallback here, if we get here, something went WRONG, probably with the DB...
        log.warn "getHostAddress() failed and ran out of fallbacks! If this happens, contact the developer, this is an \"impossible\" scenario!"
	    return "127.0.0.1:$port"
    }
}

private String convertIPtoHex(ipAddress) {
    String hex = null
    if(ipAddress != null) {
        hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
        logging("Got this IP in hex: ${hex}", 0)
    } else {
        hex = null
        if (useIPAsID) {
            logging('ERROR: To use IP as Network ID "Device IP Address" needs to be set and "Override IP" needs to be enabled! If this error persists, consult the release thread in the Hubitat Forum.')
        }
    }
    return hex
}

private String getFirstTwoIPBytes(ipAddress) {
    String ipStart = null
    if(ipAddress != null) {
        ipStart = ipAddress.tokenize( '.' ).take(2).join('.') + '.'
        logging("Got these IP bytes: ${ipStart}", 0)
    } else {
        ipStart = ''
    }
    return ipStart
}

void sync(String ip, Integer port = null) {
    String existingIp = getDataValue("ip")
    String existingPort = getDataValue("port")
    logging("Running sync()", 1)
    if (ip != null && ip != existingIp) {
        updateDataValue("ip", ip)
        sendEvent(name: 'ip', value: ip, isStateChange: false)
        sendEvent(name: "ipLink", value: "<a target=\"device\" href=\"http://$ip\">$ip</a>", isStateChange: false)
        logging("IP set to ${ip}", 1)
    }
    if (port && port != existingPort) {
        updateDataValue("port", port)
        logging("Port set to ${port}", 1)
    }
}

Integer dBmToQuality(Integer dBm) {
    // In Tasmota RSSI is actually % already, so just returning the received value here
    // Keeping this around if this behavior changes
    /*Integer quality = 0
    if(dBm > 0) dBm = dBm * -1
    if(dBm <= -100) {
        quality = 0
    } else if(dBm >= -50) {
        quality = 100
    } else {
        quality = 2 * (dBm + 100)
    }
    logging("DBM: $dBm (${quality}%)", 0)*/
    return dBm
}

/*
    Tasmota Preferences Related
*/
String configuration_model_tasmota() {
'''
<configuration>
<Value type="password" byteSize="1" index="password" label="Device Password" description="REQUIRED if set on the Device! Otherwise leave empty." min="" max="" value="" setting_type="preference" fw="">
<Help>
</Help>
</Value>
</configuration>
'''
}

/*
    HTTP Tasmota API Related
*/
private void httpGetAction(String uri, callback="parse") { 
  updateDNI()
  
  def headers = getHeader()
  logging("Using httpGetAction for 'http://${getHostAddress()}$uri'...", 100)
  try {
    /*hubAction = new hubitat.device.HubAction(
        method: "GET",
        path: uri,
        headers: headers
    )*/
    asynchttpGet(
        callback,
        [uri: "http://${getHostAddress()}$uri",
        headers: headers]
    )
  } catch (e) {
    log.error "Error in httpGetAction(uri): $e ('$uri')"
  }
}

private postAction(String uri, String data) { 
  updateDNI()

  def headers = getHeader()

  def hubAction = null
  try {
    hubAction = new hubitat.device.HubAction(
    method: "POST",
    path: uri,
    headers: headers,
    body: data
  )
  } catch (e) {
    log.error "Error in postAction(uri, data): $e ('$uri', '$data')"
  }
  return hubAction    
}

void sendCommand(String command) {
    sendCommand(command, null)
}

void sendCommand(String command, String argument) {
    String descriptionText = "${command}${argument != null ? " " + argument : ""}"
    logging("sendCommand: $descriptionText", 100)
    sendEvent(name: "commandSent", value: command, descriptionText: descriptionText, isStateChange: true)
    getAction(getCommandString(command, argument), callback="sendCommandParse")
}

def sendCommandParse(asyncResponse, data) {
    // Parse called using sendCommand
    if(asyncResponse != null) {
        try{
            def r = asyncResponse.getJson()
            logging("sendCommandParse(asyncResponse.getJson() = \"${r}\")", 100)
            sendEvent(name: "commandResult", value: asyncResponse.getData(), isStateChange: true)
            parseResult(r)
        } catch(MissingMethodException e1) {
            log.error e1
        } catch(e1) {
            try{
                logging("parse(asyncResponse.data = \"${asyncResponse.data}\", data = \"${data}\") e1=$e1", 1)
            } catch(e2) {
                logging("parse(asyncResponse.data = null, data = \"${data}\") Is the device online? e2=$e2", 1)
            }
        }
    } else {
        logging("parse(asyncResponse.data = null, data = \"${data}\")", 1)
    }
}

String getCommandString(String command, String value) {
    def uri = "/cm?"
    if (password != null) {
        uri += "user=admin&password=${urlEscape(password)}&"
    }
	if (value != null && value != "") {
		uri += "cmnd=${urlEscape(command)}%20${urlEscape(value)}"
	}
	else {
		uri += "cmnd=${urlEscape(command)}"
	}
    return uri
}

String getMultiCommandString(commands) {
    String uri = "/cm?"
    if (password != null) {
        uri += "user=admin&password=${password}&"
    }
    uri += "cmnd=backlog%20"
    if(commands.size() > 30) {
        log.warn "Backlog only supports 30 commands, the last ${commands.size() - 30} will be ignored!"
    }
    commands.each {cmd->
        if(cmd.containsKey("value")) {
          uri += "${urlEscape(cmd['command'])}%20${urlEscape(cmd['value'])}%3B%20"
        } else {
          uri += "${urlEscape(cmd['command'])}%3B%20"
        }
    }
    return uri
}

private String urlEscape(String url) {
    //logging("urlEscape(url = $url)", 1)
    return(URLEncoder.encode(url).replace("+", "%20").replace("#", "%23"))
}

private String convertPortToHex(Integer port) {
	String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}

private encodeCredentials(String username, String password) {
	String userpassascii = "${username}:${password}"
    String userpass = "Basic " + userpassascii.bytes.encodeBase64().toString()
    return userpass
}

private Map getHeader(String userpass = null) {
    Map headers = [:]
    headers.put("Host", getHostAddress())
    headers.put("Content-Type", "application/x-www-form-urlencoded")
    if (userpass != null)
       headers.put("Authorization", userpass)
    return headers
}

/**
 * --END-- TASMOTA METHODS (helpers-tasmota)
 */