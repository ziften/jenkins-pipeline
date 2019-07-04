import com.ziften.jenkins.DeploymentManager

def call(Map opts, ... instances) {
    def plainInstances = instances.flatten()
    def manager = DeploymentManager.newInstance(this)

    manager.provisionKeys(plainInstances)
    manager.copyInstaller(plainInstances, opts.installerDir)

    try {
        manager.deploy(plainInstances)
    } catch(e) {
        collectLogsFromInstances(plainInstances)
        copyFileFromInstances('/var/log/ZiftenInstallation.log', plainInstances)

        throw e
    }
    // TODO: add errors detection and process halt
}
