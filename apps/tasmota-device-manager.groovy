#!includeNC:getAppRawRepoURL()

#!includeNC:getHeaderLicense()

#!include:getDefaultParentImports()

definition(
    name: "Tasmota Device Manager",
    namespace: "tasmota",
    author: "Markus Liljergren (markus-li)",
    description: "Device Manager for Tasmota",
    category: "Convenience",
    //TODO: Replace these icons???
    iconUrl:   "",
    iconX2Url: "",
    iconX3Url: "",
    documentationLink: "https://github.com/markus-li/Hubitat/wiki"
) {
    appSetting "defaultTasmotaPassword"
}

preferences {
     page(name: "mainPage", title: "Tasmota Device Manager", install: true, uninstall: true)
     page(name: "deleteDevice")
     page(name: "refreshDevices")
     page(name: "resultPage")
     page(name: "configureTasmotaDevice")
     page(name: "addDevices", title: "Add Tasmota-based Device", content: "addDevices")
     page(name: "manuallyAdd")
     page(name: "manuallyAddConfirm")
     page(name: "changeName")

     page(name: "discoveryPage", title: "Device Discovery", content: "discoveryPage", refreshTimeout:10)
     page(name: "deviceDiscovery")
     page(name: "deviceDiscoveryPage2")
     page(name: "deviceDiscoveryReset")
     page(name: "discoveredAddConfirm")
     
}

// https://docs.smartthings.com/en/latest/smartapp-developers-guide/preferences-and-settings.html#preferences-and-settings

// This App is ONLY used for installation and management tasks, it DOES NOT RUN in the background when not used by the user.
// The code in this App has not been optimized for performance and is in many ways very "unclean".
// One day I might take the time to clean it up and optimize it, but it works as intended already and since it
// doesn't affect resource use it really isn't that important.

#!include:getHelperFunctions('device-configurations')

Long getMillisSinceDate(myDate) {
    
    //myDate
    return now() - myDate.getTime()
}

String getTimeStringSinceMillis(millis) {
    Integer seconds = (Integer) (millis / 1000) % 60
    Integer minutes = (Integer) (millis / (1000*60)) % 60
    Integer hours = (Integer) (millis / (1000*60*60)) % 24
    Integer days = (Integer) (millis / (1000*60*60*24))
    return String.format("%dT%02d:%02d:%02d", days, hours, minutes, seconds)
}

String getTimeStringSinceDate(myDate) {
    return getTimeStringSinceMillis(getMillisSinceDate(myDate))
}

Map getTimeStringSinceDateWithMaximum(myDate, maxMillis) {
    def millis = getMillisSinceDate(myDate)
    return [time:getTimeStringSinceMillis(millis), red:millis > maxMillis]
}

#!include:getDefaultAppMethods()
 
void makeAppTitle(btnDone=false) {
    section(getElementStyle('title', getMaterialIcon('build', 'icon-large') + "${app.label} <span id='version'>${getAppVersion()}</span>" + getCSSStyles(btnDone))){
        }
}

