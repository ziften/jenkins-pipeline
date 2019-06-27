import com.ziften.jenkins.SpotInstancesManager

def call(... instances) {
    def manager = SpotInstancesManager.newInstance(this)

    node('AWS-pipe-slave') {
        manager.collectLogsFromMany(instances.flatten())
        archiveArtifacts('ziften_*.log')
        sh('rm -f ziften_*.log')
    }
}
