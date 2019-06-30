import com.ziften.jenkins.DeploymentManager

def call(Map opts, ... instances) {
    def plainInstances = instances.flatten()
    def manager = DeploymentManager.newInstance(this)

    node('AWS-pipe-slave') {
        manager.provisionKeys(plainInstances)
        manager.copyInstaller(plainInstances, opts.installerDir)
        manager.deploy(plainInstances)
        waitZiftenIsUp(plainInstances)
        collectLogsFromInstances(plainInstances)
        copyFileFromInstances('/var/log/ZiftenInstallation.log', plainInstances)
        // TODO: add errors detection and process halt
    }
}
