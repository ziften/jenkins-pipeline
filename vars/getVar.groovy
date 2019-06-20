import com.ziften.jenkins.Storage

def call(Map opts=[:], key) {
    Storage.newInstance(this).getVar(key, opts)
}
