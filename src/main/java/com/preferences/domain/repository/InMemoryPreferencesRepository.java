package com.preferences.domain.repository;

import com.preferences.domain.model.Preferences;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPreferencesRepository implements PreferencesRepository {
    
    private final ConcurrentHashMap<String, Preferences> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Preferences> findById(String memberId) {
        return Optional.ofNullable(store.get(memberId));
    }

    @Override
    public Preferences save(Preferences preferences) {
        store.put(preferences.getMemberId(), preferences);
        return preferences;
    }

    @Override
    public void delete(String memberId) {
        store.remove(memberId);
    }

    @Override
    public boolean existsById(String memberId) {
        return store.containsKey(memberId);
    }
}
