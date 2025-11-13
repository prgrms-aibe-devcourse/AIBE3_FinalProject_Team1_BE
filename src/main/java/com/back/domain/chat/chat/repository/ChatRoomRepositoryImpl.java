package com.back.domain.chat.chat.repository;

import com.back.domain.chat.chat.dto.ChatPostDto;
import com.back.domain.chat.chat.dto.ChatRoomDto;
import com.back.domain.chat.chat.dto.OtherMemberDto;
import com.back.domain.chat.chat.entity.QChatMember;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.back.domain.chat.chat.entity.QChatMember.chatMember;
import static com.back.domain.chat.chat.entity.QChatRoom.chatRoom;
import static com.back.domain.member.entity.QMember.member;
import static com.back.domain.post.entity.QPost.post;

@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Long> findIdByPostAndMembers(Long postId, Long hostId, Long guestId) {
        Long chatRoomId = queryFactory
                .select(chatRoom.id)
                .from(chatRoom)
                .join(chatRoom.chatMembers, chatMember)
                .where(
                        chatRoom.post.id.eq(postId),
                        chatMember.member.id.in(hostId, guestId)
                )
                .groupBy(chatRoom.id)
                .having(chatMember.count().eq(2L))
                .fetchOne();

        return Optional.ofNullable(chatRoomId);
    }

    @Override
    public Page<ChatRoomDto> findByMemberId(Long memberId, Pageable pageable, String keyword) {
        QChatMember me = new QChatMember("me");
        QChatMember other = new QChatMember("otherMember");

        // 공통 조건
        BooleanExpression condition = me.member.id.eq(memberId)
                .and(other.member.id.ne(memberId))
                .and(createKeywordCondition(keyword));

        Long total = queryFactory
                .select(chatRoom.id.countDistinct())
                .from(chatRoom)
                .join(chatRoom.chatMembers, me)
                .join(chatRoom.chatMembers, other)
                .join(chatRoom.post, post)
                .join(other.member, member)
                .where(condition)
                .fetchOne();

        List<ChatRoomDto> content = queryFactory
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
                        )
                ))
                .from(chatRoom)
                .join(chatRoom.post, post)
                .join(chatRoom.chatMembers, me)
                .join(chatRoom.chatMembers, other)
                .join(other.member, member)
                .where(condition)
                .orderBy(chatRoom.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanExpression createKeywordCondition(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return post.title.containsIgnoreCase(keyword)
                .or(member.nickname.containsIgnoreCase(keyword));
    }
}