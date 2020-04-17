/**
 * APP TASMOTA DEVICE DISCOVERY METHODS (helpers-app-tasmota-device-discovery)
 *
 * Methods used in all APPS
 */
def discoveryPage() {
   return deviceDiscovery()
}

def deviceDiscoveryCancel() {
    logging("deviceDiscoveryCancel()", 100)
    unsubscribe()
    unschedule()
    state.deviceRefreshCount = 0
    state.devices = state.devicesCached ?: [:]
    state.devices.each {
        it.value["installed"] = null
    }
    state.devicesCached.each {
        it.value["installed"] = null
    }
}

def deviceDiscovery(params=[:]) {
    Integer deviceRefreshCount = !state.deviceRefreshCount ? 0 : state.deviceRefreshCount as Integer
    state.deviceRefreshCount = deviceRefreshCount + 1

    if(deviceRefreshCount == 0) {
	    ssdpSubscribe()
        runEvery1Minute("ssdpDiscover")
        // This is our failsafe since we REALLY don't want this to be left on...
        runIn(1800, "deviceDiscoveryCancel")
        verifyDevices()
        state.deviceRefreshStart = now()
    }

	//def devices = devicesDiscovered()
    
	def refreshInterval = 10
    
	//def options = devices ?: [:]
	//def numFound = options.size() ?: 0
    
	//if ((numFound == 0 && state.deviceRefreshCount > 25) || params.reset == "true") {
    //	log.trace "Cleaning old device memory"
    //	state.devices = [:]
    //    state.deviceRefreshCount = 0
    //    app.updateSetting("selectedDevice", "")
    //}

	//Tasmota-based Device discovery request every 15 //25 seconds
	//if((deviceRefreshCount % 5) == 0) {
	//	discoverDevices()
	//}

	//XML request and Install check every 30 seconds
	if((deviceRefreshCount % 3) == 0) {
		verifyDevices()
	}
    
	return dynamicPage(name:"deviceDiscovery", title:"", nextPage:"deviceDiscoveryPage2", refreshInterval:refreshInterval) {
    //return dynamicPage(name:"deviceDiscovery", title:"", nextPage:"deviceDiscoveryPage2") {
        makeAppTitle() // Also contains the CSS
		section(getElementStyle('header', getMaterialIcon('', 'he-discovery_1') + "Discover a Tasmota Device"), hideable: true, hidden: false) {
            paragraph("Please wait while we discover your Tasmota-based Devices using SSDP. Discovery can take five minutes or more, so sit back and relax!")
            
			paragraph("<span style=\"font-weight: 500\">Time elapsed since starting SSDP Discovery:</ span> ${new BigDecimal((now() - Long.valueOf(state.deviceRefreshStart))/1000).setScale(0, BigDecimal.ROUND_HALF_UP)} seconds")
            
            paragraph("Please note that Hue Bridge Emulation (Configuration->Configure Other->Emulation) must be turned on in Tasmota for discovery to work (this is the default with the Hubitat version of Tasmota).")

            paragraph("Installed devices are not displayed (if Tasmota Device Manager has access to them). Previously discovered devices will show quickly, devices never seen by Tasmota Device Manager before may take time to discover.")
            getAvailableDevicesList()
            paragraph("Once the device you want to install is available in the above list, click \"Next\" to go to the Installation Page.")
            paragraph("If the device you expect to find is not found within 10 minutes, use the Manual Install method instead.")
		}
        section(getElementStyle('header', getMaterialIcon('dns') + "Actions"), hideable: true, hidden: false){ 
            href("deviceDiscoveryReset", title:"Reset list of Discovered Devices", description:"")
            href("mainPage", title:"Return to the Main Page", description:"")
			//href "deviceDiscovery", title:"Reset list of discovered devices", description:"", params: ["reset": "true"]
            
		}
	}
}

String getAvailableDevicesList() {
    
    
    def vdevices = getVerifiedDevices()
	def options = [:]
	vdevices.each {
        //log.debug("Device: $it.value")
		def value = "${it.value.name}"
		def key = "${it.value.networkAddress}"
		options["${key}"] = value
	}
    String title = "Available Devices (${options.size() ?: 0} found)"
    //List optionsList = []
    //options.each{optionsList << [key: it.key, value: it.value]}
    String deviceList = ""
    options.sort({ a, b -> a.key <=> b.key }).each{
        deviceList += it.value + "<br>"
    }

    String header = """<style>
        div.btn-listing,
        div.btn-listing:hover,
        div.btn-listing:active,
        div.btn-listing:focus,
        div.btn-listing:focus:not(:active),
        div.btn-listing:active:focus,
        div.btn-listing:active:hover {
            background: rgba(158,158,158,.2);
            background-color: rgba(158,158,158,.2);
            border-color: rgb(227, 227, 227);
            cursor: unset;
            touch-action: none;
            border-left: none;
            text-align:left;
            color:#212121;
            border-left-color: rgb(227, 227, 227);
            font-weight: 400;
            box-sizing: border-box;
            user-select: none;
            border-left-style: outset;
            border-left-width: 2px;
        }
        </style><div class="btn-listing btn btn-default btn-lg btn-block device-btn-filled  btn-device  mdl-shadow--2dp">
            <span style="">""" + title + """</span><br>
            <span id="devicesSelecteddevlist" class="device-text" style="text-align: left;">"""
    String footer = "</span></div>"
    //log.debug("devices2: ${devices} , options: ${options}")
    return paragraph(header + deviceList + footer, submitOnChange: false)
}

