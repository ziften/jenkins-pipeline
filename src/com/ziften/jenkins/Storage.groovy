package com.ziften.jenkins

class Storage {
    static final FILE_STORAGE = 'global_storage.json'
    static memoryStorage = [:].withDefault { [:] }
    def steps

    Storage(steps) {
        this.steps = steps
    }

    def setVar(key, value, opts) {
        opts.stage ? setStageVar(opts.stage, key, value) : setBasicVar(key, value)
    }

    def getVar(key, opts) {
        opts.stage ? getStageVar(opts.stage, key) : getBasicVar(key)
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

    private def setStageVar(stage, key, value) {
        memoryStorage[stage] += [(key): value]
    }

    private def getStageVar(stage, key) {
        memoryStorage[stage][key]
    }

    private def setBasicVar(key, value) {
        memoryStorage[key] = value
    }

    private def getBasicVar(key) {
        memoryStorage[key]
    }
}
