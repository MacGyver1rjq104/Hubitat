 /**
 *  Copyright 2019 Markus Liljergren
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

/* Default imports */
import groovy.json.JsonSlurper


metadata {
	definition (name: "Tasmota - DO NOT USE Sonoff RF Bridge (Parent)", namespace: "tasmota", author: "Markus Liljergren", vid: "generic-switch", importURL: "https://raw.githubusercontent.com/markus-li/Hubitat/master/drivers/expanded/tasmota-sonoff-rf-bridge-parent-expanded.groovy") {
        capability "Actuator"
		capability "Switch"
		capability "Sensor"

        
        // Default Capabilities for Energy Monitor
        capability "Voltage Measurement"
        capability "Power Meter"
        capability "Energy Meter"
        
        // Default Capabilities
        capability "Refresh"
        capability "Configuration"
        capability "HealthCheck"
        
        
        // Default Attributes for Energy Monitor
        attribute   "current", "string"
        attribute   "apparentPower", "string"
        attribute   "reactivePower", "string"
        attribute   "powerFactor", "string"
        attribute   "energyToday", "string"
        attribute   "energyYesterday", "string"
        attribute   "energyTotal", "string"
        attribute   "voltageStr", "string"
        attribute   "powerStr", "string"
        attribute   "b0Code", "string"
        
        // Default Attributes
        attribute   "needUpdate", "string"
        //attribute   "uptime", "string"  // This floods the event log!
        attribute   "ip", "string"
        attribute   "ipLink", "string"
        attribute   "module", "string"
        attribute   "templateData", "string"
        attribute   "driverVersion", "string"
        
        // Commands for handling Child Devices
        command "childOn"
        command "childOff"
        command "recreateChildDevices"
        command "deleteChildren"
        
        // Default Commands
        command "reboot"
	}

	simulator {
	}
    
    preferences {
        
        // Default Preferences
        input(name: "runReset", description: "<i>For details and guidance, see the release thread in the <a href=\"https://community.hubitat.com/t/release-tasmota-7-x-firmware-with-hubitat-support/29368\"> Hubitat Forum</a>. For settings marked as ADVANCED, make sure you understand what they do before activating them. If settings are not reflected on the device, press the Configure button in this driver. Also make sure all settings really are saved and correct.<br/>Type RESET and then press 'Save Preferences' to DELETE all Preferences and return to DEFAULTS.</i>", title: "<b>Settings</b>", displayDuringSetup: false, type: "paragraph", element: "paragraph")
        generate_preferences(configuration_model_debug())
        input(name: "b1Code", type: "string", title: "<b>B1 code (received)</b>", description: "<i>Set this to a B1 code and save and the driver will calculate the B0 code.</i>", displayDuringSetup: true, required: false)
        input(name: "b0Code", type: "string", title: "<b>B0 code (command)</b>", description: "<i>Set this to a B0 code or input a B1 code and this will be calculated!</i>", displayDuringSetup: true, required: false)
        input(name: "rfRawMode", type: "bool", title: "<b>RF Raw Mode</b>", description: '<i>Set RF mode to RAW, only works with <a target="portisch" href="https://tasmota.github.io/docs/#/devices/Sonoff-RF-Bridge-433?id=rf-firmware">Portisch</a>. MAY be slower than Standard RF mode, but can handle more signals.</i>', displayDuringSetup: true, required: false)
        
        // Default Preferences for Parent Devices
        input(name: "numSwitches", type: "number", title: "<b>Number of Children</b>", description: "<i>Set the number of children (default 1)</i>", defaultValue: "1", displayDuringSetup: true, required: true)
        
        // Default Preferences for Tasmota
        input(name: "ipAddress", type: "string", title: "<b>Device IP Address</b>", description: "<i>Set this as a default fallback for the auto-discovery feature.</i>", displayDuringSetup: true, required: false)
        input(name: "port", type: "number", title: "<b>Device Port</b>", description: "<i>The http Port of the Device (default: 80)</i>", displayDuringSetup: true, required: false, defaultValue: 80)
        input(name: "override", type: "bool", title: "<b>Override IP</b>", description: "<i>Override the automatically discovered IP address and disable auto-discovery.</i>", displayDuringSetup: true, required: false)
        input(name: "telePeriod", type: "string", title: "<b>Update Frequency</b>", description: "<i>Tasmota sensor value update interval, set this to any value between 10 and 3600 seconds. See the Tasmota docs concerning telePeriod for details. This is NOT a poll frequency. Button/switch changes are immediate and are NOT affected by this. This ONLY affects SENSORS and reporting of data such as UPTIME. (default = 300)</i>", displayDuringSetup: true, required: false)
        generate_preferences(configuration_model_tasmota())
        input(name: "disableModuleSelection", type: "bool", title: "<b>Disable Automatically Setting Module and Template</b>", description: "ADVANCED: <i>Disable automatically setting the Module Type and Template in Tasmota. Enable for using custom Module or Template settings directly on the device. With this disabled, you need to set these settings manually on the device.</i>", displayDuringSetup: true, required: false)
        input(name: "moduleNumber", type: "number", title: "<b>Module Number</b>", description: "ADVANCED: <i>Module Number used in Tasmota. If Device Template is set, this value is IGNORED. (default: -1 (use the default for the driver))</i>", displayDuringSetup: true, required: false, defaultValue: -1)
        input(name: "deviceTemplateInput", type: "string", title: "<b>Device Template</b>", description: "ADVANCED: <i>Set this to a Device Template for Tasmota, leave it EMPTY to use the driver default. Set it to 0 to NOT use a Template. NAME can be maximum 14 characters! (Example: {\"NAME\":\"S120\",\"GPIO\":[0,0,0,0,0,21,0,0,0,52,90,0,0],\"FLAG\":0,\"BASE\":18})</i>", displayDuringSetup: true, required: false)
        input(name: "useIPAsID", type: "bool", title: "<b>IP as Network ID</b>", description: "ADVANCED: <i>Not needed under normal circumstances. Setting this when not needed can break updates. This requires the IP to be static or set to not change in your DHCP server. It will force the use of IP as network ID. When in use, set Override IP to true and input the correct Device IP Address. See the release thread in the Hubitat forum for details and guidance.</i>", displayDuringSetup: true, required: false)
	}
}

