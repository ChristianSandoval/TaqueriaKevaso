
package com.openbravo.pos.panels;

import java.util.*;
import javax.swing.table.AbstractTableModel;
import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.*;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.util.StringUtils;
import java.text.SimpleDateFormat;

/**
 *
 * @author adrianromero
 */
public class PaymentsModel {

    private String m_sHost;
    private int m_iSeq;
    private Date m_dDateStart;
    private Date m_dDateEnd;       
            
    private Integer m_iPayments;
    private Double m_dPaymentsTotal;
    private java.util.List<PaymentsLine> m_lpayments;
    private java.util.List<PaymentMovements> m_lpaymentsMov;
    private final static String[] PAYMENTHEADERS = {"Label.Payment", "label.totalcash"};
    
    private Integer m_iSales;
    private Double m_dSalesBase;
    private Double m_dSalesTaxes;
    private java.util.List<SalesLine> m_lsales;
        private List<ProductSalesLine> m_lproductsales;
    private final static String[] SALEHEADERS = {"label.taxcash", "label.totalcash"};
private List<Stock> m_lStock;
private List<Stock2> m_lStock2;
    private PaymentsModel() {
    }    
    
    public static PaymentsModel emptyInstance() {
        
        PaymentsModel p = new PaymentsModel();
        
        p.m_iPayments = new Integer(0);
        p.m_dPaymentsTotal = new Double(0.0);
        p.m_lpayments = new ArrayList<PaymentsLine>();
        p.m_lpaymentsMov = new ArrayList<PaymentMovements>();
        p.m_lproductsales = new ArrayList<ProductSalesLine>();
        p.m_iSales = null;
        p.m_dSalesBase = null;
        p.m_dSalesTaxes = null;
        p.m_lsales = new ArrayList<SalesLine>();
        p.m_lStock2 = new ArrayList();
        p.m_lStock = new ArrayList();
        return p;
    }
    public static PaymentsModel loadInstance(AppView app, String cash) throws BasicException {
        emptyInstance();
        PaymentsModel p = new PaymentsModel();
        Object[] valtickets = (Object []) new StaticSentence(app.getSession()
            , "SELECT COUNT(*), SUM(PAYMENTS.TOTAL) " +
              "FROM PAYMENTS, RECEIPTS " +
              "WHERE PAYMENTS.PAYMENT<>'free' AND PAYMENTS.RECEIPT = RECEIPTS.ID AND RECEIPTS.MONEY = ?"
            , SerializerWriteString.INSTANCE
            , new SerializerReadBasic(new Datas[] {Datas.INT, Datas.DOUBLE}))
            .find(cash);
            
        if (valtickets == null) {
            p.m_iPayments = new Integer(0);
            p.m_dPaymentsTotal = new Double(0.0);
        } else {
            p.m_iPayments = (Integer) valtickets[0];
            p.m_dPaymentsTotal = (Double) valtickets[1];
        }  
        
        List l = new StaticSentence(app.getSession()            
            , "SELECT PAYMENTS.PAYMENT, SUM(PAYMENTS.TOTAL) " +
              "FROM PAYMENTS, RECEIPTS " +
              "WHERE AND PAYMENTS.RECEIPT = RECEIPTS.ID AND RECEIPTS.MONEY = ? " +
              "GROUP BY PAYMENTS.PAYMENT"
            , SerializerWriteString.INSTANCE
            , new SerializerReadClass(PaymentsModel.PaymentsLine.class)) //new SerializerReadBasic(new Datas[] {Datas.STRING, Datas.DOUBLE}))
            .list(cash); 
        
        if (l == null) {
            p.m_lpayments = new ArrayList();
        } else {
            p.m_lpayments = l;
        }        
        
        List lMov = new StaticSentence(app.getSession()            
            , "SELECT PAYMENTS.PAYMENT,PAYMENTS.TOTAL, PAYMENTS.NOTES " +
              "FROM PAYMENTS, RECEIPTS " +
              "WHERE (PAYMENTS.PAYMENT='cashin' OR PAYMENTS.PAYMENT='cashout') AND PAYMENTS.RECEIPT = RECEIPTS.ID AND RECEIPTS.MONEY = ? " +
              "GROUP BY PAYMENTS.PAYMENT"
            , SerializerWriteString.INSTANCE
            , new SerializerReadClass(PaymentsModel.PaymentMovements.class)) //new SerializerReadBasic(new Datas[] {Datas.STRING, Datas.DOUBLE}))
            .list(cash); 
        
        if (lMov == null) {
            p.m_lpaymentsMov = new ArrayList();
        } else {
            p.m_lpaymentsMov = lMov;
        }
        // Sales
        Object[] recsales = (Object []) new StaticSentence(app.getSession(),
            "SELECT COUNT(DISTINCT RECEIPTS.ID), SUM(TICKETLINES.UNITS * TICKETLINES.PRICE) " +
            "FROM RECEIPTS, TICKETLINES WHERE RECEIPTS.ID = TICKETLINES.TICKET AND RECEIPTS.MONEY = ?",
            SerializerWriteString.INSTANCE,
            new SerializerReadBasic(new Datas[] {Datas.INT, Datas.DOUBLE}))
            .find(cash);
        if (recsales == null) {
            p.m_iSales = null;
            p.m_dSalesBase = null;
        } else {
            p.m_iSales = (Integer) recsales[0];
            p.m_dSalesBase = (Double) recsales[1];
        }             
        
        // Taxes
        Object[] rectaxes = (Object []) new StaticSentence(app.getSession(),
            "SELECT SUM(TAXLINES.AMOUNT) " +
            "FROM RECEIPTS, TAXLINES WHERE RECEIPTS.ID = TAXLINES.RECEIPT AND RECEIPTS.MONEY = ?"
            , SerializerWriteString.INSTANCE
            , new SerializerReadBasic(new Datas[] {Datas.DOUBLE}))
            .find(cash);            
        if (rectaxes == null) {
            p.m_dSalesTaxes = null;
        } else {
            p.m_dSalesTaxes = (Double) rectaxes[0];
        } 
                
        List<SalesLine> asales = new StaticSentence(app.getSession(),
                "SELECT TAXCATEGORIES.NAME, SUM(TAXLINES.AMOUNT) " +
                "FROM RECEIPTS, TAXLINES, TAXES, TAXCATEGORIES WHERE RECEIPTS.ID = TAXLINES.RECEIPT AND TAXLINES.TAXID = TAXES.ID AND TAXES.CATEGORY = TAXCATEGORIES.ID " +
                "AND RECEIPTS.MONEY = ?" +
                "GROUP BY TAXCATEGORIES.NAME"
                , SerializerWriteString.INSTANCE
                , new SerializerReadClass(PaymentsModel.SalesLine.class))
                .list(cash);
        if (asales == null) {
            p.m_lsales = new ArrayList<SalesLine>();
        } else {
            p.m_lsales = asales;
        }
        
        List products = new StaticSentence(app.getSession(), "SELECT PRODUCTS.NAME, SUM(TICKETLINES.UNITS), PRODUCTS.PRICEBUY, SUM(TICKETLINES.UNITS * TICKETLINES.PRICE) AS TOTAL,SUM(TICKETLINES.UNITS * TICKETLINES.PRICE) - SUM(TICKETLINES.UNITS * PRODUCTS.PRICEBUY) AS GANANCIA  FROM TICKETLINES, TICKETS, RECEIPTS, PRODUCTS, PAYMENTS WHERE PAYMENTS.RECEIPT=RECEIPTS.ID AND TICKETLINES.PRODUCT = PRODUCTS.ID AND TICKETLINES.TICKET = TICKETS.ID AND TICKETS.ID = RECEIPTS.ID AND RECEIPTS.MONEY = ? GROUP BY TICKETLINES.PRODUCT", SerializerWriteString.INSTANCE, new SerializerReadClass(ProductSalesLine.class)).list(cash);
    if (products == null) {
      p.m_lproductsales = new ArrayList();
    } else {
      p.m_lproductsales = products;
    }
    /*
    Calendar currentDate = Calendar.getInstance(); //Get the current date
SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd"); //format it as per your requirement
String dateNow = formatter.format(currentDate.getTime());
    List<Stock2> stock2 = new StaticSentence(app.getSession(), "SELECT PRODUCTS.NAME,STOCKDIARY.UNITS,STOCKDIARY.DATENEW, CASE WHEN REASON=1 THEN 'ENTRADA(COMPRA)' WHEN REASON=2 THEN 'ENTRADA(DEVOLUCION)' WHEN REASON=4 THEN 'ENTRADA(TRASPASO)' WHEN REASON=-2 THEN 'SALIDA(DEVOLUCION)' WHEN REASON=-3 THEN 'SALIDA(ROTURA)' WHEN REASON=-4 THEN 'SALIDA(TRASPASO)' END AS 'TIPO' FROM STOCKDIARY JOIN PRODUCTS ON STOCKDIARY.PRODUCT=PRODUCTS.ID WHERE REASON<>-1 AND DATENEW>=? ", SerializerWriteString.INSTANCE, new SerializerReadClass(Stock2.class)).list(dateNow+" 00:00:00");
    if (stock2 == null) {
      p.m_lStock2 = new ArrayList();
    } else {
      p.m_lStock2 = stock2;
    }*/
    
        return p;
    }
    public String findMoney(AppView app,String id) throws BasicException {

        PreparedSentence p = new PreparedSentence(app.getSession(), "SELECT MONEY FROM closedcash WHERE DATEEND IS NOT NULL AND HOSTSEQUENCE=?"
                    , SerializerWriteString.INSTANCE
                    , SerializerReadString.INSTANCE);

        String d = (String) p.find(id);
        return d;
    }
    public String findDateStart(AppView app,String id) throws BasicException {

        PreparedSentence p = new PreparedSentence(app.getSession(), "SELECT DATESTART FROM closedcash WHERE DATEEND IS NOT NULL AND HOSTSEQUENCE=?"
                    , SerializerWriteString.INSTANCE
                    , SerializerReadString.INSTANCE);

        String d = (String) p.find(id);
        return d;
    }
    public String findDateEnd(AppView app,String id) throws BasicException {

        PreparedSentence p = new PreparedSentence(app.getSession(), "SELECT DATEEND FROM closedcash WHERE DATEEND IS NOT NULL AND HOSTSEQUENCE=?"
                    , SerializerWriteString.INSTANCE
                    , SerializerReadString.INSTANCE);

        String d = (String) p.find(id);
        return d;
    }
    public static PaymentsModel loadInstance(AppView app) throws BasicException {
        
        PaymentsModel p = new PaymentsModel();
        
        // Propiedades globales
        p.m_sHost = app.getProperties().getHost();
        p.m_iSeq = app.getActiveCashSequence();
        p.m_dDateStart = app.getActiveCashDateStart();
        p.m_dDateEnd = null;
        
        
        // Pagos
        Object[] valtickets = (Object []) new StaticSentence(app.getSession()
            , "SELECT COUNT(*), SUM(PAYMENTS.TOTAL) " +
              "FROM PAYMENTS, RECEIPTS " +
              "WHERE PAYMENTS.PAYMENT<>'free' AND PAYMENTS.RECEIPT = RECEIPTS.ID AND RECEIPTS.MONEY = ?"
            , SerializerWriteString.INSTANCE
            , new SerializerReadBasic(new Datas[] {Datas.INT, Datas.DOUBLE}))
            .find(app.getActiveCashIndex());
            
        if (valtickets == null) {
            p.m_iPayments = new Integer(0);
            p.m_dPaymentsTotal = new Double(0.0);
        } else {
            p.m_iPayments = (Integer) valtickets[0];
            p.m_dPaymentsTotal = (Double) valtickets[1];
        }  
        
        List l = new StaticSentence(app.getSession()            
            , "SELECT PAYMENTS.PAYMENT, SUM(PAYMENTS.TOTAL) " +
              "FROM PAYMENTS, RECEIPTS " +
              "WHERE PAYMENTS.RECEIPT = RECEIPTS.ID AND RECEIPTS.MONEY = ? " +
              "GROUP BY PAYMENTS.PAYMENT"
            , SerializerWriteString.INSTANCE
            , new SerializerReadClass(PaymentsModel.PaymentsLine.class)) //new SerializerReadBasic(new Datas[] {Datas.STRING, Datas.DOUBLE}))
            .list(app.getActiveCashIndex()); 
        
        if (l == null) {
            p.m_lpayments = new ArrayList();
        } else {
            p.m_lpayments = l;
        }        
        
        // Sales
        Object[] recsales = (Object []) new StaticSentence(app.getSession(),
            "SELECT COUNT(DISTINCT RECEIPTS.ID), SUM(TICKETLINES.UNITS * TICKETLINES.PRICE) " +
            "FROM RECEIPTS, TICKETLINES WHERE RECEIPTS.ID = TICKETLINES.TICKET AND RECEIPTS.MONEY = ?",
            SerializerWriteString.INSTANCE,
            new SerializerReadBasic(new Datas[] {Datas.INT, Datas.DOUBLE}))
            .find(app.getActiveCashIndex());
        if (recsales == null) {
            p.m_iSales = null;
            p.m_dSalesBase = null;
        } else {
            p.m_iSales = (Integer) recsales[0];
            p.m_dSalesBase = (Double) recsales[1];
        }             
        
        // Taxes
        Object[] rectaxes = (Object []) new StaticSentence(app.getSession(),
            "SELECT SUM(TAXLINES.AMOUNT) " +
            "FROM RECEIPTS, TAXLINES WHERE RECEIPTS.ID = TAXLINES.RECEIPT AND RECEIPTS.MONEY = ?"
            , SerializerWriteString.INSTANCE
            , new SerializerReadBasic(new Datas[] {Datas.DOUBLE}))
            .find(app.getActiveCashIndex());            
        if (rectaxes == null) {
            p.m_dSalesTaxes = null;
        } else {
            p.m_dSalesTaxes = (Double) rectaxes[0];
        } 
                
        List<SalesLine> asales = new StaticSentence(app.getSession(),
                "SELECT TAXCATEGORIES.NAME, SUM(TAXLINES.AMOUNT) " +
                "FROM RECEIPTS, TAXLINES, TAXES, TAXCATEGORIES WHERE RECEIPTS.ID = TAXLINES.RECEIPT AND TAXLINES.TAXID = TAXES.ID AND TAXES.CATEGORY = TAXCATEGORIES.ID " +
                "AND RECEIPTS.MONEY = ?" +
                "GROUP BY TAXCATEGORIES.NAME"
                , SerializerWriteString.INSTANCE
                , new SerializerReadClass(PaymentsModel.SalesLine.class))
                .list(app.getActiveCashIndex());
        if (asales == null) {
            p.m_lsales = new ArrayList<SalesLine>();
        } else {
            p.m_lsales = asales;
        }
        
        List products = new StaticSentence(app.getSession(), "SELECT PRODUCTS.NAME, SUM(TICKETLINES.UNITS), PRODUCTS.PRICEBUY, SUM(TICKETLINES.UNITS * TICKETLINES.PRICE) AS TOTAL,SUM(TICKETLINES.UNITS * TICKETLINES.PRICE) - SUM(TICKETLINES.UNITS * PRODUCTS.PRICEBUY) AS GANANCIA  FROM TICKETLINES, TICKETS, RECEIPTS, PRODUCTS, PAYMENTS WHERE PAYMENTS.RECEIPT=RECEIPTS.ID AND TICKETLINES.PRODUCT = PRODUCTS.ID AND TICKETLINES.TICKET = TICKETS.ID AND TICKETS.ID = RECEIPTS.ID AND RECEIPTS.MONEY = ? GROUP BY TICKETLINES.PRODUCT", SerializerWriteString.INSTANCE, new SerializerReadClass(ProductSalesLine.class)).list(app.getActiveCashIndex());
    if (products == null) {
      p.m_lproductsales = new ArrayList();
    } else {
      p.m_lproductsales = products;
    }
    
    List<Stock> stock = new StaticSentence(app.getSession(), "SELECT STOCKCURRENT.PRODUCT,STOCKCURRENT.UNITS,PRODUCTS.NAME FROM STOCKCURRENT JOIN PRODUCTS ON STOCKCURRENT.PRODUCT=PRODUCTS.ID WHERE '1'=? ", SerializerWriteString.INSTANCE, new SerializerReadClass(Stock.class)).list("1");
    if (stock == null) {
      p.m_lStock = new ArrayList();
    } else {
      p.m_lStock = stock;
    }
    Calendar currentDate = Calendar.getInstance(); //Get the current date
SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd"); //format it as per your requirement
String dateNow = formatter.format(currentDate.getTime());
    List<Stock2> stock2 = new StaticSentence(app.getSession(), "SELECT PRODUCTS.NAME,STOCKDIARY.UNITS,STOCKDIARY.DATENEW, CASE WHEN REASON=1 THEN 'ENTRADA(COMPRA)' WHEN REASON=2 THEN 'ENTRADA(DEVOLUCION)' WHEN REASON=4 THEN 'ENTRADA(TRASPASO)' WHEN REASON=-2 THEN 'SALIDA(DEVOLUCION)' WHEN REASON=-3 THEN 'SALIDA(ROTURA)' WHEN REASON=-4 THEN 'SALIDA(TRASPASO)' END AS 'TIPO' FROM STOCKDIARY JOIN PRODUCTS ON STOCKDIARY.PRODUCT=PRODUCTS.ID WHERE REASON<>-1 AND DATENEW>=? ", SerializerWriteString.INSTANCE, new SerializerReadClass(Stock2.class)).list(dateNow+" 00:00:00");
    if (stock2 == null) {
      p.m_lStock2 = new ArrayList();
    } else {
      p.m_lStock2 = stock2;
    }
    
    
        List lMov = new StaticSentence(app.getSession()            
            , "SELECT PAYMENTS.PAYMENT,PAYMENTS.TOTAL, PAYMENTS.NOTES " +
              "FROM PAYMENTS, RECEIPTS " +
              "WHERE (PAYMENTS.PAYMENT='cashin' OR PAYMENTS.PAYMENT='cashout') AND PAYMENTS.RECEIPT = RECEIPTS.ID AND RECEIPTS.MONEY = ? " +
              ""
            , SerializerWriteString.INSTANCE
            , new SerializerReadClass(PaymentsModel.PaymentMovements.class)) //new SerializerReadBasic(new Datas[] {Datas.STRING, Datas.DOUBLE}))
            .list(app.getActiveCashIndex()); 
        
        if (lMov == null) {
            p.m_lpaymentsMov = new ArrayList();
        } else {
            p.m_lpaymentsMov = lMov;
        }
        return p;
    }
    public List<Stock> getStockLines()
  {
    return this.m_lStock;
  }
      public List<PaymentMovements> getPaymentMovements()
  {
    return this.m_lpaymentsMov;
  }
    public List<Stock2> getStockLines2()
  {
    return this.m_lStock2;
  }
    public static class Stock
    implements SerializableRead
  {
    private String productName;
    private String product;
    private Double units;
    
    public void readValues(DataRead dr)
      throws BasicException
    {
      this.product = dr.getString(1);
      this.units = dr.getDouble(2);
      this.productName = dr.getString(3);
    }
    
    public String printProduct()
    {
      return this.product;
    }
    
    public String printProductName()
    {
      return StringUtils.encodeXML(this.productName);
    }
    
    public Double printUnits()
    {
      return this.units;
    }
  }
    
