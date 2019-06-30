import com.ziften.jenkins.ConfigureManager

def call(automationTenantName, ... instances) {
    def plainInstances = instances.flatten()
    def manager = ConfigureManager.newInstance(this)

    node('AWS-pipe-slave') {
        manager.configureProperties(plainInstances)
        manager.addTenant(instances, automationTenantName)
    }
}
