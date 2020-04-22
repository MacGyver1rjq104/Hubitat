#!include:getHeaderLicense()

#!include:getDefaultImports()

metadata {
    // Do NOT rename the child driver name unless you also change the corresponding code in the Parent!
    definition (name: "Tasmota - Universal Curtain (Child)", namespace: "tasmota", author: "Markus Liljergren") {
        capability "WindowShade"
        capability "Refresh"
        // These 4 capabilities are included to be compatible with integrations like Alexa:
        capability "Actuator"
        capability "Switch"
        capability "Light"
        capability "SwitchLevel"

        attribute   "target", "number"

        #!include:getMinimumChildAttributes()
        command "stop"
    }

    preferences {
        #!include:getDefaultMetadataPreferences()

    }

    // The below line needs to exist in ALL drivers for custom CSS to work!
    #!include:getMetadataCustomizationMethods()
}

#!include:getDeviceInfoFunction()

/* These functions are unique to each driver */
void parse(List<Map> description) {
    description.each {
        if(it.name in ["position", "windowShade"]) {
            logging(it.descriptionText, 100)
            sendEvent(it)
            if(it.name == "windowShade") {
                if(it.value == "closed") {
                    sendEvent(name:"switch", value: 'off')
                } else {
                    sendEvent(name:"switch", value: 'on')
                }
            }
        } else if(it.name == "level") {
            target = device.currentValue("target")
            if(target != null && target != -1) {
                logging("target: ${target}", 1)
                String cState = device.currentValue("windowShade", true)
                if((cState == 'opening' && it.value < target + 5) ||
                    (cState == 'closing' && it.value > target - 5)) {
                    stop()
                }
            }
            sendEvent(it)
        } else if(it.name == "tuyaData") {
            // This parsing is for the ZNSN Curtain Wall Panel
            String tdata = it.value
            logging("Got Tuya Data: $tdata", 1)
            if(tdata == '55AA00070005020400010214') {
                // Stop Event occured
                BigDecimal position = device.currentValue("level", true)
                sendEvent(name: "target", value: -1, isStateChange: true)
                Integer margin = 11
                if(position > margin && position < 100 - margin) {
                    logging('Curtain status: partially open', 100)
                    sendEvent(name: "windowShade", value: "partially open", isStateChange: true)
                    sendEvent(name: "switch", value: "on", isStateChange: true)
                } else if(position <= margin) {
                    logging('Curtain status: open', 100)
                    setLevel(0)
                    sendEvent(name: "windowShade", value: "open", isStateChange: true)
                    sendEvent(name: "switch", value: "on", isStateChange: true)
                } else if(position >= 100 - margin) {
                    logging('Curtain status: closed', 100)
                    setLevel(100)
                    sendEvent(name: "windowShade", value: "closed", isStateChange: true)
                    sendEvent(name: "switch", value: "off", isStateChange: true)
                }
            } else if(tdata == '55AA00070005020400010012') {
                // Open Event occured
                logging('Curtain status: opening', 100)
                sendEvent(name: "windowShade", value: "opening", isStateChange: true)
                sendEvent(name: "switch", value: "on", isStateChange: true)
            } else if(tdata == '55AA00070005020400010113') {
                // Close Event occured
                logging('Curtain status: closing', 100)
                sendEvent(name: "windowShade", value: "closing", isStateChange: true)
                sendEvent(name: "switch", value: "on", isStateChange: true)
            }
        } else if(it.name in ["switch"]) {
            logging("Ignored: " + it.descriptionText, 1)
        } else {
            log.warn "Got '$it.name' attribute data, but doesn't know what to do with it! Did you choose the right device type?"
        }
    }
}

void updated() {
    log.info "updated()"
    #!include:getChildComponentDefaultUpdatedContent()
    refresh()
}

void installed() {
    log.info "installed()"
    device.removeSetting("logLevel")
    device.updateSetting("logLevel", "100")
    refresh()
}

void refresh() {
    #!include:getChildComponentMetaConfigCommands()
    parent?.componentRefresh(this.device)
}

void open() {
    parent?.componentOpen(this.device)    
}

void on() {
    open()
}

void close() {
    parent?.componentClose(this.device)    
}

void off() {
    close()
}

void stop() {
    parent?.componentStop(this.device)    
}

void setPosition(BigDecimal targetPosition) {
    // This command we implement here in the Child
    BigDecimal position = device.currentValue("level", true)
    logging("setPosition(targetPosition=${targetPosition}) current: ${position}", 1)
    if(targetPosition > position + 10) {
        sendEvent(name: "target", value: targetPosition, isStateChange: true)
        close()
    } else if(targetPosition < position - 10) {
        sendEvent(name: "target", value: targetPosition, isStateChange: true)
        open()
    }
    // If the parent wants to do something, it can do so now...
    parent?.componentSetPosition(this.device, position)
    
}

void setLevel(BigDecimal level) {
    setPosition(level)
}

void setLevel(BigDecimal level, BigDecimal duration) {
    setPosition(level)
}

/**
 * -----------------------------------------------------------------------------
 * Everything below here are LIBRARY includes and should NOT be edited manually!
 * -----------------------------------------------------------------------------
 * --- Nothing to edit here, move along! ---------------------------------------
 * -----------------------------------------------------------------------------
 */

#!include:getDefaultFunctions()

#!include:getHelperFunctions('all-default')

#!include:getHelperFunctions('driver-metadata')

#!include:getHelperFunctions('styling')

#!include:getLoggingFunction(specialDebugLevel=True)