    public static class Stock2
    implements SerializableRead
  {
    private String productName;
    private Double units;
    private Date fecha;
    private String tipo;
    public void readValues(DataRead dr)
      throws BasicException
    {
      this.productName = dr.getString(1);
      this.units = dr.getDouble(2);
      
      this.fecha = dr.getTimestamp(3);
      this.tipo = dr.getString(4);
    }
    public Date printDate()
    {
        return fecha;
    }
    public String printTipo()
    {
      return this.tipo;
    }
    public String printProductName()
    {
      return StringUtils.encodeXML(this.productName);
    }
    
    public Double printUnits()
    {
      return this.units;
    }
  }
    
public List<ProductSalesLine> getProductSalesLines()
  {
    return this.m_lproductsales;
  }
public static class ProductSalesLine
    implements SerializableRead
  {
    private String m_ProductName;
    private Double m_ProductUnits;
    private Double m_ProductPrice;
    private Double m_Total;
    private Double m_Ganancias;
    
    public void readValues(DataRead dr)
      throws BasicException
    {
      this.m_ProductName = dr.getString(1);
      this.m_ProductUnits = dr.getDouble(2);
      this.m_ProductPrice = dr.getDouble(3);
      this.m_Total = dr.getDouble(4);
      this.m_Ganancias = dr.getDouble(5);
    }
    
    public String printProductName()
    {
      return StringUtils.encodeXML(this.m_ProductName);
    }
    
    public String printProductUnits()
    {
      return Formats.DOUBLE.formatValue(this.m_ProductUnits);
    }
    
    public Double getProductUnits()
    {
      return this.m_ProductUnits;
    }
    public Double getProductGanancias()
    {
      return this.m_Ganancias;
    }
    public String printProductPrice()
    {
      return Formats.CURRENCY.formatValue(this.m_ProductPrice);
    }
    
    public Double getProductPrice()
    {
      return this.m_ProductPrice;
    }
    public Double getProductTotal()
    {
      return this.m_Total;
    }
    public String printTotal()
    {
      return Formats.CURRENCY.formatValue(this.m_Total);
    }
    
    public String printGanancias()
    {
      return Formats.CURRENCY.formatValue(this.m_Ganancias);
    }
  }
    public int getPayments() {
        return m_iPayments.intValue();
    }
    public double getTotal() {
        return m_dPaymentsTotal.doubleValue();
    }
    public String getHost() {
        return m_sHost;
    }
    public int getSequence() {
        return m_iSeq;
    }
    public Date getDateStart() {
        return m_dDateStart;
    }
    public void setDateEnd(Date dValue) {
        m_dDateEnd = dValue;
    }
    public Date getDateEnd() {
        return m_dDateEnd;
    }
    
