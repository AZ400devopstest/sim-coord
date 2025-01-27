package net.neology.tolling.tzc.simulator.repository;

import net.neology.tolling.tzc.simulator.pojo.VehicleData;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class InMemoryVehicleDataRepository implements VehicleDataRepository {

    private final Queue<VehicleData> vehicleDataList = new ConcurrentLinkedQueue<>();

    @Override
    public List<VehicleData> findAll() {
        return vehicleDataList.stream().toList();
    }

    @Override
    public Optional<VehicleData> findById(int id) {
        // TODO: nothing that looks like a "transaction id" is currently present in the sample file
        // TODO: we could ID by timestamp, or generate an ID when saving a new record
        return Optional.empty();
    }

    @Override
    public VehicleData save(VehicleData vehicleData) {
        vehicleDataList.add(vehicleData);
        return vehicleData;
    }

    @Override
    public List<VehicleData> saveAll(List<VehicleData> vehicleData) {
        vehicleDataList.addAll(vehicleData);
        return vehicleData;
    }

    @Override
    public int currentSize() { return vehicleDataList.size(); }
}
