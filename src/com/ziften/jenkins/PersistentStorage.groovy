package com.ziften.jenkins

class PersistentStorage {
    static final FILE_STORAGE = 'global_storage.json'
    def steps

    PersistentStorage(steps) {
        this.steps = steps
    }

    def setPersistentVar(key, value) {
        def storage = (steps.fileExists(FILE_STORAGE)) ? steps.readJSON(file: FILE_STORAGE) : steps.readJSON(text: '{}')
        storage[key] = value
        steps.writeJSON(file: FILE_STORAGE, json: storage)

        steps.stash(name: "global-storage-stash", includes: FILE_STORAGE)
    }

    def getPersistentVar(key) {
        steps.unstash(name: "global-storage-stash")

        steps.readJSON(file: FILE_STORAGE)[key]
    }
}
