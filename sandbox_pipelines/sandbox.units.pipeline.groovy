// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

pipeline {
    agent none
    parameters {
        /**
         * You must ALSO create the following string parameters by hand in the Jenkins GUI.
         *  The allows the pipeline to be manually booted.  Note that the option lightweight
         *  checkout MUST be selected in the job page.
         **/
        string(name: 'repository',
                defaultValue: 'https://github.com/unifiedcontrolsystem/dai-ds.git',
                description: 'Repository, perhaps your GitHub fork.')
        string(name: 'branch', defaultValue: 'master',
                description: 'Branch, perhaps the branch on which you are developing.')

        /**
         * You do NOT need to create the following in the Jenkins GUI by hand.  This pipeline will create the
         * Jenkins GUI.  The UI elements will be available after the first pipeline run.
         **/
        string(name: 'reason', defaultValue: 'developer build',
                description: 'Reason for launching this build.')

        string(name: 'unitAgent', defaultValue: 'UNIT', description: 'Enter your UNIT test machine')
        booleanParam(name: 'QUICK_BUILD', defaultValue: false,
                description: 'Performs a partial clean to speed up the build.  No FUNCTIONAL test will be done.')

        string(name: 'functionalTestRepository',
                defaultValue: 'ssh://sid-gerrit.devtools.intel.com:29418/css/dai_val',
                description: 'Repository, perhaps your GitHub fork.')
        string(name: 'functionalTestBranch', defaultValue: 'sandbox',
                description: 'Branch, perhaps the branch on which you are developing.')

        string(name: 'functionalAgent', defaultValue: 'FUNCTIONAL', description: 'Enter your FUNCTIONAL test machine')
        choice(name: 'functionalTestTag', choices: [
                '@beingDebugged',
                '@smokeTest',
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
                        script { utilities.updateBuildName() }
                        buildDescription "${params.repository}@${params.branch}"

                        echo "Building on ${unitAgent}"
                        sh 'hostname'
                        echo "Reason for build: ${params.reason}"

                        lastChanges since: 'PREVIOUS_REVISION', format: 'SIDE', matching: 'LINE'

                        script {
                            utilities.fixFilesPermission()
                            utilities.cleanUpMachine('.')
                            // You can no longer run component tests and unit tests concurrently on the same machine
                        }
                    }
                }
                stage('Launch Component Tests') {
                    steps {
                        build job: 'component-tests',
                                parameters: [booleanParam(name: 'QUICK_BUILD', value: "${params.QUICK_BUILD}")],
                                quietPeriod: 0, wait: false
                    }
                }
                stage('Pytests') {
                    options { catchError(message: "Pytests failed", stageResult: 'FAILURE', buildResult: 'FAILURE') }
                    steps {
                        script { utilities.invokeGradleNoRetries("testPython") }
                    }
                }
                stage('Quick Unit Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'true' } }
                    options { catchError(message: "Quick Unit Tests failed", stageResult: 'FAILURE', buildResult: 'FAILURE') }
                    steps {
                        // Quick build will not produce artifacts for components not tested
                        // So, functional tests cannot use these artifacts
                        sh 'rm -rf build/distributions'
                        script { utilities.invokeGradleNoRetries("build") }
                        sh 'touch inventory/build/test-results/test/*.xml'  // prevents junit report failures if no tests were run
                    }
                }
                stage('Unit') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    options { catchError(message: "Full Unit Tests failed", stageResult: 'FAILURE', buildResult: 'FAILURE') }
                    steps {
                        script {
                            utilities.cleanWithGit()
                            utilities.invokeGradleNoRetries("clean")
                            utilities.invokeGradleNoRetries("build")
                        }
                    }
                }
                stage('Reports') {
                    steps {
                        jacoco classPattern: '**/classes/java/main/com/intel/'
                        junit allowEmptyResults: true, keepLongStdio: true, skipPublishingChecks: true,
                                testResults: '**/test-results/test/*.xml'
                    }
                }
                stage('Archive') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        // Archive zips for total coverage reports generation later
                        zip archive: true, dir: '', glob: '**/build/jacoco/test.exec', zipFile: 'unit-test-coverage.zip'
                        zip archive: true, dir: '', glob: '**/main/**/*.java', zipFile: 'src.zip'
                        zip archive: true, dir: '', glob: '**/build/classes/java/main/**/*.class', zipFile: 'classes.zip'
                        zip archive: true, dir: '', glob: '**/test-results/test/*.xml', zipFile: 'unit-test-results.zip'

                        fileOperations([fileCopyOperation(
                                includes: 'cleanup_machine.sh',
                                targetLocation: 'build/distributions')])    // for clean other test machines

                        fileOperations([fileCopyOperation(
                                includes: 'data/db/*.sql build/distributions/',
                                targetLocation: 'build/distributions')])    // for database debugging

                        archiveArtifacts allowEmptyArchive: false, artifacts: 'build/distributions/*.rpm'
                        archiveArtifacts allowEmptyArchive: false, artifacts: 'build/distributions/cleanup_machine.sh'
                        archiveArtifacts allowEmptyArchive: false, artifacts: 'build/reports/**'
                    }
                }
                stage('Launch Functional Tests') {
                    when { expression { "${params.QUICK_BUILD}" == 'false' } }
                    steps {
                        build job: 'functional-tests',
                                parameters: [
                                        string(name: 'repository', value: "${params.functionalTestRepository}"),
                                        string(name: 'branch', value: "${params.functionalTestBranch}"),
                                        string(name: 'INSTALLER_SOURCE', value: "${JOB_BASE_NAME}"),
                                        string(name: 'tag', value: "${params.functionalTestTag}"),
                                        string(name: 'AGENT', value: "${params.functionalAgent}")
                                ],
                                quietPeriod: 0, wait: false
                    }
                }
            }
        }
    }
}
