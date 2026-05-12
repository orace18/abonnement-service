package com.clubchretien.abonnement.configs;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Custom UserDetails implementation that includes user ID and user type
 * for proper JWT token generation and authentication
 */
public class CustomUserDetails implements UserDetails {
    
    private final String userId;
    private final String userType; // "USER" or "PARTENAIRE"
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;

    public CustomUserDetails(String userId, String userType, String username, String password,
                           Collection<? extends GrantedAuthority> authorities,
                           boolean accountNonExpired, boolean accountNonLocked,
                           boolean credentialsNonExpired, boolean enabled) {
        this.userId = userId;
        this.userType = userType;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserType() {
        return userType;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}