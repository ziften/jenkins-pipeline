import com.ziften.jenkins.automation.SpotInstancesManager

def call() {
    def manager = SpotInstancesManager.newInstance(this)

    node('master') {
        manager.createOne()
    }
}
