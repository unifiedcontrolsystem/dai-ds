package com.intel.dai.dsapi;

import lombok.ToString;

/**
 * Note that ID is consists of ParentID+Type+Ordrinal.  So, there is data
 * duplication.  However, if we were to go without an ID, then the
 * identification of an FRU by location becomes a path from the root.  This
 * design imposes a complexity penalty since the parent pointer would become
 * a tuple.  It is unclear how an arbitrarily length tuple can be reasonably
 * stored in a SQL DB.  Of course, if you were to encode the tuple into a
 * string, then you will end up pretty much with this design anyway.
 */
@ToString
public class HWInvSlot {
    public String ID;
    public String ParentID;
    public String Type;
    public int Ordinal;

    public String FRUID;
    public String FRUType;
    public String FRUSubType;
}
