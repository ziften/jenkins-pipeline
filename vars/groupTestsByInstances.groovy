import com.ziften.jenkins.TestsDistributor

def call(jobs, instancesNumber) {
    TestsDistributor.newInstance().groupByInstances(jobs, instancesNumber)
}
