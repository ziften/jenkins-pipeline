import com.ziften.jenkins.automation.TestsManager

def call(Map opts) {
    def manager = TestsManager.newInstance(this)

    node('AWS-pipe-slave') {
        manager.runWithAgents(opts)
    }
}
