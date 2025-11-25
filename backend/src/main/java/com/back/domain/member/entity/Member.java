package com.back.domain.member.entity;

import com.back.domain.member.common.MemberRole;
import com.back.domain.member.dto.MemberUpdateReqBody;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column
    private String name;

    @Column(unique = true)
    private String phoneNumber;

    @Column
    private String address1;

    @Column
    private String address2;

    @Column(unique = true, nullable = false)
    private String nickname;

    @Column(nullable = false)
    private boolean isBanned;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Column
    private String profileImgUrl;

    public Member(String email, String password, String name, String phoneNumber,
                  String address1, String address2, String nickname, MemberRole role, String profileImgUrl) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address1 = address1;
        this.address2 = address2;
        this.nickname = nickname;
        this.isBanned = false;
        this.role = role;
        this.profileImgUrl = profileImgUrl;
    }

    public Member(String email, String password, String name, String phoneNumber,
                  String address1, String address2, String nickname, MemberRole role) {
        this(email, password, name, phoneNumber, address1, address2, nickname, role, null);
    }
    public Member(String email, String password, String name, String phoneNumber,
                  String address1, String address2, String nickname) {
        this(email, password, name, phoneNumber, address1, address2, nickname, MemberRole.USER, null);
    }
    public Member(String email, String password, String nickname, MemberRole role) {
        this(email, password, null, null, null, null, nickname, role, null);
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public void updateMember(MemberUpdateReqBody reqBody) {
        this.address1 = reqBody.address1();
        this.address2 = reqBody.address2();
        this.nickname = reqBody.nickname();
        this.phoneNumber = reqBody.phoneNumber();
    }

    public void updateProfileImage(String profileImgUrl) {
        this.profileImgUrl = profileImgUrl;
    }
}
