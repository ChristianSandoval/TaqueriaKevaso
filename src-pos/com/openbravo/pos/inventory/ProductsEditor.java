package com.openbravo.pos.inventory;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.awt.image.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.format.Formats;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.data.user.DirtyManager;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.panels.JProductFinder;
import com.openbravo.pos.sales.TaxesLogic;
import com.openbravo.pos.ticket.ProductInfoExt;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author adrianromero
 */
public class ProductsEditor extends JPanel implements EditorRecord {
       
    private SentenceList m_sentcat;
    private ComboBoxValModel m_CategoryModel;

    private SentenceList taxcatsent;
    private ComboBoxValModel taxcatmodel;  

    private SentenceList attsent;
    private ComboBoxValModel attmodel;
    private ProductInfoExt prod1;
    private ProductInfoExt prod2;
    private ProductInfoExt prod3;
    private ProductInfoExt prod4;
    private ProductInfoExt prod5;
    private ProductInfoExt prod6;
    private ProductInfoExt prod7;
    private ProductInfoExt prod8;
    private ProductInfoExt prod9;
    private SentenceList taxsent;
    private TaxesLogic taxeslogic;
    
    private ComboBoxValModel m_CodetypeModel;
    
    private Object m_id;
    private Object pricesell;
    private boolean priceselllock = false;
    
    private boolean reportlock = false;
    private DataLogicSales dlSales;
    
    /** Creates new form JEditProduct */
    public ProductsEditor(DataLogicSales dlSales, DirtyManager dirty) {
        initComponents();
        this.dlSales = dlSales;
        // The taxes sentence
        taxsent = dlSales.getTaxList();
             
        // The categories model
        m_sentcat = dlSales.getCategoriesList();
        m_CategoryModel = new ComboBoxValModel();
        
        // The taxes model
        taxcatsent = dlSales.getTaxCategoriesList();
        taxcatmodel = new ComboBoxValModel();

        // The attributes model
        attsent = dlSales.getAttributeSetList();
        attmodel = new ComboBoxValModel();
        
        m_CodetypeModel = new ComboBoxValModel();
        m_CodetypeModel.add(null);
        m_CodetypeModel.add(CodeType.EAN13);
        m_CodetypeModel.add(CodeType.CODE128);
        m_jCodetype.setModel(m_CodetypeModel);
        m_jCodetype.setVisible(false);
               
        m_jRef.getDocument().addDocumentListener(dirty);
        m_jCode.getDocument().addDocumentListener(dirty);
        m_jName.getDocument().addDocumentListener(dirty);
        m_jComment.addActionListener(dirty);
        m_jScale.addActionListener(dirty);
        m_jCategory.addActionListener(dirty);
        m_jTax.addActionListener(dirty);
        m_jAtt.addActionListener(dirty);
        m_jPriceBuy.getDocument().addDocumentListener(dirty);
        m_jPriceSell.getDocument().addDocumentListener(dirty);
        m_jImage.addPropertyChangeListener("image", dirty);
        m_jstockcost.getDocument().addDocumentListener(dirty);
        m_jstockvolume.getDocument().addDocumentListener(dirty);
        m_jInCatalog.addActionListener(dirty);
        m_jCatalogOrder1.getDocument().addDocumentListener(dirty);
        txtAttributes.getDocument().addDocumentListener(dirty);
this.m_jCant1.getDocument().addDocumentListener(dirty);
    this.m_jCant2.getDocument().addDocumentListener(dirty);
    this.m_jCant3.getDocument().addDocumentListener(dirty);
    this.m_jCant4.getDocument().addDocumentListener(dirty);
    this.m_jCant5.getDocument().addDocumentListener(dirty);
    this.m_jCant6.getDocument().addDocumentListener(dirty);
    this.m_jCant7.getDocument().addDocumentListener(dirty);
    this.m_jCant8.getDocument().addDocumentListener(dirty);
    this.m_jCant9.getDocument().addDocumentListener(dirty);
    
        FieldsManager fm = new FieldsManager();
        m_jPriceBuy.getDocument().addDocumentListener(fm);
        m_jPriceSell.getDocument().addDocumentListener(new PriceSellManager());
        m_jTax.addActionListener(fm);
        
        m_jPriceSellTax.getDocument().addDocumentListener(new PriceTaxManager());
        m_jmargin.getDocument().addDocumentListener(new MarginManager());
        
        writeValueEOF();
    }
    
    public void activate() throws BasicException {
        
        // Load the taxes logic
        taxeslogic = new TaxesLogic(taxsent.list());        
        
        m_CategoryModel = new ComboBoxValModel(m_sentcat.list());
        m_jCategory.setModel(m_CategoryModel);

        taxcatmodel = new ComboBoxValModel(taxcatsent.list());
        m_jTax.setModel(taxcatmodel);

        attmodel = new ComboBoxValModel(attsent.list());
        attmodel.add(0, null);
        m_jAtt.setModel(attmodel);
    }
    
    public void refresh() {
    }    
    
