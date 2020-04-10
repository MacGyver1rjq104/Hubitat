/**
 * TASMOTA RGBW METHODS (helpers-tasmota-rgbw)
 *
 * Helper functions included in all Tasmota drivers using RGB, RGBW or Dimmers
 * These methods ARE specific to Tasmota
 */
void setColorTemperature(value) {
    logging("setColorTemperature('${value}')", 10)
    if(device.currentValue('colorTemperature') != value ) sendEvent(name: "colorTemperature", value: value)
    // 153..500 = set color temperature from 153 (cold) to 500 (warm) for CT lights
    // Tasmota use mired to measure color temperature
    Integer t = value != null ?  (value as Integer) : 0
    // First make sure we have a Kelvin value we can more or less handle
    // 153 mired is approx. 6536K
    // 500 mired = 2000K
    if(t > 6536) t = 6536
    if(t < 2000) t = 2000
    t = Math.round(1000000/t)
    if(t < 153) t = 153
    if(t > 500) t = 500
    state.mired = t
    state.hue = 0
    state.saturation = 0
    state.colorMode = "CT"
    //if(device.currentValue("colorMode") != "CT" ) sendEvent(name: "colorMode", value: "CT")
    logging("setColorTemperature('${t}') ADJUSTED to Mired", 10)
    getAction(getCommandStringWithModeReset("CT", "${t}"))
}

void setHSB(h, s, b) {
    logging("setHSB('${h}','${s}','${b}')", 10)
    setHSB(h, s, b, true)
}

void setHSB(h, s, b, callWhite) {
    logging("setHSB('${h}','${s}','${b}', callWhite=${String.valueOf(callWhite)})", 10)
    boolean adjusted = False
    if(h == null || h == 'NaN') {
        h = state != null && state.containsKey("hue") ? state.hue : 0
        adjusted = True
    }
    if(s == null || s == 'NaN') {
        s = state != null && state.containsKey("saturation") ? state.saturation : 0
        adjusted = True
    }
    if(b == null || b == 'NaN') {
        b = state != null && state.containsKey("level") ? state.level : 0
        adjusted = True
    }
    if(adjusted) {
        logging("ADJUSTED setHSB('${h}','${s}','${b}'", 1)
    }
    Integer adjustedH = Math.round(h*3.6)
    if( adjustedH > 360 ) { adjustedH = 360 }
    if( b < 0 ) b = 0
    if( b > 100 ) b = 100
    String hsbcmd = "${adjustedH},${s},${b}"
    logging("hsbcmd = ${hsbcmd}", 1)
    state.hue = h
    state.saturation = s
    state.level = b
    state.colorMode = "RGB"
    if (hsbcmd.startsWith("0,0,")) {
        //state.colorMode = "white"
        //if(device.currentValue("colorMode") != "CT" ) sendEvent(name: "colorMode", value: "CT")
        white()
        //getAction(getCommandString("hsbcolor", hsbcmd))
    } else {
        //if(device.currentValue("colorMode") != "RGB" ) sendEvent(name: "colorMode", value: "RGB")
        getAction(getCommandStringWithModeReset("HsbColor", hsbcmd))
    }
}

void setRGB(r, g, b) {   
    logging("setRGB('${r}','${g}','${b}')", 10)
    boolean adjusted = False
    if(r == null || r == 'NaN') {
        r = 0
        adjusted = True
    }
    if(g == null || g == 'NaN') {
        g = 0
        adjusted = True
    }
    if(b == null || b == 'NaN') {
        b = 0
        adjusted = True
    }
    if(adjusted) {
        logging("ADJUSTED setRGB('${r}','${g}','${b}')", 1)
    }
    String rgbcmd = "${r},${g},${b}"
    logging("rgbcmd = ${rgbcmd}", 1)
    state.red = r
    state.green = g
    state.blue = b
    // Calculate from RGB values
    def hsbColor = rgbToHSB(r, g, b)
    logging("hsbColor from RGB: ${hsbColor}", 1)
    state.colorMode = "RGB"
    //if(device.currentValue("colorMode") != "RGB" ) sendEvent(name: "colorMode", value: "RGB")
    //if (hsbcmd == "${hsbColor[0]},${hsbColor[1]},${hsbColor[2]}") state.colorMode = "white"
    state.hue = hsbColor['hue']
    state.saturation = hsbColor['saturation']
    state.level = hsbColor['level']
    getAction(getCommandStringWithModeReset("Color1", rgbcmd))
}

