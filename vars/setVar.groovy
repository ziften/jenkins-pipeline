import com.ziften.jenkins.Storage

def call(Map opts=[:], key, value) {
    Storage.newInstance(this).setVar(key, value, opts)
}