def getDeviceInfoByName(infoName) { 
    // DO NOT EDIT: This is generated from the metadata!
    // TODO: Figure out how to get this from Hubitat instead of generating this?
    deviceInfo = ['name': 'Tasmota - DO NOT USE Sonoff RF Bridge (Parent)', 'namespace': 'tasmota', 'author': 'Markus Liljergren', 'vid': 'generic-switch', 'importURL': 'https://raw.githubusercontent.com/markus-li/Hubitat/master/drivers/expanded/tasmota-sonoff-rf-bridge-parent-expanded.groovy']
    return(deviceInfo[infoName])
}

/* These functions are unique to each driver */
def installedAdditional() {
    // This runs from installed()
	logging("installedAdditional()",50)
    createChildDevices()
}

def on() {
	logging("on()",50)
    //logging("device.namespace: ${getDeviceInfoByName('namespace')}, device.driverName: ${getDeviceInfoByName('name')}", 50)
    def cmds = []
    cmds << getAction(getCommandString("Power0", "1"))
    Integer numSwitchesI = numSwitches.toInteger()
    
    for (i in 1..numSwitchesI) {
        cmds << getAction(getCommandString("Power$i", "1"))
    }
    //return delayBetween(cmds, 500)
    return cmds
}

def off() {
    logging("off()",50)
    def cmds = []
    
    cmds << getAction(getCommandString("Power0", "0"))
    Integer numSwitchesI = numSwitches.toInteger()
    
    for (i in 1..numSwitchesI) {
        cmds << getAction(getCommandString("Power$i", "0"))
    }
    //return delayBetween(cmds, 500)
    return cmds
}

def updateRFMode() {
    def cmds = []
    cmds << getAction(getCommandString("seriallog", "0"))
    if(rfRawMode == true) {
        logging("Switching to RAW RF mode...", 100)
        cmds << getAction(getCommandString("rfraw", "177"))
    } else {
        logging("Switching to Standard RF mode...", 100)
        cmds << getAction(getCommandString("rfraw", "0"))
    }
    return cmds
}

