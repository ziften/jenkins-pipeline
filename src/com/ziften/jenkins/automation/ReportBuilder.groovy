package com.ziften.jenkins.automation

class ReportBuilder {
    @NonCPS
    def build(results, overallStatus) {
        def message = prepareMessage(results)
        def color = prepareColor(overallStatus)

        [message: message, status: overallStatus, color: color]
    }

    @NonCPS
    private def prepareMessage(results) {
        if (!results) {
            return ''
        }

        def groups = results.groupBy({ it.group }).collect { key, value ->
            def rows = value.sort { it.title }.collect { rowForResult(it) }
            asGroup(rows.join(), key)
        }

        asPreformatted(groups.join())
    }

    @NonCPS
    private def prepareColor(status) {
        if (status == 'SUCCESS') {
            'Green'
        } else if (status == 'FAILURE') {
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
        if (status == 'SUCCESS') {
            '✅'
        } else if (status == 'FAILURE') {
            '❌'
        } else {
            '❔'
        }
    }

    @NonCPS
    private def titleAsUrl(result) {
        if (result.url) {
            "<a href=\"${result.url}\">${result.title}</a>"
        } else {
            result.title
        }
    }

    @NonCPS
    private def asGroup(rowsHtml, groupCaption) {
        "<div>${groupCaption}:</div>" + rowsHtml
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
