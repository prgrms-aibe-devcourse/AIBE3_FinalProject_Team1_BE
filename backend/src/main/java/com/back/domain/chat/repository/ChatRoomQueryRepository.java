package com.back.domain.chat.repository;

import com.back.domain.chat.dto.ChatMessageDto;
import com.back.domain.chat.dto.ChatPostDto;
import com.back.domain.chat.dto.ChatRoomListDto;
import com.back.domain.chat.dto.OtherMemberDto;
import com.back.domain.chat.entity.ChatMember;
import com.back.domain.chat.entity.ChatRoom;
import com.back.domain.chat.entity.QChatMember;
import com.back.domain.member.entity.Member;
import com.back.global.queryDsl.CustomQuerydslRepositorySupport;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.function.Function;

import static com.back.domain.chat.entity.QChatMember.chatMember;
import static com.back.domain.chat.entity.QChatMessage.chatMessage;
import static com.back.domain.chat.entity.QChatRoom.chatRoom;
import static com.back.domain.member.entity.QMember.member;

@Repository
public class ChatRoomQueryRepository extends CustomQuerydslRepositorySupport {

    public ChatRoomQueryRepository() {
        super(ChatRoom.class);
    }

    public Optional<Long> getChatRoomId(Long postId, Long guestId) {
        Long chatRoomId = select(chatRoom.id)
                .from(chatRoom)
                .join(chatMember).on(chatMember.chatRoomId.eq(chatRoom.id))
                .where(
                        chatRoom.postId.eq(postId),
                        chatMember.memberId.eq(guestId)
                )
                .fetchOne();

        return Optional.ofNullable(chatRoomId);
    }

    public Page<ChatRoomListDto> getMyChatRooms(Long memberId, Pageable pageable, String keyword) {
        QChatMember me = new QChatMember("me");
        QChatMember other = new QChatMember("other");

        BooleanExpression condition = me.memberId.eq(memberId)
                .and(createKeywordCondition(keyword));

        // Content Query
        Function<JPAQueryFactory, JPAQuery<ChatRoomListDto>> contentQuery = query -> query
                .select(Projections.constructor(ChatRoomListDto.class,
                        chatRoom.id,
                        chatRoom.createdAt,
                        Projections.constructor(ChatPostDto.class,
                                chatRoom.postTitleSnapshot
                        ),
                        Projections.constructor(OtherMemberDto.class,
                                member.id,
                                member.nickname,
                                member.profileImgUrl
                        ),
                        chatRoom.lastMessage,
                        chatRoom.lastMessageTime,
                        Expressions.nullExpression(Integer.class)
                ))
                .from(chatRoom)
                .join(me).on(me.chatRoomId.eq(chatRoom.id))
                .join(other).on(other.chatRoomId.eq(chatRoom.id))
                .join(member).on(member.id.eq(other.memberId))
                .where(condition.and(other.memberId.ne(memberId)))
                .orderBy(chatRoom.lastMessageTime.desc().nullsLast());

        // Count Query
        Function<JPAQueryFactory, JPAQuery<Long>> countQuery = query -> query
                .select(chatRoom.id.countDistinct())
                .from(chatRoom)
                .join(me).on(me.chatRoomId.eq(chatRoom.id))
                .where(condition);

        return applyPagination(pageable, contentQuery, countQuery);
    }

    private BooleanExpression createKeywordCondition(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return chatRoom.postTitleSnapshot.containsIgnoreCase(keyword)
                .or(member.nickname.containsIgnoreCase(keyword));
    }

    public Optional<ChatRoom> getChatRoom(Long chatRoomId) {
        ChatRoom result = selectFrom(chatRoom)
                .where(chatRoom.id.eq(chatRoomId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    public boolean isMemberInChatRoom(Long chatRoomId, Long memberId) {
        Integer count = select(chatMember.id.count().intValue())
                .from(chatMember)
                .where(
                        chatMember.chatRoomId.eq(chatRoomId),
                        chatMember.memberId.eq(memberId)
                )
                .fetchOne();

        return count != null && count > 0;
    }

    public Optional<ChatMember> findChatMember(Long chatRoomId, Long memberId) {
        ChatMember result = select(chatMember)
                .from(chatMember)
                .where(
                        chatMember.chatRoomId.eq(chatRoomId),
                        chatMember.memberId.eq(memberId)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    public Optional<Long> findOtherMemberId(Long chatRoomId, Long memberId) {
        Long otherMemberId = select(chatMember.memberId)
                .from(chatMember)
                .where(
                        chatMember.chatRoomId.eq(chatRoomId),
                        chatMember.memberId.ne(memberId)
                )
                .fetchOne();

        return Optional.ofNullable(otherMemberId);
    }

    public Optional<Member> findOtherMember(Long chatRoomId, Long memberId) {
        Member otherMember = select(member)
                .from(chatMember)
                .join(member).on(chatMember.memberId.eq(member.id))
                .where(
                        chatMember.chatRoomId.eq(chatRoomId),
                        chatMember.memberId.ne(memberId)
                )
                .fetchOne();

        return Optional.ofNullable(otherMember);
    }
}