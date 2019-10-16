import com.ziften.jenkins.automation.SpotInstancesManager

def call(... instances) {
    def manager = SpotInstancesManager.newInstance(this)

    node('AWS-pipe-slave') {
        manager.collectLogsFromMany(instances.flatten())
        archiveArtifacts('*_ziften.log')
        sh('rm -f *_ziften.log')
    }
}
