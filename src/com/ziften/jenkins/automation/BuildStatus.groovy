package com.ziften.jenkins.automation

enum BuildStatus {
    FAILURE, SUCCESS

    @NonCPS
    boolean isSuccess() {
        this == SUCCESS
    }

    @NonCPS
    boolean isFailure() {
        this == FAILURE
    }
}
