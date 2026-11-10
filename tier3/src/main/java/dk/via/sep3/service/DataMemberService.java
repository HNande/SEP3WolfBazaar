package dk.via.sep3.service;

import jakarta.transaction.Transactional;
import dk.via.sep3.entities.Member;
import dk.via.sep3.repository.MemberRepository;
import org.springframework.stereotype.Service;

@Service
public class DataMemberService {

    private final MemberRepository memberRepository;

    public DataMemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public boolean addMember(String firstName, String lastName, String username, String password)
    {
        if(memberRepository.existsByUsername(username))
        {
            return false;
        }

        Member newMember = new Member(firstName, lastName, username, password);
        memberRepository.save(newMember);
        return true;
    }
}
