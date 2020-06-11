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
        stage('Unit') {
            agent { label "${AGENT}" }
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
                    sh 'rm -rf build/distributions'
                    sh 'rm -rf build/reports/spotbugs'
                    utilities.FixFilesPermission()

                    if ( "${params.QUICK_BUILD}" == 'true' ) {
                        utilities.InvokeGradle(":inventory_api:clean")
                    } else {
                        utilities.InvokeGradle("clean")
                    }

                    utilities.InvokeGradle("build || true")
                    utilities.InvokeGradle("check || true")  //add spotbugs

                    jacoco classPattern: '**/classes/java/main/com/intel/'
                    junit '**/test-results/**/*.xml'

                    // check to see if the distribution folder is created
                    sh 'ls build/distributions'
                }

                sh 'rm -f *.zip'
                zip archive: true, dir: '', glob: '**/build/jacoco/test.exec', zipFile: 'unit-test-coverage.zip'
                zip archive: true, dir: '', glob: '**/main/**/*.java', zipFile: 'src.zip'
                zip archive: true, dir: '', glob: '**/build/classes/java/main/**/*.class', zipFile: 'classes.zip'
                zip archive: true, dir: 'inventory_api/src/test/resources/data', glob: '', zipFile: 'hwInvData.zip'
                zip archive: true, dir: '', glob: '**/test-results/test/*.xml', zipFile: 'unit-test-results.zip'
                archiveArtifacts 'build/distributions/**, build/reports/**'
            }
        }
    }
}
