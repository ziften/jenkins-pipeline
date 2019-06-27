import com.ziften.jenkins.SpotInstancesManager

def call(... instances) {
    def manager = SpotInstancesManager.newInstance(this)

    node('master') {
        manager.destroyMany(instances)
    }
}
