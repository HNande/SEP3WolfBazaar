package dk.via.sep3.repository;

import dk.via.sep3.entities.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    boolean existsByUsername(String username);

    @Query("SELECT m FROM Member m WHERE m.username = :username AND m.password = :password")
    Optional<Member> findByUsernameAndPassword(@Param("username") String username, @Param("password") String password);
}
