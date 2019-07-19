import com.ziften.jenkins.automation.PersistentStorage

def call(key) {
    PersistentStorage.newInstance(this).getPersistentVar(key)
}
