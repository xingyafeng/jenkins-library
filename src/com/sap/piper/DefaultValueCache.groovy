package com.sap.piper

import com.sap.piper.MapUtils

@API
class DefaultValueCache implements Serializable {
    private static DefaultValueCache instance

    private Map defaultValues

    private List customDefaults = []

    private DefaultValueCache(Map defaultValues, List customDefaults){
        this.defaultValues = defaultValues
        if(customDefaults) {
            this.customDefaults.addAll(customDefaults)
        }
    }

    static getInstance(){
        return instance
    }

    static createInstance(Map defaultValues, List customDefaults = []){
        instance = new DefaultValueCache(defaultValues, customDefaults)
    }

    Map getDefaultValues(){
        return defaultValues
    }

    static reset(){
        instance = null
    }

    List getCustomDefaults() {
        def result = []
        result.addAll(customDefaults)
        return result
    }

    static void prepare(Script steps, Map parameters = [:]) {
        if(parameters == null) parameters = [:]
        //TODO: Double check if the condition still works, since now we possibly hand an uninitialized list over through parameters.customDefaults
        if(!DefaultValueCache.getInstance() || parameters.customDefaults) {
            def defaultValues = [:]
            List paramCustomDefaults = []
            int customDefaultsInConfig = 0
            if (parameters.customDefaults){
                paramCustomDefaults = parameters.customDefaults
                //steps.println("thats customdefautlts size: ")
                //steps.println(parameters.customDefaultsInConfig)
                if (parameters.numCustomDefaultsInConfig){
                    customDefaultsInConfig = parameters.numCustomDefaultsInConfig
                }
            } else {
                paramCustomDefaults = ['default_pipeline_environment.yml']
                steps.writeFile file: ".pipeline/${paramCustomDefaults[0]}", text: steps.libraryResource(paramCustomDefaults[0])
            }

            List customDefaults = []

            //for (def configFileName : paramCustomDefaults){
            for (int i = 0; i < paramCustomDefaults.size(); i++) {
                if(paramCustomDefaults.size() > 1) steps.echo "Loading configuration file '${paramCustomDefaults[i]}'"

                def configuration = steps.readYaml file: ".pipeline/${paramCustomDefaults[i]}"
                // TODO: check if there is a better solution, especially in case the customDefaults parameter in projectConfig does not point to a URL
                // FIXME: this does not work if URL is handed over as step parameter, better way to seperate the case of customDefaults in project config and handing over as step parameter is needed
                // FIXME contd. e.g., provide starting index of customDefaults from project config, since those will always be at the end of customDefaults list
                // Only files that were not downloaded are saved in customDefaults list, to not have duplicated customDefaults in getConfig Go step,
                // since it considers the configs defined in customDefaults section of project config in addition to the via CLI provided list of customDefaults
                if (i <= paramCustomDefaults.size()-1-customDefaultsInConfig){
                    customDefaults.add(paramCustomDefaults[i])
                }
                defaultValues = MapUtils.merge(
                    MapUtils.pruneNulls(defaultValues),
                    MapUtils.pruneNulls(configuration))
            }
            steps.println("Here customDefautls list as it will be added: ")
            steps.println(customDefaults.toListString())
            DefaultValueCache.createInstance(defaultValues, customDefaults)
        }
    }
}
