package com.ziften.jenkins

class ConfigureManager {
    def steps

    ConfigureManager(steps) {
        this.steps = steps
    }

    def configureProperties(instances) {
        steps.withEnv(["COMMA_SEPARATED_IPS=${ipsStr(instances)}"]) {
            steps.sh('''
                #!/bin/bash
                configure_properties() {
                    ip=$1
                    
                    echo "[INFO] Stopping consumer on $ip"
                    ssh $ip "systemctl stop consumer"

                    echo "[INFO] Updating Bulk insert props"
                    ssh $ip "\\
                    sed -i \\"s/BULK_INSERT_SECONDS=.*/BULK_INSERT_SECONDS=10/g\\" /opt/ziften/zlabmq.settings; \\
                    echo \\"\\" >> /opt/ziften/zlabmq.settings; \\
                    echo \\"BULK_INSERT_SECONDS_RANGE_MAX=10\\" >> /opt/ziften/zlabmq.settings; \\
                    echo \\"BULK_INSERT_SECONDS_RANGE_MIN=5\\" >> /opt/ziften/zlabmq.settings; \\
                    echo \\"BINARYFILE_RULE_PROCESSOR_RATE=3000\\" >> /opt/ziften/zlabmq.settings; \\
                    echo \\"FEED_LOOKUP_REQUEST_POLL_SECONDS=86400\\" >> /opt/ziften/zlabmq.settings;"

                    ssh $ip "systemctl restart consumer; systemctl restart feed; systemctl restart binaryfile"
                    sleep 60
                }
                export -f configure_properties
                
                ips=${COMMA_SEPARATED_IPS//,/ }
                parallel -j0 -0 configure_properties ::: $ips
            '''.stripIndent())
        }
    }

    def addTenant(instances, tenantName) {
        steps.withEnv(["COMMA_SEPARATED_IPS=${ipsStr(instances)}", "AUTOMATION_TENANT_NAME=${tenantName}"]) {
            steps.sh('''
                #!/bin/bash
                add_tenant() {
                    tenant_name=$1
                    ip=$2
                    
                    domain="${tenant_name}.ziften.local"
                    site_id=$(echo -n $domain | md5sum | awk '{print $1}')
                    
                    echo "[INFO] Adding custom tenant - $tenant_name"
                    ssh $ip "source /root/.bash_profile && /opt/ziften/bin/ziften-cli.sh add-customer ${tenant_name} ${tenant_name} ${site_id} no-dns"
                    add_customer_exit_code=$?
                    if [ $add_customer_exit_code -gt 0 ]; then
                        echo "Error while adding tenant"
                        exit 1
                    fi
                    
                    domain_secondary="${tenant_name}_secondary.ziften.local"
                    site_id_secondary=$(echo -n $domain_secondary | md5sum | awk '{print $1}')
                    echo "[INFO] Adding secondary site ID - $domain_secondary"
                    ssh $ip "source /root/.bash_profile && /opt/ziften/bin/ziften-cli.sh add-secondary-siteids ${tenant_name} ${site_id_secondary}"
                    add_secondary_siteid_exit_code=$?
                    if [ $add_secondary_siteid_exit_code -gt 0 ]; then
                        echo "Error while adding secondary siteID"
                        exit 1
                    fi

                    echo "[INFO] Adding hostname to new tenant"
                    ssh $ip "psql -U ziften -d ziften -c \\"UPDATE \\"global\\".\\"customers\\" SET \\"hostname\\"='\\`hostname\\`' WHERE \\"name\\"='$tenant_name'\\""
                    ssh $ip "systemctl restart alert-delivery"
                }
                export -f add_tenant
                
                ips=${COMMA_SEPARATED_IPS//,/ }
                parallel -j0 -0 add_tenant ::: $AUTOMATION_TENANT_NAME ::: $ips
            '''.stripIndent())
        }
    }

    private def ipsStr(instances) {
        instances*.localIp.join(',')
    }
}
