package com.capitall.config;

import com.capitall.model.User;
import com.capitall.model.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MaintenanceInterceptor implements HandlerInterceptor {

    private final MaintenanceModeState maintenanceModeState;

    public MaintenanceInterceptor(MaintenanceModeState maintenanceModeState) {
        this.maintenanceModeState = maintenanceModeState;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (maintenanceModeState.isMaintenanceMode()) {
            String uri = request.getRequestURI();

            if (uri.startsWith("/admin") || uri.startsWith("/css") || uri.startsWith("/js") || 
                uri.startsWith("/images") || uri.equals("/logout") || uri.equals("/maintenance") || uri.equals("/") || uri.equals("/login")) {
                return true;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User) {
                User user = (User) auth.getPrincipal();
                if (user.getRole() == UserRole.ADMIN) {
                    return true;
                }
            }

            response.sendRedirect("/maintenance");
            return false;
        }
        return true;
    }
}
