#!include:getHeaderLicense()

#!include:getDefaultImports()

metadata {
    // Do NOT rename the child driver name unless you also change the corresponding code in the Parent!
    definition (name: "Tasmota - Universal CT/RGB/RGB+CW+WW (Child)", namespace: "tasmota", author: "Markus Liljergren") {
        capability "Actuator"
        capability "Switch"                       // Attributes: switch - ENUM ["on", "off"]
        capability "Light"                        // Attributes: switch - ENUM ["on", "off"]
		capability "SwitchLevel"                  // Attributes: level - NUMBER
        capability "ChangeLevel"
        capability "ColorControl"                 // Attributes: RGB - STRING, color - STRING, colorName - STRING, hue - NUMBER, saturation - NUMBER
        capability "ColorTemperature"             // Attributes: colorName - STRING, colorTemperature - NUMBER
        capability "ColorMode"                    // Attributes: colorMode - ENUM ["CT", "RGB"]
        capability "Refresh"
        capability "LightEffects"                 // Attributes: effectName - STRING, lightEffects - JSON_OBJECT

        #!include:getMinimumChildAttributes()

        //lightEffects = [1: "Effect Name", 2: "Other effect", 3: "etc..."] to JSON...
        attribute  "effectNumber", "number"
        
        // For Addressable LEDs, we need to add settings for
        // https://tasmota.github.io/docs/#/Commands?id=light
        // Pixels
        // Rotation
        // Led
        // Width - This one could be complicated to get understandable...
        
        command "setPixelColor", [[name:"RGB*", type: "STRING", description: "RGB in HEX, eg: #FF0000"],
            [name:"Pixel Number*", type: "NUMBER", description: "Pixel to change the color of (1 to \"Addressable Pixels\")"]]
        command "setAddressableRotation", [[name:"Addressable Rotation*", type: "NUMBER", description: "1..512 = set amount of pixels to rotate (up to Addressable Pixels value)"]]
        command "setEffectWidth", [[name:"Addressable Effect Width*", type: "ENUM", description: "This width is used by Addressable pixel effects",
                                    constraints: ["0", "1", "2", "3", "4"]]]

        command "setColorByName", [[name:"Color Name*", type: "ENUM", description: "Choose a color",
                                    constraints: ["#FF0000":"Red",
                                                  "#00FF00":"Green",
                                                  "#0000FF":"Blue",
                                                  "#FFFF00":"Yellow",
                                                  "#00FFFF":"Cyan",
                                                  "#FF00FF":"Pink",
                                                  "#FFFFFFFFFF":"White"]]]

        //include:getMetadataCommandsForHandlingRGBWDevices()
        #!include:getMetadataCommandsForHandlingTasmotaRGBWDevices()
        #!include:getMetadataCommandsForHandlingTasmotaDimmerDevices()
    }

    preferences {
        #!include:getDefaultMetadataPreferences()
        input(name: "hideColorTemperatureCommands", type: "bool", title: addTitleDiv("Hide Color Temperature Commands"), defaultValue: false, displayDuringSetup: false, required: false)
        input(name: "hideEffectCommands", type: "bool", title: addTitleDiv("Hide Effect Commands"), defaultValue: true, displayDuringSetup: false, required: false)
        input(name: "hideColorCommands", type: "bool", title: addTitleDiv("Hide Color Commands"), defaultValue: false, displayDuringSetup: false, required: false)
        input(name: "isAddressable", type: "bool", title: addTitleDiv("Addressable Light"), description: addDescriptionDiv("Treat as an Addressable Light"), defaultValue: false, displayDuringSetup: false, required: false)
        input(name: "addressablePixels", type: "number", title: addTitleDiv("Addressable Pixels"), description: addDescriptionDiv("1..512 = set amount of pixels in strip or ring and reset Rotation"), displayDuringSetup: false, required: false, defaultValue: 30)
        //input(name: "addressableRotation", type: "number", title: addTitleDiv("Addressable Rotation"), description: addDescriptionDiv("1..512 = set amount of pixels to rotate (up to Addressable Pixels value)"), displayDuringSetup: false, required: false, defaultValue: 30)
    }

    // The below line needs to exist in ALL drivers for custom CSS to work!
    #!include:getMetadataCustomizationMethods()
}

#!include:getDeviceInfoFunction()

/* These functions are unique to each driver */
void parse(List<Map> description) {
    //logging("Child: parse()", 100)
    description.each {
        // TODO: Make sure the parent sends RGB and color! Or do we even need these?
        if (it.name in ["switch", "level", "RGB", "color", "colorName", "hue", "saturation",
                        "colorTemperature", "colorMode"]) {
            logging(it.descriptionText, 100)
            sendEvent(it)
        } else if(it.name == "effectNumber") {
            logging(it.descriptionText, 100)
            sendEvent(name: "effectName", value: getLightEffectNameByNumber(it.value), isStateChange: false)
            sendEvent(it)
        } else {
            log.warn "Got '$it.name' attribute data, but doesn't know what to do with it! Did you choose the right device type?"
        }
    }
    //logging("Child: END parse()", 100)
}

