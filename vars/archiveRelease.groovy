def call(Map opts = [:], majorVersion, minorVersion) {
    node('master') {
//        sh("rm -rf *-release-${majorVersion}_${minorVersion}")
        sh(script: """\
                #!/bin/bash
                mkdir -p /srv/salt/release-archive/${opts.patchFolder}/JAR
                cp -vf /tmp/server_installers/automation/release/ziften_release_server_*.${opts.codeBuildNumber}_*.jar /srv/salt/release-archive/${opts.patchFolder}/JAR/
                cp -rvf /srv/salt/server_patches/${opts.patchFolder}/* /srv/salt/release-archive/${opts.patchFolder}/
            """.stripIndent(), label: 'Copying JAR and patch file to release archive')
    }
}
