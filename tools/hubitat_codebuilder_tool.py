#  Copyright 2019 Markus Liljergren
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.


"""
  Hubitat driver and app developer tool
  WARNING: Do NOT run this script unless you know what it does, it may DELETE your data!
           If you use this code, please contact me so I know there is interest in this!
  NOTE: This is a Work In Progress, feel free to use it, but don't rely on it not changing completely!
"""
# External modules
from pathlib import Path
import logging
import io
from colorama import init, Fore, Style
from os import path
import sys
init()
sys.path.insert(0, "private/tools")

#logging.basicConfig(level=logging.DEBUG,
#    format="%(asctime)s:%(levelname)s:%(message)s")

# Internal modules
from hubitat_hubspider import HubitatHubSpider
from hubitat_codebuilder import HubitatCodeBuilder, HubitatCodeBuilderLogFormatter
from hubitat_codebuilder_tasmota import HubitatCodeBuilderTasmota



# Internal functions
from hubitat_driver_snippets import *
from hubitat_driver_snippets_parser import *
from hubitat_driver_snippets_new_parser import *
from hubitat_driver_snippets_zigbee_parser import *
from hubitat_driver_snippets_metadata import *

# Setup the logger
log = logging.getLogger(__name__)
errors = io.StringIO()
log.setLevel(logging.DEBUG)
log_cb = logging.getLogger(HubitatCodeBuilder.__module__)
log_cb.setLevel(logging.DEBUG)
log_hs = logging.getLogger(HubitatHubSpider.__module__)
log_hs.setLevel(logging.DEBUG)

h = logging.StreamHandler()
h.setLevel(logging.DEBUG)
h.setFormatter(HubitatCodeBuilderLogFormatter(error_beep=True))
ha = logging.StreamHandler(errors)
ha.setLevel(logging.WARN)
ha.setFormatter(HubitatCodeBuilderLogFormatter(error_beep=False))

hhs = logging.StreamHandler()
hhs.setLevel(logging.DEBUG)
hhs.setFormatter(HubitatCodeBuilderLogFormatter(error_beep=True, debug_color=Fore.CYAN, default_color=Fore.MAGENTA))

hhsa = logging.StreamHandler(errors)
hhsa.setLevel(logging.WARN)
hhsa.setFormatter(HubitatCodeBuilderLogFormatter(error_beep=False))

log.addHandler(h)
log.addHandler(ha)
log_cb.addHandler(h)
log_cb.addHandler(ha)
log_hs.addHandler(hhs)
log_hs.addHandler(hhsa)

try:
    from config.driver_list_2nd_hub import driver_files_2nd
    update_2nd_hub_drivers = True
except SyntaxError as e:
    log.error("SyntaxError: 2nd hub config NOT loaded: {}".format(e))
    update_2nd_hub_drivers = False


def getExpandedDriverList(driver_selection, all_drivers):
    driver_files_new = []
    if(driver_selection != None and driver_selection != []):
        for d in driver_selection:
            if(d['id'] != 0 and ('file' in d) == False):
                for d_info in all_drivers:
                    if (d['id'] == d_info['id']):
                        d=d_info
                        break
            if('file' in d):
                driver_files_new.append(d)
    return driver_files_new


