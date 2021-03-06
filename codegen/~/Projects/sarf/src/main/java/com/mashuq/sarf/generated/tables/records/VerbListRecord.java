/*
 * This file is generated by jOOQ.
 */
package com.mashuq.sarf.generated.tables.records;


import com.mashuq.sarf.generated.tables.VerbList;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class VerbListRecord extends UpdatableRecordImpl<VerbListRecord> implements Record3<Integer, String, Byte> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>sarf.verb_list.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>sarf.verb_list.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>sarf.verb_list.verb</code>.
     */
    public void setVerb(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>sarf.verb_list.verb</code>.
     */
    public String getVerb() {
        return (String) get(1);
    }

    /**
     * Setter for <code>sarf.verb_list.processed</code>.
     */
    public void setProcessed(Byte value) {
        set(2, value);
    }

    /**
     * Getter for <code>sarf.verb_list.processed</code>.
     */
    public Byte getProcessed() {
        return (Byte) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, Byte> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<Integer, String, Byte> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return VerbList.VERB_LIST.ID;
    }

    @Override
    public Field<String> field2() {
        return VerbList.VERB_LIST.VERB;
    }

    @Override
    public Field<Byte> field3() {
        return VerbList.VERB_LIST.PROCESSED;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getVerb();
    }

    @Override
    public Byte component3() {
        return getProcessed();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getVerb();
    }

    @Override
    public Byte value3() {
        return getProcessed();
    }

    @Override
    public VerbListRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public VerbListRecord value2(String value) {
        setVerb(value);
        return this;
    }

    @Override
    public VerbListRecord value3(Byte value) {
        setProcessed(value);
        return this;
    }

    @Override
    public VerbListRecord values(Integer value1, String value2, Byte value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached VerbListRecord
     */
    public VerbListRecord() {
        super(VerbList.VERB_LIST);
    }

    /**
     * Create a detached, initialised VerbListRecord
     */
    public VerbListRecord(Integer id, String verb, Byte processed) {
        super(VerbList.VERB_LIST);

        setId(id);
        setVerb(verb);
        setProcessed(processed);
    }
}
