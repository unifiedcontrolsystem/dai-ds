pipeline {
    agent { label 'NRE-MASTER-CD' }

    stages {
        stage('Preparation') {
            steps {
                sh 'hostname'
                lastChanges since: 'PREVIOUS_REVISION', format: 'SIDE', matching: 'LINE'
            }
        }
        stage('Run Master CD') {
            steps {
                build job: 'master-continuous-delivery-pipeline',
                        quietPeriod: 0, wait: true
            }
        }
    }
}
