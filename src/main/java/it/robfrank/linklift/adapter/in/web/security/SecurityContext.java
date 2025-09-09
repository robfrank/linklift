package it.robfrank.linklift.adapter.in.web.security;

import io.javalin.http.Context;

/**
 * Utility class for managing security context in web requests.
 * Provides thread-safe access to the current user's security information.
 */
public class SecurityContext {

    private static final String SECURITY_CONTEXT_KEY = "security.context";

    /**
     * Stores the security context in the request context.
     *
     * @param ctx the Javalin context
     * @param securityContext the security context to store
     */
    public static void setSecurityContext(Context ctx, it.robfrank.linklift.application.domain.model.SecurityContext securityContext) {
        ctx.attribute(SECURITY_CONTEXT_KEY, securityContext);
    }

    /**
     * Gets the security context from the request context.
     *
     * @param ctx the Javalin context
     * @return the security context, or anonymous if not set
     */
    public static it.robfrank.linklift.application.domain.model.SecurityContext getSecurityContext(Context ctx) {
        var securityContext = ctx.attribute(SECURITY_CONTEXT_KEY);
        if (securityContext instanceof it.robfrank.linklift.application.domain.model.SecurityContext sc) {
            return sc;
        }
        return it.robfrank.linklift.application.domain.model.SecurityContext.anonymous();
    }

    /**
     * Gets the current user ID from the security context.
     *
     * @param ctx the Javalin context
     * @return the current user ID, or null if not authenticated
     */
    public static String getCurrentUserId(Context ctx) {
        return getSecurityContext(ctx).getCurrentUserId().orElse(null);
    }

    /**
     * Gets the current username from the security context.
     *
     * @param ctx the Javalin context
     * @return the current username, or null if not authenticated
     */
    public static String getCurrentUsername(Context ctx) {
        return getSecurityContext(ctx).getCurrentUsername().orElse(null);
    }

    /**
     * Checks if the current user is authenticated.
     *
     * @param ctx the Javalin context
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated(Context ctx) {
        return getSecurityContext(ctx).isAuthenticated();
    }

    /**
     * Checks if the current user has a specific permission.
     *
     * @param ctx the Javalin context
     * @param permission the permission to check
     * @return true if user has permission, false otherwise
     */
    public static boolean hasPermission(Context ctx, String permission) {
        return getSecurityContext(ctx).hasPermission(permission);
    }
}
