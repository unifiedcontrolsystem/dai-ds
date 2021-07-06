// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
pipeline {
    options { disableConcurrentBuilds() }
    agent none
    parameters {
        booleanParam(name: 'QUICK_BUILD', defaultValue: true,
                description: 'Performs a partial clean to speed up the build.')
        choice(name: 'AGENT', choices: ['COMPONENT'], description: 'Agent label')
    }
    stages {
        stage('Restart Component Test Servers') {
            agent { label "DOCKER-TEST-SERVER-HOST" }
            steps {
                script { utilities.fixFilesPermission() }
                dir ('inventory/src/integration/resources/scripts') {
                    sh './restart_voltdb.sh'
                }
            }
        }
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
                            utilities.fixFilesPermission()
                            utilities.cleanUpMachine('.')
                        }
                    }
                }
                stage('Quick Component Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'true' } }
                    steps {
                        script {utilities.invokeGradleNoRetries("jar")}
                        teardownTestbed()
                        setupTestbed()
                        script { utilities.invokeGradleNoRetries("integrationTest") }
                    }
                }
                stage('Component Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        script {
                            utilities.cleanWithGit()
                            utilities.invokeGradleNoRetries("clean jar")
                        }
                        teardownTestbed()
                        setupTestbed()
                        script { utilities.invokeGradleNoRetries("integrationTest") }
                    }
                }
                stage('Reports') {
                    steps {
                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        junit allowEmptyResults: true, keepLongStdio: true, skipPublishingChecks: true,
                                testResults: '**/test-results/integrationTest/*.xml'
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
