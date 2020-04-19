/**
 *  Copyright 2020 Markus Liljergren
 *
 *  Code Version: v1.0.0413Tb
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

// BEGIN:getDefaultImports()
/** Default Imports */
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
// Used for MD5 calculations
import java.security.MessageDigest
// END:  getDefaultImports()


metadata {
	definition (name: "Zigbee - DO NOT USE Generic Wifi Switch/Plug", namespace: "markusl", author: "Markus Liljergren", vid: "generic-switch", importUrl: "https://raw.githubusercontent.com/markus-li/Hubitat/development/drivers/expanded/zigbee-generic-wifi-switch-plug-expanded.groovy") {
        capability "Actuator"
        capability "Light"
		capability "Switch"
		capability "Sensor"

        // BEGIN:getDefaultMetadataCapabilities()
        // Default Capabilities
        capability "Refresh"
        capability "Configuration"
        // END:  getDefaultMetadataCapabilities()
        
        // BEGIN:getDefaultMetadataAttributes()
        // Default Attributes
        attribute   "driver", "string"
        // END:  getDefaultMetadataAttributes()
        // BEGIN:getDefaultMetadataCommands()
        // Default Commands
        command "reboot"
        // END:  getDefaultMetadataCommands()
	}

	simulator {
	}
    
    preferences {
        // BEGIN:getDefaultMetadataPreferences()
        // Default Preferences
        generate_preferences(configuration_model_debug())
        // END:  getDefaultMetadataPreferences()
	}
}

// BEGIN:getDeviceInfoFunction()
String getDeviceInfoByName(infoName) { 
    // DO NOT EDIT: This is generated from the metadata!
    // TODO: Figure out how to get this from Hubitat instead of generating this?
    Map deviceInfo = ['name': 'Zigbee - DO NOT USE Generic Wifi Switch/Plug', 'namespace': 'markusl', 'author': 'Markus Liljergren', 'vid': 'generic-switch', 'importUrl': 'https://raw.githubusercontent.com/markus-li/Hubitat/development/drivers/expanded/zigbee-generic-wifi-switch-plug-expanded.groovy']
    //logging("deviceInfo[${infoName}] = ${deviceInfo[infoName]}", 1)
    return(deviceInfo[infoName])
}
// END:  getDeviceInfoFunction()


// BEGIN:getGenericOnOffFunctions()

/* Generic On/Off functions used when only 1 switch/button exists */
def on() {
	logging("on()", 50)
    def cmds = []
    cmds << getAction(getCommandString("Power", "On"))
    return cmds
}

def off() {
    logging("off()", 50)
	def cmds = []
    cmds << getAction(getCommandString("Power", "Off"))
    return cmds
}
// END:  getGenericOnOffFunctions()


/* These functions are unique to each driver */

def refresh() {
    logging("refresh()", 10)
    // http://ftp1.digi.com/support/images/APP_NOTE_XBee_ZigBee_Device_Profile.pdf
    // https://docs.hubitat.com/index.php?title=Zigbee_Object
    // https://docs.smartthings.com/en/latest/ref-docs/zigbee-ref.html
    //zigbee.clusterLookup(0x0001)
    msgMap = [profileId:0, clusterId:"0x0001", sourceEndpoint:0, 
              destinationEndpoint:0, options:0, messageType:0, dni:"${device.endpointId}", 
              isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0, 
              command:0, direction:0]
    
    logging("${device.deviceNetworkId}", 10)
    logging(zigbee.swapOctets("${device.deviceNetworkId}"), 10)
    zigbee.command(0x0001, 0, "${device.deviceNetworkId}")
    zigbee.command(0x0001, 0, zigbee.swapOctets("${device.deviceNetworkId}"))
    zigbee.command(0x0001, 0, msgMap)
    zigbee.command(0x0001, 0, '')
    zigbee.enrollResponse()
    /*return [
            "he 0x0001 0x${device.endpointId} 0x00 {}","delay 200",  //light state
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0004 0 {}","delay 200",  //light state
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0000 0 {}","delay 200",  //light state
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0086 0 {}","delay 200",  //light state
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0085 0 {}","delay 200",  //light state
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x8600 0 {}","delay 200",  //light state
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x8500 0 {}","delay 200",  //light state
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}","delay 200",  //light state
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0008 0 {}","delay 200",  //light level
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0000 {}","delay 200", //hue
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0001 {}","delay 200", //sat
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0007 {}","delay 200",	//color temp
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"  		//color mode
    ]*/
    return [
        "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}"
    ]
}

def reboot() {
    logging('reboot() is NOT implemented for this device', 1)
    // Ignore
}

def parse(description) {
    // BEGIN:getGenericZigbeeParseHeader()
    // parse() Generic Zigbee-device header BEGINS here
    logging("Parsing: ${description}", 0)
    def events = []
    def msgMap = zigbee.parseDescriptionAsMap(description)
    logging("msgMap: ${msgMap}", 0)
    // parse() Generic header ENDS here
    // END:  getGenericZigbeeParseHeader()
            
    // BEGIN:getGenericZigbeeParseFooter()
    // parse() Generic Zigbee-device footer BEGINS here
    
    return events
    // parse() Generic footer ENDS here
    // END:  getGenericZigbeeParseFooter()
}

def updated()
{
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

def updateNeededSettings()
{
    
}

// BEGIN:getDefaultFunctions()
/* Default Driver Methods go here */
private String getDriverVersion() {
    //comment = ""
    //if(comment != "") state.comment = comment
    String version = "v1.0.0413Tb"
    logging("getDriverVersion() = ${version}", 100)
    sendEvent(name: "driver", value: version)
    updateDataValue('driver', version)
    return version
}
// END:  getDefaultFunctions()


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
 * DEFAULT METHODS (helpers-default)
 *
 * DEPRECATED: This include should NOT be used, refactor to 
 *             include the different parts separately!
 */

/**
 * --END-- DEFAULT METHODS (helpers-default)
 */
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

