package com.sap.piper

class PiperGoUtils implements Serializable {

    private static Script steps
    private static Utils utils

    PiperGoUtils(Script steps) {
        this.steps = steps
        this.utils = new Utils()
    }

    PiperGoUtils(Script steps, Utils utils) {
        this.steps = steps
        this.utils = utils
    }

    void unstashPiperBin() {

        if (utils.unstash('piper-bin').size() > 0) return

        if (steps.env.REPOSITORY_UNDER_TEST && steps.env.LIBRARY_VERSION_UNDER_TEST) {
            buildGoBinary(steps, steps.env.REPOSITORY_UNDER_TEST, steps.env.LIBRARY_VERSION_UNDER_TEST)
        } else {
            def libraries = getLibrariesInfo()
            String version
            libraries.each {lib ->
                if (lib.name == 'piper-lib-os') {
                    version = lib.version
                }
            }

            def fallbackUrl = 'https://github.com/SAP/jenkins-library/releases/latest/download/piper_master'
            def piperBinUrl = (version == 'master') ? fallbackUrl : "https://github.com/SAP/jenkins-library/releases/download/${version}/piper"

            boolean downloaded = downloadGoBinary(piperBinUrl)
            if (!downloaded) {
                steps.echo ("Not able to download go binary of Piper for version ${version}. Trying to build it instead.")
                steps.retry(2) {
                    buildGoBinary(steps, 'SAP/jenkins-library', version)
                }

            }
        }

        utils.stashWithMessage('piper-bin', 'failed to stash piper binary', 'piper')
    }

    private buildGoBinary(Script steps, String repository, String libraryVersion){
        steps.echo("Building piper binary.")
        steps.dockerExecute(script: steps, dockerImage: 'golang:1.13', dockerOptions: '-u 0') {
            steps.sh "wget https://github.com/${repository}/archive/${libraryVersion}.tar.gz"
            steps.sh "tar xzf ${libraryVersion}.tar.gz"
            steps.dir("jenkins-library-${libraryVersion}") {
                steps.sh 'XDG_CACHE_HOME=/tmp/.cache CGO_ENABLED=0 go build -tags release -o ../piper . && chmod +x ../piper'
            }
            steps.sh "rm -rf ${libraryVersion}.tar.gz jenkins-library-${libraryVersion}"
        }
    }

    List getLibrariesInfo() {
        return new JenkinsUtils().getLibrariesInfo()
    }

    private boolean downloadGoBinary(url) {

        try {
            def httpStatus = steps.sh(returnStdout: true, script: "curl --insecure --silent --location --write-out '%{http_code}' --output ./piper '${url}'")

            if (httpStatus == '200') {
                steps.sh(script: 'chmod +x ./piper')
                return true
            }
        } catch(err) {
            //nothing to do since error should just result in downloaded=false
            steps.echo "Failed downloading Piper go binary with error '${err}'"
        }
        return false
    }
}