void installed() {
    log.info "installed()"
    device.removeSetting("logLevel")
    device.updateSetting("logLevel", "100")
    sendEvent(name: "colorMode", value: "CT")
    sendEvent(name: "colorTemperature", value: "3000")
    sendEvent(name: "hue", value: "0")
    sendEvent(name: "saturation", value: "0")
    sendEvent(name: "level", value: "100")
    sendEvent(name: "colorName", value: "Daylight")
    refresh()
}

void updated() {
    log.info "updated()"
    if(addressablePixels != null && addressablePixels != state.addressablePixels) {
        setAddressablePixels(addressablePixels.toInteger())
        state.addressablePixels = addressablePixels
    }
    #!include:getChildComponentDefaultUpdatedContent()
    //if(addressableRotation != null) setAddressableRotation(addressableRotation.toInteger())
    refresh()
}

void refresh() {
    #!include:getChildComponentMetaConfigCommands()

    metaConfig = setStateVariablesToHide(["mode", "effectnumber"], metaConfig=metaConfig)

    List commandsToHide = ["setEffect", "setNextEffect", "setPreviousEffect"]
    if(hideColorTemperatureCommands == true) {
        commandsToHide.addAll(["setColorTemperature"])
    }
    if(hideEffectCommands == null || hideEffectCommands == true) {
        commandsToHide.addAll(["setEffectWithSpeed", "setNextEffectWithSpeed", "setPreviousEffectWithSpeed", "modeWakeUp", "setEffectSingleColor", "setEffectCycleUpColors", "setEffectCycleDownColors", "setEffectRandomColors"])
    }
    if(hideColorCommands == null || hideColorCommands == true) {
        //"colorWhite", "colorRed", "colorGreen", "colorBlue", "colorYellow", "colorCyan", "colorPink"
        commandsToHide.addAll(["setColor", "setColorByName", "setHue", "setSaturation"])
    }
    
    Map lightEffects = [:]
    if(isAddressable == true) {
        lightEffects = [0: "Single Color", 1: "Wake Up", 2: "Cycle Up Colors", 3: "Cycle Down Colors", 
                        4: "Random Colors", 5: "Clock Mode", 6: "Candlelight Pattern", 7: "RGB Pattern",
                        8: "Christmas Pattern", 9: "Hanukkah Pattern", 10: "Kwanzaa Pattern",
                        11: "Rainbow Pattern", 12: "Fire Pattern"]
    } else {
        metaConfig = setStateVariablesToHide(["addressablePixels"], metaConfig=metaConfig)
        commandsToHide.addAll(["addressablePixel", "setEffectWidth", "setPixelColor", "setAddressableRotation"])
        metaConfig = setPreferencesToHide(["addressablePixels"], metaConfig=metaConfig)
        lightEffects = [0: "Single Color", 1: "Wake Up", 2: "Cycle Up Colors", 3: "Cycle Down Colors", 
                        4: "Random Colors"]
    }
    if(commandsToHide != []) metaConfig = setCommandsToHide(commandsToHide, metaConfig=metaConfig)

    sendEvent(name: "lightEffects", value: JsonOutput.toJson(lightEffects))
    parent?.componentRefresh(this.device)
}

void on() {
    parent?.componentOn(this.device)
}

void off() {
    parent?.componentOff(this.device)
}

void setLevel(BigDecimal level) {
    parent?.componentSetLevel(this.device, level)
}

void setLevel(BigDecimal level, BigDecimal duration) {
    parent?.componentSetLevel(this.device, level, duration)
}

void startLevelChange(String direction) {
    parent?.componentStartLevelChange(this.device, direction)
}

void stopLevelChange() {
    parent?.componentStopLevelChange(this.device)
}

void setColor(Map colormap) {
    parent?.componentSetColor(this.device, colormap)
}

void setHue(BigDecimal hue) {
    parent?.componentSetHue(this.device, hue)
}

void setSaturation(BigDecimal saturation) {
    parent?.componentSetSaturation(this.device, saturation)
}

void setColorTemperature(BigDecimal colortemperature) {
    parent?.componentSetColorTemperature(this.device, colortemperature)
}

void colorWhite() {
    parent?.componentWhite(this.device)
}

void colorRed() {
    parent?.componentSetRGB(this.device, 255, 0, 0)
}

void colorGreen() {
    parent?.componentSetRGB(this.device, 0, 255, 0)
}

void colorBlue() {
    parent?.componentSetRGB(this.device, 0, 0, 255)
}

void colorYellow() {
    parent?.componentSetRGB(this.device, 255, 255, 0)
}

void colorCyan() {
    parent?.componentSetRGB(this.device, 0, 255, 255)
}

void colorPink() {
    parent?.componentSetRGB(this.device, 255, 0, 255)
}

void setColorByName(String colorName) {
    logging("setColorByName(colorName ${colorName})", 1)
    String colorRGB = ""
    switch(colorName) {
        case "Red":
            colorRGB = "#FF0000"
            break
        case "Green":
            colorRGB = "#00FF00"
            break
        case "Blue":
            colorRGB = "#0000FF"
            break
        case "Yellow":
            colorRGB = "#FFFF00"
            break
        case "Cyan":
            colorRGB = "#00FFFF"
            break
        case "Pink":
            colorRGB = "#FF00FF"
            break
        default:
            colorRGB = "#FFFFFFFFFF"
    }
    setColorByRGBString(colorRGB)
}

