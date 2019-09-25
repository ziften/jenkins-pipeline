import com.ziften.jenkins.automation.SpotInstancesManager

def call(... instances) {
    def manager = SpotInstancesManager.newInstance(this)

    node('AWS-pipe-slave') {
        manager.collectLogsFromMany(instances.flatten())
        s3Upload(profileName: 'artifacts-s3',
                dontWaitForConcurrentBuildCompletion: true,
                consoleLogLevel: 'INFO',
                pluginFailureResultConstraint: 'FAILURE',
                entries: [[bucket: 'ziften-jenkins-artifacts-dev',
                           sourceFile: "ziften_*.log",
                           selectedRegion: 'us-east-1',
                           noUploadOnFailure: false,
                           uploadFromSlave: true,
                           managedArtifacts: true,
                           useServerSideEncryption: false,
                           flatten: false,
                           gzipFiles: false,
                           keepForever: true,
                           showDirectlyInBrowser: false]],
                userMetadata: [])
        sh('rm -f ziften_*.log')
    }
}