def parse(description) {
    // parse() Generic Tasmota-device header BEGINS here
    //log.debug "Parsing: ${description}"
    def events = []
    def descMap = parseDescriptionAsMap(description)
    def body
    //log.debug "descMap: ${descMap}"
    
    if (!state.mac || state.mac != descMap["mac"]) {
        logging("Mac address of device found ${descMap["mac"]}",1)
        state.mac = descMap["mac"]
    }
    
    prepareDNI()
    
    if (descMap["body"] && descMap["body"] != "T04=") body = new String(descMap["body"].decodeBase64())
    
    if (body && body != "") {
        if(body.startsWith("{") || body.startsWith("[")) {
            logging("========== Parsing Report ==========",99)
            def slurper = new JsonSlurper()
            def result = slurper.parseText(body)
            
            logging("result: ${result}",0)
            // parse() Generic header ENDS here
            
            
            // Standard Basic Data parsing
            if (result.containsKey("POWER")) {
                logging("POWER: $result.POWER",99)
                events << createEvent(name: "switch", value: result.POWER.toLowerCase())
            }
            if (result.containsKey("StatusNET")) {
                logging("StatusNET: $result.StatusNET",99)
                result << result.StatusNET
                //logging("result: ${result}",0)
            }
            if (result.containsKey("LoadAvg")) {
                logging("LoadAvg: $result.LoadAvg",99)
            }
            if (result.containsKey("Sleep")) {
                logging("Sleep: $result.Sleep",99)
            }
            if (result.containsKey("SleepMode")) {
                logging("SleepMode: $result.SleepMode",99)
            }
            if (result.containsKey("Vcc")) {
                logging("Vcc: $result.Vcc",99)
            }
            if (result.containsKey("Hostname")) {
                logging("Hostname: $result.Hostname",99)
            }
            if (result.containsKey("IPAddress") && (override == false || override == null)) {
                logging("IPAddress: $result.IPAddress",99)
                events << createEvent(name: "ip", value: "$result.IPAddress")
                //logging("ipLink: <a target=\"device\" href=\"http://$result.IPAddress\">$result.IPAddress</a>",10)
                events << createEvent(name: "ipLink", value: "<a target=\"device\" href=\"http://$result.IPAddress\">$result.IPAddress</a>")
            }
            if (result.containsKey("WebServerMode")) {
                logging("WebServerMode: $result.WebServerMode",99)
            }
            if (result.containsKey("Version")) {
                logging("Version: $result.Version",99)
            }
            if (result.containsKey("Module") && !result.containsKey("Version")) {
                // The check for Version is here to avoid using the wrong message
                logging("Module: $result.Module",50)
                events << createEvent(name: "module", value: "$result.Module")
            }
            // When it is a Template, it looks a bit different
            if (result.containsKey("NAME") && result.containsKey("GPIO") && result.containsKey("FLAG") && result.containsKey("BASE")) {  
                n = result.toMapString()
                n = n.replaceAll(', ',',')
                n = n.replaceAll('\\[','{').replaceAll('\\]','}')
                n = n.replaceAll('NAME:', '"NAME":"').replaceAll(',GPIO:\\{', '","GPIO":\\[')
                n = n.replaceAll('\\},FLAG', '\\],"FLAG"').replaceAll('BASE', '"BASE"')
                // TODO: Learn how to do this the right way in Groovy
                logging("Template: $n",50)
                events << createEvent(name: "templateData", value: "${n}")
            }
            if (result.containsKey("RestartReason")) {
                logging("RestartReason: $result.RestartReason",99)
            }
            if (result.containsKey("TuyaMCU")) {
                logging("TuyaMCU: $result.TuyaMCU",99)
                events << createEvent(name: "tuyaMCU", value: "$result.TuyaMCU")
            }
            if (result.containsKey("SetOption81")) {
                logging("SetOption81: $result.SetOption81",99)
            }
            if (result.containsKey("SetOption113")) {
                logging("SetOption113 (Hubitat enabled): $result.SetOption113",99)
            }
            if (result.containsKey("Uptime")) {
                logging("Uptime: $result.Uptime",99)
                // Even with "displayed: false, archivable: false" these events still show up under events... There is no way of NOT having it that way...
                //events << createEvent(name: 'uptime', value: result.Uptime, displayed: false, archivable: false)
                state.uptime = result.Uptime
            }
            if (result.containsKey("RestartReason")) {
                events << updateRFMode()
            }
            if (result.containsKey("RfReceived")) {
                logging("RfReceived: $result.RfReceived", 100)
                if(rfRawMode == true) {
                    logging("Switching to RAW RF mode...", 100)
                    events << getAction(getCommandString("rfraw", "177"))
                }
            }
            if (result.containsKey("RfRaw")) {
                logging("RfRaw: $result.RfRaw", 100)
                if (!(result.RfRaw instanceof String) && result.RfRaw.containsKey("Data")) {
                    rawData = result.RfRaw.Data.toString()
                    
                    if(rawData.substring(3,5) != 'B1' && rawData != "AAA055") {
                        // We have RAW data and it is NOT B1 data, fix it:
                        logging("Incorrect RAW mode, fixing it now...", 100) 
                        events << getAction(getCommandString("rfraw", "177"))
                    }
                    // Save CPU:
                    if (logLevel == "100" && rawData.substring(3,5) == 'B1' ) {
                        logging("Calculated B0: ${calculateB0(rawData, 0).replace(' ', '')}", 100)
                    }
                }
                if(rfRawMode != true) {
                    logging("Switching to Standard RF mode...", 100)
                    events << getAction(getCommandString("rfraw", "0"))
                }
            }
            
            // Standard Wifi Data parsing
            if (result.containsKey("Wifi")) {
                if (result.Wifi.containsKey("AP")) {
                    logging("AP: $result.Wifi.AP",99)
                }
                if (result.Wifi.containsKey("BSSId")) {
                    logging("BSSId: $result.Wifi.BSSId",99)
                }
                if (result.Wifi.containsKey("Channel")) {
                    logging("Channel: $result.Wifi.Channel",99)
                }
                if (result.Wifi.containsKey("RSSI")) {
                    logging("RSSI: $result.Wifi.RSSI",99)
                }
                if (result.Wifi.containsKey("SSId")) {
                    logging("SSId: $result.Wifi.SSId",99)
                }
            }
            
            // Standard TuyaSwitch Data parsing
            if (result.containsKey("POWER1")) {
                logging("POWER1: $result.POWER1",1)
                childSendState("1", result.POWER1.toLowerCase())
                events << createEvent(name: "switch", value: (areAllChildrenSwitchedOn(result.POWER1.toLowerCase() == "on"?1:0) && result.POWER1.toLowerCase() == "on"? "on" : "off"))
            }
            if (result.containsKey("POWER2")) {
                logging("POWER2: $result.POWER2",1)
                childSendState("2", result.POWER2.toLowerCase())
                events << createEvent(name: "switch", value: (areAllChildrenSwitchedOn(result.POWER2.toLowerCase() == "on"?2:0) && result.POWER2.toLowerCase() == "on"? "on" : "off"))
            }
            if (result.containsKey("POWER3")) {
                logging("POWER3: $result.POWER3",1)
                childSendState("3", result.POWER3.toLowerCase())
                events << createEvent(name: "switch", value: (areAllChildrenSwitchedOn(result.POWER3.toLowerCase() == "on"?3:0) && result.POWER3.toLowerCase() == "on"? "on" : "off"))
            }
            if (result.containsKey("POWER4")) {
                logging("POWER4: $result.POWER4",1)
                childSendState("4", result.POWER4.toLowerCase())
                events << createEvent(name: "switch", value: (areAllChildrenSwitchedOn(result.POWER4.toLowerCase() == "on"?4:0) && result.POWER4.toLowerCase() == "on" ? "on" : "off"))
            }
            
            // Standard Energy Monitor Data parsing
            if (result.containsKey("StatusSNS")) {
                result << result.StatusSNS
            }
            if (result.containsKey("ENERGY")) {
                //logging("Has ENERGY...", 1)
                //if (!state.containsKey('energy')) state.energy = {}
                if (result.ENERGY.containsKey("Total")) {
                    logging("Total: $result.ENERGY.Total kWh",99)
                    events << createEvent(name: "energyTotal", value: "$result.ENERGY.Total kWh")  
                }
                if (result.ENERGY.containsKey("Today")) {
                    logging("Today: $result.ENERGY.Today kWh",99)
                    events << createEvent(name: "energyToday", value: "$result.ENERGY.Today kWh")
                }
                if (result.ENERGY.containsKey("Yesterday")) {
                    logging("Yesterday: $result.ENERGY.Yesterday kWh",99)
                    events << createEvent(name: "energyYesterday", value: "$result.ENERGY.Yesterday kWh")
                }
                if (result.ENERGY.containsKey("Current")) {
                    logging("Current: $result.ENERGY.Current A",99)
                    r = (result.ENERGY.Current == null) ? 0 : result.ENERGY.Current
                    events << createEvent(name: "current", value: "$r A")
                }
                if (result.ENERGY.containsKey("ApparentPower")) {
                    logging("apparentPower: $result.ENERGY.ApparentPower VA",99)
                    events << createEvent(name: "apparentPower", value: "$result.ENERGY.ApparentPower VA")
                }
                if (result.ENERGY.containsKey("ReactivePower")) {
                    logging("reactivePower: $result.ENERGY.ReactivePower VAr",99)
                    events << createEvent(name: "reactivePower", value: "$result.ENERGY.ReactivePower VAr")
                }
                if (result.ENERGY.containsKey("Factor")) {
                    logging("powerFactor: $result.ENERGY.Factor",99)
                    events << createEvent(name: "powerFactor", value: "$result.ENERGY.Factor")
                }
                if (result.ENERGY.containsKey("Voltage")) {
                    logging("Voltage: $result.ENERGY.Voltage V",99)
                    r = (result.ENERGY.Voltage == null) ? 0 : result.ENERGY.Voltage
                    events << createEvent(name: "voltageWithUnit", value: "$r V")
                    events << createEvent(name: "voltage", value: r, unit: "V")
                }
                if (result.ENERGY.containsKey("Power")) {
                    logging("Power: $result.ENERGY.Power W",99)
                    r = (result.ENERGY.Power == null) ? 0 : result.ENERGY.Power
                    events << createEvent(name: "powerWithUnit", value: "$r W")
                    events << createEvent(name: "power", value: r, unit: "W")
                    //state.energy.power = r
                }
            }
            // StatusPTH:[PowerDelta:0, PowerLow:0, PowerHigh:0, VoltageLow:0, VoltageHigh:0, CurrentLow:0, CurrentHigh:0]
        // parse() Generic Tasmota-device footer BEGINS here
        } else {
                //log.debug "Response is not JSON: $body"
            }
        }
        
        if (!device.currentValue("ip") || (device.currentValue("ip") != getDataValue("ip"))) {
            curIP = getDataValue("ip")
            logging("Setting IP: $curIP", 1)
            events << createEvent(name: 'ip', value: curIP)
            events << createEvent(name: "ipLink", value: "<a target=\"device\" href=\"http://$curIP\">$curIP</a>")
        }
        
        return events
        // parse() Generic footer ENDS here
}

