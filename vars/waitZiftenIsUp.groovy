def call(Map opts = [:], ... instances) {
    def hostnamesStr = instances.flatten()*.hostname.join(',')
    def upTimeout = opts.timeout ?: 180
    def saltTimeout = upTimeout + 60

    node('AWS-pipe-slave') {
        sh(script: "salt -L '${hostnamesStr}' -t ${saltTimeout} cmd.script salt://files/wait_ziften_is_up.sh ${upTimeout} saltenv=dev-pipe-spot",
                label: 'Waiting Ziften is up on all instances')
    }
}
