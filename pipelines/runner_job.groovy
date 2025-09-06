node('maven_gev') {

    // Declare build variables
    def apiBuild = null
    def mobileBuild = null
    def webBuild = null

    stage('Run All Tests in Parallel') {
        parallel(
                'API': {
                    script {
                        apiBuild = build job: 'Api', propagate: false
                    }
                },
                'MobileTest': {
                    script {
                        mobileBuild = build job: 'MobileTest', propagate: false
                    }
                },
                'UI': {
                    script {
                        webBuild = build job: 'UI', propagate: false
                    }
                }
        )
    }

    stage('Collect Allure Results') {
        script {
            echo "API build number: ${apiBuild.number}, result: ${apiBuild.result}"
            echo "Mobile build number: ${mobileBuild.number}, result: ${mobileBuild.result}"
            echo "Web build number: ${webBuild.number}, result: ${webBuild.result}"

            if (apiBuild != null) {
                copyArtifacts(
                        projectName: 'Api',
                        selector: lastSuccessful(),
                        filter: 'allure-results/**',
                        target: 'allure-results/api',
                        flatten: true,
                        optional: true
                )
            }
            if (mobileBuild != null) {
                copyArtifacts(
                        projectName: 'Mobile',
                        selector: lastSuccessful(),
                        filter: 'allure-results/**',
                        target: 'allure-results/mobile',
                        flatten: true,
                        optional: true
                )
            }
            if (webBuild != null) {
                copyArtifacts(
                        projectName: 'UI',
                        selector: lastSuccessful(),
                        filter: 'allure-results/**',
                        target: 'allure-results/ui',
                        flatten: true,
                        optional: true
                )
            }
        }
    }

    stage('Generate Allure Report') {
        script {
            allure includeProperties: false, jdk: '', results: [
                    [path: 'allure-results/api'],
                    [path: 'allure-results/mobile'],
                    [path: 'allure-results/ui']
            ]
        }
    }
}
