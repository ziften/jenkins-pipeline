import com.ziften.jenkins.TestsManager

def call(Map opts, instance, cukeProfile) {
    def manager = TestsManager.newInstance(this)

    node('AWS-pipe-slave') {
        manager.run(instance, cukeProfile,
                additionalFlags: opts.additionalFlags,
                tenantName: opts.tenantName,
                privateIp: opts.privateIp)
    }
}