Map mainPage() {
    return dynamicPage(name: "mainPage", title: "", nextPage: null, uninstall: true, install: true) {
        makeAppTitle() // Also contains the CSS
        logging("Building mainPage", 1)
        // Hubitat green: #81BC00
        // box-shadow: 2px 3px #A9A9A9
        installCheck()
        initializeAdditional()
        if (state.appInstalled == 'COMPLETE') {
            section(getElementStyle('header', getMaterialIcon('settings_applications') + "Configure App"), hideable: true, hidden: false){
                getElementStyle('separator')
                //input(name: "sendToAWSwitch", type: "bool", defaultValue: "false", title: "Use App Watchdog to track this apps version info?", description: "Update App Watchdog", submitOnChange: "true")}
                generate_preferences(configuration_model_debug())
                input("passwordDefault", "password", title:"Default Tasmota Password", submitOnChange: true, displayDuringSetup: true)
            
            //input(name: "pushAll", type: "bool", defaultValue: "false", submitOnChange: true, title: "Only send Push if there is something to actually report", description: "Push All")
            //href "deviceDiscoveryCancel", title:"Cancel Discover Device", description:""
            }
            section(getElementStyle('header', getMaterialIcon('library_add') + "Install New Devices"), hideable: true, hidden: false){
                href("deviceDiscovery", title:getMaterialIcon('', 'he-discovery_1') + "Discover Devices (using SSDP)", description:"")
                href("manuallyAdd", title:getMaterialIcon('', 'he-add_1') + "Manually Install Device", description:"")
            }
            section(getElementStyle('header', getMaterialIcon('playlist_add') + 'Grant Access to Additional Devices'), hideable: true, hidden: true){
                paragraph("Select the devices to grant access to, if the device doesn't use a compatible driver it will be ignored, so selecting too many or the wrong ones, doesn't matter. Easiest is probably to just select all devices. Only Parent devices are shown.")
                input(name:	"devicesSelected", type: "capability.initialize", title: "Available Devices", multiple: true, required: false, submitOnChange: true)
            }
            section(getElementStyle('header', getMaterialIcon('', 'he-settings1') + "Configure Devices"), hideable: true, hidden: false){ 
                paragraph('<div style="margin: 8px;">All devices below use a compatible driver, if any device is missing, add them above in "Grant Access to Additional Devices". Newly selected devices will not be shown until after you\'ve pressed Done. \"Refresh Devices\" runs the \"Refresh\" command on all devices in the list, this can take a bit of time if you have many devices...</div>')
                
                //input(name: "refreshDevices", type: "bool", defaultValue: "false", submitOnChange: true, title: "Refresh Devices", description: "Refresh Devices Desc")
                href("resultPage", title:getMaterialIcon('autorenew') + "Result Page", description: "")
                href("refreshDevices", title:getMaterialIcon('autorenew') + "Refresh Devices", description: "")
                
                getAllTasmotaDevices().each { rawDev ->
                //state.devices.each { rawDev ->
                    def cDev = getTasmotaDevice(rawDev.deviceNetworkId)
                    //getLastActivity()
                    if(cDev != null) {
                        href("configureTasmotaDevice", title:"${getMaterialIcon('', 'he-bulb_1 icon-small')} $cDev.label", description:"", params: [did: cDev.deviceNetworkId])
                        
                        Map lastActivity = getTimeStringSinceDateWithMaximum(cDev.getLastActivity(), 2*60*60*1000)
                        // Status
                        def deviceStatus = cDev.currentState('presence')?.value
                        logging("$cDev.id - deviceStatus = $deviceStatus", 1)
                        if(deviceStatus == null || deviceStatus == "not present") {
                            deviceStatus = "Timeout"
                        } else {
                            deviceStatus = "Available"
                        }

                        // Wifi
                        def wifiSignalQuality = cDev.currentState('wifiSignal')
                        
                        boolean wifiSignalQualityRed = true
                        if(wifiSignalQuality != null) {
                            wifiSignalQuality = wifiSignalQuality.value
                            wifiSignalQualityRed = extractInt(wifiSignalQuality) < 50
                        }
                        logging("$cDev.id - wifiSignalQuality = $wifiSignalQuality", 1)
                        String uptime = "${cDev.getDeviceDataByName('uptime')}"
                        String firmware = "${cDev.getDeviceDataByName('firmware')}"
                        String driverVersion = "${cDev.getDeviceDataByName('driver')}"
                        String driverName = "${getDeviceDriverName(cDev)}"
                        List childDevs = runDeviceCommand(rawDev, 'getChildDevices')
                        //logging("childDevs: $childDevs", 1)
                        List childDevsFiltered = []
                        childDevs.each {
                            logging("Child: $it.id, label: $it.label, driver: $it.data.driver", 1)
                            childDevsFiltered += ['id': it.id.toInteger(), 'label': it.label, 'driver': it.data.driver]
                        }
                        childDevsFiltered.sort({ a, b -> a["id"] <=> b["id"] })
                        logging("Children: $childDevsFiltered", 1)
                        getDeviceTable([href:           [href:getDeviceConfigLink(cDev.id)],
                                        ip:             [data:rawDev['data']['ip']],
                                        //[data:runDeviceCommand(getTasmotaDevice(cDev.deviceNetworkId), 'getDeviceDataByName', ['uptime'])],])
                                        uptime:         [data:uptime, red:uptime == "null"],
                                        lastActivity:   [data:lastActivity['time'], red:lastActivity['red']],
                                        wifi:           [data:"${wifiSignalQuality}", red:wifiSignalQualityRed],
                                        firmware:       [data:firmware, red:firmware == "null"],
                                        driverVersion:  [data:driverVersion, red:driverVersion == "null"],
                                        deviceStatus:   [data:deviceStatus, red:deviceStatus != "Available"],
                                        driverName:     [data:driverName, red:driverName == "null"],
                                        childDevices:   childDevsFiltered,])
                                // it.label
                            //btnParagraph([[href:getDeviceConfigLink(cDev.id), target:"_blank", title:"Config"],
                            //            [href:getDeviceTasmotaConfigLink(cDev['data']['ip']), target:"_blank", title:'Tasmota&nbsp;Web&nbsp;Config (' + cDev['data']['ip'] + ')']],
                            //)
                                //paragraph('<table style="border-spacing: 10px 0px"><tr><td class="btn btn-default btn-lg hrefElem mdl-button--raised mdl-shadow--2dp"><a style="color: #000;" href="' + "${getDeviceConfigLink(it.id)}" + '" target="deviceConfig">Config</a></td>' + 
                                        //' | <a href="' + "${getDeviceLogLink(it.id)}" + '" target="deviceConfig">Log</a>' +
                                //        '<td class="btn btn-default btn-lg hrefElem mdl-button--raised mdl-shadow--2dp"><a style="color: #000;" href="' + "${getDeviceTasmotaConfigLink(it['data']['ip'])}" + '" target="deviceWebConfig">Tasmota&nbsp;Web&nbsp;Config (' + it['data']['ip'] + ')</a></td></tr></table>' )
                        
                        //%7B%22did%22%3A%225002915AFD0A%22%7D
                        //%7B%22did%22%3A%222CF43222E6AD%22%7D
                    }

                }
            }
            /*section(getElementStyle('header', "More things"), hideable: true, hidden: true){
                paragraph("Select the devices to configure, if the device doesn't use a compatible driver it will be ignored, so selecting too many or the wrong ones, doesn't matter. Easiest is probably to just select all devices. Only Parent devices are shown.")
                
                input(name:	"devicesAvailable", type: "enum", title: "Available Devices", multiple: true, required: false, submitOnChange: true, options: state.devicesSelectable)
            }*/
        } else {
            section(getElementStyle('subtitle', "Configure")){
                generate_preferences(configuration_model_debug())
            }
        }
        footer()
    }
}

