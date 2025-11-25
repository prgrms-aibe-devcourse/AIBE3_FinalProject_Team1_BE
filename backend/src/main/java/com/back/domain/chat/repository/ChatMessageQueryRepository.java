package com.back.domain.chat.repository;

import com.back.domain.chat.dto.ChatMessageDto;
import com.back.domain.chat.entity.ChatMessage;
import com.back.global.queryDsl.CustomQuerydslRepositorySupport;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.function.Function;

import static com.back.domain.chat.entity.QChatMember.chatMember;
import static com.back.domain.chat.entity.QChatMessage.chatMessage;
import static com.back.domain.member.entity.QMember.member;

@Repository
public class ChatMessageQueryRepository extends CustomQuerydslRepositorySupport {

    public ChatMessageQueryRepository() {
        super(ChatMessage.class);
    }

    public Page<ChatMessageDto> getChatMessages(Long chatRoomId, Long memberId, Pageable pageable) {
        Function<JPAQueryFactory, JPAQuery<ChatMessageDto>> contentQuery = query -> query
                .select(Projections.constructor(ChatMessageDto.class,
                        chatMessage.id,
                        member.id,
                        chatMessage.content,
                        chatMessage.createdAt
                ))
                .from(chatMessage)
                .join(chatMember).on(chatMessage.chatMemberId.eq(chatMember.id))
                .join(member).on(chatMember.memberId.eq(member.id))
                .where(chatMessage.chatRoomId.eq(chatRoomId))
                .orderBy(chatMessage.id.desc());

        Function<JPAQueryFactory, JPAQuery<Long>> countQuery = query -> query
                .select(chatMessage.count())
                .from(chatMessage)
                .where(chatMessage.chatRoomId.eq(chatRoomId));

        return applyPagination(pageable, contentQuery, countQuery);
    }
}
