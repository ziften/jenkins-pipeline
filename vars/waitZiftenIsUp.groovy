def call(... instances) {
    def hostnamesStr = instances.flatten()*.hostname.join(',')

    node('AWS-pipe-slave') {
        sh("salt -L '${hostnamesStr}' -t 660 cmd.script salt://files/wait_ziften_is_up.sh saltenv=dev-pipe-spot")
    }
}