def refreshDevices(){
    logging("refreshDevices()", 1)
    Integer numDevices = 0
    Integer numDevicesSuccess = 0
    getAllTasmotaDevices().each {
        numDevices += 1
        try{
            //logging("BEFORE Refreshing Device \"${it.label}\" (${it.id})", 1)
            it.refresh()
            logging("AFTER Refreshing Device \"${it.label}\" (${it.id})", 1)
            numDevicesSuccess += 1
        } catch(e) {
            log.warn("Failed to Refresh Device \"${it.label}\" (${it.id})")
        }
    }
    String result = "COMPLETE REFRESH FAILURE!"
    if(numDevicesSuccess == numDevices) {
        result = "All $numDevices Device(s) have been refreshed!"
    } else {
        result = "PARTIAL FAILURE: $numDevicesSuccess of $numDevices Device(s) have been refreshed! (${numDevices - numDevicesSuccess} failed!)"
    }
    updatedAdditional()
    return resultPage("refreshDevices", "Devices Refreshed", result)
}

Map resultPage(){
    logging("resultPage()", 1)
    return resultPage("resultPage", "Result Page", "My little result...")
}

Map resultPage(name, title, result, nextPage = "mainPage", otherReturnPage = null, otherReturnTitle="Return Page"){
    logging("resultPage(name = $name, title = $title, result = $result, nextPage = $nextPage)", 1)

    return dynamicPage(name: name, title: "", nextPage: nextPage) {
        makeAppTitle(btnDone=true) // Also contains the CSS

        section(getElementStyle('header', getMaterialIcon('done') + "Action Completed"), hideable: true, hidden: false){
            paragraph("<div style=\"font-size: 16px;\">${result}</div>")
        }
        if(otherReturnPage != null) {
            section(getElementStyle('header', getMaterialIcon('dns') + "Actions"), hideable: true, hidden: false){ 
                href("$otherReturnPage", title:"$otherReturnTitle", description:"")
            }
        }
    }
}

