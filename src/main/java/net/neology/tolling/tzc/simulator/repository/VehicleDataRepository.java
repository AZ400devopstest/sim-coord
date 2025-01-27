package net.neology.tolling.tzc.simulator.repository;

import net.neology.tolling.tzc.simulator.pojo.VehicleData;

import java.util.List;
import java.util.Optional;

public interface VehicleDataRepository {

    List<VehicleData> findAll();

    Optional<VehicleData> findById(int id);

    VehicleData save(VehicleData vehicleData);

    List<VehicleData> saveAll(List<VehicleData> vehicleData);

    int currentSize();
}
