// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
pipeline {
    agent none
    parameters {
        booleanParam(name: 'QUICK_BUILD', defaultValue: false,
                description: 'Skips the clean step')
        choice(name: 'AGENT', choices: [
                'Nightly-Build',
                'loki-n3-build',
                'Sindhu-test'
        ], description: 'Agent label')
    }
    stages {
        stage ('unit-test') {
            agent { label "${AGENT}" }
            environment {
                PATH = "${PATH}:/home/${USER}/voltdb9.1/bin"
            }
            stages {
                stage('Preparation') {
                    steps {
                        echo "Building on ${AGENT}"
                        sh 'hostname'
                        lastChanges format: 'LINE', matchWordsThreshold: '0.25', matching: 'NONE',
                                matchingMaxComparisons: '1000', showFiles: true, since: 'PREVIOUS_REVISION',
                                specificBuild: '', specificRevision: '', synchronisedScroll: true, vcsDir: ''

                        script {
                            utilities.FixFilesPermission()
                            CleanUpMachine()
                        }
                    }
                }
                stage('Clean') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        sh 'rm -rf build'
                        script{ utilities.InvokeGradle("clean") }
                    }
                }
                stage('Unit Test') {
                    options{ catchError(message: "Unit Test failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    steps {
                        script {
                            utilities.InvokeGradle("build")
                            sh 'touch inventory/build/test-results/test/*.xml' // overrides strict behavior of junit step
                        }
                    }
                }
                stage('Report') {
                    options{ catchError(message: "Report failed", stageResult: 'UNSTABLE',
                            buildResult: 'UNSTABLE') }
                    steps {
                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        junit '**/test-results/**/*.xml'
                    }
                }
                stage('Archive') {
                    steps {
                        sh 'rm -f *.zip'
                        zip archive: true, dir: '', glob: '**/build/jacoco/test.exec', zipFile: 'unit-test-coverage.zip'
                        zip archive: true, dir: '', glob: '**/main/**/*.java', zipFile: 'src.zip'
                        zip archive: true, dir: '', glob: '**/build/classes/java/main/**/*.class', zipFile: 'classes.zip'
                        zip archive: true, dir: '', glob: '**/test-results/test/*.xml', zipFile: 'unit-test-results.zip'
                        archiveArtifacts 'build/distributions/**, build/reports/**'
                    }
                }
            }
        }
    }
}

def CleanUpMachine() {
    sh 'inventory/src/integration/resources/scripts/clean_up_machine.sh'
}