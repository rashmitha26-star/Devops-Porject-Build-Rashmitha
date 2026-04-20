# GitHub CI/CD Pipeline - Step-by-Step Implementation Guide

## 🎯 Your Interview Scenario (6 Years Experience)

**You're a Senior DevOps Engineer** who needs to set up a complete CI/CD pipeline for a Spring Boot application that:
- Builds and tests automatically on every push
- Scans for security vulnerabilities
- Builds Docker images and pushes to AWS ECR
- Deploys to Kubernetes (EKS) across multiple environments
- Sends notifications to Slack

---

## 📋 Step 1: Understand the Pipeline Flow

```
Code Push → Trigger Workflow → Checkout Code → Setup Java → 
Run Tests → Build JAR → Build Docker Image → Push to ECR → 
Deploy to K8s → Health Check → Notify Team
```

**Why this flow?**
- Early feedback (tests run first)
- Security scanning before deployment
- Automated deployment reduces errors
- Health checks ensure stability

---

## 📋 Step 2: Create GitHub Workflow Directory

**What to do:**
```bash
mkdir -p .github/workflows
```

**Why?**
- GitHub Actions looks for workflows in `.github/workflows/` directory
- YAML files here define your CI/CD pipelines

**Interview Tip:** "I organize workflows by purpose - ci.yml for testing, deploy.yml for deployments"

---

## 📋 Step 3: Create Your First Workflow File

**What to do:**
```bash
touch .github/workflows/ci-cd-pipeline.yml
```

**Structure to follow:**
```yaml
name: CI/CD Pipeline
on: [triggers]
jobs:
  job-name:
    runs-on: ubuntu-latest
    steps:
      - name: Step description
        uses: action-name
```

**Key Components:**
- `name`: Pipeline name (shows in GitHub UI)
- `on`: When to trigger (push, pull_request, schedule)
- `jobs`: What to execute
- `steps`: Individual tasks

---

## 📋 Step 4: Define Workflow Triggers

**What to add:**
```yaml
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:  # Manual trigger
```

**Why each trigger?**
- `push`: Auto-deploy on code merge
- `pull_request`: Test before merging
- `workflow_dispatch`: Manual deployments for production

**Interview Answer:** "I use branch-based triggers to ensure only tested code reaches production"

---

## 📋 Step 5: Setup Build Job

**What to configure:**
```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build with Maven
        run: mvn clean package -DskipTests
      
      - name: Run Tests
        run: mvn test
```

**Why this order?**
- Checkout gets your code
- Setup Java prepares environment
- Build creates JAR file
- Tests ensure quality

---

## 📋 Step 6: Add Docker Build Job

**What to configure:**
```yaml
  docker:
    needs: build  # Runs after build succeeds
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Build Docker Image
        run: docker build -t myapp:${{ github.sha }} .
      
      - name: Login to AWS ECR
        uses: aws-actions/amazon-ecr-login@v1
      
      - name: Push to ECR
        run: |
          docker tag myapp:${{ github.sha }} $ECR_REGISTRY/myapp:${{ github.sha }}
          docker push $ECR_REGISTRY/myapp:${{ github.sha }}
```

**Key Concepts:**
- `needs: build` - Creates dependency chain
- `${{ github.sha }}` - Git commit hash for versioning
- ECR login requires AWS credentials

---

## 📋 Step 7: Setup GitHub Secrets

**What to do in GitHub UI:**
1. Go to your repository
2. Settings → Secrets and variables → Actions
3. Click "New repository secret"
4. Add these secrets:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AWS_REGION`
   - `ECR_REGISTRY`
   - `EKS_CLUSTER_NAME`
   - `SLACK_WEBHOOK_URL` (optional)

**Why secrets?**
- Never commit credentials to code
- Encrypted at rest
- Masked in logs

**Interview Answer:** "I use GitHub Secrets for credentials and AWS IAM roles with OIDC for better security"

---

## 📋 Step 8: Add Deployment Job

**What to configure:**
```yaml
  deploy:
    needs: docker
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ secrets.AWS_REGION }}
      
      - name: Update kubeconfig
        run: aws eks update-kubeconfig --name ${{ secrets.EKS_CLUSTER_NAME }}
      
      - name: Deploy to Kubernetes
        run: |
          kubectl set image deployment/myapp myapp=${{ secrets.ECR_REGISTRY }}/myapp:${{ github.sha }}
          kubectl rollout status deployment/myapp
