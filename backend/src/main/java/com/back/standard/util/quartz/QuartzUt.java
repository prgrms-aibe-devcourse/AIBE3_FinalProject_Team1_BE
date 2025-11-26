package com.back.standard.util.quartz;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class QuartzUt {
    
    // 반납 하루 전 10시
    public static LocalDateTime reminderAt10AmOneDayBefore(LocalDateTime returnDateTime) {
        LocalDate returnDate = returnDateTime.toLocalDate();
        LocalDate reminderDate = returnDate.minusDays(1);
        
        return reminderDate.atTime(10, 0);
    }

    // 예약 생성 후 10초 뒤 (테스트 용)
    public static LocalDateTime after10Seconds(LocalDateTime createdAt) {
        return createdAt.plusSeconds(10);
    }
}
