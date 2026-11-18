package dk.via.sep3.service;

import jakarta.transaction.Transactional;
import dk.via.sep3.entities.Member;
import dk.via.sep3.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DataMemberService {

    private final MemberRepository memberRepository;

    public DataMemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public Optional<Member> addMember(String firstName, String lastName, String username, String password, String email)
    {
        if(memberRepository.existsByUsername(username)) {
            return Optional.empty();
        }

        Member newMember = new Member(firstName,lastName,username,password,email);
        memberRepository.save(newMember);
        return Optional.of(newMember);
    }

    public Optional<Member> login(String username, String password) {
        return memberRepository.findByUsernameAndPassword(username, password);
    }
}
