import com.ziften.jenkins.PersistentStorage

def call(key) {
    PersistentStorage.newInstance(this).getPersistentVar(key)
}
