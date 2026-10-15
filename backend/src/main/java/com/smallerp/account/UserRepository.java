package com.smallerp.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginIdIgnoreCase(String loginId);
}
