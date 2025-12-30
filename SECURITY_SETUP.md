# Security Setup Guide

## 🔐 Protecting Sensitive Information

This project uses environment variables to protect sensitive information like API keys, database passwords, and secrets.

## Setup Steps

### 1. Create .env file

```bash
# Copy the example file
cp .env.example .env
```

### 2. Fill in your actual values in .env

Open `.env` and replace all placeholder values with your actual credentials:

```properties
# Database
DB_USERNAME=sa
DB_PASSWORD=your_actual_password

# JWT Secret (Generate a strong one!)
JWT_SECRET=your_very_long_random_secret_here

# Google OAuth2
GOOGLE_CLIENT_ID=your-actual-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-actual-client-secret

# OpenAI
OPENAI_API_KEY=sk-proj-your-actual-api-key

# VNPay
VNPAY_TMN_CODE=your-actual-tmn-code
VNPAY_HASH_SECRET=your-actual-hash-secret
```

### 3. Generate Strong JWT Secret

Use one of these methods:

**Option 1: Using OpenSSL (Recommended)**
```bash
openssl rand -base64 32
```

**Option 2: Using Node.js**
```bash
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

**Option 3: Using Python**
```bash
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

### 4. Configure IntelliJ IDEA

#### Method 1: Using EnvFile Plugin (Recommended)

1. Install **EnvFile** plugin:
   - Go to `File > Settings > Plugins`
   - Search for "EnvFile"
   - Install and restart IntelliJ

2. Configure Run Configuration:
   - Go to `Run > Edit Configurations`
   - Select your Spring Boot application
   - Go to `EnvFile` tab
   - Click `+` and select your `.env` file
   - Check "Enable EnvFile"
   - Apply and OK

#### Method 2: Manual Environment Variables

1. Go to `Run > Edit Configurations`
2. Select your Spring Boot application
3. In "Environment variables" field, click the folder icon
4. Add each variable manually:
   ```
   JWT_SECRET=your_secret_here
   GOOGLE_CLIENT_ID=your_client_id
   GOOGLE_CLIENT_SECRET=your_secret
   OPENAI_API_KEY=your_key
   VNPAY_TMN_CODE=your_code
   VNPAY_HASH_SECRET=your_secret
   ```

### 5. For Production Deployment

#### On Linux/Mac Server:
```bash
# Add to ~/.bashrc or ~/.zshrc
export JWT_SECRET="your_secret"
export GOOGLE_CLIENT_ID="your_id"
export GOOGLE_CLIENT_SECRET="your_secret"
export OPENAI_API_KEY="your_key"
export VNPAY_TMN_CODE="your_code"
export VNPAY_HASH_SECRET="your_secret"

# Reload
source ~/.bashrc
```

#### On Windows Server:
```cmd
# Set permanently
setx JWT_SECRET "your_secret"
setx GOOGLE_CLIENT_ID "your_id"
setx GOOGLE_CLIENT_SECRET "your_secret"
setx OPENAI_API_KEY "your_key"
setx VNPAY_TMN_CODE "your_code"
setx VNPAY_HASH_SECRET "your_secret"
```

#### Using Docker:
```yaml
# docker-compose.yml
services:
  app:
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - VNPAY_TMN_CODE=${VNPAY_TMN_CODE}
      - VNPAY_HASH_SECRET=${VNPAY_HASH_SECRET}
```

## ⚠️ Important Security Notes

1. **NEVER commit .env file to Git**
   - `.env` is already in `.gitignore`
   - Always double-check before pushing

2. **Rotate secrets regularly**
   - Change JWT secret every 3-6 months
   - Rotate API keys if compromised

3. **Use different secrets for different environments**
   - Development: `.env`
   - Staging: `.env.staging`
   - Production: Server environment variables

4. **Check for exposed secrets**
   ```bash
   # Search for potential secrets in Git history
   git log -p | grep -i "api.key\|secret\|password"
   ```

5. **If you accidentally committed secrets:**
   ```bash
   # Remove from Git history (DANGEROUS - coordinate with team!)
   git filter-branch --force --index-filter \
     "git rm --cached --ignore-unmatch .env" \
     --prune-empty --tag-name-filter cat -- --all
   
   # Force push (WARNING: This rewrites history!)
   git push origin --force --all
   
   # IMPORTANT: Rotate all exposed secrets immediately!
   ```

## 🔍 Verify Setup

Run this command to check if environment variables are loaded:

```bash
# In IntelliJ, add this to your main method temporarily:
System.out.println("JWT_SECRET loaded: " + (System.getenv("JWT_SECRET") != null));
System.out.println("OPENAI_API_KEY loaded: " + (System.getenv("OPENAI_API_KEY") != null));
```

## 📚 Additional Resources

- [Spring Boot External Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [12 Factor App - Config](https://12factor.net/config)
- [OWASP Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
