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
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
public class ConversationDAO {

    private final ConversationRepository conversationRepository;
    private final MemberRepository memberRepository;
    private final ItemListingRepository itemListingRepository;

    public ConversationDAO(
            ConversationRepository conversationRepository,
            MemberRepository memberRepository,
            ItemListingRepository itemListingRepository) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.itemListingRepository = itemListingRepository;
    }

    @Transactional
    public ConversationDataDTO addConversation(int ownerId, int senderId, int itemListingId) {
        Member owner = memberRepository.findById(ownerId)
                .orElseThrow(() -> new NoSuchElementException("Member " + ownerId + " not found"));
        Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new NoSuchElementException("Member " + senderId + " not found"));
        ItemListing itemListing = itemListingRepository.findById(itemListingId)
                .orElseThrow(() -> new NoSuchElementException("Item listing " + itemListingId + " not found"));

        if (itemListing.getOwnerId() != ownerId) {
            throw new IllegalArgumentException("Conversation owner must own the item listing");
        }
        if (ownerId == senderId) {
            throw new IllegalArgumentException("Conversation participants must be different members");
        }

        Conversation conversation = conversationRepository.save(new Conversation(owner, sender, itemListing));
        return toDataDTO(conversation);
    }

    @Transactional(readOnly = true)
    public Optional<ConversationDataDTO> getConversation(int id) {
        return conversationRepository.findWithMessagesById(id).map(ConversationDAO::toDataDTO);
    }

    @Transactional
    public boolean removeConversation(int id) {
        Optional<Conversation> conversation = conversationRepository.findById(id);
        if (conversation.isEmpty()) {
            return false;
        }
        conversationRepository.delete(conversation.get());
        return true;
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryDataDTO> getConversationSummaryList(int memberId) {
        return conversationRepository.findSummariesForMember(memberId).stream()
                .map(conversation -> new ConversationSummaryDataDTO(
                        conversation.getId(),
                        conversation.getOwner().getMemberId(),
                        conversation.getItemListing().getItemName()))
                .toList();
    }

    @Transactional
    public MessageDataDTO addMessage(int conversationId, int senderId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NoSuchElementException("Conversation " + conversationId + " not found"));
        Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new NoSuchElementException("Member " + senderId + " not found"));

        if (conversation.getOwner().getMemberId() != senderId
                && conversation.getSender().getMemberId() != senderId) {
            throw new IllegalArgumentException("Only conversation participants can send messages");
        }

        ConversationMessage message = new ConversationMessage(sender, content);
        conversation.addMessage(message);
        conversationRepository.save(conversation);
        return toDataDTO(message);
    }

    private static ConversationDataDTO toDataDTO(Conversation conversation) {
        List<MessageDataDTO> messages = conversation.getMessages().stream()
                .map(ConversationDAO::toDataDTO)
                .toList();
        return new ConversationDataDTO(conversation.getId(), messages);
    }

    private static MessageDataDTO toDataDTO(ConversationMessage message) {
        return new MessageDataDTO(
                message.getMessageId(),
                message.getConversation().getId(),
                message.getSender().getMemberId(),
                message.getContent(),
                message.getCreatedAt());
    }
}