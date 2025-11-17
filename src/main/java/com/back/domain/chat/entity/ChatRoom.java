package com.back.domain.chat.entity;


import com.back.domain.member.entity.Member;
import com.back.domain.post.entity.Post;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMember> chatMembers = new ArrayList<>();

    //캐시 필드
    private String lastMessage;
    private LocalDateTime lastMessageTime;

    public static ChatRoom create(Post post, Member... members) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.post = post;
        for (Member member : members) {
            chatRoom.addMember(member);
        }
        return chatRoom;
    }

    private void addMember(Member member) {
        ChatMember chatMember = new ChatMember(this, member, 0L);
        this.chatMembers.add(chatMember);
    }

    public void updateLastMessage(String lastMessage, LocalDateTime lastMessageTime) {
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }
}
