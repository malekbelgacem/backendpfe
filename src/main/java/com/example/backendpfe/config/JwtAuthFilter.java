package com.example.backendpfe.config;

import com.example.backendpfe.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Pour debug → à supprimer ou passer en logger plus tard
        System.out.println("JwtAuthFilter → Path: " + request.getRequestURI());

        // On NE saute PAS les routes ici → c'est déjà géré dans SecurityConfig avec .permitAll()
        // Le filtre JWT doit juste ignorer les requêtes SANS token (ne pas planter)
        // et ne valider que quand il y a un Bearer valide

        final String authHeader = request.getHeader("Authorization");

        // Cas 1 : pas de header → on passe (important pour /signup, /signin)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Cas 2 : header présent mais token vide ou mal formé
        String jwt = authHeader.substring(7).trim();
        if (jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractUsername(jwt);

            // Si username valide ET pas déjà authentifié dans le contexte
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token invalide/expiré → on ne met rien dans le contexte → Spring Security bloquera si besoin
            // → NE PAS renvoyer 401 ici manuellement
        }

        filterChain.doFilter(request, response);
    }
}