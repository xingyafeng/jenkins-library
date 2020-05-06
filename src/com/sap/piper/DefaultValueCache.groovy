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
        //TODO: Double check if the condition still works, since now we possible hadn an uninitialized list over through parameters.customDefaults
        if(!DefaultValueCache.getInstance() || parameters.customDefaults) {
            def defaultValues = [:]
            List configFileList = ['default_pipeline_environment.yml']
            if (parameters.customDefaults){
                List paramCustomDefaults = parameters.customDefaults
                configFileList += paramCustomDefaults
            }

            List customDefaults = []
            
            // TODO: Support for customDefaults as simple files not just library resources
            for (def configFileName : configFileList){
                if(configFileList.size() > 1) steps.echo "Loading configuration file '${configFileName}'"

                def configuration = steps.readYaml text: steps.libraryResource(configFileName)
                defaultValues = MapUtils.merge(
                    MapUtils.pruneNulls(defaultValues),
                    MapUtils.pruneNulls(configuration))
            }
            DefaultValueCache.createInstance(defaultValues, customDefaults)
        }
    }

    /*
    static void prepare(Script steps, Map parameters = [:]) {
        if(parameters == null) parameters = [:]
        if(!DefaultValueCache.getInstance() || parameters.customDefaults) {
            def defaultValues = [:]
            def configFileList = ['default_pipeline_environment.yml']

            def customDefaults = parameters.customDefaults


            if(customDefaults in List)
                configFileList += customDefaults

            // TODO: move to setupCPE -> consider custom defaults defined in config.yml
            List configCustomDefaults = steps.commonPipelineEnvironment.configuration.customDefaults ?: []

            steps.println("tahts configcustomdefaults: ")
            //steps.println(configCustomDefaults.toListString())
            steps.println(steps.commonPipelineEnvironment.configuration.toMapString())

            if(configCustomDefaults.size() > 0){
                configFileList += configCustomDefaults
            }

            for (def configFileName : configFileList){
                if(configFileList.size() > 1) steps.echo "Loading configuration file '${configFileName}'"
                String prefixHttp = 'http://'
                String prefixHttps = 'https://'
                Map configuration
                if(configFileName.startsWith(prefixHttp) || configFileName.startsWith(prefixHttps)){
                    String fileName = ${configFileName.substring(configFileName.lastIndexOf('/')+1)}
                    String configFilePath = ".pipeline/${fileName}"
                    steps.sh(script: "curl --fail --location --output ${configFilePath} ${configFileName}")
                    configuration = steps.readYaml file: configFilePath
                    // TODO: use logical files names and remove links from customDefaults list (customDefaults in the list should be relative filepaths, setupCPE copies them to .pipeline/)
                    customDefaults.remove(configFileName)
                    customDefaults += [configFilePath]

                    steps.println("thats the downloaded config: ")
                    steps.println(configuration.toMapString())
                    steps.println("thats customDefaults: ")
                    steps.println(customDefaults.toListString())
                }
                // TODO: add else if that can handle customDefaults which are not library resources
                else {
                    configuration = steps.readYaml text: steps.libraryResource(configFileName)
                }
                defaultValues = MapUtils.merge(
                        MapUtils.pruneNulls(defaultValues),
                        MapUtils.pruneNulls(configuration))
            }
            DefaultValueCache.createInstance(defaultValues, customDefaults)
        }
    }*/
}
