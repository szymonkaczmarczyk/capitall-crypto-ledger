package com.capitall.security;

import com.capitall.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TwoFactorAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    public static final String PRE_2FA_USER_ID = "CAPITALL_PRE_2FA_USER_ID";
    public static final String PRE_2FA_USERNAME = "CAPITALL_PRE_2FA_USERNAME";

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public TwoFactorAuthenticationSuccessHandler() {
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user && user.isTotpEnabled()) {
            HttpSession session = request.getSession(true);
            session.setAttribute(PRE_2FA_USER_ID, user.getId().toString());
            session.setAttribute(PRE_2FA_USERNAME, user.getUsername());

            SecurityContext emptyCtx = SecurityContextHolder.createEmptyContext();
            SecurityContextHolder.setContext(emptyCtx);
            securityContextRepository.saveContext(emptyCtx, request, response);

            response.sendRedirect(request.getContextPath() + "/login/2fa");
            return;
        }
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }
}
