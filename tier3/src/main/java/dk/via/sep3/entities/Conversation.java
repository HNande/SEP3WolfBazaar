package dk.via.sep3.entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversation", uniqueConstraints = @UniqueConstraint(
        name = "uk_conversation_listing_sender",
        columnNames = {"item_listing_id", "sender_member_id"}))
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_member_id", nullable = false)
    private Member owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_member_id", nullable = false)
    private Member sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_listing_id", nullable = false)
    private ItemListing itemListing;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, messageId ASC")
    private List<ConversationMessage> messages = new ArrayList<>();

    public Conversation() {
    }

    public Conversation(Member owner, Member sender, ItemListing itemListing) {
        this.owner = owner;
        this.sender = sender;
        this.itemListing = itemListing;
    }

    public void addMessage(ConversationMessage message) {
        messages.add(message);
        message.setConversation(this);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Member getOwner() { return owner; }
    public void setOwner(Member owner) { this.owner = owner; }
    public Member getSender() { return sender; }
    public void setSender(Member sender) { this.sender = sender; }
    public ItemListing getItemListing() { return itemListing; }
    public void setItemListing(ItemListing itemListing) { this.itemListing = itemListing; }
    public List<ConversationMessage> getMessages() { return messages; }
    public void setMessages(List<ConversationMessage> messages) { this.messages = messages; }
}