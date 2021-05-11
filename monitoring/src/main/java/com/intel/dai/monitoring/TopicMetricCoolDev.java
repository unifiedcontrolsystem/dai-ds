// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;

import java.util.List;

class TopicMetricCoolDev extends TopicBaseProcessor {
    TopicMetricCoolDev(Logger log, boolean doAggregation) { super(log, doAggregation); }

    @Override
    void processTopic(EnvelopeData data, PropertyMap map, List<CommonDataFormat> results) {
        processNumberKey("Inverter_01_Enable_IO", "", "STATE", data, map, results);
        processNumberKey("Inverter_02_Enable_IO", "", "STATE", data, map, results);
        processNumberKey("Inverter_Drive_1_Demand_IO", "%", "LOAD", data, map, results);
        processNumberKey("Inverter_Drive_2_Demand_IO", "%", "LOAD", data, map, results);
        processNumberKey("Control_Valve_IO", "%", "LOAD", data, map, results);
        processNumberKey("Control_Valve_Feedback_IO", "", "RATE", data, map, results);
        processNumberKey("Pump_Delta_Pressure", "", "PRESSURE_DELTA", data, map, results);
        processNumberKey("Secondary_Delta_Pressure", "", "PRESSURE_DELTA", data, map, results);
        processNumberKey("PS1_IT_Return_Pressure", "", "PRESSURE", data, map, results);
        processNumberKey("PS2_IT_HEX_Input_Pressure", "", "PRESSURE", data, map, results);
        processNumberKey("PS3_IT_HEX_Output_Pressure", "", "PRESSURE", data, map, results);
        processNumberKey("PS4_IT_Supply_Pressure", "", "PRESSURE", data, map, results);
        processNumberKey("PS5_Facility_Supply_Pressure", "", "PRESSURE", data, map, results);
        processNumberKey("Primary_Delta_Pressure", "", "PRESSURE_DELTA", data, map, results);
        processNumberKey("Secondary_Filter_Delta_Press", "", "PRESSURE_DELTA", data, map, results);
        processNumberKey("Primary_Flow_IO", "", "RATE", data, map, results);
        processNumberKey("Secondary_Flow_IO", "", "RATE", data, map, results);
        processNumberKey("T1_Facility_Supply_Temperature", "C", "TEMPERATURE", data, map, results);
        processNumberKey("T2_IT_Supply_Temperature", "C", "TEMPERATURE", data, map, results);
        processNumberKey("T3_CDU_Temperature", "C", "TEMPERATURE", data, map, results);
        processNumberKey("T4_IT_Return_Temperature", "C", "TEMPERATURE", data, map, results);
        processNumberKey("Relative_Humidity_Average", "%", "HUMIDITY", data, map, results);
        processNumberKey("Dew_Point_Average", "C", "TEMPERATURE", data, map, results);
        processNumberKey("ATS_A_Feed", "", "STATE", data, map, results);
        processNumberKey("ATS_B_Feed", "", "STATE", data, map, results);
        processNumberKey("Primary_Return_Temp_T5_IO", "C", "TEMPERATURE", data, map, results);
        processNumberKey("MCS_Supply_Air_Temp_Avg", "C", "TEMPERATURE", data, map, results);
        processNumberKey("MCS_Return_Air_Temp_Avg", "C", "TEMPERATURE", data, map, results);
        processNumberKey("MCS_Dew_Point_Average", "C", "TEMPERATURE", data, map, results);
        processNumberKeyWithFactor("MCS_kWatts_of_heat_removed", "W", "POWER", data, map, results, 1000.0);
        processNumberKey("MCS_Input_Water_Temperature", "C", "TEMPERATURE", data, map, results);
        processNumberKey("MCS_Output_Water_Temperature", "C", "TEMPERATURE", data, map, results);
        processNumberKey("MCS_Supply_Valve_Position", "%", "STATE", data, map, results);
        processNumberKey("MCS_Supply_Water_Pressure", "", "PRESSURE", data, map, results);
        processNumberKey("MCS_Return_Water_Pressure", "", "PRESSURE", data, map, results);
        processNumberKey("MCS_Output_Water_Flow", "", "RATE", data, map, results);
    }
}