def deviceDiscoveryPage2() {
    Integer deviceRefreshCount = !state.deviceRefreshCount ? 0 : state.deviceRefreshCount as Integer

    def devices = devicesDiscovered()
    
	def options = devices ?: [:]
	def numFound = options.size() ?: 0
    
	//return dynamicPage(name:"deviceDiscovery", title:"", nextPage:"discoveredAddConfirm", refreshInterval:refreshInterval) {
    return dynamicPage(name:"deviceDiscoveryPage2", title:"", nextPage:"discoveredAddConfirm") {
        makeAppTitle() // Also contains the CSS
		section(getElementStyle('header', getMaterialIcon('', 'he-discovery_1') + "Discover a Tasmota Device"), hideable: true, hidden: false) {
            paragraph("Installed devices are not displayed (if Tasmota Device Manager has access to them). Previously discovered devices will show quickly, devices never seen by Tasmota Device Manager before may take time to discover.")
            input("deviceType", "enum", title:"Device Type", description: "", required: true, submitOnChange: true, options: 
                #!include:makeTasmotaConnectDriverListV1()
            )
            input(name: "deviceConfig", type: "enum", title: "Device Configuration", 
                description: "Select a Device Configuration (default: Generic Device)<br/>'Generic Device' doesn't configure device Template and/or Module on Tasmota. Child devices and types are auto-detected as well as auto-created and does NOT depend on this setting.", 
                options: getDeviceConfigurationsAsListOption(), defaultValue: "01generic-device", required: false, submitOnChange: true)
            input("selectedDiscoveredDevice", "enum", required:false, title:"Select a Tasmota Device (${numFound} found)", multiple:false, options:options, submitOnChange: true)
            //input("ipAddress", "text", title:"IP Address", description: "", required: true, submitOnChange: false)
            input("deviceLabel", "text", title:"Device Label", description: "", required: true, defaultValue: (deviceType ? deviceType : "Tasmota - Universal Parent") + " (%device_ip%)", submitOnChange: true)
            paragraph("'%device_ip%' = insert device IP here")
            paragraph("Suffixes \" (Parent)\" and \" Parent\" at the end of the Device Label will be removed from the Child Device Label.")
            input("passwordDevice", "password", title:"Tasmota Device Password", description: "Only needed if set in Tasmota.", defaultValue: passwordDefault, submitOnChange: true, displayDuringSetup: true)            
            paragraph("Only needed if set in Tasmota.")
            paragraph("<br/>To exit without installing a device, complete the required fields and DON'T select a device, then click \"Next\".")
		}
	}
}

def discoveredAddConfirm() {
    def devices = getDevices()
    def selectedDevice = devices.find { it.value.mac == selectedDiscoveredDevice }
    def ipAddress = convertHexToIP(selectedDevice?.value?.networkAddress)
    //log.debug("Discovered IP: $ipAddress")
    if ( ipAddress != null && ipAddress =~ /^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$/) {
        logging("Creating Tasmota-based Wifi Device with dni: ${convertIPtoHex(ipAddress)}", 1)
        if(passwordDevice == null || passwordDevice == "") {
           passwordDevice = "[installed]"
        }
        def child = addChildDevice("tasmota", deviceType ? deviceType : "Tasmota - Universal Parent", "${convertIPtoHex(ipAddress)}", location.hubs[0].id, [
           "label": (deviceLabel ? deviceLabel : "Tasmota - Universal Parent (%device_ip%)").replace("%device_ip%", "${ipAddress}"),
           "data": [
                "ip": ipAddress,
                "port": "80",
                "password": encrypt(passwordDevice),
                "deviceConfig": deviceConfig
           ]
        ])

        // We do this to get everything setup correctly
        //child.updateSetting("deviceConfig", [type: "enum", value:deviceConfig])
        // After adding preferences, Configure() needs to run to apply them, but we have to wait a little bit.
        child.configureDelayed()
        // This will refresh and detect child devices based on the above config
        child.refresh()

        // Restore for next time
        app.updateSetting("ipAddress", [type: "string", value:getFirstTwoIPBytes(ipAddress)])
        //app.updateSetting("deviceLabel", "")
        app.updateSetting("passwordDevice", "")
        //app.updateSetting("deviceConfig", [type: "enum", value:"01generic-device"])
        
        resultPage("discoveredAddConfirm", "Discovered Tasmota-based Device", 
                   'The device has been added. To add another device click "Add Next Device". Click "Done" to return to the Main Page.<br/>It may take up to a minute or so before all child devices have been created if many are needed. Be patient. If all child devices are not created as expected, press Configure and Refresh in the Universal Parent and wait again. Don\'t click multiple times, it takes time for the device to reconfigure itself.', 
                   nextPage="mainPage", otherReturnPage="deviceDiscovery", otherReturnPageTitle="Add Next Device")
    } else {
        resultPageFailed("discoveredAddConfirm", "Discovered Tasmota-based Device", "No device was selected. To add another device click \"Add Another Device\". Click \"Done\" to return to the Main page.", 
                   nextPage="mainPage", otherReturnPage="deviceDiscovery", otherReturnPageTitle="Add Another Device")
    }
}

