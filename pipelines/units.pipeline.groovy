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
                'NRE-UNIT',
                'cmcheung-centos-7-unit',
                'css-centos-8-00-unit',
                'css-centos-8-01-unit'
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

                        script { utilities.FixFilesPermission() }
                        // No need to clean up system because component tests and functional tests should
                        // not have run on this system
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
                            sh 'touch inventory/build/test-results/test/*.xml'
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
                        CopyIntegrationTestScriptsToBuildDistributions()
                        archiveArtifacts 'build/reports/**, build/distributions/*.sh, build/json-server/**'
                    }
                }
            }
        }
    }
}

def CopyIntegrationTestScriptsToBuildDistributions() {
    sh 'mkdir -p build/json-server'
    sh 'mkdir -p build/distributions'
    sh 'cp ./inventory/src/integration/resources/scripts/json-server/* ./build/json-server'
    sh 'cp ./inventory/src/integration/resources/scripts/clean_up_machine.sh ./build/distributions'
}
