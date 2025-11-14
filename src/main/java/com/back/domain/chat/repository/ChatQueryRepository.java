package com.back.domain.chat.repository;

import com.back.domain.chat.dto.ChatMessageDto;
import com.back.domain.chat.dto.ChatPostDto;
import com.back.domain.chat.dto.ChatRoomDto;
import com.back.domain.chat.dto.OtherMemberDto;
import com.back.domain.chat.entity.ChatRoom;
import com.back.domain.chat.entity.QChatMember;
import com.back.domain.chat.entity.QChatMessage;
import com.back.domain.chat.entity.QChatRoom;
import com.back.domain.member.entity.QMember;
import com.back.domain.post.entity.QPost;
import com.back.global.queryDsl.CustomQuerydslRepositorySupport;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.function.Function;

import static com.back.domain.chat.entity.QChatMember.chatMember;
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
                        chatMember.member.id.in(guestId)
                )
                .groupBy(chatRoom.id)
                .fetchOne();

        return Optional.ofNullable(chatRoomId);
    }

    public Page<ChatRoomDto> getMyChatRooms(Long memberId, Pageable pageable, String keyword) {
        QChatMember me = new QChatMember("me");
        QChatMember other = new QChatMember("otherMember");

        // 공통 조건
        BooleanExpression condition = me.member.id.eq(memberId)
                .and(createKeywordCondition(keyword));

        //contentQuery
        Function<JPAQueryFactory, JPAQuery<ChatRoomDto>> contentQuery = query -> query
                .select(Projections.constructor(ChatRoomDto.class,
                        chatRoom.id,
                        chatRoom.createdAt,
                        Projections.constructor(ChatPostDto.class,
                                post.title
                        ),
                        Projections.constructor(OtherMemberDto.class,
                                member.id,
                                member.nickname,
                                member.profileImgUrl
                        )))
                .from(chatRoom)
                .join(chatRoom.post, post)
                .join(chatRoom.chatMembers, me)
                .join(chatRoom.chatMembers, other)
                .join(other.member, member)
                .where(condition.and(other.member.id.ne(memberId)))
                .orderBy(chatRoom.id.desc());

        //countQuery
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
        QChatRoom qChatRoom = QChatRoom.chatRoom;
        QChatMember qChatMember = QChatMember.chatMember;

        ChatRoom chatRoom = selectFrom(qChatRoom)
                .join(qChatRoom.post, QPost.post).fetchJoin()
                .join(qChatRoom.chatMembers, qChatMember).fetchJoin()
                .join(qChatMember.member, QMember.member).fetchJoin()
                .where(qChatRoom.id.eq(chatRoomId))
                .distinct()
                .fetchOne();

        return Optional.ofNullable(chatRoom);
    }

    public Page<ChatMessageDto> getChatMessages(Long chatRoomId, Long memberId, Pageable pageable) {
        QChatMessage qChatMessage = QChatMessage.chatMessage;
        QChatMember qChatMember = QChatMember.chatMember;
        QMember qMember = QMember.member;

        //contentQuery
        Function<JPAQueryFactory, JPAQuery<ChatMessageDto>> contentQuery = query -> query
                .select(Projections.constructor(ChatMessageDto.class,
                        qChatMessage.id,
                        qMember.id,
                        qChatMessage.content,
                        qChatMessage.createdAt
                ))
                .from(qChatMessage)
                .join(qChatMember).on(qChatMessage.chatMemberId.eq(qChatMember.id))
                .join(qMember).on(qChatMember.member.id.eq(qMember.id))
                .where(
                        qChatMessage.chatRoomId.eq(chatRoomId),
                        qChatMember.chatRoom.id.eq(chatRoomId)
                )
                .orderBy(qChatMessage.id.desc());

        //countQuery
        Function<JPAQueryFactory, JPAQuery<Long>> countQuery = query -> query
                .select(qChatMessage.count())
                .from(qChatMessage)
                .where(qChatMessage.chatRoomId.eq(chatRoomId));

        return applyPagination(pageable, contentQuery, countQuery);
    }

    public boolean isMemberInChatRoom(Long chatRoomId, Long memberId) {
        QChatMember qChatMember = QChatMember.chatMember;

        Integer count = select(qChatMember.id.count().intValue())
                .from(qChatMember)
                .where(
                        qChatMember.chatRoom.id.eq(chatRoomId),
                        qChatMember.member.id.eq(memberId)
                )
                .fetchOne();

        return count != null && count > 0;
    }

    public Long findChatMemberId(Long chatRoomId, Long memberId) {
        QChatMember qChatMember = QChatMember.chatMember;

        return select(qChatMember.id)
                .from(qChatMember)
                .where(
                        qChatMember.chatRoom.id.eq(chatRoomId),
                        qChatMember.member.id.eq(memberId)
                )
                .fetchOne();
    }
}