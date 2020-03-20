#!include:getHeaderLicense()

#!include:getDefaultImports()

metadata {
    // Do NOT rename the child driver name unless you also change the corresponding code in the Parent!
    definition (name: "Tasmota - Universal Fan Control (Child)", namespace: "tasmota", author: "Markus Liljergren") {
        capability "FanControl"
        capability "Refresh"

        #!include:getMinimumChildAttributes()
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
        if (it.name in ["speed"]) {
            logging(it.descriptionText, 100)
            sendEvent(it)
        } else if (it.name in ["level"]) {
            it.name = "speed"
            it.descriptionText = "'speed' set to '$it.value'"
            logging(it.descriptionText, 100)
            sendEvent(it)
        } else if (it.name in ["switch"]) {
            // ignore
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

void setSpeed(String value) {
    parent?.componentSetSpeed(this.device, value)
    
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
