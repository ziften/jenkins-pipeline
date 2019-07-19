import com.ziften.jenkins.automation.TestsDistributor

def call(jobs) {
    def pipeType = (env.PIPE_TYPE == 'DEVELOPMENT' || params.INSTALLER == 'USE_LATEST_DEVELOPMENT_INSTALLER') ? 'DEVELOPMENT' : 'RELEASE'

    TestsDistributor.newInstance(pipeType).groupByDuration(jobs)
}