    public void writeValueEOF() {
        
        reportlock = true;
        // Los valores
        m_jTitle.setText(AppLocal.getIntString("label.recordeof"));
        m_id = null;
        m_jRef.setText(null);
        m_jCode.setText(null);
        m_jName.setText(null);
        m_jComment.setSelected(false);
        m_jScale.setSelected(false);
        m_CategoryModel.setSelectedKey(null);
        taxcatmodel.setSelectedKey(null);
        attmodel.setSelectedKey(null);
        m_jPriceBuy.setText(null);
        setPriceSell(null);         
        m_jImage.setImage(null);
        m_jstockcost.setText(null);
        m_jstockvolume.setText(null);
        m_jInCatalog.setSelected(false);
        m_jCatalogOrder1.setText(null);
        txtAttributes.setText(null);
        reportlock = false;
        
        // Los habilitados
        m_jRef.setEnabled(false);
        m_jCode.setEnabled(false);
        m_jName.setEnabled(false);
        m_jComment.setEnabled(false);
        m_jScale.setEnabled(false);
        m_jCategory.setEnabled(false);
        m_jTax.setEnabled(false);
        m_jAtt.setEnabled(false);
        m_jPriceBuy.setEnabled(false);
        m_jPriceSell.setEnabled(false);
        m_jPriceSellTax.setEnabled(false);
        m_jmargin.setEnabled(false);
        m_jImage.setEnabled(false);
        m_jstockcost.setEnabled(false);
        m_jstockvolume.setEnabled(false);
        m_jInCatalog.setEnabled(false);
        m_jCatalogOrder1.setEnabled(false);
        txtAttributes.setEnabled(false);
        
        this.m_jCant1.setText(null);
        m_jCant1.setEnabled(false);
        this.m_jCant2.setText(null);
        m_jCant2.setEnabled(false);
        this.m_jCant3.setText(null);
        m_jCant3.setEnabled(false);
        
        m_jProd1.setText(null);
        m_jProd2.setText(null);
        m_jProd3.setText(null);
        m_jRef1.setText(null);
        m_jRef2.setText(null);
        m_jRef3.setText(null);
        
        this.m_jCant4.setText(null);
        m_jCant4.setEnabled(false);
        this.m_jCant5.setText(null);
        m_jCant5.setEnabled(false);
        this.m_jCant6.setText(null);
        m_jCant6.setEnabled(false);
        
        m_jProd4.setText(null);
        m_jProd5.setText(null);
        m_jProd6.setText(null);
        m_jRef4.setText(null);
        m_jRef5.setText(null);
        m_jRef6.setText(null);
        
        this.m_jCant7.setText(null);
        m_jCant7.setEnabled(false);
        this.m_jCant8.setText(null);
        m_jCant8.setEnabled(false);
        this.m_jCant9.setText(null);
        m_jCant9.setEnabled(false);
        
        m_jProd7.setText(null);
        m_jProd8.setText(null);
        m_jProd9.setText(null);
        m_jRef7.setText(null);
        m_jRef8.setText(null);
        m_jRef9.setText(null);
        
        calculateMargin();
        calculatePriceSellTax();
    }
    public void writeValueInsert() {
       
        reportlock = true;
        // Los valores
        m_jTitle.setText(AppLocal.getIntString("label.recordnew"));
        m_id = UUID.randomUUID().toString();
        m_jRef.setText(null);
        m_jCode.setText(null);
        m_jName.setText(null);
        m_jComment.setSelected(false);
        m_jScale.setSelected(false);
        m_CategoryModel.setSelectedKey(null);
        taxcatmodel.setSelectedKey(null);
        attmodel.setSelectedKey(null);
        m_jPriceBuy.setText(null);
        setPriceSell(null);                     
        m_jImage.setImage(null);
        m_jstockcost.setText(null);
        m_jstockvolume.setText(null);
        m_jInCatalog.setSelected(true);
        m_jCatalogOrder1.setText(null);
        txtAttributes.setText(null);
        reportlock = false;
        
        // Los habilitados
        m_jRef.setEnabled(true);
        m_jCode.setEnabled(true);
        m_jName.setEnabled(true);
        m_jComment.setEnabled(true);
        m_jScale.setEnabled(true);
        m_jCategory.setEnabled(true);
        m_jTax.setEnabled(true);
        m_jAtt.setEnabled(true);
        m_jPriceBuy.setEnabled(true);
        m_jPriceSell.setEnabled(true); 
        m_jPriceSellTax.setEnabled(true);
        m_jmargin.setEnabled(true);
        m_jImage.setEnabled(true);
        m_jstockcost.setEnabled(true);
        m_jstockvolume.setEnabled(true);
        m_jInCatalog.setEnabled(true); 
        m_jCatalogOrder1.setEnabled(false);
        txtAttributes.setEnabled(true);
        
        this.m_jCant1.setText(null);
        m_jCant1.setEnabled(true);
        this.m_jCant2.setText(null);
        m_jCant2.setEnabled(true);
        this.m_jCant2.setText(null);
        m_jCant2.setEnabled(true);
        
        m_jProd1.setText(null);
        m_jProd2.setText(null);
        m_jProd3.setText(null);
        m_jRef1.setText(null);
        m_jRef2.setText(null);
        m_jRef3.setText(null);
        
        this.m_jCant4.setText(null);
        m_jCant4.setEnabled(true);
        this.m_jCant5.setText(null);
        m_jCant5.setEnabled(true);
        this.m_jCant6.setText(null);
        m_jCant6.setEnabled(true);
        
        m_jProd4.setText(null);
        m_jProd5.setText(null);
        m_jProd6.setText(null);
        m_jRef4.setText(null);
        m_jRef5.setText(null);
        m_jRef6.setText(null);
        
        
        
        this.m_jCant7.setText(null);
        m_jCant7.setEnabled(true);
        this.m_jCant8.setText(null);
        m_jCant8.setEnabled(true);
        this.m_jCant9.setText(null);
        m_jCant9.setEnabled(true);
        
        m_jProd7.setText(null);
        m_jProd8.setText(null);
        m_jProd9.setText(null);
        m_jRef7.setText(null);
        m_jRef8.setText(null);
        m_jRef9.setText(null);
        calculateMargin();
        calculatePriceSellTax();
   }
    public void writeValueDelete(Object value) {
        
        reportlock = true;       
        Object[] myprod = (Object[]) value;
        m_jTitle.setText(Formats.STRING.formatValue(myprod[1]) + " - " + Formats.STRING.formatValue(myprod[3]) + " " + AppLocal.getIntString("label.recorddeleted"));
        m_id = myprod[0];
        m_jRef.setText(Formats.STRING.formatValue(myprod[1]));
        m_jCode.setText(Formats.STRING.formatValue(myprod[1]));
        m_jName.setText(Formats.STRING.formatValue(myprod[3]));
        m_jComment.setSelected(((Boolean)myprod[4]).booleanValue());
        m_jScale.setSelected(((Boolean)myprod[5]).booleanValue());
        m_jPriceBuy.setText(Formats.CURRENCY.formatValue(myprod[6]));
        setPriceSell(myprod[7]);                    
        m_CategoryModel.setSelectedKey(myprod[8]);
        taxcatmodel.setSelectedKey(myprod[9]);
        attmodel.setSelectedKey(myprod[10]);
        m_jImage.setImage((BufferedImage) myprod[11]);
        m_jstockcost.setText(Formats.CURRENCY.formatValue(myprod[12]));
        m_jstockvolume.setText(Formats.DOUBLE.formatValue(myprod[13]));
        m_jInCatalog.setSelected(((Boolean)myprod[14]).booleanValue());
        m_jCatalogOrder.setText(Formats.INT.formatValue(myprod[15]));
        txtAttributes.setText(Formats.BYTEA.formatValue(myprod[16]));
        txtAttributes.setCaretPosition(0);
        reportlock = false;
        
        // Los habilitados
        m_jRef.setEnabled(false);
        m_jCode.setEnabled(false);
        m_jName.setEnabled(false);
        m_jComment.setEnabled(false);
        m_jScale.setEnabled(false);
        m_jCategory.setEnabled(false);
        m_jTax.setEnabled(false);
        m_jAtt.setEnabled(false);
        m_jPriceBuy.setEnabled(false);
        m_jPriceSell.setEnabled(false);
        m_jPriceSellTax.setEnabled(false);
        m_jmargin.setEnabled(false);
        m_jImage.setEnabled(false);
        m_jstockcost.setEnabled(false);
        m_jstockvolume.setEnabled(false);
        m_jInCatalog.setEnabled(false);
        m_jCatalogOrder.setEnabled(false);
        txtAttributes.setEnabled(false);
        
        this.m_jCant1.setText(null);
        m_jCant1.setEnabled(false);
        this.m_jCant2.setText(null);
        m_jCant2.setEnabled(false);
        this.m_jCant2.setText(null);
        m_jCant2.setEnabled(false);
        
        m_jProd1.setText(null);
        m_jProd2.setText(null);
        m_jProd3.setText(null);
        m_jRef1.setText(null);
        m_jRef2.setText(null);
        m_jRef3.setText(null);
        
        this.m_jCant4.setText(null);
        m_jCant4.setEnabled(false);
        this.m_jCant5.setText(null);
        m_jCant5.setEnabled(false);
        this.m_jCant6.setText(null);
        m_jCant6.setEnabled(false);
        
        m_jProd4.setText(null);
        m_jProd5.setText(null);
        m_jProd6.setText(null);
        m_jRef4.setText(null);
        m_jRef5.setText(null);
        m_jRef6.setText(null);
        
        this.m_jCant7.setText(null);
        m_jCant7.setEnabled(false);
        this.m_jCant8.setText(null);
        m_jCant8.setEnabled(false);
        this.m_jCant9.setText(null);
        m_jCant9.setEnabled(false);
        
        m_jProd7.setText(null);
        m_jProd8.setText(null);
        m_jProd9.setText(null);
        m_jRef7.setText(null);
        m_jRef8.setText(null);
        m_jRef9.setText(null);
        
        calculateMargin();
        calculatePriceSellTax();
    }    
    
