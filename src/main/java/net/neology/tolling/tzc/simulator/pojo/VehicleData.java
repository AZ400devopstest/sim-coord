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
        String plateReadConfidence,
        String plateNationality,
        String platePlateFinderConfidence,
        String plateXpos,
        String plateWidth,
        String plateYpos,
        String plateHeight,
        String tagId,
        String tid,
        String tagType,
        String antenna,
        String rssi,
        String txPower,
        String userData
) {
        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append(facilityId).append(",")
                  .append(tollPoint).append(",")
                        .append(lane).append(",")
                        .append(vehicleId).append(",")
                        .append(vehicleClass).append(",")
                        .append(confidence).append(",")
                        .append(length).append(",")
                        .append(width).append(",")
                        .append(height).append(",")
                        .append(axleCount).append(",")
                        .append(speed).append(",")
                        .append(direction).append(",")
                        .append(plateText).append(",")
                        .append(plateReadConfidence).append(",")
                        .append(plateNationality).append(",")
                        .append(platePlateFinderConfidence).append(",")
                        .append(plateXpos).append(",")
                        .append(plateWidth).append(",")
                        .append(plateYpos).append(",")
                        .append(plateHeight).append(",")
                        .append(tagId).append(",")
                        .append(tid).append(",")
                        .append(tagType).append(",")
                        .append(antenna).append(",")
                        .append(rssi).append(",")
                        .append(txPower).append(",")
                        .append(userData);
                return sb.toString();
        }
}