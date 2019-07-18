package com.ziften.jenkins

class TestsReportBuilder {
    def buildForTelegram(Map pipeInfo, statuses) {
        def title = title(pipeInfo.pipeType, pipeInfo.pipeNumber)
        def divider = divider(title.size())
        def statusRows = statuses.collect { rowForStatus(it) }

        [title, divider, *statusRows].join('%0A')
    }

    private def title(pipeType, pipeNumber) {
        "${pipeType} Pipeline #${pipeNumber}"
    }

    private def divider(size) {
        '=' * size
    }

    private def rowForStatus(statusMap) {
        if (statusMap.status == 'SUCCESS') {
            successRow(statusMap.title)
        } else if (statusMap.status == 'FAILURE') {
            successRow(statusMap.title)
        }
    }

    private def successRow(testsName) {
        "✅ ${testsName}"
    }

    private def failureRow(testsName) {
        "❌ ${testsName}"
    }

    private def undefinedRow(testsName) {
        "❔ ${testsName}"
    }
}
