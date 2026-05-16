package com.materials.quoting.service;

import com.materials.quoting.model.CarBatteryBreakdown;
import com.materials.quoting.model.CarSearch;
import com.materials.quoting.repository.CarBatteryBreakdownRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CarBatteryBreakdownServiceImpl implements CarBatteryBreakdownService {

    private final CarBatteryBreakdownRepository repository;

    @Override
    public CarBatteryBreakdown create(CarBatteryBreakdown entry) {
        entry.setId(null);
        return repository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarBatteryBreakdown> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarBatteryBreakdown> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarBatteryBreakdown> findBySearch(CarSearch search) {
        return repository.findBySearch(
                search.make(),
                search.model(),
                search.trim(),
                search.year());
    }

    @Override
    public CarBatteryBreakdown update(Long id, CarBatteryBreakdown updated) {
        CarBatteryBreakdown existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("CarBatteryBreakdown not found with id: " + id));

        existing.setMake(updated.getMake());
        existing.setModel(updated.getModel());
        existing.setTrim(updated.getTrim());
        existing.setPropulsionType(updated.getPropulsionType());
        existing.setBatteryCapacityKwh(updated.getBatteryCapacityKwh());
        existing.setModelYearStart(updated.getModelYearStart());
        existing.setModelYearEnd(updated.getModelYearEnd());
        existing.setPackWeightKg(updated.getPackWeightKg());
        existing.setNiWeightKg(updated.getNiWeightKg());
        existing.setCoWeightKg(updated.getCoWeightKg());
        existing.setLiWeightKg(updated.getLiWeightKg());
        existing.setCuWeightKg(updated.getCuWeightKg());
        existing.setAlWeightKg(updated.getAlWeightKg());

        return repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        CarBatteryBreakdown entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("CarBatteryBreakdown not found with id: " + id));
        repository.delete(entity);
    }
}
