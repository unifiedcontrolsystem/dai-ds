// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
pipeline {
    agent none
    parameters {
        booleanParam(name: 'QUICK_CHECK', defaultValue: false,
        description: 'Performing quick checks only')
        choice(name: 'AGENT', choices: [
                'NRE-TEST',
                'cmcheung-centos-7-test',
                'css-centos-8-00-test',
                'css-centos-8-01-test'
        ], description: 'Agent label')
    }    

    stages {
        stage ('unit-test') {
            agent { label "${AGENT}" }
            stages {
                stage('Preparation') {
                    steps {
                        echo "Building on ${AGENT}"
                        sh 'hostname'
                        lastChanges format: 'LINE', matchWordsThreshold: '0.25', matching: 'NONE',
                                matchingMaxComparisons: '1000', showFiles: true, since: 'PREVIOUS_REVISION',
                                specificBuild: '', specificRevision: '', synchronisedScroll: true, vcsDir: ''

                        sh 'rm -rf build'
                        script { utilities.FixFilesPermission() }
                    }
                }
                stage('Quick Test') {
                    options{ catchError(message: "Test failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_CHECK}" == 'true' } }
                    steps {
                        script {
                            utilities.InvokeGradle(":inventory_api:test")
                            utilities.InvokeGradle(":inventory:test")
                            utilities.InvokeGradle(":dai_core:test")
                            utilities.InvokeGradle(":dai_network_listener:test")
                            utilities.InvokeGradle(":procedures:test")
                        }
                    }
                }
                stage('Quick Report') {
                    options{ catchError(message: "Test failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_CHECK}" == 'true' } }
                    steps {
                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        junit '**/test-results/**/*.xml'
                    }
                }
                stage('Quick Check') {
                    options{ catchError(message: "Test failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_CHECK}" == 'true' } }
                    steps {
                        script {
                            utilities.InvokeGradle(":inventory_api:check")
                            utilities.InvokeGradle(":inventory:check")
                            utilities.InvokeGradle(":dai_core:check")
                            utilities.InvokeGradle(":dai_network_listener:check")
                            utilities.InvokeGradle(":procedures:check")
                        }
                    }
                }
                stage('Full Test') {
                    options{ catchError(message: "Test failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_CHECK}" == 'false' } }
                    steps {
                        script {
                            utilities.InvokeGradle("clean")
                            utilities.InvokeGradle("test")
                        }
                    }
                }
                stage('Full Report') {
                    options{ catchError(message: "Test failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_CHECK}" == 'false' } }
                    steps {
                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        junit '**/test-results/**/*.xml'
                    }
                }
                stage('Full Check') {
                    options{ catchError(message: "Test failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_CHECK}" == 'false' } }
                    steps {
                        script { utilities.InvokeGradle("check") }
                    }
                }
                stage('Archive') {
                    steps {
                        archiveArtifacts 'build/reports/**'
                    }
                }
            }
        }
    }
}
