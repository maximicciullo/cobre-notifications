package com.cobre.notifications.infrastructure.adapter.out.persistence.repository;

import com.cobre.notifications.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventJpaRepository extends JpaRepository<NotificationEventEntity, String> {
}