    public String printHost() {
        return StringUtils.encodeXML(m_sHost);
    }
    public String printSequence() {
        return Formats.INT.formatValue(m_iSeq);
    }
    public String printDateStart() {
        return Formats.TIMESTAMP.formatValue(m_dDateStart);
    }
    public String printDateEnd() {
        return Formats.TIMESTAMP.formatValue(m_dDateEnd);
    }  
    
    public String printPayments() {
        return Formats.INT.formatValue(m_iPayments);
    }

    public String printPaymentsTotal() {
        return Formats.CURRENCY.formatValue(m_dPaymentsTotal);
    }     
    
    public List<PaymentsLine> getPaymentLines() {
        return m_lpayments;
    }
    
    public int getSales() {
        return m_iSales == null ? 0 : m_iSales.intValue();
    }    
    public String printSales() {
        return Formats.INT.formatValue(m_iSales);
    }
    public String printSalesBase() {
        return Formats.CURRENCY.formatValue(m_dSalesBase);
    }     
    public String printSalesTaxes() {
        return Formats.CURRENCY.formatValue(m_dSalesTaxes);
    }     
    public String printSalesTotal() {            
        return Formats.CURRENCY.formatValue((m_dSalesBase == null || m_dSalesTaxes == null)
                ? null
                : m_dSalesBase + m_dSalesTaxes);
    }     
    public List<SalesLine> getSaleLines() {
        return m_lsales;
    }
    
