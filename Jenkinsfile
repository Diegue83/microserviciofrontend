pipeline {
    agent any
    tools {
        // Deben estar definidos en Jenkins -> Global Tool Configuration
        jdk 'JDK21'
        maven 'Maven 3.9.0'
    }
    stages {
        stage('Checkout') {
            steps {
                // Descarga el código del repositorio
                git branch: 'main', url: 'https://github.com/Diegue83/microserviciofrontend.git'
            }
        }
        stage('Ver Directorio') {
            steps {
                sh 'ls -la'
                sh 'java -version'
                sh 'bash "$MAVEN_HOME/bin/mvn" -version'
            }
        }
        stage('Build & Test') {
            steps {
                sh '''
                    bash "$MAVEN_HOME/bin/mvn" clean package -Dsurefire.useFile=false
                '''
            }
        }
        stage('SonarQube Analysis') {
            steps {
                // Utiliza el servidor de SonarQube configurado en Jenkins con el nombre 'SonnarServer'
                withSonarQubeEnv('SonnarServer') {
                    sh '''
                        bash "$MAVEN_HOME/bin/mvn" \
                        org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar \
                          -Dsonar.projectKey=Ejercicio5Cliente \
                          -Dsonar.projectName="Ejercicio5Cliente" \
                          -Dsonar.host.url=http://host.docker.internal:9000 \
                          -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    '''
                }
            }
        }
        stage('Security Scan (SAST) - Semgrep') {
            steps {
                sh '''
                    mkdir -p reportes

                    docker run --rm \
                      --network bridge \
                      -v "${WORKSPACE}":/src \
                      -w /src \
                      semgrep/semgrep \
                      semgrep scan \
                        --config=auto \
                        --exclude=target \
                        --json \
                        --output=reportes/semgrep.json \
                        .
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'reportes/semgrep.json',
                        allowEmptyArchive: true,
                        fingerprint: true
                }
            }
        }
    }
}
