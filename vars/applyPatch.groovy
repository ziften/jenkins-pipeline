import com.ziften.jenkins.automation.PatchManager

def call(Map opts, ... instances) {
    def manager = PatchManager.newInstance(this)
    def plainInstances = instances.flatten()

    node('master') {
        manager.applyPatchToMany(plainInstances,
                patchProperties: opts.patchProperties,
                startZiftenAfterPatch: opts.startZiftenAfterPatch)
    }
}
