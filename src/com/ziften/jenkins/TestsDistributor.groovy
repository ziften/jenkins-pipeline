package com.ziften.jenkins

class TestsDistributor {
    private def tests
    private def result
    private def longestTest
    private def utils

    final DEVIATION = 10

    @NonCPS
    def run(tests) {
        this.utils = new PipelineUtils()
        this.tests = jobsWithDuration(tests)
        this.result = []
        this.longestTest = longestTest()

        addChunk([longestTest])

        while (!this.tests.isEmpty()) {
            def combinations = allCombinations(this.tests)
            def chunk = pickBest(combinations)
            addChunk(chunk)
        }

        result
    }

    @NonCPS
    private def allCombinations(tests) {
        (1..(tests.size())).inject([]) { list, i ->
            list + comb(i, tests as List)
        }.collect { it as LinkedHashSet } as LinkedHashSet
    }

    @NonCPS
    private def comb(m, list) {
        def n = list.size()
        m == 0 ?
                [[]] :
                (0..(n - m)).inject([]) { newlist, k ->
                    def sublist = (k + 1 == n) ? [] : list[(k + 1)..<n]
                    newlist + comb(m - 1, sublist).collect { [list[k]] + it }
                }.findAll { totalDuration(it) <= longestTest.duration + DEVIATION }
    }

    @NonCPS
    private def pickBest(combinations) {
        combinations.min { deviationFromLongest(it) }
    }

    @NonCPS
    private def deviationFromLongest(combination) {
        (totalDuration(combination) - longestTest.duration).abs()
    }

    @NonCPS
    private def addChunk(chunk) {
        result << extractJobs(chunk)
        tests.removeAll(chunk)
    }

    @NonCPS
    private def extractJobs(chunk) {
        chunk*.name
    }

    @NonCPS
    private def longestTest() {
        tests.max { it.duration }
    }

    @NonCPS
    private def totalDuration(chunk) {
        chunk.sum { it.duration }
    }

    @NonCPS
    private def jobsWithDuration(names) {
        names.collect { jobWithDuration(it) }
    }

    @NonCPS
    private def jobWithDuration(name) {
        [name: name, duration: utils.jobDuration(name)]
    }
}
