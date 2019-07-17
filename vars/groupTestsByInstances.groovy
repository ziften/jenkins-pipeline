import com.ziften.jenkins.TestsDistributor

def call(jobs, instancesNumber) {
    TestsDistributor.newInstance(env.PIPE_TYPE).groupByInstances(jobs, instancesNumber)
}
