package dk.via.sep3.repository;

import dk.via.sep3.entities.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    boolean existsByUsername(String username);
}
