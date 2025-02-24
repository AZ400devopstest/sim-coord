package net.neology.tolling.tzc.simulator.pojo;

//@JsonIgnoreProperties("timeDelay")
public record VehicleData (
        String timeDelay,
        String facilityId,
        String tollPoint,
        String lane,
        String vehicleId,
        String vehicleClass,
        String confidence, // what confidence?
        String length,
        String width,
        String height,
        String axleCount,
        String speed,
        String direction,
        String plateText,
        String plateReadConfidence,
        String plateNationality,
        String platePlateFinderConfidence,
        String plateXpos,
        String plateWidth,
        String plateYpos,
        String plateHeight,
        String tagId,
        String tid,
        String type,
        String antenna,
        String rssi,
        String txPower,
        String userData
) {}