    public void writeValueEdit(Object value) {
        
        reportlock = true;
        Object[] myprod = (Object[]) value;
        m_jTitle.setText(Formats.STRING.formatValue(myprod[1]) + " - " + Formats.STRING.formatValue(myprod[3]));
        m_id = myprod[0];
        m_jRef.setText(Formats.STRING.formatValue(myprod[1]));
        m_jCode.setText(Formats.STRING.formatValue(myprod[1]));
        m_jName.setText(Formats.STRING.formatValue(myprod[3]));
        m_jComment.setSelected(((Boolean)myprod[4]).booleanValue());
        m_jScale.setSelected(((Boolean)myprod[5]).booleanValue());
        m_jPriceBuy.setText(Formats.CURRENCY.formatValue(myprod[6]));
        setPriceSell(myprod[7]);                               
        m_CategoryModel.setSelectedKey(myprod[8]);
        taxcatmodel.setSelectedKey(myprod[9]);
        attmodel.setSelectedKey(myprod[10]);
        m_jImage.setImage((BufferedImage) myprod[11]);
        m_jstockcost.setText(Formats.CURRENCY.formatValue(myprod[12]));
        m_jstockvolume.setText(Formats.DOUBLE.formatValue(myprod[13]));
        m_jInCatalog.setSelected(((Boolean)myprod[14]).booleanValue());
        m_jCatalogOrder.setText(Formats.INT.formatValue(myprod[15]));
   //     txtAttributes.setText(Formats.BYTEA.formatValue(myprod[16]));
        txtAttributes.setCaretPosition(0);
        reportlock = false;
        
        // Los habilitados
        m_jRef.setEnabled(true);
        m_jCode.setEnabled(true);
        m_jName.setEnabled(true);
        m_jComment.setEnabled(true);
        m_jScale.setEnabled(true);
        m_jCategory.setEnabled(true);
        m_jTax.setEnabled(true);
        m_jAtt.setEnabled(true);
        m_jPriceBuy.setEnabled(true);
        m_jPriceSell.setEnabled(true); 
        m_jPriceSellTax.setEnabled(true);
        m_jmargin.setEnabled(true);
        m_jImage.setEnabled(true);
        m_jstockcost.setEnabled(true);
        m_jstockvolume.setEnabled(true);
        m_jInCatalog.setEnabled(true);
        m_jCatalogOrder1.setEnabled(m_jInCatalog.isSelected());  
        txtAttributes.setEnabled(true);
        
        this.m_jCant1.setText(null);
        m_jCant1.setEnabled(true);
        this.m_jCant2.setText(null);
        m_jCant2.setEnabled(true);
        this.m_jCant3.setText(null);
        m_jCant3.setEnabled(true);
        
        m_jProd1.setText(null);
        m_jProd2.setText(null);
        m_jProd3.setText(null);
        m_jRef1.setText(null);
        m_jRef2.setText(null);
        m_jRef3.setText(null);
        
        this.m_jCant4.setText(null);
        m_jCant4.setEnabled(true);
        this.m_jCant5.setText(null);
        m_jCant5.setEnabled(true);
        this.m_jCant6.setText(null);
        m_jCant6.setEnabled(true);
        
        m_jProd4.setText(null);
        m_jProd5.setText(null);
        m_jProd6.setText(null);
        m_jRef4.setText(null);
        m_jRef5.setText(null);
        m_jRef6.setText(null);
        
        this.m_jCant7.setText(null);
        m_jCant7.setEnabled(true);
        this.m_jCant8.setText(null);
        m_jCant8.setEnabled(true);
        this.m_jCant9.setText(null);
        m_jCant9.setEnabled(true);
        
        m_jProd7.setText(null);
        m_jProd8.setText(null);
        m_jProd9.setText(null);
        m_jRef7.setText(null);
        m_jRef8.setText(null);
        m_jRef9.setText(null);
        try
    {
      ProductInfoExt prod = this.dlSales.getProductInfoByReference(this.m_jRef.getText());
      Properties props = prod.getProperties();
      Enumeration keys = props.keys();
      Enumeration elements = props.elements();
      int i = 0;
      while (keys.hasMoreElements())
      {
        String key = (String)keys.nextElement();
        String element = (String)elements.nextElement();
        ProductInfoExt prodTemp = this.dlSales.getProductInfo(key);
        switch (i)
        {
        case 0: 
          this.m_jRef1.setText(prodTemp.getReference());
          this.m_jProd1.setText(prodTemp.getName());
          this.m_jCant1.setText(element);
          this.m_jCant1.setEditable(true);
          this.m_jCant1.setEnabled(true);
          this.m_jCant1.requestFocus();
          this.prod1 = prodTemp;
          break;
        case 1: 
          this.m_jRef2.setText(prodTemp.getReference());
          this.m_jProd2.setText(prodTemp.getName());
          this.m_jCant2.setText(element);
          this.m_jCant2.setEditable(true);
          this.m_jCant2.setEnabled(true);
          this.m_jCant2.requestFocus();
          this.prod2 = prodTemp;
          break;
        case 2: 
          this.m_jRef3.setText(prodTemp.getReference());
          this.m_jProd3.setText(prodTemp.getName());
          this.m_jCant3.setText(element);
          this.m_jCant3.setEditable(true);
          this.m_jCant3.setEnabled(true);
          this.m_jCant3.requestFocus();
          this.prod3 = prodTemp;
            break;
            case 3: 
          this.m_jRef4.setText(prodTemp.getReference());
          this.m_jProd4.setText(prodTemp.getName());
          this.m_jCant4.setText(element);
          this.m_jCant4.setEditable(true);
          this.m_jCant4.setEnabled(true);
          this.m_jCant4.requestFocus();
          this.prod4 = prodTemp;
                break;
                case 4: 
          this.m_jRef5.setText(prodTemp.getReference());
          this.m_jProd5.setText(prodTemp.getName());
          this.m_jCant5.setText(element);
          this.m_jCant5.setEditable(true);
          this.m_jCant5.setEnabled(true);
          this.m_jCant5.requestFocus();
          this.prod5 = prodTemp;
                break;
                    case 5: 
          this.m_jRef6.setText(prodTemp.getReference());
          this.m_jProd6.setText(prodTemp.getName());
          this.m_jCant6.setText(element);
          this.m_jCant6.setEditable(true);
          this.m_jCant6.setEnabled(true);
          this.m_jCant6.requestFocus();
          this.prod6 = prodTemp;
                break;
                case 6: 
          this.m_jRef7.setText(prodTemp.getReference());
          this.m_jProd7.setText(prodTemp.getName());
          this.m_jCant7.setText(element);
          this.m_jCant7.setEditable(true);
          this.m_jCant7.setEnabled(true);
          this.m_jCant7.requestFocus();
          this.prod7 = prodTemp;
                break;
                case 7: 
          this.m_jRef8.setText(prodTemp.getReference());
          this.m_jProd8.setText(prodTemp.getName());
          this.m_jCant8.setText(element);
          this.m_jCant8.setEditable(true);
          this.m_jCant8.setEnabled(true);
          this.m_jCant8.requestFocus();
          this.prod8 = prodTemp;
                break;
                case 8: 
          this.m_jRef9.setText(prodTemp.getReference());
          this.m_jProd9.setText(prodTemp.getName());
          this.m_jCant9.setText(element);
          this.m_jCant9.setEditable(true);
          this.m_jCant9.setEnabled(true);
          this.m_jCant9.requestFocus();
          this.prod9 = prodTemp;
                break;
        }
        i++;
      }
    }catch (BasicException ex) {
            Logger.getLogger(ProductsEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        calculateMargin();
        calculatePriceSellTax();
    }   

    public Object createValue() throws BasicException {
        
        Object[] myprod = new Object[17];
        myprod[0] = m_id;
        myprod[1] = m_jRef.getText();
        myprod[2] = m_jRef.getText();
        myprod[3] = m_jName.getText();
        myprod[4] = Boolean.valueOf(m_jComment.isSelected());
        myprod[5] = Boolean.valueOf(m_jScale.isSelected());
        myprod[6] = Formats.CURRENCY.parseValue(m_jPriceBuy.getText());
        myprod[7] = pricesell;
        myprod[8] = m_CategoryModel.getSelectedKey();
        myprod[9] = "000";//taxcatmodel.getSelectedKey();
        myprod[10] = attmodel.getSelectedKey();
        myprod[11] = m_jImage.getImage();
        myprod[12] = Formats.CURRENCY.parseValue(m_jstockcost.getText());
        myprod[13] = Formats.DOUBLE.parseValue(m_jstockvolume.getText());
        myprod[14] = Boolean.valueOf(m_jInCatalog.isSelected());
        myprod[15] = Formats.INT.parseValue(m_jCatalogOrder1.getText());
        //myprod[16] = Formats.BYTEA.parseValue(txtAttributes.getText());
        String str = "";
    if (!this.m_jRef1.getText().equals(""))
    {
      if (!str.contains("<?xml")) {
        str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\"><properties>";
      }
      str = str + "<entry key=\"" + this.prod1.getID() + "\">" + this.m_jCant1.getText() + "</entry>";
    }
    if (!this.m_jRef2.getText().equals(""))
    {
      if (!str.contains("<?xml")) {
        str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\"><properties>";
      }
      str = str + "<entry key=\"" + this.prod2.getID() + "\">" + this.m_jCant2.getText() + "</entry>";
    }
    if (!this.m_jRef3.getText().equals(""))
    {
      if (!str.contains("<?xml")) {
        str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\"><properties>";
      }
      str = str + "<entry key=\"" + this.prod3.getID() + "\">" + this.m_jCant3.getText() + "</entry>";
    }
    if (!this.m_jRef4.getText().equals(""))
    {
      if (!str.contains("<?xml")) {
        str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\"><properties>";
      }
      str = str + "<entry key=\"" + this.prod4.getID() + "\">" + this.m_jCant4.getText() + "</entry>";
    }
    if (!this.m_jRef5.getText().equals(""))
    {
      if (!str.contains("<?xml")) {
        str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\"><properties>";
      }
      str = str + "<entry key=\"" + this.prod5.getID() + "\">" + this.m_jCant5.getText() + "</entry>";
    }
    if (!this.m_jRef6.getText().equals(""))
    {
      if (!str.contains("<?xml")) {
        str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\"><properties>";
      }
      str = str + "<entry key=\"" + this.prod6.getID() + "\">" + this.m_jCant6.getText() + "</entry>";
    }
    if (!this.m_jRef7.getText().equals(""))
    {
      if (!str.contains("<?xml")) {
        str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\"><properties>";
      }
      str = str + "<entry key=\"" + this.prod7.getID() + "\">" + this.m_jCant7.getText() + "</entry>";
    }
    if (!this.m_jRef8.getText().equals(""))
    {
      if (!str.contains("<?xml")) {
        str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\"><properties>";
      }
      str = str + "<entry key=\"" + this.prod8.getID() + "\">" + this.m_jCant8.getText() + "</entry>";
    }
    if (!this.m_jRef9.getText().equals(""))
    {
      if (!str.contains("<?xml")) {
        str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\"><properties>";
      }
      str = str + "<entry key=\"" + this.prod9.getID() + "\">" + this.m_jCant9.getText() + "</entry>";
    }
    if (str.length() > 0) {
      str = str + "</properties>";
    }
    myprod[16] = Formats.BYTEA.parseValue(str);
        return myprod;
    }    
    
    public Component getComponent() {
        return this;
    }
    
    private void calculateMargin() {
        
        if (!reportlock) {
            reportlock = true;
            
            Double dPriceBuy = readCurrency(m_jPriceBuy.getText());
            Double dPriceSell = (Double) pricesell;

            if (dPriceBuy == null || dPriceSell == null) {
                m_jmargin.setText(null);
            } else {
                m_jmargin.setText(Formats.PERCENT.formatValue(new Double(dPriceSell.doubleValue() / dPriceBuy.doubleValue() - 1.0)));
            }    
            reportlock = false;
        }
    }
    
    private void calculatePriceSellTax() {
        
        if (!reportlock) {
            reportlock = true;
            
            Double dPriceSell = (Double) pricesell;
            
            if (dPriceSell == null) {
                m_jPriceSellTax.setText(null);
            } else {               
                double dTaxRate = taxeslogic.getTaxRate((TaxCategoryInfo) taxcatmodel.getSelectedItem(), new Date());
                m_jPriceSellTax.setText(Formats.CURRENCY.formatValue(new Double(dPriceSell.doubleValue() * (1.0 + dTaxRate))));
            }            
            reportlock = false;
        }
    }
    
    private void calculatePriceSellfromMargin() {
        
        if (!reportlock) {
            reportlock = true;
            
            Double dPriceBuy = readCurrency(m_jPriceBuy.getText());
            Double dMargin = readPercent(m_jmargin.getText());  
            
            if (dMargin == null || dPriceBuy == null) {
                setPriceSell(null);
            } else {
                setPriceSell(new Double(dPriceBuy.doubleValue() * (1.0 + dMargin.doubleValue())));
            }                        
            
            reportlock = false;
        }
      
    }
    
    private void calculatePriceSellfromPST() {
        
        if (!reportlock) {
            reportlock = true;
                    
            Double dPriceSellTax = readCurrency(m_jPriceSellTax.getText());  

            if (dPriceSellTax == null) {
                setPriceSell(null);
            } else {
                double dTaxRate = taxeslogic.getTaxRate((TaxCategoryInfo) taxcatmodel.getSelectedItem(), new Date());
                setPriceSell(new Double(dPriceSellTax.doubleValue() / (1.0 + dTaxRate)));
            }   
                        
            reportlock = false;
        }    
    }
    
    private void setPriceSell(Object value) {
        
        if (!priceselllock) {
            priceselllock = true;
            pricesell = value;
            m_jPriceSell.setText(Formats.CURRENCY.formatValue(pricesell));  
            priceselllock = false;
        }
    }
    
    private class PriceSellManager implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            if (!priceselllock) {
                priceselllock = true;
                pricesell = readCurrency(m_jPriceSell.getText());
                priceselllock = false;
            }
            calculateMargin();
            calculatePriceSellTax();
        }
        public void insertUpdate(DocumentEvent e) {
            if (!priceselllock) {
                priceselllock = true;
                pricesell = readCurrency(m_jPriceSell.getText());
                priceselllock = false;
            }
            calculateMargin();
            calculatePriceSellTax();
        }    
        public void removeUpdate(DocumentEvent e) {
            if (!priceselllock) {
                priceselllock = true;
                pricesell = readCurrency(m_jPriceSell.getText());
                priceselllock = false;
            }
            calculateMargin();
            calculatePriceSellTax();
        }  
    }
    
