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
                'NRE-BUILD',
                'css-centos-8-00-build'
        ], description: 'Agent label')
    }    

    stages {
        stage ('unit-Component-test') {
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
                        CleanUpMachine()
                    }
                }
                stage('Quick Unit Test') {
                    options{ catchError(message: "Quick Unit Test failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_BUILD}" == 'true' } }
                    steps {
                        script {
                            utilities.InvokeGradle("build")
                        }
                    }
                }
                stage('Quick Component Test') {
                    options{ catchError(message: "Quick Component Test failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_BUILD}" == 'true' } }
                    steps {
                        script {
                            RestartHWInvDb()
                            utilities.InvokeGradle(":dai_core:integrationTest")
                        }
                    }
                }
                stage('Quick Report') {
                    options{ catchError(message: "Quick Report failed", stageResult: 'UNSTABLE',
                            buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_BUILD}" == 'true' } }
                    steps {
                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        junit '**/test-results/**/*.xml'
                    }
                }
                stage('Quick Archive') {
                    when { expression { "${params.QUICK_BUILD}" == 'true' } }
                    steps {
                        CopyIntegrationTestScriptsToBuildDistributions()
                        archiveArtifacts 'build/reports/**, build/distributions/*.sh, build/json-server/**'
                    }
                }
                stage('Full Unit Test') {
                    options{ catchError(message: "Full Unit Test failed", stageResult: 'UNSTABLE',
                            buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        script {
                            sh 'rm -rf build'
                            utilities.InvokeGradle("clean")
                            utilities.InvokeGradle("build")
                        }
                    }
                }
                stage('Full Component Test') {
                    options{ catchError(message: "Full Component Test failed", stageResult: 'UNSTABLE',
                            buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        script {
                            RestartHWInvDb()
                            utilities.InvokeGradle("integrationTest")
                        }
                    }
                }
                stage('Full Report') {
                    options{ catchError(message: "Full Report failed", stageResult: 'UNSTABLE',
                            buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        junit '**/test-results/**/*.xml'
                    }
                }
                stage('Full Archive') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        sh 'rm -f *.zip'
                        CopyIntegrationTestScriptsToBuildDistributions()
                        zip archive: true, dir: '', glob: '**/build/jacoco/test.exec', zipFile: 'unit-test-coverage.zip'
                        zip archive: true, dir: '', glob: '**/main/**/*.java', zipFile: 'src.zip'
                        zip archive: true, dir: '', glob: '**/build/classes/java/main/**/*.class', zipFile: 'classes.zip'
                        zip archive: true, dir: 'inventory_api/src/test/resources/data', glob: '', zipFile: 'hwInvData.zip'
                        zip archive: true, dir: '', glob: '**/test-results/test/*.xml', zipFile: 'unit-test-results.zip'
                        archiveArtifacts 'build/reports/**, build/distributions/*.sh, build/json-server/**'
                    }
                }
            }
        }
    }
}

def RestartHWInvDb() {
    StopHWInvDb()
    StartHWInvDb()
}

// This is one way to setup for component level testing.  You can also use docker-compose or partially
// starts DAI.
// Currently, our docker image has some dependencies that are fulfilled after DAI is installed.  So,
// we cannot use this easily for component tests.
def StartHWInvDb() {
    def scriptDir = 'inventory_api/src/integration/resources/scripts/'
    sh "${scriptDir}init-voltdb.sh"
    sh "${scriptDir}start-voltdb.sh"
    sh "${scriptDir}wait-for-voltdb.sh 21211"
    sh "sqlcmd < ${scriptDir}load_procedures.sql"
    sh "sqlcmd < data/db/DAI-Volt-Tables.sql"
    sh "sqlcmd < data/db/DAI-Volt-Procedures.sql"
}

// Shutdown method depends on how voltdb was procured.  For example,
// if docker-compose was used to procure the voltdb, the relevant
// containers need to be shutdown.
def StopHWInvDb() {
    sh 'voltadmin shutdown --force || true'
}

def CleanUpMachine() {
    CopyIntegrationTestScriptsToBuildDistributions()
    sh './build/distributions/clean_up_machine.sh'
}

def CopyIntegrationTestScriptsToBuildDistributions() {
    sh 'mkdir -p build/json-server'
    sh 'mkdir -p build/distributions'
    sh 'cp ./inventory_api/src/integration/resources/scripts/json-server/* ./build/json-server'
    sh 'cp ./inventory_api/src/integration/resources/scripts/clean_up_machine.sh ./build/distributions'
}
