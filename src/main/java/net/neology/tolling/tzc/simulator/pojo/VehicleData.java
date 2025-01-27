package net.neology.tolling.tzc.simulator.pojo;

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
        String plateReadConfidence, // why is this named 'plate_readconfidence' in the sample file?
        String plateNationality, // why is this named 'plate_nationality' in the sample file?
        String platePlateFinderConfidence, // why is this named 'plate_platefinderconfidence' in the sample file, and what does that mean?
        String plateXpos,
        String plateWidth,
        String plateYpos,
        String plateHeight,
        String tagId, // what is the format of this data?
        String tid, // what is this?  and what is the format?
        String tagType,
        String antenna, // what is this?
        String rssi, // what is this?
        String txPower, // what is this?
        String userData // what is this?
        ) {}