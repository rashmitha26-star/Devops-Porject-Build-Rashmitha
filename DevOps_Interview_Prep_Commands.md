# DevOps Interview Preparation - Essential Commands

## SSH Security & System Administration

### SSH Configuration Management
```bash
# View SSH configuration
sudo cat /etc/ssh/sshd_config
sudo sshd -T | grep permitrootlogin

# Test SSH configuration
sudo sshd -t

# SSH service management
sudo systemctl status sshd
sudo systemctl restart sshd
sudo systemctl reload sshd

# SSH connection testing
ssh -v username@hostname  # Verbose output
ssh -T git@github.com     # Test git SSH keys
```

### User Management
```bash
# Create user with home directory
sudo useradd -m -s /bin/bash username

# Add user to sudo group
sudo usermod -aG sudo username     # Ubuntu/Debian
sudo usermod -aG wheel username    # CentOS/RHEL

# Check user groups
groups username
id username

# Switch user
su - username
sudo -u username command

# Lock/unlock user account
sudo usermod -L username  # Lock
sudo usermod -U username  # Unlock
```

### File Permissions & Security
```bash
# Change file permissions
chmod 600 ~/.ssh/authorized_keys
chmod 700 ~/.ssh/
chmod 644 /etc/ssh/sshd_config

# Change ownership
chown user:group filename
chown -R user:group directory/

# View file permissions
ls -la filename
stat filename

# Find files with specific permissions
find /path -perm 777
find /path -type f -perm /u+s  # Find SUID files
```

### System Monitoring & Logs
```bash
# View system logs
sudo journalctl -u sshd
sudo tail -f /var/log/auth.log
sudo tail -f /var/log/secure

# Check system status
systemctl status
systemctl list-failed
systemctl list-units --type=service

# Process monitoring
ps aux | grep sshd
netstat -tlnp | grep :22
ss -tlnp | grep :22

# System resource usage
top
htop
free -h
df -h
```

## Network & Firewall Commands

### Firewall Management
```bash
# UFW (Ubuntu)
sudo ufw status
sudo ufw enable
sudo ufw allow ssh
sudo ufw allow 22/tcp
sudo ufw deny from 192.168.1.100

# Firewalld (CentOS/RHEL)
sudo firewall-cmd --state
sudo firewall-cmd --list-all
sudo firewall-cmd --add-service=ssh --permanent
sudo firewall-cmd --reload

# iptables
sudo iptables -L
sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT
```

### Network Diagnostics
```bash
# Network connectivity
ping -c 4 hostname
telnet hostname port
nc -zv hostname port

# DNS resolution
nslookup hostname
dig hostname
host hostname

# Network interfaces
ip addr show
ifconfig
ip route show
```

## Package Management

### APT (Ubuntu/Debian)
```bash
sudo apt update
sudo apt upgrade
sudo apt install package-name
sudo apt remove package-name
sudo apt search package-name
apt list --installed
```

### YUM/DNF (CentOS/RHEL)
```bash
sudo yum update
sudo yum install package-name
sudo yum remove package-name
sudo yum search package-name
yum list installed

# DNF (newer systems)
sudo dnf update
sudo dnf install package-name
```

## File Operations & Text Processing

### File Management
```bash
# Copy with preservation
cp -p source destination
cp -r source/ destination/

# Move/rename
mv oldname newname

# Create directories
mkdir -p path/to/directory

# Find files
find /path -name "filename"
find /path -type f -mtime -7  # Modified in last 7 days
locate filename
```

### Text Processing
```bash
# Search in files
grep -r "pattern" /path/
grep -i "pattern" file  # Case insensitive
grep -v "pattern" file  # Invert match

# Text manipulation
sed 's/old/new/g' file
awk '{print $1}' file
cut -d: -f1 /etc/passwd

# File comparison
diff file1 file2
vimdiff file1 file2
```

## Archive & Compression

```bash
# Tar operations
tar -czf archive.tar.gz directory/
tar -xzf archive.tar.gz
tar -tzf archive.tar.gz  # List contents

# Zip operations
zip -r archive.zip directory/
unzip archive.zip
unzip -l archive.zip  # List contents
```

## System Information

```bash
# System details
uname -a
lsb_release -a
cat /etc/os-release
hostnamectl

# Hardware information
lscpu
lsmem
lsblk
lsusb
lspci

# Disk usage
du -sh directory/
df -h
fdisk -l
```

## Process Management

```bash
# Process control
ps aux
ps -ef
pgrep process-name
pkill process-name
killall process-name

# Job control
jobs
bg %1
fg %1
nohup command &
screen -S session-name
tmux new-session -s session-name
```

## Cron & Scheduling

```bash
# Crontab management
crontab -l        # List cron jobs
crontab -e        # Edit cron jobs
crontab -r        # Remove all cron jobs

# System cron
sudo cat /etc/crontab
ls /etc/cron.d/
ls /etc/cron.daily/

# At command
at now + 5 minutes
atq               # List scheduled jobs
atrm job-number   # Remove scheduled job
```

## Git Commands (DevOps Essential)

```bash
# Basic operations
git status
git add .
git commit -m "message"
git push origin branch-name
git pull origin branch-name

# Branch management
git branch
git checkout -b new-branch
git merge branch-name
git branch -d branch-name

# Remote management
git remote -v
git remote add origin url
git fetch origin
```

## Docker Commands (Container Management)

```bash
# Container operations
docker ps
docker ps -a
docker run -d --name container-name image
docker stop container-name
docker start container-name
docker rm container-name

# Image operations
docker images
docker pull image-name
docker build -t image-name .
docker rmi image-name

# Logs and debugging
docker logs container-name
docker exec -it container-name /bin/bash
```

## Kubernetes Commands

```bash
# Cluster information
kubectl cluster-info
kubectl get nodes
kubectl get pods
kubectl get services

# Deployment management
kubectl apply -f deployment.yaml
kubectl delete -f deployment.yaml
kubectl scale deployment deployment-name --replicas=3

# Debugging
kubectl describe pod pod-name
kubectl logs pod-name
kubectl exec -it pod-name -- /bin/bash
```

## Performance & Troubleshooting

```bash
# System performance
iostat
vmstat
sar
dstat

# Memory analysis
free -h
cat /proc/meminfo
pmap process-id

# Disk I/O
iotop
lsof | grep deleted
fuser -v /path/to/file

# Network performance
iftop
nethogs
tcpdump -i interface
```

## Security Commands

```bash
# File integrity
md5sum file
sha256sum file
find /path -type f -exec md5sum {} \;

# Security scanning
sudo lynis audit system
sudo chkrootkit
sudo rkhunter --check

# Access control
sudo -l          # List sudo privileges
last            # Last logins
w               # Current users
who             # Logged in users
```

## Daily Practice Routine

### Morning (15 minutes)
1. Check system status on practice servers
2. Review logs for any issues
3. Practice 5 random commands from this list

### Evening (30 minutes)
1. Complete one KodeKloud scenario
2. Document new commands learned
3. Practice troubleshooting scenarios

### Weekly Goals
- Master one new service (SSH, Apache, Nginx, etc.)
- Complete security hardening tasks
- Practice incident response scenarios
- Review and update documentation