Map resultPageFailed(name, title, result, nextPage = "mainPage", otherReturnPage = null, otherReturnTitle="Return Page"){
    logging("resultPage(name = $name, title = $title, result = $result, nextPage = $nextPage)", 1)

    return dynamicPage(name: name, title: "", nextPage: nextPage) {
        makeAppTitle(btnDone=true) // Also contains the CSS

        section(getElementStyle('header', getMaterialIcon('warning') + "Action Failed!"), hideable: true, hidden: false){
            paragraph("<div style=\"font-size: 16px;\">${result}</div>")
        }
        if(otherReturnPage != null) {
            section(getElementStyle('header', getMaterialIcon('dns') + "Actions"), hideable: true, hidden: false){ 
                href("$otherReturnPage", title:"$otherReturnTitle", description:"")
            }
        }
    }
}

String getElementStyle(style, String content=""){
    switch (style) {
        case 'header':
            //return '<div style="font-weight: bold; color:#fff;">' + content + '</div>'
            return content
            break
        case 'title':
            return '<h2 style="font-weight: bold; color:#382e2b;">' + content + '</h2>'
            break
        case 'subtitle':
            return '<div style="font-weight: bold; color:#382e2b;">' + content + '</div>'
            break
        case 'line':
            return '<hr style="height: 1px; border: 0px; background-color:#382e2b;"></hr>'
        case 'separator':
            return '\n<hr style="background-color:#1A77C9; height: 1px; border: 0;"></hr>'
            break
    }
}

String getMaterialIcon(String iconName, String extraClass='') {
    // Browse icons here
    // https://material.io/resources/icons/?style=baseline
    // known HE icons (set as class): he-bulb_1, he-settings1, he-file1, he-default_dashboard_icon, he-calendar1
    // he-discovery_1, he-add_2, he-door_closed
    return '<i class="material-icons icon-position ' + extraClass + '">' + iconName + '</i>'
}

Map btnParagraph(buttons, extra="") {
    //getDeviceConfigLink(it.id)
    String content = '<table style="border-spacing: 10px 0px"><tr>'
    buttons.each {
        //content += '<td class="btn btn-default btn-lg hrefElem mdl-button--raised mdl-shadow--2dp">'
        content += '<td>'
        
        content += '<a style="color: #000;" href="' + "${it['href']}" + '" target="' +"${it['target']}" + '">'
        
        content += '<button type="button" class="btn btn-default hrefElem btn-lg mdl-button--raised mdl-shadow--2dp btn-sub">'
        
        content += "${it['title']}"
        //content += '<i class="material-icons icon-position hrefElemAlt">arrow_drop_down</i>'
        content += '</button></a></td>'

//                                '<td class="btn btn-default btn-lg hrefElem mdl-button--raised mdl-shadow--2dp"><a style="color: #000;" href="' + "${getDeviceTasmotaConfigLink(it['data']['ip'])}" + '" target="deviceWebConfig">Tasmota&nbsp;Web&nbsp;Config (' + it['data']['ip'] + ')</a></td> )
    }
    content += '</tr></table>' // + extra
    return paragraph(content) 
}

String getDeviceTableCell(deviceInfoEntry, link=true) {
    def it = deviceInfoEntry
    String content = '<td class="device-config_td ' + "${it['td_class']}" + '">'
        
    if(link == true) {
        content += '<a class="device-config_btn ' + "${it['class']}" + '" href="' + "${it['href']}" + '" target="' +"${it['target']}" + '">'
    }
    
    //content += '<button type="button" class="btn btn-default hrefElem btn-lg mdl-button--raised mdl-shadow--2dp btn-sub">'
    String extraTitle = ""
    if(it['title'] != null && it['title'].indexOf('material-icons') == -1) {
        extraTitle = "title=\"${it['title']}\""
    }
    if(it['red'] == true) {
        
            content += "<div ${extraTitle} style=\"color: red;\" >${it['title']}</div>"
        
    } else {
        content += "<div ${extraTitle} >${it['title']}</div>"
    }
    //content += '<i class="material-icons icon-position hrefElemAlt">arrow_drop_down</i>'
    //content += '</button></a></td>'
    if(link == true) {
        content += '</a>'
    }
    content += '</td>'

    return content
}

