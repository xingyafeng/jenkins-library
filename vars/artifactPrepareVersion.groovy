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
    sshagent(['bf6f9f15-362b-4523-acc9-98ff64adc44f']) {
        sh "echo test before pipeexecute bin call"
    }
    withEnv(["SSH_KNOWN_HOSTS=/var/jenkins_home/.ssh/known_hosts"]) {
        piperExecuteBin(parameters, STEP_NAME, METADATA_FILE, credentials)
    }
}
