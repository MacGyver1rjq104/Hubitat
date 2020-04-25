#!include:getHeaderLicense()

#!include:getDefaultImports()

metadata {
	definition (name: "Testing - Get Driver Runtime Data", namespace: "markusl", author: "Markus Liljergren", vid: "generic-shade") {
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