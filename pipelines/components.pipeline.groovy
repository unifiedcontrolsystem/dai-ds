// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
pipeline {
    agent none
    parameters {
        string(name: 'funtionalTestPipeline', defaultValue: 'functional-tests',
                description: 'Functional Test Pipeline to extract the script to clean the machine.')
        booleanParam(name: 'QUICK_BUILD', defaultValue: false,
                description: 'Speeds up build by only performing a partial gradle clean')
        choice(name: 'AGENT', choices: [
                'NRE-TEST',
                'cmcheung-centos-7-test',
                'css-centos-8-00-test',
                'css-centos-8-01-test'
        ], description: 'Agent label')
    }
    stages {
        stage('Component Tests') {
            agent { label "${AGENT}" }
            environment {
                PATH = "${PATH}:/home/${USER}/voltdb9.1/bin"
            }
            steps {
                echo "Building on ${AGENT}"
                sh 'hostname'
                lastChanges format: 'LINE', matchWordsThreshold: '0.25', matching: 'NONE',
                        matchingMaxComparisons: '1000', showFiles: true, since: 'PREVIOUS_REVISION',
                        specificBuild: '', specificRevision: '', synchronisedScroll: true, vcsDir: ''

                sh 'rm -rf build/tmp/cleanup-scripts'
                dir ('build/tmp/cleanup-scripts') {
                    copyArtifacts fingerprintArtifacts: true, projectName: "${params.funtionalTestPipeline}",
                            excludes: '*.zip',
                            selector: lastWithArtifacts()
                    script { utilities.CleanUpMachine() }
                }

                script {
                    def scriptDir = 'inventory_api/src/integration/resources/scripts/'

                    utilities.FixFilesPermission()
                    StopHWInvDb()

                    if ( "${params.QUICK_BUILD}" == 'true' ) {
                        utilities.InvokeGradle(":inventory_api:clean")
                    } else {
                        utilities.InvokeGradle("clean")
                    }

                    utilities.InvokeGradle(":procedures:jar")
                    StartHWInvDb(scriptDir)

                    TestStoredProcedures(scriptDir, 'tests.sql', 'testOutput.txt')
                    utilities.InvokeGradle("compilejava compiletestjava compileIntegrationGroovy")
                    utilities.InvokeGradle("integrationTest || true")

                    StopHWInvDb()
                }

                sh 'rm -f *.zip'
                zip archive: true, dir: '', glob: '**/build/jacoco/integrationTest.exec', zipFile: 'component-test-coverage.zip'
                zip archive: true, dir: '', glob: '**/test-results/test/*.xml', zipFile: 'component-test-results.zip'

                jacoco classPattern: '**/classes/java/main/com/intel/', execPattern: '**/integrationTest.exec'
                junit '**/test-results/**/*.xml'
            }
        }
    }
}

def TestStoredProcedures(def scriptDir, def tests, def testOutput) {
    sh "rm -f ${testOutput}"
    sh "sqlcmd < ${scriptDir}${tests} > ${testOutput}"
//            "| sed '/Returned/ d' > ${testOutput}"
//    sh "diff ${testOutput} ${scriptDir}${testOutput}"
}

def StartHWInvDb(def scriptDir) {
    sh "${scriptDir}init-voltdb.sh"
    sh "${scriptDir}start-voltdb.sh"
    sh "${scriptDir}wait-for-voltdb.sh 21211"
    sh "sqlcmd < ${scriptDir}hw-inv.sql"
}
def StopHWInvDb() {
    StopHWInvDocker()
    sh 'voltadmin shutdown --force || true'
}
def StopHWInvDocker() {
    sh 'docker-compose -f /opt/dai-docker/voltdb.yml down || true'
    sh 'docker-compose -f /opt/dai-docker/postgres.yml down || true'
    sh 'docker stop hw-inv-db || true'
    sh 'docker rm hw-inv-db || true'
}
