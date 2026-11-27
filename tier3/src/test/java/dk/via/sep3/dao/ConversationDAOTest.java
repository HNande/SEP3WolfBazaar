package dk.via.sep3.dao;

import dk.via.sep3.dto.ConversationDataDTO;
import dk.via.sep3.dto.ConversationSummaryDataDTO;
import dk.via.sep3.dto.MessageDataDTO;
import dk.via.sep3.entities.Conversation;
import dk.via.sep3.entities.ConversationMessage;
import dk.via.sep3.entities.ItemListing;
import dk.via.sep3.entities.Member;
import dk.via.sep3.repository.ConversationRepository;
import dk.via.sep3.repository.ItemListingRepository;
import dk.via.sep3.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationDAOTest {

    private ConversationRepository conversationRepository;
    private MemberRepository memberRepository;
    private ItemListingRepository itemListingRepository;
    private ConversationDAO dao;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        memberRepository = mock(MemberRepository.class);
        itemListingRepository = mock(ItemListingRepository.class);
        dao = new ConversationDAO(conversationRepository, memberRepository, itemListingRepository);
    }

    @Test
    void addConversationChecksEntitiesAndMapsSavedConversation() {
        Member owner = member(2);
        Member sender = member(3);
        ItemListing listing = listing(4, 2, "Coat");
        when(memberRepository.findById(2)).thenReturn(Optional.of(owner));
        when(memberRepository.findById(3)).thenReturn(Optional.of(sender));
        when(itemListingRepository.findById(4)).thenReturn(Optional.of(listing));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation saved = invocation.getArgument(0);
            saved.setId(12);
            return saved;
        });

        ConversationDataDTO result = dao.addConversation(2, 3, 4);

        assertEquals(new ConversationDataDTO(12, List.of()), result);
        ArgumentCaptor<Conversation> saved = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(saved.capture());
        assertEquals(owner, saved.getValue().getOwner());
        assertEquals(sender, saved.getValue().getSender());
        assertEquals(listing, saved.getValue().getItemListing());
    }

    @Test
    void addConversationRejectsMissingEntitiesWrongOwnerAndSameParticipant() {
        when(memberRepository.findById(2)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> dao.addConversation(2, 3, 4));

        when(memberRepository.findById(2)).thenReturn(Optional.of(member(2)));
        when(memberRepository.findById(3)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> dao.addConversation(2, 3, 4));

        when(memberRepository.findById(3)).thenReturn(Optional.of(member(3)));
        when(itemListingRepository.findById(4)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> dao.addConversation(2, 3, 4));

        when(itemListingRepository.findById(4)).thenReturn(Optional.of(listing(4, 9, "Coat")));
        assertThrows(IllegalArgumentException.class, () -> dao.addConversation(2, 3, 4));

        when(itemListingRepository.findById(4)).thenReturn(Optional.of(listing(4, 2, "Coat")));
        assertThrows(IllegalArgumentException.class, () -> dao.addConversation(2, 2, 4));
    }

    @Test
    void getConversationMapsMessagesAndMissingConversationRemainsEmpty() {
        Conversation conversation = conversation(12, member(2), member(3), listing(4, 2, "Coat"));
        ConversationMessage message = new ConversationMessage(member(3), "Hello");
        message.setMessageId(8);
        message.setCreatedAt(Instant.EPOCH);
        conversation.addMessage(message);
        when(conversationRepository.findWithMessagesById(12)).thenReturn(Optional.of(conversation));

        assertEquals(new ConversationDataDTO(12,
                        List.of(new MessageDataDTO(8, 12, 3, "Hello", Instant.EPOCH))),
                dao.getConversation(12).orElseThrow());
        when(conversationRepository.findWithMessagesById(99)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), dao.getConversation(99));
    }

    @Test
    void removeConversationReturnsFalseWhenMissingAndDeletesWhenPresent() {
        when(conversationRepository.findById(1)).thenReturn(Optional.empty());
        assertFalse(dao.removeConversation(1));

        Conversation conversation = conversation(2, member(3), member(4), listing(5, 3, "Table"));
        when(conversationRepository.findById(2)).thenReturn(Optional.of(conversation));
        assertTrue(dao.removeConversation(2));
        verify(conversationRepository).delete(conversation);
    }

    @Test
    void getConversationSummaryListMapsOwnerAndListingName() {
        Conversation conversation = conversation(12, member(2), member(3), listing(4, 2, "Coat"));
        when(conversationRepository.findSummariesForMember(2)).thenReturn(List.of(conversation));

        assertEquals(List.of(new ConversationSummaryDataDTO(12, 2, "Coat")),
                dao.getConversationSummaryList(2));
    }

    @Test
    void addMessageRequiresParticipantAndPersistsLinkedMessage() {
        Member owner = member(2);
        Member sender = member(3);
        Conversation conversation = conversation(12, owner, sender, listing(4, 2, "Coat"));
        when(conversationRepository.findById(12)).thenReturn(Optional.of(conversation));
        when(memberRepository.findById(3)).thenReturn(Optional.of(sender));

        MessageDataDTO result = dao.addMessage(12, 3, "Hello");

        assertEquals(12, result.conversationId());
        assertEquals(3, result.senderId());
        assertEquals("Hello", result.content());
        assertEquals(1, conversation.getMessages().size());
        assertEquals("Hello", conversation.getMessages().get(0).getContent());
        assertEquals(conversation, conversation.getMessages().get(0).getConversation());
        verify(conversationRepository).save(conversation);
    }

    @Test
    void addMessageRejectsMissingConversationMemberAndNonParticipant() {
        when(conversationRepository.findById(12)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> dao.addMessage(12, 3, "Hello"));

        Conversation conversation = conversation(12, member(2), member(3), listing(4, 2, "Coat"));
        when(conversationRepository.findById(12)).thenReturn(Optional.of(conversation));
        when(memberRepository.findById(4)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> dao.addMessage(12, 4, "Hello"));

        when(memberRepository.findById(4)).thenReturn(Optional.of(member(4)));
        assertThrows(IllegalArgumentException.class, () -> dao.addMessage(12, 4, "Hello"));
    }

    private static Member member(int id) {
        Member member = new Member("First", "Last", "user" + id, "pw", "user" + id + "@example.com");
        member.setMemberId(id);
        return member;
    }

    private static ItemListing listing(int id, int ownerId, String name) {
        ItemListing listing = new ItemListing(name, "Description", 10, ownerId);
        listing.setItemId(id);
        return listing;
    }

    private static Conversation conversation(int id, Member owner, Member sender, ItemListing listing) {
        Conversation conversation = new Conversation(owner, sender, listing);
        conversation.setId(id);
        return conversation;
    }
}