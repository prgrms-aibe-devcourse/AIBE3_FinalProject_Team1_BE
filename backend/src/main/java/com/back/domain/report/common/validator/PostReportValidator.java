package com.back.domain.report.common.validator;


import com.back.domain.post.entity.Post;
import com.back.domain.post.repository.PostRepository;
import com.back.domain.report.common.ReportType;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostReportValidator implements ReportValidator {

    private final PostRepository postRepository;

    @Override
    public boolean validateTargetId(ReportType reportType, Long targetId, Long reporterId) {
        if (reportType != ReportType.POST) {
            return false;
        }

        Post post = postRepository.findById(targetId)
                                  .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));

        if (post.getAuthor().getId().equals(reporterId)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "본인의 게시글은 신고할 수 없습니다.");
        }

        return true;
    }
}
