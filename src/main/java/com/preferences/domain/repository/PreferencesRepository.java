package com.preferences.domain.repository;

import com.preferences.domain.model.Preferences;
import java.util.Optional;

public interface PreferencesRepository {
    Optional<Preferences> findById(String memberId);
    Preferences save(Preferences preferences);
    void delete(String memberId);
    boolean existsById(String memberId);
}
