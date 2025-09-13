pipeline {
    agent { label 'maven_gev' }

    parameters {
        choice(name: 'RUN_TESTS', choices: ['API', 'Mobile', 'Web'], description: 'Select which tests to run')
    }

    stages {
        stage('Run Selected Tests') {
            steps {
                script {
                    def apiBuild = null
                    def mobileBuild = null
                    def webBuild = null

                    if (params.RUN_TESTS == 'Api') {
                        apiBuild = build job: 'Api', wait: true
                    }
                    if (params.RUN_TESTS == 'MobileTest') {
                        mobileBuild = build job: 'MobileTest', wait: true
                    }
                    if (params.RUN_TESTS == 'Web') {
                        webBuild = build job: 'Web', wait: true
                    }
                }
            }
        }

        stage('Collect Allure Results') {
            steps {
                script {
                    if (apiBuild) {
                        copyArtifacts(
                                projectName: 'Api',
                                selector: specific(apiBuild.number.toString()),
                                filter: 'allure-results/**',
                                target: 'allure-results/api',
                                flatten: true,
                                optional: true
                        )
                    }
                    if (mobileBuild) {
                        copyArtifacts(
                                projectName: 'MobileTest',
                                selector: specific(mobileBuild.number.toString()),
                                filter: 'allure-results/**',
                                target: 'allure-results/mobile',
                                flatten: true,
                                optional: true
                        )
                    }
                    if (webBuild) {
                        copyArtifacts(
                                projectName: 'UI',
                                selector: specific(webBuild.number.toString()),
                                filter: 'allure-results/**',
                                target: 'allure-results/ui',
                                flatten: true,
                                optional: true
                        )
                    }
                }
            }
        }

        stage('Generate Allure Report') {
            steps {
                script {
                    def results = []
                    if (apiBuild) results << [path: 'allure-results/api']
                    if (mobileBuild) results << [path: 'allure-results/mobile']
                    if (webBuild) results << [path: 'allure-results/ui']

                    if (results) {
                        allure includeProperties: false, jdk: '', results: results
                    } else {
                        echo "⚠️ No test results to generate report"
                    }
                }
            }
        }
    }
}
