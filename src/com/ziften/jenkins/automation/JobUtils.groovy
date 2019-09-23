package com.ziften.jenkins.automation

@NonCPS
def jobDuration(String name) {
    jenkins.model.Jenkins.instance.getItem(name).getEstimatedDuration()
}

@NonCPS
def getSuccessfulBuildByFilter(String jobName, Map filter) {
    def allBuilds = jenkins.model.Jenkins.instance.getItem(jobName).builds
    allBuilds.find { build ->
        build.result == hudson.model.Result.SUCCESS &&
                filter.keySet().every { key -> build.buildVariableResolver.resolve(key) == filter[key] }
    }
}

@NonCPS
def getSuccessfulBuildNumberByFilter(Map filter, String jobName) {
    def build = getSuccessfulBuildByFilter(jobName, filter)

    build ? build.number : -1
}

def generateUUID() {
    UUID.randomUUID().toString()
}

@NonCPS
def getAllUpstreamCauses(build) {
    def causes = []

    def cause = build.getCauses()[0]
    while (cause.getClass() == hudson.model.Cause$UpstreamCause) {
        causes += cause.getUpstreamRun()

        if (cause.getUpstreamCauses()[0].getClass() == hudson.model.Cause$UpstreamCause$DeeplyNestedUpstreamCause) {
            def lastAccessibleBuild = cause.getUpstreamRun()
            causes += getAllUpstreamCauses(lastAccessibleBuild.getCauses()[0])
        }

        cause = cause.getUpstreamCauses()[0]
    }

    causes
}

@NonCPS
def getDownstreamBuild(upstreamBuild, downstreamJobName) {
    def job = jenkins.model.Jenkins.instance.getItem(downstreamJobName)

    job.builds.find { build ->
        def causes = getAllUpstreamCauses(build)
        causes.any { it.project.name == upstreamBuild.projectName && it.number == upstreamBuild.number }
    }
}

def getConsoleUrl(build) {
    "${build.getAbsoluteUrl()}/console"
}
