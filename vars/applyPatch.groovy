def call(Map opts, ... instances) {
    node('master') {
        copyArtifacts(filter: 'props', projectName: 'QA-SERVER-PreparePatch', selector: specific("${opts.preparePatchBuildNumber}"))
        def hostnames = instances.flatten()*.hostname
        def hostsStr = hostnames.join(',')
        def props = readProperties(file: 'props', defaults: [HOSTS: hostsStr]).collect { k, v -> "${k}=${v}" }

        withEnv(props) {
            sh('''\
                #!/bin/bash
                echo "[INFO] Running patch on $HOSTS"
                echo "[INFO] Started at: $(date)"
                set -x
                salt -L $HOSTS test.ping
                salt -L $HOSTS -t 1500 state.sls server_patches.$PATCH_BASE_FOLDER.$(echo $FINAL_PATCH|sed 's/\\.sls//')
            '''.stripIndent())
            if (opts.startZiftenAfterPatch) {
                sh('''\
                    #!/bin/bash
                    echo "[INFO] Starting Ziften services..."
                    salt -L $HOSTS -t 360 service.start ziften.target
                '''.stripIndent())
                waitZiftenIsUp(hostnames)
            }
        }

        archiveArtifacts('props')
    }
}
