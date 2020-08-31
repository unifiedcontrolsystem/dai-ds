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

                        script { utilities.FixFilesPermission() }
                        CleanUpMachine()
                    }
                }
                stage('Quick Unit Test') {
                    options{ catchError(message: "Quick Unit Test failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_CHECK}" == 'true' } }
                    steps {
                        script {
//                            utilities.InvokeGradle("build")
//                            utilities.InvokeGradle(":dai_core:clean")
//                            utilities.InvokeGradle(":inventory_api:test")
//                            utilities.InvokeGradle(":inventory:test")
                            utilities.InvokeGradle(":dai_core:test")
//                            utilities.InvokeGradle(":dai_network_listener:test")
//                            utilities.InvokeGradle(":procedures:test")
                        }
                    }
                }
                stage('Quick Component Test') {
                    options{ catchError(message: "Quick Component Test failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_CHECK}" == 'true' } }
                    steps {
                        script {
                            RestartHWInvDb()
                            utilities.InvokeGradle(":dai_core:integrationTest")
                            utilities.InvokeGradle("makeAllArtifacts")
                        }
                    }
                }
                stage('Quick Report') {
                    options{ catchError(message: "Quick Report failed", stageResult: 'UNSTABLE',
                            buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_CHECK}" == 'true' } }
                    steps {
                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        junit '**/test-results/**/*.xml'
                    }
                }
                stage('Quick Archive') {
                    when { expression { "${params.QUICK_CHECK}" == 'true' } }
                    steps {
                        CopyCleanUpMachineScript()
                        archiveArtifacts 'build/reports/**, build/distributions/**, build/distribution/clean_up_machine.sh'
                    }
                }
                stage('Full Unit Test') {
                    options{ catchError(message: "Full Unit Test failed", stageResult: 'UNSTABLE',
                            buildResult: 'UNSTABLE') }
                    when { expression { "${params.QUICK_CHECK}" == 'false' } }
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
                    when { expression { "${params.QUICK_CHECK}" == 'false' } }
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
                    when { expression { "${params.QUICK_CHECK}" == 'false' } }
                    steps {
                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        junit '**/test-results/**/*.xml'
                    }
                }
                stage('Full Archive') {
                    when { expression { "${params.QUICK_CHECK}" == 'false' } }
                    steps {
                        sh 'rm -f *.zip'
                        CopyCleanUpMachineScript()
                        zip archive: true, dir: '', glob: '**/build/jacoco/test.exec', zipFile: 'unit-test-coverage.zip'
                        zip archive: true, dir: '', glob: '**/main/**/*.java', zipFile: 'src.zip'
                        zip archive: true, dir: '', glob: '**/build/classes/java/main/**/*.class', zipFile: 'classes.zip'
                        zip archive: true, dir: 'inventory_api/src/test/resources/data', glob: '', zipFile: 'hwInvData.zip'
                        zip archive: true, dir: '', glob: '**/test-results/test/*.xml', zipFile: 'unit-test-results.zip'
                        archiveArtifacts 'build/reports/**, build/distributions/**, build/distribution/clean_up_machine.sh'
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
    CopyCleanUpMachineScript()
    sh './build/distributions/clean_up_machine.sh'
}

def CopyCleanUpMachineScript() {
    sh 'mkdir -p build/distributions'
    sh 'cp ./inventory_api/src/integration/resources/scripts/clean_up_machine.sh ./build/distributions'
}
