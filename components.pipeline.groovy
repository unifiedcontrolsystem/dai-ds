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
            environment { PATH = "${PATH}:/home/${USER}/voltdb9.1/bin" }
            stages {    // another stages is required to force operations on the same machine
                stage('Preparation') {
                    steps {
                        echo "Building on ${AGENT}"
                        sh 'hostname'

                        lastChanges format: 'LINE', matchWordsThreshold: '0.25', matching: 'NONE',
                                matchingMaxComparisons: '1000', showFiles: true, since: 'PREVIOUS_REVISION',
                                specificBuild: '', specificRevision: '', synchronisedScroll: true, vcsDir: ''

                        script {
                            utilities.copyIntegrationTestScriptsToBuildDistributions()
                            utilities.fixFilesPermission()
                            utilities.cleanUpMachine('build/distributions')
                        }
                    }
                }
                stage('Quick Component Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'true' } }
                    options{ catchError(message: "Quick Component Tests failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    steps {
                        script { utilities.invokeGradle("jar") }
                        StartHWInvDb()
                        script {utilities.invokeGradle("integrationTest") }
                        StopHWInvDb()
                    }
                }
                stage('Component Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    options{ catchError(message: "Component Tests failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    steps {
                        sh 'rm -rf build'
                        script { utilities.invokeGradle("clean jar") }
                        StartHWInvDb()
                        script { utilities.invokeGradle("integrationTest") }
                        StopHWInvDb()
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
                        sh 'rm -f *.zip'
                        zip archive: true, dir: '', glob: '**/build/jacoco/integrationTest.exec', zipFile: 'component-test-coverage.zip'
                        zip archive: true, dir: '', glob: '**/test-results/test/*.xml', zipFile: 'component-test-results.zip'
                        archiveArtifacts allowEmptyArchive: true, artifacts: 'build/reports/**'
                    }
                }
            }
        }
    }
}

// This is one way to setup for component level testing.  You can also use docker-compose or partially
// starts DAI.
// Currently, our docker image has some dependencies that are fulfilled after DAI is installed.  So,
// we cannot use this easily for component tests.
def StartHWInvDb() {
    def scriptDir = './inventory/src/integration/resources/scripts'
    sh "${scriptDir}/init-voltdb.sh"
    sh "${scriptDir}/start-voltdb.sh"
    sh "${scriptDir}/wait-for-voltdb.sh 21211"
    sh "sqlcmd < ${scriptDir}/load_procedures.sql"
    sh "sqlcmd < data/db/DAI-Volt-Tables.sql"
    sh "sqlcmd < data/db/DAI-Volt-Procedures.sql"
}

// Shutdown method depends on how voltdb was procured.  For example,
// if docker-compose was used to procure the voltdb, the relevant
// containers need to be shutdown.
def StopHWInvDb() {
    sh 'voltadmin shutdown --force || true'
}
