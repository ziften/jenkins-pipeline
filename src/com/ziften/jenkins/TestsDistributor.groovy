package com.ziften.jenkins

@Grab('com.google.guava:guava:24.1-jre')
import com.google.common.collect.Sets

class TestsDistributor {
    private def tests
    private def result
    private def utils

    final DEVIATION_MSEC = 100000

    TestsDistributor() {
        this.utils = new PipelineUtils()
    }

    @NonCPS
    def groupByDuration(jobs) {
        this.tests = jobsWithDuration(jobs)
        this.result = []

        def longestTest = longestTest()

        addChunk([longestTest])

        while (!tests.isEmpty()) {
            def combinations = allCombinations(tests).findAll {
                totalDuration(it) <= longestTest.duration + DEVIATION_MSEC
            }
            def chunk = pickBest(combinations, longestTest.duration)
            addChunk(chunk)
        }

        result
    }

    def groupByInstances(jobs, instancesNumber) {
        this.tests = jobsWithDuration(jobs)
        this.result = []

        instancesNumber.times { idx ->
            if (idx + 1 == instancesNumber) {
                addChunk(tests)
                return
            }

            def reference = averageChunkDuration(instancesNumber - idx)
            def combinations = allCombinations(tests)
            def chunk = pickBest(combinations, reference)
            addChunk(chunk)
        }

        result
    }

    @NonCPS
    private def allCombinations(tests) {
        Sets.powerSet(tests.toSet()).toList()[1..-1]
    }

    @NonCPS
    private def pickBest(combinations, reference) {
        combinations.min { deviationFromReference(it, reference) }
    }

    @NonCPS
    private def deviationFromReference(combination, reference) {
        (totalDuration(combination) - reference).abs()
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

    @NonCPS
    private def averageChunkDuration(chunksNumber) {
        def avg = totalDuration(tests)/chunksNumber.toFloat()
        avg.round()
    }
}
