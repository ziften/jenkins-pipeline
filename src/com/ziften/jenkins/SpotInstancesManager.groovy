package com.ziften.jenkins

class SpotInstancesManager {
    def steps

    SpotInstancesManager(steps) {
        this.steps = steps
    }

    def createMany(number) {
        def hosts = prepareInstances(number)

//        fixDeadInstances(hosts)

        hosts.collect { wrapInstance(getLocalIp(it), getExternalIp(it), it) }
    }

    def destroyMany(instances) {
        def hosts = instances*.hostname
        destroyInstances(hosts)
    }

    def createOne() {
        createMany(1).first()
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

    private def prepareInstances(number) {
        def hosts = generateHosts(number)

        provisionInstances(hosts)
        initializeInstances(hosts)

        hosts
    }

    private fixDeadInstances(hosts) {
        def deadHosts = selectDeadHosts(hosts)

        if (deadHosts) {
            steps.echo "Found ${deadHosts.size()} dead instances. Replacing them with new..."
            replaceDeadInstances(hosts, deadHosts)
        }
    }

    private def selectDeadHosts(hosts) {
        hosts.findAll { host ->
            !(getLocalIp(host) ==~ /172\.\d{1,3}\.\d{1,3}\.\d{1,3}/)
        }
    }

    private def replaceDeadInstances(allHosts, deadHosts) {
        allHosts.removeAll(deadHosts)

        def newHosts = prepareInstances(deadHosts.size())
        allHosts.addAll(newHosts)
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