def calculateB0(inputStr, repeats) {
    // This calculates the B0 value from the B1 for use with the Sonoff RF Bridge
    logging('inputStr: ' + inputStr, 0)
    inputStr = inputStr.replace(' ', '')
    //logging('inputStr.substring(4,6): ' + inputStr.substring(4,6), 0)
    numBuckets = Integer.parseInt(inputStr.substring(4,6), 16)
    buckets = []

    logging('numBuckets: ' + numBuckets.toString(), 0)

    outAux = String.format(' %02X ', numBuckets.toInteger())
    outAux = outAux + String.format(' %02X ', repeats.toInteger())
    
    logging('outAux1: ' + outAux, 0)
    
    j = 0
    for(i in (0..numBuckets-1)){
        outAux = outAux + inputStr.substring(6+i*4,10+i*4) + " "
        j = i
    }
    logging('outAux2: ' + outAux, 0)
    outAux = outAux + inputStr.substring(10+j*4, inputStr.length()-2)
    logging('outAux3: ' + outAux, 0)

    dataStr = outAux.replace(' ', '')
    outAux = outAux + ' 55'
    length = (dataStr.length() / 2).toInteger()
    outAux = "AA B0 " + String.format(' %02X ', length.toInteger()) + outAux
    logging('outAux4: ' + outAux, 0)
    logging('outAux: ' + outAux.replace(' ', ''), 10)

    return(outAux)
}

