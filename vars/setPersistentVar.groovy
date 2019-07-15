import com.ziften.jenkins.PersistentStorage

def call(key, value) {
    PersistentStorage.newInstance(this).setPersistentVar(key, value)
}
