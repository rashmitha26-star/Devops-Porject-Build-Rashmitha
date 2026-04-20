# Java App → GitHub → Jenkins Pipeline → ECR → EKS
# Complete Guide with Code Explanations + Interview Questions

---

# PART 1: JENKINS SETUP

---

## Step 1: Install Jenkins on Mac

```bash
brew install jenkins-lts
brew services start jenkins-lts
```

> `brew install jenkins-lts` — downloads and installs the Long Term Support version of Jenkins on your Mac
> `brew services start jenkins-lts` — starts Jenkins as a background service so it runs automatically

Open browser:
```
http://localhost:8080
```

Get unlock password:
```bash
cat ~/.jenkins/secrets/initialAdminPassword
```

> Jenkins generates a one-time password on first install. This command reads it from the file.
> Paste it in the browser → click Continue → Install suggested plugins → Create admin user

---

## Step 2: Install Required Plugins

Go to `Manage Jenkins` → `Plugins` → `Available Plugins`

Install these:

| Plugin | What it does |
|---|---|
| Git | Allows Jenkins to clone code from GitHub |
| Maven Integration | Lets Jenkins run mvn commands |
| Pipeline | Enables Jenkinsfile-based pipelines |
| Docker Pipeline | Allows Jenkins to build and push Docker images |
| AWS Credentials | Securely stores AWS keys inside Jenkins |
| JUnit | Reads test XML files and shows results in UI |
| JaCoCo | Reads code coverage reports and shows % in UI |
| Pipeline Maven Integration | Runs mvn inside pipeline stages |
| Pipeline: AWS Steps | Lets pipeline interact with AWS services |
| JUnit Realtime Test Reporter | Shows test pass/fail live while tests run |

Click `Install without restart`

---

## Step 3: Configure Maven in Jenkins UI

1. Go to `Manage Jenkins` → `Tools`
2. Scroll to `Maven installations` → `Add Maven`
3. Name: `Maven3`, check `Install automatically`, Version: `3.9.6`
4. Click `Save`

> Jenkins needs to know where Maven is so it can run `mvn` commands in pipeline stages.
> Naming it `Maven3` lets you reference it in the pipeline using `tools { maven 'Maven3' }`.

---

## Step 4: Configure JDK in Jenkins UI

1. Go to `Manage Jenkins` → `Tools`
2. Scroll to `JDK installations` → `Add JDK`
3. Name: `JDK17`, check `Install automatically`, Version: `17`
4. Click `Save`

> Java code needs a JDK to compile. Jenkins uses this configuration to set up the correct
> Java version before running any build commands.

---

## Step 5: Add AWS Credentials

1. Go to `Manage Jenkins` → `Credentials` → `(global)` → `Add Credentials`
2. Kind: `AWS Credentials`
3. ID: `aws-credentials`
4. Enter your Access Key ID and Secret Access Key
5. Click `Save`

> Jenkins needs AWS keys to push Docker images to ECR and deploy to EKS.
> Storing them here means they are encrypted and never visible in logs or code.
> The ID `aws-credentials` is what you reference inside the pipeline script.

---

## Step 6: Add GitHub Credentials (if private repo)

1. Go to `Manage Jenkins` → `Credentials` → `(global)` → `Add Credentials`
2. Kind: `Username with password`
3. ID: `github-credentials`
4. Username: your GitHub username
5. Password: your GitHub Personal Access Token
6. Click `Save`

> GitHub no longer accepts plain passwords. You must use a Personal Access Token (PAT).
> Go to GitHub → Settings → Developer Settings → Personal Access Tokens → Generate new token.

---

## Step 7: Create Pipeline Job in Jenkins UI

1. Dashboard → `New Item`
2. Name: `hello-app-pipeline`
3. Select `Pipeline` → click `OK`

### Inside Job Configuration:

**General section:**
- Check `Discard old builds` → Max: `5`

> Keeps only the last 5 builds so Jenkins server disk doesn't fill up over time.

**Build Triggers section:**
- Check `GitHub hook trigger for GITScm polling`

> This tells Jenkins: when GitHub sends a push notification (webhook),
> automatically start a new build. Without this you'd click "Build Now" manually every time.

