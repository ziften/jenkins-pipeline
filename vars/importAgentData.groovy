import com.ziften.jenkins.automation.AgentDataImportManager

def call(Map opts, ... instances) {
    def plainInstances = instances.flatten()
    def manager = AgentDataImportManager.newInstance(this)

    manager.scm(opts.branch)
    manager.build()
    manager.copyArtifacts()

    def threads = opts.threads ?: 10
    def agentCount = opts.agentCount ?: 1
    def loops = opts.loops ?: 1
    manager.importData(plainInstances, tenantName: opts.tenantName, threads: threads, agentCount: agentCount, loops: loops)
}