    private class FieldsManager implements DocumentListener, ActionListener {
        public void changedUpdate(DocumentEvent e) {
            calculateMargin();
            calculatePriceSellTax();
        }
        public void insertUpdate(DocumentEvent e) {
            calculateMargin();
            calculatePriceSellTax();
        }    
        public void removeUpdate(DocumentEvent e) {
            calculateMargin();
            calculatePriceSellTax();
        }         
        public void actionPerformed(ActionEvent e) {
            calculateMargin();
            calculatePriceSellTax();
        }
    }

    private class PriceTaxManager implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            calculatePriceSellfromPST();
            calculateMargin();
        }
        public void insertUpdate(DocumentEvent e) {
            calculatePriceSellfromPST();
            calculateMargin();
        }    
        public void removeUpdate(DocumentEvent e) {
            calculatePriceSellfromPST();
            calculateMargin();
        }         
    }
    
    private class MarginManager implements DocumentListener  {
        public void changedUpdate(DocumentEvent e) {
            calculatePriceSellfromMargin();
            calculatePriceSellTax();
        }
        public void insertUpdate(DocumentEvent e) {
            calculatePriceSellfromMargin();
            calculatePriceSellTax();
        }    
        public void removeUpdate(DocumentEvent e) {
            calculatePriceSellfromMargin();
            calculatePriceSellTax();
        }         
    }
    
    private final static Double readCurrency(String sValue) {
        try {
            return (Double) Formats.CURRENCY.parseValue(sValue);
        } catch (BasicException e) {
            return null;
        }
    }
        
    private final static Double readPercent(String sValue) {
        try {
            return (Double) Formats.PERCENT.parseValue(sValue);
        } catch (BasicException e) {
            return null;
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        m_jCode = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        m_jPriceSellTax = new javax.swing.JTextField();
        m_jCodetype = new javax.swing.JComboBox();
        jLabel13 = new javax.swing.JLabel();
        m_jAtt = new javax.swing.JComboBox();
        jLabel7 = new javax.swing.JLabel();
        m_jTax = new javax.swing.JComboBox();
        jLabel12 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        m_jstockcost = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        m_jstockvolume = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        m_jCatalogOrder = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtAttributes = new javax.swing.JTextArea();
        jPanel3 = new javax.swing.JPanel();
        m_jmargin = new javax.swing.JTextField();
        m_jImage1 = new com.openbravo.data.gui.JImageEditor();
        jLabel14 = new javax.swing.JLabel();
        m_jPriceBuy1 = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        m_jPriceSell1 = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        m_jCategory1 = new javax.swing.JComboBox();
        jLabel19 = new javax.swing.JLabel();
        m_jInCatalog1 = new javax.swing.JCheckBox();
        jLabel20 = new javax.swing.JLabel();
        m_jComment = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        m_jRef = new javax.swing.JTextField();
        m_jName = new javax.swing.JTextField();
        m_jTitle = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        m_jImage = new com.openbravo.data.gui.JImageEditor();
        jLabel3 = new javax.swing.JLabel();
        m_jPriceBuy = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        m_jPriceSell = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        m_jCategory = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();
        m_jInCatalog = new javax.swing.JCheckBox();
        m_jScale = new javax.swing.JCheckBox();
        jLabel21 = new javax.swing.JLabel();
        m_jCatalogOrder1 = new javax.swing.JTextField();
        jLabel29 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        m_jRef1 = new javax.swing.JTextField();
        m_jCant1 = new javax.swing.JTextField();
        m_jProd1 = new javax.swing.JTextField();
        m_jRef2 = new javax.swing.JTextField();
        m_jCant2 = new javax.swing.JTextField();
        m_jProd2 = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        m_jRef3 = new javax.swing.JTextField();
        m_jCant3 = new javax.swing.JTextField();
        m_jProd3 = new javax.swing.JTextField();
        jButton4 = new javax.swing.JButton();
        m_jRef4 = new javax.swing.JTextField();
        m_jCant4 = new javax.swing.JTextField();
        m_jProd4 = new javax.swing.JTextField();
        jButton5 = new javax.swing.JButton();
        m_jRef5 = new javax.swing.JTextField();
        m_jCant5 = new javax.swing.JTextField();
        m_jProd5 = new javax.swing.JTextField();
        jButton6 = new javax.swing.JButton();
        m_jRef6 = new javax.swing.JTextField();
        m_jCant6 = new javax.swing.JTextField();
        m_jProd6 = new javax.swing.JTextField();
        jButton7 = new javax.swing.JButton();
        m_jRef7 = new javax.swing.JTextField();
        m_jCant7 = new javax.swing.JTextField();
        m_jProd7 = new javax.swing.JTextField();
        jButton8 = new javax.swing.JButton();
        m_jRef8 = new javax.swing.JTextField();
        m_jCant8 = new javax.swing.JTextField();
        m_jProd8 = new javax.swing.JTextField();
        jButton9 = new javax.swing.JButton();
        m_jRef9 = new javax.swing.JTextField();
        m_jCant9 = new javax.swing.JTextField();
        m_jProd9 = new javax.swing.JTextField();
        jButton10 = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        m_jRef10 = new javax.swing.JTextField();
        m_jName1 = new javax.swing.JTextField();
        m_jTitle1 = new javax.swing.JLabel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel6 = new javax.swing.JPanel();
        m_jImage2 = new com.openbravo.data.gui.JImageEditor();
        jLabel24 = new javax.swing.JLabel();
        m_jPriceBuy2 = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        m_jPriceSell2 = new javax.swing.JTextField();
        jLabel26 = new javax.swing.JLabel();
        m_jCategory2 = new javax.swing.JComboBox();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();
        m_jRef11 = new javax.swing.JTextField();
        m_jCant10 = new javax.swing.JTextField();
        m_jProd10 = new javax.swing.JTextField();
        m_jRef12 = new javax.swing.JTextField();
        m_jCant11 = new javax.swing.JTextField();
        m_jProd11 = new javax.swing.JTextField();
        m_jRef13 = new javax.swing.JTextField();
        m_jCant12 = new javax.swing.JTextField();
        m_jProd12 = new javax.swing.JTextField();
        m_jRef14 = new javax.swing.JTextField();
        m_jCant13 = new javax.swing.JTextField();
        m_jProd13 = new javax.swing.JTextField();
        m_jRef15 = new javax.swing.JTextField();
        m_jCant14 = new javax.swing.JTextField();
        m_jProd14 = new javax.swing.JTextField();
        m_jRef16 = new javax.swing.JTextField();
        m_jCant15 = new javax.swing.JTextField();
        m_jProd15 = new javax.swing.JTextField();
        m_jRef17 = new javax.swing.JTextField();
        m_jCant16 = new javax.swing.JTextField();
        m_jProd16 = new javax.swing.JTextField();
        m_jRef18 = new javax.swing.JTextField();
        m_jCant17 = new javax.swing.JTextField();
        m_jProd17 = new javax.swing.JTextField();
        m_jRef19 = new javax.swing.JTextField();
        m_jCant18 = new javax.swing.JTextField();
        m_jProd18 = new javax.swing.JTextField();

        jLabel6.setText(AppLocal.getIntString("label.prodbarcode")); // NOI18N

        jLabel16.setText(AppLocal.getIntString("label.prodpriceselltax")); // NOI18N

        m_jPriceSellTax.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel13.setText(AppLocal.getIntString("label.attributes")); // NOI18N

        jLabel7.setText(AppLocal.getIntString("label.taxcategory")); // NOI18N

        jLabel12.setText("Es bebida?");

        jPanel2.setLayout(null);

        jLabel9.setText(AppLocal.getIntString("label.prodstockcost")); // NOI18N
        jPanel2.add(jLabel9);
        jLabel9.setBounds(10, 20, 150, 14);

        m_jstockcost.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel2.add(m_jstockcost);
        m_jstockcost.setBounds(160, 20, 80, 20);

        jLabel10.setText(AppLocal.getIntString("label.prodstockvol")); // NOI18N
        jPanel2.add(jLabel10);
        jLabel10.setBounds(10, 50, 150, 14);

        m_jstockvolume.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel2.add(m_jstockvolume);
        m_jstockvolume.setBounds(160, 50, 80, 20);

        jLabel18.setText(AppLocal.getIntString("label.prodorder")); // NOI18N
        jPanel2.add(jLabel18);
        jLabel18.setBounds(250, 80, 60, 14);

        m_jCatalogOrder.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel2.add(m_jCatalogOrder);
        m_jCatalogOrder.setBounds(310, 80, 80, 20);

        jLabel11.setText(AppLocal.getIntString("label.prodaux")); // NOI18N
        jPanel2.add(jLabel11);
        jLabel11.setBounds(10, 110, 150, 14);

        txtAttributes.setFont(new java.awt.Font("DialogInput", 0, 12)); // NOI18N
        jScrollPane1.setViewportView(txtAttributes);

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel3.setLayout(new java.awt.BorderLayout());

        m_jmargin.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel14.setText(AppLocal.getIntString("label.prodpricebuy")); // NOI18N

        m_jPriceBuy1.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel15.setText(AppLocal.getIntString("label.prodpricesell")); // NOI18N

        m_jPriceSell1.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel17.setText(AppLocal.getIntString("label.prodcategory")); // NOI18N

        jLabel19.setText(AppLocal.getIntString("label.prodincatalog")); // NOI18N

        m_jInCatalog1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jInCatalog1ActionPerformed(evt);
            }
        });

        jLabel20.setText("Barra 2");

        setLayout(null);

        jLabel1.setText(AppLocal.getIntString("label.prodref")); // NOI18N
        add(jLabel1);
        jLabel1.setBounds(10, 50, 80, 14);

        jLabel2.setText(AppLocal.getIntString("label.prodname")); // NOI18N
        add(jLabel2);
        jLabel2.setBounds(180, 50, 70, 14);
        add(m_jRef);
        m_jRef.setBounds(90, 50, 80, 20);
        add(m_jName);
        m_jName.setBounds(250, 50, 320, 20);

        m_jTitle.setFont(new java.awt.Font("SansSerif", 3, 18)); // NOI18N
        add(m_jTitle);
        m_jTitle.setBounds(10, 10, 560, 30);

        jPanel1.setLayout(null);
        jPanel1.add(m_jImage);
        m_jImage.setBounds(340, 20, 200, 180);

        jLabel3.setText(AppLocal.getIntString("label.prodpricebuy")); // NOI18N
        jPanel1.add(jLabel3);
        jLabel3.setBounds(10, 10, 150, 14);

        m_jPriceBuy.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel1.add(m_jPriceBuy);
        m_jPriceBuy.setBounds(160, 10, 80, 20);

        jLabel4.setText(AppLocal.getIntString("label.prodpricesell")); // NOI18N
        jPanel1.add(jLabel4);
        jLabel4.setBounds(10, 40, 150, 14);

        m_jPriceSell.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel1.add(m_jPriceSell);
        m_jPriceSell.setBounds(160, 40, 80, 20);

        jLabel5.setText(AppLocal.getIntString("label.prodcategory")); // NOI18N
        jPanel1.add(jLabel5);
        jLabel5.setBounds(10, 70, 150, 14);
        jPanel1.add(m_jCategory);
        m_jCategory.setBounds(160, 70, 170, 20);

        jLabel8.setText(AppLocal.getIntString("label.prodincatalog")); // NOI18N
        jPanel1.add(jLabel8);
        jLabel8.setBounds(10, 100, 150, 14);

        m_jInCatalog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jInCatalogActionPerformed(evt);
            }
        });
        jPanel1.add(m_jInCatalog);
        m_jInCatalog.setBounds(160, 100, 50, 21);
        jPanel1.add(m_jScale);
        m_jScale.setBounds(160, 130, 60, 21);

        jLabel21.setText("Bebida");
        jPanel1.add(jLabel21);
        jLabel21.setBounds(10, 130, 80, 14);

        m_jCatalogOrder1.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel1.add(m_jCatalogOrder1);
        m_jCatalogOrder1.setBounds(160, 160, 80, 20);

        jLabel29.setText(AppLocal.getIntString("label.prodorder")); // NOI18N
        jPanel1.add(jLabel29);
        jLabel29.setBounds(10, 160, 60, 14);

        jTabbedPane1.addTab(AppLocal.getIntString("label.prodgeneral"), jPanel1); // NOI18N

        jPanel4.setLayout(null);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search.png"))); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton1);
        jButton1.setBounds(10, 10, 30, 25);

        m_jRef1.setEnabled(false);
        jPanel4.add(m_jRef1);
        m_jRef1.setBounds(50, 10, 80, 20);
        jPanel4.add(m_jCant1);
        m_jCant1.setBounds(490, 10, 50, 20);

        m_jProd1.setEnabled(false);
        jPanel4.add(m_jProd1);
        m_jProd1.setBounds(140, 10, 340, 20);

        m_jRef2.setEnabled(false);
        jPanel4.add(m_jRef2);
        m_jRef2.setBounds(50, 40, 80, 20);
        jPanel4.add(m_jCant2);
        m_jCant2.setBounds(490, 40, 50, 20);

        m_jProd2.setEnabled(false);
        jPanel4.add(m_jProd2);
        m_jProd2.setBounds(140, 40, 340, 20);

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search.png"))); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton2);
        jButton2.setBounds(10, 40, 30, 25);

        m_jRef3.setEnabled(false);
        jPanel4.add(m_jRef3);
        m_jRef3.setBounds(50, 70, 80, 20);
        jPanel4.add(m_jCant3);
        m_jCant3.setBounds(490, 70, 50, 20);

        m_jProd3.setEnabled(false);
        jPanel4.add(m_jProd3);
        m_jProd3.setBounds(140, 70, 340, 20);

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search.png"))); // NOI18N
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton4);
        jButton4.setBounds(10, 70, 30, 25);

        m_jRef4.setEnabled(false);
        jPanel4.add(m_jRef4);
        m_jRef4.setBounds(50, 100, 80, 20);
        jPanel4.add(m_jCant4);
        m_jCant4.setBounds(490, 100, 50, 20);

        m_jProd4.setEnabled(false);
        jPanel4.add(m_jProd4);
        m_jProd4.setBounds(140, 100, 340, 20);

        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search.png"))); // NOI18N
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton5);
        jButton5.setBounds(10, 100, 30, 25);

        m_jRef5.setEnabled(false);
        jPanel4.add(m_jRef5);
        m_jRef5.setBounds(50, 130, 80, 20);
        jPanel4.add(m_jCant5);
        m_jCant5.setBounds(490, 130, 50, 20);

        m_jProd5.setEnabled(false);
        jPanel4.add(m_jProd5);
        m_jProd5.setBounds(140, 130, 340, 20);

        jButton6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search.png"))); // NOI18N
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton6);
        jButton6.setBounds(10, 130, 30, 25);

        m_jRef6.setEnabled(false);
        jPanel4.add(m_jRef6);
        m_jRef6.setBounds(50, 160, 80, 20);
        jPanel4.add(m_jCant6);
        m_jCant6.setBounds(490, 160, 50, 20);

        m_jProd6.setEnabled(false);
        jPanel4.add(m_jProd6);
        m_jProd6.setBounds(140, 160, 340, 20);

        jButton7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search.png"))); // NOI18N
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton7);
        jButton7.setBounds(10, 160, 30, 25);

        m_jRef7.setEnabled(false);
        jPanel4.add(m_jRef7);
        m_jRef7.setBounds(50, 190, 80, 20);
        jPanel4.add(m_jCant7);
        m_jCant7.setBounds(490, 190, 50, 20);

        m_jProd7.setEnabled(false);
        jPanel4.add(m_jProd7);
        m_jProd7.setBounds(140, 190, 340, 20);

        jButton8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search.png"))); // NOI18N
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton8);
        jButton8.setBounds(10, 190, 30, 25);

        m_jRef8.setEnabled(false);
        jPanel4.add(m_jRef8);
        m_jRef8.setBounds(50, 220, 80, 20);
        jPanel4.add(m_jCant8);
        m_jCant8.setBounds(490, 220, 50, 20);

        m_jProd8.setEnabled(false);
        jPanel4.add(m_jProd8);
        m_jProd8.setBounds(140, 220, 340, 20);

        jButton9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search.png"))); // NOI18N
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton9);
        jButton9.setBounds(10, 220, 30, 25);

        m_jRef9.setEnabled(false);
        jPanel4.add(m_jRef9);
        m_jRef9.setBounds(50, 250, 80, 20);
        jPanel4.add(m_jCant9);
        m_jCant9.setBounds(490, 250, 50, 20);

        m_jProd9.setEnabled(false);
        jPanel4.add(m_jProd9);
        m_jProd9.setBounds(140, 250, 340, 20);

        jButton10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search.png"))); // NOI18N
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });
        jPanel4.add(jButton10);
        jButton10.setBounds(10, 250, 30, 25);

        jTabbedPane1.addTab("Composicion", jPanel4);

        add(jTabbedPane1);
        jTabbedPane1.setBounds(10, 80, 560, 310);

        jPanel5.setLayout(null);

        jLabel22.setText(AppLocal.getIntString("label.prodref")); // NOI18N
        jPanel5.add(jLabel22);
        jLabel22.setBounds(10, 50, 80, 14);

        jLabel23.setText(AppLocal.getIntString("label.prodname")); // NOI18N
        jPanel5.add(jLabel23);
        jLabel23.setBounds(180, 50, 70, 14);
        jPanel5.add(m_jRef10);
        m_jRef10.setBounds(90, 50, 80, 20);
        jPanel5.add(m_jName1);
        m_jName1.setBounds(250, 50, 320, 20);

        m_jTitle1.setFont(new java.awt.Font("SansSerif", 3, 18)); // NOI18N
        jPanel5.add(m_jTitle1);
        m_jTitle1.setBounds(10, 10, 560, 30);

        jPanel6.setLayout(null);
        jPanel6.add(m_jImage2);
        m_jImage2.setBounds(340, 20, 200, 180);

        jLabel24.setText(AppLocal.getIntString("label.prodpricebuy")); // NOI18N
        jPanel6.add(jLabel24);
        jLabel24.setBounds(10, 10, 150, 14);

        m_jPriceBuy2.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel6.add(m_jPriceBuy2);
        m_jPriceBuy2.setBounds(160, 10, 80, 20);

        jLabel25.setText(AppLocal.getIntString("label.prodpricesell")); // NOI18N
        jPanel6.add(jLabel25);
        jLabel25.setBounds(10, 40, 150, 14);

        m_jPriceSell2.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel6.add(m_jPriceSell2);
        m_jPriceSell2.setBounds(160, 40, 80, 20);

        jLabel26.setText(AppLocal.getIntString("label.prodcategory")); // NOI18N
        jPanel6.add(jLabel26);
        jLabel26.setBounds(10, 70, 150, 14);
        jPanel6.add(m_jCategory2);
        m_jCategory2.setBounds(160, 70, 170, 20);

        jLabel27.setText(AppLocal.getIntString("label.prodincatalog")); // NOI18N
        jPanel6.add(jLabel27);
        jLabel27.setBounds(10, 100, 150, 14);

        jLabel28.setText("Bebida");
        jPanel6.add(jLabel28);
        jLabel28.setBounds(10, 130, 80, 14);

        jTabbedPane2.addTab("General", jPanel6);

        jPanel7.setLayout(null);

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search.png"))); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jPanel7.add(jButton3);
        jButton3.setBounds(10, 10, 30, 25);

        m_jRef11.setEnabled(false);
        m_jRef11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jRef11ActionPerformed(evt);
            }
        });
        jPanel7.add(m_jRef11);
        m_jRef11.setBounds(50, 10, 80, 20);
        jPanel7.add(m_jCant10);
        m_jCant10.setBounds(490, 10, 50, 20);

        m_jProd10.setEnabled(false);
        jPanel7.add(m_jProd10);
        m_jProd10.setBounds(140, 10, 340, 20);

        m_jRef12.setEnabled(false);
        jPanel7.add(m_jRef12);
        m_jRef12.setBounds(50, 40, 80, 20);
        jPanel7.add(m_jCant11);
        m_jCant11.setBounds(490, 40, 50, 20);

        m_jProd11.setEnabled(false);
        jPanel7.add(m_jProd11);
        m_jProd11.setBounds(140, 40, 340, 20);

        m_jRef13.setEnabled(false);
        jPanel7.add(m_jRef13);
        m_jRef13.setBounds(50, 70, 80, 20);
        jPanel7.add(m_jCant12);
        m_jCant12.setBounds(490, 70, 50, 20);

        m_jProd12.setEnabled(false);
        jPanel7.add(m_jProd12);
        m_jProd12.setBounds(140, 70, 340, 20);

        m_jRef14.setEnabled(false);
        jPanel7.add(m_jRef14);
        m_jRef14.setBounds(50, 100, 80, 20);
        jPanel7.add(m_jCant13);
        m_jCant13.setBounds(490, 100, 50, 20);

        m_jProd13.setEnabled(false);
        jPanel7.add(m_jProd13);
        m_jProd13.setBounds(140, 100, 340, 20);

        m_jRef15.setEnabled(false);
        jPanel7.add(m_jRef15);
        m_jRef15.setBounds(50, 130, 80, 20);
        jPanel7.add(m_jCant14);
        m_jCant14.setBounds(490, 130, 50, 20);

        m_jProd14.setEnabled(false);
        jPanel7.add(m_jProd14);
        m_jProd14.setBounds(140, 130, 340, 20);

        m_jRef16.setEnabled(false);
        jPanel7.add(m_jRef16);
        m_jRef16.setBounds(50, 160, 80, 20);
        jPanel7.add(m_jCant15);
        m_jCant15.setBounds(490, 160, 50, 20);

        m_jProd15.setEnabled(false);
        jPanel7.add(m_jProd15);
        m_jProd15.setBounds(140, 160, 340, 20);

        m_jRef17.setEnabled(false);
        jPanel7.add(m_jRef17);
        m_jRef17.setBounds(50, 190, 80, 20);
        jPanel7.add(m_jCant16);
        m_jCant16.setBounds(490, 190, 50, 20);

        m_jProd16.setEnabled(false);
        jPanel7.add(m_jProd16);
        m_jProd16.setBounds(140, 190, 340, 20);

        m_jRef18.setEnabled(false);
        jPanel7.add(m_jRef18);
        m_jRef18.setBounds(50, 220, 80, 20);
        jPanel7.add(m_jCant17);
        m_jCant17.setBounds(490, 220, 50, 20);

        m_jProd17.setEnabled(false);
        jPanel7.add(m_jProd17);
        m_jProd17.setBounds(140, 220, 340, 20);

        m_jRef19.setEnabled(false);
        jPanel7.add(m_jRef19);
        m_jRef19.setBounds(50, 250, 80, 20);
        jPanel7.add(m_jCant18);
        m_jCant18.setBounds(490, 250, 50, 20);

        m_jProd18.setEnabled(false);
        jPanel7.add(m_jProd18);
        m_jProd18.setBounds(140, 250, 340, 20);

        jTabbedPane2.addTab("Composicion", jPanel7);

        jPanel5.add(jTabbedPane2);
        jTabbedPane2.setBounds(10, 80, 560, 310);

        add(jPanel5);
        jPanel5.setBounds(0, 0, 0, 0);
    }// </editor-fold>//GEN-END:initComponents

    private void m_jInCatalogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jInCatalogActionPerformed
 
        if (m_jInCatalog1.isSelected()) {
            m_jCatalogOrder1.setEnabled(true);   
        } else {
            m_jCatalogOrder1.setEnabled(false);   
            m_jCatalogOrder1.setText(null);   
        }

    }//GEN-LAST:event_m_jInCatalogActionPerformed

    private void m_jInCatalog1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jInCatalog1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_m_jInCatalog1ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    this.prod1 = JProductFinder.showMessage(this, this.dlSales);
    if (this.prod1 != null)
    {
      this.m_jRef1.setText(this.prod1.getReference());
      this.m_jProd1.setText(this.prod1.getName());
      this.m_jCant1.setText("");
      this.m_jCant1.setEnabled(true);
      this.m_jCant1.requestFocus();
    }
    else
    {
      this.m_jRef1.setText("");
      this.m_jProd1.setText("");
      this.m_jCant1.setText("");
      this.m_jCant1.setEnabled(false);
    }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {                                         
       this.prod2 = JProductFinder.showMessage(this, this.dlSales);
    if (this.prod2 != null)
    {
      this.m_jRef2.setText(this.prod2.getReference());
      this.m_jProd2.setText(this.prod2.getName());
      this.m_jCant2.setText("");
      this.m_jCant2.setEnabled(true);
      this.m_jCant2.requestFocus();
    }
    else
    {
      this.m_jRef2.setText("");
      this.m_jProd2.setText("");
      this.m_jCant2.setText("");
      this.m_jCant2.setEnabled(false);
    }
    }                                        

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
    this.prod2 = JProductFinder.showMessage(this, this.dlSales);
        if (this.prod2 != null)
        {
          this.m_jRef2.setText(this.prod2.getReference());
          this.m_jProd2.setText(this.prod2.getName());
          this.m_jCant2.setText("");
          this.m_jCant2.setEnabled(true);
          this.m_jCant2.requestFocus();
        }
        else
        {
          this.m_jRef2.setText("");
          this.m_jProd2.setText("");
          this.m_jCant2.setText("");
          this.m_jCant2.setEnabled(false);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        this.prod3 = JProductFinder.showMessage(this, this.dlSales);
        if (this.prod3 != null)
        {
          this.m_jRef3.setText(this.prod3.getReference());
          this.m_jProd3.setText(this.prod3.getName());
          this.m_jCant3.setText("");
          this.m_jCant3.setEnabled(true);
          this.m_jCant3.requestFocus();
        }
        else
        {
          this.m_jRef3.setText("");
          this.m_jProd3.setText("");
          this.m_jCant3.setText("");
          this.m_jCant3.setEnabled(false);
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        this.prod4 = JProductFinder.showMessage(this, this.dlSales);
        if (this.prod4 != null)
        {
          this.m_jRef4.setText(this.prod4.getReference());
          this.m_jProd4.setText(this.prod4.getName());
          this.m_jCant4.setText("");
          this.m_jCant4.setEnabled(true);
          this.m_jCant4.requestFocus();
        }
        else
        {
          this.m_jRef4.setText("");
          this.m_jProd4.setText("");
          this.m_jCant4.setText("");
          this.m_jCant4.setEnabled(false);
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        this.prod5 = JProductFinder.showMessage(this, this.dlSales);
        if (this.prod5 != null)
        {
          this.m_jRef5.setText(this.prod5.getReference());
          this.m_jProd5.setText(this.prod5.getName());
          this.m_jCant5.setText("");
          this.m_jCant5.setEnabled(true);
          this.m_jCant5.requestFocus();
        }
        else
        {
          this.m_jRef5.setText("");
          this.m_jProd5.setText("");
          this.m_jCant5.setText("");
          this.m_jCant5.setEnabled(false);
        }
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
      this.prod6 = JProductFinder.showMessage(this, this.dlSales);
        if (this.prod6 != null)
        {
          this.m_jRef6.setText(this.prod6.getReference());
          this.m_jProd6.setText(this.prod6.getName());
          this.m_jCant6.setText("");
          this.m_jCant6.setEnabled(true);
          this.m_jCant6.requestFocus();
        }
        else
        {
          this.m_jRef6.setText("");
          this.m_jProd6.setText("");
          this.m_jCant6.setText("");
          this.m_jCant6.setEnabled(false);
        }
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
       this.prod7 = JProductFinder.showMessage(this, this.dlSales);
        if (this.prod7 != null)
        {
          this.m_jRef7.setText(this.prod7.getReference());
          this.m_jProd7.setText(this.prod7.getName());
          this.m_jCant7.setText("");
          this.m_jCant7.setEnabled(true);
          this.m_jCant7.requestFocus();
        }
        else
        {
          this.m_jRef7.setText("");
          this.m_jProd7.setText("");
          this.m_jCant7.setText("");
          this.m_jCant7.setEnabled(false);
        }
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        this.prod8 = JProductFinder.showMessage(this, this.dlSales);
        if (this.prod8 != null)
        {
          this.m_jRef8.setText(this.prod8.getReference());
          this.m_jProd8.setText(this.prod8.getName());
          this.m_jCant8.setText("");
          this.m_jCant8.setEnabled(true);
          this.m_jCant8.requestFocus();
        }
        else
        {
          this.m_jRef8.setText("");
          this.m_jProd8.setText("");
          this.m_jCant8.setText("");
          this.m_jCant8.setEnabled(false);
        }
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        this.prod9 = JProductFinder.showMessage(this, this.dlSales);
        if (this.prod9 != null)
        {
          this.m_jRef9.setText(this.prod9.getReference());
          this.m_jProd9.setText(this.prod9.getName());
          this.m_jCant9.setText("");
          this.m_jCant9.setEnabled(true);
          this.m_jCant9.requestFocus();
        }
        else
        {
          this.m_jRef9.setText("");
          this.m_jProd9.setText("");
          this.m_jCant9.setText("");
          this.m_jCant9.setEnabled(false);
        }
    }//GEN-LAST:event_jButton10ActionPerformed

    private void m_jRef11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jRef11ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_m_jRef11ActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JComboBox m_jAtt;
    private javax.swing.JTextField m_jCant1;
    private javax.swing.JTextField m_jCant10;
    private javax.swing.JTextField m_jCant11;
    private javax.swing.JTextField m_jCant12;
    private javax.swing.JTextField m_jCant13;
    private javax.swing.JTextField m_jCant14;
    private javax.swing.JTextField m_jCant15;
    private javax.swing.JTextField m_jCant16;
    private javax.swing.JTextField m_jCant17;
    private javax.swing.JTextField m_jCant18;
    private javax.swing.JTextField m_jCant2;
    private javax.swing.JTextField m_jCant3;
    private javax.swing.JTextField m_jCant4;
    private javax.swing.JTextField m_jCant5;
    private javax.swing.JTextField m_jCant6;
    private javax.swing.JTextField m_jCant7;
    private javax.swing.JTextField m_jCant8;
    private javax.swing.JTextField m_jCant9;
    private javax.swing.JTextField m_jCatalogOrder;
    private javax.swing.JTextField m_jCatalogOrder1;
    private javax.swing.JComboBox m_jCategory;
    private javax.swing.JComboBox m_jCategory1;
    private javax.swing.JComboBox m_jCategory2;
    private javax.swing.JTextField m_jCode;
    private javax.swing.JComboBox m_jCodetype;
    private javax.swing.JCheckBox m_jComment;
    private com.openbravo.data.gui.JImageEditor m_jImage;
    private com.openbravo.data.gui.JImageEditor m_jImage1;
    private com.openbravo.data.gui.JImageEditor m_jImage2;
    private javax.swing.JCheckBox m_jInCatalog;
    private javax.swing.JCheckBox m_jInCatalog1;
    private javax.swing.JTextField m_jName;
    private javax.swing.JTextField m_jName1;
    private javax.swing.JTextField m_jPriceBuy;
    private javax.swing.JTextField m_jPriceBuy1;
    private javax.swing.JTextField m_jPriceBuy2;
    private javax.swing.JTextField m_jPriceSell;
    private javax.swing.JTextField m_jPriceSell1;
    private javax.swing.JTextField m_jPriceSell2;
    private javax.swing.JTextField m_jPriceSellTax;
    private javax.swing.JTextField m_jProd1;
    private javax.swing.JTextField m_jProd10;
    private javax.swing.JTextField m_jProd11;
    private javax.swing.JTextField m_jProd12;
    private javax.swing.JTextField m_jProd13;
    private javax.swing.JTextField m_jProd14;
    private javax.swing.JTextField m_jProd15;
    private javax.swing.JTextField m_jProd16;
    private javax.swing.JTextField m_jProd17;
    private javax.swing.JTextField m_jProd18;
    private javax.swing.JTextField m_jProd2;
    private javax.swing.JTextField m_jProd3;
    private javax.swing.JTextField m_jProd4;
    private javax.swing.JTextField m_jProd5;
    private javax.swing.JTextField m_jProd6;
    private javax.swing.JTextField m_jProd7;
    private javax.swing.JTextField m_jProd8;
    private javax.swing.JTextField m_jProd9;
    private javax.swing.JTextField m_jRef;
    private javax.swing.JTextField m_jRef1;
    private javax.swing.JTextField m_jRef10;
    private javax.swing.JTextField m_jRef11;
    private javax.swing.JTextField m_jRef12;
    private javax.swing.JTextField m_jRef13;
    private javax.swing.JTextField m_jRef14;
    private javax.swing.JTextField m_jRef15;
    private javax.swing.JTextField m_jRef16;
    private javax.swing.JTextField m_jRef17;
    private javax.swing.JTextField m_jRef18;
    private javax.swing.JTextField m_jRef19;
    private javax.swing.JTextField m_jRef2;
    private javax.swing.JTextField m_jRef3;
    private javax.swing.JTextField m_jRef4;
    private javax.swing.JTextField m_jRef5;
    private javax.swing.JTextField m_jRef6;
    private javax.swing.JTextField m_jRef7;
    private javax.swing.JTextField m_jRef8;
    private javax.swing.JTextField m_jRef9;
    private javax.swing.JCheckBox m_jScale;
    private javax.swing.JComboBox m_jTax;
    private javax.swing.JLabel m_jTitle;
    private javax.swing.JLabel m_jTitle1;
    private javax.swing.JTextField m_jmargin;
    private javax.swing.JTextField m_jstockcost;
    private javax.swing.JTextField m_jstockvolume;
    private javax.swing.JTextArea txtAttributes;
    // End of variables declaration//GEN-END:variables
    
}
