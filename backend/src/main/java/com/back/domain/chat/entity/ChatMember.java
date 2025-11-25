package com.back.domain.chat.entity;

import com.back.domain.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "chat_member")
public class ChatMember extends BaseEntity {

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "last_read_message_id", nullable = false)
    private Long lastReadMessageId;

    public static ChatMember create(Long chatRoomId, Long memberId) {
        ChatMember chatMember = new ChatMember();
        chatMember.chatRoomId = chatRoomId;
        chatMember.memberId = memberId;
        chatMember.lastReadMessageId = 0L;

        return chatMember;
    }

    public void updateLastReadMessageId(Long messageId) {
        if(messageId != null && messageId > this.lastReadMessageId){
            this.lastReadMessageId = messageId;
        }
    }
}
