package io.github.student.agile.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String PRINCIPAL_ATTRIBUTE = "preferred_username";
    private static final String RESOURCE_ACCESS_ATTRIBUTE = "resource_access";
    private static final String ROLES_ATTRIBUTE = "roles";
    private static final String DEFAULT_ROLE_PREFIX = "ROLE_";

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Value("${jwt.auth.converter.resource-id}")
    private String resourceId;

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                jwtGrantedAuthoritiesConverter.convert(jwt).stream(),
                extractResourceRoles(jwt).stream()
        ).collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, authorities, getPrincipleClaimName(jwt));
    }

    private String getPrincipleClaimName(Jwt jwt) {
        String claimName = isNull(jwt.getClaim(PRINCIPAL_ATTRIBUTE)) ? JwtClaimNames.SUB : PRINCIPAL_ATTRIBUTE;
        return jwt.getClaim(claimName);
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        if (isNull(jwt.getClaim(RESOURCE_ACCESS_ATTRIBUTE))) {
            return Set.of();
        }

        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS_ATTRIBUTE);
        if (isNull(resourceAccess.get(resourceId))) {
            return Set.of();
        }

        Map<String, Object> resource = (Map<String, Object>) resourceAccess.get(resourceId);
        Collection<String> resourceRoles = (Collection<String>) resource.get(ROLES_ATTRIBUTE);
        return resourceRoles.stream()
                .map(role -> new SimpleGrantedAuthority(DEFAULT_ROLE_PREFIX.concat(role)))
                .collect(Collectors.toSet());
    }
}