# NOTE: All function names use mixedCaps since this is used with Groovy and it makes
#       it less confusing not changing style all the time. 
def main():
    base_repo_url = 'https://github.com/markus-li/Hubitat/blob/development/drivers/expanded/'
    base_raw_repo_url = 'https://raw.githubusercontent.com/markus-li/Hubitat/development/drivers/expanded/'
    app_raw_repo_url = 'https://raw.githubusercontent.com/markus-li/Hubitat/development/apps/expanded/'

    # Get us a Code Builder...
    
    log.debug('Getting started...')
    #HubitatHubSpider.saveConfig('192.168.1.1', 'username', 'password', 'hhs_sample.cfg')
    hhs = HubitatHubSpider(None, 'hubitat_hubspider.cfg')
    # Check the result from login()
    log.debug(hhs.login())

    # By including our namespace, anything we import in this file is available
    # to call by the include tags in the .groovy files when we process them
    cb = HubitatCodeBuilderTasmota(hhs, calling_namespace=sys.modules[__name__], driver_raw_repo_url=base_raw_repo_url,
                app_raw_repo_url=app_raw_repo_url, default_version='v1.0.1.MMDDTb')
    #cb = HubitatCodeBuilderTasmota()
    
    driver_files = [
        # Tasmota drivers WITHOUT their own base-file:
        {'id': 418, 'file': 'tasmota-tuyamcu-wifi-touch-switch-child.groovy', \
         'alternate_output_filename': 'tasmota-tuyamcu-wifi-touch-switch-legacy-child', \
         'alternate_name': 'Tasmota - TuyaMCU Wifi Touch Switch Legacy (Child)', \
         'alternate_namespace': 'tasmota-legacy'},
        {'id': 556, 'file': 'tasmota-sonoff-basic.groovy', \
         'alternate_output_filename': 'tasmota-sonoff-basic-r3', \
         'alternate_name': 'Tasmota - Sonoff Basic R3',
         'deviceLink': 'https://templates.blakadder.com/sonoff_basic_R3.html'},
        {'id': 580, 'file': 'tasmota-tuyamcu-wifi-dimmer.groovy' , \
         'alternate_output_filename': 'tasmota-tuyamcu-ce-wf500d-dimmer', \
         'alternate_name': 'Tasmota - TuyaMCU CE Smart Home WF500D Dimmer (EXPERIMENTAL)', \
         'alternate_template': '{"NAME":"CE WF500D","GPIO":[255,255,255,255,255,255,0,0,255,108,255,107,255],"FLAG":0,"BASE":54}',
         'deviceLink': 'https://templates.blakadder.com/ce_smart_home-WF500D.html',
         'comment': 'WORKING, but need feedback from users'},
        
        {'id': 583, 'file': 'tasmota-generic-wifi-switch-plug.groovy' , \
         'alternate_output_filename': 'tasmota-awp02l-n-plug', \
         'alternate_name': 'Tasmota - AWP02L-N Plug', \
         'alternate_template': '{"NAME":"AWP02L-N","GPIO":[57,0,56,0,0,0,0,0,0,17,0,21,0],"FLAG":1,"BASE":18}',
         'deviceLink': 'https://templates.blakadder.com/hugoai_awp02l-n.html'},
        {'id': 584, 'file': 'tasmota-generic-wifi-switch-plug.groovy' , \
         'alternate_output_filename': 'tasmota-cyyltf-bifans-j23-plug', \
         'alternate_name': 'Tasmota - CYYLTF BIFANS J23 Plug', \
         'alternate_template': '{"NAME":"CYYLTF J23","GPIO":[56,0,0,0,0,0,0,0,21,17,0,0,0],"FLAG":1,"BASE":18}',
         'deviceLink': 'https://templates.blakadder.com/cyyltd_bifans_J23.html'},
        {'id': 585, 'file': 'tasmota-generic-wifi-switch-plug.groovy' , \
         'alternate_output_filename': 'tasmota-gosund-wp3-plug', \
         'alternate_name': 'Tasmota - Gosund WP3 Plug', \
         'alternate_template': '{"NAME":"Gosund WP3","GPIO":[0,0,0,0,17,0,0,0,56,57,21,0,0],"FLAG":0,"BASE":18}',
         'deviceLink': 'https://templates.blakadder.com/gosund_wp3.html'},
        {'id': 586, 'file': 'tasmota-generic-pm-plug.groovy' , \
         'alternate_output_filename': 'tasmota-sk03-pm-outdoor-plug', \
         'alternate_name': 'Tasmota - SK03 Power Monitor Outdoor Plug', \
         'alternate_template': '{"NAME":"SK03 Outdoor","GPIO":[17,0,0,0,133,132,0,0,131,57,56,21,0],"FLAG":0,"BASE":57}',
         'deviceLink': 'https://templates.blakadder.com/SK03_outdoor.html'},
        #{'id': 587, 'file': 'tasmota-generic-pm-plug.groovy' , \
        # 'alternate_output_filename': 'tasmota-aoycocr-x10s-pm-plug', \
        # 'alternate_name': 'Tasmota - Aoycocr X10S Power Monitor Plug', \
        # 'alternate_template': '{"NAME":"Aoycocr X10S","GPIO":[56,0,57,0,21,134,0,0,131,17,132,0,0],"FLAG":0,"BASE":45}',
        # 'deviceLink': 'https://templates.blakadder.com/aoycocr_X10S.html'},
        {'id': 592, 'file': 'tasmota-generic-wifi-switch-plug.groovy' , \
         'alternate_output_filename': 'tasmota-sonoff-sv', \
         'alternate_name': 'Tasmota - Sonoff SV', \
         'alternate_template': '{"NAME":"Sonoff SV","GPIO":[17,255,0,255,255,255,0,0,21,56,255,0,0],"FLAG":1,"BASE":3}',
         'deviceLink': 'https://templates.blakadder.com/sonoff_SV.html'},
        #{'id': 361, 'file': 'tasmota-generic-thp-device.groovy' , \
        # 'alternate_output_filename': 'tasmota-sonoff-th', \
        # 'alternate_name': 'Tasmota - Sonoff TH', \
        # 'alternate_template': '{"NAME":"Sonoff TH","GPIO":[17,255,0,255,255,0,0,0,21,56,255,0,0],"FLAG":0,"BASE":4}',
        # 'deviceLink': 'https://templates.blakadder.com/sonoff_TH.html'},
        #{'id': 547, 'file': 'tasmota-sonoff-powr2.groovy' , \
        # 'alternate_output_filename': 'tasmota-sonoff-pow', \
        # 'alternate_name': 'Tasmota - Sonoff POW', \
        # 'alternate_template': '{"NAME":"Sonoff Pow","GPIO":[17,0,0,0,0,130,0,0,21,132,133,52,0],"FLAG":0,"BASE":6}',
        # 'deviceLink': 'https://templates.blakadder.com/sonoff_Pow.html'},
        #{'id': 359, 'file': 'tasmota-sonoff-powr2.groovy' , \
        # 'alternate_output_filename': 'tasmota-sonoff-s31', \
        # 'alternate_name': 'Tasmota - Sonoff S31', \
        # 'alternate_template': '{"NAME":"Sonoff S31","GPIO":[17,145,0,146,0,0,0,0,21,56,0,0,0],"FLAG":0,"BASE":41}',
        # 'deviceLink': 'https://templates.blakadder.com/sonoff_S31.html'},
        {'id': 643, 'file': 'tasmota-generic-pm-plug-parent.groovy' , \
         'alternate_output_filename': 'tasmota-kmc-4-pm-plug', \
         'alternate_name': 'Tasmota - KMC 4 Power Monitor Plug', \
         'alternate_template': '{"NAME":"KMC 4 Plug","GPIO":[0,56,0,0,133,132,0,0,130,22,23,21,17],"FLAG":0,"BASE":36}',
         'numSwitches': 3, 'deviceLink': 'https://templates.blakadder.com/kmc-4.html'},
        #{'id': 644, 'file': 'tasmota-generic-pm-plug-child.groovy' , \
        # 'alternate_output_filename': 'tasmota-kmc-4-pm-plug-child', \
        # 'alternate_name': 'Tasmota - KMC 4 Power Monitor Plug (Child)'},
        #{'id': 555, 'file': 'tasmota-generic-pm-plug.groovy' , \
        # 'alternate_output_filename': 'tasmota-awp04l-pm-plug', \
        # 'alternate_name': 'Tasmota - AWP04L Power Monitor Plug', \
        # 'alternate_template': '{"NAME":"AWP04L","GPIO":[57,255,255,131,255,134,0,0,21,17,132,56,255],"FLAG":0,"BASE":18}',
        # 'deviceLink': 'https://templates.blakadder.com/awp04l.html'},
        #{'id': 646, 'file': 'tasmota-sonoff-4ch-parent.groovy' , \
        # 'alternate_output_filename': 'tasmota-sonoff-4ch-pro-parent', \
        # 'alternate_name': 'Tasmota - Sonoff 4CH Pro (Parent)', \
        # 'alternate_template': '{"NAME":"Sonoff 4CH Pro","GPIO":[17,255,255,255,23,22,18,19,21,56,20,24,0],"FLAG":0,"BASE":23}',
        # 'comment': 'UNTESTED driver', 'numSwitches': 4,
        # 'deviceLink': 'https://templates.blakadder.com/sonoff_4CH_Pro.html'},
        #{'id': 647, 'file': 'tasmota-generic-pm-plug-child.groovy' , \
        # 'alternate_output_filename': 'tasmota-sonoff-4ch-pro-child', \
        # 'alternate_name': 'Tasmota - Sonoff 4CH Pro (Child)'},

        # Tasmota Drivers WITH their own base-file
        {'id': 548, 'file': 'tasmota-tuyamcu-wifi-touch-switch.groovy' },
        {'id': 549, 'file': 'tasmota-tuyamcu-wifi-touch-switch-child.groovy' },
        #{'id': 550, 'file': 'tasmota-tuyamcu-wifi-touch-switch-child-test.groovy' },
        {'id': 513, 'file': 'tasmota-sonoff-powr2.groovy', 'deviceLink': 'https://templates.blakadder.com/sonoff_Pow_R2.html'},
        {'id': 551, 'file': 'tasmota-sonoff-s2x.groovy', 'comment': 'Works with both Sonoff S20 and S26.',
        'deviceLink': 'https://templates.blakadder.com/sonoff_S20.html'},
        {'id': 554, 'file': 'tasmota-sonoff-mini.groovy', 'deviceLink': 'https://templates.blakadder.com/sonoff_mini.html'},
        {'id': 560, 'file': 'tasmota-sonoff-basic.groovy', 'deviceLink': 'https://templates.blakadder.com/sonoff_basic.html'},
        {'id': 553, 'file': 'tasmota-s120-plug.groovy' },
        #{'id': 559, 'file': 'tasmota-brilliant-bl20925-pm-plug.groovy', 'deviceLink': 'https://templates.blakadder.com/brilliant_BL20925.html'},
        #{'id': 577, 'file': 'tasmota-prime-ccrcwfii113pk-plug.groovy', 'deviceLink': 'https://templates.blakadder.com/prime_CCRCWFII113PK.html'},
        {'id': 590, 'file': 'tasmota-tuyamcu-wifi-dimmer.groovy', 'comment': 'WORKING, but need feedback from users'},
        {'id': 588, 'file': 'tasmota-unbranded-rgb-controller-with-ir.groovy' },
        {'id': 362, 'file': 'tasmota-sonoff-4ch-parent.groovy' , 
         'comment': 'UNTESTED driver',
         'deviceLink': 'https://templates.blakadder.com/sonoff_4CH.html',
         'numSwitches': 4},
        {'id': 645, 'file': 'tasmota-generic-pm-plug-child.groovy' , \
         'alternate_output_filename': 'tasmota-sonoff-4ch-child', \
         'alternate_name': 'Tasmota - Sonoff 4CH (Child)'},

        #{'id': 738, 'file': 'tasmota-znsn-tuyamcu-wifi-curtain-wall-panel.groovy',
        # 'comment': 'NOT GENERIC - read the instructions', },
        # The below one is unused and safe for testing:
        {'id': 587, 'file': 'tasmota-znsn-tuyamcu-wifi-curtain-wall-panel.groovy',
         'comment': 'NOT GENERIC - read the instructions', },
        
        # https://tasmota.github.io/docs/#/devices/Sonoff-RF-Bridge-433pi 
        {'id': 648, 'file': 'tasmota-sonoff-rf-bridge-parent.groovy' , 
         'comment': 'Functional - Need feedback',
         'deviceLink': 'https://templates.blakadder.com/sonoff_RF_bridge.html',
         'numSwitches': 1, 'specialDebugLabel': 'RF Codes', 'childType': 'not_component'},
        
        {'id': 650, 'file': 'tasmota-rflink-parent.groovy' , 
         'comment': 'Functional - Need feedback',
         'deviceLink': 'http://www.rflink.nl/blog2/wiring',
         'numSwitches': 1, 'specialDebugLabel': 'RF Codes', 'childType': 'not_component'},
        #{'id': 651, 'file': 'tasmota-sonoff-rf-bridge-child.groovy' , \
        # 'alternate_output_filename': 'tasmota-rflink-child', \
        # 'alternate_name': 'Tasmota - DO NOT USE RFLink (Child)'},
        {'id': 649, 'file': 'tasmota-rf-ir-switch-toggle-push-child.groovy', 
         'specialDebugLabel': 'Code Learning'},
        {'id': 673, 'file': 'tasmota-rf-ir-motion-sensor-child.groovy', 
         'specialDebugLabel': 'Code Learning'},
        {'id': 674, 'file': 'tasmota-rf-ir-contact-sensor-child.groovy', 
         'specialDebugLabel': 'Code Learning'},
        {'id': 675, 'file': 'tasmota-rf-ir-water-sensor-child.groovy', 
         'specialDebugLabel': 'Code Learning'},
        {'id': 676, 'file': 'tasmota-rf-ir-smoke-detector-child.groovy', 
         'specialDebugLabel': 'Code Learning'},

        # Special sensor drivers:
        {'id': 651, 'file': 'tasmota-sensor-distance.groovy' , 
         'comment': 'UNTESTED driver',
         'deviceLink': 'https://github.com/arendst/Tasmota/wiki/HC-SR04',},
        
        # Generic Tasmota Devices:
        {'id': 552, 'file': 'tasmota-generic-wifi-switch-plug.groovy',
         'comment': 'Works as Plug/Outlet with Alexa' },
        {'id': 769, 'file': 'tasmota-generic-wifi-switch-light.groovy',
         'comment': 'Works as Light with Alexa' },
        {'id': 591, 'file': 'tasmota-generic-rgb-rgbw-controller-bulb-dimmer.groovy', 'comment': 'RGB+WW+CW should all work properly, please report progress' },
        {'id': 578, 'file': 'tasmota-generic-thp-device.groovy' },
        {'id': 558, 'file': 'tasmota-generic-pm-plug.groovy'},
        {'id': 641, 'file': 'tasmota-generic-pm-plug-parent.groovy', 'comment': 'Multi-relay support'},
        {'id': 642, 'file': 'tasmota-generic-pm-plug-child.groovy' },
        {'id': 737, 'file': 'tasmota-generic-wifi-dimmer.groovy' },

        # Universal drivers
        {'id': 865, 'file': 'tasmota-universal-parent.groovy', 'specialDebugLabel': 'descriptionText' },
        
        {'id': 866, 'file': 'tasmota-universal-multi-sensor-child.groovy', 
            'specialDebugLabel': 'descriptionText' },
        {'id': 993, 'file': 'tasmota-universal-fancontrol-child.groovy', 
            'specialDebugLabel': 'descriptionText' },
        {'id': 589, 'file': 'tasmota-universal-curtain-child.groovy', 
            'specialDebugLabel': 'descriptionText' },
        {'id': 359, 'file': 'tasmota-universal-switch-as-contact-sensor-child.groovy', 
            'specialDebugLabel': 'descriptionText' },
        {'id': 361, 'file': 'tasmota-universal-switch-as-motion-sensor-child.groovy', 
            'specialDebugLabel': 'descriptionText' },
        {'id': 555, 'file': 'tasmota-universal-switch-as-water-sensor-child.groovy', 
            'specialDebugLabel': 'descriptionText' },
        {'id': 581, 'file': 'tasmota-universal-plug-outlet-child.groovy', 
            'specialDebugLabel': 'descriptionText' },
        {'id': 582, 'file': 'tasmota-universal-bulb-light-child.groovy', 
            'specialDebugLabel': 'descriptionText' },
        
        {'id': 577, 'file': 'tasmota-universal-ct-rgb-cw-ww-child.groovy', 
            'specialDebugLabel': 'descriptionText' },
        {'id': 646, 'file': 'tasmota-universal-dimmer-child.groovy', 
            'specialDebugLabel': 'descriptionText' },
        {'id': 559, 'file': 'tasmota-universal-metering-dimmer-child.groovy', 
            'specialDebugLabel': 'descriptionText' },
        {'id': 644, 'file': 'tasmota-universal-metering-plug-outlet-child.groovy', 
            'specialDebugLabel': 'descriptionText' },
        {'id': 647, 'file': 'tasmota-universal-metering-bulb-light-child.groovy', 
            'specialDebugLabel': 'descriptionText' },
        

        {'id': 867, 'file': 'tasmota-universal-parent.groovy', 'specialDebugLabel': 'descriptionText',
         'alternate_output_filename': 'tasmota-universal-parent-testing', \
         'alternate_name': 'Tasmota - Universal Parent Testing' },
        {'id': 868, 'file': 'tasmota-universal-multi-sensor-child.groovy', 'specialDebugLabel': 'descriptionText',
         'alternate_output_filename': 'tasmota-universal-multi-sensor-testing-child', \
         'alternate_name': 'Tasmota - Universal Multi Sensor Testing (Child)' },

        {'id': 547, 'file': 'testing-bare-minimum-driver.groovy' },

        # Zigbee
        {'id': 579, 'file': 'zigbee-generic-wifi-switch-plug.groovy' },
        {'id': 801, 'file': 'zigbee-aqara-smart-curtain-motor.groovy', 'version': 'v0.9.1.MMDDb' },

        # Virtual
        {'id': 962, 'file': 'javascript-injection-driver.groovy', 'version': 'v0.1.0.MMDDb' },

        # The following can be overwritten: 
    ]
    # Future devices to implement support for:
    # https://templates.blakadder.com/maxcio_400ml_diffuser.html
    # https://templates.blakadder.com/ytf_ir_bridge.html

    # Future devices to MAYBE implement support for, if someone asks... :
    # https://templates.blakadder.com/sonoff_SC.html

    # RF Bridge functions to implement:
    # * Motion Sensor (receive) - DONE
    # * Button (receive) - DONE
    # * RF Remote??? (receive) - DONE
    # * Door/Window Sensor (receive) - DONE
    # * Smoke Alarm (receive) - DONE
    # * Water Sensor (receive) - DONE
    # * On/Off signal transmitter (send)
    # * Custom Signal Transmitter??? (send)

    # IR Bridge functions to implement
    # * Custom IR Remote Control Button (receive, each button as a Child) 
    # * Specific IR Remote Control with full button support (SEND, many buttons per Child)
    # * IR Repeater (in on one device, out on another or same, select output device in Child?)
    # * Send Custom IR command (implement as toggle/button?)
    # 

    expected_num_drivers = len(driver_files)
    
    # Example driver: https://github.com/hubitat/HubitatPublic/blob/master/examples/drivers/GenericZigbeeRGBWBulb.groovy
    # RGB Example: https://github.com/damondins/hubitat/blob/master/Tasmota%20RGBW%20LED%20Light%20Bulb/Tasmota%20RGBW%20LED%20Light%20Bulb

    # As long as we have an id, we can just supply that here instead of the whole config...
    # 651 left over from RF Link Child
    driver_files_testing = [
        #{'id': 866},
        {'id': 359}, # Switch as Contact Sensor Child
        {'id': 361}, # Switch as Motion Sensor Child
        {'id': 555}, # Switch as Water Sensor Child
        {'id': 993}, # Fan Control Child
        
        {'id': 577}, # CT/RGB/RGB+CW+WW Child
        {'id': 646}, # Dimmer Child
        {'id': 559}, # Metering Dimmer Child
        {'id': 589}, # Curtain Child
        {'id': 644}, {'id': 647}, # Metering Children
        {'id': 581}, {'id': 582}, # Switch Children
        {'id': 547},

        {'id': 962}, # Javascript Injection Driver
        #{'id': 867}, {'id': 868},  # Universal Drivers TESTING
        {'id': 865}, {'id': 866}, # Universal Drivers RELEASE
        #{'id': 865}
        #{'id': 801}, #{'id': 579},  # Zigbee drivers
    #     {'id':587},  # Wifi Curtain Wall Panel
    #    {'id':590},
    #    {'id':651},    # Sensor - Distance
    #    {'id':649}, {'id':673}, {'id':674}, {'id':675}, {'id':676}, # RF/IR Children
    #    {'id':591},
    #    {'id':548},  # TuyaMCU Touch Switches
        # These three following rows represent most of the drivers personally used by markus-li
    #    {'id':548},{'id':549},{'id':513},{'id':592},{'id':418},{'id':554},{'id':590},{'id':557},
    #    {'id':649},{'id':673},{'id':674},{'id':675},{'id':676},{'id':641},{'id':642},{'id':591},
    #    {'id':578},{'id':738},{'id':553},{'id':648},{'id':650},{'id':651},{'id':551},{'id':588},
    #   {'id':641},{'id':642},
    #   {'id':650},{'id':651},  # RFLink
    #    {'id':648},  # Sonoff RF Bridge
    #    {'id':865}, {'id':550}, # Universal Parent + Universal Children
    #    {'id':589}, {'id':591},
    #     {'id':551},{'id':578}, {'id':362}, {'id':645}, {'id':590}, {'id':588}, 
    #    {'id': 0, 'file': 'tasmota-generic-thp-device.groovy' , \
    #     'alternate_output_filename': 'tasmota-sonoff-th', \
    #     'alternate_name': 'WRONG Tasmota - Sonoff TH', \
    #     'alternate_module': '4'},
    ]
    # 222, 145, 144, 163, 161
    # 
    expected_num_drivers = 1

    driver_files_testing = getExpandedDriverList(driver_files_testing, driver_files)
    #print(driver_files)

    # Setting id to 0 will have the Code Builder submit the driver as a new one, don't forget to note the ID 
    # and put it in before submitting again. Also, if there are code errors when submitting a NEW file
    # there's no error messages explaining why, only that it failed... When UPDATING code, any failure messages
    # normally seen in the web code editor, will be seen in the build console.

    #log.debug('Testing to create a new driver...')
    #new_id = hhs.push_new_driver(cb.getBuildDir('driver') / 'tasmota-unbranded-rgb-controller-with-ir-expanded.groovy')

    #cb.clearChecksums()

    generic_drivers = []
    specific_drivers = []

    parent_drivers = []
    child_drivers = []
    
    used_driver_list = cb.expandGroovyFilesAndPush(driver_files_testing, code_type='driver')
    #print(used_driver_list)
    #print(driver_files_testing)
    for d in used_driver_list:
        if(used_driver_list[d]['name'].startswith('Tasmota - ')):
            # Get all Info
            newD = used_driver_list[d].copy()
            # Add the rest of what we know about this ID:
            for d_info in driver_files_testing:
                if (used_driver_list[d]['id'] == d_info['id']):
                    #log.debug('d_info: {}'.format(d_info))
                    newD.update(d_info)
                    break
            # Modify it a little bit
            newD.update({'name': used_driver_list[d]['name'][10:], 
                        'file': used_driver_list[d]['file'].stem + used_driver_list[d]['file'].suffix,
                        'filestem': used_driver_list[d]['file'].stem})
            newD['filestem'] = newD['filestem'].replace('-expanded', '')
            newD['wikiname'] = newD['name'].replace(' ', '-').replace('/', '-')
            #log.debug('d_info 2: {}'.format(d_info))
            # We will modify these later, make sure we have COPIES
            if(newD['name'].startswith('Generic')):
                generic_drivers.append(newD.copy())
            else:
                specific_drivers.append(newD.copy())
            if(newD['name'].find("Parent") != -1):
                parent_drivers.append(newD.copy())
            else:
                child_drivers.append(newD.copy())
  
    # Make Driver Lists if we have all files we expect...
    if(len(used_driver_list) >= expected_num_drivers):
        log.info('Making the driver list file...')
        my_driver_list_1 = [
            {'name': '', 
             'format': 'These are the currently available drivers (updated: %(asctime)s):\n\n'},
            {'name': 'Generic Drivers',
             'format': '**%(name)s**\n',
             'items': generic_drivers,
             'items_format': [
                 "* [%(name)s](%(base_url)s%(file)s) (%(comment)s) - Import URL: [RAW](%(base_raw_url)s%(file)s)\n",
                 "* [%(name)s](%(base_url)s%(file)s) - Import URL: [RAW](%(base_raw_url)s%(file)s)\n",]},
            {'name': '\n', 'format': '%(name)s'},
            {'name': 'Specific Drivers',
             'format': '**%(name)s**\n',
             'items': specific_drivers,
             # Make sure the format requesting the most amount of data is first in the list
             'items_format': [
                 "* [%(name)s](%(base_url)s%(file)s) %(version)s (%(comment)s) - Import URL: [RAW](%(base_raw_url)s%(file)s) - [Device Model Info](%(deviceLink)s)\n",
                 "* [%(name)s](%(base_url)s%(file)s) %(version)s - Import URL: [RAW](%(base_raw_url)s%(file)s) - [Device Model Info](%(deviceLink)s)\n",
                 "* [%(name)s](%(base_url)s%(file)s) %(version)s (%(comment)s) - Import URL: [RAW](%(base_raw_url)s%(file)s)\n",
                 "* [%(name)s](%(base_url)s%(file)s) %(version)s - Import URL: [RAW](%(base_raw_url)s%(file)s)\n"]}]
        #cb.makeDriverListDoc(my_driver_list_1, output_file='DRIVERLIST_OLD', filter_function=cb.makeDriverListFilter,
        #    base_data={'base_url': base_repo_url, 'base_raw_url': base_raw_repo_url})
        full_header = '<tr><td><b>Device</b></td><td><b>Comment</b></td><td><b>Import&nbsp;URL</b></td><td><b>Model&nbsp;Info</b></td></tr>'
        my_driver_list_1b = [
            {'name': '', 
             'format': 'These are the currently available drivers (updated: %(asctime)s):\n\n'},
            {'name': '<table>\n', 'format': '%(name)s'},
            {'name': 'Generic&nbsp;Drivers',
             'format': '<tr><th><b>%(name)s</b></th><th></th><th></th><th></th></tr>' + full_header + '\n',
             'items': generic_drivers,
             'items_format': [
                 "<tr><td><a href=\"%(base_url)s%(file)s\">%(name)s</a></td><td>%(comment)s</td><td><a href=\"%(base_raw_url)s%(file)s\">RAW</a></td><td></td></tr>\n",
                 "<tr><td><a href=\"%(base_url)s%(file)s\">%(name)s</td><td></td><td><a href=\"%(base_raw_url)s%(file)s\">RAW</a></td><td></td></tr>\n",]},
            {'name': '\n', 'format': '%(name)s'},
            {'name': '</table><table>\n', 'format': '%(name)s'},
            {'name': 'Specific&nbsp;Drivers',
             'format': '<tr><th><b>%(name)s</b></th><th></th><th></th><th></th></tr>' + full_header + '\n',
             'items': specific_drivers,
             # Make sure the format requesting the most amount of data is first in the list
             'items_format': [
                 "<tr><td><a href=\"%(base_url)s%(file)s\">%(name)s</td><td>%(comment)s</td><td><a href=\"%(base_raw_url)s%(file)s\">RAW</a></td><td><a href=\"%(deviceLink)s\">Link</a></td></tr>\n",
                 "<tr><td><a href=\"%(base_url)s%(file)s\">%(name)s</td><td></td><td><a href=\"%(base_raw_url)s%(file)s\">RAW</a></td><td><a href=\"%(deviceLink)s\">Link</a></td></tr>\n",
                 "<tr><td><a href=\"%(base_url)s%(file)s\">%(name)s</td><td>%(comment)s</td><td><a href=\"%(base_raw_url)s%(file)s\">RAW</a></td><td></td></tr>\n",
                 "<tr><td><a href=\"%(base_url)s%(file)s\">%(name)s</td><td></td><td><a href=\"%(base_raw_url)s%(file)s\">RAW</a></td><td></td></tr>\n"]},
            {'name': '</table>\n', 'format': '%(name)s'},]
        #cb.makeDriverListDoc(my_driver_list_1b, filter_function=cb.makeDriverListFilter,
        #    base_data={'base_url': base_repo_url, 'base_raw_url': base_raw_repo_url})
        my_driver_list_2 = [
            {'name': 'Driver List', 'format': '# %(name)s \n'},
            {'name': '', 
             'format': 'These are the currently available drivers (updated: %(asctime)s):\n\n'},
            {'name': 'Parent Device Drivers',
             'format': '**%(name)s**\n',
             'items': parent_drivers,
             'items_format': [
                 "* [%(name)s](%(base_url)s%(file)s) %(version)s (%(comment)s)\n",
                 "* [%(name)s](%(base_url)s%(file)s) %(version)s\n",]},
            {'name': '\n', 'format': '%(name)s'},
            {'name': 'Child Device Drivers',
             'format': '**%(name)s**\n',
             'items': child_drivers,
             # Make sure the format requesting the most amount of data is first in the list
             'items_format': [
                 #"* [%(name)s](%(base_url)s%(file)s) (%(comment)s) - [Device Model Info](%(deviceLink)s)\n", 
                 #"* [%(name)s](%(base_url)s%(file)s) - [Device Model Info](%(deviceLink)s)\n", 
                 "* [%(name)s](%(base_url)s%(file)s) %(version)s (%(comment)s)\n", 
                 "* [%(name)s](%(base_url)s%(file)s) %(version)s\n"]}]
        cb.makeDriverListDoc(my_driver_list_2, output_file='DRIVERLIST.md', filter_function=cb.makeDriverListFilter, 
            base_data={'base_url': base_repo_url, 'base_raw_url': base_raw_repo_url})
        print(parent_drivers)
        my_driver_list_table = [
            {'name': '', 
             'format': 'These are the currently available drivers (updated: %(asctime)s):\n\n'},
            {'name': 'Tasmota Parent Device Drivers',
             'format': '**%(name)s**\n\n' + \
                       '| Name | URL | RAW URL | Version | Comment |\n' + \
                       '| --- | --- | --- | --- | --- |\n',
             'items': parent_drivers,
             'items_format': [
                 "| [%(name)s](https://github.com/markus-li/Hubitat/wiki/[Driver-List]-Tasmota-%(wikiname)s) | [URL](%(base_url)s%(file)s) | [RAW](%(base_raw_url)s%(file)s) | %(version)s | %(comment)s |\n",
                 "| [%(name)s](https://github.com/markus-li/Hubitat/wiki/[Driver-List]-Tasmota-%(wikiname)s) | [URL](%(base_url)s%(file)s) | [RAW](%(base_raw_url)s%(file)s) | %(version)s | |\n",]},
            {'name': '\n', 'format': '%(name)s'},
            {'name': 'Tasmota Child Device Drivers',
             'format': '**%(name)s**\n\n' + \
                       '| Name | URL | RAW URL | Version | Comment |\n' + \
                       '| --- | --- | --- | --- | --- |\n',
             'items': child_drivers,
             # Make sure the format requesting the most amount of data is first in the list
             'items_format': [
                 "| [%(name)s](https://github.com/markus-li/Hubitat/wiki/[Driver-List]-Tasmota-%(wikiname)s) | [URL](%(base_url)s%(file)s) | [RAW](%(base_raw_url)s%(file)s) | %(version)s | | %(comment)s |\n",
                 "| [%(name)s](https://github.com/markus-li/Hubitat/wiki/[Driver-List]-Tasmota-%(wikiname)s) | [URL](%(base_url)s%(file)s) | [RAW](%(base_raw_url)s%(file)s) | %(version)s | | |\n",]}]
        cb.makeDriverListDoc(my_driver_list_table, output_file='../Hubitat.wiki/Driver-List/Driver-List.md', filter_function=cb.makeDriverListFilter, 
            base_data={'base_url': base_repo_url, 'base_raw_url': base_raw_repo_url})
        for d in parent_drivers + child_drivers:
            output_file = "../Hubitat.wiki/Driver-List/[Driver-List]-Tasmota-" + d['wikiname'] + '.md'
            print(output_file)
            if(not path.exists(output_file)):
                with open (output_file, "w") as wd:
                    wd.write('**Tasmota ' + d['name'] + '**')
                    wd.write('''
                    
***Commands***

***Capabilities***

***Comments***
''')
        if(update_2nd_hub_drivers):
            # Get the 2nd hub driver list id-assignments
            id_map = {}
            for d_2nd in driver_files_2nd:
                id_map[d_2nd['original_id']] = d_2nd['id']
            for d in generic_drivers + specific_drivers:
                if(d['id'] in id_map):
                    d['id_2nd'] = id_map[d['id']]
                else:
                    d['id_2nd'] = -1
            my_driver_list_3 = [
                {'name': 'driver_files_2nd = [', 'format': '%(name)s \n'},
                {'name': '', 'format': '  %(name)s\n'},
                {'name': 'DO NOT MODIFY ANYTHING EXCEPT "id", THIS FILE IS AUTOGENERATED!', 'format': '  # %(name)s\n'},
                {'name': '', 'format': '  %(name)s\n'},
                {'name': 'Tasmota - Generic Drivers',
                'format': '  # %(name)s\n',
                'items': generic_drivers,
                'items_format': [
                    "  {'id': %(id_2nd)d, 'original_id': %(id)d, 'name': '%(name)s'},\n",]},
                {'name': '\n', 'format': '%(name)s'},
                {'name': 'Tasmota - Specific Device Drivers',
                'format': '  # %(name)s\n',
                'items': specific_drivers,
                'items_format': [
                    "  {'id': %(id_2nd)d, 'original_id': %(id)d, 'name': '%(name)s'},\n",]},
                {'name': ']', 'format': '%(name)s'},]
            cb.makeDriverListDoc(my_driver_list_3, output_file='private/tools/config/driver_list_2nd_hub.py')
        else:
            log.warn("Can't update the 2nd hub drivers! Check other errors/warnings for details...")
    else:
        log.info("SKIPPING making of the driver list file since we don't have enough drivers in the list...")
    #print('Generic drivers: ' + str(generic_drivers))
    #print('Specific drivers: ' + str(specific_drivers))
    #pp.pprint(used_driver_list)
    

    
    app_files = [
        #{'id': 97, 'file': 'tasmota-connect.groovy' },
        # 163 is available for re-use
        #{'id': 163, 'file': 'tasmota-connect-test.groovy' },
        {'id': 289, 'file': 'tasmota-device-manager.groovy' },
    ]

    cb.setUsedDriverList(used_driver_list)
    filtered_app_files = []
    for a in app_files:
        if(a['id'] != 97):
            filtered_app_files.append(a)
        if(a['id'] != 0 and len(used_driver_list) >= expected_num_drivers):
            filtered_app_files.append(a)
            log.info('Found ' + str(len(used_driver_list)) + ' driver(s)...')
            log.debug("Just found App ID " + str(id))
        else:
            if(a['id'] == 0):
                log.info("Not making App updates since this app has no ID set yet! Skipped updating App with path: '" + str(a['file']) + "'")
            else:
                log.info("Not ready for App updates! Only " + str(len(used_driver_list)) + " driver(s) currently active! Skipped updating App ID " + str(a['id']))
    used_app_list = cb.expandGroovyFilesAndPush(filtered_app_files, code_type='app')

    #cb.expandGroovyFile('tasmota-sonoff-powr2.groovy', expanded_dir)
    #hhs.push_driver_code(513, cb.getOutputGroovyFile('tasmota-sonoff-powr2.groovy', expanded_dir))
    
    #hhs.logout()
    if(len(cb.driver_new)>0):
        log.warning('These new drivers were created: \n{}'.format(cb.driver_new))
    else:
        log.info('No new drivers where created!')
    if(len(cb.app_new)>0):
        log.warn('These new apps were created: \n{}'.format(cb.app_new))
    else:
        log.info('No new apps where created!')

    log.info('Current Default Version Number: {}'.format(getDriverVersion(driverVersionSpecial=cb.default_version)))
    
    contents=errors.getvalue()
    if(len(contents) > 0):
        print('ERRORS and/or WARNINGS occured during this run:')
        print(contents)
    else:
        log.info('No ERRORS or WARNINGS occured during this run :)')
    errors.close()

    cb.saveChecksums()
    hhs.save_session()

if(Path('DEVELOPER').exists()):
    main()