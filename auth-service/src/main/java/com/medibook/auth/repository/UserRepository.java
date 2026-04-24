package com.medibook.auth.repository;

import com.medibook.auth.entity.User;
import com.medibook.auth.enums.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserId(String userId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<User> findAllByRole(Role role);

    Optional<User> findByPhone(String phone);

    List<User> findByFullNameContaining(String fullName);

    void deleteByUserId(String userId);
}

