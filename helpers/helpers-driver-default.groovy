/**
 * DRIVER DEFAULT METHODS (helpers-driver-default)
 *
 * General Methods used in ALL drivers except some CHILD drivers
 * Though some may have no effect in some drivers, they're here to
 * maintain a general structure
 */

// Since refresh, with any number of arguments, is accepted as we always have it declared anyway, 
// we use it as a wrapper
// All our "normal" refresh functions take 0 arguments, we can declare one with 1 here...
void refresh(cmd) {
    deviceCommand(cmd)
}
// Call order: installed() -> configure() -> updated() -> initialize() -> refresh()
// Calls installed() -> [configure() -> [updateNeededSettings(), updated() -> [updatedAdditional(), initialize() -> refresh() -> refreshAdditional()], installedAdditional()]
def installed() {
	logging("installed()", 100)
    
    try {
        // Used by certain types of drivers, like Tasmota Parent drivers
        installedPreConfigure()
    } catch (MissingMethodException e) {
        // ignore
    }
	//configure()
    try {
        // In case we have some more to run specific to this Driver
        installedAdditional()
    } catch (MissingMethodException e) {
        // ignore
    }
}

// Call order: installed() -> configure() -> updated() -> initialize() -> refresh()
def configure() {
    logging("configure()", 100)
    if(isDriver()) {
        // Do NOT call updateNeededSettings() here!
        try {
            return configureAdditional()
        } catch (MissingMethodException e) {
            updated()
        }
        try {
            // Run the getDriverVersion() command
            getDriverVersion()
        } catch (MissingMethodException e) {
            // ignore
        }
    }
}

void configureDelayed() {
    runIn(10, "configure")
    runIn(30, "refresh")
}

/**
 * --END-- DRIVER DEFAULT METHODS (helpers-driver-default)
 */