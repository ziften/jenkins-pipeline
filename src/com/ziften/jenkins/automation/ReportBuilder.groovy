package com.ziften.jenkins.automation

class ReportBuilder {
    def build(tests) {
        def elements = []
        elements << headerRows(tests)
        elements << bodyRows(tests)

        asPreformatted(elements.join())
    }

    private def headerRows(tests) {

    }

    private def bodyRows(tests) {
        tests.collect { rowForStatus(it) }
    }

    private def rowForStatus(test) {
        def content
        if (test.status == 'SUCCESS') {
            content = successRow(test.title)
        } else if (test.status == 'FAILURE') {
            content = failureRow(test.title)
        } else {
            content = undefinedRow(test.title)
        }

        asDiv(content)
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

    private def asDiv(text) {
        "<div>${text}</div>"
    }

    private def asParagraph(text) {
        "<p>${text}</p>"
    }

    private def asPreformatted(text) {
        "<pre>${text}</pre>"
    }
}