def update_needed_settings()
{
    // updateNeededSettings() Generic header BEGINS here
    def cmds = []
    def currentProperties = state.currentProperties ?: [:]
    
    state.settings = settings
    
    def configuration = new XmlSlurper().parseText(configuration_model_tasmota())
    def isUpdateNeeded = "NO"
    
    if(runReset != null && runReset == 'RESET') {
        for ( e in state.settings ) {
            logging("Deleting '${e.key}' with value = ${e.value} from Settings", 50)
            // Not sure which ones are needed, so doing all...
            device.clearSetting("${e.key}")
            device.removeSetting("${e.key}")
            state.settings.remove("${e.key}")
        }
    }
    
    prepareDNI()
    
    // updateNeededSettings() Generic header ENDS here

    /*logging('Just saved...', 10)
    if((b0Code == null || b0Code == '') && b1Code != null && b1Code != '') {
        b0CodeTmp = calculateB0(b1Code, 0)
        b0Code = b0CodeTmp.replace(' ', '')
        sendEvent(name: "b0Code", value: b0CodeTmp)
        state.b0Code = b0Code
        logging('Calculated b0Code! ', 10)
    }*/

    
    // Don't send any other commands until AFTER setting the correct Module/Template
    
    // Tasmota Module and Template selection command (autogenerated)
    cmds << getAction(getCommandString("Module", null))
    cmds << getAction(getCommandString("Template", null))
    if(disableModuleSelection == null) disableModuleSelection = false
    moduleNumberUsed = moduleNumber
    if(moduleNumber == null || moduleNumber == -1) moduleNumberUsed = 0
    useDefaultTemplate = false
    defaultDeviceTemplate = ''
    if(deviceTemplateInput != null && deviceTemplateInput == "0") {
        useDefaultTemplate = true
        defaultDeviceTemplate = ''
    }
    if(deviceTemplateInput == null || deviceTemplateInput == "") {
        // We should use the default of the driver
        useDefaultTemplate = true
        defaultDeviceTemplate = '{"NAME":"Sonoff Bridge","GPIO":[17,148,255,149,255,255,0,0,255,56,255,0,0],"FLAG":0,"BASE":25}'
    }
    if(deviceTemplateInput != null) deviceTemplateInput = deviceTemplateInput.replaceAll(' ','')
    if(disableModuleSelection == false && ((deviceTemplateInput != null && deviceTemplateInput != "") || 
                                           (useDefaultTemplate && defaultDeviceTemplate != ""))) {
        if(useDefaultTemplate == false && deviceTemplateInput != null && deviceTemplateInput != "") {
            usedDeviceTemplate = deviceTemplateInput
        } else {
            usedDeviceTemplate = defaultDeviceTemplate
        }
        logging("Setting the Template soon...", 10)
        logging("templateData = ${device.currentValue('templateData')}", 10)
        if(usedDeviceTemplate != '') moduleNumberUsed = 0  // This activates the Template when set
        if(usedDeviceTemplate != null && device.currentValue('templateData') != null && device.currentValue('templateData') != usedDeviceTemplate) {
            logging("The template is NOT set to '${usedDeviceTemplate}', it is set to '${device.currentValue('templateData')}'",10)
            urlencodedTemplate = URLEncoder.encode(usedDeviceTemplate).replace("+", "%20")
            // The NAME part of th Device Template can't exceed 14 characters! More than that and they will be truncated.
            // TODO: Parse and limit the size of NAME
            cmds << getAction(getCommandString("Template", "${urlencodedTemplate}"))
        } else if (device.currentValue('module') == null){
            // Update our stored value!
            cmds << getAction(getCommandString("Template", null))
        }else if (usedDeviceTemplate != null) {
            logging("The template is set to '${usedDeviceTemplate}' already!",10)
        }
    } else {
        logging("Can't set the Template...", 10)
        logging(device.currentValue('templateData'), 10)
        //logging("deviceTemplateInput: '${deviceTemplateInput}'", 10)
        //logging("disableModuleSelection: '${disableModuleSelection}'", 10)
    }
    if(disableModuleSelection == false && moduleNumberUsed != null && moduleNumberUsed >= 0) {
        logging("Setting the Module soon...", 10)
        logging("device.currentValue('module'): '${device.currentValue('module')}'", 10)
        if(moduleNumberUsed != null && device.currentValue('module') != null && !device.currentValue('module').startsWith("[${moduleNumberUsed}:")) {
            logging("This DOESN'T start with [${moduleNumberUsed} ${device.currentValue('module')}",10)
            cmds << getAction(getCommandString("Module", "${moduleNumberUsed}"))
        } else if (moduleNumberUsed != null && device.currentValue('module') != null){
            logging("This starts with [${moduleNumberUsed} ${device.currentValue('module')}",10)
        } else if (device.currentValue('module') == null){
            // Update our stored value!
            cmds << getAction(getCommandString("Module", null))
        } else {
            logging("Module is set to '${device.currentValue('module')}', and it's set to be null, report this to the creator of this driver!",10)
        }
    } else {
        logging("Setting the Module has been disabled!", 10)
    }

    //cmds << getAction(getCommandString("SetOption81", "1")) // Set PCF8574 component behavior for all ports as inverted (default=0)
    //cmds << getAction(getCommandString("LedPower", "1"))  // 1 = turn LED ON and set LedState 8
    //cmds << getAction(getCommandString("LedState", "8"))  // 8 = LED on when Wi-Fi and MQTT are connected.
    
    
    // updateNeededSettings() TelePeriod setting
    cmds << getAction(getCommandString("TelePeriod", (telePeriod == '' || telePeriod == null ? "300" : telePeriod)))
    
    // Don't send these types of commands until AFTER setting the correct Module/Template
    cmds << updateRFMode()

    
    // updateNeededSettings() Generic footer BEGINS here
    cmds << getAction(getCommandString("SetOption113", "1")) // Hubitat Enabled
    // Disabling Emulation so that we don't flood the logs with upnp traffic
    //cmds << getAction(getCommandString("Emulation", "0")) // Emulation Disabled
    cmds << getAction(getCommandString("HubitatHost", device.hub.getDataValue("localIP")))
    logging("HubitatPort: ${device.hub.getDataValue("localSrvPortTCP")}", 1)
    cmds << getAction(getCommandString("HubitatPort", device.hub.getDataValue("localSrvPortTCP")))
    cmds << getAction(getCommandString("FriendlyName1", URLEncoder.encode(device.displayName.take(32)))) // Set to a maximum of 32 characters
    
    if(override == true) {
        cmds << sync(ipAddress)
    }
    
    //logging("Cmds: " +cmds,1)
    sendEvent(name:"needUpdate", value: isUpdateNeeded, displayed:false, isStateChange: true)
    return cmds
    // updateNeededSettings() Generic footer ENDS here
}

