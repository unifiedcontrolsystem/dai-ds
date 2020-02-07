// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
pipeline {
    agent none
    parameters {
        booleanParam(name: 'QUICK_BUILD', defaultValue: false,
        description: 'Speeds up build by only performing a partial gradle clean')
    }    
    stages {
        stage('Component Tests') {
            agent { label 'Nightly-Build' }
            environment {
                PATH = "${PATH}:/home/${USER}/voltdb9.1/bin"
            }
            steps {
                lastChanges format: 'LINE', matchWordsThreshold: '0.25', matching: 'NONE',
                        matchingMaxComparisons: '1000', showFiles: true, since: 'PREVIOUS_REVISION',
                        specificBuild: '', specificRevision: '', synchronisedScroll: true, vcsDir: ''

                copyArtifacts filter: '**', fingerprintArtifacts: true,
                        projectName: "external-components",
                        selector: lastWithArtifacts()

                script {
                    utilities.FixFilesPermission()
                    StopHWInvDb()
                    StartHWInvDb()

                    if ( "${params.QUICK_BUILD}" == 'true' ) {
                        echo '*** This is a QUICK build'
                        utilities.InvokeGradle(":inventory:clean")
                    } else {
                        echo '*** This is a CLEAN build'
                        utilities.InvokeGradle("clean")
                    }

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

def StartHWInvDb() {
    sh './init-voltdb.sh'
    sh './start-voltdb.sh'
    sh './wait-for-voltdb.sh 21211'
    sh 'sqlcmd < hw-inv.sql'
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