String getDeviceTable(deviceInfo, String extra="") {
    //getDeviceConfigLink(it.id)
    String content = '<table class="device-config_table"><tr>'
    content += '<th style="width: 40px;"><div>Config</div></th>'
    content += '<th style="width: 100px;"><div>Tasmota&nbsp;Config</div></th>'
    content += '<th style="width: 80px;"><div>Uptime</div></th>'
    content += '<th style="width: 80px;"><div>Heartbeat</div></th>'
    content += '<th style="width: 33px;"><div>Wifi</div></th>'
    content += '<th style="width: 100px;"><div>Firmware</div></th>'
    content += '<th style="width: 100px;"><div>Driver</div></th>'
    content += '<th style="width: 60px;"><div>Status</div></th>'
    content += '<th style=""><div>Type</div></th>'
    content += '</tr><tr>'

    // Config Link
    content += getDeviceTableCell([href:deviceInfo['href']['href'], 
        target:'_blank', title:getMaterialIcon('', 'he-settings1 icon-tiny device-config_btn_icon')])

    // Tasmota Web Config Link
    content += getDeviceTableCell([class:'device-config_btn', href:getDeviceTasmotaConfigLink(deviceInfo['ip']['data']), 
        target:'_blank', title:deviceInfo['ip']['data']])

    // Tasmota Device Uptime
    content += getDeviceTableCell([title:deviceInfo['uptime']['data'], red:deviceInfo['uptime']['red']], false)

    // Tasmota Heartbeat
    content += getDeviceTableCell([title:deviceInfo['lastActivity']['data'], red:deviceInfo['lastActivity']['red']], false)

    // Wifi Signal Quality
    content += getDeviceTableCell([title:deviceInfo['wifi']['data'], red:deviceInfo['wifi']['red']], false)

    // Firmware Version
    content += getDeviceTableCell([title:deviceInfo['firmware']['data'], red:deviceInfo['firmware']['red']], false)

    // Driver Version
    content += getDeviceTableCell([title:deviceInfo['driverVersion']['data'], red:deviceInfo['driverVersion']['red']], false)

    // Status
    content += getDeviceTableCell([title:deviceInfo['deviceStatus']['data'], red:deviceInfo['deviceStatus']['red']], false)

    // Driver Type
    content += getDeviceTableCell([title:deviceInfo['driverName']['data'], red:deviceInfo['driverName']['red']], false)

    content += '</tr>'
    content += '<tr>'
    content += "<td class=\"childlist-cell\" colspan=\"9\" >${getChildDeviceHREFList(deviceInfo['childDevices'])}</ td>"
    content += '</tr></table>' // + extra
    paragraph(content) 
}

String getChildDeviceHREFList(childDevices) {
    String r = ''
    String prefix = ''
    // id:1091, label:Parent Testing (192.168.10.146) (POWER1), driver:v1.0.0222Tb
    childDevices.each {
        r += prefix
        r += "<a href=\"${getDeviceConfigLink(it.id)}\" target=\"_blank\">"
        r += "${getMaterialIcon('', 'he-settings1 icon-tiny-compact')}"
        r += "${it.label.replace(' ', '&nbsp;')}"
        r += "</a>"
        prefix = ',&nbsp; '
    }
    return r
}

def configureTasmotaDevice(params) {
    if (params?.did || params?.params?.did) {
        if (params.did) {
            //getTasmotaDevice
            //
            state.currentDeviceId = params.did
            state.currentDisplayName = getTasmotaDevice(params.did).label
            logging("params.did: $params.did, label: ${getTasmotaDevice(params.did)?.label}", 1)
        } else {
            logging("params.params.did: $params.params.did", 1)
            state.currentDeviceId = params.params.did
            state.currentDisplayName = getTasmotaDevice(params.params.did)?.label
        }
    }
    def device = getTasmotaDevice(state.currentDeviceId)
    state.currentDisplayName = device.label
    logging("state.currentDeviceId: ${state.currentDeviceId}, label: ${device.label}", 1)
    //if (device != null) device.configure()
    dynamicPage(name: "configureTasmotaDevice", title: "Configure Tasmota-based Devices created with this app", nextPage: "mainPage") {
            section {
                app.updateSetting("${state.currentDeviceId}_label", device.label)
                input "${state.currentDeviceId}_label", "text", title:"Device Name" + getCSSStyles(), description: "", required: false
                href "changeName", title:"Change Device Name", description: "Edit the name above and click here to change it"
            }
            section {
                href "deleteDevice", title:"Delete \"$device.label\"", description: ""
            }
    }
    // device = getChildDevice(did)
    //dynamicPage(name: "configureTasmotaDevice", nextPage: null, uninstall: false, install: false) {
    //    paragraph('Device: <a href="' + "${getDeviceConfigLink(device.deviceId)}" + '" target="deviceConfig">' + device.label + '</a>' + 
    //                          ' - <a href="' + "${getDeviceLogLink(device.deviceId)}" + '" target="deviceConfig">Device&nbsp;Log</a>')
    //}
}

