// Copyright (C) 2018-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;

import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class JsonConverter {

    public PropertyMap convertToJsonResultSet(ResultSet resultsetinp) throws SQLException
    {
        PropertyMap topLevel = new PropertyMap();
        ResultSetMetaData rasmetadata = resultsetinp.getMetaData();
        int numofcolumns = rasmetadata.getColumnCount();
        PropertyArray allRowsData = new PropertyArray();
        long numOfRows = 0;
        while(resultsetinp.next()) {
            numOfRows++;
            PropertyArray rowData = new PropertyArray();
            for (int i=1; i<numofcolumns+1; i++) {
                String column_name = rasmetadata.getColumnName(i);
                if(rasmetadata.getColumnType(i)== Types.ARRAY){
                    rowData.add( resultsetinp.getArray(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.BIGINT){
                    rowData.add( resultsetinp.getLong(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.TINYINT){
                    rowData.add( resultsetinp.getInt(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.SMALLINT){
                    rowData.add( resultsetinp.getInt(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.DOUBLE){
                    BigDecimal bd = new BigDecimal(Double.toString(resultsetinp.getDouble(i)));
                    bd = bd.setScale(4, RoundingMode.HALF_UP);
                    rowData.add( bd.toPlainString());
                }
                else if(rasmetadata.getColumnType(i)== Types.FLOAT){
                    BigDecimal bd = new BigDecimal(Float.toString(resultsetinp.getFloat(i)));
                    bd = bd.setScale(4, RoundingMode.HALF_UP);
                    rowData.add( bd.toPlainString());
                }
                else if(rasmetadata.getColumnType(i)== Types.INTEGER){
                    rowData.add( resultsetinp.getInt(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.NVARCHAR){
                    rowData.add( resultsetinp.getNString(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.VARCHAR){
                    if(rasmetadata.getColumnName(i).compareToIgnoreCase("manifestcontent") == 0) {
                        rowData.add( resultsetinp.getString(i).toString());
                    }
                    else
                        rowData.add( resultsetinp.getString(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.DATE){
                    rowData.add( resultsetinp.getDate(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.TIMESTAMP){
                    if (resultsetinp.getTimestamp(column_name) == null)
                        rowData.add( null);
                    else
                        rowData.add( resultsetinp.getTimestamp(i).toString());

                }
                else if(rasmetadata.getColumnType(i)== Types.BOOLEAN){
                    rowData.add( resultsetinp.getBoolean(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.BLOB){
                    rowData.add( resultsetinp.getBlob(i));
                }
                else{
                    rowData.add( resultsetinp.getObject(i));
                }
            }
            allRowsData.add(rowData);
        }
        topLevel.put("result-data-columns" ,numofcolumns);
        topLevel.put("result-data-lines" ,numOfRows);
        topLevel.put("result-status-code" ,0);
        topLevel.put("schema", extractSchemaFromResultSet(rasmetadata));
        topLevel.put("data" ,allRowsData);
        return topLevel;
    }

    private PropertyArray extractSchemaFromResultSet(ResultSetMetaData rasMetaData) throws SQLException {

        PropertyArray schema = new PropertyArray();
        int numOfColumns = rasMetaData.getColumnCount();
        for (int i=1; i<numOfColumns+1; i++) {
            String columnName = rasMetaData.getColumnName(i);
            PropertyMap columnInfo = new PropertyMap();
            columnInfo.put("data", columnName);
            columnInfo.put("unit", "string");
            columnInfo.put("heading", columnName);
            schema.add(columnInfo);
        }
        return schema;
    }
}
