import com.sap.piper.DownloadCacheUtils
import com.sap.piper.PiperGoUtils
import com.sap.piper.Utils
import groovy.transform.Field

import static com.sap.piper.Prerequisites.checkScript

@Field String STEP_NAME = getClass().getName()
@Field String METADATA_FILE = 'metadata/versioning.yaml'

void call(Map parameters = [:]) {
    List credentials = [
        [type: 'ssh', id: 'gitSshKeyCredentialsId'],
        [type: 'usernamePassword', id: 'gitHttpsCredentialsId', env: ['PIPER_username', 'PIPER_password']],
    ]
    sh "printenv | sort"
    //String dlCacheHost = sh(returnStdout: true, script: 'echo $DL_CACHE_HOSTNAME')
    //String dlCacheNet = sh(returnStdout: true, script: 'echo $DL_CACHE_NETWORK')
    parameters = DownloadCacheUtils.injectDownloadCacheInMavenParameters(parameters.script, parameters)
    List knownHosts = findFiles(glob: '**/known_hosts')
    for (int i=0;i<knownHosts.size();i++){
        println(knownHosts[i])
    }
    //withEnv(["SSH_KNOWN_HOSTS=/var/jenkins_home/.ssh/known_hosts"]) {//, "DL_CACHE_HOSTNAME=$dlCacheHost", "DL_CACHE_NETWORK=$dlCacheNet"]) {
    piperExecuteBin(parameters, STEP_NAME, METADATA_FILE, credentials)
    //}
}
