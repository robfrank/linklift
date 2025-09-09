package it.robfrank.linklift.adapter.in.web.security;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import it.robfrank.linklift.application.domain.service.AuthorizationService;
import java.util.regex.Pattern;

/**
 * Javalin handler for JWT authentication.
 * Extracts JWT tokens from requests and creates security context.
 */
public class JwtAuthenticationHandler implements Handler {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern BEARER_PATTERN = Pattern.compile("^Bearer\\s+(.+)$");

    private final AuthorizationService authorizationService;

    public JwtAuthenticationHandler(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        String token = extractTokenFromRequest(ctx);
        String ipAddress = getClientIpAddress(ctx);
        String userAgent = ctx.header("User-Agent");

        // Create security context from token
        var securityContext = authorizationService.createSecurityContext(token, ipAddress, userAgent);

        // Store in request context
        SecurityContext.setSecurityContext(ctx, securityContext);
    }

    /**
     * Extracts JWT token from the Authorization header.
     *
     * @param ctx the Javalin context
     * @return the JWT token, or null if not present
     */
    private String extractTokenFromRequest(Context ctx) {
        String authHeader = ctx.header(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        var matcher = BEARER_PATTERN.matcher(authHeader);
        if (matcher.matches()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Gets the client IP address from the request.
     * Handles X-Forwarded-For header for proxied requests.
     *
     * @param ctx the Javalin context
     * @return the client IP address
     */
    private String getClientIpAddress(Context ctx) {
        // Check X-Forwarded-For header (for reverse proxy/load balancer)
        String xForwardedFor = ctx.header("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        // Check X-Real-IP header (for nginx proxy)
        String xRealIp = ctx.header("X-Real-IP");
        if (xRealIp != null && !xRealIp.trim().isEmpty()) {
            return xRealIp.trim();
        }

        // Fall back to direct IP
        return ctx.ip();
    }
}
