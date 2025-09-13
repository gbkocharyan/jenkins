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
                    // declare variables once
                    def apiBuild = null
                    def mobileBuild = null
                    def webBuild = null

                    // stash them in binding so they are visible in later stages
                    this.binding.setVariable("apiBuild", apiBuild)
                    this.binding.setVariable("mobileBuild", mobileBuild)
                    this.binding.setVariable("webBuild", webBuild)
                }
            }
        }

        stage('Run Selected Tests in Parallel') {
            steps {
                script {
                    def branches = [:]

                    if (params.RUN_API) {
                        branches['API'] = {
                            def buildResult = build job: 'Api', propagate: false
                            this.binding.setVariable("apiBuild", buildResult)
                        }
                    }
                    if (params.RUN_MOBILE) {
                        branches['MobileTest'] = {
                            def buildResult = build job: 'MobileTest', propagate: false
                            this.binding.setVariable("mobileBuild", buildResult)
                        }
                    }
                    if (params.RUN_UI) {
                        branches['UI'] = {
                            def buildResult = build job: 'UI', propagate: false
                            this.binding.setVariable("webBuild", buildResult)
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
                    def apiBuild = this.binding.hasVariable("apiBuild") ? this.binding.getVariable("apiBuild") : null
                    def mobileBuild = this.binding.hasVariable("mobileBuild") ? this.binding.getVariable("mobileBuild") : null
                    def webBuild = this.binding.hasVariable("webBuild") ? this.binding.getVariable("webBuild") : null

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
                    def apiBuild = this.binding.hasVariable("apiBuild") ? this.binding.getVariable("apiBuild") : null
                    def mobileBuild = this.binding.hasVariable("mobileBuild") ? this.binding.getVariable("mobileBuild") : null
                    def webBuild = this.binding.hasVariable("webBuild") ? this.binding.getVariable("webBuild") : null

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
