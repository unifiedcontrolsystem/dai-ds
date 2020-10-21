// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
pipeline {
    agent none
    parameters {
        booleanParam(name: 'QUICK_BUILD', defaultValue: false,
                description: 'Performs a partial clean to speed up the build.')
        choice(name: 'AGENT', choices: [
                'NRE-UNIT',
                'Sindhu-test'
        ], description: 'Agent label')
    }
    stages {
        stage('Sequential Stages') { // all the sub-stages needs to be run on the same machine
            agent { label "${AGENT}" }
            environment {
                PATH = "${PATH}:/home/${USER}/voltdb9.1/bin"
            }
            stages {    // another stages is required to force operations on the same machine
                stage('Preparation') {
                    steps {
                        echo "Building on ${AGENT}"
                        sh 'hostname'

                        lastChanges format: 'LINE', matchWordsThreshold: '0.25', matching: 'NONE',
                                matchingMaxComparisons: '1000', showFiles: true, since: 'PREVIOUS_REVISION',
                                specificBuild: '', specificRevision: '', synchronisedScroll: true, vcsDir: ''

                        script {
                            utilities.copyIntegrationTestScriptsToBuildDistributions()  // for cleaning this machine
                            utilities.fixFilesPermission()
                            script { utilities.cleanUpMachine('build/distributions') }
                            // You can no longer run component tests and unit tests concurrently on the same machine
                        }
                    }
                }
                stage('Quick Unit Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'true' } }
                    options{ catchError(message: "Quick Unit Tests failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    steps {
                        // Quick build assumes that the current build artifacts are not corrupted
                        script { utilities.invokeGradle("build") }
                    }
                }
                stage('Unit Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    options{ catchError(message: "Unit Tests failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    steps {
                        sh 'rm -rf build'
                        script { utilities.invokeGradle("clean build") }
                    }
                }
                stage('Reports') {
                    options{ catchError(message: "Reports failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    steps {
                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        script { utilities.generateJunitReport('**/test-results/**/*.xml') }
                    }
                }
                stage('Archive') {
                    steps {
                        script { utilities.copyIntegrationTestScriptsToBuildDistributions() }   // for cleaning other machines

                        sh 'rm -f *.zip'
                        zip archive: true, dir: '', glob: '**/build/jacoco/test.exec', zipFile: 'unit-test-coverage.zip'
                        zip archive: true, dir: '', glob: '**/main/**/*.java', zipFile: 'src.zip'
                        zip archive: true, dir: '', glob: '**/build/classes/java/main/**/*.class', zipFile: 'classes.zip'
                        zip archive: true, dir: '', glob: '**/test-results/test/*.xml', zipFile: 'unit-test-results.zip'

                        archiveArtifacts allowEmptyArchive: true, artifacts:'build/distributions/*, build/reports/**'
                    }
                }
            }
        }
    }
}

