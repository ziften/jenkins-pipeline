import com.ziften.jenkins.AgentDataImportManager

def call(Map opts, ... instances) {
    def plainInstances = instances.flatten()
    def manager = AgentDataImportManager.newInstance(this)

    node('AWS-CentOS_7-DEV01') {
        def scm = manager.scm(opts.branch)
        def newChanges = scm.GIT_COMMIT != scm.GIT_PREVIOUS_COMMIT
        if (newChanges) {
            manager.build()
            manager.copyArtifacts()
        }

        def threads = opts.threads ?: 10
        def agentCount = opts.agentCount ?: 1
        def loops = opts.loops ?: 1
        importData(plainInstances, tenantName: opts.tenantName, threads: threads, agentCount: agentCount, loops: loops)
    }
}
