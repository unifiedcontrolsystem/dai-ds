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
                'NRE-COMPONENT',
                'Sindhu-test'
        ], description: 'Agent label')
    }
    stages {
        stage('Sequential Stages') { // all the sub-stages needs to be run on the same machine
            agent { label "${AGENT}" }
            environment {
                PATH = "${PATH}:/home/${USER}/voltdb9.1/bin"
                scriptDir = 'inventory/src/integration/resources/scripts'
                dataDir = 'inventory/src/integration/resources/data'
                etcDir = '/opt/ucs/etc'
                tmpDir = 'build/tmp'
            }
            stages {    // another stages is required to force operations on the same machine
                stage('Preparation') {
                    steps {
                        script { utilities.updateBuildName() }

                        echo "Building on ${AGENT}"
                        sh 'hostname'

                        lastChanges since: 'PREVIOUS_REVISION', format: 'SIDE', matching: 'LINE'

                        script {
                            utilities.copyIntegrationTestScriptsToBuildDistributions()
                            utilities.fixFilesPermission()
                            utilities.cleanUpMachine('build/distributions')
                        }
                    }
                }
                stage('Quick Component Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'true' } }
                    options { catchError(message: "Quick Component Tests failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    steps {
                        script {utilities.invokeGradleNoRetries("jar")}
                        teardownTestbed()
                        setupTestbed()
                        script { utilities.invokeGradleNoRetries("integrationTest") }
                    }
                }
                stage('Component Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    options { catchError(message: "Component Tests failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    steps {
                        script {
                            utilities.cleanWithGit()
                            utilities.invokeGradle("clean jar")
                        }
                        teardownTestbed()
                        setupTestbed()
                        script { utilities.invokeGradle("integrationTest") }
                    }
                }
                stage('Reports') {
                    options { catchError(message: "Reports failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    steps {
                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        script { utilities.generateJunitReport('**/test-results/integrationTest/*.xml') }
                    }
                }
                stage('Archive') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        sh 'rm -f *.zip'
                        zip archive: true, dir: '', glob: '**/build/jacoco/integrationTest.exec', zipFile: 'component-test-coverage.zip'
                        zip archive: true, dir: '', glob: '**/test-results/test/*.xml', zipFile: 'component-test-results.zip'
                    }
                }
            }
        }
    }
}

def setupTestbed() {
    sh 'inventory/src/integration/resources/scripts/setup_testbed.sh'
}

def teardownTestbed() {
    sh 'inventory/src/integration/resources/scripts/teardown_testbed.sh'
}
