import com.ziften.jenkins.automation.ConfigureManager

def call(Map opts, ... instances) {
    def plainInstances = instances.flatten()
    def manager = ConfigureManager.newInstance(this)

    node('AWS-pipe-slave') {
        manager.configureProperties(plainInstances)
        manager.addTenant(plainInstances, opts.tenantName)
    }
}
