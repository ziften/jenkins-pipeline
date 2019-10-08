import com.ziften.jenkins.automation.PatchManager

def call(Map opts) {
    def manager = PatchManager.newInstance(this)

    node('AWS-CentOS_7-DEV01') {
        manager.copyPatchFiles()
    }

    node('master') {
        manager.preparePatch(opts)
    }
}
