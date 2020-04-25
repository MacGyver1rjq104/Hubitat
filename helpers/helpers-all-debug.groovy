/**
 * ALL DEBUG METHODS (helpers-all-debug)
 *
 * Helper Debug functions included in all drivers/apps
 */
String configuration_model_debug() {
    if(!isDeveloperHub()) {
        if(!isDriver()) {
            app.removeSetting("logLevel")
            app.updateSetting("logLevel", "0")
        }
        return '''
<configuration>
<Value type="bool" index="debugLogging" label="Enable debug logging" description="" value="false" submitOnChange="true" setting_type="preference" fw="">
<Help></Help>
</Value>
<Value type="bool" index="infoLogging" label="Enable descriptionText logging" description="" value="true" submitOnChange="true" setting_type="preference" fw="">
<Help></Help>
</Value>
</configuration>
'''
    } else {
        if(!isDriver()) {
            app.removeSetting("debugLogging")
            app.updateSetting("debugLogging", "false")
            app.removeSetting("infoLogging")
            app.updateSetting("infoLogging", "false")
        }
        return '''
<configuration>
<Value type="list" index="logLevel" label="Debug Log Level" description="Under normal operations, set this to None. Only needed for debugging. Auto-disabled after 30 minutes." value="100" submitOnChange="true" setting_type="preference" fw="">
<Help>
</Help>
    <Item label="None" value="0" />
    <Item label="Insanely Verbose" value="-1" />
    <Item label="Very Verbose" value="1" />
    <Item label="Verbose" value="10" />
    <Item label="Reports+Status" value="50" />
    <Item label="Reports" value="99" />
    #!include:getSpecialDebugEntry()
</Value>
</configuration>
'''
    }
}

def generate_preferences(configuration_model) {
    def configuration = new XmlSlurper().parseText(configuration_model)
   
    configuration.Value.each {
        if(it.@hidden != "true" && it.@disabled != "true") {
            switch(it.@type) {   
                case "number":
                    input("${it.@index}", "number",
                        title:"${addTitleDiv(it.@label)}" + "${it.Help}",
                        description: makeTextItalic(it.@description),
                        range: "${it.@min}..${it.@max}",
                        defaultValue: "${it.@value}",
                        submitOnChange: it.@submitOnChange == "true",
                        displayDuringSetup: "${it.@displayDuringSetup}")
                    break
                case "list":
                    def items = []
                    it.Item.each { items << ["${it.@value}":"${it.@label}"] }
                    input("${it.@index}", "enum",
                        title:"${addTitleDiv(it.@label)}" + "${it.Help}",
                        description: makeTextItalic(it.@description),
                        defaultValue: "${it.@value}",
                        submitOnChange: it.@submitOnChange == "true",
                        displayDuringSetup: "${it.@displayDuringSetup}",
                        options: items)
                    break
                case "password":
                    input("${it.@index}", "password",
                            title:"${addTitleDiv(it.@label)}" + "${it.Help}",
                            description: makeTextItalic(it.@description),
                            submitOnChange: it.@submitOnChange == "true",
                            displayDuringSetup: "${it.@displayDuringSetup}")
                    break
                case "decimal":
                    input("${it.@index}", "decimal",
                            title:"${addTitleDiv(it.@label)}" + "${it.Help}",
                            description: makeTextItalic(it.@description),
                            range: "${it.@min}..${it.@max}",
                            defaultValue: "${it.@value}",
                            submitOnChange: it.@submitOnChange == "true",
                            displayDuringSetup: "${it.@displayDuringSetup}")
                    break
                case "bool":
                    input("${it.@index}", "bool",
                            title:"${addTitleDiv(it.@label)}" + "${it.Help}",
                            description: makeTextItalic(it.@description),
                            defaultValue: "${it.@value}",
                            submitOnChange: it.@submitOnChange == "true",
                            displayDuringSetup: "${it.@displayDuringSetup}")
                    break
            }
        }
    }
}

/**
 *   --END-- ALL DEBUG METHODS (helpers-all-debug)
 */