//
def deviceDiscoveryTEMP() {
   dynamicPage(name: "deviceDiscoveryTEMP", title: "Discover Tasmota-based Devices", nextPage: "mainPage") {
		section {
			paragraph "NOT FUNCTIONAL: This process will automatically discover your device, this may take a few minutes. Please be patient. Tasmota Device Manager then communicates with the device to obtain additional information from it. Make sure the device is on and connected to your WiFi network."
            /*input "deviceType", "enum", title:"Device Type", description: "", required: true, options: 
                #!include:makeTasmotaConnectDriverListV1()
            input "ipAddress", "text", title:"IP Address", description: "", required: true */
		}
    }
}


def manuallyAdd() {
    dynamicPage(name: "manuallyAdd", title: "", nextPage: "manuallyAddConfirm", previousPage: "mainPage") {
        makeAppTitle() // Also contains the CSS
		section(getElementStyle('header', getMaterialIcon('', 'he-add_1') + "Manually Install a Tasmota-based Device"), hideable: true, hidden: false) {
            paragraph("This process will install a Tasmota-based Device with the entered IP address. Tasmota Device Manager then communicates with the device to obtain additional information from it. Make sure the device is on and connected to your wifi network.")
            
            input("deviceType", "enum", title:"Device Type", description: "", required: true, submitOnChange: false, options: 
                #!include:makeTasmotaConnectDriverListV1()
            )
            input(name: "deviceConfig", type: "enum", title: "Device Configuration", 
                description: "Select a Device Configuration (default: Generic Device)<br/>'Generic Device' doesn't configure device Template and/or Module on Tasmota. Child devices and types are auto-detected as well as auto-created and does NOT depend on this setting.", 
                options: getDeviceConfigurationsAsListOption(), defaultValue: "01generic-device", required: false)
            input("ipAddress", "text", title:"IP Address", description: "", required: false, submitOnChange: false)
            input("deviceLabel", "text", title:"Device Label", description: "", required: true, defaultValue: (deviceType ? deviceType : "Tasmota - Universal Parent") + " (%device_ip%)")
            paragraph("'%device_ip%' = insert device IP here")
            input("passwordDevice", "password", title:"Tasmota Device Password", description: "Only needed if set in Tasmota.", defaultValue: passwordDefault, submitOnChange: true, displayDuringSetup: true)            
            paragraph("Only needed if set in Tasmota.")
            paragraph("To exit without installing a device, complete the required fields but DON'T enter a correct IP, then click \"Next\".")
            // Have to find a way to leave this page without filling in the required fields...
            //href("mainPage", title:getMaterialIcon('cancel') + "Cancel", description: "")
		}
    }
}

def manuallyAddConfirm(){
   if ( ipAddress =~ /^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$/) {
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
        def tmpIpAddress = ipAddress
        // Restore for next time
        app.updateSetting("ipAddress", [type: "string", value:getFirstTwoIPBytes(ipAddress)])
        app.updateSetting("deviceLabel", "")
        app.updateSetting("passwordDevice", "")
        //app.updateSetting("deviceConfig", [type: "enum", value:"01generic-device"])
        
        resultPage("manuallyAddConfirm", "Manual Installation Summary", 
                   "The device with IP \"$tmpIpAddress\" has been installed. It may take up to a minute or so before all child devices have been created if many are needed. Be patient. If all child devices are not created as expected, press Configure and Refresh in the Universal Parent and wait again. Don't click multiple times, it takes time for the device to reconfigure itself. Click \"Done\" to Continue.", 
                   nextPage="mainPage")
    } else {
        resultPageFailed("manuallyAddConfirm", "Manual Installation Summary", 
                   "The entered ip address ($ipAddress) is not valid. Please try again. To add another device click \"Add Another Device\". Click \"Done\" to Continue.", 
                   nextPage="mainPage", otherReturnPage="manuallyAdd", otherReturnPageTitle="Add Another Device")
    }
}

