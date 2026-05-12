package com.clubchretien.abonnement.authentication.uri;

import lombok.Data;
import org.springframework.http.HttpMethod;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


@Data
public class UriCollection<T extends String> {
    private Set<String> simples = new HashSet<>();
    private Set<Pattern> patterns = new HashSet<>();

    public void add(T uri) {
        String[] parts = uri.split(" ", 2);
        if (HttpMethod.resolve(parts[0]) != null && parts.length == 2)
            patterns.add(Pattern.compile(uri));
        else if (parts.length == 1) {
            simples.add(uri);
        }
        else return;
    }

    public void addAllSimple(Set<String> uris) {
        if (uris.isEmpty()) return;
        simples.addAll(uris);
    }

    public void addAllPatterns(Set<Pattern> uris) {
        if (uris.isEmpty()) return;
        patterns.addAll(uris);
    }

    public void addAll(UriCollection<T> uris) {
        this.addAllSimple(simples);
        this.addAllPatterns(patterns);
    }

    public boolean contains(T uri) {
        return simples.contains(uri);
    }

    public boolean contains(T uri, T httpMethod) {
        if (simples.contains(uri)) return true;
        return patterns.stream().anyMatch(pattern -> pattern.matcher(httpMethod + " " + uri).matches());
    }
}
