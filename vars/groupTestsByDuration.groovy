import com.ziften.jenkins.TestsDistributor

def call(jobs) {
    def distributor = new TestsDistributor()

    distributor.run(jobs)
}