```

**Why this approach?**
- AWS credentials configured securely
- kubectl connects to EKS cluster
- Image tag uses commit hash for traceability
- Rollout status waits for successful deployment

---

## 📋 Step 9: Add Environment-Specific Deployments

**What to configure:**
```yaml
  deploy-dev:
    needs: docker
    runs-on: ubuntu-latest
    environment: development
    steps:
      # deployment steps for dev
  
  deploy-staging:
    needs: deploy-dev
    runs-on: ubuntu-latest
    environment: staging
    steps:
      # deployment steps for staging
  
  deploy-prod:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment: production
    steps:
      # deployment steps for production
```

**Setup Environments in GitHub:**
1. Settings → Environments → New environment
2. Add protection rules:
   - Required reviewers for production
   - Wait timer for staging
   - Deployment branches (only main)

**Interview Answer:** "I use GitHub Environments with approval gates to ensure production deployments are reviewed"

---

## 📋 Step 10: Add Security Scanning

**What to add:**
```yaml
  security-scan:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          format: 'sarif'
          output: 'trivy-results.sarif'
      
      - name: Upload results to GitHub Security
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'
```

**Why security scanning?**
- Catches vulnerabilities early
- Compliance requirements
- Integrates with GitHub Security tab

---

## 📋 Step 11: Add Notifications

**What to add:**
```yaml
  notify:
    needs: [deploy-prod]
    runs-on: ubuntu-latest
    if: always()
    steps:
      - name: Send Slack notification
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          webhook_url: ${{ secrets.SLACK_WEBHOOK_URL }}
          text: 'Deployment to production: ${{ job.status }}'
```

**Why notifications?**
- Team awareness
- Quick response to failures
- Audit trail

---

## 📋 Step 12: Test Your Pipeline

**What to do:**
1. Commit your workflow file:
   ```bash
   git add .github/workflows/ci-cd-pipeline.yml
   git commit -m "Add CI/CD pipeline"
   git push origin main
   ```

2. Go to GitHub → Actions tab
3. Watch your workflow run
4. Check each job's logs

**Debugging tips:**
- Click on failed jobs to see logs
- Use `echo` statements for debugging
- Test locally with `act` tool

---

## 📋 Step 13: Add Caching for Speed

**What to add:**
```yaml
- name: Cache Maven dependencies
  uses: actions/cache@v3
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
    restore-keys: |
      ${{ runner.os }}-maven-
```

**Why caching?**
- Speeds up builds (5-10 minutes → 2-3 minutes)
- Reduces bandwidth
- Saves GitHub Actions minutes

**Interview Answer:** "I implement caching for dependencies and Docker layers to reduce build time by 60%"

---

## 📋 Step 14: Add Rollback Capability

**What to create:**
```yaml
name: Rollback Deployment

on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Environment to rollback'
        required: true
        type: choice
        options:
          - development
          - staging
          - production
      version:
        description: 'Version/tag to rollback to'
        required: true

jobs:
  rollback:
    runs-on: ubuntu-latest
    steps:
      - name: Rollback deployment
        run: |
          kubectl rollout undo deployment/myapp
