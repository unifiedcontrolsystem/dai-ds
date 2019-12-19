pipeline {
    agent none
    stages {
        stage('Unit + Integration') {
            parallel {
                stage('Unit Tests') {
                    agent { label 'NRE-BUILD' }
                    steps {
                        lastChanges format: 'LINE', matchWordsThreshold: '0.25', matching: 'NONE',
                                matchingMaxComparisons: '1000', showFiles: true, since: 'PREVIOUS_REVISION',
                                specificBuild: '', specificRevision: '', synchronisedScroll: true, vcsDir: ''

                        script {
                            utilities.FixFilesPermission()
                            // sh 'docker-build/docker-build.sh clean build || true'
                            utilities.InvokeGradle(":foreign_bus:clean compilejava compiletestjava compiletestgroovy")
                            utilities.InvokeGradle("build || true")
                        }

                        sh 'rm -f *.zip'
                        zip archive: true, dir: '', glob: '**/build/jacoco/test.exec', zipFile: 'unit-test-coverage.zip'
                        zip archive: true, dir: '', glob: '**/main/**/*.java', zipFile: 'src.zip'
                        zip archive: true, dir: '', glob: '**/build/classes/java/main/**/*.class', zipFile: 'classes.zip'
                        zip archive: true, dir: 'foreign_bus/src/test/resources/data', glob: '', zipFile: 'hwInvData.zip'
                        zip archive: true, dir: '', glob: '**/test-results/test/*.xml', zipFile: 'unit-and-integration-test-results.zip'
                        archiveArtifacts 'build/distributions/**, build/reports/**'

                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        junit '**/test-results/**/*.xml'
                    }
                }
                stage('Integration Tests') {
                    agent { label 'NRE-TEST' }
                    environment {
                        PATH = "${PATH}:/home/${USER}/voltdb9.1/bin"
                    }
                    steps {
                        copyArtifacts filter: '**', fingerprintArtifacts: true,
                                projectName: "Components",
                                selector: lastWithArtifacts()

                        script {
                            utilities.FixFilesPermission()
                            StartHWInvDb()
                            utilities.InvokeGradle(":foreign_bus:clean compilejava compiletestjava compileIntegrationGroovy")
                            utilities.InvokeGradle("integrationTest || true")
                            StopHWInvDb()
                        }

                        sh 'rm -f *.zip'
                        zip archive: true, dir: '', glob: '**/build/jacoco/integrationTest.exec', zipFile: 'integration-test-coverage.zip'

                        jacoco classPattern: '**/classes/java/main/com/intel/', execPattern: '**/integrationTest.exec'
                        junit '**/test-results/**/*.xml'
                    }
                }
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
    sh 'voltadmin shutdown --force || true'
}
