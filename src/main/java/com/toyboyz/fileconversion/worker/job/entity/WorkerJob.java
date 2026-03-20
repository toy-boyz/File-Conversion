package com.toyboyz.fileconversion.worker.job.entity;

import com.toyboyz.fileconversion.debezium.entity.OutboxEvent;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "worker_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkerJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long jobId;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "LONGTEXT")
    private String payload; // JSON 문자열

    public static WorkerJob of(String payload) {
        return WorkerJob.builder()
                .payload(payload)
                .build();
    }
}