**Pipeline section:**
- Definition: `Pipeline script`
- Paste the full pipeline code from Step 8 below into the text box

Click `Save`

---

# PART 2: THE PIPELINE SCRIPT — FULL CODE WITH EXPLANATIONS

---

Paste this entire script into the Pipeline Script text box in Jenkins UI:

```
pipeline {
    agent any
```
> `pipeline` — declares this is a Jenkins declarative pipeline
> `agent any` — run this pipeline on any available Jenkins agent/node.
> If you have multiple Jenkins nodes, it picks whichever is free.

---

```
    tools {
        maven 'Maven3'
        jdk   'JDK17'
    }
```
> Tells Jenkins to use the Maven and JDK versions configured in Steps 3 and 4.
> Jenkins automatically sets up the PATH so `mvn` and `java` commands work in all stages.
> Without this, Jenkins wouldn't know which Java or Maven version to use.

---

```
    environment {
        AWS_REGION   = 'us-east-1'
        ECR_REGISTRY = '<your-account-id>.dkr.ecr.us-east-1.amazonaws.com'
        ECR_REPO     = 'hello-app'
        EKS_CLUSTER  = 'hello-cluster'
        IMAGE_TAG    = "${env.BUILD_NUMBER}"
    }
```
> Defines variables available to every stage in the pipeline.
> `AWS_REGION` — the AWS region where your ECR and EKS live.
> `ECR_REGISTRY` — the full URL of your AWS container registry.
> `ECR_REPO` — the name of your ECR repository.
> `EKS_CLUSTER` — the name of your Kubernetes cluster on AWS.
> `IMAGE_TAG` — uses Jenkins build number (1, 2, 3...) as the Docker image tag.
> This means every build creates a uniquely tagged image — you can roll back to any version.

---

```
    stages {
```
> Opens the stages block. All pipeline stages are defined inside here.
> Stages run in order — if one fails, the rest are skipped.

---

```
        stage('Checkout Code') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/<your-username>/hello-app.git'
            }
        }
```
> This stage clones your GitHub repository onto the Jenkins server.
> `branch: 'main'` — pulls the latest code from the main branch.
> `url` — the GitHub repo URL to clone from.
> Every build starts fresh by pulling the latest code first.

---

```
        stage('Maven Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
```
> `sh` — runs a shell command on the Jenkins server.
> `mvn clean` — deletes the previous build output (target/ folder) to start fresh.
> `mvn compile` — compiles all Java source files (.java) into bytecode (.class files).
> If there are any syntax errors in the Java code, this stage fails and stops the pipeline.
> This catches compilation errors before wasting time on tests or packaging.

---

```
        stage('Run Unit Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
```
> `mvn test` — runs all JUnit test classes in your project.
> Maven's Surefire plugin runs the tests and writes results to XML files in target/surefire-reports/.
> `post { always }` — this block runs whether tests pass or fail.
> `junit` step — reads those XML files and publishes results in Jenkins UI.
> After the build you can click "Test Results" in Jenkins to see exactly which tests passed/failed.
> If any test fails, this stage is marked red but the pipeline continues to collect all results.

---

```
        stage('Code Coverage') {
            steps {
                sh 'mvn jacoco:report'
            }
            post {
                always {
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java'
                    )
                }
            }
        }
```
> `mvn jacoco:report` — generates a code coverage report from the test run.
> JaCoCo tracks which lines of code were executed during tests.
> `jacoco.exec` — binary file created during test run containing raw coverage data.
> `classPattern` — tells JaCoCo where the compiled .class files are.
> `sourcePattern` — tells JaCoCo where the Java source files are so it can highlight them.
> Jenkins reads this and shows a coverage % trend graph in the UI.
> Helps teams enforce rules like "coverage must stay above 80%".

---

```
        stage('Package JAR') {
            steps {
                sh 'mvn package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }
```
> `mvn package` — compiles + packages everything into a single runnable JAR file.
> `-DskipTests` — skips running tests again since we already ran them in the previous stage.
> The JAR file is created in the `target/` folder.
> `archiveArtifacts` — saves the JAR file inside Jenkins so you can download it from the UI.
> `fingerprint: true` — Jenkins records an MD5 hash of the JAR to track which build produced it.
> Useful for auditing — you can always trace a deployed JAR back to its exact build.

