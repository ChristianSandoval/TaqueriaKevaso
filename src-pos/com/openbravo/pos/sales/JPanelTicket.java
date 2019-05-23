package com.openbravo.pos.sales;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Date;

import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.printer.*;

import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.panels.JProductFinder;
import com.openbravo.pos.scale.ScaleException;
import com.openbravo.pos.payment.JPaymentSelect;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ListKeyed;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.data.loader.StaticSentence;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.customers.JCustomerFinder;
import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptException;
import com.openbravo.pos.scripting.ScriptFactory;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.inventory.TaxCategoryInfo;
import com.openbravo.pos.payment.JPaymentSelectReceipt;
import com.openbravo.pos.payment.JPaymentSelectRefund;
import com.openbravo.pos.sales.restaurant.Place;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.pos.ticket.TaxInfo;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import com.openbravo.pos.util.JRPrinterAWT300;
import com.openbravo.pos.util.ReportUtils;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.PrintService;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapArrayDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

/**
 *
 * @author adrianromero
 */
public abstract class JPanelTicket extends JPanel implements JPanelView, BeanFactoryApp, TicketsEditor {
   
    // Variable numerica
    private final static int NUMBERZERO = 0;
    private final static int NUMBERVALID = 1;
    
    private final static int NUMBER_INPUTZERO = 0;
    private final static int NUMBER_INPUTZERODEC = 1;
    private final static int NUMBER_INPUTINT = 2;
    private final static int NUMBER_INPUTDEC = 3; 
    private final static int NUMBER_PORZERO = 4; 
    private final static int NUMBER_PORZERODEC = 5; 
    private final static int NUMBER_PORINT = 6; 
    private final static int NUMBER_PORDEC = 7; 

    protected JTicketLines m_ticketlines;
        
    // private Template m_tempLine;
    private TicketParser m_TTP;
    
    protected TicketInfo m_oTicket; 
    protected Place m_oTicketExt; 
    
    // Estas tres variables forman el estado...
    private int m_iNumberStatus;
    private int m_iNumberStatusInput;
    private int m_iNumberStatusPor;
    private StringBuffer m_sBarcode;
            
    private JTicketsBag m_ticketsbag;
    
    private SentenceList senttax;
    private ListKeyed taxcollection;
    // private ComboBoxValModel m_TaxModel;
    
    private SentenceList senttaxcategories;
    private ListKeyed taxcategoriescollection;
    private ComboBoxValModel taxcategoriesmodel;
    
    private TaxesLogic taxeslogic;
    
//    private ScriptObject scriptobjinst;
    protected JPanelButtons m_jbtnconfig;
    
    protected AppView m_App;
    protected DataLogicSystem dlSystem;
    protected DataLogicSales dlSales;
    protected DataLogicCustomers dlCustomers;
    
    private JPaymentSelect paymentdialogreceipt;
    private JPaymentSelect paymentdialogrefund;

    /** Creates new form JTicketView */
    public JPanelTicket() {
        
        initComponents ();
    }
   
    public void init(AppView app) throws BeanFactoryException {
        
        m_App = app;
        dlSystem = (DataLogicSystem) m_App.getBean("com.openbravo.pos.forms.DataLogicSystem");
        dlSales = (DataLogicSales) m_App.getBean("com.openbravo.pos.forms.DataLogicSales");
        dlCustomers = (DataLogicCustomers) m_App.getBean("com.openbravo.pos.customers.DataLogicCustomers");
                    
        // borramos el boton de bascula si no hay bascula conectada
        /*if (!m_App.getDeviceScale().existsScale()) {
            m_jbtnScale.setVisible(false);
        }*/
        
        m_ticketsbag = getJTicketsBag();
        m_jPanelBag.add(m_ticketsbag.getBagComponent(), BorderLayout.LINE_START);
        add(m_ticketsbag.getNullComponent(), "null");

        m_ticketlines = new JTicketLines(dlSystem.getResourceAsXML("Ticket.Line"));
        m_jPanelCentral.add(m_ticketlines, java.awt.BorderLayout.CENTER);
        
        m_TTP = new TicketParser(m_App.getDeviceTicket(), dlSystem);
               
        // Los botones configurables...
        m_jbtnconfig = new JPanelButtons("Ticket.Buttons", this);
        m_jButtonsExt.add(m_jbtnconfig);           
       
        // El panel de los productos o de las lineas...        
        catcontainer.add(getSouthComponent(), BorderLayout.CENTER);
        
        // El modelo de impuestos
        senttax = dlSales.getTaxList();
        senttaxcategories = dlSales.getTaxCategoriesList();
        
        taxcategoriesmodel = new ComboBoxValModel();    
              
        // ponemos a cero el estado
        stateToZero();  
        
        // inicializamos
        m_oTicket = null;
        m_oTicketExt = null;      
    }
    
    public Object getBean() {
        return this;
    }
    
    public JComponent getComponent() {
        return this;
    }

    public void activate() throws BasicException {

        paymentdialogreceipt = JPaymentSelectReceipt.getDialog(this);
        paymentdialogreceipt.init(m_App);
        paymentdialogrefund = JPaymentSelectRefund.getDialog(this); 
        paymentdialogrefund.init(m_App);
        
        // impuestos incluidos seleccionado ?
        m_jaddtax.setSelected("true".equals(m_jbtnconfig.getProperty("taxesincluded")));

        // Inicializamos el combo de los impuestos.
        java.util.List<TaxInfo> taxlist = senttax.list();
        taxcollection = new ListKeyed<TaxInfo>(taxlist);
        java.util.List<TaxCategoryInfo> taxcategorieslist = senttaxcategories.list();
        taxcategoriescollection = new ListKeyed<TaxCategoryInfo>(taxcategorieslist);
        
        taxcategoriesmodel = new ComboBoxValModel(taxcategorieslist);
        m_jTax.setModel(taxcategoriesmodel);

        String taxesid = m_jbtnconfig.getProperty("taxcategoryid");
        if (taxesid == null) {
            if (m_jTax.getItemCount() > 0) {
                m_jTax.setSelectedIndex(0);
            }
        } else {
            taxcategoriesmodel.setSelectedKey(taxesid);
        }              
                
        taxeslogic = new TaxesLogic(taxlist);
        
        // Show taxes options
        if (m_App.getAppUserView().getUser().hasPermission("sales.ChangeTaxOptions")) {
            m_jTax.setVisible(true);
            m_jaddtax.setVisible(true);
        } else {
            m_jTax.setVisible(false);
            m_jaddtax.setVisible(false);
        }
        
        // Authorization for buttons
        btnSplit.setEnabled(m_App.getAppUserView().getUser().hasPermission("sales.Total"));
        m_jDelete.setEnabled(m_App.getAppUserView().getUser().hasPermission("sales.EditLines"));
        m_jNumberKeys.setMinusEnabled(m_App.getAppUserView().getUser().hasPermission("sales.EditLines"));
        m_jNumberKeys.setEqualsEnabled(m_App.getAppUserView().getUser().hasPermission("sales.Total"));
        m_jbtnconfig.setPermissions(m_App.getAppUserView().getUser());  
               
        m_ticketsbag.activate();        
    }
    
    public boolean deactivate() {

        return m_ticketsbag.deactivate();
    }
    
    protected abstract JTicketsBag getJTicketsBag();
    protected abstract Component getSouthComponent();
    protected abstract void resetSouthComponent();
     
    public void setActiveTicket(TicketInfo oTicket, Place oTicketExt) {
       
        m_oTicket = oTicket;
        m_oTicketExt = oTicketExt;
        
        if (m_oTicket != null) {            
            m_oTicket.setUser(m_App.getAppUserView().getUser().getUserInfo());
            
            m_oTicket.setActiveCash(m_App.getActiveCashIndex());
            m_oTicket.setDate(new Date()); // Set the edition date.
        }
        
        //executeEvent(m_oTicket, m_oTicketExt.getName(), "ticket.show");
        
        refreshTicket();               
    }
    
    public TicketInfo getActiveTicket() {
        return m_oTicket;
    }
    