/* Default functions go here */
private def getDriverVersion() {
    logging("getDriverVersion()", 50)
	def cmds = []
    comment = "UNTESTED driver - <a target=\"blakadder\" href=\"https://templates.blakadder.com/sonoff_RF_bridge.html\">Device Model Info</a>"
    if(comment != "") state.comment = comment
    sendEvent(name: "driverVersion", value: "v0.9.2 for Tasmota 7.x (Hubitat version)")
    return cmds
}


/* Logging function included in all drivers */
private def logging(message, level) {
    if (logLevel != "0"){
        switch (logLevel) {
        case "-1": // Insanely verbose
            if (level >= 0 && level < 99 || level == 100)
                log.debug "$message"
        break
        case "1": // Very verbose
            if (level >= 1 && level < 99 || level == 100)
                log.debug "$message"
        break
        case "10": // A little less
            if (level >= 10 && level < 99 || level == 100)
                log.debug "$message"
        break
        case "50": // Rather chatty
            if (level >= 50 )
                log.debug "$message"
        break
        case "99": // Only parsing reports
            if (level >= 99 )
                log.debug "$message"
        break
        
        case "100": // Only special debug messages, eg IR and RF codes
            if (level == 100 )
                log.debug "$message"
        break
        }
    }
}


/* Helper functions included in all drivers */
def ping() {
    logging("ping()", 50)
    refresh()
}

def installed() {
	logging("installed()", 50)
	configure()
    try {
        // In case we have some more to run specific to this driver
        installedAdditional()
    } catch (MissingMethodException e) {
        // ignore
    }
}

/*
	initialize

	Purpose: initialize the driver
	Note: also called from updated() in most drivers
*/
void initialize()
{
    logging("initialize()", 50)
	unschedule()
    // disable debug logs after 30 min, unless override is in place
	if (logLevel != "0") {
        if(runReset != "DEBUG") {
            log.warn "Debug logging will be disabled in 30 minutes..."
        } else {
            log.warn "Debug logging will NOT BE AUTOMATICALLY DISABLED!"
        }
        runIn(1800, logsOff)
    }
}

def configure() {
    logging("configure()", 50)
    def cmds = []
    cmds = update_needed_settings()
    try {
        // Run the getDriverVersion() command
        newCmds = getDriverVersion()
        if (newCmds != null && newCmds != []) cmds = cmds + newCmds
    } catch (MissingMethodException e) {
        // ignore
    }
    if (cmds != []) cmds
}

def generate_preferences(configuration_model)
{
    def configuration = new XmlSlurper().parseText(configuration_model)
   
    configuration.Value.each
    {
        if(it.@hidden != "true" && it.@disabled != "true"){
        switch(it.@type)
        {   
            case ["number"]:
                input "${it.@index}", "number",
                    title:"<b>${it.@label}</b>\n" + "${it.Help}",
                    description: "<i>${it.@description}</i>",
                    range: "${it.@min}..${it.@max}",
                    defaultValue: "${it.@value}",
                    displayDuringSetup: "${it.@displayDuringSetup}"
            break
            case "list":
                def items = []
                it.Item.each { items << ["${it.@value}":"${it.@label}"] }
                input "${it.@index}", "enum",
                    title:"<b>${it.@label}</b>\n" + "${it.Help}",
                    description: "<i>${it.@description}</i>",
                    defaultValue: "${it.@value}",
                    displayDuringSetup: "${it.@displayDuringSetup}",
                    options: items
            break
            case ["password"]:
                input "${it.@index}", "password",
                    title:"<b>${it.@label}</b>\n" + "${it.Help}",
                    description: "<i>${it.@description}</i>",
                    displayDuringSetup: "${it.@displayDuringSetup}"
            break
            case "decimal":
               input "${it.@index}", "decimal",
                    title:"<b>${it.@label}</b>\n" + "${it.Help}",
                    description: "<i>${it.@description}</i>",
                    range: "${it.@min}..${it.@max}",
                    defaultValue: "${it.@value}",
                    displayDuringSetup: "${it.@displayDuringSetup}"
            break
            case "boolean":
               input "${it.@index}", "boolean",
                    title:"<b>${it.@label}</b>\n" + "${it.Help}",
                    description: "<i>${it.@description}</i>",
                    defaultValue: "${it.@value}",
                    displayDuringSetup: "${it.@displayDuringSetup}"
            break
        }
        }
    }
}

def update_current_properties(cmd)
{
    def currentProperties = state.currentProperties ?: [:]
    currentProperties."${cmd.name}" = cmd.value

    if (state.settings?."${cmd.name}" != null)
    {
        if (state.settings."${cmd.name}".toString() == cmd.value)
        {
            sendEvent(name:"needUpdate", value:"NO", displayed:false, isStateChange: true)
        }
        else
        {
            sendEvent(name:"needUpdate", value:"YES", displayed:false, isStateChange: true)
        }
    }
    state.currentProperties = currentProperties
}

