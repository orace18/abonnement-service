package com.clubchretien.abonnement.abonnement.services;

import com.owomi.database.entities.User;
import com.owomi.database.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserSubscriptionService {

    @Autowired
    private UserRepository userRepository;

    /**
     *
     * Vérifie si un utilisateur existe
     */
    public boolean isUserExists(Long userId) {
        User user = userRepository.findOneById(userId);
        return user != null;
    }

    /**
     * Vérifie si un utilisateur est actif (non désactivé)
     */
    public boolean isUserActive(Long userId) {
        User user = userRepository.findOneById(userId);
        return user != null && (user.getDisabled() == null || !user.getDisabled());
    }

    /**
     * Récupère tous les utilisateurs actifs
     */
    public List<User> getActiveUsers() {
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .filter(u -> u.getDisabled() == null || !u.getDisabled())
                .collect(Collectors.toList());
    }

    /**
     * Récupère un utilisateur par son ID
     */
    public User getUserById(Long userId) {
        return userRepository.findOneById(userId);
    }

    /**
     * Récupère un utilisateur par son numéro de téléphone
     */
    public User getUserByTelephone(String telephone) {
        return userRepository.findOneByPhoneFullNumber(telephone);
    }

    /**
     * Récupère un utilisateur par son email
     */
    public User getUserByEmail(String email) {
        List<User> users = userRepository.findAllByEmail(email);
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Récupère tous les utilisateurs
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }


}
