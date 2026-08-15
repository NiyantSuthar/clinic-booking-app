package com.clinic.booking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Branches on the token's "role" claim:
 * - PATIENT -> principal is the accountId as a Long (so
 *   @AuthenticationPrincipal Long accountId keeps working unchanged on
 *   BeneficiaryController, exactly as in Session 5).
 * - ADMIN   -> principal is the literal string "ADMIN" (there's no
 *   accountId for admin - controllers gated to admin-only routes
 *   shouldn't need the principal's value at all, only the ROLE_ADMIN
 *   authority, which SecurityConfig checks).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Optional<JwtService.ParsedToken> parsed = jwtService.parseToken(token);

            if (parsed.isPresent()) {
                JwtService.ParsedToken p = parsed.get();

                if ("ADMIN".equals(p.role())) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            "ADMIN", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else if ("PATIENT".equals(p.role())) {
                    Long accountId = Long.valueOf(p.subject());
                    var authentication = new UsernamePasswordAuthenticationToken(
                            accountId, null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                // Unknown/missing role claim -> leave unauthenticated, same as an invalid token.
            }
        }

        filterChain.doFilter(request, response);
    }
}