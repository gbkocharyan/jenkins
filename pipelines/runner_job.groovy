node('maven_gev') {

    // Declare build variables
    def apiBuild = null
    def mobileBuild = null
    def webBuild = null

    stage('Run All Tests in Parallel') {
        parallel(
                'API Tests': {
                    script {
                        apiBuild = build job: 'Api', propagate: false
                    }
                },
                'Mobile Tests': {
                    script {
                        mobileBuild = build job: 'Mobile', propagate: false
                    }
                },
                'Web Tests': {
                    script {
                        webBuild = build job: 'Web', propagate: false
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
                        projectName: 'Web',
                        selector: lastSuccessful(),
                        filter: 'allure-results/**',
                        target: 'allure-results/web',
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
                    [path: 'allure-results/web']
            ]
        }
    }
}
