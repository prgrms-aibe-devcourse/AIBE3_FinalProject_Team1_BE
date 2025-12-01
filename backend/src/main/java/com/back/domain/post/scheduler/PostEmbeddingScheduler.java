package com.back.domain.post.scheduler;

import com.back.domain.post.scheduler.job.PostEmbeddingJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class PostEmbeddingScheduler {

    @Bean
    public JobDetail postEmbeddingJobDetail() {
        return JobBuilder.newJob(PostEmbeddingJob.class)
                .withIdentity("PostEmbeddingJob", "post")
                .withDescription("게시글 임베딩 작업")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger postEmbeddingTrigger(JobDetail postEmbeddingJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(postEmbeddingJobDetail)
                .withIdentity("PostEmbeddingTrigger", "post")
                .withSchedule(
                        CronScheduleBuilder.cronSchedule("0 0 * * * ?")
//                            SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(30).repeatForever()
                )
                .build();
    }

    @Bean
    public CommandLineRunner registerPostEmbeddingJob(
            Scheduler scheduler,
            JobDetail postEmbeddingJobDetail,
            Trigger postEmbeddingTrigger
    ) {
        return args -> {
            JobKey jobKey = postEmbeddingJobDetail.getKey();

            if (!scheduler.checkExists(jobKey)) {
                scheduler.scheduleJob(postEmbeddingJobDetail, postEmbeddingTrigger);
                log.info("✅ 스케줄러 등록 완료: 게시글 임베딩 작업이 정각마다 실행됩니다.");
            } else {
                scheduler.rescheduleJob(
                        postEmbeddingTrigger.getKey(),
                        postEmbeddingTrigger
                );
                log.info("🔄 스케줄러 재등록 완료: 게시글 임베딩 작업이 갱신되었습니다.");
            }
        };
    }
}