/*
	logsOff

	Purpose: automatically disable debug logging after 30 mins.
	Note: scheduled in Initialize()
*/
void logsOff(){
    if(runReset != "DEBUG") {
        log.warn "Debug logging disabled..."
        // Setting logLevel to "0" doesn't seem to work, it disables logs, but does not update the UI...
        //device.updateSetting("logLevel",[value:"0",type:"string"])
        //app.updateSetting("logLevel",[value:"0",type:"list"])
        // Not sure which ones are needed, so doing all... This works!
        device.clearSetting("logLevel")
        device.removeSetting("logLevel")
        state.settings.remove("logLevel")
    } else {
        log.warn "OVERRIDE: Disabling Debug logging will not execute with 'DEBUG' set..."
        if (logLevel != "0") runIn(1800, logsOff)
    }
}

def configuration_model_debug()
{
'''
<configuration>
<Value type="list" index="logLevel" label="Debug Log Level" description="Under normal operations, set this to None. Only needed for debugging. Auto-disabled after 30 minutes." value="0" setting_type="preference" fw="">
<Help>
</Help>
    <Item label="None" value="0" />
    <Item label="Insanely Verbose" value="-1" />
    <Item label="Very Verbose" value="1" />
    <Item label="Verbose" value="10" />
    <Item label="Reports+Status" value="50" />
    <Item label="Reports" value="99" />
    <Item label="RF Codes" value="100" />
</Value>
</configuration>
'''
}

/* Helper functions included when needing Child devices */
// Get the button number
private channelNumber(String dni) {
    def ch = dni.split("-")[-1] as Integer
    return ch
}

def childOn(String dni) {
    // Make sure to create an onOffCmd that sends the actual command
    onOffCmd(1, channelNumber(dni))
}

def childOff(String dni) {
    // Make sure to create an onOffCmd that sends the actual command
    onOffCmd(0, channelNumber(dni))
}

private childSendState(String currentSwitchNumber, String state) {
    def childDevice = childDevices.find{it.deviceNetworkId.endsWith("-${currentSwitchNumber}")}
    if (childDevice) {
        logging("childDevice.sendEvent ${currentSwitchNumber} ${state}",1)
        childDevice.sendEvent(name: "switch", value: state, type: type)
    } else {
        logging("childDevice.sendEvent ${currentSwitchNumber} is missing!",1)
    }
}

private areAllChildrenSwitchedOn(Integer skip = 0) {
    def children = getChildDevices()
    boolean status = true
    Integer i = 1
    // Enumerating this way may be incorrect if we have more children than actual switches
    // due to having changed the number of switches in the config and not deleted the extra
    // switches. Just delete unneeded children...
    children.each {child->
        if (i!=skip) {
  		    if(child.currentState("switch")?.value == "off") {
                status = false
            }
        }
        i++
    }
    return status
}

def getChildDriverName() {
    deviceDriverName = getDeviceInfoByName('name')
    if(deviceDriverName.toLowerCase().endsWith(' (parent)')) {
        deviceDriverName = deviceDriverName.substring(0, deviceDriverName.length()-9)
    }
    childDriverName = "${deviceDriverName} (Child)"
    logging("childDriverName = '$childDriverName'", 1)
    return(childDriverName)
}

private void createChildDevices() {
    Integer numSwitchesI = numSwitches.toInteger()
    logging("createChildDevices: creating $numSwitchesI device(s)",1)
    
    // If making changes here, don't forget that recreateDevices need to have the same settings set
    for (i in 1..numSwitchesI) {
        // https://community.hubitat.com/t/composite-devices-parent-child-devices/1925
        addChildDevice("${getDeviceInfoByName("namespace")}", "${getChildDriverName()}", "$device.id-$i", [name: "${getDeviceInfoByName("name")} #$i", label: "$device.displayName $i", isComponent: false])
    }
}

def recreateChildDevices() {
    Integer numSwitchesI = numSwitches.toInteger()
    logging("recreateChildDevices: recreating $numSwitchesI device(s)",1)
    def childDevice = null

    for (i in 1..numSwitchesI) {
        childDevice = childDevices.find{it.deviceNetworkId.endsWith("-$i")}
        if (childDevice) {
            // The device exists, just update it
            childDevice.setName("${getDeviceInfoByName('name')} #$i")
            childDevice.setDeviceNetworkId("$device.id-$i")  // This doesn't work right now...
            logging(childDevice.getData(), 10)
            // We leave the device Label alone, since that might be desired by the user to change
            //childDevice.setLabel("$device.displayName $i")
            //.setLabel doesn't seem to work on child devices???
        } else {
            // No such device, we should create it
            addChildDevice("${getDeviceInfoByName("namespace")}", "${getChildDriverName()}", "$device.id-$i", [name: "${getDeviceInfoByName("name")} #$i", label: "$device.displayName $i", isComponent: false])
        }
    }
    if (numSwitchesI < 4) {
        // Check if we should delete some devices
        for (i in 1..4) {
            if (i > numSwitchesI) {
                childDevice = childDevices.find{it.deviceNetworkId.endsWith("-$i")}
                if (childDevice) {
                    logging("Removing child #$i!", 10)
                    deleteChildDevice(childDevice.deviceNetworkId)
                }
            }
        }
    }
}

def deleteChildren() {
	logging("deleteChildren",1)
	def children = getChildDevices()
    
    children.each {child->
  		deleteChildDevice(child.deviceNetworkId)
    }
}

