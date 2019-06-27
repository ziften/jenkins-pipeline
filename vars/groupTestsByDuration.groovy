import com.ziften.jenkins.TestsDistributor

def call(jobs) {
    TestsDistributor.newInstance().groupByDuration(jobs)
}