void setColorByRGBString(String colorRGB) {
    logging("setColorByRGBString(colorRGB ${colorRGB})", 1)
    parent?.componentSetColorByRGBString(this.device, colorRGB)
}

void setEffect(BigDecimal effectnumber) {
    setEffectWithSpeed(effectnumber, 2)
}

Map getLightEffects() {
    String lightEffectsJSON = device.currentValue('lightEffects')
    Map lightEffects = [0: "Undefined"]
    if(lightEffectsJSON != null) {
        log.debug "lightEffectsJSON = $lightEffectsJSON"
        JsonSlurper jsonSlurper = new JsonSlurper()
        lightEffects = jsonSlurper.parseText(lightEffectsJSON)
    }
    return lightEffects
}

String getLightEffectNameByNumber(BigDecimal effectnumber) {
    Map lightEffects = getLightEffects()
    lightEffects.get(effectnumber.toString(), 'Unknown')
}

void setNextEffect() {
    setNextEffectWithSpeed(2)
}

void setPreviousEffect() {
    setPreviousEffectWithSpeed(2)
}

void setEffectWithSpeed(BigDecimal effectnumber, BigDecimal speed=3) {
    state.effectnumber = effectnumber
    parent?.componentSetEffect(this.device, effectnumber, speed)
}

void setNextEffectWithSpeed(BigDecimal speed=3) {
    logging("setNextEffectWithSpeed()", 10)
    if (state.effectnumber != null && state.effectnumber < getLightEffects().size() - 1) {
        state.effectnumber = state.effectnumber + 1
    } else {
        state.effectnumber = 0
    }
    setEffectWithSpeed(state.effectnumber, speed)
}

void setPreviousEffectWithSpeed(BigDecimal speed=3) {
    logging("setPreviousEffectWithSpeed()", 10)
    if (state.effectnumber != null && state.effectnumber > 0) {
        state.effectnumber = state.effectnumber - 1
    } else {
        state.effectnumber = getLightEffects().size() - 1
    }
    setEffectWithSpeed(state.effectnumber, speed)
}

void setEffectSingleColor(BigDecimal speed=3) {
    setEffectWithSpeed(0, speed)
}

void setEffectCycleUpColors(BigDecimal speed=3) {
    setEffectWithSpeed(2, speed)
}

void setEffectCycleDownColors(BigDecimal speed=3) {
    setEffectWithSpeed(3, speed)
}

void setEffectRandomColors(BigDecimal speed=3) {
    setEffectWithSpeed(4, speed)
}

void modeWakeUp(BigDecimal wakeUpDuration) {
    Integer level = device.currentValue('level')
    Integer nlevel = level > 10 ? level : 10
    modeWakeUp(wakeUpDuration, nlevel)
}

void modeWakeUp(BigDecimal wakeUpDuration, BigDecimal level) {
    state.effectnumber = 1
    parent?.componentModeWakeUp(this.device, wakeUpDuration, level)
}

void setPixelColor(String colorRGB, BigDecimal pixel) {
    parent?.componentSetPixelColor(this.device, colorRGB, pixel)
}

void setAddressablePixels(BigDecimal pixels) {
    parent?.componentSetAddressablePixels(this.device, pixels)
}

void setAddressableRotation(BigDecimal pixels) {
    parent?.componentSetAddressableRotation(this.device, pixels)
}

void setEffectWidth(String pixels) {
    setEffectWidth(pixels.toInteger())
}

void setEffectWidth(BigDecimal pixels) {
    parent?.componentSetEffectWidth(this.device, pixels)
}

/*
Fade between colours:
rule3 on Rules#Timer=1 do backlog color1 #ff0000; ruletimer2 10; endon on Rules#Timer=2 do backlog color1 #0000ff; ruletimer3 10; endon on Rules#Timer=3 do backlog color1 #00ff00; ruletimer1 10; endon
backlog rule3 1; color1 #ff0000; fade 1; speed 18; color1 #0000ff; ruletimer3 10;

Fade up to color:
backlog fade 0; dimmer 0; color2 #ff0000; fade 1; speed 20; dimmer 100;

Fade down from color:
backlog fade 0; dimmer 100; color2 #ff0000; fade 1; speed 20; dimmer 0;

Fade up and down:

rule3 on Rules#Timer=1 do backlog color2 #ff0000; dimmer 100; ruletimer2 10; endon on Rules#Timer=2 do backlog dimmer 0; ruletimer3 10; endon  on Rules#Timer=3 do backlog color2 #00ff00; dimmer 100; ruletimer4 10; endon on Rules#Timer=4 do backlog dimmer 0; ruletimer1 10; endon 
backlog rule3 1; fade 0; dimmer 0; color2 #ff0000; fade 1; speed 20; dimmer 100; ruletimer2 10;

To disable a running effect:
rule3 0
backlog

*/

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
