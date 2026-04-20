# Complete AWS EKS & ECR Setup Guide for Jenkins CI/CD

This guide covers everything needed to set up Docker, Kubernetes, AWS CLI, ECR, and EKS on your Jenkins EC2 server.

---

## Table of Contents
1. [Install Docker on EC2](#1-install-docker-on-ec2)
2. [Add Jenkins User to Docker Group](#2-add-jenkins-user-to-docker-group)
3. [Install AWS CLI](#3-install-aws-cli)
4. [Configure AWS Credentials](#4-configure-aws-credentials)
5. [Install kubectl](#5-install-kubectl)
6. [Install eksctl](#6-install-eksctl)
7. [Create ECR Repository](#7-create-ecr-repository)
8. [Create EKS Cluster](#8-create-eks-cluster)
9. [Configure Jenkins AWS Credentials](#9-configure-jenkins-aws-credentials)
10. [Verify Complete Setup](#10-verify-complete-setup)
11. [Troubleshooting](#11-troubleshooting)

---

## 1. Install Docker on EC2

SSH into your EC2 instance and install Docker:

```bash
# SSH into EC2
ssh -i jenkins-key.pem ubuntu@<your-ec2-public-ip>

# Update package list
sudo apt update

# Install Docker
sudo apt install docker.io -y

# Start Docker service
sudo systemctl start docker

# Enable Docker to start on boot
sudo systemctl enable docker

# Verify Docker installation
docker --version
```

**Expected output:**
```
Docker version 24.x.x, build xxxxx
```

---

## 2. Add Jenkins User to Docker Group

Jenkins needs permission to run Docker commands:

```bash
# Add ubuntu user to docker group (Jenkins runs as ubuntu user)
sudo usermod -aG docker ubuntu

# Apply group changes
newgrp docker

# Fix Docker socket permissions
sudo chmod 666 /var/run/docker.sock

# Or permanently fix it
sudo chown root:docker /var/run/docker.sock

# Restart Jenkins to apply changes
sudo systemctl restart jenkins

# Verify Docker works without sudo
docker ps
```

**Expected output:**
```
CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
```

---

## 3. Install AWS CLI

Install AWS CLI v2 using the official installer:

```bash
# Download AWS CLI installer
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"

# Install unzip if not available
sudo apt install unzip -y

# Unzip the installer
unzip awscliv2.zip

# Run the installer
sudo ./aws/install

# Verify installation
aws --version
```

**Expected output:**
```
aws-cli/2.x.x Python/3.x.x Linux/x.x.x
```

---

## 4. Configure AWS Credentials

Configure AWS credentials for CLI access:

```bash
# Configure AWS credentials
aws configure
```

**You'll be prompted for:**
```
AWS Access Key ID: [Enter your AWS access key]
AWS Secret Access Key: [Enter your AWS secret key]
Default region name: eu-west-2
Default output format: json
```

### How to Get AWS Credentials:

1. **Go to AWS Console** → **IAM** → **Users** → **Your user**
2. **Security credentials** tab
3. **Create access key** → **Command Line Interface (CLI)**
4. **Copy Access Key ID and Secret Access Key**

### Verify Configuration:

```bash
# Test AWS CLI
aws sts get-caller-identity
```

**Expected output:**
```json
{
    "UserId": "AIDAXXXXXXXXXX",
    "Account": "052251888400",
    "Arn": "arn:aws:iam::052251888400:user/your-username"
}
```

---

## 5. Install kubectl

Install kubectl (Kubernetes command-line tool):

```bash
# Download kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"

# Make it executable
chmod +x kubectl

# Move to system path
sudo mv kubectl /usr/local/bin/

# Verify installation
kubectl version --client
```

**Expected output:**
```
Client Version: v1.29.x
```

---

## 6. Install eksctl

Install eksctl (EKS cluster management tool):

```bash
# Download eksctl
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp

# Move to system path
sudo mv /tmp/eksctl /usr/local/bin

# Verify installation
eksctl version
```

**Expected output:**
```
0.x.x
```

---

## 7. Create ECR Repository

Create an Elastic Container Registry (ECR) repository to store Docker images:

```bash
# Create ECR repository in eu-west-2 (London)
aws ecr create-repository --repository-name hello-app --region eu-west-2
```

**Expected output:**
```json
{
    "repository": {
        "repositoryArn": "arn:aws:ecr:eu-west-2:052251888400:repository/hello-app",
        "registryId": "052251888400",
        "repositoryName": "hello-app",
        "repositoryUri": "052251888400.dkr.ecr.eu-west-2.amazonaws.com/hello-app",
        "createdAt": "2026-04-20T12:00:00+00:00"
    }
}
```

### Get Repository URI:

```bash
# Get the repository URI (save this for Jenkins pipeline)
aws ecr describe-repositories --repository-names hello-app --region eu-west-2 --query 'repositories[0].repositoryUri' --output text
```

**Output:**
```
052251888400.dkr.ecr.eu-west-2.amazonaws.com/hello-app
```

**Save this URI** - you'll need it in your Jenkins pipeline!

---

## 8. Create EKS Cluster

Create an Elastic Kubernetes Service (EKS) cluster:

### Option 1: Basic Cluster (Recommended for Testing)

```bash
# Create EKS cluster with 2 nodes (takes 15-20 minutes)
eksctl create cluster \
  --name hello-cluster \
  --region eu-west-2 \
  --nodes 2 \
  --node-type t3.medium \
  --managed
```

### Option 2: Advanced Cluster Configuration

```bash
# Create EKS cluster with custom configuration
eksctl create cluster \
  --name hello-cluster \
  --region eu-west-2 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 3 \
  --managed
```

**Wait for completion message:**
```
[✓]  EKS cluster "hello-cluster" in "eu-west-2" region is ready
```

### Verify Cluster Creation:

```bash
# Configure kubectl to use the new cluster
aws eks update-kubeconfig --region eu-west-2 --name hello-cluster

# Check cluster nodes
kubectl get nodes
```

**Expected output:**
```
NAME                                       STATUS   ROLES    AGE   VERSION
ip-192-168-x-x.eu-west-2.compute.internal  Ready    <none>   5m    v1.29.x
ip-192-168-x-x.eu-west-2.compute.internal  Ready    <none>   5m    v1.29.x
```

### Check Cluster Info:

```bash
# Get cluster information
eksctl get cluster --region eu-west-2

# Get nodegroup information
eksctl get nodegroup --cluster hello-cluster --region eu-west-2
```

---

## 9. Configure Jenkins AWS Credentials

Add AWS credentials to Jenkins for pipeline access:

### Step 1: Go to Jenkins UI

1. **Open Jenkins:** `http://your-ec2-ip:8080`
2. **Manage Jenkins** → **Credentials**
3. **System** → **Global credentials (unrestricted)**
4. **Add Credentials**

### Step 2: Add AWS Credentials

```
Kind: AWS Credentials
ID: aws-credentials
Description: AWS credentials for ECR and EKS
Access Key ID: [Your AWS Access Key]
Secret Access Key: [Your AWS Secret Key]
```

5. **Click OK**

---

## 10. Verify Complete Setup

Run these commands to verify everything is installed correctly:

```bash
# Check all tools
echo "=== Docker ==="
docker --version
docker ps

echo "=== AWS CLI ==="
aws --version
aws sts get-caller-identity

echo "=== kubectl ==="
kubectl version --client
kubectl get nodes

echo "=== eksctl ==="
eksctl version
eksctl get cluster --region eu-west-2

echo "=== Java & Maven ==="
java -version
mvn -version

echo "=== Jenkins ==="
sudo systemctl status jenkins
```

### Expected Summary:

```
✅ Docker: v24.x.x
✅ AWS CLI: v2.x.x
✅ kubectl: v1.29.x
✅ eksctl: v0.x.x
✅ Java: 21.0.10
✅ Maven: 3.9.6
✅ Jenkins: Active (running)
✅ EKS Cluster: hello-cluster (Ready)
✅ ECR Repository: hello-app
```

---

## 11. Troubleshooting

### Issue 1: Docker Permission Denied

**Error:**
```
Got permission denied while trying to connect to the Docker daemon socket
```

**Fix:**
```bash
sudo usermod -aG docker ubuntu
sudo chmod 666 /var/run/docker.sock
sudo systemctl restart jenkins
```

### Issue 2: AWS CLI Not Found

**Error:**
```
Command 'aws' not found
```

**Fix:**
```bash
# Reinstall AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

### Issue 3: kubectl Cannot Connect to Cluster

**Error:**
```
The connection to the server localhost:8080 was refused
```

**Fix:**
```bash
# Reconfigure kubectl
aws eks update-kubeconfig --region eu-west-2 --name hello-cluster

# Verify
kubectl get nodes
```

### Issue 4: EKS Cluster Creation Failed

**Error:**
```
Error: creating CloudFormation stack
```

**Fix:**
```bash
# Delete failed cluster
eksctl delete cluster --name hello-cluster --region eu-west-2

# Recreate cluster
eksctl create cluster --name hello-cluster --region eu-west-2 --nodes 2
```

### Issue 5: ECR Push Access Denied

**Error:**
```
denied: User is not authorized to perform: ecr:PutImage
```

**Fix:**
```bash
# Login to ECR
aws ecr get-login-password --region eu-west-2 | docker login --username AWS --password-stdin 052251888400.dkr.ecr.eu-west-2.amazonaws.com

# Verify IAM permissions
aws iam get-user
```

---

## Cost Breakdown (eu-west-2 Region)

### EKS Cluster Costs:
- **EKS Control Plane:** $0.10/hour (~$73/month)
- **2x t3.medium nodes:** $0.0832/hour each (~$60/month each)
- **Total:** ~$193/month

### ECR Costs:
- **Storage:** $0.10/GB per month
- **Data Transfer:** First 1GB free, then $0.09/GB

### Cost Saving Tips:

```bash
# Delete cluster when not in use
eksctl delete cluster --name hello-cluster --region eu-west-2

# Recreate when needed
eksctl create cluster --name hello-cluster --region eu-west-2 --nodes 2

# Use smaller instance types for testing
--node-type t3.small  # ~$30/month per node
```

---

## Quick Reference Commands

### Docker Commands:
```bash
docker ps                    # List running containers
docker images                # List images
docker build -t myapp .      # Build image
docker push myapp            # Push to registry
```

### AWS CLI Commands:
```bash
aws ecr describe-repositories --region eu-west-2
aws eks list-clusters --region eu-west-2
aws sts get-caller-identity
```

### kubectl Commands:
```bash
kubectl get nodes            # List cluster nodes
kubectl get pods             # List pods
kubectl get svc              # List services
kubectl apply -f file.yaml   # Apply configuration
kubectl rollout status deployment/hello-app
```

### eksctl Commands:
```bash
eksctl get cluster --region eu-west-2
eksctl get nodegroup --cluster hello-cluster --region eu-west-2
eksctl delete cluster --name hello-cluster --region eu-west-2
```

---

## Next Steps

After completing this setup:

1. ✅ Update your Jenkins pipeline with correct ECR registry URL
2. ✅ Commit Dockerfile and deployment.yaml to your repository
3. ✅ Run Jenkins pipeline to deploy to EKS
4. ✅ Get your app URL: `kubectl get svc hello-app-svc`

---

## Resources

- **AWS EKS Documentation:** https://docs.aws.amazon.com/eks/
- **kubectl Documentation:** https://kubernetes.io/docs/reference/kubectl/
- **eksctl Documentation:** https://eksctl.io/
- **Docker Documentation:** https://docs.docker.com/

---

**Setup Complete! Your Jenkins server is now ready for full CI/CD deployment to AWS EKS!** 🚀
