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
from colorama import init, Fore, Style
import sys
init()

#logging.basicConfig(level=logging.DEBUG,
#    format="%(asctime)s:%(levelname)s:%(message)s")

# Internal modules
from hubitat_hubspider import HubitatHubSpider
from hubitat_codebuilder import HubitatCodeBuilder, HubitatCodeBuilderLogFormatter
from hubitat_codebuilder_tasmota import HubitatCodeBuilderTasmota

# Internal functions
from hubitat_driver_snippets import *
from hubitat_driver_snippets_parser import *

# Setup the logger
log = logging.getLogger(__name__)
log.setLevel(logging.DEBUG)
log_cb = logging.getLogger(HubitatCodeBuilder.__module__)
log_cb.setLevel(logging.DEBUG)
log_hs = logging.getLogger(HubitatHubSpider.__module__)
log_hs.setLevel(logging.DEBUG)
h = logging.StreamHandler()
h.setLevel(logging.DEBUG)
h.setFormatter(HubitatCodeBuilderLogFormatter(error_beep=True))
hhs = logging.StreamHandler()
hhs.setLevel(logging.DEBUG)
hhs.setFormatter(HubitatCodeBuilderLogFormatter(error_beep=True, debug_color=Fore.CYAN, default_color=Fore.MAGENTA))
log.addHandler(h)
log_cb.addHandler(h)
log_hs.addHandler(hhs)


