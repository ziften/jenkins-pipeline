import com.ziften.jenkins.automation.TestsReportBuilder

def call(statuses, chatId) {
    def reportsBuilder = new TestsReportBuilder()
    def report = reportsBuilder.buildForTelegram(statuses, pipeType: env.PIPE_TYPE, pipeNumber: env.BUILD_NUMBER)

    withCredentials([string(credentialsId: 'telegram-bot', variable: 'TOKEN')]) {
        sh("curl -s -X POST https://api.telegram.org/bot${TOKEN}/sendMessage -d chat_id=${chatId} -d text=\"${report}\"")
    }
}

