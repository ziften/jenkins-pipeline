package com.ziften.jenkins.automation

class ReportBuilder {
    @NonCPS
    def build(Map opts = [:], results) {
        def message = prepareMessage(results)
        def status = prepareStatus(results)
        def color = prepareColor(status)

        [message: message, status: status.toString(), color: color]
    }

    @NonCPS
    private def prepareMessage(results) {
        def rows = results.sort { it.title }.collect { rowForResult(it) }
        asPreformatted(rows.join())
    }

    @NonCPS
    def prepareStatus(results) {
        if (results.every { it.status == hudson.model.Result.SUCCESS }) {
            hudson.model.Result.SUCCESS
        } else {
            hudson.model.Result.FAILURE
        }
    }

    @NonCPS
    private def prepareColor(status) {
        if (status == hudson.model.Result.SUCCESS) {
            'Green'
        } else if (status == hudson.model.Result.FAILURE) {
            'Red'
        } else {
            'Blue'
        }
    }

    @NonCPS
    private def rowForResult(result) {
        asDiv(resultDescription(result))
    }

    @NonCPS
    private def resultDescription(result) {
        def description = titleAsUrl(result)
        def icon = iconForStatus(result.status)

        "${icon} ${description}"
    }

    @NonCPS
    private def iconForStatus(status) {
        if (status == hudson.model.Result.SUCCESS) {
            '✅'
        } else if (status == hudson.model.Result.FAILURE) {
            '❌'
        } else {
            '❔'
        }
    }

    @NonCPS
    private def titleAsUrl(result) {
        "<a href=\"${result.url}\">${result.title}</a>"
    }

    @NonCPS
    private def asDiv(text) {
        "<div>${text}</div>"
    }

    @NonCPS
    private def asPreformatted(text) {
        "<pre>${text}</pre>"
    }
}
