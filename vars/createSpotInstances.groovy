import com.ziften.jenkins.SpotInstancesManager

def call(number) {
    def manager = SpotInstancesManager.newInstance(this)

    node('master') {
        manager.createMany(number)
    }
}
