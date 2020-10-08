// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

/**
 ** This is the sandbox unit test.
 **/
pipeline {
    agent none
    parameters {
        /**
         * You must ALSO create the following string parameters by hand in the Jenkins GUI.
         *  The allows the pipeline to be manually booted.  Note that the option lightweight
         *  checkout MUST be selected in the job page.
         **/
        string(name: 'repository',
                defaultValue: 'ssh://cmcheung@git-amr-1.devtools.intel.com:29418/ccg_widi_automation_tutorials-dai',
                description: 'Repository, perhaps your GitHub fork.')
        string(name: 'branch', defaultValue: 'master',
                description: 'Branch, perhaps the branch on which you are developing/.')

        /**
         * You do NOT need to create the following in the Jenkins GUI by hand.  This pipeline will create the
         * Jenkins GUI.  The UI elements will be available after the first pipeline run.
        **/
        string(name: 'unitAgent', defaultValue: 'UNIT', description: 'Enter your UNIT test machine')
        booleanParam(name: 'QUICK_BUILD', defaultValue: false,
                description: 'Performs a partial clean to speed up the build.  No FUNCTIONAL test will be done.')

        string(name: 'functionalTestRepository',
                defaultValue: 'ssh://cmcheung@git-amr-1.devtools.intel.com:29418/ccg_widi_automation_tutorials-functional',
                description: 'Repository, perhaps your GitHub fork.')
        string(name: 'functionalTestBranch', defaultValue: 'master',
                description: 'Branch, perhaps the branch on which you are developing/.')

        string(name: 'functionalAgent', defaultValue: 'FUNCTIONAL', description: 'Enter your FUNCTIONAL test machine')
        choice(name: 'functionTestTag', choices: [
                '@beingDebugged',
                '@underDevelopment',
                '@diagnostics',
                '@inventory',
                '@Cobalt',
                '@Ras',
                '@CLI',
                '@NearlineAdapter',
                '@DAIManager',
                '@eventsim-installation',
                '@thirdparty-installation',
                '@ucs-installation',
                '@fabric-adapter',
                '@provisioner-adapter',
                '@rabbitmq-snooper',
                'ALL'
        ], description: 'Tag of functional tests to run')
    }
    stages {
        stage('Sequential Stages') { // all the sub-stages needs to be run on the same machine
            agent { label "${unitAgent}" }
            environment {
                PATH = "${PATH}:/home/${USER}/voltdb9.1/bin"
            }
            stages {    // another stages is required to force operations on the same machine
                stage('Preparation') {
                    steps {
                        echo "repo: ${repository} on */${branch}"
                        echo "Building on ${unitAgent}"
                        sh 'hostname'

                        lastChanges since: 'PREVIOUS_REVISION', format:'SIDE', matching: 'LINE'

                        script {
                            utilities.copyIntegrationTestScriptsToBuildDistributions()  // for cleaning this machine
                            utilities.fixFilesPermission()
                            // Do NOT clean the machine here!  It will break component tests
                        }
                    }
                }
                stage('Quick Unit Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'true' } }
                    options { catchError(message: "Quick Unit Tests failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    steps {
                        // Quick build will not produce artifacts for components not tested
                        // So, functional tests cannot use these artifacts
                        sh 'rm -rf build/distributions'
                        script { utilities.invokeGradle("build") }
                    }
                }
                stage('Unit') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    options { catchError(message: "Unit Tests failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    steps {
                        sh 'rm -rf build'
                        script { utilities.invokeGradle("clean build") }
                    }
                }
                stage('Reports') {
                    options { catchError(message: "Reports failed", stageResult: 'UNSTABLE', buildResult: 'UNSTABLE') }
                    steps {
                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        junit allowEmptyResults: true, testResults: '**/test-results/test/*.xml'
                        archiveArtifacts allowEmptyArchive: true, artifacts: 'build/reports/**'
                    }
                }
                stage('Launch Functional Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        // Archive zips for total coverage report generation later
                        sh 'rm -f *.zip'
                        zip archive: true, dir: '', glob: '**/build/jacoco/test.exec', zipFile: 'unit-test-coverage.zip'
                        zip archive: true, dir: '', glob: '**/main/**/*.java', zipFile: 'src.zip'
                        zip archive: true, dir: '', glob: '**/build/classes/java/main/**/*.class', zipFile: 'classes.zip'
                        zip archive: true, dir: '', glob: '**/test-results/test/*.xml', zipFile: 'unit-test-results.zip'

                        script { utilities.copyIntegrationTestScriptsToBuildDistributions() }   // for cleaning functional machines
                        sh 'cp data/db/*.sql build/distributions/'  // for database debugging
                        archiveArtifacts allowEmptyArchive: false, artifacts: 'build/distributions/*.s*'

                        build job: 'functional-tests',
                                parameters: [
                                        string(name: 'repository', value: "${params.functionalTestRepository}"),
                                        string(name: 'branch', value: "${params.functionalTestBranch}"),
                                        string(name: 'INSTALLER_SOURCE', value: "${env.JOB_BASE_NAME}"),
                                        string(name: 'tag', value: "${params.functionTestTag}")
                                ],
                                quietPeriod: 0, wait: false
                    }
                }
            }
        }
    }
}
