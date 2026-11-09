package org.example.data.repository;

import org.example.data.entities.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    boolean existsByUsername(String username);
}
