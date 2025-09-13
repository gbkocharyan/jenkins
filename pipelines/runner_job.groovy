pipeline {
    agent { label 'maven_gev' }

    parameters {
        booleanParam(name: 'RUN_API', defaultValue: true, description: 'Run API Tests')
        booleanParam(name: 'RUN_MOBILE', defaultValue: true, description: 'Run Mobile Tests')
        booleanParam(name: 'RUN_UI', defaultValue: true, description: 'Run UI Tests')
    }

    stages {
        stage('Init') {
            steps {
                script {
                    apiBuild = null
                    mobileBuild = null
                    webBuild = null
                }
            }
        }

        stage('Run Selected Tests in Parallel') {
            steps {
                script {
                    def branches = [:]

                    if (params.RUN_API) {
                        branches['API'] = {
                            apiBuild = build job: 'Api', propagate: false
                        }
                    }
                    if (params.RUN_MOBILE) {
                        branches['MobileTest'] = {
                            mobileBuild = build job: 'MobileTest', propagate: false
                        }
                    }
                    if (params.RUN_UI) {
                        branches['UI'] = {
                            webBuild = build job: 'UI', propagate: false
                        }
                    }

                    if (branches) {
                        parallel branches
                    } else {
                        echo "⚠️ No tests selected, skipping execution"
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