```

**Why manual rollback?**
- Quick recovery from issues
- Controlled by senior engineers
- Audit trail of who rolled back

---

## 🎤 Interview Preparation - What to Say

### When Asked: "Walk me through your CI/CD pipeline"

**Your Answer:**
"I designed a multi-stage GitHub Actions pipeline that starts with automated testing on every pull request. Once tests pass and code is merged, it builds a Docker image tagged with the git commit SHA for traceability. 

The image is scanned for vulnerabilities using Trivy before being pushed to AWS ECR. Deployment happens in stages - first to development automatically, then staging after a 5-minute wait, and finally to production which requires manual approval from two senior engineers.

Each deployment uses kubectl to update the Kubernetes deployment with the new image tag, and we monitor the rollout status. If health checks fail, the pipeline automatically rolls back. The entire process takes about 12 minutes from commit to production, and we send Slack notifications at each stage."

### When Asked: "How do you handle secrets?"

**Your Answer:**
"I use GitHub Secrets for storing sensitive credentials like AWS access keys and API tokens. These are encrypted at rest and masked in logs. For AWS access, I'm moving towards OIDC-based authentication which eliminates the need for long-lived credentials.

I also use environment-specific secrets - development uses different credentials than production. Secrets are never hardcoded in code or Dockerfiles, and we regularly rotate them using AWS Secrets Manager."

### When Asked: "What if deployment fails?"

**Your Answer:**
"I've implemented multiple safety nets. First, the pipeline includes comprehensive tests that must pass before deployment. Second, we use Kubernetes health checks that verify the application is responding correctly.

If a deployment fails health checks, kubectl rollout status will fail, and the pipeline triggers an automatic rollback to the previous stable version. We also have a manual rollback workflow that can be triggered via workflow_dispatch for emergency situations.

All deployments are tagged with git commit SHAs, so we can quickly identify and rollback to any previous version."

---

## 📊 Metrics to Track (Interview Gold)

**Prepare these numbers:**
- Deployment frequency: "20+ per day"
- Lead time: "From commit to production in 12 minutes"
- MTTR: "Mean time to recovery under 5 minutes with automated rollback"
- Change failure rate: "Less than 5% due to automated testing"
- Pipeline success rate: "99.2%"

---

## ✅ Final Checklist

Before your interview, ensure you can:
- [ ] Explain each stage of the pipeline
- [ ] Draw the pipeline architecture on a whiteboard
- [ ] Discuss why you chose GitHub Actions over Jenkins/GitLab
- [ ] Explain your branching strategy (GitFlow, trunk-based)
- [ ] Describe your testing strategy (unit, integration, e2e)
- [ ] Discuss security measures (scanning, secrets, IAM)
- [ ] Explain your monitoring and alerting setup
- [ ] Talk about cost optimization (caching, self-hosted runners)
- [ ] Describe a time you debugged a pipeline failure
- [ ] Explain how you handle database migrations

---

## 🚀 Practice Exercise

**Do this before your interview:**
1. Create the complete pipeline for your Spring Boot app
2. Trigger it and watch it run
3. Intentionally break something and fix it
4. Time how long each stage takes
5. Calculate your DORA metrics
6. Screenshot the successful pipeline run

**Why?** You'll speak confidently about real experience, not theory.

---

## 💡 Common Pitfalls to Avoid

1. **Don't hardcode secrets** - Always use GitHub Secrets
2. **Don't skip tests** - They catch issues early
3. **Don't deploy directly to prod** - Use staging first
4. **Don't ignore failed jobs** - Fix them immediately
5. **Don't forget rollback plans** - Always have an escape hatch
6. **Don't over-complicate** - Start simple, add complexity as needed

---

## 📚 Study These Topics

- **YAML syntax** - You'll write lots of it
- **Docker multi-stage builds** - For optimized images
- **Kubernetes deployments** - Rolling updates, health checks
- **Git workflows** - Feature branches, pull requests
- **AWS IAM** - Roles, policies, least privilege
- **Monitoring** - Prometheus, Grafana, CloudWatch

---

## 🎯 Next Steps

1. **Create `.github/workflows/` directory** in your project
2. **Start with a simple CI workflow** (just build and test)
3. **Add Docker build** once CI works
4. **Add deployment** after Docker works
5. **Add environments and approvals** for production safety
6. **Add notifications** for team awareness
7. **Document everything** - You'll reference it in interviews

**Remember:** Interviewers want to see you can explain WHY you made each decision, not just HOW you implemented it.

Good luck! 🚀
