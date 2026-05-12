package com.clubchretien.abonnement;

import java.security.SecureRandom;

public class FirebaseUidGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Génère un UID Firebase-like (28 caractères par défaut)
     * @return UID alphanumérique unique
     */
    public static String generate() {
        return generate(28);
    }

    /**
     * Génère un UID Firebase-like de longueur personnalisée
     * @param length Longueur du UID (par défaut Firebase ~28)
     * @return UID alphanumérique unique
     */
    public static String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    // Exemple d’utilisation
    public static void main(String[] args) {
        System.out.println("UID Firebase-like : " + generate());
    }
}
