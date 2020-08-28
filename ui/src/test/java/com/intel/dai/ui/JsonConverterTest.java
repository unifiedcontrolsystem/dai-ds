package com.intel.dai.ui;

import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import org.junit.Before;
import org.junit.Test;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonConverterTest {

    @Before
    public void setUp() throws SQLException {
        resultSet = mock(ResultSet.class);
        metaData = mock(ResultSetMetaData.class);
        when(resultSet.getMetaData()).thenReturn(metaData);
    }

    @Test
    public void convertToJsonResultSetReturnsEmptyResultSet() throws SQLException {
        when(metaData.getColumnCount()).thenReturn(0);
        when(resultSet.next()).thenReturn(false);
        JsonConverter jsonConverter = new JsonConverter();
        PropertyMap jsonMap = jsonConverter.convertToJsonResultSet(resultSet);
        assertEquals(0L, jsonMap.get("result-data-lines"));
    }

    @Test
    public void convertToJsonResultSetReturnsFullResultSet() throws SQLException {
        when(resultSet.next()).thenReturn(true, false);

        when(metaData.getColumnCount()).thenReturn(10);

        when(metaData.getColumnName(1)).thenReturn("EntryNumber");
        when(metaData.getColumnType(1)).thenReturn(Types.BIGINT);
        when(resultSet.getLong(1)).thenReturn(123456L);

        when(metaData.getColumnName(2)).thenReturn("id");
        when(metaData.getColumnType(2)).thenReturn(Types.TINYINT);
        when(resultSet.getInt(2)).thenReturn(12);

        when(metaData.getColumnName(3)).thenReturn("AdapterName");
        when(metaData.getColumnType(3)).thenReturn(Types.VARCHAR);
        when(resultSet.getString(3)).thenReturn("Monitor");

        when(metaData.getColumnName(4)).thenReturn("SensorNumber");
        when(metaData.getColumnType(4)).thenReturn(Types.SMALLINT);
        when(resultSet.getInt(4)).thenReturn(34);

        when(metaData.getColumnName(5)).thenReturn("SensorID");
        when(metaData.getColumnType(5)).thenReturn(Types.INTEGER);
        when(resultSet.getInt(5)).thenReturn(1);

        when(metaData.getColumnName(6)).thenReturn("SensorValue1");
        when(metaData.getColumnType(6)).thenReturn(Types.FLOAT);
        when(resultSet.getFloat(6)).thenReturn(17.1f);

        when(metaData.getColumnName(7)).thenReturn("SensorValue2");
        when(metaData.getColumnType(7)).thenReturn(Types.DOUBLE);
        when(resultSet.getDouble(7)).thenReturn(25.1);

        when(metaData.getColumnName(8)).thenReturn("CurrentDate");
        when(metaData.getColumnType(8)).thenReturn(Types.DATE);
        when(resultSet.getDate(8)).thenReturn(java.sql.Date.valueOf("2019-06-19"));

        when(metaData.getColumnName(9)).thenReturn("CurrentTimestamp");
        when(metaData.getColumnType(9)).thenReturn(Types.TIMESTAMP);
        when(resultSet.getTimestamp("CurrentTimestamp")).thenReturn(java.sql.Timestamp.valueOf("2019-06-19 12:00:00"));
        when(resultSet.getTimestamp(9)).thenReturn(java.sql.Timestamp.valueOf("2019-06-19 12:00:00"));

        when(metaData.getColumnName(10)).thenReturn("ValidSensor");
        when(metaData.getColumnType(10)).thenReturn(Types.BOOLEAN);
        when(resultSet.getBoolean(10)).thenReturn(true);

        JsonConverter jsonConverter = new JsonConverter();
        PropertyMap jsonMap = jsonConverter.convertToJsonResultSet(resultSet);
        assertEquals(5, jsonMap.size());

    }

    private ResultSet resultSet;
    private ResultSetMetaData metaData;
}