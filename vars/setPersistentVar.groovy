import com.ziften.jenkins.automation.PersistentStorage

def call(key, value) {
    PersistentStorage.newInstance(this).setPersistentVar(key, value)
}
