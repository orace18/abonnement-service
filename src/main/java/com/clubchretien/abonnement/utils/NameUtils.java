package com.clubchretien.abonnement.utils;

import java.text.Normalizer;

public class NameUtils {
    /**
     * Normalise un nom :
     * - Enlève les accents
     * - Supprime les espaces multiples
     * - Met tout en minuscule
     */
    public static String normalizeName(String name) {
        if (name == null) return "";

        // Enlève les accents : "é" => "e"
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Supprime les caractères spéciaux inutiles (optionnel)
        normalized = normalized.replaceAll("[^\\p{L}\\p{Z}]", ""); // garde lettres et espaces

        // Met tout en minuscule, supprime les espaces en trop
        return normalized.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /**
     * Compare deux noms après normalisation.
     */
    public static boolean areNamesEqual(String name1, String name2) {
        return normalizeName(name1).equals(normalizeName(name2));
    }
}
