package com.recircle.service;

import com.recircle.dto.ApiDtos.NotificationDto;
import com.recircle.entity.Notification;
import com.recircle.entity.User;
import com.recircle.exception.ApiException;
import com.recircle.mapper.DtoMapper;
import com.recircle.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DtoMapper dtoMapper;

    public NotificationService(NotificationRepository notificationRepository, DtoMapper dtoMapper) {
        this.notificationRepository = notificationRepository;
        this.dtoMapper = dtoMapper;
    }

    @Transactional
    public Notification createNotification(
        User recipient,
        String type,
        String title,
        String message,
        String targetPath,
        Long relatedEntityId
    ) {
        Notification n = new Notification();
        n.setUser(recipient);
        n.setNotificationType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setTargetPath(targetPath);
        n.setRelatedEntityId(relatedEntityId);
        n.setRead(false);
        return notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(dtoMapper::toNotificationDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationDto markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found."));
        if (!n.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot modify another user's notification.");
        }
        n.setRead(true);
        return dtoMapper.toNotificationDto(notificationRepository.save(n));
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        int count = 0;
        for (Notification n : list) {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
                count++;
            }
        }
        return count;
    }
}