    public AbstractTableModel getPaymentsModel() {
        return new AbstractTableModel() {
            public String getColumnName(int column) {
                return AppLocal.getIntString(PAYMENTHEADERS[column]);
            }
            public int getRowCount() {
                return m_lpayments.size();
            }
            public int getColumnCount() {
                return PAYMENTHEADERS.length;
            }
            public Object getValueAt(int row, int column) {
                PaymentsLine l = m_lpayments.get(row);
                switch (column) {
                case 0: return l.getType();
                case 1: return l.getValue();
                default: return null;
                }
            }  
        };
    }
    public AbstractTableModel getPaymentsModel1() {
        return new AbstractTableModel() {
            public String getColumnName(int column) {
                switch (column) {
                case 0: return "TIPO";//l.printProductName();
                case 1: return "TOTAL";//l.printUnits();
                    case 2: return "DESC";//l.printDate();
                        //case 3: return "TIPO";//l.printTipo();
                default: return null;
                }
                //return AppLocal.getIntString(PAYMENTHEADERS[column]);
            }
            public int getRowCount() {
                return m_lpaymentsMov.size();
            }
            public int getColumnCount() {
                return 4;//PAYMENTHEADERS.length;
            }
            public Object getValueAt(int row, int column) {
                PaymentMovements l = m_lpaymentsMov.get(row);
                switch (column) {
                case 0: return l.printType();
                case 1: return l.printValue();
                    case 2: return l.printType2();
                default: return null;
                }
            }  
        };
    }
    public static class SalesLine implements SerializableRead {
        
