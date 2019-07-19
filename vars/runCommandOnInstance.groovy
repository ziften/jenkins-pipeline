import com.ziften.jenkins.automation.SpotInstancesManager

def call(instance, command) {
    def manager = SpotInstancesManager.newInstance(this)

    manager.runCommand(instance, command)
}
