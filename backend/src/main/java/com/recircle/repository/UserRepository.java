package com.recircle.repository;

import com.recircle.entity.User;
import com.recircle.enums.AccountStatus;
import com.recircle.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);
    long countByCampusVerifiedTrue();
    long countByAccountStatus(AccountStatus status);
    List<User> findByRoleIn(List<UserRole> roles);
}
