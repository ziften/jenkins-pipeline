package com.ziften.jenkins

class SpotInstance {
    def localIp
    def externalIp
    def hostname

    SpotInstance(opts) {
        this.localIp = opts.localIp
        this.externalIp = opts.externalIp
        this.hostname = opts.hostname
    }
}