    private void refreshTicket() {
        
        CardLayout cl = (CardLayout)(getLayout());
        
        if (m_oTicket == null) {        
            m_jTicketId.setText(null);            
            m_ticketlines.clearTicketLines();
           
            m_jSubtotalEuros.setText(null);
            m_jTaxesEuros.setText(null);
            m_jTotalEuros.setText(null); 
        
            stateToZero();
            
            // Muestro el panel de nulos.
            cl.show(this, "null");  
            resetSouthComponent();

        } else {
            if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
                //Make disable Search and Edit Buttons
                m_jEditLine.setVisible(false);
                m_jList.setVisible(false);
            }
            
            // Refresh ticket taxes
            for (TicketLineInfo line : m_oTicket.getLines()) {
                line.setTaxInfo(taxeslogic.getTaxInfo(line.getProductTaxCategoryID(), m_oTicket.getDate(), m_oTicket.getCustomer()));
            }  
        
            // The ticket name
            m_jTicketId.setText(m_oTicket.getName(m_oTicketExt.getName()));

            // Limpiamos todas las filas y anadimos las del ticket actual
            m_ticketlines.clearTicketLines();

            for (int i = 0; i < m_oTicket.getLinesCount(); i++) {
                m_ticketlines.addTicketLine(m_oTicket.getLine(i));
            }
            printPartialTotals();
            stateToZero();
            
            // Muestro el panel de tickets.
            cl.show(this, "ticket");
            resetSouthComponent();
            
            // activo el tecleador...
            m_jKeyFactory.setText(null);       
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    m_jKeyFactory.requestFocus();
                }
            });
        }
    }
       
    private void printPartialTotals(){
               
        if (m_oTicket.getLinesCount() == 0) {
            m_jSubtotalEuros.setText(null);
            m_jTaxesEuros.setText(null);
            m_jTotalEuros.setText(null);
        } else {
            m_jSubtotalEuros.setText(m_oTicket.printSubTotal());
            m_jTaxesEuros.setText(m_oTicket.printTax());
            m_jTotalEuros.setText(m_oTicket.printTotal());
        }
    }
    
    private void paintTicketLine(int index, TicketLineInfo oLine){
        
        //if (executeEventAndRefresh("ticket.setline", new ScriptArg("index", index), new ScriptArg("line", oLine)) == null) {

            m_oTicket.setLine(index, oLine);
            m_ticketlines.setTicketLine(index, oLine);
            m_ticketlines.setSelectedIndex(index);

            //visorTicketLine(oLine); // Y al visor tambien...
            printPartialTotals();   
            stateToZero();  

            // event receipt
            //executeEventAndRefresh("ticket.change");
        //}
   }

    private void addTicketLine(ProductInfoExt oProduct, double dMul, double dPrice) {   
        
        TaxInfo tax = taxeslogic.getTaxInfo(oProduct.getTaxCategoryID(),  m_oTicket.getDate(), m_oTicket.getCustomer());
        
        addTicketLine(new TicketLineInfo(oProduct, dMul, dPrice, tax, (java.util.Properties) (oProduct.getProperties().clone())));
    }
    
    protected void addTicketLine(TicketLineInfo oLine) {   
       /*Calendar cal=Calendar.getInstance(); 
       int value=cal.get(Calendar.HOUR_OF_DAY);
       System.out.println(String.valueOf(value));
        if((value>=0&&value<=7)||(value>=22&&value<=24))
        {
                if(oLine.getProductName().contains("CUBETA"))
                {
                    oLine.setPrice(oLine.getPrice()+50.00);
                }
        }*/
                
        //if (executeEventAndRefresh("ticket.addline", new ScriptArg("line", oLine)) == null) {
                oLine.setProperty("impreso", "NO");
                /*if(oLine.getProductCategoryID().equals("0"))
                {
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                    Date date = new Date();
                    oLine.setProperty("comentario", dateFormat.format(date));
                }*/
                m_oTicket.addLine(oLine);            
                m_ticketlines.addTicketLine(oLine); // Pintamos la linea en la vista... 
            

            //visorTicketLine(oLine);
            printPartialTotals();   
            stateToZero();  
            
            // event receipt
            //executeEventAndRefresh("ticket.change");
        //}
    }    
    
    private void removeTicketLine(int i){
        
//        if (executeEventAndRefresh("ticket.removeline", new ScriptArg("index", i)) == null) {
        
            
                // Es un producto normal, lo borro.
                m_oTicket.removeLine(i);
                m_ticketlines.removeTicketLine(i); 
                
            

            //visorTicketLine(null); // borro el visor 
            printPartialTotals(); // pinto los totales parciales...                           
            stateToZero(); // Pongo a cero    

            // event receipt
            //executeEventAndRefresh("ticket.change");
        //}
    }
    
    private ProductInfoExt getInputProduct() {
        ProductInfoExt oProduct = new ProductInfoExt(); // Es un ticket
        oProduct.setReference(null);
        oProduct.setCode(null);
        oProduct.setName("");
        oProduct.setTaxCategoryID(((TaxCategoryInfo) taxcategoriesmodel.getSelectedItem()).getID());
        
        oProduct.setPriceSell(includeTaxes(oProduct.getTaxCategoryID(), getInputValue()));
        
        return oProduct;
    }
    
    private double includeTaxes(String tcid, double dValue) {
        if (m_jaddtax.isSelected()) {
            TaxInfo tax = taxeslogic.getTaxInfo(tcid,  m_oTicket.getDate(), m_oTicket.getCustomer());
            double dTaxRate = tax == null ? 0.0 : tax.getRate();           
            return dValue / (1.0 + dTaxRate);      
        } else {
            return dValue;
        }
    }
    
    private double getInputValue() {
        try {
            return Double.parseDouble(m_jPrice.getText());
        } catch (NumberFormatException e){
            return 0.0;
        }
    }

    private double getPorValue() {
        try {
            return Double.parseDouble(m_jPor.getText().substring(1));                
        } catch (NumberFormatException e){
            return 1.0;
        } catch (StringIndexOutOfBoundsException e){
            return 1.0;
        }
    }
    
    private void stateToZero(){
        m_jPor.setText("");
        m_jPrice.setText("");
        m_sBarcode = new StringBuffer();

        m_iNumberStatus = NUMBER_INPUTZERO;
        m_iNumberStatusInput = NUMBERZERO;
        m_iNumberStatusPor = NUMBERZERO;
    }
    
    private void incProductByCode(String sCode) {
    // precondicion: sCode != null
        
        try {
            ProductInfoExt oProduct = dlSales.getProductInfoByCode(sCode);
            if (oProduct == null) {                  
                Toolkit.getDefaultToolkit().beep();                   
                new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noproduct")).show(this);           
                stateToZero();
            } else {
                // Se anade directamente una unidad con el precio y todo
                incProduct(oProduct);
            }
        } catch (BasicException eData) {
            stateToZero();           
            new MessageInf(eData).show(this);           
        }
    }
    
    private void incProductByCodePrice(String sCode, double dPriceSell) {
    // precondicion: sCode != null
        
        try {
            ProductInfoExt oProduct = dlSales.getProductInfoByCode(sCode);
            if (oProduct == null) {                  
                Toolkit.getDefaultToolkit().beep();                   
                new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noproduct")).show(this);           
                stateToZero();
            } else {
                // Se anade directamente una unidad con el precio y todo
                if (m_jaddtax.isSelected()) {
                    // debemos quitarle los impuestos ya que el precio es con iva incluido...
                    TaxInfo tax = taxeslogic.getTaxInfo(oProduct.getTaxCategoryID(),  m_oTicket.getDate(), m_oTicket.getCustomer());
                    addTicketLine(oProduct, 1.0, dPriceSell / (1.0 + tax.getRate()));
                } else {
                    addTicketLine(oProduct, 1.0, dPriceSell);
                }                
            }
        } catch (BasicException eData) {
            stateToZero();
            new MessageInf(eData).show(this);               
        }
    }
    
    private void incProduct(ProductInfoExt prod) {
        
        if (prod.isScale() && m_App.getDeviceScale().existsScale()) {
            try {
                Double value = m_App.getDeviceScale().readWeight();
                if (value != null) {
                    incProduct(value.doubleValue(), prod);
                }
            } catch (ScaleException e) {
                Toolkit.getDefaultToolkit().beep();                
                new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noweight"), e).show(this);           
                stateToZero(); 
            }
        } else {
            // No es un producto que se pese o no hay balanza
            incProduct(1.0, prod);
        }
    }
    
    private void incProduct(double dPor, ProductInfoExt prod) {
        // precondicion: prod != null
        addTicketLine(prod, dPor, prod.getPriceSell());       
    }
       
    protected void buttonTransition(ProductInfoExt prod) {
    // precondicion: prod != null
        
         if (m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERZERO) {
            incProduct(prod);
        } else if (m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO) {
            incProduct(getInputValue(), prod);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }       
    }
    
    private void stateTransition(char cTrans) {

        if (cTrans == '\n') {
            // Codigo de barras introducido
            if (m_sBarcode.length() > 0) {            
                String sCode = m_sBarcode.toString();
                if (sCode.startsWith("c")) {
                    // barcode of a customers card
                    try {
                        CustomerInfoExt newcustomer = dlSales.findCustomerExt(sCode);
                        if (newcustomer == null) {
                            Toolkit.getDefaultToolkit().beep();                   
                            new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.nocustomer")).show(this);           
                        } else {
                            m_oTicket.setCustomer(newcustomer);
                            m_jTicketId.setText(m_oTicket.getName(m_oTicketExt.getName()));
                        }
                    } catch (BasicException e) {
                        Toolkit.getDefaultToolkit().beep();                   
                        new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.nocustomer"), e).show(this);           
                    }
                    stateToZero();
                } else if (sCode.length() == 13 && sCode.startsWith("250")) {
                    // barcode of the other machine
                    ProductInfoExt oProduct = new ProductInfoExt(); // Es un ticket
                    oProduct.setReference(null); // para que no se grabe
                    oProduct.setCode(sCode);
                    oProduct.setName("Ticket " + sCode.substring(3, 7));
                    oProduct.setPriceSell(Double.parseDouble(sCode.substring(7, 12)) / 100);   
                    oProduct.setTaxCategoryID(((TaxCategoryInfo) taxcategoriesmodel.getSelectedItem()).getID());
                    // Se anade directamente una unidad con el precio y todo
                    addTicketLine(oProduct, 1.0, includeTaxes(oProduct.getTaxCategoryID(), oProduct.getPriceSell()));
                } else if (sCode.length() == 13 && sCode.startsWith("210")) {
                    // barcode of a weigth product
                    incProductByCodePrice(sCode.substring(0, 7), Double.parseDouble(sCode.substring(7, 12)) / 100);
                } else {
                    incProductByCode(sCode);
                }
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        } else {
            // otro caracter
            // Esto es para el codigo de barras...
            m_sBarcode.append(cTrans);

            // Esto es para el los productos normales...
            if (cTrans == '\u007f') { 
                stateToZero();

            } else if ((cTrans == '0') 
                    && (m_iNumberStatus == NUMBER_INPUTZERO)) {
                m_jPrice.setText("0");            
            } else if ((cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5' || cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
                    && (m_iNumberStatus == NUMBER_INPUTZERO)) { 
                // Un numero entero
                m_jPrice.setText(Character.toString(cTrans));
                m_iNumberStatus = NUMBER_INPUTINT;    
                m_iNumberStatusInput = NUMBERVALID;
            } else if ((cTrans == '0' || cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5' || cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
                       && (m_iNumberStatus == NUMBER_INPUTINT)) { 
                // Un numero entero
                m_jPrice.setText(m_jPrice.getText() + cTrans);

            } else if (cTrans == '.' && m_iNumberStatus == NUMBER_INPUTZERO) {
                m_jPrice.setText("0.");
                m_iNumberStatus = NUMBER_INPUTZERODEC;            
            } else if (cTrans == '.' && m_iNumberStatus == NUMBER_INPUTINT) {
                m_jPrice.setText(m_jPrice.getText() + ".");
                m_iNumberStatus = NUMBER_INPUTDEC;

            } else if ((cTrans == '0')
                       && (m_iNumberStatus == NUMBER_INPUTZERODEC || m_iNumberStatus == NUMBER_INPUTDEC)) { 
                // Un numero decimal
                m_jPrice.setText(m_jPrice.getText() + cTrans);
            } else if ((cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5' || cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
                       && (m_iNumberStatus == NUMBER_INPUTZERODEC || m_iNumberStatus == NUMBER_INPUTDEC)) { 
                // Un numero decimal
                m_jPrice.setText(m_jPrice.getText() + cTrans);
                m_iNumberStatus = NUMBER_INPUTDEC;
                m_iNumberStatusInput = NUMBERVALID;

            } else if (cTrans == '*' 
                    && (m_iNumberStatus == NUMBER_INPUTINT || m_iNumberStatus == NUMBER_INPUTDEC)) {
                m_jPor.setText("x");
                m_iNumberStatus = NUMBER_PORZERO;            
            } else if (cTrans == '*' 
                    && (m_iNumberStatus == NUMBER_INPUTZERO || m_iNumberStatus == NUMBER_INPUTZERODEC)) {
                m_jPrice.setText("0");
                m_jPor.setText("x");
                m_iNumberStatus = NUMBER_PORZERO;       

            } else if ((cTrans == '0') 
                    && (m_iNumberStatus == NUMBER_PORZERO)) {
                m_jPor.setText("x0");            
            } else if ((cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5' || cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
                    && (m_iNumberStatus == NUMBER_PORZERO)) { 
                // Un numero entero
                m_jPor.setText("x" + Character.toString(cTrans));
                m_iNumberStatus = NUMBER_PORINT;            
                m_iNumberStatusPor = NUMBERVALID;
            } else if ((cTrans == '0' || cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5' || cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
                       && (m_iNumberStatus == NUMBER_PORINT)) { 
                // Un numero entero
                m_jPor.setText(m_jPor.getText() + cTrans);

            } else if (cTrans == '.' && m_iNumberStatus == NUMBER_PORZERO) {
                m_jPor.setText("x0.");
                m_iNumberStatus = NUMBER_PORZERODEC;            
            } else if (cTrans == '.' && m_iNumberStatus == NUMBER_PORINT) {
                m_jPor.setText(m_jPor.getText() + ".");
                m_iNumberStatus = NUMBER_PORDEC;

            } else if ((cTrans == '0')
                       && (m_iNumberStatus == NUMBER_PORZERODEC || m_iNumberStatus == NUMBER_PORDEC)) { 
                // Un numero decimal
                m_jPor.setText(m_jPor.getText() + cTrans);
            } else if ((cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5' || cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
                       && (m_iNumberStatus == NUMBER_PORZERODEC || m_iNumberStatus == NUMBER_PORDEC)) { 
                // Un numero decimal
                m_jPor.setText(m_jPor.getText() + cTrans);
                m_iNumberStatus = NUMBER_PORDEC;            
                m_iNumberStatusPor = NUMBERVALID;  
            
            } else if (cTrans == '\u00a7' 
                    && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO) {
                // Scale button pressed and a number typed as a price
                if (m_App.getDeviceScale().existsScale() && m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
                    try {
                        Double value = m_App.getDeviceScale().readWeight();
                        if (value != null) {
                            ProductInfoExt product = getInputProduct();
                            addTicketLine(product, value.doubleValue(), product.getPriceSell());
                        }
                    } catch (ScaleException e) {
                        Toolkit.getDefaultToolkit().beep();
                        new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noweight"), e).show(this);           
                        stateToZero(); 
                    }
                } else {
                    // No existe la balanza;
                    Toolkit.getDefaultToolkit().beep();
                }
            } else if (cTrans == '\u00a7' 
                    && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERZERO) {
                // Scale button pressed and no number typed.
                int i = m_ticketlines.getSelectedIndex();
                if (i < 0){
                    Toolkit.getDefaultToolkit().beep();
                } else if (m_App.getDeviceScale().existsScale()) {
                    try {
                        Double value = m_App.getDeviceScale().readWeight();
                        if (value != null) {
                            TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
                            newline.setMultiply(value.doubleValue());
                            newline.setPrice(Math.abs(newline.getPrice()));
                            paintTicketLine(i, newline);
                        }
                    } catch (ScaleException e) {
                        // Error de pesada.
                        Toolkit.getDefaultToolkit().beep();
                        new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noweight"), e).show(this);           
                        stateToZero(); 
                    }
                } else {
                    // No existe la balanza;
                    Toolkit.getDefaultToolkit().beep();
                }      
                
            // Add one product more to the selected line
            } else if (cTrans == '+' 
                    && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERZERO) {
                int i = m_ticketlines.getSelectedIndex();
                if (i < 0){
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
                    //If it's a refund + button means one unit less
                    if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND){
                        newline.setMultiply(newline.getMultiply() - 1.0);
                        paintTicketLine(i, newline);                   
                    }
                    else {
                        // add one unit to the selected line
                        newline.setMultiply(newline.getMultiply() + 1.0);
                        paintTicketLine(i, newline); 
                    }
                }

            // Delete one product of the selected line
            } else if (cTrans == '-' 
                    && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERZERO
                    && m_App.getAppUserView().getUser().hasPermission("sales.EditLines")&&this.m_App.getAppUserView().getUser().getRole().equals("0")) {
                
                int i = m_ticketlines.getSelectedIndex();
                if (i < 0){
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
                    //If it's a refund - button means one unit more
                    if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND){
                        newline.setMultiply(newline.getMultiply() + 1.0);
                        if (newline.getMultiply() >= 0) {
                            removeTicketLine(i);
                        } else {
                            paintTicketLine(i, newline);
                        }
                    } else {
                        // substract one unit to the selected line
                        newline.setMultiply(newline.getMultiply() - 1.0);
                        if (newline.getMultiply() <= 0.0) {                   
                            removeTicketLine(i); // elimino la linea
                        } else {
                            paintTicketLine(i, newline);                   
                        }
                    }
                }

            // Set n products to the selected line
            } else if (cTrans == '+' 
                    && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERVALID) {
                int i = m_ticketlines.getSelectedIndex();
                if (i < 0){
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    double dPor = getPorValue();
                    TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i)); 
                    if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
                        newline.setMultiply(-dPor);
                        newline.setPrice(Math.abs(newline.getPrice()));
                        paintTicketLine(i, newline);                
                    } else {
                        newline.setMultiply(dPor);
                        newline.setPrice(Math.abs(newline.getPrice()));
                        paintTicketLine(i, newline);
                    }
                }

            // Set n negative products to the selected line
            } else if (cTrans == '-' 
                    && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERVALID
                    && m_App.getAppUserView().getUser().hasPermission("sales.EditLines")&&this.m_App.getAppUserView().getUser().getRole().equals("0")) {
                
                int i = m_ticketlines.getSelectedIndex();
                if (i < 0){
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    double dPor = getPorValue();
                    TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
                    if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_NORMAL) {
                        newline.setMultiply(dPor);
                        newline.setPrice(-Math.abs(newline.getPrice()));
                        paintTicketLine(i, newline);
                    }           
                }

            // Anadimos 1 producto
            } else if (cTrans == '+' 
                    && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO
                    && m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
                ProductInfoExt product = getInputProduct();
                addTicketLine(product, 1.0, product.getPriceSell());
                
            // Anadimos 1 producto con precio negativo
            } else if (cTrans == '-' 
                    && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO
                    && m_App.getAppUserView().getUser().hasPermission("sales.EditLines")&&this.m_App.getAppUserView().getUser().getRole().equals("0")) {
                ProductInfoExt product = getInputProduct();
                addTicketLine(product, 1.0, -product.getPriceSell());

            // Anadimos n productos
            } else if (cTrans == '+' 
                    && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERVALID
                    && m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
                ProductInfoExt product = getInputProduct();
                addTicketLine(product, getPorValue(), product.getPriceSell());

            // Anadimos n productos con precio negativo ?
            } else if (cTrans == '-' 
                    && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERVALID
                    && m_App.getAppUserView().getUser().hasPermission("sales.EditLines")&&this.m_App.getAppUserView().getUser().getRole().equals("0")) {
                ProductInfoExt product = getInputProduct();
                addTicketLine(product, getPorValue(), -product.getPriceSell());

            // Totals() Igual;
            } else if (cTrans == ' ' || cTrans == '=') {
                if (m_oTicket.getLinesCount() > 0&&(this.m_App.getAppUserView().getUser().getRole().equals("0")||this.m_App.getAppUserView().getUser().getRole().equals("1")))
                {
                    if (closeTicket(m_oTicket, m_oTicketExt)) {
                        // Ends edition of current receipt
                        m_ticketsbag.deleteTicket();  
                    } else {
                        // repaint current ticket
                        refreshTicket();
                    }
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        }
    }
    
    private boolean closeTicket(TicketInfo ticket, Place ticketext) {
        /*int numlines = this.m_oTicket.getLinesCount();
        boolean isOK = true;
        for (int i = 0; i < numlines; i++)
        {
            if(this.m_oTicket.getLine(i).getProperty("impreso", "NO")=="NO")
            {
                if(!this.m_oTicket.getLine(i).getProductCode().equals("0"))
                {
                    isOK= false;
                    break;
                }
            }
        }
        if(isOK == false)
        {
            JOptionPane.showMessageDialog(this, "No puede realizar la venta sin enviar comandas.");
            return false;
        }*/
        boolean resultok = false;
        if (m_App.getAppUserView().getUser().hasPermission("sales.Total")) {  
            
            try {
                // reset the payment info
                taxeslogic.calculateTaxes(ticket);
                if (ticket.getTotal()>=0.0){
                    ticket.resetPayments(); //Only reset if is sale
                }
                
                //if (executeEvent(ticket, ticketext, "ticket.total") == null) {

                    // Muestro el total
                    printTicket("Printer.TicketTotal", ticket, ticketext);
                    
                    
                    // Select the Payments information
                    JPaymentSelect paymentdialog = ticket.getTicketType() == TicketInfo.RECEIPT_NORMAL
                            ? paymentdialogreceipt
                            : paymentdialogrefund;
                    paymentdialog.setPrintSelected("true".equals(m_jbtnconfig.getProperty("printselected", "true")));

                    paymentdialog.setTransactionID(ticket.getTransactionID());

                    if (paymentdialog.showDialog(ticket.getTotal(), ticket.getCustomer())) {

                        // assign the payments selected and calculate taxes.         
                        ticket.setPayments(paymentdialog.getSelectedPayments());

                        // Asigno los valores definitivos del ticket...
                        ticket.setUser(dlSales.getUserById(dlSales.getUser(ticketext.getName()))); // El usuario que lo cobra
                        ticket.setActiveCash(m_App.getActiveCashIndex());
                        ticket.setDate(new Date()); // Le pongo la fecha de cobro
                        ticket.setProperty("mesa", ticketext.getName());
                        ticket.setProperty("piso", ticketext.getNameFloor());
                        ticket.setProperty("propina", com.openbravo.format.Formats.CURRENCY.formatValue(ticket.getTotal()*0.10));
                        //if (executeEvent(ticket, ticketext, "ticket.save") == null) {
                            // Save the receipt and assign a receipt number
                            try {
                                dlSales.saveTicket(ticket,ticketext, m_App.getInventoryLocation());                       
                            } catch (BasicException eData) {
                                MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.nosaveticket"), eData);
                                msg.show(this);
                            }

                            //executeEvent(ticket, ticketext, "ticket.close", new ScriptArg("print", paymentdialog.isPrintSelected()));

                            // Print receipt.
                            printTicket(paymentdialog.isPrintSelected()
                                    ? "Printer.Ticket"
                                    : "Printer.Ticket2", ticket, ticketext);
                            resultok = true;
                        //}
                    }
                //}
            } catch (TaxesException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotcalculatetaxes"));
                msg.show(this);
                resultok = false;
            } catch (BasicException ex) {
                Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            // reset the payment info
            m_oTicket.resetTaxes();
            m_oTicket.resetPayments();
        }
        
        return resultok;        
    }
       
    private void printTicket(String sresourcename, TicketInfo ticket, Place ticketext) {

        String sresource = dlSystem.getResourceAsXML(sresourcename);
        if (sresource == null) {
            MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotprintticket"));
            msg.show(JPanelTicket.this);
        } else {
            try {
                ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
                script.put("ticket", ticket);
                script.put("place", ticketext);
                m_TTP.printTicket(script.eval(sresource).toString());
            } catch (ScriptException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotprintticket"), e);
                msg.show(JPanelTicket.this);
            } catch (TicketPrinterException e) {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotprintticket"), e);
                msg.show(JPanelTicket.this);
            }
        }
    }
    public void printTicket(String resource) {
        printTicket(resource, m_oTicket, m_oTicketExt);
    }
    
    public String getResourceAsXML(String sresourcename) {
        return dlSystem.getResourceAsXML(sresourcename);
    }

    public BufferedImage getResourceAsImage(String sresourcename) {
        return dlSystem.getResourceAsImage(sresourcename);
    }
    
    private void setSelectedIndex(int i) {
        
        if (i >= 0 && i < m_oTicket.getLinesCount()) {
            m_ticketlines.setSelectedIndex(i);
        } else if (m_oTicket.getLinesCount() > 0) {
            m_ticketlines.setSelectedIndex(m_oTicket.getLinesCount() - 1);
        }    
    }
/** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the FormEditor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        m_jTaxesEuros = new javax.swing.JLabel();
        m_jLblTotalEuros2 = new javax.swing.JLabel();
        m_jLblTotalEuros3 = new javax.swing.JLabel();
        m_jSubtotalEuros = new javax.swing.JLabel();
        m_jTax = new javax.swing.JComboBox();
        m_jaddtax = new javax.swing.JToggleButton();
        m_jPor = new javax.swing.JLabel();
        m_jEnter = new javax.swing.JButton();
        m_jDown = new javax.swing.JButton();
        m_jList = new javax.swing.JButton();
        btnCustomer5 = new javax.swing.JButton();
        btnCustomer6 = new javax.swing.JButton();
        btnCustomer4 = new javax.swing.JButton();
        jEditAttributes1 = new javax.swing.JButton();
        jEditAttributes2 = new javax.swing.JButton();
        m_jPanContainer = new javax.swing.JPanel();
        m_jOptions = new javax.swing.JPanel();
        m_jButtons = new javax.swing.JPanel();
        m_jPanelBag = new javax.swing.JPanel();
        jEditAttributes = new javax.swing.JButton();
        m_jTicketId = new javax.swing.JLabel();
        m_jDelete = new javax.swing.JButton();
        m_jEditLine = new javax.swing.JButton();
        btnCustomer = new javax.swing.JButton();
        btnCustomer2 = new javax.swing.JButton();
        btnCustomer1 = new javax.swing.JButton();
        btnSplit = new javax.swing.JButton();
        btnCustomer3 = new javax.swing.JButton();
        btnCustomer7 = new javax.swing.JButton();
        m_jUp = new javax.swing.JButton();
        m_jPrice = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        m_jbtnScale = new javax.swing.JButton();
        m_jPanelScripts = new javax.swing.JPanel();
        m_jButtonsExt = new javax.swing.JPanel();
        m_jPanTicket = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        m_jPanelCentral = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        m_jPanTotals = new javax.swing.JPanel();
        m_jTotalEuros = new javax.swing.JLabel();
        m_jLblTotalEuros1 = new javax.swing.JLabel();
        m_jContEntries = new javax.swing.JPanel();
        m_jPanEntries = new javax.swing.JPanel();
        m_jNumberKeys = new com.openbravo.beans.JNumberKeys();
        jPanel9 = new javax.swing.JPanel();
        m_jKeyFactory = new javax.swing.JTextField();
        catcontainer = new javax.swing.JPanel();

        m_jTaxesEuros.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        m_jTaxesEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jTaxesEuros.setOpaque(true);
        m_jTaxesEuros.setPreferredSize(new java.awt.Dimension(150, 25));
        m_jTaxesEuros.setRequestFocusEnabled(false);

        m_jLblTotalEuros2.setText(AppLocal.getIntString("label.taxcash")); // NOI18N

        m_jLblTotalEuros3.setText(AppLocal.getIntString("label.subtotalcash")); // NOI18N

        m_jSubtotalEuros.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        m_jSubtotalEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jSubtotalEuros.setOpaque(true);
        m_jSubtotalEuros.setPreferredSize(new java.awt.Dimension(150, 25));
        m_jSubtotalEuros.setRequestFocusEnabled(false);

        m_jTax.setFocusable(false);
        m_jTax.setRequestFocusEnabled(false);

        m_jaddtax.setText("+");
        m_jaddtax.setFocusPainted(false);
        m_jaddtax.setFocusable(false);
        m_jaddtax.setRequestFocusEnabled(false);

        m_jPor.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        m_jPor.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jPor.setOpaque(true);
        m_jPor.setPreferredSize(new java.awt.Dimension(22, 22));
        m_jPor.setRequestFocusEnabled(false);

        m_jEnter.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/barcode.png"))); // NOI18N
        m_jEnter.setFocusPainted(false);
        m_jEnter.setFocusable(false);
        m_jEnter.setRequestFocusEnabled(false);
        m_jEnter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jEnterActionPerformed(evt);
            }
        });

        m_jDown.setText("Barra");
        m_jDown.setFocusPainted(false);
        m_jDown.setFocusable(false);
        m_jDown.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jDown.setMaximumSize(new java.awt.Dimension(58, 42));
        m_jDown.setMinimumSize(new java.awt.Dimension(58, 42));
        m_jDown.setPreferredSize(new java.awt.Dimension(68, 42));
        m_jDown.setRequestFocusEnabled(false);
        m_jDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jDownActionPerformed(evt);
            }
        });

        m_jList.setText("Cocina");
        m_jList.setFocusPainted(false);
        m_jList.setFocusable(false);
        m_jList.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jList.setMaximumSize(new java.awt.Dimension(64, 42));
        m_jList.setMinimumSize(new java.awt.Dimension(64, 42));
        m_jList.setPreferredSize(new java.awt.Dimension(78, 42));
        m_jList.setRequestFocusEnabled(false);
        m_jList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jListActionPerformed(evt);
            }
        });

        btnCustomer5.setText("Barra 2");
        btnCustomer5.setFocusPainted(false);
        btnCustomer5.setFocusable(false);
        btnCustomer5.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnCustomer5.setMaximumSize(new java.awt.Dimension(54, 42));
        btnCustomer5.setMinimumSize(new java.awt.Dimension(54, 42));
        btnCustomer5.setPreferredSize(new java.awt.Dimension(83, 62));
        btnCustomer5.setRequestFocusEnabled(false);
        btnCustomer5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCustomer5ActionPerformed(evt);
            }
        });

        btnCustomer6.setText("--");
        btnCustomer6.setFocusPainted(false);
        btnCustomer6.setFocusable(false);
        btnCustomer6.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnCustomer6.setMaximumSize(new java.awt.Dimension(54, 42));
        btnCustomer6.setMinimumSize(new java.awt.Dimension(54, 42));
        btnCustomer6.setPreferredSize(new java.awt.Dimension(54, 62));
        btnCustomer6.setRequestFocusEnabled(false);
        btnCustomer6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCustomer6ActionPerformed(evt);
            }
        });

        btnCustomer4.setText("Coc");
        btnCustomer4.setFocusPainted(false);
        btnCustomer4.setFocusable(false);
        btnCustomer4.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnCustomer4.setMaximumSize(new java.awt.Dimension(54, 42));
        btnCustomer4.setMinimumSize(new java.awt.Dimension(54, 42));
        btnCustomer4.setPreferredSize(new java.awt.Dimension(63, 62));
        btnCustomer4.setRequestFocusEnabled(false);
        btnCustomer4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCustomer4ActionPerformed(evt);
            }
        });

        jEditAttributes1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/edit_group.png"))); // NOI18N
        jEditAttributes1.setFocusPainted(false);
        jEditAttributes1.setFocusable(false);
        jEditAttributes1.setMargin(new java.awt.Insets(8, 14, 8, 14));
        jEditAttributes1.setPreferredSize(new java.awt.Dimension(54, 62));
        jEditAttributes1.setRequestFocusEnabled(false);
        jEditAttributes1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEditAttributes1ActionPerformed(evt);
            }
        });

        jEditAttributes2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/kdmconfig.png"))); // NOI18N
        jEditAttributes2.setFocusPainted(false);
        jEditAttributes2.setFocusable(false);
        jEditAttributes2.setMargin(new java.awt.Insets(8, 14, 8, 14));
        jEditAttributes2.setPreferredSize(new java.awt.Dimension(54, 62));
        jEditAttributes2.setRequestFocusEnabled(false);
        jEditAttributes2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEditAttributes2ActionPerformed(evt);
            }
        });

        setBackground(new java.awt.Color(255, 204, 153));
        setLayout(new java.awt.CardLayout());

        m_jPanContainer.setLayout(new java.awt.BorderLayout());

        m_jOptions.setPreferredSize(new java.awt.Dimension(1263, 72));
        m_jOptions.setLayout(new java.awt.BorderLayout());

        m_jButtons.setPreferredSize(new java.awt.Dimension(900, 72));

        m_jPanelBag.setLayout(new java.awt.BorderLayout());
        m_jButtons.add(m_jPanelBag);

        jEditAttributes.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/pay.png"))); // NOI18N
        jEditAttributes.setFocusPainted(false);
        jEditAttributes.setFocusable(false);
        jEditAttributes.setMargin(new java.awt.Insets(8, 14, 8, 14));
        jEditAttributes.setPreferredSize(new java.awt.Dimension(64, 62));
        jEditAttributes.setRequestFocusEnabled(false);
        jEditAttributes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEditAttributesActionPerformed(evt);
            }
        });
        m_jButtons.add(jEditAttributes);

        m_jTicketId.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        m_jTicketId.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jTicketId.setOpaque(true);
        m_jTicketId.setPreferredSize(new java.awt.Dimension(120, 62));
        m_jTicketId.setRequestFocusEnabled(false);
        m_jButtons.add(m_jTicketId);

        m_jDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/locationbar_erase.png"))); // NOI18N
        m_jDelete.setFocusPainted(false);
        m_jDelete.setFocusable(false);
        m_jDelete.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jDelete.setPreferredSize(new java.awt.Dimension(64, 62));
        m_jDelete.setRequestFocusEnabled(false);
        m_jDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jDeleteActionPerformed(evt);
            }
        });
        m_jButtons.add(m_jDelete);

        m_jEditLine.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/color_line.png"))); // NOI18N
        m_jEditLine.setFocusPainted(false);
        m_jEditLine.setFocusable(false);
        m_jEditLine.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jEditLine.setPreferredSize(new java.awt.Dimension(64, 62));
        m_jEditLine.setRequestFocusEnabled(false);
        m_jEditLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jEditLineActionPerformed(evt);
            }
        });
        m_jButtons.add(m_jEditLine);

        btnCustomer.setText("%");
        btnCustomer.setFocusPainted(false);
        btnCustomer.setFocusable(false);
        btnCustomer.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnCustomer.setMaximumSize(new java.awt.Dimension(54, 42));
        btnCustomer.setMinimumSize(new java.awt.Dimension(54, 42));
        btnCustomer.setPreferredSize(new java.awt.Dimension(64, 62));
        btnCustomer.setRequestFocusEnabled(false);
        btnCustomer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCustomerActionPerformed(evt);
            }
        });
        m_jButtons.add(btnCustomer);

        btnCustomer2.setText("+");
        btnCustomer2.setFocusPainted(false);
        btnCustomer2.setFocusable(false);
        btnCustomer2.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnCustomer2.setMaximumSize(new java.awt.Dimension(54, 42));
        btnCustomer2.setMinimumSize(new java.awt.Dimension(54, 42));
        btnCustomer2.setPreferredSize(new java.awt.Dimension(64, 62));
        btnCustomer2.setRequestFocusEnabled(false);
        btnCustomer2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCustomer2ActionPerformed(evt);
            }
        });
        m_jButtons.add(btnCustomer2);

        btnCustomer1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/fileprint.png"))); // NOI18N
        btnCustomer1.setToolTipText("");
        btnCustomer1.setFocusPainted(false);
        btnCustomer1.setFocusable(false);
        btnCustomer1.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnCustomer1.setPreferredSize(new java.awt.Dimension(64, 62));
        btnCustomer1.setRequestFocusEnabled(false);
        btnCustomer1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCustomer1ActionPerformed(evt);
            }
        });
        m_jButtons.add(btnCustomer1);

        btnSplit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/editcut.png"))); // NOI18N
        btnSplit.setFocusPainted(false);
        btnSplit.setFocusable(false);
        btnSplit.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnSplit.setMaximumSize(new java.awt.Dimension(54, 42));
        btnSplit.setMinimumSize(new java.awt.Dimension(54, 42));
        btnSplit.setPreferredSize(new java.awt.Dimension(64, 62));
        btnSplit.setRequestFocusEnabled(false);
        btnSplit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSplitActionPerformed(evt);
            }
        });
        m_jButtons.add(btnSplit);

        btnCustomer3.setText("BEB");
        btnCustomer3.setToolTipText("");
        btnCustomer3.setFocusPainted(false);
        btnCustomer3.setFocusable(false);
        btnCustomer3.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnCustomer3.setMaximumSize(new java.awt.Dimension(54, 42));
        btnCustomer3.setMinimumSize(new java.awt.Dimension(54, 42));
        btnCustomer3.setPreferredSize(new java.awt.Dimension(64, 62));
        btnCustomer3.setRequestFocusEnabled(false);
        btnCustomer3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCustomer3ActionPerformed(evt);
            }
        });
        m_jButtons.add(btnCustomer3);

        btnCustomer7.setText("COM");
        btnCustomer7.setFocusPainted(false);
        btnCustomer7.setFocusable(false);
        btnCustomer7.setMargin(new java.awt.Insets(8, 14, 8, 14));
        btnCustomer7.setMaximumSize(new java.awt.Dimension(54, 42));
        btnCustomer7.setMinimumSize(new java.awt.Dimension(54, 42));
        btnCustomer7.setPreferredSize(new java.awt.Dimension(64, 62));
        btnCustomer7.setRequestFocusEnabled(false);
        btnCustomer7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCustomer7ActionPerformed(evt);
            }
        });
        m_jButtons.add(btnCustomer7);

        m_jUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/apply.png"))); // NOI18N
        m_jUp.setFocusPainted(false);
        m_jUp.setFocusable(false);
        m_jUp.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jUp.setMaximumSize(new java.awt.Dimension(54, 42));
        m_jUp.setMinimumSize(new java.awt.Dimension(54, 42));
        m_jUp.setPreferredSize(new java.awt.Dimension(64, 62));
        m_jUp.setRequestFocusEnabled(false);
        m_jUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jUpActionPerformed(evt);
            }
        });
        m_jButtons.add(m_jUp);

        m_jOptions.add(m_jButtons, java.awt.BorderLayout.LINE_START);

        m_jPrice.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        m_jPrice.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        m_jPrice.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jPrice.setOpaque(true);
        m_jPrice.setPreferredSize(new java.awt.Dimension(80, 22));
        m_jPrice.setRequestFocusEnabled(false);
        m_jOptions.add(m_jPrice, java.awt.BorderLayout.LINE_END);

        m_jPanContainer.add(m_jOptions, java.awt.BorderLayout.NORTH);

        m_jbtnScale.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/ark216.png"))); // NOI18N
        m_jbtnScale.setText(AppLocal.getIntString("button.scale")); // NOI18N
        m_jbtnScale.setFocusPainted(false);
        m_jbtnScale.setFocusable(false);
        m_jbtnScale.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jbtnScale.setRequestFocusEnabled(false);
        m_jbtnScale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jbtnScaleActionPerformed(evt);
            }
        });
        jPanel1.add(m_jbtnScale);

        m_jPanContainer.add(jPanel1, java.awt.BorderLayout.CENTER);

        m_jPanelScripts.setLayout(new java.awt.BorderLayout());

        m_jButtonsExt.setLayout(new javax.swing.BoxLayout(m_jButtonsExt, javax.swing.BoxLayout.LINE_AXIS));
        m_jPanelScripts.add(m_jButtonsExt, java.awt.BorderLayout.LINE_END);

        m_jPanContainer.add(m_jPanelScripts, java.awt.BorderLayout.LINE_END);

        m_jPanTicket.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        m_jPanTicket.setLayout(new java.awt.BorderLayout());

        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5));
        jPanel2.setLayout(new java.awt.GridLayout(0, 1, 5, 5));
        jPanel5.add(jPanel2, java.awt.BorderLayout.NORTH);

        m_jPanTicket.add(jPanel5, java.awt.BorderLayout.LINE_END);

        m_jPanelCentral.setLayout(new java.awt.BorderLayout());

        jPanel4.setLayout(new java.awt.BorderLayout());

        m_jPanTotals.setLayout(new java.awt.GridBagLayout());

        m_jTotalEuros.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        m_jTotalEuros.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        m_jTotalEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        m_jTotalEuros.setOpaque(true);
        m_jTotalEuros.setPreferredSize(new java.awt.Dimension(150, 25));
        m_jTotalEuros.setRequestFocusEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        m_jPanTotals.add(m_jTotalEuros, gridBagConstraints);

        m_jLblTotalEuros1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        m_jLblTotalEuros1.setText(AppLocal.getIntString("label.totalcash")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        m_jPanTotals.add(m_jLblTotalEuros1, gridBagConstraints);

        jPanel4.add(m_jPanTotals, java.awt.BorderLayout.LINE_END);

        m_jPanelCentral.add(jPanel4, java.awt.BorderLayout.SOUTH);

        m_jPanTicket.add(m_jPanelCentral, java.awt.BorderLayout.CENTER);

        m_jPanContainer.add(m_jPanTicket, java.awt.BorderLayout.CENTER);

        m_jContEntries.setLayout(new java.awt.BorderLayout());

        m_jPanEntries.setLayout(new javax.swing.BoxLayout(m_jPanEntries, javax.swing.BoxLayout.Y_AXIS));

        m_jNumberKeys.setPreferredSize(new java.awt.Dimension(249, 280));
        m_jNumberKeys.addJNumberEventListener(new com.openbravo.beans.JNumberEventListener() {
            public void keyPerformed(com.openbravo.beans.JNumberEvent evt) {
                m_jNumberKeysKeyPerformed(evt);
            }
        });
        m_jPanEntries.add(m_jNumberKeys);

        jPanel9.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel9.setLayout(new java.awt.GridBagLayout());
        m_jPanEntries.add(jPanel9);

        m_jKeyFactory.setBackground(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        m_jKeyFactory.setForeground(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        m_jKeyFactory.setBorder(null);
        m_jKeyFactory.setCaretColor(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        m_jKeyFactory.setPreferredSize(new java.awt.Dimension(1, 1));
        m_jKeyFactory.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                m_jKeyFactoryKeyTyped(evt);
            }
        });
        m_jPanEntries.add(m_jKeyFactory);

        m_jContEntries.add(m_jPanEntries, java.awt.BorderLayout.NORTH);

        m_jPanContainer.add(m_jContEntries, java.awt.BorderLayout.LINE_END);

        catcontainer.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        catcontainer.setLayout(new java.awt.BorderLayout());
        m_jPanContainer.add(catcontainer, java.awt.BorderLayout.SOUTH);

        add(m_jPanContainer, "ticket");
    }// </editor-fold>//GEN-END:initComponents

    private void m_jbtnScaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jbtnScaleActionPerformed

        stateTransition('\u00a7');
        
    }//GEN-LAST:event_m_jbtnScaleActionPerformed

    private void m_jEnterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jEnterActionPerformed

        stateTransition('\n');

    }//GEN-LAST:event_m_jEnterActionPerformed

    private void m_jNumberKeysKeyPerformed(com.openbravo.beans.JNumberEvent evt) {//GEN-FIRST:event_m_jNumberKeysKeyPerformed

        stateTransition(evt.getKey());

    }//GEN-LAST:event_m_jNumberKeysKeyPerformed

    private void m_jKeyFactoryKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_m_jKeyFactoryKeyTyped

        m_jKeyFactory.setText(null);
        stateTransition(evt.getKeyChar());

    }//GEN-LAST:event_m_jKeyFactoryKeyTyped

    private void m_jUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jUpActionPerformed

    if (this.m_oTicket.getLinesCount() > 0)//&&m_App.getAppUserView().getUser().getRole().equals("0"))
    {
      if (closeTicket(this.m_oTicket, this.m_oTicketExt)) {
        this.m_ticketsbag.deleteTicket();
      } else {
        refreshTicket();
      }
    }
    else {
      Toolkit.getDefaultToolkit().beep();
    }
    }//GEN-LAST:event_m_jUpActionPerformed

    private void m_jDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jDownActionPerformed
    int c = 0;
    for (TicketLineInfo tli : this.m_oTicket.getLines()) {
      try
      {
        ProductInfoExt productInfo = this.dlSales.getProductInfo(tli.getProductID());
        if (productInfo.isScale())
        {
          tli.setTipo("bebida");
          tli.setProperty("bebida", "bebida");
          c++;
        }
      }
      catch (BasicException ex)
      {
        Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    if (c > 0)
    {
      printTicket("Printer.TicketBarra", this.m_oTicket, this.m_oTicketExt);
      for (TicketLineInfo tli : this.m_oTicket.getLines()) {
        try
        {
          ProductInfoExt productInfo = this.dlSales.getProductInfo(tli.getProductID());
          if (productInfo.isScale()) {
            tli.setImpreso("SI");
          }
        }
        catch (BasicException ex)
        {
          Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      refreshTicket();
    }
    }//GEN-LAST:event_m_jDownActionPerformed

    private void m_jListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jListActionPerformed
        int c = 0;
    for (TicketLineInfo tli : this.m_oTicket.getLines()) {
      try
      {
        ProductInfoExt productInfo = this.dlSales.getProductInfo(tli.getProductID());
        if (!productInfo.isScale())
        {
          tli.setTipo("comida");
          tli.setProperty("comida", "comida");
          c++;
        }
      }
      catch (BasicException ex)
      {
        Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    if (c > 0)
    {
      printTicket("Printer.TicketCocina", this.m_oTicket, this.m_oTicketExt);
      for (TicketLineInfo tli : this.m_oTicket.getLines()) {
        try
        {
          ProductInfoExt productInfo = this.dlSales.getProductInfo(tli.getProductID());
          if (!productInfo.isScale())
          {
            tli.setImpreso("SI");
            tli.setProperty("comida", "comida");
          }
        }
        catch (BasicException ex)
        {
          Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      refreshTicket();
    }
    }//GEN-LAST:event_m_jListActionPerformed
private void performCustomerAdd(String id, String nombre, String direccion, String referencia, String telefono, String celular, String observaciones, String deuda)
    throws BasicException
  {
      new StaticSentence(this.m_App.getSession(), "INSERT INTO CUSTOMERS (ID, SEARCHKEY, NAME, ADDRESS, ADDRESS2, PHONE, PHONE2, NOTES, MAXDEBT) VALUES (?, ?, ?, ?, ?, ?, ?, ?,?)", new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING })).exec(new Object[] { id, id, nombre, direccion, referencia, telefono, celular, observaciones, deuda });
    
    this.m_oTicket.setCustomer(this.dlSales.loadCustomerExt(id));
  
  }
private void performCustomerUpdate(String id, String nombre, String direccion, String referencia, String telefono, String celular, String observaciones, String deuda)
    throws BasicException
  {
    new StaticSentence(this.m_App.getSession(), "UPDATE CUSTOMERS SET NAME = ?, ADDRESS=?, ADDRESS2=?, PHONE=?, PHONE2=?, NOTES=?, MAXDEBT=? WHERE ID=?", new SerializerWriteBasic(new Datas[] { Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING })).exec(new Object[] { nombre, direccion, referencia, telefono, celular, observaciones,deuda, id });
    
    this.m_oTicket.setCustomer(this.dlSales.loadCustomerExt(id));
  }
    private void jEditAttributes1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jEditAttributes1ActionPerformed
    ArrayList mensajeList = new ArrayList();
    
    mensajeList.add(new JLabel("Nombre*: "));
    JTextField nombre = new JTextField();
    mensajeList.add(nombre);
    
    mensajeList.add(new JLabel("Direccion*: "));
    JTextField direccion = new JTextField();
    mensajeList.add(direccion);
    
    mensajeList.add(new JLabel("Referencia: "));
    JTextField referencia = new JTextField();
    mensajeList.add(referencia);
    
    mensajeList.add(new JLabel("Telefono*: "));
    JTextField telefono = new JTextField();
    mensajeList.add(telefono);
    
    mensajeList.add(new JLabel("Celular: "));
    JTextField celular = new JTextField();
    mensajeList.add(celular);
    
    mensajeList.add(new JLabel("Deuda Maxima: "));
    JTextField deuda = new JTextField("0.00");
    mensajeList.add(deuda);
    /*
    deuda.addKeyListener(new KeyAdapter()
    {
      public void keyTyped(KeyEvent e)
      {
        char caracter = e.getKeyChar();
        if (((caracter < '0') || (caracter > '9')||(caracter > '.')) && (caracter != '\b')) {
          e.consume();
        }
      }
    });*/
    
    telefono.addKeyListener(new KeyAdapter()
    {
      public void keyTyped(KeyEvent e)
      {
        char caracter = e.getKeyChar();
        if (((caracter < '0') || (caracter > '9')) && (caracter != '\b')) {
          e.consume();
        }
      }
    });
    mensajeList.add(new JLabel("Observaciones: "));
    JTextField observaciones = new JTextField();
    mensajeList.add(observaciones);
    
    Object[] objetosList2 = mensajeList.toArray();
    String[] opciones2 = { "Aceptar", "Cancelar" };
    objetosList2 = mensajeList.toArray();
    int respuesta2 = JOptionPane.showOptionDialog(null, objetosList2, "Captura de informacion del cliente.", 0, 3, null, opciones2, nombre);
    if (respuesta2 == 0) {
      if ((!nombre.getText().equals("")) && (!direccion.getText().equals("")) && (!telefono.getText().equals("")))
      {
        try
        {
          if (!this.dlSales.getCustomer(telefono.getText())) {
            performCustomerAdd(telefono.getText(), nombre.getText(), direccion.getText(), referencia.getText(), telefono.getText(), celular.getText(), observaciones.getText(), deuda.getText());
          } else {
            JOptionPane.showMessageDialog(null, "Este cliente ya esta registrado.", "Error", 1);
          }
        }
        catch (BasicException e) {}
        refreshTicket();
      }
      else
      {
        JOptionPane.showMessageDialog(null, "No se puede agregar al cliente, faltan campos por ingresar", "Error", 1);
      }
    }
    }//GEN-LAST:event_jEditAttributes1ActionPerformed

    private void jEditAttributes2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jEditAttributes2ActionPerformed
    JCustomerFinder finder = JCustomerFinder.getCustomerFinder(this, this.dlCustomers);
    finder.search(this.m_oTicket.getCustomer());
    finder.setVisible(true);
    if (finder.getSelectedCustomer() != null) {
      try
      {
        CustomerInfoExt temp = this.dlSales.findCustomerExtById(finder.getSelectedCustomer().getId());
        ArrayList mensajeList = new ArrayList();
        mensajeList.add(new JLabel("Nombre*: "));
        JTextField nombre = new JTextField(temp.getName());
        mensajeList.add(nombre);
        mensajeList.add(new JLabel("Direccion*: "));
        JTextField direccion = new JTextField(temp.getAddress());
        mensajeList.add(direccion);
        mensajeList.add(new JLabel("Referencia: "));
        JTextField referencia = new JTextField(temp.getAddress2());
        mensajeList.add(referencia);
        mensajeList.add(new JLabel("Telefono*: "));
        JTextField telefono = new JTextField(temp.getPhone());
        telefono.setEnabled(false);
        mensajeList.add(telefono);
        mensajeList.add(new JLabel("Celular: "));
        JTextField celular = new JTextField(temp.getPhone2());
        mensajeList.add(celular);
        
        mensajeList.add(new JLabel("Deuda maxima: "));
        JTextField deuda = new JTextField(temp.getMaxdebt().toString());
        mensajeList.add(deuda);
        
        /*deuda.addKeyListener(new KeyAdapter()
        {
          public void keyTyped(KeyEvent e)
          {
            char caracter = e.getKeyChar();
            if (((caracter < '0') || (caracter > '9')|| (caracter > '.')) && (caracter != '\b')) {
              e.consume();
            }
          }
        });
        telefono.addKeyListener(new KeyAdapter()
        {
          public void keyTyped(KeyEvent e)
          {
            char caracter = e.getKeyChar();
            if (((caracter < '0') || (caracter > '9')) && (caracter != '\b')) {
              e.consume();
            }
          }
        });*/
        mensajeList.add(new JLabel("Observaciones: "));
        JTextField observaciones = new JTextField(temp.getNotes());
        mensajeList.add(observaciones);
        Object[] objetosList2 = mensajeList.toArray();
        String[] opciones2 = { "Aceptar", "Cancelar" };
        objetosList2 = mensajeList.toArray();
        int respuesta2 = JOptionPane.showOptionDialog(null, objetosList2, "Captura de informacion del cliente.", 0, 3, null, opciones2, nombre);
        if (respuesta2 == 0)
        {
          try
          {
            performCustomerUpdate(telefono.getText(), nombre.getText(), direccion.getText(), referencia.getText(), telefono.getText(), celular.getText(), observaciones.getText(),deuda.getText());
          }
          catch (BasicException e) {}
          refreshTicket();
        }
        else
        {
          this.m_oTicket.setCustomer(null);
        }
      }
      catch (BasicException ex) {}
    } else {
      this.m_oTicket.setCustomer(null);
    }
    refreshTicket();
    }//GEN-LAST:event_jEditAttributes2ActionPerformed

    private void btnCustomer2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomer2ActionPerformed
        if (this.m_oTicket.getLinesCount() > 0&& (this.m_App.getAppUserView().getUser().getRole().equals("0")))
        {
            ArrayList mensajeList = new ArrayList();
            mensajeList.add(new JLabel("Nombre del producto: "));
            JTextField nombre = new JTextField("PROPINA");
            mensajeList.add(nombre);
            mensajeList.add(new JLabel("Cantidad: "));
            JTextField cant = new JTextField("1.00");
            mensajeList.add(cant);
            mensajeList.add(new JLabel("Monto: "));
            JTextField monto = new JTextField("");
            mensajeList.add(monto);

            Object[] objetosList2 = mensajeList.toArray();
            String[] opciones2 = { "Aceptar", "Cancelar" };
            objetosList2 = mensajeList.toArray();
            int respuesta2 = JOptionPane.showOptionDialog(null, objetosList2, "Extra.", 0, 3, null, opciones2, nombre);
            if (respuesta2 == 0)
            {
                try
                {
                    double parseDouble = 0.0D;
                    try
                    {
                        parseDouble = Double.parseDouble(cant.getText().trim());
                    }
                    catch (NumberFormatException e1)
                    {
                        JOptionPane.showMessageDialog(this, "Error, la cantidad es incorrecta.");
                        return;
                    }
                    double parseDouble2 = 0.0D;
                    try
                    {
                        parseDouble2 = Double.parseDouble(monto.getText().trim());
                    }
                    catch (NumberFormatException e1)
                    {
                        JOptionPane.showMessageDialog(this, "Error, el monto es incorrecto.");
                        return;
                    }
                    ProductInfoExt prod = this.dlSales.getProductInfoByReference("0");
                    prod.setName(nombre.getText().trim());
                    TaxInfo t = new TaxInfo("000", "Tax Exempt", "000", new Date(), null, null, 0.0D, false, Integer.valueOf(0));
                    this.m_oTicket.addLine(new TicketLineInfo(prod, parseDouble, parseDouble2, t, new Properties()));
                    TicketLineInfo tli = this.m_oTicket.getLine(this.m_oTicket.getLinesCount() - 1);
                    /*if (esbebida.isSelected())
                    {
                        tli.setTipo("bebida");
                        tli.setProperty("bebida", "bebida");
                    }
                    else
                    {
                        tli.setTipo("comida");
                        tli.setProperty("comida", "comida");
                    }*/
                }
                catch (BasicException e) {}
                refreshTicket();
            }
        }
    }//GEN-LAST:event_btnCustomer2ActionPerformed

    private void btnCustomer1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomer1ActionPerformed
        /*int numlines = this.m_oTicket.getLinesCount();
        for (int i = 0; i < numlines; i++)
        {
            TicketLineInfo current_ticketline = this.m_oTicket.getLine(i);
            double current_unit = current_ticketline.getMultiply();
            if (current_unit != 0.0D)
            {
                for (int j = i + 1; j < numlines; j++)
                {
                    TicketLineInfo loop_ticketline = this.m_oTicket.getLine(j);
                    double loop_unit = loop_ticketline.getMultiply();
                    String current_productid = current_ticketline.getProductID();
                    String loop_productid = loop_ticketline.getProductID();
                    if ((loop_productid.equals(current_productid)) && (loop_ticketline.getPrice() == current_ticketline.getPrice()) && (loop_unit != 0.0D))
                    {
                        current_unit += loop_unit;
                        loop_ticketline.setMultiply(0.0D);
                    }
                }
                current_ticketline.setMultiply(current_unit);
            }
        }
        for (int i = numlines - 1; i > 0; i--)
        {
            TicketLineInfo loop_ticketline = this.m_oTicket.getLine(i);
            double loop_unit = loop_ticketline.getMultiply();
            if (loop_unit == 0.0D) {
                this.m_oTicket.removeLine(i);
            }
        }
        if (this.m_App.getAppUserView().getUser().getRole().equals("0"))
        {
            printTicket("Printer.TicketPreview", this.m_oTicket, this.m_oTicketExt);
            refreshTicket();
            return;
        }
        if (this.m_oTicket.getProperty("imp", "").equals("SI"))
        {
            JOptionPane.showMessageDialog(this, "Este ticket ya se imprimio");
            return;
        }
        this.m_oTicket.setProperty("imp", "SI");*/
        
        /*int numlines = this.m_oTicket.getLinesCount();
        boolean isOK = true;
        for (int i = 0; i < numlines; i++)
        {
            if(this.m_oTicket.getLine(i).getProperty("impreso", "NO")=="NO")
            {
                if(!this.m_oTicket.getLine(i).getProductCode().equals("0"))
                {
                    isOK= false;
                    break;
                }
            }
        }
        if(isOK == false)
        {
            JOptionPane.showMessageDialog(this, "No puede imprimir el previo sin enviar comandas.");
            return;
        }*/
        printTicket("Printer.TicketPreview", this.m_oTicket, this.m_oTicketExt);
        JOptionPane.showMessageDialog(this, "Se ha mandado a imprimir la cuenta.");
        refreshTicket();
        /*printTicket("Printer.TicketPreview", this.m_oTicket, this.m_oTicketExt);
            refreshTicket();
            return;*/
    }//GEN-LAST:event_btnCustomer1ActionPerformed

    private void btnCustomer4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomer4ActionPerformed
        int c = 0;
        for (TicketLineInfo tli : this.m_oTicket.getLines()) {
            try
            {
                ProductInfoExt productInfo = this.dlSales.getProductInfo(tli.getProductID());
                if (!productInfo.isScale()&&tli.getProperty("impreso", "NO")=="NO")
                {
                    tli.setTipo("comida");
                    tli.setProperty("comida", "comida");
                    c++;
                }
            }
            catch (BasicException ex)
            {
                Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (c > 0)
        {
            printTicket("Printer.TicketCocina", this.m_oTicket, this.m_oTicketExt);
            for (TicketLineInfo tli : this.m_oTicket.getLines()) {
                try
                {
                    ProductInfoExt productInfo = this.dlSales.getProductInfo(tli.getProductID());
                    if (!productInfo.isScale())
                    {
                        tli.setImpreso("SI");
                        tli.setProperty("impreso", "SI");
                        tli.setProperty("comida", "comida");
                    }
                }
                catch (BasicException ex)
                {
                    Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            //JOptionPane.showMessageDialog(this, "Se ha mandado a imprimir la comanda de comida.");
            refreshTicket();
        }
        else
        {
            JOptionPane.showMessageDialog(this, "NO hay elementos de cocina pendientes a imprimir.");
        }
    }//GEN-LAST:event_btnCustomer4ActionPerformed

    private void btnCustomer5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomer5ActionPerformed
        int c = 0;
        for (TicketLineInfo tli : this.m_oTicket.getLines()) {
            try
            {
                ProductInfoExt productInfo = this.dlSales.getProductInfo(tli.getProductID());
                if (productInfo.isCom()&&tli.getProperty("bebida2", "NO")=="NO")
                {
                    tli.setTipo("bebida2");
                    tli.setProperty("bebida2", "bebida2");
                    c++;
                }
            }
            catch (BasicException ex)
            {
                Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (c > 0)
        {
            printTicket("Printer.TicketBarra2", this.m_oTicket, this.m_oTicketExt);
            for (TicketLineInfo tli : this.m_oTicket.getLines()) {
                try
                {
                    ProductInfoExt productInfo = this.dlSales.getProductInfo(tli.getProductID());
                    if (productInfo.isCom()) {
                        tli.setImpreso("SI");
                        tli.setProperty("impreso", "SI");
                        tli.setProperty("bebida2", "bebida2");
                    }
                }
                catch (BasicException ex)
                {
                    Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            refreshTicket();
        }
        
        else
        {
            JOptionPane.showMessageDialog(this, "NO hay elementos de barra 2 pendientes a imprimir.");
        }
    }//GEN-LAST:event_btnCustomer5ActionPerformed

    private void btnCustomer3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomer3ActionPerformed
        int c = 0;
        for (TicketLineInfo tli : this.m_oTicket.getLines()) {
            try
            {
                ProductInfoExt productInfo = this.dlSales.getProductInfo(tli.getProductID());
                if (productInfo.isScale()&&tli.getProperty("impreso", "NO").equals("NO"))
                //if (productInfo.getCategoryID().equals("6")&&tli.getProperty("impreso", "NO").equals("NO"))
                {
                    tli.setTipo("bebida");
                    tli.setProperty("bebida", "bebida");
                    c++;
                }
            }
            catch (BasicException ex)
            {
                Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (c > 0)
        {
            printTicket("Printer.TicketBarra", this.m_oTicket, this.m_oTicketExt);
            for (TicketLineInfo tli : this.m_oTicket.getLines()) {
                try
                {
                    ProductInfoExt productInfo = this.dlSales.getProductInfo(tli.getProductID());
                    if (productInfo.isScale()){
//if (productInfo.getCategoryID().equals("6")) {
                        tli.setImpreso("SI");
                        tli.setProperty("impreso", "SI");
                        tli.setProperty("bebida", "bebida");
                    }
                }
                catch (BasicException ex)
                {
                    Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
            //JOptionPane.showMessageDialog(this, "Se ha mandado a imprimir la comanda de bebida.");
            refreshTicket();
        }
        
        
        /*
        c = 0;
        for (TicketLineInfo tli : this.m_oTicket.getLines()) {
            try
            {
                ProductInfoExt productInfo = this.dlSales.getProductInfo(tli.getProductID());
                if (
                        productInfo.getCategoryID().equals("3")
                        &&tli.getProperty("impreso", "NO")=="NO")
                {
                    tli.setTipo("especialidad");
                    tli.setProperty("especialidad", "especialidad");
                    c++;
                }
            }
            catch (BasicException ex)
            {
                Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (c > 0)
        {
            printTicket("Printer.TicketEspecialidades", this.m_oTicket, this.m_oTicketExt);
            for (TicketLineInfo tli : this.m_oTicket.getLines()) {
                try
                {
                    ProductInfoExt productInfo = this.dlSales.getProductInfo(tli.getProductID());
                    if (productInfo.getCategoryID().equals("3"))
                    {
                        tli.setImpreso("SI");
                        tli.setProperty("impreso", "SI");
                        tli.setProperty("especialidad", "especialidad");
                    }
                }
                catch (BasicException ex)
                {
                    Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            //JOptionPane.showMessageDialog(this, "Se ha mandado a imprimir la comanda de comida.");
            refreshTicket();
        }*/
    }//GEN-LAST:event_btnCustomer3ActionPerformed

    private void btnCustomerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomerActionPerformed
        if ((this.m_oTicket.getLinesCount() > 0) && (this.m_App.getAppUserView().getUser().getRole().equals("0")))
        {
            for (TicketLineInfo line : this.m_oTicket.getLines()) {
                if (line.getProductName().contains("DESCUENTO"))
                {
                    JOptionPane.showMessageDialog(this, "Esta cuenta ya contiene un descuento.");
                    return;
                }
            }
            double parseDouble = 0.0D;
            try
            {
                String propina = JOptionPane.showInputDialog("Escriba el % de descuento:");
                parseDouble = Double.parseDouble(propina);
            }
            catch (NumberFormatException e1)
            {
                JOptionPane.showMessageDialog(this, "Error.");
                return;
            }
            try
            {
                ProductInfoExt prod = this.dlSales.getProductInfoByReference("0");
                prod.setName("DESCUENTO " + String.valueOf(parseDouble) + "%");
                parseDouble /= 100.0D;
                TaxInfo t = new TaxInfo("000", "Tax Exempt", "000", new Date(), null, null, 0.0D, false, Integer.valueOf(0));
                this.m_oTicket.addLine(new TicketLineInfo(prod, 1.0D, -(parseDouble * this.m_oTicket.getTotal()), t, new Properties()));
                refreshTicket();
            }
            catch (BasicException ex)
            {
                Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_btnCustomerActionPerformed

    private void m_jEditLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jEditLineActionPerformed

        int i = m_ticketlines.getSelectedIndex();
        if (i < 0){
            Toolkit.getDefaultToolkit().beep(); // no line selected
        }
        else {
            if(m_App.getAppUserView().getUser().getRole().equals("0"))
            {
                try {
                    TicketLineInfo newline = JProductLineEdit.showMessage(this, m_App, m_oTicket.getLine(i));
                    if (newline != null) {
                        // line has been modified
                        paintTicketLine(i, newline);
                    }
                } catch (BasicException e) {
                    new MessageInf(e).show(this);
                }
            }

        }
    }//GEN-LAST:event_m_jEditLineActionPerformed

    private void m_jDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jDeleteActionPerformed
        //if (this.m_App.getAppUserView().getUser().getRole().equals("0"))
        //{
        int i = m_ticketlines.getSelectedIndex();
        if (i < 0){
            Toolkit.getDefaultToolkit().beep(); // No hay ninguna seleccionada
        } else {
            if(m_oTicket.getLine(i).getProperty("impreso", "NO").equals("NO"))
            {
                removeTicketLine(i); // elimino la linea
            }
            else if(m_App.getAppUserView().getUser().getRole().equals("0")||m_App.getAppUserView().getUser().getRole().equals("1"))
            {
                removeTicketLine(i); // elimino la linea
            }
        }
        //}
    }//GEN-LAST:event_m_jDeleteActionPerformed

    private void btnSplitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSplitActionPerformed

        if (m_oTicket.getLinesCount() > 0&& (this.m_App.getAppUserView().getUser().getRole().equals("0"))) 
        {
            ReceiptSplit splitdialog = ReceiptSplit.getDialog(this, dlSystem.getResourceAsXML("Ticket.Line"), dlSales, dlCustomers, taxeslogic);

            TicketInfo ticket1 = m_oTicket.copyTicket();
            TicketInfo ticket2 = new TicketInfo();
            ticket2.setCustomer(m_oTicket.getCustomer());

            if (splitdialog.showDialog(ticket1, ticket2, m_oTicketExt)) {
                if (closeTicket(ticket2, m_oTicketExt)) { // already checked  that number of lines > 0
                    setActiveTicket(ticket1, m_oTicketExt);// set result ticket
                }
            }
        }
    }//GEN-LAST:event_btnSplitActionPerformed

    private void jEditAttributesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jEditAttributesActionPerformed

        int i = this.m_ticketlines.getSelectedIndex();
        if (i < 0)
        {
            Toolkit.getDefaultToolkit().beep();
        }
        else
        {
            String com = JOptionPane.showInputDialog("Comentario:");
            /*if(com.length()<=20)
            {
                this.m_oTicket.getLine(i).setProperty("comentario", com);
            }
            else
            {
                this.m_oTicket.getLine(i).setProperty("comentario", com.substring(0, 20));
                this.m_oTicket.getLine(i).setProperty("comentario2", com.substring(20, com.length()));
            }*/
            this.m_oTicket.getLine(i).setProperty("comentario", com);
            refreshTicket();
        }
    }//GEN-LAST:event_jEditAttributesActionPerformed

    private void btnCustomer6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomer6ActionPerformed
        try {
            ProductInfoExt prod = this.dlSales.getProductInfoByReference("0");
            prod.setName("----------------------------------------------------------------------");
            TaxInfo t = new TaxInfo("000", "Tax Exempt", "000", new Date(), null, null, 0.0D, false, Integer.valueOf(0));
            this.m_oTicket.addLine(new TicketLineInfo(prod, 0, 0, t, new Properties()));
        } catch (BasicException ex) {
            Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
        }
        refreshTicket();
    }//GEN-LAST:event_btnCustomer6ActionPerformed

    private void btnCustomer7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCustomer7ActionPerformed
int c = 0;
        for (TicketLineInfo tli : this.m_oTicket.getLines()) {
            try
            {
                ProductInfoExt productInfo = this.dlSales.getProductInfo(tli.getProductID());
                if (!productInfo.isScale()&&tli.getProperty("impreso", "NO").equals("NO"))
                        /*(productInfo.getCategoryID().equals("d37974e1-3976-42e0-a033-b2f0940d52ed")||
                        productInfo.getCategoryID().equals("1")||
                        productInfo.getCategoryID().equals("2")||
                        productInfo.getCategoryID().equals("4")||
                        productInfo.getCategoryID().equals("5"))
                        &&tli.getProperty("impreso", "NO")=="NO")*/
                {
                    tli.setTipo("comida");
                    tli.setProperty("comida", "comida");
                    c++;
                }
            }
            catch (BasicException ex)
            {
                Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (c > 0)
        {
            printTicket("Printer.TicketCocina", this.m_oTicket, this.m_oTicketExt);
            for (TicketLineInfo tli : this.m_oTicket.getLines()) {
                try
                {
                    ProductInfoExt productInfo = this.dlSales.getProductInfo(tli.getProductID());
                    if (!productInfo.isScale())
                            /*(productInfo.getCategoryID().equals("d37974e1-3976-42e0-a033-b2f0940d52ed")||
                        productInfo.getCategoryID().equals("1")||
                        productInfo.getCategoryID().equals("2")||
                        productInfo.getCategoryID().equals("4")||
                        productInfo.getCategoryID().equals("5")))*/
                    {
                        tli.setImpreso("SI");
                        
                        tli.setProperty("impreso", "SI");
                        tli.setProperty("comida", "comida");
                    }
                }
                catch (BasicException ex)
                {
                    Logger.getLogger(JPanelTicket.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            //JOptionPane.showMessageDialog(this, "Se ha mandado a imprimir la comanda de comida.");
            refreshTicket();
        }        // TODO add your handling code here:
    }//GEN-LAST:event_btnCustomer7ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCustomer;
    private javax.swing.JButton btnCustomer1;
    private javax.swing.JButton btnCustomer2;
    private javax.swing.JButton btnCustomer3;
    private javax.swing.JButton btnCustomer4;
    private javax.swing.JButton btnCustomer5;
    private javax.swing.JButton btnCustomer6;
    private javax.swing.JButton btnCustomer7;
    private javax.swing.JButton btnSplit;
    private javax.swing.JPanel catcontainer;
    private javax.swing.JButton jEditAttributes;
    private javax.swing.JButton jEditAttributes1;
    private javax.swing.JButton jEditAttributes2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JPanel m_jButtons;
    private javax.swing.JPanel m_jButtonsExt;
    private javax.swing.JPanel m_jContEntries;
    private javax.swing.JButton m_jDelete;
    private javax.swing.JButton m_jDown;
    private javax.swing.JButton m_jEditLine;
    private javax.swing.JButton m_jEnter;
    private javax.swing.JTextField m_jKeyFactory;
    private javax.swing.JLabel m_jLblTotalEuros1;
    private javax.swing.JLabel m_jLblTotalEuros2;
    private javax.swing.JLabel m_jLblTotalEuros3;
    private javax.swing.JButton m_jList;
    private com.openbravo.beans.JNumberKeys m_jNumberKeys;
    private javax.swing.JPanel m_jOptions;
    private javax.swing.JPanel m_jPanContainer;
    private javax.swing.JPanel m_jPanEntries;
    private javax.swing.JPanel m_jPanTicket;
    private javax.swing.JPanel m_jPanTotals;
    private javax.swing.JPanel m_jPanelBag;
    private javax.swing.JPanel m_jPanelCentral;
    private javax.swing.JPanel m_jPanelScripts;
    private javax.swing.JLabel m_jPor;
    private javax.swing.JLabel m_jPrice;
    private javax.swing.JLabel m_jSubtotalEuros;
    private javax.swing.JComboBox m_jTax;
    private javax.swing.JLabel m_jTaxesEuros;
    private javax.swing.JLabel m_jTicketId;
    private javax.swing.JLabel m_jTotalEuros;
    private javax.swing.JButton m_jUp;
    private javax.swing.JToggleButton m_jaddtax;
    private javax.swing.JButton m_jbtnScale;
    // End of variables declaration//GEN-END:variables

}
