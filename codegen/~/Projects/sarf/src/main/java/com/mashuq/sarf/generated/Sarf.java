/*
 * This file is generated by jOOQ.
 */
package com.mashuq.sarf.generated;


import com.mashuq.sarf.generated.tables.VerbList;

import java.util.Arrays;
import java.util.List;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Sarf extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>sarf</code>
     */
    public static final Sarf SARF = new Sarf();

    /**
     * The table <code>sarf.verb_list</code>.
     */
    public final VerbList VERB_LIST = VerbList.VERB_LIST;

    /**
     * No further instances allowed
     */
    private Sarf() {
        super("sarf", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.<Table<?>>asList(
            VerbList.VERB_LIST);
    }
}