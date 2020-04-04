/**
 * DEVICE CONFIGURATIONS METHODS (helpers-device-configurations)
 *
 *   Device configurations and functions for using them
 */
TreeMap getDeviceConfigurations() {
    // To add more devices, just add them below ;)
    // Don't forget that BOTH the App and the Universal driver needs to have this configuration.
    // If you add a device and it works well for you, please share your
    // configuration in the Hubitat Community Forum.
    //
    // typeId HAS to be unique
    // 
    // For the rest of the settings, see below to figure it out :P
    List deviceConfigurations = [
        [typeId: 'sonoff-basic-r3', 
         name: 'Sonoff Basic R3',
         module: 1,
         installCommands: [["SetOption81", "0"]],
         deviceLink: 'https://templates.blakadder.com/sonoff_basic_R3.html'],

        [typeId: 'tuyamcu-ce-wf500d-dimmer',
         name: 'TuyaMCU CE Smart Home WF500D Dimmer',
         template: '{"NAME":"CE WF500D","GPIO":[255,255,255,255,255,255,0,0,255,108,255,107,255],"FLAG":0,"BASE":54}',
         installCommands: [["SetOption66", "0"], // Set publishing TuyaReceived to MQTT to DISABLED
         ],
         deviceLink: 'https://templates.blakadder.com/ce_smart_home-WF500D.html'],

        [typeId: 'ce-la-2-w3-wall-outlet',
         name: 'CE Smart Home LA-2-W3 Wall Outlet',
         template: '{"NAME":"CE LA-2-W3","GPIO":[255,255,255,255,157,17,0,0,21,255,255,255,255],"FLAG":15,"BASE":18}',
         installCommands: [],
         deviceLink: 'https://templates.blakadder.com/ce_smart_home_LA-2-W3.html'],

        [typeId: 'ce-lq-2-w3-wall-outlet',
         name: 'CE Smart Home LQ-2-W3 Wall Outlet',
         template: '{"NAME":"CE LQ-2-W3","GPIO":[255,255,255,255,255,17,255,255,21,255,255,255,255],"FLAG":15,"BASE":18}',
         installCommands: [],
         deviceLink: 'https://templates.blakadder.com/ce_smart_home_LQ-2-W3.html'],

        [typeId: 'ce-la-wf7-pm-plug',
         name: 'CE Smart Home LA-WF7 Power Monitor Plug',
         template: '{"NAME":"CESmartHLA-WF7","GPIO":[0,56,0,17,134,132,0,0,131,57,21,0,0],"FLAG":0,"BASE":18}',
         installCommands: [],
         deviceLink: 'https://templates.blakadder.com/ce_smart_home_LA-WF7.html'],

        [typeId: 'awp02l-n-plug',
         name: 'AWP02L-N Plug',
         template: '{"NAME":"AWP02L-N","GPIO":[57,0,56,0,0,0,0,0,0,17,0,21,0],"FLAG":1,"BASE":18}',
         installCommands: [],
         deviceLink: 'https://templates.blakadder.com/hugoai_awp02l-n.html'],

        [typeId: 'cyyltf-bifans-j23-plug',
         name: 'CYYLTF BIFANS J23 Plug',
         template: '{"NAME":"CYYLTF J23","GPIO":[56,0,0,0,0,0,0,0,21,17,0,0,0],"FLAG":1,"BASE":18}',
         installCommands: [],
         deviceLink: 'https://templates.blakadder.com/cyyltd_bifans_J23.html'],

        [typeId: 'gosund-wp3-plug',
         name: 'Gosund WP3 Plug',
         template: '{"NAME":"Gosund WP3","GPIO":[0,0,0,0,17,0,0,0,56,57,21,0,0],"FLAG":0,"BASE":18}',
         installCommands: [],
         deviceLink: 'https://templates.blakadder.com/gosund_wp3.html'],

        [typeId: 'sk03-pm-outdoor-plug',
         name: 'SK03 Power Monitor Outdoor Plug',
         template: '{"NAME":"SK03 Outdoor","GPIO":[17,0,0,0,133,132,0,0,131,57,56,21,0],"FLAG":0,"BASE":57}',
         installCommands: [],
         deviceLink: 'https://templates.blakadder.com/SK03_outdoor.html'],

        [typeId: 'aoycocr-x10s-pm-plug',
         name: 'Aoycocr X10S Power Monitor Plug',
         template: '{"NAME":"Aoycocr X10S","GPIO":[56,0,57,0,21,134,0,0,131,17,132,0,0],"FLAG":0,"BASE":45}',
         installCommands: [],
         deviceLink: 'https://templates.blakadder.com/aoycocr_X10S.html'],

        [typeId: 'brilliant-20699-rgbw-bulb',
         name: 'Brilliant 20699 800lm RGBW Bulb',
         template: '{"NAME":"Brilliant20699","GPIO":[0,0,0,0,141,140,0,0,37,142,0,0,0],"FLAG":0,"BASE":18}',
         installCommands: [["WebLog", "2"]],
         deviceLink: 'https://templates.blakadder.com/brilliant_20699.html'],

        [typeId: 'sonoff-sv',
         name: 'Sonoff SV',
         template: '{"NAME":"Sonoff SV","GPIO":[17,255,0,255,255,255,0,0,21,56,255,0,0],"FLAG":1,"BASE":3}',
         installCommands: [],
         deviceLink: 'https://templates.blakadder.com/sonoff_SV.html'],

        [typeId: 'sonoff-th',
         name: 'Sonoff TH',
         template: '{"NAME":"Sonoff TH","GPIO":[17,255,0,255,255,0,0,0,21,56,255,0,0],"FLAG":0,"BASE":4}',
         installCommands: [["TempRes", (tempRes == '' || tempRes == null ? "1" : tempRes)]],
         deviceLink: 'https://templates.blakadder.com/sonoff_TH.html'],

        [typeId: 'sonoff-pow',
         name: 'Sonoff POW',
         template: '{"NAME":"Sonoff Pow","GPIO":[17,0,0,0,0,130,0,0,21,132,133,52,0],"FLAG":0,"BASE":6}',
         installCommands: [["SetOption81", "1"], ["LedPower", "1"], ["LedState", "8"]],
         deviceLink: 'https://templates.blakadder.com/sonoff_Pow.html'],

        [typeId: 'sonoff-s31',
         name: 'Sonoff S31',
         template: '{"NAME":"Sonoff S31","GPIO":[17,145,0,146,0,0,0,0,21,56,0,0,0],"FLAG":0,"BASE":41}',
         installCommands: [["SetOption81", "1"], ["LedPower", "1"], ["LedState", "8"]],
         deviceLink: 'https://templates.blakadder.com/sonoff_S31.html'],
        
        [typeId: 'sonoff-ifan02',
         name: 'Sonoff iFan02',
         module: 44,
         //template: '{"NAME":"Sonoff iFan02","GPIO":[17,255,0,255,23,22,18,19,21,56,20,24,0],"FLAG":0,"BASE":44}',
         installCommands: [['Rule1', '0']],
         deviceLink: 'https://templates.blakadder.com/sonoff_ifan02.html'],

        /*[typeId: 'sonoff-ifan03-no_beep-m44',
         name: 'Sonoff iFan03 (No Beep) M44',
         template: '{"NAME":"Sonoff iFan03","GPIO":[17,255,0,255,0,0,29,33,23,56,22,24,0],"FLAG":0,"BASE":44}',
         installCommands: [["SetOption67", "0"], ['Rule1', '0']],
         deviceLink: 'https://templates.blakadder.com/sonoff_ifan03.html'],

        [typeId: 'sonoff-ifan03-beep-m44',
         name: 'Sonoff iFan03 (Beep) M44',
         template: '{"NAME":"Sonoff iFan03","GPIO":[17,255,0,255,0,0,29,33,23,56,22,24,0],"FLAG":0,"BASE":44}',
         installCommands: [["SetOption67", "0"],
                           ['Rule1', 'ON Fanspeed#Data>=1 DO Buzzer %value%; ENDON ON Fanspeed#Data==0 DO Buzzer 1; ENDON'],
                           ['Rule1', '1']],
         deviceLink: 'https://templates.blakadder.com/sonoff_ifan03.html'],*/

        [typeId: 'sonoff-ifan03-no_beep-m71',
         name: 'Sonoff iFan03 (No Beep)',
         module: 71,
         //template: '{"NAME":"SonoffiFan03","GPIO":[17,148,0,149,0,0,29,161,23,56,22,24,0],"FLAG":0,"BASE":71}',
         installCommands: [["SetOption67", "0"], ['Rule1', '0']],
         deviceLink: 'https://templates.blakadder.com/sonoff_ifan03.html'],

        [typeId: 'sonoff-ifan03-beep-m71',
         name: 'Sonoff iFan03 (Beep)',
         module: 71,
         //template: '{"NAME":"SonoffiFan03","GPIO":[17,148,0,149,0,0,29,161,23,56,22,24,0],"FLAG":0,"BASE":71}',
         installCommands: [["SetOption67", "1"], 
                           ['Rule1', 'ON Fanspeed#Data>=1 DO Buzzer %value%; ENDON ON Fanspeed#Data==0 DO Buzzer 1; ENDON'],
                           ['Rule1', '1']],
         deviceLink: 'https://templates.blakadder.com/sonoff_ifan03.html'],

        [typeId: 'kmc-4-pm-plug',
         name: 'KMC 4 Power Monitor Plug',
         template: '{"NAME":"KMC 4 Plug","GPIO":[0,56,0,0,133,132,0,0,130,22,23,21,17],"FLAG":0,"BASE":36}',
         installCommands: [],
         deviceLink: 'https://templates.blakadder.com/kmc-4.html'],

        [typeId: 'teckin-sp10-plug',
         name: 'Teckin SP10 Plug',
         template: '{"NAME":"Teckin SP10","GPIO":[255,255,56,255,255,255,0,0,255,17,255,21,255],"FLAG":0,"BASE":18}',
         installCommands: [],
         deviceLink: 'https://templates.blakadder.com/teckin_SP10.html'],

        [typeId: 'awp04l-pm-plug',
         name: 'AWP04L Power Monitor Plug',
         template: '{"NAME":"AWP04L","GPIO":[57,255,255,131,255,134,0,0,21,17,132,56,255],"FLAG":0,"BASE":18}',
         installCommands: [],
         deviceLink: 'https://templates.blakadder.com/awp04l.html'],

        [typeId: 'sonoff-4ch-pro',
         name: 'Sonoff 4CH Pro',
         template: '{"NAME":"Sonoff 4CH Pro","GPIO":[17,255,255,255,23,22,18,19,21,56,20,24,0],"FLAG":0,"BASE":23}',
         installCommands: [],
         deviceLink: 'https://templates.blakadder.com/sonoff_4CH_Pro.html'],

        [typeId: 'unbranded-rgbwwcw-controller-type-1',
         name: 'Unbranded RGBWWCW Controller (Type 1)',
         template: '{"NAME":"CtrlRGBWWCW-T1","GPIO":[17,0,0,0,0,40,0,0,38,39,37,41,0],"FLAG":0,"BASE":18}',
         installCommands: [["WebLog", "2"]],
         deviceLink: ''],
        
        [typeId: 'tuyamcu-touch-switch-1-button',
        name: 'TuyaMCU Touch Switch - 1 button',
        module: 54,
        installCommands: [["TuyaMCU", "11,1"], ["TuyaMCU", "12,0"], 
                          ["TuyaMCU", "13,0"], ["TuyaMCU", "14,0"]],
        deviceLink: ''],

        // {"NAME":"Lucci Fan","GPIO":[0,107,0,108,0,0,0,0,0,0,0,0,0],"FLAG":0,"BASE":44}
        [typeId: 'tuyamcu-lucci-connect-remote-as-switches',
        name: 'TuyaMCU Lucci Connect Remote',
        template: '{"NAME":"Lucci Fan","GPIO":[0,0,0,0,0,0,0,0,0,108,0,107,0],"FLAG":0,"BASE":54}',
        installCommands: [["TuyaMCU", "11,102"], ["TuyaMCU", "12,1"], ["TuyaMCU", "13,0"], 
                          ["TuyaMCU", "14,0"], ["TuyaMCU", "15,0"], ["TuyaMCU", "21,50"], 
                          ["Rule1", "on TuyaReceived#Data=55AA00070005020400010012 do dimmer 1 endon on TuyaReceived#Data=55AA00070005020400010113 do dimmer 2 endon on TuyaReceived#Data=55AA00070005020400010214 do dimmer 3 endon on Dimmer#State=1 do TuyaSend4 2,0 endon on Dimmer#State=2 do TuyaSend4 2,1 endon on Dimmer#State=3 do TuyaSend4 2,2 endon"], 
                          ["Rule1", "1"]],
        deviceLink: 'https://templates.blakadder.com/luci-connect-remote-control.html'],

        [typeId: 'tuyamcu-touch-switch-2-button',
        name: 'TuyaMCU Touch Switch - 2 buttons',
        module: 54,
        installCommands: [["TuyaMCU", "11,1"], ["TuyaMCU", "12,2"], 
                          ["TuyaMCU", "13,0"], ["TuyaMCU", "14,0"]],
        deviceLink: ''],

        [typeId: 'tuyamcu-touch-switch-3-button',
        name: 'TuyaMCU Touch Switch - 3 buttons',
        module: 54,
        installCommands: [["TuyaMCU", "11,1"], ["TuyaMCU", "12,2"], 
                          ["TuyaMCU", "13,3"], ["TuyaMCU", "14,0"]],
        deviceLink: ''],

        [typeId: 'tuyamcu-touch-switch-4-button',
        name: 'TuyaMCU Touch Switch - 4 buttons',
        module: 54,
        template: '',
        installCommands: [["TuyaMCU", "11,1"], ["TuyaMCU", "12,2"], 
                          ["TuyaMCU", "13,3"], ["TuyaMCU", "14,4"]],
        deviceLink: ''],

        [typeId: 'sonoff-powr2', 
        name: 'Sonoff POW R2',
        template: '{"NAME":"Sonoff Pow R2","GPIO":[17,145,0,146,0,0,0,0,21,56,0,0,0],"FLAG":0,"BASE":43}',
        installCommands: [["SetOption81", "1"], ["LedPower", "1"], ["LedState", "8"]],
        deviceLink: 'https://templates.blakadder.com/sonoff_Pow_R2.html'],

        [typeId: 'sonoff-s20', 
        name: 'Sonoff S20',
        template: '{"NAME":"Sonoff S20","GPIO":[17,255,255,255,0,0,0,0,21,56,0,0,0],"FLAG":0,"BASE":8}',
        installCommands: [["SetOption81", "1"], ["LedPower", "1"], ["LedState", "8"]],
        deviceLink: 'https://templates.blakadder.com/sonoff_S20.html'],

        [typeId: 'sonoff-s26', 
        name: 'Sonoff S26',
        template: '{"NAME":"Sonoff S26","GPIO":[17,255,255,255,0,0,0,0,21,158,0,0,0],"FLAG":0,"BASE":8}',
        installCommands: [["SetOption81", "1"]],
        deviceLink: 'https://templates.blakadder.com/sonoff_S26.html'],

        [typeId: 'sonoff-mini', 
        name: 'Sonoff Mini',
        template: '{"NAME":"Sonoff Mini","GPIO":[17,0,0,0,9,0,0,0,21,56,0,0,255],"FLAG":0,"BASE":1}',
        installCommands: [],
        deviceLink: 'https://templates.blakadder.com/sonoff_mini.html'],

        [typeId: 'sonoff-basic',
        name: 'Sonoff Basic',
        module: 1,
        installCommands: [["SetOption81", "1"]],
        deviceLink: 'https://templates.blakadder.com/sonoff_basic.html'],

        [typeId: 's120-plug' ,
        name: 'S120 USB Charger Plug',
        template: '{"NAME":"S120 Plug","GPIO":[0,0,0,0,0,21,0,0,0,52,90,0,0],"FLAG":0,"BASE":18}',
        installCommands: [["SetOption81", "1"]],
        deviceLink: 'https://templates.blakadder.com/brilliantsmart_20676.html'],

        [typeId: 's120-plug-bmp280' ,
        name: 'S120 USB Charger Plug + BMP280',
        template: '{"NAME":"S120THPPlug","GPIO":[0,6,0,5,0,21,0,0,0,52,90,0,0],"FLAG":0,"BASE":18}',
        installCommands: [["SetOption81", "1"]],
        deviceLink: 'https://templates.blakadder.com/brilliantsmart_20676.html'],

        [typeId: 'globe-34207-bulb' ,
        name: 'Globe 34207 800lm RGBCCT Bulb',
        template: '{"NAME":"GlobeRGBWW","GPIO":[0,0,0,0,37,40,0,0,38,41,39,0,0],"FLAG":0,"BASE":18}',
        installCommands: [],
        deviceLink: 'https://templates.blakadder.com/globe-34207.html'],

        [typeId: 'brilliantsmart-20676-plug' ,
        name: 'BrilliantSmart 20676 USB Charger Plug',
        template: '{"NAME":"Brilliant20676","GPIO":[0,0,0,0,0,21,0,0,0,52,90,0,0],"FLAG":0,"BASE":18}',
        installCommands: [["SetOption81", "1"]],
        deviceLink: 'https://templates.blakadder.com/brilliantsmart_20676.html'],

        [typeId: 'brilliant-bl20925-pm-plug', 
        name: 'Brilliant Lighting BL20925 PM Plug',
        template: '{"NAME":"BL20925","GPIO":[0,0,0,17,133,132,0,0,131,158,21,0,0],"FLAG":0,"BASE":52}',
        installCommands: [],
        deviceLink: 'https://templates.blakadder.com/brilliant_BL20925.html'],

        [typeId: 'deta-6930ha-plug', 
        name: 'Deta 6930HA Plug',
        template: '{"NAME":"Deta6930HAPlug","GPIO":[0,17,0,0,0,0,0,0,0,56,21,0,0],"FLAG":0,"BASE":18}',
        installCommands: [],
        deviceLink: 'https://templates.blakadder.com/deta_6930HA.html'],

        [typeId: 'prime-ccrcwfii113pk-plug', 
        name: 'Prime CCRCWFII113PK Plug',
        template: '{"NAME":"PrimeCCRC13PK","GPIO":[0,0,0,0,57,56,0,0,21,122,0,0,0],"FLAG":0,"BASE":18}',
        installCommands: [],
        deviceLink: 'https://templates.blakadder.com/prime_CCRCWFII113PK.html'],

        [typeId: 'ykyc-wj1y0-10a', 
        name: 'YKYC-WJ1Y0-10A PM Plug',
        template: '{"NAME":"YKYC-001PMPlug","GPIO":[0,17,0,57,133,132,0,0,130,56,21,0,0],"FLAG":0,"BASE":18}',
        installCommands: [],
        deviceLink: ''],

        [typeId: 'ykyc-wj1y0-10a', 
        name: 'Merkury MI-BW210-999W',
        template: '{"NAME":"MI-BW210-999W","GPIO":[0,0,0,0,140,37,0,0,142,38,141,0,0],"FLAG":0,"BASE":48}',
        installCommands: [],
        deviceLink: ''],

        [typeId: 'lumary-rgbcct-led-strip', 
        name: 'Lumary RGBCCT LED Strip ',
        template: '{"NAME":"Lumary LED","GPIO":[17,0,0,0,37,40,0,0,38,41,39,0,0],"FLAG":0,"BASE":18}',
        installCommands: [],
        deviceLink: 'https://templates.blakadder.com/lumary_led_strip.html'],

        [typeId: 'tuyamcu-wifi-dimmer', 
        name: 'TuyaMCU Wifi Dimmer',
        module: 54,
        installCommands: [["SetOption66", "0"], // Set publishing TuyaReceived to MQTT to DISABLED
        ],
        deviceLink: ''],

        [typeId: 'zigbee-controller-default' ,
        name: 'Zigbee Controller (default pinout)',
        template: '{"NAME":"Zigbee","GPIO":[0,0,0,0,0,0,0,0,0,166,0,165,0],"FLAG":0,"BASE":18}',
        installCommands: [["SerialLog", "0"],
                          //['setoption3', '1'], // enable MQTT - REQUIRED for this Zigbee devices to work!
                          ],
        deviceLink: 'https://tasmota.github.io/docs/#/Zigbee'],

        [typeId: 'unbranded-rgb-controller-with-ir-type-1' ,
        name: 'Unbranded RGB Controller with IR (Type 1)',
        template: '{"NAME":"RGB Controller","GPIO":[0,0,0,0,0,38,0,0,39,51,0,37,0],"FLAG":15,"BASE":18}',
        installCommands: [["WebLog", "2"]],
        deviceLink: ''],

        [typeId: 'sonoff-4ch',
        name: 'Sonoff 4CH',
        template: '{"NAME":"Sonoff 4CH","GPIO":[17,255,255,255,23,22,18,19,21,56,20,24,0],"FLAG":0,"BASE":7}',
        installCommands: [],
        deviceLink: 'https://templates.blakadder.com/sonoff_4CH.html'],

        [typeId: 'sonoff-4ch-pro-r2',
        name: 'Sonoff 4CH Pro (R2)',
        template: '{"NAME":"Sonoff 4CH Pro","GPIO":[17,255,255,255,23,22,18,19,21,56,20,24,0],"FLAG":0,"BASE":23}',
        installCommands: [],
        deviceLink: 'https://templates.blakadder.com/sonoff_4CH_Pro.html'],

        [typeId: 'tuyamcu-znsn-wifi-curtain-wall-panel',
        comment: 'NOT GENERIC - read the instructions',
        name: 'TuyaMCU ZNSN Wifi Curtain Wall Panel',
        module: 54,
        installCommands: [["WebLog", "2"], // A good idea for dimmers
                        //SetOption66 - Set publishing TuyaReceived to MQTT  »6.7.0
                        //0 = disable publishing TuyaReceived over MQTT (default)
                        //1 = enable publishing TuyaReceived over MQTT
                        ['SetOption66', "1"], // This is REQUIRED to get the Tuya Data
                        ['Mem1', '100'],   // Updated with the current Curtain location
                        ['Mem2', '11'],    // Step for each increase
                        ['Mem3', '1'],     // delay in 10th of a second (1 = 100ms)
                        ['Mem4', '9'],     // Motor startup steps
                        ['Mem5', '1'],     // Extra step when opening
                        ['Delay', '15'],   // Set delay between Backlog commands
                        ['Rule1', 'ON Dimmer#State DO Mem1 %value%; ENDON'],
                        ['Rule1', '+ ON TuyaReceived#Data=55AA00070005650400010277 DO Backlog Var1 %mem1%; Var2 Go; Var5 C; Add1 %mem2%; Sub1 %mem4%; Var4 %mem2%; Event Go; ENDON'],
                        ['Rule1', '+ ON Event#Go DO Backlog Dimmer %var1%; Event %var5%%var1%; Event %var2%2; ENDON'],
                        ['Rule1', '+ ON Event#Go2 DO Backlog Add1 %var4%; Delay %mem3%; Event %var1%; Event %var2%;  ENDON'],
                        ['Rule1', '+ ON Event#O-7 DO Var2 sC; ENDON ON Event#O-8 DO Var2 sC; ENDON ON Event#O-9 DO Var2 sC; ENDON ON Event#O-10 DO Var2 sC; ENDON ON Event#O-11 DO Var2 sC; ENDON'],
                        ['Rule1', '1'],
                        ['Rule2', 'ON TuyaReceived#Data=55AA00070005650400010176 DO Backlog Var1 %mem1%; Var2 Go; Var5 O; Sub1 %mem2%; Add1 %mem4%; Var4 %mem2%; Add4 %mem5%; Mult4 -1; Event Go; ENDON'],
                        ['Rule2', '+ ON Event#sC DO Backlog Var2 sC2; Event sC2; ENDON'],
                        ['Rule2', '+ ON Event#sC2 DO Backlog Var2 sC2; TuyaSend4 101,1; ENDON'],
                        ['Rule2', '+ ON TuyaReceived#Data=55AA00070005650400010075 DO Var2 sC3; ENDON'],
                        ['Rule2', '+ ON Event#C107 DO Var2 sC; ENDON ON Event#C108 DO Var2 sC; ENDON ON Event#C109 DO Var2 sC; ENDON ON Event#C110 DO Var2 sC; END ON ON Event#C111 DO Var2 sC; ENDON'],
                        ['Rule2', '1'],
                        ['Rule3', 'ON Event#C100 DO Var2 sC; ENDON ON Event#C101 DO Var2 sC; ENDON ON Event#C102 DO Var2 sC; ENDON ON Event#C103 DO Var2 sC; ENDON ON Event#C104 DO Var2 sC; ENDON ON Event#C105 DO Var2 sC; ENDON ON Event#C106 DO Var2 sC; ENDON ON Event#O0 DO Var2 sC; ENDON ON Event#O-1 DO Var2 sC; ENDON ON Event#O-2 DO Var2 sC; ENDON ON Event#O-3 DO Var2 sC; ENDON ON Event#O-4 DO Var2 sC; ENDON ON Event#O-5 DO Var2 sC; ENDON ON Event#O-6 DO Var2 sC; ENDON ON Event#O-12 DO Var2 sC; ENDON'],
                        ['Rule3', '1']],
        deviceLink: '',
        open: ["TuyaSend4", "101,0"],
        stop: ["TuyaSend4", "101,1"],
        close: ["TuyaSend4", "101,2"],],
        
        [typeId: 'mj-sd02-dimmer-switch',
        comment: 'WITHOUT power status LED active by design',
        name: 'Martin Jerry MJ-SD02 Dimmer Switch',
        template: '{"NAME":"MJ-SD02","GPIO":[19,18,0,33,34,32,255,255,31,37,30,126,29],"FLAG":15,"BASE":18}',
        // Possible alternative: {"NAME":"MJ-SD02","GPIO":[19,18,0,35,36,34,255,255,33,37,32,126,29],"FLAG":15,"BASE":18}
        installCommands: [["WebLog", "2"], // A good idea for dimmers
                        ['SerialLog', '0'],
                        ['setoption3', '1'], // enable MQTT - REQUIRED for these rules to work!
                        ['setoption1', '1'], // restrict to single, double and hold actions (i.e., disable inadvertent reset due to long press)
                        ['setoption32', '8'],     // Number of 0.1 seconds to hold button before sending HOLD action message.
                        ['buttontopic', '0'],   // This enables the below Rule triggers
                        ['Rule1', 'on Button3#state=2 do dimmer + endon on Button2#state=2 do dimmer - endon '],
                        ['Rule1', '+ on Button2#state=3 do dimmer 20 endon on Button3#state=3 do dimmer 100 endon '],
                        ['Rule1', '+ on Button1#state=2 do power1 2 endon on Button1#state=3 do power1 0 endon'],
                        ['Rule1', '1']],
        deviceLink: ''],

        [typeId: 'mj-sd02-dimmer-switch-led',
        comment: 'WITH power status LED active by design',
        name: 'Martin Jerry MJ-SD02 Dimmer Switch',
        template: '{"NAME":"MJ-SD02-LED","GPIO":[19,18,0,33,56,32,255,255,31,37,30,126,29],"FLAG":15,"BASE":18}',
        // Possible alternative: {"NAME":"MJ-SD02","GPIO":[19,18,0,35,36,34,255,255,33,37,32,126,29],"FLAG":15,"BASE":18}
        installCommands: [["WebLog", "2"], // A good idea for dimmers
                        ['SerialLog', '0'],
                        ['setoption3', '1'], // enable MQTT - REQUIRED for these rules to work!
                        ['setoption1', '1'], // restrict to single, double and hold actions (i.e., disable inadvertent reset due to long press)
                        ['setoption32', '8'],     // Number of 0.1 seconds to hold button before sending HOLD action message.
                        ['buttontopic', '0'],   // This enables the below Rule triggers
                        ['LedPower', '1'],
                        ['SetOption31', '0'],
                        ['Rule1', 'on Button3#state=2 do dimmer + endon on Button2#state=2 do dimmer - endon '],
                        ['Rule1', '+ on Button2#state=3 do dimmer 20 endon on Button3#state=3 do dimmer 100 endon '],
                        ['Rule1', '+ on Button1#state=2 do power1 2 endon on Button1#state=3 do power1 0 endon'],
                        ['Rule1', '1']],
        deviceLink: ''],

        //https://templates.blakadder.com/oil_diffuser_550ml.html

        // https://tasmota.github.io/docs/#/devices/Sonoff-RF-Bridge-433pi 
        [typeId: 'sonoff-rf-bridge-parent' , 
        notForUniversal: true,
        comment: 'Functional - Need feedback',
        name: '',
        template: '',
        installCommands: [],
        deviceLink: 'https://templates.blakadder.com/sonoff_RF_bridge.html'],
        
        [typeId: 'rflink-parent' , 
        notForUniversal: true,
        comment: 'Functional - Need feedback',
        name: '',
        template: '',
        installCommands: [],
        deviceLink: 'http://www.rflink.nl/blog2/wiring'],
        
        // Generic Tasmota Devices:
        [typeId: '01generic-device',
        comment: 'Works with most devices' ,
        name: 'Generic Device',
        installCommands: [],
        deviceLink: ''],

        /*[typeId: '01generic-switch-plug',
        comment: 'Works as Plug/Outlet with Alexa' ,
        name: 'Generic Switch/Plug',
        template: '',
        installCommands: [],
        deviceLink: ''],

        [typeId: '01generic-switch-light',
         comment: 'Works as Light with Alexa' ,
        name: 'Generic Switch/Light',
        template: '',
        installCommands: [],
        deviceLink: ''],*/

        [typeId: '01generic-rgb-rgbw-controller-bulb-dimmer', 
        comment: 'RGB+WW+CW should all work properly',
        name: 'Generic RGB/RGBW Controller/Bulb/Dimmer',
        template: '',
        installCommands: [["WebLog", "2"]],
        deviceLink: ''],

        [typeId: '01generic-thp-device' ,
        name: 'Generic Temperature/Humidity/Pressure Device',
        template: '',
        installCommands: [["TempRes", (tempRes == '' || tempRes == null ? "1" : tempRes)]],
        deviceLink: ''],

        /*[typeId: '01generic-dimmer' ,
        name: 'Generic Dimmer',
        template: '',
        installCommands: [["WebLog", "2"]],
        deviceLink: ''],*/
    ]

    TreeMap deviceConfigurationsMap = [:] as TreeMap
    deviceConfigurations.each{
        deviceConfigurationsMap[it["typeId"]] = it
    }
    return deviceConfigurationsMap
}

def getDeviceConfiguration(String typeId) {
    TreeMap deviceConfigurationsMap = getDeviceConfigurations()
    try{
        return deviceConfigurationsMap[typeId]
    } catch(e) {
        log.warn "Failed to retrieve Device Configuration '$typeId': $e"
        return null
    }
}

def getDeviceConfigurationsAsListOption() {
    TreeMap deviceConfigurationsMap = getDeviceConfigurations()
    def items = []
    deviceConfigurationsMap.each { k, v ->
        def label = v["name"]
        if(v.containsKey("comment") && v["comment"].length() > 0) {
            label += " (${v["comment"]})"
        }
        if(!(v.containsKey("notForUniversal") && v["notForUniversal"] == true)) {
            items << ["${v["typeId"]}":"$label"] 
        }
    }
    return items
}

/**
 * --END-- DEVICE CONFIGURATIONS METHODS (helpers-device-configurations)
 */