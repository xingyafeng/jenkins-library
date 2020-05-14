import com.sap.piper.DownloadCacheUtils
import groovy.transform.Field

import static com.sap.piper.Prerequisites.checkScript

@Field String STEP_NAME = getClass().getName()
@Field String METADATA_FILE = 'metadata/versioning.yaml'

void call(Map parameters = [:]) {
    final script = checkScript(this, parameters) ?: this
    handlePipelineStepErrors (stepName: STEP_NAME, stepParameters: parameters) {

        parameters = DownloadCacheUtils.injectDownloadCacheInMavenParameters(script, parameters)
        piperExecuteBin parameters, STEP_NAME, "metadata/${STEP_NAME}.yaml", []
    }
}
