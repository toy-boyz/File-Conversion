package com.toyboyz.fileconversion.worker.job.repository;

import com.toyboyz.fileconversion.worker.job.entity.WorkerJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerJobRepository extends JpaRepository<WorkerJob, Long> {
}