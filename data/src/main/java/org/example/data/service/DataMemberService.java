package org.example.data.service;

import jakarta.transaction.Transactional;
import org.example.data.entities.Member;
import org.example.data.repository.MemberRepository;
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
