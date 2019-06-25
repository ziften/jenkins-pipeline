import com.ziften.jenkins.SpotInstancesManager

def call(instance) {
    def manager = SpotInstancesManager.newInstance(this)

    node('AWS-pipe-slave') {
        manager.collectLogs(instance)
    }
}
