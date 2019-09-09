import com.ziften.jenkins.automation.ReportBuilder

def call(results) {
    def reportsBuilder = new ReportBuilder()
    def report = reportsBuilder.build(results)

    withCredentials([string(credentialsId: 'teams-webhook_qa-automation', variable: 'TEAMS_WEBHOOK')]) {
        office365ConnectorSend(
                message: report.message,
                status: report.status,
                color: report.color,
                webhookUrl: env.TEAMS_WEBHOOK
        )
    }
}