void setLevel(l, duration) {
    if (duration == 0) {
        if (false && state.colorMode == "RGB") {
            setHSB(null, null, l)
        } else {
            state.level = l
            getAction(getCommandString("Dimmer", "${l}"))
        }
    } else if (duration > 0) {
        if (false && state.colorMode == "RGB") {
            setHSB(null, null, l)
        } else {
            if (duration > 5400) {
                log.warn "Maximum supported dimming duration is 5400 seconds due to current implementation method used."
                duration = 5400 // Maximum duration is 1.5 hours
            } 
            Integer cLevel = state.level
            
            Integer levelDistance = l - cLevel
            Integer direction = 1
            if(levelDistance < 0) {
                direction = -1
                levelDistance = levelDistance * -1
            }
            Integer steps = 13
            Integer increment = Math.round(((levelDistance as Float)  / steps) as Float)
            if(increment <= 1 && Math.abs(levelDistance) < steps) {
                steps = Math.abs(levelDistance)
            }
            List fadeCommands = []
            fadeCommands.add([command: "Fade", value: "1"])
            fadeCommands.add([command: "Speed", value: "20"])
            if(steps > 0) {
                // If we have less than 1 step, we shouldn't execute any of the below...

                // Each Backlog command has 200ms delay, deduct that delay and add 1 second extra
                duration = ((duration as Float) - (2 * steps * 0.2) + 1) as Float
                BigDecimal stepTime = round2((duration / steps) as Float, 1)
                Integer stepTimeTasmota = Math.round((stepTime as Float) * 10)
                BigDecimal lastStepTime = round2((stepTime + (duration - (stepTime * steps)) as Float), 1)
                Integer lastStepTimeTasmota = Math.round((lastStepTime as Float) * 10)
                
                Integer cmdLevel = cLevel
                
                (1..steps).each{
                    cmdLevel += (increment * direction)
                    if(direction == 1 && (cmdLevel > l || it == steps)) cmdLevel = l
                    if(direction == -1 && (cmdLevel < l || it == steps)) cmdLevel = l
                    if(it != steps) {
                        fadeCommands.add([command: "Delay", value: "$stepTimeTasmota"])
                    } else {
                        fadeCommands.add([command: "Delay", value: "$lastStepTimeTasmota"])
                    }
                    fadeCommands.add([command: "Dimmer", value: "$cmdLevel"])
                }
            } else {
                fadeCommands.add([command: "Dimmer", value: "$l"])
            }
            fadeCommands.add([command: "Fade", value: "0"])
            //fadeCommands = "Fade 1;Speed ${speed};Dimmer ${l};Delay ${duration};Fade 0"
            logging("fadeCommands: '" + fadeCommands + "', cmdData=${[cLevel:cLevel, levelDistance:levelDistance, direction:direction, steps:steps, increment:increment, stepTime:stepTime, lastStepTime:lastStepTime]}", 1)
            getAction(getMultiCommandString(fadeCommands))
        }
   }
}

void stopLevelChange() {
    // Since sending a backlog command without arguments will cancel any current level change we have, 
    // so that is what we do...
    getAction(getCommandString("Fade", "0"))
    getAction(getCommandString("Backlog", null))
    modeSingleColor(1)
}

void startLevelChange(String direction) {
    Integer cLevel = state.level
    Integer delay = 30
    modeSingleColor(1)
    if(direction == "up") {
        if(cLevel != null) {
            delay = Math.round(((delay / 100) * (100-cLevel)) as Float)
        }
        setLevel(100, delay)
    } else {
        if(cLevel != null) {
            delay = Math.round(((delay / 100) * (cLevel)) as Float)
        }
        setLevel(0, delay)
    }
}

void whiteForPlatform() {
    logging("whiteForPlatform()", 10)
    Integer l = state.level
    //state.colorMode = "white"
    if (l < 10) l = 10
    l = Math.round(l * 2.55).toInteger()
    if (l > 255) l = 255
    def lHex = l.toHexString(l)
    String hexCmd = "#${lHex}${lHex}${lHex}${lHex}${lHex}"
    logging("hexCmd='${hexCmd}'", 1)
    state.hue = 0
    state.saturation = 0
    state.red = l
    state.green = l
    state.blue = l
    state.colorMode = "CT"
    //if(device.currentValue("colorMode") != "CT" ) sendEvent(name: "colorMode", value: "CT")
    getAction(getCommandStringWithModeReset("Color1", hexCmd))
}

