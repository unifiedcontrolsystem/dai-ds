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
        stage('Unit') {
            agent { label 'NRE-BUILD' }
            steps {
                lastChanges format: 'LINE', matchWordsThreshold: '0.25', matching: 'NONE',
                        matchingMaxComparisons: '1000', showFiles: true, since: 'PREVIOUS_REVISION',
                        specificBuild: '', specificRevision: '', synchronisedScroll: true, vcsDir: ''

                script {
                    sh 'rm -rf build/distributions'
                    utilities.FixFilesPermission()

                    if ( "${params.QUICK_BUILD}" == 'true' ) {
                        utilities.InvokeGradle(":inventory_api:clean")
                    } else {
                        utilities.InvokeGradle("clean")
                    }

                    utilities.InvokeGradle("build || true")

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
