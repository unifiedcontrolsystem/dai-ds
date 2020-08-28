// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;

import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

public class JsonConverterGUI {

    public PropertyArray convertToJsonResultSet(ResultSet resultsetinp) throws SQLException
    {
        PropertyArray jsonarray = new PropertyArray();
        ResultSetMetaData rasmetadata = resultsetinp.getMetaData();
        int numofcolumns = rasmetadata.getColumnCount();
        while(resultsetinp.next()) {
            PropertyMap obj_json = new PropertyMap();
            for (int i=1; i<numofcolumns+1; i++) {
                String column_name = rasmetadata.getColumnName(i);
                if(rasmetadata.getColumnType(i)== Types.ARRAY){
                    obj_json.put(column_name, resultsetinp.getArray(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.BIGINT){
                    obj_json.put(column_name, resultsetinp.getLong(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.TINYINT){
                    obj_json.put(column_name, resultsetinp.getInt(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.SMALLINT){
                    obj_json.put(column_name, resultsetinp.getInt(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.DOUBLE){
                    obj_json.put(column_name, resultsetinp.getDouble(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.FLOAT){
                    obj_json.put(column_name, resultsetinp.getFloat(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.INTEGER){
                    obj_json.put(column_name, resultsetinp.getInt(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.NVARCHAR){
                    obj_json.put(column_name, resultsetinp.getNString(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.VARCHAR){
                    if(rasmetadata.getColumnName(i).compareToIgnoreCase("manifestcontent") == 0) {
                        obj_json.put(column_name, resultsetinp.getString(i).toString());
                    }
                    else
                        obj_json.put(column_name, resultsetinp.getString(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.DATE){
                    obj_json.put(column_name, resultsetinp.getDate(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.TIMESTAMP){
                    if (resultsetinp.getTimestamp(column_name) == null)
                        obj_json.put(column_name, null);
                    else
                        obj_json.put(column_name, resultsetinp.getTimestamp(i).toString());

                }
                else if(rasmetadata.getColumnType(i)== Types.BOOLEAN){
                    obj_json.put(column_name, resultsetinp.getBoolean(i));
                }
                else if(rasmetadata.getColumnType(i)== Types.BLOB){
                    obj_json.put(column_name, resultsetinp.getBlob(i));
                }
                else{
                    obj_json.put(column_name, resultsetinp.getObject(i));
                }
            }
            jsonarray.add(obj_json);
        }
        return jsonarray;
    }
}
