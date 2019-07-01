package com.ziften.jenkins

class TestsManager {
    def steps
//    def utils

    TestsManager(steps) {
        this.steps = steps
//        this.utils = new PipelineUtils()
    }

    def run(Map opts, instance, cukeProfile) {
//        def testsId = utils.generateUUID()
//        def pipeId = "pipe-${testsId}"

        def env = ["ADDITIONAL_FLAGS=${opts.additionalFlags}",
                   "AUTOMATION_TENANT_NAME=${opts.tenantName}",
                   "CUKE_PROFILE=${cukeProfile}",
                   "IDP_HOST1=idp-${testsId}-1.automation.test",
                   "IDP_HOST2=idp-${testsId}-2.automation.test",
                   "PARTIAL_TESTS=true",
                   "PIPE_ID=pipe-${opts.testsId}",
                   "PRIVATE_IP=${opts.privateIp}",
                   "SPLUNK_EMULATOR_HOST=splunk-${testsId}.automation.test",
                   "SSO_TENANT_NAME=sso",
                   "STORAGE_DB_HOST=${opts.privateIp}",
                   "TESTS_ID=${opts.testsId}",
                   "THREATLIST_FAKE_HOST=threatlist-${testsId}.automation.test",
                   "ZIFTEN_EMULATOR_HOST=ziften-${testsId}.automation.test",
                   "ZIFTEN_SERVER=${instance.localIp}"]

        steps.withEnv(env) {
            steps.sh('''\
                #!/bin/sh               
                docker-compose -f docker-compose.proxy.yml build
                docker exec nginx-proxy echo 0 &>/dev/null || docker-compose -f docker-compose.proxy.yml up -d

                case "$CUKE_PROFILE" in
                       splunk_tests) override_options="-f docker-compose.server.splunk.yml" ;;
                          sso_tests) override_options="-f docker-compose.server.sso.yml" ;;
                   threatlist_tests) override_options="-f docker-compose.server.threatlist.yml" ;;
                                  *) override_options="" ;;
                esac
                
                docker-compose -p $PIPE_ID -f docker-compose.server.yml $override_options build
                docker-compose -p $PIPE_ID -f docker-compose.server.yml $override_options up --abort-on-container-exit
                exit_code=$?
                echo "[DEBUG] exit_code=${exit_code}"
                docker-compose -p $PIPE_ID -f docker-compose.server.yml $override_options down

                if [ $exit_code -gt 0 ]; then exit 1; fi
            '''.stripIndent())
        }
    }
}
