package com.ziften.jenkins.automation

class AgentDataImportManager {
    def steps

    AgentDataImportManager(steps) {
        this.steps = steps
    }

    def scm(branch) {
        steps.checkout([$class: 'GitSCM',
                branches: [[name: branch]],
                doGenerateSubmoduleConfigurations: false,
                extensions: [[$class: 'CloneOption',
                        noTags: false,
                        reference: '',
                        shallow: true,
                        timeout: 10],
                        [$class: 'SubmoduleOption',
                                disableSubmodules: false,
                                parentCredentials: false,
                                recursiveSubmodules: true,
                                reference: '',
                                trackingSubmodules: false]],
                submoduleCfg: [],
                userRemoteConfigs: [[credentialsId: '97a05e06-750c-4a9b-86c3-f1945e013bce',
                        url: 'git@github.com:ziften/qa-server.git']]])
    }

    def build() {
        steps.echo('Building JMeter project')
        steps.withMaven(maven: 'maven', options: [
                steps.artifactsPublisher(disabled: true),
                steps.junitPublisher(disabled: true),
                steps.openTasksPublisher(disabled: true)
        ]) {
            steps.sh('mvn -f jmeter-agent-emulator/pom.xml clean package')
        }
    }

    def copyArtifacts() {
        steps.sh("""\
            #!/bin/bash
            # copy latest artifacts
            csv_zip=jmeter-agent-emulator/src/test/resources/csv.zip

            rm -rf csv/*
            unzip \$csv_zip -d .
        """.stripIndent())
    }

    def importData(Map opts, instances) {
        def env = ["COMMA_SEPARATED_IPS=${ipsStr(instances)}", "AUTOMATION_TENANT_NAME=${opts.tenantName}",
               "THREADS=${opts.threads}", "AGENT_COUNT=${opts.agentCount}", "LOOPS=${opts.loops}"]
        steps.withEnv(env) {
            steps.sh(script: '''\
                #!/bin/bash
                run_and_validate() (
                    tenant_name=$1
                    ip=$2
                    
                    test_plan="jmeter-agent-emulator/src/test/resources/testplan/ZiftenAgentsPatch.jmx"
                    csvs="./csv"
                    threads=$THREADS
                    agent_count=$AGENT_COUNT
                    loops=$LOOPS
                    
                    domain="${tenant_name}.ziften.local"
                    site_id=$(echo -n $domain | md5sum | awk '{print $1}')
                    
                    echo "Munging to '${tenant_name}' tenant on ${ip}"
                    insert_dir="insert_${site_id}"
    
                    run_jmeter() {
                        export PATH=/etc/alternatives/jre_1.8.0/bin:$PATH
                        JMETER_HOME=/usr/local/apache-jmeter-3.2
                        emulator_jar=jmeter-agent-emulator/target/agent-emulator-0.0.1.jar
    
                        $JMETER_HOME/bin/jmeter.sh -n -t $test_plan -Jsearch_paths=$emulator_jar -JZIFTEN_SERVER=$ip -Jsite_id=$site_id -Jcsvs=$csvs -Jthreads=$threads -Jagent_count=$agent_count -Jloops=$loops -Jreport=jmeter_run.xml
                    }
    
                    validate() {
                        base_bsql_dir=/opt/ziften/lib
    
                        # wait for Consumer starts moving bsql-s to insert dir
                        sleep 30
    
                        echo "[INFO] Waiting for .bsql appear in the insert dir"
                        ssh -i /root/.ssh/QualityAssurance.pem -o StrictHostKeyChecking=no root@$ip "\\
                        elapsed=0; \\
                        sleep_time=5; \\
                        until [[ -n \\"\\$(find ${base_bsql_dir}/insert/${insert_dir}/ -maxdepth 1 -type f -name \\"*.bsql\\" -print -quit)\\" || \\$elapsed -gt 300 ]]; do sleep \\$sleep_time; let elapsed=elapsed+sleep_time; done; \\
                        if [ ! -n \\"\\$(find ${base_bsql_dir}/insert/${insert_dir}/ -maxdepth 1 -type f -name \\"*.bsql\\" -print -quit)\\" ]; then exit 101; fi"
    
                        echo "[INFO] Waiting for all .bsql moved out from the insert dir"
                        ssh -i /root/.ssh/QualityAssurance.pem -o StrictHostKeyChecking=no root@$ip "\\
                        elapsed=0; \\
                        sleep_time=5; \\
                        while [[ \\$elapsed -le 1800 && -n \\"\\$(find ${base_bsql_dir}/insert/${insert_dir}/ -maxdepth 1 -type f -name \\"*.bsql\\" -print -quit)\\" ]]; do sleep \\$sleep_time; let elapsed=elapsed+sleep_time; done; \\
                        if [ -n \\"\\$(find ${base_bsql_dir}/insert/${insert_dir}/ -maxdepth 1 -type f -name \\"*.bsql\\" -print -quit)\\" ]; then exit 102; fi"
                    }
    
                    validate &
                    validate_job_pid=$!
    
                    run_jmeter
                    jmeter_exit_code=$?
    
                    wait ${validate_job_pid}
                    validate_exit_code=$?
    
                    if [ $jmeter_exit_code -ne 0 ]; then
                        echo "[ERROR] JMeter returns non-zero exit code: ${jmeter_exit_code}"
                        exit $jmeter_exit_code
                    elif [ $validate_exit_code -ne 0 ]; then
                        if [ $validate_exit_code -eq 101 ]; then
                            echo "[ERROR] .bsql files do not appear in the insert dir"
                        elif [ $validate_exit_code -eq 102 ]; then
                            echo "[ERROR] .bsql files are still present in the insert dir"
                        else
                            echo "[ERROR] Validate returns non-zero exit code: ${validate_exit_code}"
                        fi
                        exit $validate_exit_code
                    else
                        echo "[INFO] Munging completed"
                    fi
                )
                export -f run_and_validate
    
                ips=${COMMA_SEPARATED_IPS//,/ }
                parallel -j0 -0 run_and_validate ::: $AUTOMATION_TENANT_NAME ::: $ips
            '''.stripIndent(), label: 'Importing agents data')
        }
    }

    private def ipsStr(instances) {
        instances*.localIp.join(',')
    }
}
