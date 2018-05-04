#!/usr/bin/groovy
package com.ziften.jenkins


def setGitEnvVars() {
    println "Setting env vars to tag containers"

    sh 'git rev-parse HEAD > git_commit_id.txt'
    try {
        env.GIT_COMMIT_ID = readFile('git_commit_id.txt').trim()
        env.GIT_SHA = env.GIT_COMMIT_ID.substring(0, 7)
    } catch (e) {
        error "${e}"
    }
    println "env.GIT_COMMIT_ID ==> ${env.GIT_COMMIT_ID}"

    sh 'git config --get remote.origin.url> git_remote_origin_url.txt'
    try {
        env.GIT_REMOTE_URL = readFile('git_remote_origin_url.txt').trim()
    } catch (e) {
        error "${e}"
    }
    println "env.GIT_REMOTE_URL ==> ${env.GIT_REMOTE_URL}"
    println "env.BRANCH_NAME ==> ${env.BRANCH_NAME}"
}


def containerBuildPub(List<Map> argsList, List tags = ['latest']) {
    for(Map args : argsList) {
        println "Running Docker build/publish: ${args.image}:${tags}"

        docker.withRegistry("https://${args.container_reg.host}", "${args.container_reg.jenkins_creds_id}") {
            def img = docker.image("${args.image}")
            sh "docker build --build-arg VCS_REF=${env.GIT_SHA} --build-arg BUILD_DATE=`date -u +'%Y-%m-%dT%H:%M:%SZ'` --build-arg VERSION=${env.APP_VERSION} -t ${args.image} ${args.dockerfile}"
            for (int i = 0; i < tags.size(); i++) {
                img.push(tags.get(i))
            }

            return img.id
        }
    }
}


def getContainerTags(config, List tags = []) {
    println "getting list of tags for containers"
    def String commit_tag
    def String version_tag

    try {
        // if PR branch tag with only branch name
        if (env.BRANCH_NAME.contains('PR')) {
            commit_tag = env.BRANCH_NAME
            tags << commit_tag
            return tags
        }
    } catch (Exception e) {
        println "WARNING: commit unavailable from env. ${e}"
    }

    // commit tag
    try {
        // if branch available, use as prefix, otherwise only commit hash
        if (env.BRANCH_NAME) {
            commit_tag = env.BRANCH_NAME.replace('/', '-') + '-' + env.GIT_COMMIT_ID.substring(0, 7)
        } else {
            commit_tag = env.GIT_COMMIT_ID.substring(0, 7)
        }
        tags << commit_tag
    } catch (Exception e) {
        println "WARNING: commit unavailable from env. ${e}"
    }

    // master tag
    try {
        if (env.BRANCH_NAME == 'master') {
            tags << 'latest'
        }
    } catch (Exception e) {
        println "WARNING: branch unavailable from env. ${e}"
    }

    // app version tag
    try {
        if (env.APP_VERSION) {
            tags << env.APP_VERSION
        }
    } catch (Exception e) {
        println "WARNING: APP_VERSION unavailable from env. ${e}"
    }

    return tags
}

def helmLint(String chart_dir) {
    // lint helm chart
    println "running helm lint ${chart_dir}"
    sh "helm lint ${chart_dir}"
}

def helmConfig() {
    //setup helm connectivity to Kubernetes API and Tiller
    println "initiliazing helm client"
    sh "helm init"
    sh "helm repo add incubator https://kubernetes-charts-incubator.storage.googleapis.com/"
    sh "helm repo add stable https://kubernetes-charts.storage.googleapis.com"
    println "checking client/server version"
    sh "helm version"
}

def helmDeploy(Map args) {
    //configure helm client and confirm tiller process is installed
    helmConfig()
    def String release_overrides = ""
    if (args.set) {
        args.set.each { key, value ->
            release_overrides += "$key=$value,"
        }
    }

    def String namespace

    // If namespace isn't parsed into the function set the namespace to the name
    if (args.namespace == null) {
        namespace = args.name
    } else {
        namespace = args.namespace
    }

    if (args.dry_run) {
        println "Running dry-run deployment"

        sh "helm upgrade --dry-run --install ${args.name} ${args.chart_dir} " + (release_overrides ? "--set ${release_overrides}" : "") + " --namespace=${namespace}"
    } else {
        println "Running deployment"

        sh "helm dependency update ${args.chart_dir}"
        sh "helm upgrade --install ${args.name} ${args.chart_dir} " + (release_overrides ? "--set ${release_overrides}" : "") + " --namespace=${namespace}" + " --wait"

        echo "Application ${args.name} successfully deployed. Use helm status ${args.name} to check"
    }
}