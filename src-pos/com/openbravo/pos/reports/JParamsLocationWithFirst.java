package com.openbravo.pos.reports;

import java.util.List;

/**
 *
 * @author adrian
 */
public class JParamsLocationWithFirst extends JParamsLocation {
    
    /** Creates a new instance of JParamsLocationWithFirst */
    public JParamsLocationWithFirst() {
        super();
    }
    
    protected void addFirst(List a) {
        a.add(0, null);
    }    
}
