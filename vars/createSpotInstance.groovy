import com.ziften.jenkins.SpotInstancesManager

def call() {
    def manager = SpotInstancesManager.newInstance(this)

    node('master') {
        manager.createOne()
    }
}
