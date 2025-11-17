package com.back.domain.chat.repository;

import com.back.domain.chat.dto.ChatMessageDto;
import com.back.domain.chat.dto.ChatPostDto;
import com.back.domain.chat.dto.ChatRoomListDto;
import com.back.domain.chat.dto.OtherMemberDto;
import com.back.domain.chat.entity.ChatMember;
import com.back.domain.chat.entity.ChatRoom;
import com.back.domain.chat.entity.QChatMember;
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
import static com.back.domain.post.entity.QPost.post;

@Repository
public class ChatQueryRepository extends CustomQuerydslRepositorySupport {

    public ChatQueryRepository() {
        super(ChatRoom.class);
    }

    public Optional<Long> getChatRoomId(Long postId, Long guestId) {
        Long chatRoomId = select(chatRoom.id)
                .from(chatRoom)
                .join(chatRoom.chatMembers, chatMember)
                .where(
                        chatRoom.post.id.eq(postId),
                        chatMember.member.id.eq(guestId)
                )
                .fetchOne();

        return Optional.ofNullable(chatRoomId);
    }

    public Page<ChatRoomListDto> getMyChatRooms(Long memberId, Pageable pageable, String keyword) {

        QChatMember me = new QChatMember("me");
        QChatMember other = new QChatMember("other");

        BooleanExpression condition = me.member.id.eq(memberId)
                .and(createKeywordCondition(keyword));

        // Content Query
        Function<JPAQueryFactory, JPAQuery<ChatRoomListDto>> contentQuery = query -> query
                .select(Projections.constructor(ChatRoomListDto.class,
                        chatRoom.id,
                        chatRoom.createdAt,
                        Projections.constructor(ChatPostDto.class,
                                post.title
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
                .join(chatRoom.post, post)
                .join(chatRoom.chatMembers, me)
                .join(chatRoom.chatMembers, other)
                .join(other.member, member)
                .where(condition.and(other.member.id.ne(memberId)))
                .orderBy(chatRoom.lastMessageTime.desc().nullsLast());

        // Count Query
        Function<JPAQueryFactory, JPAQuery<Long>> countQuery = query -> query
                .select(chatRoom.id.countDistinct())
                .from(chatRoom)
                .join(chatRoom.chatMembers, me)
                .where(condition);

        return applyPagination(pageable, contentQuery, countQuery);
    }

    private BooleanExpression createKeywordCondition(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return post.title.containsIgnoreCase(keyword)
                .or(member.nickname.containsIgnoreCase(keyword));
    }

    public Optional<ChatRoom> getChatRoom(Long chatRoomId) {

        ChatRoom result = selectFrom(chatRoom)
                .join(chatRoom.post, post).fetchJoin()
                .join(chatRoom.chatMembers, chatMember).fetchJoin()
                .join(chatMember.member, member).fetchJoin()
                .where(chatRoom.id.eq(chatRoomId))
                .distinct()
                .fetchOne();

        return Optional.ofNullable(result);
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
                .join(member).on(chatMember.member.id.eq(member.id))
                .where(chatMessage.chatRoomId.eq(chatRoomId))
                .orderBy(chatMessage.id.desc());

        Function<JPAQueryFactory, JPAQuery<Long>> countQuery = query -> query
                .select(chatMessage.count())
                .from(chatMessage)
                .where(chatMessage.chatRoomId.eq(chatRoomId));

        return applyPagination(pageable, contentQuery, countQuery);
    }

    public boolean isMemberInChatRoom(Long chatRoomId, Long memberId) {

        Integer count = select(chatMember.id.count().intValue())
                .from(chatMember)
                .where(
                        chatMember.chatRoom.id.eq(chatRoomId),
                        chatMember.member.id.eq(memberId)
                )
                .fetchOne();

        return count != null && count > 0;
    }

    public Optional<ChatMember> findChatMember(Long chatRoomId, Long memberId) {

        ChatMember result = select(chatMember)
                .from(chatMember)
                .join(chatMember.chatRoom, chatRoom).fetchJoin()
                .join(chatMember.member, member).fetchJoin()
                .where(
                        chatMember.chatRoom.id.eq(chatRoomId),
                        chatMember.member.id.eq(memberId)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    public Optional<Long> findOtherMemberId(Long chatRoomId, Long memberId) {
        Long otherMemberId = select(member.id)
                .from(chatMember)
                .join(chatMember.member, member)
                .where(
                        chatMember.chatRoom.id.eq(chatRoomId),
                        chatMember.member.id.ne(memberId)
                )
                .fetchOne();

        return Optional.ofNullable(otherMemberId);
    }
}
