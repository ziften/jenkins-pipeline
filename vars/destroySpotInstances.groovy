import com.ziften.jenkins.SpotInstancesManager

def call(... instances) {
    def manager = SpotInstancesManager.newInstance(this)
    def plainInstances = instances.flatten()

    node('master') {
        manager.destroyMany(plainInstances)
    }
}
