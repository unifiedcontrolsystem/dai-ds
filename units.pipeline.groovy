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
                'NRE-UNIT'
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
                        script { utilities.updateBuildName() }
                        echo "Building on ${AGENT}"
                        sh 'hostname'

                        lastChanges since: 'PREVIOUS_REVISION', format: 'SIDE', matching: 'LINE'

                        script {
                            utilities.fixFilesPermission()
                            utilities.cleanUpMachine('.')
                            // You can no longer run component tests and unit tests concurrently on the same machine
                        }
                    }
                }
                stage('Pytests') {
                    options { catchError(message: "Pytests failed", stageResult: 'UNSTABLE', buildResult: 'SUCCESS') }
                    steps {
                        dir('cli/cli') {
                            sh 'pytest . --cov-config=.coveragerc --cov=cli --cov-report term-missing' +
                                    ' --cov-report xml:results.xml --cov-report html:coverage-report.xml --fulltrace'
                        }
                    }
                }
                stage('Quick Unit Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'true' } }
                    steps {
                        // Quick build will not produce artifacts for components not tested
                        // So, functional tests cannot use these artifacts                        sh 'rm -rf build/distributions'
                        script { utilities.invokeGradleNoRetries("build") }
                        sh 'touch inventory/build/test-results/test/*.xml'  // prevents junit report failures if no tests were run
                    }
                }
                stage('Unit') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        script {
                            utilities.cleanWithGit()
                            utilities.invokeGradleNoRetries("clean")
                            utilities.invokeGradleNoRetries("build")
                        }
                    }
                }
                stage('Reports') {
                    steps {
                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        junit allowEmptyResults: true, keepLongStdio: true, skipPublishingChecks: true,
                                testResults: '**/test-results/test/*.xml'
                    }
                }
                stage('Archive') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        // Archive zips for total coverage reports generation later
                        zip archive: true, dir: '', glob: '**/build/jacoco/test.exec', zipFile: 'unit-test-coverage.zip'
                        zip archive: true, dir: '', glob: '**/main/**/*.java', zipFile: 'src.zip'
                        zip archive: true, dir: '', glob: '**/build/classes/java/main/**/*.class', zipFile: 'classes.zip'
                        zip archive: true, dir: '', glob: '**/test-results/test/*.xml', zipFile: 'unit-test-results.zip'

                        fileOperations([fileCopyOperation(
                                includes: 'cleanup_machine.sh',
                                targetLocation: 'build/distributions')])    // for clean other test machines

                        fileOperations([fileCopyOperation(
                                includes: 'data/db/*.sql build/distributions/',
                                targetLocation: 'build/distributions')])    // for database debugging

                        archiveArtifacts allowEmptyArchive: false, artifacts: 'build/distributions/*.rpm'
                        archiveArtifacts allowEmptyArchive: false, artifacts: 'build/distributions/cleanup_machine.sh'
                        archiveArtifacts allowEmptyArchive: false, artifacts: 'build/reports/**'
                    }
                }
            }
        }
    }
}
