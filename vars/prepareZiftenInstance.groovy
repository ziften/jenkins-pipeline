def call(Map opts) {
    def instance = createSpotInstance()
    echo("Fresh Host: ${instance.hostname}\n Local IP: ${instance.localIp}\n External IP: ${instance.externalIp}")

    deployZiften(instance, installerDir: opts.installerDir)
    configureZiften(instance, tenantName: opts.tenantName)
    node('AWS-CentOS_7-DEV01') {
        dir('agent-data-import') {
            importAgentData(instance, branch: opts.automationBranch, tenantName: opts.tenantName)
        }
    }

    return instance
}
