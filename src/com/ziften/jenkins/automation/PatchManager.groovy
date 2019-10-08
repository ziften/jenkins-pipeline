package com.ziften.jenkins.automation

class PatchManager {
    def steps

    PatchManager(steps) {
        this.steps = steps
    }

    def copyPatchFiles() {
        steps.withEnv(["WORKSPACE=${steps.env.WORKSPACE}"]) {
            steps.sh(script: '''\
                #!/bin/bash
                PFILE_DIR=$WORKSPACE/patches
                PATCH_DIR=$PFILE_DIR/files
    
                # Clean old artifacts
                rm -rvf $PATCH_DIR/*
    
                if [ -f $PFILE_DIR/patch_files.txt ]; then
                    # Create directory structure for patch files with wildcard
                    grep "*" $PFILE_DIR/patch_files.txt|awk '{print $2}'|sed -e "s|/opt/ziften/|$PATCH_DIR/|g" -e 's/*//1' -e 's|/$||'|xargs mkdir -pv
    
                    # Create directory structure for patch files
                    for i in $(grep -v "*" $PFILE_DIR/patch_files.txt|awk '{print $2}');do dirname $i;done|sort|uniq|sed "s|/opt/ziften|$PATCH_DIR|g"|xargs mkdir -pv
    
                    # Copy patch files to patch directory
                    while read line; do
                        cp -rv $line
                    done < <(cat $PFILE_DIR/patch_files.txt|sed -e 's/^.//1' -e "s|/opt/ziften/|$PATCH_DIR/|g" -e 's/*//2' -e 's|/$||')
                fi
            '''.stripIndent(), label: 'Gathering patch files together in patch dir')
        }

        steps.archiveArtifacts('patches/**')
    }

