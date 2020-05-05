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
        if(!DefaultValueCache.getInstance() || parameters.customDefaults) {
            def defaultValues = [:]
            def configFileList = ['default_pipeline_environment.yml']
            def customDefaults = parameters.customDefaults

            if(customDefaults in String)
                customDefaults = [customDefaults]
            if(customDefaults in List)
                configFileList += customDefaults

            // consider custom defaults defined in config.yml
            List configCustomDefaults = steps.commonPipelineEnvironment.configuration.customDefaults ?: []
            if(configCustomDefaults.size() > 0){
                configFileList += configCustomDefaults
            }

            for (def configFileName : configFileList){
                if(configFileList.size() > 1) steps.echo "Loading configuration file '${configFileName}'"
                String prefixHttp = 'http://'
                String prefixHttps = 'https://'
                Map configuration
                if(configFileName.startsWith(prefixHttp) || configFileName.startsWith(prefixHttps)){
                    String configFilePath = ".pipeline/${configFileName.substring(configFileName.lastIndexOf('/'))}"
                    steps.sh(script: "curl --fail --location --output ${configFilePath} ${configFileName}")
                    configuration = steps.readYaml file: configFilePath
                    customDefaults += [configFilePath]
                    println("thats the downloaded config: ")
                    println(configuration.toMapString())
                    println("thats customDefaults: ")
                    println(customDefaults.toListString())
                }
                else {
                    configuration = steps.readYaml text: steps.libraryResource(configFileName)
                }
                defaultValues = MapUtils.merge(
                        MapUtils.pruneNulls(defaultValues),
                        MapUtils.pruneNulls(configuration))
            }
            DefaultValueCache.createInstance(defaultValues, customDefaults)
        }
    }
}
