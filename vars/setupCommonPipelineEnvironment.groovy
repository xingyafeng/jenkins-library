import static com.sap.piper.Prerequisites.checkScript

import com.sap.piper.GenerateDocumentation
import com.sap.piper.ConfigurationHelper
import com.sap.piper.Utils
import com.sap.piper.analytics.InfluxData

import groovy.transform.Field

@Field String STEP_NAME = getClass().getName()

@Field Set GENERAL_CONFIG_KEYS = [
    /** */
    'collectTelemetryData'
]

@Field Set STEP_CONFIG_KEYS = []

@Field Set PARAMETER_KEYS = [
    /** Property file defining project specific settings.*/
    'configFile'
]

/**
 * Initializes the [`commonPipelineEnvironment`](commonPipelineEnvironment.md), which is used throughout the complete pipeline.
 *
 * !!! tip
 *     This step needs to run at the beginning of a pipeline right after the SCM checkout.
 *     Then subsequent pipeline steps consume the information from `commonPipelineEnvironment`; it does not need to be passed to pipeline steps explicitly.
 */
@GenerateDocumentation
void call(Map parameters = [:]) {

    handlePipelineStepErrors (stepName: STEP_NAME, stepParameters: parameters) {

        def script = checkScript(this, parameters)
        String configFile = parameters.get('configFile')

        loadConfigurationFromFile(script, configFile)

        List customDefaults = []
        if(parameters.customDefaults in String) {
            customDefaults = [parameters.customDefaults]
        } else if(parameters.customDefaults in List){
            customDefaults = parameters.customDefaults
        }

        if (script.commonPipelineEnvironment.configuration.customDefaults){
            script.commonPipelineEnvironment.configuration.customDefaults.each{
                cd ->
                    customDefaults.add(cd)
            }
        }

        if (customDefaults.size() > 0) {
            int urlCount = 0
            for (def configFileName : customDefaults) {
                String prefixHttp = 'http://'
                String prefixHttps = 'https://'

                if (configFileName.startsWith(prefixHttp) || configFileName.startsWith(prefixHttps)) {
                    String fileName = "customDefaultFromUrl_${urlCount}"
                    String configFilePath = ".pipeline/${fileName}"
                    sh(script: "curl --fail --location --output ${configFilePath} ${fileName}")
                    urlCount += 1

                    //TODO: else if its a lib resource?
                //} else if () {

                    //TODO: else (its a file)?
                } else {
                    println("check for lib resource")
                    String configContent = libraryResource(configFileName)
                    if (configContent) {
                        println("its a lib resource")
                    } else {
                        writeFile file: ".pipeline/${configFileName}", text: readYaml(file: configFileName)
                        println("its a random file")
                    }


                }
            }
        }
        //TODO: put all file handling here, save all customDefaults in .pipeline/ and let defaultValueCache read all customDefaults from .pipeline/
        prepareDefaultValues script: script, customDefaults: customDefaults

        println("customDefaults in step parameters: ")
        println(customDefaults.toListString())

        customDefaults = ['default_pipeline_environment.yml'].plus(customDefaults?:[])

        println("thats customDefaults in setupCPE")
        println(customDefaults.toListString())

        String prefixHttp = 'http://'
        String prefixHttps = 'https://'
        //TODO: Add handling of customDefaults provided as links or other filepaths
        customDefaults.each {
            cd ->
                if(!(cd.startsWith(prefixHttp) || cd.startsWith(prefixHttps))) {
                    writeFile file: ".pipeline/${cd}", text: libraryResource(cd)
                }
        }

        stash name: 'pipelineConfigAndTests', includes: '.pipeline/**', allowEmpty: true



        Map config = ConfigurationHelper.newInstance(this)
            .loadStepDefaults()
            .mixinGeneralConfig(script.commonPipelineEnvironment, GENERAL_CONFIG_KEYS)
            .use()

        (parameters.utils ?: new Utils()).pushToSWA([
            step: STEP_NAME,
            stepParamKey4: 'customDefaults',
            stepParam4: parameters.customDefaults?'true':'false'
        ], config)

        InfluxData.addField('step_data', 'build_url', env.BUILD_URL)
        InfluxData.addField('pipeline_data', 'build_url', env.BUILD_URL)
    }
}

private loadConfigurationFromFile(script, String configFile) {
    if (!configFile) {
        String defaultYmlConfigFile = '.pipeline/config.yml'
        String defaultYamlConfigFile = '.pipeline/config.yaml'
        if (fileExists(defaultYmlConfigFile)) {
            configFile = defaultYmlConfigFile
        } else if (fileExists(defaultYamlConfigFile)) {
            configFile = defaultYamlConfigFile
        }
    }

    // A file passed to the function is not checked for existence in order to fail the pipeline.
    if (configFile) {
        script.commonPipelineEnvironment.configuration = readYaml(file: configFile)
        println("Thats the commenPipelineEnv ocnfig after loading in setup: ")
        println(script.commonPipelineEnvironment.configuration.toMapString())
        println(script.commonPipelineEnvironment.configuration.customDefaults)
        script.commonPipelineEnvironment.configurationFile = configFile
    }
}
