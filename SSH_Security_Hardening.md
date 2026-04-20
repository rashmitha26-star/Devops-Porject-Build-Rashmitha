# SSH Security Hardening - Disable Root Login

## Task Overview
**Objective**: Disable direct SSH root login on all app servers in Stratos Datacenter following security audit recommendations.

## Why Disable Root SSH Login?

### Security Risks of Direct Root Access
1. **No Accountability**: Direct root login doesn't track which user performed actions
2. **Privilege Escalation**: Immediate access to highest privileges without audit trail
3. **Brute Force Target**: Root is a known username, making it a prime target for attacks
4. **No Multi-Factor Authentication**: Often bypasses additional security layers
5. **Compliance Issues**: Many security frameworks require disabling direct root access

### Best Practices Alternative
- Use regular user accounts with sudo privileges
- Implement proper logging and accountability
- Enable multi-factor authentication for privileged access

## Implementation Steps

### Step 1: Connect to Each App Server
```bash
# Connect to app server (as current user with sudo privileges)
ssh username@app-server-ip

# Or if you currently have root access
ssh root@app-server-ip
```

### Step 2: Backup SSH Configuration
```bash
# Always backup before making changes
sudo cp /etc/ssh/sshd_config /etc/ssh/sshd_config.backup.$(date +%Y%m%d)

# Verify backup was created
ls -la /etc/ssh/sshd_config.backup.*
```

### Step 3: Edit SSH Configuration
```bash
# Edit the SSH daemon configuration file
sudo vi /etc/ssh/sshd_config

# Or use nano if preferred
sudo nano /etc/ssh/sshd_config
```

### Step 4: Modify Root Login Setting
Find and modify the following line in `/etc/ssh/sshd_config`:

```bash
# Change from:
#PermitRootLogin yes
# Or
PermitRootLogin yes

# To:
PermitRootLogin no
```

**Important Configuration Options:**
- `PermitRootLogin no` - Completely disables root login
- `PermitRootLogin without-password` - Allows key-based root login only
- `PermitRootLogin forced-commands-only` - Allows root login only for specific commands

### Step 5: Validate Configuration
```bash
# Test SSH configuration syntax
sudo sshd -t

# Check if the configuration is valid
echo $?  # Should return 0 if successful
```

### Step 6: Restart SSH Service
```bash
# On systemd systems (CentOS 7+, Ubuntu 16+)
sudo systemctl restart sshd
sudo systemctl status sshd

# On older systems
sudo service ssh restart
sudo service ssh status

# Verify service is running
sudo systemctl is-active sshd
```

### Step 7: Verify Changes
```bash
# Check current SSH configuration
sudo grep "PermitRootLogin" /etc/ssh/sshd_config

# Test from another terminal (DON'T CLOSE CURRENT SESSION)
ssh root@localhost
# Should be denied

# Check SSH logs
sudo tail -f /var/log/auth.log  # Ubuntu/Debian
sudo tail -f /var/log/secure    # CentOS/RHEL
```

## Critical Safety Measures

### Before Making Changes
1. **Ensure Alternative Access**: Have a non-root user with sudo privileges
2. **Keep Current Session Open**: Don't close your current SSH session until verified
3. **Test Access**: Verify you can still access the server through alternative means

### Create Sudo User (if needed)
```bash
# Create new user
sudo useradd -m -s /bin/bash adminuser

# Set password
sudo passwd adminuser

# Add to sudo group
sudo usermod -aG sudo adminuser  # Ubuntu/Debian
sudo usermod -aG wheel adminuser # CentOS/RHEL

# Test sudo access
su - adminuser
sudo whoami  # Should return 'root'
```

## Verification Commands

### Check SSH Configuration
```bash
# View current SSH settings
sudo sshd -T | grep permitrootlogin

# Check SSH service status
sudo systemctl status sshd

# View SSH configuration file
sudo grep -E "^PermitRootLogin|^#PermitRootLogin" /etc/ssh/sshd_config
```

### Test Access
```bash
# From another server or terminal
ssh root@target-server-ip
# Should receive: Permission denied

# Check logs for denied attempts
sudo grep "Failed password for root" /var/log/auth.log
```

## Troubleshooting

### Common Issues
1. **Locked Out**: If you lose access, use console access or recovery mode
2. **Service Won't Start**: Check configuration syntax with `sudo sshd -t`
3. **Permission Denied**: Ensure user has proper sudo privileges

### Recovery Steps
```bash
# If SSH service fails to start
sudo sshd -t  # Check for syntax errors

# Restore from backup
sudo cp /etc/ssh/sshd_config.backup.YYYYMMDD /etc/ssh/sshd_config
sudo systemctl restart sshd
```

## Compliance and Auditing

### Log Monitoring
```bash
# Monitor SSH attempts
sudo tail -f /var/log/auth.log | grep ssh

# Check for root login attempts
sudo grep "Failed password for root" /var/log/auth.log

# Monitor successful logins
sudo grep "Accepted" /var/log/auth.log
```

### Documentation Requirements
- Record all servers modified
- Document the change in change management system
- Update security compliance documentation
- Notify relevant teams about the change

## Interview Questions & Answers

### Q1: Why is disabling root SSH login important?
**Answer**: It enhances security by eliminating a high-privilege attack vector, ensures accountability through user-specific logins, and follows the principle of least privilege by requiring users to authenticate first, then escalate privileges as needed.

### Q2: What are the risks of making this change?
**Answer**: The main risk is losing administrative access if not properly planned. Mitigation includes ensuring alternative access methods (console, non-root sudo users) and keeping current sessions open during testing.

### Q3: How would you verify the change was successful?
**Answer**: Test SSH root login from another terminal (should be denied), check SSH logs for denied attempts, verify SSH configuration with `sshd -T`, and ensure alternative access methods work.

### Q4: What if you need root access after this change?
**Answer**: Use a regular user account with sudo privileges, authenticate as that user, then use `sudo` for administrative tasks. This provides better auditing and security.

## Next Steps for Daily Practice

1. **Set up lab environment** with multiple VMs
2. **Practice on different Linux distributions** (Ubuntu, CentOS, RHEL)
3. **Implement additional SSH hardening** (key-based auth, fail2ban)
4. **Learn related security concepts** (PAM, SELinux, firewall rules)
5. **Practice incident response** scenarios