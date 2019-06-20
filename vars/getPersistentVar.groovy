import com.ziften.jenkins.Storage

def call(key) {
    Storage.newInstance(this).getPersistentVar(key)
}
