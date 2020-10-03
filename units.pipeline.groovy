// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
pipeline {
    agent none
    parameters {
        booleanParam(name: 'QUICK_BUILD', defaultValue: true,
                description: 'Performs a partial clean to speed up the build.')
        choice(name: 'AGENT', choices: [
                'NRE-UNIT',
                'loki-n3-build',
                'Sindhu-test'
        ], description: 'Agent label')
    }
    stages {
        stage ('unit-tests') {
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
                            utilities.CopyIntegrationTestScriptsToBuildDistributions()  // for cleaning this machine
                            utilities.FixFilesPermission()
                            utilities.CleanUpMachine('build/distributions')
                        }
                    }
                }
                stage('Full Clean') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        sh 'rm -rf build'
                        script{ utilities.InvokeGradle("clean") }
                    }
                }
                stage('Partial Clean') {
                    when { expression { "${params.QUICK_BUILD}" == 'true' } }
                    steps {
                        sh 'rm -rf build/distributions/*.sh'
                        script{ utilities.InvokeGradle(":properties:clean") }
                    }
                }
                stage('Unit Tests') {
                    options{ catchError(message: "Unit Tests failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    steps {
                        script { utilities.InvokeGradle("build") }
                        script { utilities.CopyIntegrationTestScriptsToBuildDistributions() }   // for cleaning other machines

                    }
                }
                stage('Reports') {
                    options{ catchError(message: "Reports failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
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
                        archiveArtifacts allowEmptyArchive: true, artifacts:'build/distributions/*.sh, build/reports/**'
                    }
                }
            }
        }
    }
}

