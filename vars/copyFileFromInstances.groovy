import com.ziften.jenkins.automation.SpotInstancesManager
import com.ziften.jenkins.automation.FileUtils

def call(filepath, ... instances) {
    def manager = SpotInstancesManager.newInstance(this)

    node('AWS-pipe-slave') {
        manager.collectFileFromMany(instances.flatten(), filepath)

        def filename = FileUtils.basename(filepath)
        archiveArtifacts("*_${filename}")
        sh("rm -f *_${filename}")
    }
}