        private String m_SalesTaxName;
        private Double m_SalesTaxes;
        
        public void readValues(DataRead dr) throws BasicException {
            m_SalesTaxName = dr.getString(1);
            m_SalesTaxes = dr.getDouble(2);
        }
        public String printTaxName() {
            return m_SalesTaxName;
        }      
        public String printTaxes() {
            return Formats.CURRENCY.formatValue(m_SalesTaxes);
        }
        public String getTaxName() {
            return m_SalesTaxName;
        }
        public Double getTaxes() {
            return m_SalesTaxes;
        }        
    }

    public AbstractTableModel getSalesModel() {
        return new AbstractTableModel() {
            public String getColumnName(int column) {
                return AppLocal.getIntString(SALEHEADERS[column]);
            }
            public int getRowCount() {
                return m_lsales.size();
            }
            public int getColumnCount() {
                return SALEHEADERS.length;
            }
            public Object getValueAt(int row, int column) {
                SalesLine l = m_lsales.get(row);
                switch (column) {
                case 0: return l.getTaxName();
                case 1: return l.getTaxes();
                default: return null;
                }
            }  
        };
    }
    
    public static class PaymentsLine implements SerializableRead {
        
        private String m_PaymentType;
        private Double m_PaymentValue;
        
        public void readValues(DataRead dr) throws BasicException {
            m_PaymentType = dr.getString(1);
            m_PaymentValue = dr.getDouble(2);
        }
        
        public String printType() {
            return AppLocal.getIntString("transpayment." + m_PaymentType);
        }
        public String getType() {
            return m_PaymentType;
        }
        public String printValue() {
            return Formats.CURRENCY.formatValue(m_PaymentValue);
        }
        public Double getValue() {
            return m_PaymentValue;
        }        
    }
    
    public static class PaymentMovements implements SerializableRead {
        
        private String m_PaymentType;
        private Double m_PaymentValue;
        private String m_PaymentType2;
        public void readValues(DataRead dr) throws BasicException {
            m_PaymentType = dr.getString(1);
            m_PaymentValue = dr.getDouble(2);
            m_PaymentType2 = dr.getString(3);
        }
        
        public String printType() {
            return AppLocal.getIntString("transpayment." + m_PaymentType);
        }
        public String printType2() {
            return m_PaymentType2;
        }
        public String getType() {
            return m_PaymentType;
        }
        public String printValue() {
            return Formats.CURRENCY.formatValue(m_PaymentValue);
        }
        public Double getValue() {
            return m_PaymentValue;
        }        
    }
}    