Map deviceDiscoveryReset() {
    logging("deviceDiscoveryReset()", 1)
    resetDeviceDiscovery()
    return resultPage("deviceDiscoveryReset", "Device Discovery Reset", "Device Discovery Reset Done!", nextPage="deviceDiscovery")
}

void resetDeviceDiscovery(){
    logging("Cleaning old device from the list...", 100)
    //log.debug("resetDeviceDiscovery()")
    state.devices = state.devicesCached ?: [:]
    //state.devices = [:]
    state.devices.each {
        it.value["verified"] = null
    }
    state.devices.each {
        it.value["installed"] = null
    }
    state.deviceRefreshCount = 0
    verifyDevices()
    app.updateSetting("selectedDiscoveredDevice", "")
}

Map devicesDiscovered() {
	def vdevices = getVerifiedDevices()
	def map = [:]
	vdevices.each {
		def value = "${it.value.name}"
		def key = "${it.value.mac}"
		map["${key}"] = value
	}
	return map
}

def getVerifiedDevices() {
    //return getDevices()
	return getDevices().findAll{ it?.value?.verified == true && it?.value?.installed == false }
}

void ssdpSubscribe() {
    subscribe(location, "ssdpTerm.urn:schemas-upnp-org:device:Basic:1", ssdpHandler)
}

void ssdpDiscover() {
    sendHubCommand(new hubitat.device.HubAction("lan discovery urn:schemas-upnp-org:device:Basic:1", hubitat.device.Protocol.LAN))
}

def ssdpHandler(evt) {
    def description = evt.description
    def hub = evt?.hubId
    def parsedEvent = parseLanMessage(description)
    
    parsedEvent << ["hub":hub]
    //log.debug("ssdpHandler parsedEvent: $parsedEvent")

    def devices = getDevices()
    def devicesCache = getDevicesCache()
    
    String ssdpUSN = parsedEvent.ssdpUSN.toString()
    
    if (devices."${ssdpUSN}" == null) {
        devices << ["${ssdpUSN}": parsedEvent]
    }
    if (devicesCache."${ssdpUSN}" == null) {
        devicesCache << ["${ssdpUSN}": parsedEvent]
    }
}

void verifyDevices() {
    // First check if the devices actually are Tasmota devices
    def devices = getDevices().findAll { it?.value?.verified != true }
    devices.each {
        try{
            def ip = convertHexToIP(it.value.networkAddress)
            def port = convertHexToInt(it.value.deviceAddress)
            String host = "${ip}:${port}"
            sendHubCommand(new hubitat.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", hubitat.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
        } catch(e) {
            // Do nothing
            //log.debug("Device incorrect=$it")
        }
    }
    // Then check if we have already installed them
    devices = getDevices().findAll { it?.value?.verified == true && it?.value?.installed == null }
    //log.trace "devices = $devices"
    if(devices != [:]) {
        def installedDeviceIPs = getAllTasmotaDeviceIPs()
        //log.trace "installedDeviceIPs = $installedDeviceIPs"
        devices.each {
            def ip = convertHexToIP(it.value.networkAddress)
            if(ip in installedDeviceIPs) {
                //log.warn "Already installed: $ip"
                it.value << [installed:true]
            } else {
                //log.warn "NOT installed: $ip"
                it.value << [installed:false]
            }
        }
    }
}

def getDevices() {
    return state.devices = state.devices ?: [:]
}

def getDevicesCache() {
    return state.devicesCached = state.devicesCached ?: [:]
}

void deviceDescriptionHandler(hubitat.device.HubResponse hubResponse) {
	//log.trace "description.xml response (application/xml)"
	def body = hubResponse.xml
    //log.debug body?.device?.manufacturer?.text()
	if (body?.device?.manufacturer?.text().startsWith("iTead")) {
		def devices = getDevices()
		def device = devices.find {it?.key?.contains(body?.device?.UDN?.text())}
		if (device) {
			device.value << [name:body?.device?.friendlyName?.text() + " (" + convertHexToIP(hubResponse.ip) + ")", serialNumber:body?.device?.serialNumber?.text(), verified: true]
            //log.debug("deviceDescriptionHandler: $device.value")
		} else {
			log.error "/description.xml returned a device that didn't exist"
		}
	}
}

private String convertHexToIP(hex) {
    if(hex != null) {
	    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
    } else {
        return null
    }
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

 /**
 * --END-- APP TASMOTA DEVICE DISCOVERY METHODS (helpers-app-tasmota-device-discovery)
 */