def call(... instances) {
    def hostnamesStr = instances.flatten()*.hostname.join(',')

    node('AWS-pipe-slave') {
        sh(script: "salt -L '${hostnamesStr}' -t 240 cmd.script salt://files/wait_ziften_is_up.sh 180 saltenv=dev-pipe-spot",
                label: 'Waiting Ziften is up on all instances')
    }
}
