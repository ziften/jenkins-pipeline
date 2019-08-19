package com.ziften.jenkins.automation

import com.ziften.jenkins.PipelineUtils

class TestsManager {
    def steps
    def utils

    TestsManager(steps) {
        this.steps = steps
        this.utils = new PipelineUtils()
    }

    def run(opts) {
        customRun(opts)
    }

    def runWithAgents(opts) {
        customRun(opts, useSalt: true)
    }

    private def customRun(Map opts = [:], runOpts) {
        def compositeEnv = compositeEnv(steps.env, runOpts)

        steps.echo "[DEBUG] Environment: ${compositeEnv}"

        clone(compositeEnv)
        if (opts.useSalt) {
            setupSaltFiles(compositeEnv)
        }
        execute(compositeEnv)
    }

    private def compositeEnv(baseEnv, customOpts) {
        def testsId = utils.generateUUID()
        def hostPrivateIp = Jenkins.getInstance().getComputer(baseEnv.NODE_NAME).getNode().getLauncher().getHost()

        def defaultEnv = [
                IDP_HOST1           : "idp-${testsId}-1.automation.test",
                IDP_HOST2           : "idp-${testsId}-2.automation.test",
                PARTIAL_TESTS       : true,
                PIPE_ID             : "pipe-${testsId}",
                PRIVATE_IP          : hostPrivateIp,
                SALTAPI_EAUTH       : 'pam',
                SALTAPI_HOST        : hostPrivateIp,
                SALTAPI_PASS        : baseEnv.SALTAPI_CREDENTIALS_PSW,
                SALTAPI_USER        : baseEnv.SALTAPI_CREDENTIALS_USR,
                SALT_TIMEOUT        : 600,
                SPLUNK_EMULATOR_HOST: "splunk-${testsId}.automation.test",
                SSO_TENANT_NAME     : 'sso',
                STORAGE_DB_HOST     : hostPrivateIp,
                THREATLIST_FAKE_HOST: "threatlist-${testsId}.automation.test",
                WORKSPACE           : baseEnv.WORKSPACE, // evaluated here as env.getEnvironment() doesn't return all variables
                Z_SSL_CERT_FILE     : '',
                ZIFTEN_EMULATOR_HOST: "ziften-${testsId}.automation.test",
                ZIFTEN_SERVER       : customOpts.instance.localIp,
                ZIFTEN_SERVER_PUBLIC: customOpts.instance.externalIp
        ]

        def customEnv = optsToEnv(customOpts)

        defaultEnv + baseEnv.getEnvironment() + customEnv
    }

    private def clone(env) {
        steps.checkout([$class                           : 'GitSCM',
                        branches                         : [[name: env.AUTOMATION_BRANCH]],
                        doGenerateSubmoduleConfigurations: false,
                        extensions                       : [[$class   : 'CloneOption',
                                                             noTags   : true,
                                                             reference: '',
                                                             shallow  : true,
                                                             timeout  : 20],
                                                            [$class             : 'SubmoduleOption',
                                                             disableSubmodules  : false,
                                                             parentCredentials  : true,
                                                             recursiveSubmodules: true,
                                                             reference          : '',
                                                             trackingSubmodules : false]],
                        submoduleCfg                     : [],
                        userRemoteConfigs                : [[credentialsId: '97a05e06-750c-4a9b-86c3-f1945e013bce',
                                                             url          : 'git@github.com:ziften/qa-server.git']]])
    }

    private def setupSaltFiles(env) {
        steps.s3CopyArtifact(projectName: env.AGENT_JOB_NAME,
                buildSelector: steps.lastCompleted(),
                filter: '**/ziftenagent-*.deb,**/ziftenagent-*.rpm,release*ziften_installer.msi',
                target: "${env.WORKSPACE}/Ziften/tmp_installers",
                optional: false,
                excludeFilter: '',
                flatten: true)
        steps.withEnv(envToList(env)) {
            steps.sh('''\
                rm -f $WORKSPACE/Ziften/external/salt/automation/linux/linux_agent/ziftenagent.*
                rm -f $WORKSPACE/Ziften/external/salt/states/win/repo-ng/win_agent/ziftenagent.*
                [[ -e $WORKSPACE/Ziften/tmp_installers/ziftenagent-*.deb ]] && cp $WORKSPACE/Ziften/tmp_installers/ziftenagent-*.deb $WORKSPACE/Ziften/external/salt/automation/linux/linux_agent/ziftenagent.deb
                [[ -e $WORKSPACE/Ziften/tmp_installers/ziftenagent-*.rpm ]] && cp $WORKSPACE/Ziften/tmp_installers/ziftenagent-*.rpm $WORKSPACE/Ziften/external/salt/automation/linux/linux_agent/ziftenagent.rpm
                [[ -e $WORKSPACE/Ziften/tmp_installers/ziften_installer.msi ]] && cp $WORKSPACE/Ziften/tmp_installers/ziften_installer.msi $WORKSPACE/Ziften/external/salt/states/win/repo-ng/win_agent/ziftenagent.msi
            '''.stripIndent())
            steps.sh('''\
                rm -f /srv/salt/automation/linux/linux_agent/ziftenagent.*
                rm -f /srv/salt/win/repo-ng/win_agent/ziftenagent.*
                cp -r $WORKSPACE/Ziften/external/salt/states/* /srv/salt/
                cp -r $WORKSPACE/Ziften/external/salt/pillars/* /srv/pillar/
            '''.stripIndent())
        }
    }

    private def execute(env) {
        steps.withEnv(envToList(env)) {
            steps.sh('''\
                #!/bin/bash
                docker-compose -f docker-compose.proxy.yml build
                docker exec nginx-proxy echo 0 &>/dev/null || docker-compose -f docker-compose.proxy.yml up -d

                case "$CUKE_PROFILE" in
                       mitre_notable_events) override_options="-f docker-compose.server.agents.yml" ;;
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

    private def envToList(env) {
        env.collect { k, v -> "${k}=${v}" }
    }

    private def optsToEnv(Map opts) {
        transformKeys(opts, this.&toSnakeCase)
    }

    private def toSnakeCase(text) {
        text.replaceAll(/([A-Z])/, /_$1/).toUpperCase().replaceAll( /^_/, '' )
    }

    private def transformKeys(map, transformation) {
        map.collectEntries { [(transformation.call(it.key)): it.value] }
    }
}