def deleteDevice(){
    try {
        unsubscribe()
        deleteChildDevice(state.currentDeviceId)
        resultPage("deleteDevice", "Deletion Summary", 
                   "The device with DNI $state.currentDeviceId has been deleted. Click \"Done\" to Continue.", 
                   nextPage="mainPage")
	} catch (e) {
        resultPage("deleteDevice", "Deletion Summary", 
                   "Error: ${(e as String).split(":")[1]}.", 
                   nextPage="mainPage")    
    }
}

def changeName(){
    def thisDevice = getChildDevice(state.currentDeviceId)
    thisDevice.label = settings["${state.currentDeviceId}_label"]

    resultPage("changeName", "Change Name Summary", 
                   "The device has been renamed to \"$thisDevice.label\". Click \"Done\" to Continue.", 
                   nextPage="mainPage")
}

def getDeviceDriverName(device) {
    //getTasmotaDevice device.deviceNetworkId
    String driverName = 'Unknown'
    try {
        driverName = runDeviceCommand(device, 'getDeviceInfoByName', args=['name'])
    } catch(e) {
        logging("Failed getting DriverName ($e), trying again...", 1)
        device = getTasmotaDevice(device.deviceNetworkId)
        try{
            driverName = runDeviceCommand(device, 'getDeviceInfoByName', args=['name'])
        } catch(e1) {
            driverName = "Unknown"
        }
    }
    if (driverName.startsWith("Tasmota - ")) driverName = driverName.substring(10)
    logging("Found Driver Name: '$driverName'", 0)
    return driverName
}

def getDeviceConfigLink(deviceId) {
    ///device/edit/
    return("http://${location.hub.localIP}/device/edit/${deviceId}")
}

def getDeviceLogLink(deviceId) {
    ///device/edit/
    return("http://${location.hub.localIP}/logs#dev${deviceId}")
}

def getDeviceTasmotaConfigLink(deviceIP) {
    ///device/edit/
    return("http://${deviceIP}/")
}

def installCheck() {
	state.appInstalled = app.getInstallationState()
	
	if (state.appInstalled != 'COMPLETE') {
		section{paragraph "Please hit 'Done' to finish installing '${app.label}'"}
  	}
  	else {
    	logging("Parent Installed OK", 1)
  	}
}

def footer() {
    section() {
        paragraph(getElementStyle('line'))
        paragraph('<div style="color:#382e2b; text-align:center">' + app.label + " ${getAppVersion()} " + '- Copyright&nbsp;2020&nbsp;Markus&nbsp;Liljergren - <a href="https://github.com/markus-li/Hubitat/tree/release" target="_blank">GitHub repo</a></div>')
    }
}

/*
	installedAdditional

	Purpose: initialize the app
	Note: if present, called from installed() in all drivers/apps
    installed() does NOT call initalize() by default, so if needed, call it here.
*/
def installedAdditional() {
    logging("installedAdditional()", 1)
	initialize()
}

def uninstalled() {
    logging("uninstalled()", 1)
    unsubscribe()
    unschedule()
}

def updatedAdditional() {
    logging("updatedAdditional()", 1)
	unsubscribe()
    unschedule()
    def devices = getAllTasmotaDevices()
    
    //app.removeSetting("devicesAvailable")
    //app.updateSetting("devicesAvailable", devices)
    //devicesSelected = devices
    state.devices = devices.sort({ a, b -> a["label"] <=> b["label"] })
    def devicesSelectable = []
    state.devices.each { devicesSelectable << ["${it.deviceNetworkId}":"${it.label}"] }

    logging("devicesSelectable: ${devicesSelectable}", 1)
    state.devicesSelectable = devicesSelectable
	initialize()
}