---

```
        stage('Docker Build') {
            steps {
                sh "docker build -t ${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG} ."
            }
        }
```
> Reads your `Dockerfile` and builds a Docker image containing your JAR file.
> `-t` — tags the image with a name so it can be pushed to ECR.
> The tag format is: `<ecr-registry>/<repo-name>:<build-number>`
> Example: `123456789.dkr.ecr.us-east-1.amazonaws.com/hello-app:42`
> The `.` at the end means use the current directory (where Dockerfile is) as the build context.

---

```
        stage('Push to ECR') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                                  credentialsId: 'aws-credentials']]) {
                    sh """
                        aws ecr get-login-password --region ${AWS_REGION} | \
                        docker login --username AWS --password-stdin ${ECR_REGISTRY}

                        docker push ${ECR_REGISTRY}/${ECR_REPO}:${IMAGE_TAG}
                    """
                }
            }
        }
```
> `withCredentials` — injects AWS keys from Jenkins credentials store into this block only.
> Keys are available as environment variables but never printed in the console logs.
> `aws ecr get-login-password` — calls AWS API to get a temporary Docker authentication token.
> The `|` pipes that token directly into `docker login` — authenticates Docker to your ECR registry.
> `docker push` — uploads the Docker image to ECR so EKS can pull it during deployment.
> Without this step, EKS wouldn't know where to get the image from.

---

```
        stage('Deploy to EKS') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                                  credentialsId: 'aws-credentials']]) {
                    sh """
                        aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER}

                        kubectl apply -f k8s/deployment.yaml

                        kubectl rollout status deployment/hello-app
                    """
                }
            }
        }
```
> `aws eks update-kubeconfig` — downloads the EKS cluster connection config to the Jenkins server.
> This allows `kubectl` to know which cluster to talk to and how to authenticate.
> `kubectl apply -f k8s/deployment.yaml` — sends the deployment config to Kubernetes.
> Kubernetes reads the YAML, pulls the new Docker image from ECR, and starts new pods.
> It does a rolling update — new pods start before old ones stop, so there is zero downtime.
> `kubectl rollout status` — waits and watches until all new pods are running successfully.
> If the deployment fails (e.g. image not found), this command reports the error and fails the stage.

---

```
    post {
        success {
            echo "Build #${env.BUILD_NUMBER} deployed successfully!"
        }
        failure {
            echo "Build #${env.BUILD_NUMBER} failed. Check Console Output."
        }
        always {
            cleanWs()
        }
    }
}
```
> `post` — runs after all stages complete regardless of outcome.
> `success` — runs only if every stage passed. Good place to add Slack/email success notification.
> `failure` — runs only if any stage failed. Good place to alert the team.
> `always` — runs no matter what. `cleanWs()` deletes all files from the Jenkins workspace.
> Cleaning the workspace prevents disk space from filling up on the Jenkins server over builds.

---

# PART 3: PROJECT FILES

---

## Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> `FROM eclipse-temurin:17-jre-alpine` — uses a lightweight Java 17 runtime as the base image.
> Alpine Linux is used because it's tiny (~5MB) compared to full Linux images.
> `WORKDIR /app` — sets the working directory inside the container to /app.
> `COPY target/*.jar app.jar` — copies the JAR built by Maven into the container.
> `EXPOSE 8080` — documents that the app listens on port 8080 (doesn't actually open the port).
> `ENTRYPOINT` — the command that runs when the container starts. Launches the Spring Boot app.

---

## k8s/deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hello-app
spec:
  replicas: 2
  selector:
    matchLabels:
      app: hello-app
  template:
    metadata:
      labels:
        app: hello-app
    spec:
      containers:
        - name: hello-app
          image: <your-ecr-uri>/hello-app:latest
          ports:
            - containerPort: 8080
```

> `apiVersion: apps/v1` — the Kubernetes API version for Deployments.
> `kind: Deployment` — tells Kubernetes this is a Deployment resource.
> `replicas: 2` — run 2 copies of the app at all times for high availability.
> `selector.matchLabels` — Kubernetes uses labels to know which pods belong to this deployment.
> `template` — defines the pod blueprint — every pod created will look like this.
> `image` — the Docker image to pull from ECR. Jenkins replaces `latest` with the build number tag.
> `containerPort: 8080` — the port the app inside the container listens on.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: hello-app-svc
spec:
  type: LoadBalancer
  selector:
    app: hello-app
  ports:
    - port: 80
      targetPort: 8080
```

> `kind: Service` — exposes the pods to network traffic.
> `type: LoadBalancer` — creates an AWS Load Balancer with a public IP address.
> `selector: app: hello-app` — routes traffic to pods with this label.
> `port: 80` — the external port users access (standard HTTP).
> `targetPort: 8080` — forwards traffic to port 8080 inside the container.

---

## pom.xml — JaCoCo Plugin Section

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

> `prepare-agent` — attaches the JaCoCo Java agent to the JVM when tests run.
> The agent silently monitors which lines of code get executed during tests.
> `phase: test` — the report goal runs automatically after the test phase completes.
> `report` goal — generates HTML and XML coverage reports in target/site/jacoco/.
> Without this plugin, `mvn jacoco:report` in the Jenkinsfile would fail.

---

# PART 4: GITHUB WEBHOOK SETUP

---

## Set Up Auto-Trigger on Git Push

1. Go to your GitHub repo → `Settings` → `Webhooks` → `Add webhook`
2. Payload URL: `http://<your-jenkins-url>/github-webhook/`
3. Content type: `application/json`
4. Event: `Just the push event`
5. Click `Add webhook`

> A webhook is an HTTP callback.
> When a developer runs `git push`, GitHub immediately sends a POST request to Jenkins.
> Jenkins receives it and starts a new pipeline build automatically.
> This is what makes the entire flow hands-free after the initial setup.

---

# PART 5: FULL FLOW SUMMARY

```
Developer writes code and runs:
  git add . → git commit → git push
          ↓
GitHub receives the push
          ↓
GitHub Webhook sends notification to Jenkins
          ↓
Jenkins auto-triggers the pipeline
          ↓
Stage 1: Checkout    — clones latest code from GitHub
Stage 2: Maven Build — compiles Java source code
Stage 3: Unit Tests  — runs all JUnit tests, publishes results in UI
Stage 4: Coverage    — generates JaCoCo coverage report in UI
Stage 5: Package JAR — creates runnable JAR, archives in Jenkins
Stage 6: Docker Build— wraps JAR into Docker image
Stage 7: Push ECR    — uploads image to AWS container registry
Stage 8: Deploy EKS  — kubectl applies new image to Kubernetes cluster
          ↓
kubectl rollout confirms all pods are running
          ↓
App is live — get URL with: kubectl get svc hello-app-svc
```

---

# PART 6: INTERVIEW QUESTIONS & ANSWERS

---

## Jenkins

**Q: What is Jenkins?**
A: Jenkins is an open-source CI/CD automation server. It automates building, testing, and deploying applications so developers don't have to do it manually after every code push.

**Q: What is the difference between Pipeline Script and Pipeline Script from SCM?**
A: Pipeline Script — you type the pipeline code directly in Jenkins UI. Jenkins stores it internally. Pipeline Script from SCM — Jenkins reads the Jenkinsfile from your GitHub repo. SCM is better for teams because the pipeline is version-controlled alongside the code.

**Q: What is a Jenkinsfile?**
A: A text file written in Groovy that defines the entire CI/CD pipeline as code. It lives in the root of your repository and Jenkins reads it to know what stages to run.

**Q: What is the difference between declarative and scripted pipeline?**
A: Declarative uses a structured `pipeline {}` block — easier to read, recommended for most use cases. Scripted uses `node {}` blocks with full Groovy — more flexible but complex.

**Q: What does `agent any` mean?**
A: Run the pipeline on any available Jenkins agent/node. You can also use `agent { label 'linux' }` to target a specific machine.

**Q: How do you store secrets in Jenkins?**
A: Using Jenkins Credentials store via `Manage Jenkins → Credentials`. Reference them in the pipeline using `withCredentials`. Secrets are encrypted and never printed in console logs.

**Q: What is the `post` block?**
A: Defines actions after all stages complete. `success` runs if pipeline passed, `failure` if it failed, `always` runs regardless of result.

**Q: How does Jenkins auto-trigger on a git push?**
A: Two things — enable `GitHub hook trigger for GITScm polling` in the job config, and add a webhook in GitHub pointing to `http://<jenkins-url>/github-webhook/`.

**Q: What is `cleanWs()`?**
A: Deletes all files from the Jenkins workspace after the build. Prevents disk space from filling up on the Jenkins server over time.

**Q: What is `archiveArtifacts`?**
A: Saves build output files (like JARs) inside Jenkins so they can be downloaded from the UI. `fingerprint: true` tracks which build produced which artifact.

**Q: What is `BUILD_NUMBER` in Jenkins?**
A: An auto-incrementing number Jenkins assigns to each build (1, 2, 3...). Used here as the Docker image tag so every build produces a uniquely identifiable image.

---

## Maven

**Q: What is Maven?**
A: A build automation tool for Java. It manages dependencies, compiles code, runs tests, and packages the app — all configured in pom.xml.

**Q: What is pom.xml?**
A: Project Object Model — the Maven configuration file. Defines dependencies, plugins, build lifecycle, and packaging type (jar/war).

**Q: What does `mvn clean package` do?**
A: `clean` deletes previous build output. `package` compiles code, runs tests, and creates a JAR in the `target/` folder.

**Q: What is the Maven build lifecycle order?**
A: `validate → compile → test → package → verify → install → deploy`
Each phase automatically runs all previous phases.

**Q: What does `-DskipTests` do?**
A: Skips running unit tests during the build. Used when tests were already run in a previous stage.

**Q: What is JaCoCo?**
A: Java Code Coverage library. Measures what percentage of your code is executed during tests. Generates reports showing covered and uncovered lines.

**Q: What is the Surefire plugin?**
A: A Maven plugin that runs JUnit tests and generates XML result files in `target/surefire-reports/`. Jenkins reads these XML files to display test results in the UI.

**Q: What are Maven dependency scopes?**
A: `compile` — available everywhere (default). `test` — only during testing. `provided` — available at compile time but not packaged. `runtime` — not needed to compile but needed to run.

---

## Docker

**Q: What is a Dockerfile?**
A: A text file with instructions to build a Docker image. `FROM` sets the base image, `COPY` adds files, `ENTRYPOINT` defines what runs when the container starts.

**Q: What is the difference between an image and a container?**
A: An image is a read-only template (like a class). A container is a running instance of that image (like an object). Many containers can run from one image.

**Q: What is AWS ECR?**
A: Elastic Container Registry — AWS's private Docker image registry. Jenkins pushes images here and EKS pulls from here during deployment.

**Q: Why tag Docker images with the build number?**
A: So every build produces a unique image. Allows rolling back to any previous version by deploying an older tag.

**Q: What does `docker login` do in the pipeline?**
A: Authenticates Docker to the ECR registry using a temporary token from AWS. Required before pushing images. The token expires after 12 hours.

---

## Kubernetes / EKS

**Q: What is Kubernetes?**
A: A container orchestration platform. Manages deploying, scaling, and running containers across a cluster of machines automatically.

**Q: What is AWS EKS?**
A: Elastic Kubernetes Service — AWS managed Kubernetes. AWS handles the control plane, you manage worker nodes and deployments.

**Q: What is a Deployment in Kubernetes?**
A: Defines how many replicas of your app to run and which Docker image to use. Kubernetes ensures that number of replicas is always running.

**Q: What is a Service in Kubernetes?**
A: Exposes pods to network traffic. `LoadBalancer` type creates an AWS load balancer with a public IP so users can access the app from the internet.

**Q: What does `kubectl apply` do?**
A: Applies the YAML configuration to the cluster. Creates the resource if it doesn't exist, updates it if it does.

**Q: What is `kubectl rollout status`?**
A: Waits for a deployment to finish and confirms all pods are running the new version. Reports an error if the rollout fails.

**Q: What is a rolling update in Kubernetes?**
A: Kubernetes starts new pods with the new image one by one, and only terminates old pods after new ones are healthy. Zero downtime deployment.

**Q: What does `aws eks update-kubeconfig` do?**
A: Downloads the EKS cluster connection config to the local machine so `kubectl` knows which cluster to connect to and how to authenticate.

**Q: What is a Pod?**
A: The smallest deployable unit in Kubernetes. A pod wraps one or more containers and runs on a worker node in the cluster.

**Q: What is `replicas: 2` in a Deployment?**
A: Tells Kubernetes to always keep 2 copies of the app running. If one pod crashes, Kubernetes automatically starts a new one to maintain the count.

---

## CI/CD General

**Q: What is CI/CD?**
A: CI (Continuous Integration) — automatically build and test on every push. CD (Continuous Delivery/Deployment) — automatically deploy tested code to an environment.

**Q: What is the difference between Continuous Delivery and Continuous Deployment?**
A: Continuous Delivery — code is always ready to deploy but a human approves the release. Continuous Deployment — every passing build deploys automatically with no human approval.

**Q: What is a webhook?**
A: An HTTP callback. GitHub sends a POST request to Jenkins the moment code is pushed. Jenkins receives it and starts the pipeline automatically.

**Q: Why should secrets never be hardcoded in a Jenkinsfile?**
A: Jenkinsfiles are stored in GitHub — hardcoded secrets are visible to anyone with repo access. Always use Jenkins Credentials store and inject at runtime with `withCredentials`.

**Q: What is a rolling deployment?**
A: New pods are brought up gradually while old pods are taken down. Users always have a running version — zero downtime.

**Q: What is the benefit of tagging Docker images with build numbers instead of `latest`?**
A: `latest` always gets overwritten. Build number tags are immutable — you can deploy any specific version and roll back instantly if something breaks in production.

---

## Git Commands Explained

**Q: What is `git config pull.rebase false` and why do we need it?**

A: When you run `git pull`, git needs to know HOW to combine the remote commits with your local commits. There are two strategies — merge or rebase. Git doesn't pick one by default and throws an error asking you to choose.

`git config pull.rebase false` tells git — use merge strategy when pulling. This is the safe and simple choice.

Without this config git throws this error:
```
fatal: Need to specify how to reconcile divergent branches.
```

You only need to run this once. Git saves it in your local config and remembers it forever.

The three options git gives you:
```
git config pull.rebase false   # merge  — safe, keeps full history
git config pull.rebase true    # rebase — cleaner history, rewrites commits
git config pull.ff only        # fast-forward only — strictest, fails if branches diverged
```
For beginners and teams — always use `pull.rebase false`.

---

**Q: What is `--allow-unrelated-histories` in git pull?**

A: Normally git refuses to merge two branches that have no common starting point. This happens when:
- You created a repo on GitHub with a README
- You created a separate local project
- They were never connected — git sees them as completely unrelated

`--allow-unrelated-histories` forces git to merge them anyway.

```bash
git pull origin main --allow-unrelated-histories
```

You only need this flag once — the first time you connect a local project to an existing GitHub repo. After that, both histories are merged and future pulls work normally.

---

**Q: What is `origin` in git commands?**

A: `origin` is just a nickname/alias for your GitHub repo URL. Instead of typing the full URL every time, you give it a short name.

```bash
# without origin
git push https://github.com/rashmitha26-star/Devops-Porject-Build-Rashmitha.git main

# with origin — clean and short
git push origin main
```

You set it up once with:
```bash
git remote add origin https://github.com/your-username/your-repo.git
```

`origin` is the default name everyone uses by convention. You could name it anything but everyone uses `origin`.

---

**Q: What does `git remote -v` do? What does `-v` mean?**

A: `git remote -v` shows all the remotes configured for your repo and their URLs.
`-v` means verbose — show full details, not just the name.

```bash
git remote -v
```

Output:
```
origin  https://github.com/rashmitha26-star/Devops-Porject-Build-Rashmitha.git (fetch)
origin  https://github.com/rashmitha26-star/Devops-Porject-Build-Rashmitha.git (push)
```

Two lines appear — one for fetch (download) and one for push (upload). Both point to the same URL normally.

---

**Q: What is the difference between merge and rebase?**

A: Both combine commits from two branches but in different ways.

Merge — combines branches and creates a new merge commit. Full history is preserved.
```
GitHub:  A --- B
Local:       C --- D
Result:  A --- B --- C --- D --- Merge commit
```

Rebase — replays your local commits on top of remote commits. No merge commit. Cleaner history but rewrites commits.
```
GitHub:  A --- B
Local:       C --- D
Result:  A --- B --- C --- D
```

For teams — merge is safer because it never rewrites history. Rebase is used when you want a clean linear history before merging a feature branch.

---

**Q: What is `git branch -m master main`?**

A: Renames your local branch from `master` to `main`.
`-m` means move/rename.

GitHub now uses `main` as the default branch name. Older git versions create branches as `master` by default. This command aligns your local branch name with GitHub.

---

**Q: What is the full git push flow from local to GitHub?**

```
git init                          — initialise a local git repo
git add .                         — stage all files for commit
git commit -m "message"           — save a snapshot of staged files
git remote add origin <url>       — link your local repo to GitHub
git branch -m master main         — rename branch to main
git config pull.rebase false      — set merge as pull strategy
git pull origin main              — download GitHub commits and merge
  --allow-unrelated-histories     — needed if repo histories are unrelated
git push origin main              — upload your commits to GitHub
```


---

# PART 7: MOVING JENKINS TO AWS EC2 (REAL PRODUCTION SETUP)

---

## Why EC2 and not your local Mac?

| | Your Mac | EC2 |
|---|---|---|
| Always on | No | Yes — 24/7 |
| Public IP | No | Yes |
| GitHub webhook works | No | Yes |
| Whole team can access | No | Yes |
| Builds while you sleep | No | Yes |

Jenkins must be on a machine with a public IP so GitHub can send webhook notifications to it.
EC2 is the standard choice when you are already using AWS (ECR + EKS).

---

## Public Subnet vs Private Subnet — Which to choose?

Always choose PUBLIC subnet for Jenkins.

| | Public Subnet | Private Subnet |
|---|---|---|
| Has public IP | Yes | No |
| GitHub can reach it | Yes | No |
| Jenkins UI accessible | Yes | No |
| Use case | Jenkins, web servers | Databases, internal apps |

Also make sure `Auto-assign public IP` is ENABLED when launching the instance.
This is what gives EC2 a public IP that GitHub can reach.

---

## Step 1: Launch EC2 Instance

1. Go to AWS Console → search `EC2` → click `Launch Instance`
2. Fill in:
   - Name: `jenkins-server`
   - AMI: `Ubuntu Server 22.04 LTS` — 64-bit (x86)
   - Instance type: `t2.medium` (Jenkins needs minimum 2 CPUs, 4GB RAM — t2.micro is too small)
3. Key Pair:
   - Click `Create new key pair`
   - Name: `jenkins-key`
   - Type: `RSA`
   - Format: `.pem`
   - Click `Create key pair` — saves `jenkins-key.pem` to your Mac — keep this safe
4. Network Settings → click `Edit`:
   - VPC: select your VPC
   - Subnet: select a PUBLIC subnet
   - Auto-assign public IP: ENABLE

5. Add Security Group inbound rules:

| Type | Port | Source | Why |
|---|---|---|---|
| SSH | 22 | My IP | SSH from your Mac only |
| Custom TCP | 8080 | Anywhere | Jenkins UI |
| Custom TCP | 50000 | Anywhere | Jenkins agents |

6. Storage: change from 8GB to `20GB` (builds need space)
7. Click `Launch Instance`
8. Wait 2 minutes → Instance State shows `Running`
9. Copy the `Public IPv4 address` — example: `54.123.45.67`

---

## Step 2: SSH Into EC2 From Your Mac

```bash
# go to where the key was downloaded
cd ~/Downloads

# fix key permissions — required by SSH
chmod 400 jenkins-key.pem

# connect to EC2
ssh -i jenkins-key.pem ubuntu@<ec2-public-ip>
```

> `chmod 400` — makes the key file read-only. SSH refuses to connect if the key has open permissions.
> You should see `ubuntu@ip-172-xx-xx-xx:~$` — you are now inside the EC2 server.

---

## Step 3: Install Java on EC2

```bash
sudo apt update
sudo apt install openjdk-17-jdk -y
java -version
```

> Jenkins is a Java application — it needs Java to run.

---

## Step 4: Install Jenkins on EC2

```bash
curl -fsSL https://pkg.jenkins.io/debian/jenkins.io-2023.key | sudo tee \
  /usr/share/keyrings/jenkins-keyring.asc > /dev/null

echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt update
sudo apt install jenkins -y
sudo systemctl start jenkins
sudo systemctl enable jenkins
```

> `systemctl enable jenkins` — makes Jenkins start automatically if EC2 reboots.

Check Jenkins is running:
```bash
sudo systemctl status jenkins
```

---

## Step 5: Install Required Tools on EC2

```bash
# Maven — to run mvn commands
sudo apt install maven -y

# Docker — to build and push images
sudo apt install docker.io -y
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins

# AWS CLI — to talk to ECR and EKS
sudo apt install awscli -y

# kubectl — to deploy to EKS
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

> `usermod -aG docker jenkins` — adds Jenkins user to the docker group so it can run docker commands.
> Without this, `docker build` in the pipeline would fail with permission denied.

---

## Step 6: Open Jenkins UI

Open in your browser:
```
http://<ec2-public-ip>:8080
```

Get the unlock password:
```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

Paste it → Install suggested plugins → Create admin user → Done.

---

## Step 7: Attach IAM Role to EC2 (No hardcoded AWS keys)

In production you never store AWS keys inside Jenkins.
Instead you attach an IAM Role to the EC2 — Jenkins inherits the permissions automatically.

1. Go to AWS Console → IAM → Roles → `Create Role`
2. Trusted entity: `EC2`
3. Attach these policies:
   - `AmazonEC2ContainerRegistryFullAccess`
   - `AmazonEKSClusterPolicy`
   - `AmazonEKSWorkerNodePolicy`
4. Name: `jenkins-ec2-role` → Create
5. Go to EC2 → select `jenkins-server` → Actions → Security → Modify IAM Role → attach `jenkins-ec2-role`

> Now Jenkins on EC2 can push to ECR and deploy to EKS without any AWS keys stored anywhere.
> This is the secure production approach — no keys in code, no keys in Jenkins credentials.

---

## Step 8: Add GitHub Webhook

Your Jenkins now has a real public URL:
```
http://<ec2-public-ip>:8080/github-webhook/
```

1. Go to your GitHub repo → Settings → Webhooks → Add webhook
2. Payload URL: `http://<ec2-public-ip>:8080/github-webhook/`
3. Content type: `application/json`
4. Event: `Just the push event`
5. Click `Add webhook`

You should see a green tick — GitHub can now reach Jenkins directly. No ngrok needed.

---

## Step 9: Create Pipeline Job in Jenkins UI

1. Open `http://<ec2-public-ip>:8080`
2. New Item → `hello-app-pipeline` → Pipeline → OK
3. Build Triggers → check `GitHub hook trigger for GITScm polling`
4. Pipeline → Pipeline script → paste your Jenkinsfile
5. Save

---

## Step 10: Test the Full Flow

On your Mac:
```bash
git add .
git commit -m "trigger jenkins on ec2"
git push origin main
```

Go to `http://<ec2-public-ip>:8080` — build triggers automatically within seconds.

---

## Full Production Flow

```
Developer pushes code on Mac
          ↓
GitHub receives the push
          ↓
GitHub webhook hits EC2 public IP directly
          ↓
Jenkins on EC2 triggers pipeline automatically
          ↓
EC2 uses IAM Role — no keys needed
          ↓
mvn build → docker build → push to ECR
          ↓
kubectl deploy to EKS
          ↓
App live on LoadBalancer URL
```

---

## Local vs EC2 Comparison

| | Local Mac Jenkins | EC2 Jenkins |
|---|---|---|
| Jenkins URL | localhost:8080 | ec2-public-ip:8080 |
| GitHub webhook works | No | Yes |
| ngrok needed | Yes | No |
| AWS keys | Stored in Jenkins | IAM Role — no keys |
| Always running | No | Yes 24/7 |
| Team access | Only you | Whole team |
| Cost | Free | ~$1/day (t2.medium) |