# NOTE: All function names use mixedCaps since this is used with Groovy and it makes
#       it less confusing not changing style all the time. 
def main():
    # Get us a Code Builder...
    
    log.debug('Getting started...')
    #HubitatHubSpider.saveConfig('192.168.1.1', 'username', 'password', 'hhs_sample.cfg')
    hhs = HubitatHubSpider(None, 'hubitat_hubspider.cfg')
    # Check the result from login()
    log.debug(hhs.login())

    # By including our namespace, anything we import in this file is available
    # to call by the include tags in the .groovy files when we process them
    cb = HubitatCodeBuilderTasmota(hhs, calling_namespace=sys.modules[__name__])
    #cb = HubitatCodeBuilderTasmota()
    
    #log.debug(code_version)

    driver_files = [
        # Drivers without their own base-file:
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
         'deviceLink': 'https://templates.blakadder.com/ce_smart_home-WF500D.html'},
        {'id': 581, 'file': 'tasmota-generic-wifi-switch-plug.groovy' , \
         'alternate_output_filename': 'tasmota-ce-la-2-w3-wall-outlet', \
         'alternate_name': 'Tasmota - CE Smart Home LA-2-W3 Wall Outlet', \
         'alternate_template': '{"NAME":"CE LA-2-W3","GPIO":[255,255,255,255,157,17,0,0,21,255,255,255,255],"FLAG":15,"BASE":18}',
         'deviceLink': 'https://templates.blakadder.com/ce_smart_home_LA-2-W3.html'},
        {'id': 582, 'file': 'tasmota-generic-wifi-switch-plug.groovy' , \
         'alternate_output_filename': 'tasmota-ce-lq-2-w3-wall-outlet', \
         'alternate_name': 'Tasmota - CE Smart Home LQ-2-W3 Wall Outlet', \
         'alternate_template': '{"NAME":"CE LQ-2-W3","GPIO":[255,255,255,255,255,17,255,255,21,255,255,255,255],"FLAG":15,"BASE":18}',
         'deviceLink': 'https://templates.blakadder.com/ce_smart_home_LQ-2-W3.html'},
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
        {'id': 587, 'file': 'tasmota-generic-pm-plug.groovy' , \
         'alternate_output_filename': 'tasmota-aoycocr-x10s-pm-plug', \
         'alternate_name': 'Tasmota - Aoycocr X10S Power Monitor Plug', \
         'alternate_template': '{"NAME":"Aoycocr X10S","GPIO":[56,0,57,0,21,134,0,0,131,17,132,0,0],"FLAG":0,"BASE":45}',
         'deviceLink': 'https://templates.blakadder.com/aoycocr_X10S.html'},
        {'id': 589, 'file': 'tasmota-generic-rgb-rgbw-controller-bulb-dimmer.groovy',
         'alternate_output_filename': 'tasmota-brilliant-20699-rgbw-bulb', \
         'alternate_name': 'Tasmota - Brilliant 20699 800lm RGBW Bulb', \
         'alternate_template': '{"NAME":"Brilliant20699","GPIO":[0,0,0,0,141,140,0,0,37,142,0,0,0],"FLAG":0,"BASE":18}',
         'deviceLink': 'https://templates.blakadder.com/brilliant_20699.html'},
        {'id': 592, 'file': 'tasmota-generic-wifi-switch-plug.groovy' , \
         'alternate_output_filename': 'tasmota-sonoff-sv', \
         'alternate_name': 'Tasmota - Sonoff SV', \
         'alternate_template': '{"NAME":"Sonoff SV","GPIO":[17,255,0,255,255,255,0,0,21,56,255,0,0],"FLAG":1,"BASE":3}',
         'deviceLink': 'https://templates.blakadder.com/sonoff_SV.html'},
        {'id': 361, 'file': 'tasmota-generic-thp-device.groovy' , \
         'alternate_output_filename': 'tasmota-sonoff-th', \
         'alternate_name': 'Tasmota - Sonoff TH', \
         'alternate_template': '{"NAME":"Sonoff TH","GPIO":[17,255,0,255,255,0,0,0,21,56,255,0,0],"FLAG":0,"BASE":4}',
         'deviceLink': 'https://templates.blakadder.com/sonoff_TH.html'},
        {'id': 547, 'file': 'tasmota-sonoff-powr2.groovy' , \
         'alternate_output_filename': 'tasmota-sonoff-pow', \
         'alternate_name': 'Tasmota - Sonoff POW', \
         'alternate_template': '{"NAME":"Sonoff Pow","GPIO":[17,0,0,0,0,130,0,0,21,132,133,52,0],"FLAG":0,"BASE":6}',
         'deviceLink': 'https://templates.blakadder.com/sonoff_Pow.html'},
        {'id': 359, 'file': 'tasmota-sonoff-powr2.groovy' , \
         'alternate_output_filename': 'tasmota-sonoff-s31', \
         'alternate_name': 'Tasmota - Sonoff S31', \
         'alternate_template': '{"NAME":"Sonoff S31","GPIO":[17,145,0,146,0,0,0,0,21,56,0,0,0],"FLAG":0,"BASE":41}',
         'deviceLink': 'https://templates.blakadder.com/sonoff_S31.html'},
        {'id': 643, 'file': 'tasmota-generic-pm-plug-parent.groovy' , \
         'alternate_output_filename': 'tasmota-kmc-4-pm-plug', \
         'alternate_name': 'Tasmota - KMC 4 Power Monitor Plug', \
         'alternate_template': '{"NAME":"KMC 4 Plug","GPIO":[0,56,0,0,133,132,0,0,130,22,23,21,17],"FLAG":0,"BASE":36}',
         'numSwitches': 3, 'deviceLink': 'https://templates.blakadder.com/kmc-4.html'},
        {'id': 644, 'file': 'tasmota-generic-pm-plug-child.groovy' , \
         'alternate_output_filename': 'tasmota-kmc-4-pm-plug-child', \
         'alternate_name': 'Tasmota - KMC 4 Power Monitor Plug (Child)'},
        {'id': 555, 'file': 'tasmota-generic-pm-plug.groovy' , \
         'alternate_output_filename': 'tasmota-awp04l-pm-plug', \
         'alternate_name': 'Tasmota - AWP04L Power Monitor Plug', \
         'alternate_template': '{"NAME":"AWP04L","GPIO":[57,255,255,131,255,134,0,0,21,17,132,56,255],"FLAG":0,"BASE":18}',
         'deviceLink': 'https://templates.blakadder.com/awp04l.html'},
        {'id': 646, 'file': 'tasmota-sonoff-4ch-parent.groovy' , \
         'alternate_output_filename': 'tasmota-sonoff-4ch-pro-parent', \
         'alternate_name': 'Tasmota - Sonoff 4CH Pro (Parent)', \
         'alternate_template': '{"NAME":"Sonoff 4CH Pro","GPIO":[17,255,255,255,23,22,18,19,21,56,20,24,0],"FLAG":0,"BASE":23}',
         'comment': 'UNTESTED driver', 'numSwitches': 4,
         'deviceLink': 'https://templates.blakadder.com/sonoff_4CH_Pro.html'},
        {'id': 647, 'file': 'tasmota-generic-pm-plug-child.groovy' , \
         'alternate_output_filename': 'tasmota-sonoff-4ch-pro-child', \
         'alternate_name': 'Tasmota - Sonoff 4CH Pro (Child)'},

        # Drivers WITH their own base-file
        {'id': 548, 'file': 'tasmota-tuyamcu-wifi-touch-switch.groovy' },
        {'id': 549, 'file': 'tasmota-tuyamcu-wifi-touch-switch-child.groovy' },
        {'id': 550, 'file': 'tasmota-tuyamcu-wifi-touch-switch-child-test.groovy' },
        {'id': 513, 'file': 'tasmota-sonoff-powr2.groovy', 'deviceLink': 'https://templates.blakadder.com/sonoff_Pow_R2.html'},
        {'id': 551, 'file': 'tasmota-sonoff-s2x.groovy', 'comment': 'Works with both Sonoff S20 and Sonoff S26.',
        'deviceLink': 'https://templates.blakadder.com/sonoff_S20.html'},
        {'id': 554, 'file': 'tasmota-sonoff-mini.groovy', 'deviceLink': 'https://templates.blakadder.com/sonoff_mini.html'},
        {'id': 560, 'file': 'tasmota-sonoff-basic.groovy', 'deviceLink': 'https://templates.blakadder.com/sonoff_basic.html'},
        {'id': 552, 'file': 'tasmota-generic-wifi-switch-plug.groovy' },
        {'id': 553, 'file': 'tasmota-s120-plug.groovy' },
        {'id': 557, 'file': 'tasmota-ykyc-001-pm-plug.groovy' },
        {'id': 559, 'file': 'tasmota-brilliant-bl20925-pm-plug.groovy', 'deviceLink': 'https://templates.blakadder.com/brilliant_BL20925.html'},
        {'id': 577, 'file': 'tasmota-prime-ccrcwfii113pk-plug.groovy', 'deviceLink': 'https://templates.blakadder.com/prime_CCRCWFII113PK.html'},
        {'id': 558, 'file': 'tasmota-generic-pm-plug.groovy'},
        {'id': 578, 'file': 'tasmota-generic-thp-device.groovy' },
        {'id': 590, 'file': 'tasmota-tuyamcu-wifi-dimmer.groovy'},
        {'id': 588, 'file': 'tasmota-unbranded-rgb-controller-with-ir.groovy' },
        {'id': 591, 'file': 'tasmota-generic-rgb-rgbw-controller-bulb-dimmer.groovy' },
        {'id': 641, 'file': 'tasmota-generic-pm-plug-parent.groovy', 'comment': 'Multi-relay support'},
        {'id': 642, 'file': 'tasmota-generic-pm-plug-child.groovy' },
        {'id': 362, 'file': 'tasmota-sonoff-4ch-parent.groovy' , 
         'comment': 'UNTESTED driver',
         'deviceLink': 'https://templates.blakadder.com/sonoff_4CH.html',
         'numSwitches': 4},
        {'id': 645, 'file': 'tasmota-generic-pm-plug-child.groovy' , \
         'alternate_output_filename': 'tasmota-sonoff-4ch-child', \
         'alternate_name': 'Tasmota - Sonoff 4CH (Child)'},

        # Zigbee
        {'id': 579, 'file': 'zigbee-generic-wifi-switch-plug.groovy' },

        # The following can be overwritten: 
    ]
    # Future devices to implement support for:
    # https://templates.blakadder.com/sonoff_RF_bridge.html
    # https://templates.blakadder.com/maxcio_400ml_diffuser.html
    # https://templates.blakadder.com/ytf_ir_bridge.html

    # Still not fully functional (or BROKEN):
    # https://templates.blakadder.com/ce_smart_home-WF500D.html


    base_repo_url = 'https://github.com/markus-li/Hubitat/blob/master/drivers/expanded/'
    base_raw_repo_url = 'https://raw.githubusercontent.com/markus-li/Hubitat/master/drivers/expanded/'
    expected_num_drivers = len(driver_files)
    
    # Example driver: https://github.com/hubitat/HubitatPublic/blob/master/examples/drivers/GenericZigbeeRGBWBulb.groovy
    # RGB Example: https://github.com/damondins/hubitat/blob/master/Tasmota%20RGBW%20LED%20Light%20Bulb/Tasmota%20RGBW%20LED%20Light%20Bulb

    
    
    # As long as we have an id, we can just supply that here instead of the whole config...
    driver_files_testing = [
        
    #     {'id':551},{'id':578}, {'id':362}, {'id':645}, {'id':590}, {'id':588}, 
    #    {'id': 0, 'file': 'tasmota-generic-thp-device.groovy' , \
    #     'alternate_output_filename': 'tasmota-sonoff-th', \
    #     'alternate_name': 'WRONG Tasmota - Sonoff TH', \
    #     'alternate_module': '4'},
    ]
    
    #expected_num_drivers = 1

    if(driver_files_testing != None and driver_files_testing != []):
        driver_files_new = []
        for d in driver_files_testing:
            if(d['id'] != 0 and ('file' in d) == False):
                for d_info in driver_files:
                    if (d['id'] == d_info['id']):
                        d=d_info
                        break
            if('file' in d):
                driver_files_new.append(d)
        driver_files = driver_files_new
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
    
    used_driver_list = cb.expandGroovyFilesAndPush(driver_files, code_type='driver')
    for d in used_driver_list:
        if(used_driver_list[d]['name'].startswith('Tasmota - ')):
            # Get all Info
            newD = used_driver_list[d].copy()
            # Add the rest of what we know about this ID:
            for d_info in driver_files:
                if (used_driver_list[d]['id'] == d_info['id']):
                    #log.debug('d_info: {}'.format(d_info))
                    newD.update(d_info)
                    break
            # Modify it a little bit
            newD.update({'name': used_driver_list[d]['name'][10:], 'file': used_driver_list[d]['file'].stem + used_driver_list[d]['file'].suffix})
            #log.debug('d_info 2: {}'.format(d_info))
            if(newD['name'].startswith('Generic')):
                generic_drivers.append(newD)
            else:
                specific_drivers.append(newD)
    
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
                 "* [%(name)s](%(base_url)s%(file)s) (%(comment)s) - Import URL: [RAW](%(base_raw_url)s%(file)s) - [Device Info](%(deviceLink)s)\n",
                 "* [%(name)s](%(base_url)s%(file)s) - Import URL: [RAW](%(base_raw_url)s%(file)s) - [Device Info](%(deviceLink)s)\n",
                 "* [%(name)s](%(base_url)s%(file)s) (%(comment)s) - Import URL: [RAW](%(base_raw_url)s%(file)s)\n",
                 "* [%(name)s](%(base_url)s%(file)s) - Import URL: [RAW](%(base_raw_url)s%(file)s)\n"]}]
        cb.makeDriverListDoc(my_driver_list_1, filter_function=cb.makeDriverListFilter,
            base_data={'base_url': base_repo_url, 'base_raw_url': base_raw_repo_url})
        my_driver_list_2 = [
            {'name': 'Driver List', 'format': '#%(name)s# \n\n'},
            {'name': '', 
             'format': 'These are the currently available drivers (updated: %(asctime)s):\n\n'},
            {'name': 'Tasmota - Generic Drivers',
             'format': '**%(name)s**\n',
             'items': generic_drivers,
             'items_format': [
                 "* [%(name)s](%(base_url)s%(file)s) (%(comment)s)\n",
                 "* [%(name)s](%(base_url)s%(file)s)\n",]},
            {'name': '\n', 'format': '%(name)s'},
            {'name': 'Tasmota - Specific Device Drivers',
             'format': '**%(name)s**\n',
             'items': specific_drivers,
             # Make sure the format requesting the most amount of data is first in the list
             'items_format': [
                 "* [%(name)s](%(base_url)s%(file)s) (%(comment)s) - [Device Info](%(deviceLink)s)\n", 
                 "* [%(name)s](%(base_url)s%(file)s) - [Device Info](%(deviceLink)s)\n", 
                 "* [%(name)s](%(base_url)s%(file)s) (%(comment)s)\n", 
                 "* [%(name)s](%(base_url)s%(file)s)\n"]}]
        cb.makeDriverListDoc(my_driver_list_2, output_file='DRIVERLIST.md', filter_function=cb.makeDriverListFilter, 
            base_data={'base_url': base_repo_url, 'base_raw_url': base_raw_repo_url})
    else:
        log.info("SKIPPING making of the driver list file since we don't have enough drivers in the list...")
    #print('Generic drivers: ' + str(generic_drivers))
    #print('Specific drivers: ' + str(specific_drivers))
    #pp.pprint(used_driver_list)
    
    app_files = [
        {'id': 97, 'file': 'tasmota-connect.groovy' },
        {'id': 163, 'file': 'tasmota-connect-test.groovy' },
    ]

    cb.setUsedDriverList(used_driver_list)
    filtered_app_files = []
    for a in app_files:
        if(a['id'] != 97 and a['id'] != 163):
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
    cb.saveChecksums()
    hhs.save_session()

if(Path('DEVELOPER').exists()):
    main()