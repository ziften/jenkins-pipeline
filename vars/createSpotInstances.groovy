import com.ziften.jenkins.automation.SpotInstancesManager

def call(number) {
    def manager = SpotInstancesManager.newInstance(this)

    node('master') {
        manager.createMany(number)
    }
}