    def preparePatch(Map opts) {
        steps.sh('rm -f props')

        steps.copyArtifacts(filter: 'props', projectName: 'QA-SERVER-CodeBuild', selector: steps.specific("${opts.codeBuildNumber}"))
        def defaultProps = [PREVIOUS_RELEASE_DIR: steps.env.PREVIOUS_RELEASE_DIR,
                            WORKSPACE: steps.env.WORKSPACE,
                            BUILD_DIR: "${steps.env.JENKINS_HOME}/jobs/${steps.env.JOB_NAME}/builds/${steps.env.BUILD_NUMBER}"]
        def env = steps.readProperties(file: 'props', defaults: defaultProps).collect { k, v -> "${k}=${v}" }
        steps.withEnv(env) {
            steps.sh(script: '''\
                #!/bin/bash
                FEATURE_BRANCH=$(echo ${SERVER_BRANCH}|sed 's/\\//-/g')
                BUILD_MAJ=$(echo $SERVER_VERSION|sed 's/\\./_/g')
                M_DATE=$(echo ${BUILD_DATE}|sed 's/-//g')
                PFILE_DIR=$BUILD_DIR/archive/patches
                NEWTON_DIR=/srv/salt/server_patches/$M_DATE-$(echo $FEATURE_BRANCH|sed 's/\\./_/g')-${BUILD_MAJ}_$BUILD_NUMBER
                RELEASE_VERSION_FULL=$(echo $PREVIOUS_RELEASE_DIR|awk -F'-' '{gsub(/_/, ".", $4); print $4}')
                RELEASE_VERSION=$(echo $RELEASE_VERSION_FULL|sed 's/\\(.*\\)\\..*/\\1/')
                RELEASE_BUILD=$(echo $RELEASE_VERSION_FULL|awk -F'.' '{print $NF}')
                FINAL_PATCH=$RELEASE_BUILD-$BUILD_NUMBER
                PATCH_BASE_FOLDER=$(echo $NEWTON_DIR|awk -F'/' '{print $NF}')
                PATCH_FILES_FOLDER=$(echo $NEWTON_DIR|awk -F'/' '{print $NF}')/files

                if [ -d $NEWTON_DIR ]; then
                  rm -rf $NEWTON_DIR
                fi

                mkdir -pv $NEWTON_DIR/files
                cp -rv $PFILE_DIR/* $NEWTON_DIR/

                cd $NEWTON_DIR; awk '//; /# BEGIN PATCH SPECIFIC OPERATIONS #/{while(getline<"patch.sls"){print}}' $NEWTON_DIR/patch_template.sls > $NEWTON_DIR/${FINAL_PATCH}.sls

                if [ -f $NEWTON_DIR/patch_files.txt ] && [ $(wc -l < $NEWTON_DIR/patch_files.txt) -gt 0 ]; then
                  BACKUP_TARGETS=$(while read line; do echo $line; done < <(cat $NEWTON_DIR/patch_files.txt|sed -e 's/^.//1' -e 's/*//2' -e 's|/$||'|awk '{print $2}'|xargs))
                  sed -i -e "s@ZBACKUP_TARGETS@$BACKUP_TARGETS@" \\
                      -i -e "s@ZPATCH_FILES_FOLDER@$PATCH_FILES_FOLDER@" $NEWTON_DIR/${FINAL_PATCH}.sls
                else
                  sed -i "/# BEGIN FILE UPDATES #/,/# END FILE UPDATES #/ { /$END/"'!'" d;}" $NEWTON_DIR/${FINAL_PATCH}.sls
                fi

                sed -i -e "s@ZRELEASE_VERSION@$RELEASE_VERSION@" \\
                    -i -e "s@ZRELEASE_BUILD@$RELEASE_BUILD@" \\
                    -i -e "s@ZSERVER_VERSION@$SERVER_VERSION@" \\
                    -i -e "s@ZBUILD_NUMBER@$BUILD_NUMBER@" \\
                    -i -e "s@ZCOMMIT_HASH@$COMMIT_HASH@" \\
                    -i -e "s@ZBUILD_DATE@$BUILD_DATE@" \\
                    -i -e "s@ZBUILD_TIME@$BUILD_TIME@" \\
                    -i -e "s@ZSERVER_BRANCH@$SERVER_BRANCH@" $NEWTON_DIR/${FINAL_PATCH}.sls

                echo "RELEASE_VERSION=$RELEASE_VERSION" >> $WORKSPACE/props
                echo "RELEASE_BUILD=$(echo $RELEASE_VERSION_FULL|awk -F'.' '{print $NF}')" >> $WORKSPACE/props
                echo "FEATURE_BRANCH=$FEATURE_BRANCH" >> $WORKSPACE/props
                echo "FEATURE_DATE=$M_DATE" >> $WORKSPACE/props
                echo "PATCH_BASE_FOLDER=$PATCH_BASE_FOLDER" >> $WORKSPACE/props
                echo "PATCH_FILES_FOLDER=$M_DATE-$FEATURE_BRANCH/files" >> $WORKSPACE/props
                echo "FINAL_PATCH=$FINAL_PATCH" >> $WORKSPACE/props
            '''.stripIndent(), label: 'Preparing patch')
        }

        steps.readProperties(file: 'props')
    }

    def applyPatchToMany(Map opts, instances) {
        def hostsStr = instances*.hostname.join(',')
        def props = opts.patchProperties + [HOSTS: hostsStr]
        def env = props.collect { k, v -> "${k}=${v}" }

        steps.withEnv(env) {
            steps.sh(script: '''\
                #!/bin/bash
                echo "[INFO] Running patch on $HOSTS"
                echo "[INFO] Started at: $(date)"
                set -x
                salt -L $HOSTS test.ping
                salt -L $HOSTS -t 1500 state.sls server_patches.$PATCH_BASE_FOLDER.$(echo $FINAL_PATCH|sed 's/\\.sls//')
            '''.stripIndent(), label: 'Applying patch')
            if (opts.startZiftenAfterPatch) {
                steps.sh(script: '''\
                    #!/bin/bash
                    echo "[INFO] Restarting Ziften services..."
                    salt -L $HOSTS -t 180 service.restart ziften.target
                '''.stripIndent(), label: 'Starting Ziften services')
                steps.waitZiftenIsUp(instances, timeout: 240)
            }
        }
    }
}
