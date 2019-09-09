package com.ziften.jenkins.automation

class ReportBuilder {
    @NonCPS
    def build(Map opts = [:], results) {
        def message = prepareMessage(results)
        def status = prepareStatus(results)
        def color = color(status)

        [message: message, status: status.name(), color: color]
    }

    @NonCPS
    private def prepareMessage(results) {
        def rows = results.sort { it.title }.collect { rowForResult(it) }
        asPreformatted(rows.join())
    }

    @NonCPS
    def prepareStatus(results) {
        if (results.every { it.status.isSuccess() }) {
            BuildStatus.SUCCESS
        } else {
            BuildStatus.FAILURE
        }
    }

    @NonCPS
    private def rowForResult(result) {
        asDiv(resultDescription(result))
    }

    @NonCPS
    private def resultDescription(result) {
        if (result.status.isSuccess()) {
            successRow(result.title)
        } else if (result.status.isFailure()) {
            failureRow(result.title)
        } else {
            undefinedRow(result.title)
        }
    }

    @NonCPS
    private def successRow(testsTitle) {
        "✅ ${testsTitle}"
    }

    @NonCPS
    private def failureRow(testsTitle) {
        "❌ ${testsTitle}"
    }

    @NonCPS
    private def undefinedRow(testsTitle) {
        "❔ ${testsTitle}"
    }

    @NonCPS
    private def asDiv(text) {
        "<div>${text}</div>"
    }

    @NonCPS
    private def asPreformatted(text) {
        "<pre>${text}</pre>"
    }

    @NonCPS
    private def color(status) {
        if (status.isSuccess()) {
            'Green'
        } else if (status.isFailure()) {
            'Red'
        } else {
            'Blue'
        }
    }
}