def runDeviceCommand(device, cmd, args=[]) {
    def jsonSlurper = new JsonSlurper()
    logging("runDeviceCommand(device=${device.deviceId.toString()}, cmd=${cmd}, args=${args})", 0)
    // Since Refresh is defined as a command in all my drivers, 
    // it can be used to forward the call to deviceCommand.
    //device.parse(JsonOutput.toJson([cmd: cmd, args: args]), 1)
    
    device.refresh(JsonOutput.toJson([cmd: cmd, args: args]))
    //device.deviceCommand(JsonOutput.toJson([cmd: cmd, args: args]))
    r = null
    r = jsonSlurper.parseText(device.getDataValue('appReturn'))

    device.updateDataValue('appReturn', null)
    return r
}

// 
def getAllTasmotaDevices() {
    def toRemove = []
    def devicesFiltered = []
    devicesSelected.eachWithIndex { it, i ->
        def namespace = 'unknown'
        try {
            //runDeviceCommand(it, 'setDeviceInfoAsData', ['namespace'])
            //namespace = runDeviceCommand(it, 'getDeviceInfoByName', ['namespace'])
            //runDeviceCommand(it, 'testing', [])
            namespace = it.getDataValue('namespace')
        } catch(e) {
            // This only fails when a device is of no interest to us anyway, so mute it...
            logging("Device ID: ${it.deviceId.toString()}, e: ${e}", 1)
        }

        logging("Device ID: ${it.deviceId.toString()}, Parent ID: ${it.parentAppId.toString()}, name: ${it.getName()}, namespace: ${namespace}, deviceNetworkId: ${it.deviceNetworkId}, i: ${i}", 0)
        //logging(it.getProperties().toString(), 1)
        //logging(it.parentAppId, 1)
        //parentAppId
        if(namespace == 'tasmota' && it.parentAppId != app.id) {
            devicesFiltered << it
        }
    }
    def childDevices = getChildDevices()
    logging("getChildDevices: ${getChildDevices()}", 1)
    childDevices.eachWithIndex { it, i ->
        def namespace = 'unknown'
        try {
            //runDeviceCommand(it, 'setDeviceInfoAsData', ['namespace'])
            namespace = runDeviceCommand(it, 'getDeviceInfoByName', ['namespace'])
            //runDeviceCommand(it, 'testing', [])
            //namespace = it.getDataValue('namespace')
        } catch(e) {
            // This only fails when a device is of no interest to us anyway, so mute it...
            logging("Device ID: ${it.id.toString()}, e: ${e}", 1)
        }

        logging("Device ID: ${it.id.toString()}, Parent ID: ${it.parentAppId.toString()}, name: ${it.getName()}, namespace: ${namespace}, deviceNetworkId: ${it.deviceNetworkId}, i: ${i}", 0)
        //logging(it.getProperties().toString(), 1)
        //logging(it.parentAppId, 1)
        //parentAppId
        if(namespace == 'tasmota') {
            devicesFiltered << it
        }
    }
    //devicesFiltered << childDevices
    return devicesFiltered.sort({ a, b -> a.label <=> b.label })
}

def getAllTasmotaDeviceIPs() {
    def deviceIPs = []
    getAllTasmotaDevices().each { rawDev ->
        def cDev = getTasmotaDevice(rawDev.deviceNetworkId)
        if(cDev != null) {
            deviceIPs << rawDev['data']['ip']
        }
    }
    return deviceIPs
}

def getTasmotaDevice(deviceNetworkId) {
    def r = getChildDevice(deviceNetworkId)
    if(r == null) {
        devicesSelected.each {
            //logging("'" + it.deviceNetworkId + "' =? '" + deviceNetworkId + "'", 1)
            if(it.deviceNetworkId == deviceNetworkId) {
                //logging("'" + it.deviceNetworkId + "' == '" + deviceNetworkId + "' (dev: ${it})", 1)
                r = it
            }
        }
    }
    return r
}

/*
	initializeAdditional

	Purpose: initialize the app
	Note: if present, called from initialize() in all drivers/apps
    Called when Done is pressed in the App
*/
def initializeAdditional() {
    logging("initializeAdditional()", 1)
    // Do NOT start SSDP discovery here! That should ONLY be done when searching for devices
    deviceDiscoveryCancel()
}

#!include:getLoggingFunction()

#!include:getHelperFunctions('app-default')

#!include:getHelperFunctions('app-css')

#!include:getHelperFunctions('app-tasmota-device-discovery')

#!include:getHelperFunctions('tasmota')

#!include:getHelperFunctions('styling')
