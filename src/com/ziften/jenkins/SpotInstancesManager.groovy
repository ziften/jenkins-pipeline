package com.ziften.jenkins

class SpotInstancesManager {
    def steps
    def utils

    SpotInstancesManager(steps) {
        this.steps = steps
        this.utils = new PipelineUtils()
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
        collectFileFromMany(instances, '/var/log/ziften.log')
    }

    def collectFileFromMany(instances, filepath) {
        instances.each { collectFile(it, filepath) }
    }

    def collectFile(instance, filepath) {
        steps.sh("""\
            #!/bin/bash
            filename_with_ext=\$(basename ${filepath})
            filename="\${filename_with_ext%.*}"
            extension="\${filename_with_ext##*.}"
            
            echo "Pulling file from server: ${instance.externalIp}"
            scp -o StrictHostKeyChecking=no -i /etc/salt/qa.pem root@${instance.localIp}:${filepath} ${steps.env.WORKSPACE}/\${filename}_${instance.externalIp}.\${extension}
        """.stripIndent())
    }

    def runCommand(instance, command) {
        steps.echo("Running command on ${instance.localIp}: ${command}")
        steps.sh(script: """\
            #!/bin/bash
            ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o LogLevel=ERROR -i /etc/salt/qa.pem root@${instance.localIp} "${command}"
        """.stripIndent(), returnStdout: true)
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
        "dev-pipe-spot-${utils.generateUUID()}"
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
        steps.sh("salt-cloud -P -p dev-pipe-spot ${hostnames.join(' ')}")
    }

    private def initializeInstances(hostnames) {
        steps.sh("salt -L '${hostnames.join(',')}' state.highstate saltenv=dev-pipe-spot")
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
