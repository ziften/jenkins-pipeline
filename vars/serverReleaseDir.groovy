def call(majorVersion, minorVersion) {
    node('master') {
        sh(script: "ls /srv/salt/release-archive | grep release-${majorVersion}_${minorVersion} | tail -1 | tr -d '\n'",
                returnStdout: true)
    }
}