// Functions to set RGBW Mode
void modeSet(Integer mode, BigDecimal speed=3) {
    logging("modeSet('${mode}')", 10)
    getAction(getMultiCommandString([[command:"Speed", value:"$speed"], [command:"Scheme", value:"${mode}"]]))
}

void modeNext(BigDecimal speed=3) {
    logging("modeNext()", 10)
    if (state.mode < 4) {
        state.mode = state.mode + 1
    } else {
        state.mode = 0
    }
    modeSet(state.mode, speed)
}

void modePrevious(BigDecimal speed=3) {
    if (state.mode > 0) {
        state.mode = state.mode - 1
    } else {
        state.mode = 4
    }
    modeSet(state.mode, speed)
}

void modeSingleColor(BigDecimal speed=3) {
    state.mode = 0
    modeSet(state.mode, speed)
}

void modeCycleUpColors(BigDecimal speed=3) {
    state.mode = 2
    modeSet(state.mode, speed)
}

void modeCycleDownColors(BigDecimal speed=3) {
    state.mode = 3
    modeSet(state.mode, speed)
}

void modeRandomColors(BigDecimal speed=3) {
    state.mode = 4
    modeSet(state.mode, speed)
}

void modeWakeUp(BigDecimal wakeUpDuration) {
    Integer level = device.currentValue('level')
    Integer nlevel = level > 10 ? level : 10
    logging("modeWakeUp(wakeUpDuration ${wakeUpDuration}, current level: ${nlevel})", 1)
    modeWakeUp(wakeUpDuration, nlevel)
}

void modeWakeUp(BigDecimal wakeUpDuration, BigDecimal level) {
    logging("modeWakeUp(wakeUpDuration ${wakeUpDuration}, level: ${level})", 1)
    state.mode = 1
    wakeUpDuration = wakeUpDuration < 1 ? 1 : wakeUpDuration > 3000 ? 3000 : wakeUpDuration
    level = level < 1 ? 1 : level > 100 ? 100 : level
    state.level = level
    getAction(getMultiCommandString([[command: "WakeupDuration", value: "${wakeUpDuration}"],
                                    [command: "Wakeup", value: "${level}"]]))
}

void setColorByRGBString(String colorRGB) {
    logging("setColorByRGBString(colorRGB ${colorRGB})", 100)
    getAction(getCommandStringWithModeReset("Color1", colorRGB.take(11)))
}

void setPixelColor(String colorRGB, BigDecimal pixel) {
    logging("setPixelColor(colorRGB ${colorRGB}, pixel: ${pixel})", 1)
    if(pixel < 1) pixel = 1
    if(pixel > 512) pixel = 512
    getAction(getCommandStringWithModeReset("Led$pixel", colorRGB.take(7)))
}

void setAddressablePixels(BigDecimal pixels) {
    logging("setAddressablePixels(pixels: ${pixels})", 100)
    if(pixels < 1) pixels = 1
    if(pixels > 512) pixels = 512
    getAction(getCommandString("Pixels", "$pixels"))
}

void setAddressableRotation(BigDecimal pixels) {
    logging("setAddressableRotation(pixels: ${pixels})", 100)
    if(pixels < 1) pixels = 1
    if(pixels > 512) pixels = 512
    getAction(getCommandString("Rotation", "$pixels"))
}

void setEffectWidth(BigDecimal pixels) {
    logging("setEffectWidth(pixels: ${pixels})", 100)
    if(pixels < 0) pixels = 0
    if(pixels > 4) pixels = 4
    getAction(getCommandString("Width1", "$pixels"))
}

String getCommandStringWithModeReset(String command, String value) {
    if(useAlternateColorCommand == true && command == "Color1") command = "Var1"
    return getMultiCommandString([[command: "Scheme", value: "0"], [command: "Fade", value: "0"], 
                                  [command: command, value: value]])
}

/**
 * --END-- TASMOTA RGBW METHODS (helpers-tasmota-rgbw)
 */