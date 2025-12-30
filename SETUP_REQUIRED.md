# ⚠️ SETUP REQUIRED BEFORE RUNNING

## Quick Start

1. **Copy environment file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your actual credentials**

3. **Install EnvFile plugin in IntelliJ** (recommended)
   - `File > Settings > Plugins`
   - Search "EnvFile"
   - Install and restart

4. **Configure Run Configuration:**
   - `Run > Edit Configurations`
   - Select Spring Boot app
   - Go to `EnvFile` tab
   - Add your `.env` file
   - Enable EnvFile

5. **Run the application**

## Need Help?

See [SECURITY_SETUP.md](SECURITY_SETUP.md) for detailed instructions.

## ⚠️ IMPORTANT

- **NEVER commit `.env` file to Git!**
- `.env` contains sensitive API keys and secrets
- Always use `.env.example` as template
