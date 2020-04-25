/**
 *  Copyright 2020 Markus Liljergren
 *
 *  Version: v0.1.0.0421
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
	definition (name: "Testing - Get Driver Runtime Data", namespace: "markusl", author: "Markus Liljergren", vid: "generic-shade", importUrl: "https://raw.githubusercontent.com/markus-li/Hubitat/development/drivers/expanded/testing-get-driver-runtime-data-expanded.groovy") {
        capability "Refresh"
        capability "Initialize"

        command "unscheduleTasks"
	}
	
    preferences {        
	}
}

void refresh() {
     log.debug("refresh()")
     this.class.declaredFields.each {
        log.debug("class.declaredFields: $it")
    }
    this.properties.each {
        log.debug("properties: $it")
    }
    this.class.methods.each {
        log.debug("class.methods: $it")
    }
    this.class.fields.each {
        log.debug("class.fields: $it")
    }
    this.getProperties().each {
        log.debug("getProperties(): $it")
    }
    log.debug("getAllEntitlements(): ${getAllEntitlements()}")
    log.debug("getCommands(): ${getCommands()}")
    log.debug("getTiles(): ${getTiles()}")
}

void initialize() {
    log.debug("initialize()")
    makeSchedule()
}

void installed() {
    log.debug("installed()")
    makeSchedule()
}

void updated() {
    log.debug("updated()")
    makeSchedule()
}

void makeSchedule() {
    log.debug("makeSchedule()")
    // https://www.freeformatter.com/cron-expression-generator-quartz.html
    schedule("16 3 2/12 * * ? *", scheduledTask)
}

void unscheduleTasks() {
    log.debug("unscheduleTasks()")
    unschedule()
}

void scheduledTask() {
    log.debug("scheduledTask()")
}