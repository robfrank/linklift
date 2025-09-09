# Security Configuration

## JWT Secret Configuration

This application uses JWT tokens for authentication. The JWT secret must be properly configured for secure operation.

### Environment Variables

| Variable                   | Description                            | Required                         |
| -------------------------- | -------------------------------------- | -------------------------------- |
| `LINKLIFT_JWT_SECRET`      | JWT signing secret (minimum 256 bits)  | Production: Yes, Development: No |
| `LINKLIFT_JWT_SECRET_FILE` | Path to file containing JWT secret     | Alternative to direct env var    |
| `ENVIRONMENT`              | Environment indicator (dev/local/prod) | No                               |

### Security Requirements

-   JWT secret must be at least 256 bits (32 characters when UTF-8 encoded)
-   Different secrets should be used for different environments
-   Secrets should be randomly generated with high entropy
-   Secrets must never be committed to version control

### Production Deployment

Set the JWT secret environment variable:

```bash
export LINKLIFT_JWT_SECRET="your-randomly-generated-256-bit-secret-here"
```

Or use a secrets file:

```bash
echo "your-randomly-generated-256-bit-secret-here" > /etc/linklift/jwt-secret
export LINKLIFT_JWT_SECRET_FILE="/etc/linklift/jwt-secret"
chmod 600 /etc/linklift/jwt-secret
```

### Generating Secure Secrets

Use one of these methods to generate a secure 256-bit secret:

```bash
# Using OpenSSL
openssl rand -base64 32

# Using /dev/urandom
head -c 32 /dev/urandom | base64

# Using Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

### Development Environment

In development, the application will generate a deterministic but secure secret automatically. You'll see these warning messages:

```
WARN - Using development JWT secret. DO NOT USE IN PRODUCTION!
WARN - Set LINKLIFT_JWT_SECRET environment variable or LINKLIFT_JWT_SECRET_FILE for production deployment
```

This is normal and expected in development. The auto-generated development secret:

-   Is deterministic per installation (same secret for the same development setup)
-   Provides adequate security for development
-   Includes sufficient entropy for testing
-   Will fail to start in production environments without proper configuration

### Security Features

-   **Environment Detection**: Automatically detects development vs production environments
-   **Validation**: Enforces minimum secret length (256 bits)
-   **Multiple Sources**: Supports both direct environment variables and file-based secrets
-   **Secure Fallbacks**: Provides secure development fallbacks while preventing production misuse
-   **Logging**: Provides clear guidance on security configuration issues

### Migration from Hardcoded Secrets

Previous versions used hardcoded JWT secrets in the source code. These have been removed and replaced with secure environment-based configuration. If you're upgrading:

1. Set the `LINKLIFT_JWT_SECRET` environment variable
2. Restart the application
3. Verify no development warnings appear in production logs

### Troubleshooting

| Issue                              | Solution                                                           |
| ---------------------------------- | ------------------------------------------------------------------ |
| "JWT secret not configured" error  | Set `LINKLIFT_JWT_SECRET` environment variable                     |
| "JWT secret is too short" warning  | Use a secret with at least 32 characters                           |
| Development warnings in production | Set `ENVIRONMENT=production` or ensure proper secret configuration |
| File not found error               | Check `LINKLIFT_JWT_SECRET_FILE` path and permissions              |
