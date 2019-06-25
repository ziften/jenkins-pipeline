package com.ziften.jenkins

class SpotInstancesManager {
    def steps

    SpotInstancesManager(steps) {
        this.steps = steps
    }

    def createMany(number) {
        def hosts = generateHosts(number)

        provisionInstances(hosts)
        initializeInstances(hosts)

        hosts.collect { wrapInstance(getLocalIp(it), getExternalIp(it), it) }
    }

    def destroyMany(instances) {
        def instancesToCollectFrom = instances.findAll { it.collectLogs }*.instance
        collectLogsFromMany(instancesToCollectFrom)

        def hosts = instances*.instance*.hostname
        destroyInstances(hosts)

        if (instancesToCollectFrom) {
            steps.archiveArtifacts('ziften_*.log')
            steps.sh('rm -f ziften_*.log')
        }
    }

    def createOne() {
        createMany(1).first()
    }

    def destroyOne(instance) {
        destroyMany([instance])
    }

    def collectLogsFromMany(instances) {
        instances.each { collectLogs(it) }
    }

    def collectLogs(instance) {
        steps.sh("""\
            #!/bin/bash
            echo "Pulling logs from server: ${instance.externalIp}"
            scp -i /etc/salt/qa.pem -o StrictHostKeyChecking=no root@${instance.localIp}:/var/log/ziften.log ${steps.env.WORKSPACE}/ziften_${instance.externalIp}.log
        """.stripIndent())
    }

    private def wrapInstance(localIp, externalIp, hostname) {
        new SpotInstance(localIp: localIp, externalIp: externalIp, hostname: hostname)
    }

    private def generateHosts(number) {
        (1..number).inject([]) { result, i ->
            result << generateHost()
        }
    }

    private def generateHost() {
        "dev-pipe-spot-${generateUUID()}"
    }

    private def generateUUID() {
        UUID.randomUUID().toString()
    }

    private def provisionInstances(hostnames) {
        def hostnamesStr = hostnames.join(' ')

        steps.sh("salt-cloud -P -p dev-pipe-spot ${hostnamesStr}")
    }

    private def initializeInstances(hostnames) {
        def hostnamesStr = hostnames.join(',')

        steps.sh("salt -L '${hostnamesStr}' state.highstate saltenv=dev-pipe-spot")
    }

    private def getLocalIp(hostname) {
        steps.sh(script: "salt ${hostname} grains.get ipv4 | awk '/- 172/ {printf \$NF}'", returnStdout: true)
    }

    private def getExternalIp(hostname) {
        steps.sh(script: "salt ${hostname} cmd.run \"curl http://checkip.amazonaws.com --silent\"|tail -1|awk '{printf \$NF}'", returnStdout: true)
    }

    private def destroyInstances(hostnames) {
        def hostnamesStr = hostnames.join(' ')

        steps.sh("""\
            #!/bin/bash
            echo "Killing VMs: ${hostnamesStr}"
            salt-cloud -y -d -P ${hostnamesStr}
        """.stripIndent())
    }
}
