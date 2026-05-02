package com.example.esercizi.service;

import com.example.esercizi.model.Session;
import com.example.esercizi.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.example.esercizi.constants.AppConstants.SESSION_EXPIRATION_DAYS;

@Service
@Transactional
public class SessionService {
    private final SessionRepository sessionRepository;
    private final TimeService timeService;

    public SessionService(SessionRepository sessionRepository, TimeService timeService) {
        this.sessionRepository = sessionRepository;
        this.timeService = timeService;
    }

    /**
     * Recupera la sessione dal DB o ne crea una nuova se non esiste o è scaduta
     */
    public Session getOrCreate(String sessionId) {
        if (sessionId != null) {
            return sessionRepository.findById(sessionId)
                    .filter(s -> !isExpired(s))
                    .orElseGet(this::createAndSave);
        }
        return createAndSave();
    }

    /**
     * Crea nuova sessione e la salva nel DB
     */
    private Session createAndSave() {
        Session session = new Session();
        session.setId(UUID.randomUUID().toString());
        session.setCreatedAt(timeService.now());
        session.setExpiresAt(timeService.now().plus(SESSION_EXPIRATION_DAYS));
        session.setActive(true);
        return sessionRepository.save(session);
    }

    private boolean isExpired(Session session) {
        return timeService.now().isAfter(session.getExpiresAt()) || !session.isActive();
    }
}