/* Helper functions included in all Tasmota drivers */
def refresh() {
	logging("refresh()", 10)
    def cmds = []
    cmds << getAction(getCommandString("Status", "0"))
    return cmds
}

def reboot() {
	logging("reboot()", 10)
    getAction(getCommandString("Restart", "1"))
}

def updated()
{
    logging("updated()", 10)
    def cmds = [] 
    cmds = update_needed_settings()
    sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "lan", hubHardwareId: device.hub.hardwareID])
    sendEvent(name:"needUpdate", value: device.currentValue("needUpdate"), displayed:false, isStateChange: true)
    logging(cmds,0)
    try {
        // Also run initialize(), if it exists...
        initialize()
    } catch (MissingMethodException e) {
        // ignore
    }
    if (cmds != [] && cmds != null) cmds
}

def prepareDNI() {
    if (useIPAsID) {
        hexIPAddress = setDeviceNetworkId(ipAddress, true)
        if(hexIPAddress != null && state.dni != hexIPAddress) {
            state.dni = hexIPAddress
            updateDNI()
        }
    }
    else if (state.mac != null && state.dni != state.mac) { 
        state.dni = setDeviceNetworkId(state.mac)
        updateDNI()
    }
}



def getCommandString(command, value) {
    def uri = "/cm?"
    if (password) {
        uri += "user=admin&password=${password}&"
    }
	if (value) {
		uri += "cmnd=${command}%20${value}"
	}
	else {
		uri += "cmnd=${command}"
	}
    return uri
}

def parseDescriptionAsMap(description) {
	description.split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
        
        if (nameAndValue.length == 2) map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
        else map += [(nameAndValue[0].trim()):""]
	}
}

private getAction(uri){ 
    logging("Using getAction for '${uri}'...", 0)
    return httpGetAction(uri)
}

private httpGetAction(uri){ 
  updateDNI()
  
  def headers = getHeader()
  //logging("Using httpGetAction for '${uri}'...", 0)
  def hubAction = new hubitat.device.HubAction(
    method: "GET",
    path: uri,
    headers: headers
  )
  return hubAction    
}

private postAction(uri, data){ 
  updateDNI()

  def headers = getHeader()

  def hubAction = new hubitat.device.HubAction(
    method: "POST",
    path: uri,
    headers: headers,
    body: data
  )
  return hubAction    
}

private onOffCmd(value, endpoint) {
    logging("onOffCmd, value: $value, endpoint: $endpoint", 1)
    def cmds = []
    cmds << getAction(getCommandString("Power$endpoint", "$value"))
    return cmds
}

private setDeviceNetworkId(macOrIP, isIP = false){
    def myDNI
    if (isIP == false) {
        myDNI = macOrIP
    } else {
        logging("About to convert ${macOrIP}...", 0)
        myDNI = convertIPtoHex(macOrIP)
    }
    logging("Device Network Id should be set to ${myDNI} from ${macOrIP}", 0)
    return myDNI
}

private updateDNI() { 
    if (state.dni != null && state.dni != "" && device.deviceNetworkId != state.dni) {
        logging("Device Network Id will be set to ${state.dni} from ${device.deviceNetworkId}", 0)
        device.deviceNetworkId = state.dni
    }
}

private getHostAddress() {
    if (port == null) {
        port = 80
    }
    if (override == true && ipAddress != null){
        return "${ipAddress}:${port}"
    }
    else if(getDeviceDataByName("ip") && getDeviceDataByName("port")){
        return "${getDeviceDataByName("ip")}:${getDeviceDataByName("port")}"
    }else{
	    return "${ip}:80"
    }
}

private String convertIPtoHex(ipAddress) {
    String hex = null
    if(ipAddress != null) {
        hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
        logging("Get this IP in hex: ${hex}", 0)
    } else {
        hex = null
        if (useIPAsID) {
            logging('ERROR: To use IP as Network ID "Device IP Address" needs to be set and "Override IP" needs to be enabled! If this error persists, consult the release thread in the Hubitat Forum.')
        }
    }
    return hex
}

private String urlEscape(url) {
    return(URLEncoder.encode(url).replace("+", "%20"))
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}

private encodeCredentials(username, password){
	def userpassascii = "${username}:${password}"
    def userpass = "Basic " + userpassascii.bytes.encodeBase64().toString()
    return userpass
}

private getHeader(userpass = null){
    def headers = [:]
    headers.put("Host", getHostAddress())
    headers.put("Content-Type", "application/x-www-form-urlencoded")
    if (userpass != null)
       headers.put("Authorization", userpass)
    return headers
}

def sync(ip, port = null) {
    def existingIp = getDataValue("ip")
    def existingPort = getDataValue("port")
    logging("Running sync()", 1)
    if (ip && ip != existingIp) {
        updateDataValue("ip", ip)
        sendEvent(name: 'ip', value: ip)
        sendEvent(name: "ipLink", value: "<a target=\"device\" href=\"http://$ip\">$ip</a>")
        logging("IP set to ${ip}", 1)
    }
    if (port && port != existingPort) {
        updateDataValue("port", port)
        logging("Port set to ${port}", 1)
    }
}

def configuration_model_tasmota()
{
'''
<configuration>
<Value type="password" byteSize="1" index="password" label="Device Password" description="REQUIRED if set on the Device! Otherwise leave empty." min="" max="" value="" setting_type="preference" fw="">
<Help>
</Help>
</Value>
</configuration>
'''
}