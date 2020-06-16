package com.intel.dai.fabric

import com.intel.logging.Logger
import spock.lang.Specification

class AccumulatorSpec extends Specification {
    def underTest_
    void setup() {
        underTest_ = new Accumulator(Mock(Logger))
        Accumulator.moving_ = false
        Accumulator.useTime_ = false
    }

    def "Test AddValue Count Window"() {
        Accumulator.count_ = 3
        underTest_.addValue(new FabricTelemetryItem(0L, "test", "location", 1.0))
        underTest_.addValue(new FabricTelemetryItem(0L, "test", "location", 2.0))
        FabricTelemetryItem result = new FabricTelemetryItem(0L, "test", "location", 3.0)
        underTest_.addValue(result)
        expect: result.getAverage() == 2.0
        and:    result.getMinimum() == 1.0
        and:    result.getMaximum() == 3.0
    }

    def "Test AddValue Time Window"() {
        Accumulator.useTime_ = true
        Accumulator.us_ = 2_000_000
        underTest_.addValue(new FabricTelemetryItem(0L, "test", "location", 1.0))
        underTest_.addValue(new FabricTelemetryItem(1_000_000L, "test", "location", 2.0))
        FabricTelemetryItem result = new FabricTelemetryItem(2_000_000L, "test", "location", 3.0)
        underTest_.addValue(result)
        expect: result.getAverage() == 2.0
        and:    result.getMinimum() == 1.0
        and:    result.getMaximum() == 3.0
    }

    def "Test AddValue Moving Count Window"() {
        Accumulator.count_ = 3
        Accumulator.moving_ = true;
        underTest_.addValue(new FabricTelemetryItem(0L, "test", "location", 1.0))
        underTest_.addValue(new FabricTelemetryItem(0L, "test", "location", 2.0))
        FabricTelemetryItem result = new FabricTelemetryItem(0L, "test", "location", 3.0)
        underTest_.addValue(result)
        expect: result.getAverage() == 2.0
        and:    result.getMinimum() == 1.0
        and:    result.getMaximum() == 3.0
    }
}
