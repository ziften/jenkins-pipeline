import com.ziften.jenkins.SpotInstancesManager
import com.ziften.jenkins.FileUtils

def call(filepath, ... instances) {
    def manager = SpotInstancesManager.newInstance(this)

    node('AWS-pipe-slave') {
        manager.collectFileFromMany(instances.flatten(), filepath)

        def filename = FileUtils.filename(filepath)
        def extension = FileUtils.extension(filepath)

        archiveArtifacts("${filename}_*.${extension}")
        sh("rm -f ${filename}_*.${extension}")
    }
}
