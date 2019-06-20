import com.ziften.jenkins.Storage

def call(key, value) {
    Storage.newInstance(this).setPersistentVar(key, value)
}
