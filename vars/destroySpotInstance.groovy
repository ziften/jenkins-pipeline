import com.ziften.jenkins.SpotInstancesManager

def call(instance) {
    def manager = SpotInstancesManager.newInstance(this)

    node('master') {
        manager.destroyOne(instance)
    }
}
