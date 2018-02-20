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
}


def containerBuildPub(List<Map> argsList) {
    for(Map args : argsList) {
        println "Running Docker build/publish: ${args.image}:${args.tags}"

        docker.withRegistry("https://${args.container_reg.host}", "${args.container_reg.jenkins_creds_id}") {
            def img = docker.image("${args.image}")
            sh "docker build --build-arg VCS_REF=${env.GIT_SHA} --build-arg BUILD_DATE=`date -u +'%Y-%m-%dT%H:%M:%SZ'` --build-arg VERSION=${env.APP_VERSION} -t ${args.image} ${args.dockerfile}"
            return img.id
        }
    }
}