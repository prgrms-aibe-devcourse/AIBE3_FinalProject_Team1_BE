
package com.back.domain.post.dto.res;

import com.back.domain.member.entity.Member;

public record PostAuthorDto(
        Long id,
        String nickname,
        String profileImgUrl
) {
    public static PostAuthorDto from(Member member) {
        return new PostAuthorDto(
                member.getId(),
                member.getName(),
                member.getProfileImgUrl()
        );
    }
}
