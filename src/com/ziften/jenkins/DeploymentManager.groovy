package com.ziften.jenkins

class DeploymentManager {
    def steps

    DeploymentManager(steps) {
        this.steps = steps
    }

    def provisionKeys(instances) {
        steps.sh(script: "salt -L '${hostnamesStr(instances)}' -t 600 state.sls soft_key saltenv=dev-pipe-spot", label: 'Setup keys')
    }

    def copyInstaller(instances, installerDir) {
        steps.withEnv(["COMMA_SEPARATED_IPS=${ipsStr(instances)}", "INSTALLER_DIR=${installerDir}"]) {
            steps.sh(script: '''\
                copyInstaller() {
                    installer_dir=$1
                    ip=$2
                
                    echo "[INFO] Copying installer from $installer_dir to the VM"
                    scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i /etc/salt/qa.pem -r $installer_dir/*.jar root@$ip:/root
                }
                export -f copyInstaller
    
                ips=${COMMA_SEPARATED_IPS//,/ }
                parallel -j0 -0 copyInstaller ::: $INSTALLER_DIR ::: $ips
            '''.stripIndent(), label: 'Copying installer to the instances')
        }
    }

    def deploy(instances) {
        steps.withEnv(["COMMA_SEPARATED_HOSTS=${hostnamesStr(instances)}", "COMMA_SEPARATED_IPS=${ipsStr(instances)}"]) {
            steps.sh(script: '''\
                #!/bin/bash
                deploy_ziften() {
                    echo "#######################################"
                    echo "#      Deploy ZIFTEN - START          #"
                    echo "#######################################"
    
                    date
                    
                    host=$1
                    ip=$2
                    
                    echo "[INFO] Fixing issue with firewalld service on CentOS 7.3"
                    salt $host -t 600 cmd.run 'yum -y install firewalld'
                    salt $host -t 120 system.reboot
                    count=0
                    while [ $count -lt 30 ]; do
                        is_rebooted=$(salt $host test.ping -t 60 | awk 'END{print $1}')
                        if [ "$is_rebooted" != "True" ]; then
                            let count=count+1
                            sleep 10
                            let seconds=$count*10
                            echo "Waiting for $host to come up after restart : $seconds seconds"
                            continue
                        else
                            echo "$host is up!"                  
                            break
                        fi
                    done
    
                    echo "[INFO] Installing JAR"
                    salt $host -t 3600 cmd.script salt://files/qa_install_automation.sh saltenv=dev-pipe-spot runas=root shell=/bin/bash
    
                    salt $host cmd.run "sed -i 's/log\\.retention\\.hours.*/log\\.retention\\.hours=24/g' /opt/ziften/kafka/config/server.properties"
                    salt $host cmd.run "sed -i 's/knowledgecloud-52\\.cloud\\.ziften\\.com/172.16.9.85/g' /opt/ziften/etc/systemd/agent.service; systemctl daemon-reload"
                    salt $host cmd.run "sed -i 's/ZIFTEN_CLOUD_SERVICE_URL=.*/ZIFTEN_CLOUD_SERVICE_URL=https:\\/\\/172.16.9.85/g' /opt/ziften/zlabmq.settings"
                    salt $host cmd.run "systemctl restart scheduler &"
    
                    MC_PASSWORD=$(salt $host cmd.run "grep MCZIFTENPASSWORD /opt/ziften/zlabmq.settings"|tail -1|awk -F'=' '{print $NF}')
                    MC_DATABASE_PASSWORD=$(salt $host cmd.run "grep MC_DATABASE_PASSWORD /opt/ziften/zlabmq.settings"|tail -1|awk -F'=' '{print $NF}')
                    LANDLORD_PASSWORD=$(salt $host cmd.run "grep LANDLORD_PASSWORD /opt/ziften/zlabmq.settings"|tail -1|awk -F'=' '{print $NF}')
    
                    salt $host -t 600 cmd.script salt://files/server_setup_post_automation.sh saltenv=dev-pipe-spot
                    
                    ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i /etc/salt/qa.pem root@$ip "sed -i \\"s/shared_buffers.*/shared_buffers=2048MB/g\\" /var/lib/pgsql/10/data/postgresql.conf"
    
                    salt $host cmd.run "rm -rvf /root/ziften*.jar"
    
                    salt $host cmd.run "mkdir -p /root/bin; echo '#! /bin/sh' > /root/bin/tl; echo 'tail -f /var/log/ziften.log | grep -v -e \\".*metrics - type=.*\\" -e \\".*metrics-logger.*\\" -e \\".*\\.NoSqlSession.*\\"' >> /root/bin/tl; chmod +x /root/bin/tl; ln -sf /root/bin/tl /usr/bin/tl"
                    
                    date
                    
                    echo "#######################################"
                    echo "#      Deploy Ziften - END            #"
                    echo "#######################################"
                }
                export -f deploy_ziften
                
                hosts=${COMMA_SEPARATED_HOSTS//,/ }
                ips=${COMMA_SEPARATED_IPS//,/ }
                parallel -j0 -0 --xapply deploy_ziften ::: $hosts ::: $ips
            '''.stripIndent(), label: 'Deploying Ziften')
        }

        steps.waitZiftenIsUp(instances)
    }

    private def hostnamesStr(instances) {
        instances*.hostname.join(',')
    }

    private def ipsStr(instances) {
        instances*.localIp.join(',')
    }
}
