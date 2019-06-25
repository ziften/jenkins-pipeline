def call(... ips) {
    def ipsStr = ips.join(',')

    withEnv(["COMA_SEPARATED_IPS=${ipsStr}"]) {
        steps.sh('''\
            wait_ziften_is_up() {
                timeout=900
                loop_interval=5
                let iterations=$timeout/$loop_interval
            
                count=0
                ip=$1
                echo "[INFO] Waiting $timeout seconds for Ziften ${ip} to come up..."
                while :; do
                    J_STATUS=$(curl -Ik https://$ip/manage/health --silent|head -1|awk '{print $2}')
            
                    if [ "$J_STATUS" = "200" ]; then
                        let time=$count*loop_interval
                        echo "[INFO] Ziften is up in $time seconds"
                        break
                    fi
            
                    let count=count+1
                    if [ $count -le $iterations ]; then
                        sleep $loop_interval
                        continue
                    else
                        echo "[ERROR] Ziften is still down after $timeout seconds"
                        exit 1
                    fi
                done
            }
            
            export -f wait_ziften_is_up
            
            ips=${COMA_SEPARATED_IPS//,/ }
            parallel -0 wait_ziften_is_up ::: $ips
        '''.stripIndent())
    }
}
