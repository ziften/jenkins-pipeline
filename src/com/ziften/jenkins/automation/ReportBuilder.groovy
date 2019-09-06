package com.ziften.jenkins.automation

class ReportBuilder {
    class Report {
        def message
        def status
        def color

        Report(message, status) {
            this.message = message
            this.status = status
            this.color = color(status)
        }

        private def color(status) {
            if (status == 'SUCCESS') {
                'Green'
            } else if (status == 'FAILURE') {
                'Red'
            } else {
                'Blue'
            }
        }
    }

    def build(Map opts = [:], results) {
        def message = prepareMessage(results)
        def status = prepareStatus(results)

        new Report(message, status)
    }

    private def prepareMessage(results) {
        def rows = results.collect { rowForResult(it) }
        asPreformatted(rows.join())
    }

    def prepareStatus(results) {
        if (results.every { it.status == 'SUCCESS' }) {
            'SUCCESS'
        } else {
            'FAILURE'
        }
    }

    private def rowForResult(result) {
        asDiv(resultDescription(result))
    }

    private def resultDescription(result) {
        if (result.status == 'SUCCESS') {
            successRow(result.title)
        } else if (result.status == 'FAILURE') {
            failureRow(result.title)
        } else {
            undefinedRow(result.title)
        }
    }

    private def successRow(testsTitle) {
        "✅ ${testsTitle}"
    }

    private def failureRow(testsTitle) {
        "❌ ${testsTitle}"
    }

    private def undefinedRow(testsTitle) {
        "❔ ${testsTitle}"
    }

    private def asDiv(text) {
        "<div>${text}</div>"
    }

    private def asPreformatted(text) {
        "<pre>${text}</pre>"
    }
}
