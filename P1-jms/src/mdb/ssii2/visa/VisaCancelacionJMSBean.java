/**
 * Pr&aacute;ctricas de Sistemas Inform&aacute;ticos II
 * VisaCancelacionJMSBean.java
 */

package ssii2.visa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.ejb.ActivationConfigProperty;
import javax.jms.MessageListener;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.JMSException;
import javax.annotation.Resource;
import java.util.logging.Logger;

/**
 * @author Pablo Sanchez y Javier Perez
 */
@MessageDriven(mappedName = "jms/VisaPagosQueue")
public class VisaCancelacionJMSBean extends DBTester implements MessageListener {
  static final Logger logger = Logger.getLogger("VisaCancelacionJMSBean");
  @Resource
  private MessageDrivenContext mdc;

  private static final String UPDATE_CANCELA_QRY = "update pago " +
                                                   "set codRespuesta = 999 " +
                                                   "where idAutorizacion = ?";
  private static final String SELECT_COD_RESPUESTA = "select codRespuesta, importe, numeroTarjeta " +
                                                     "from pago " +
                                                     "where idAutorizacion = ?";
  private static final String RECTIFICA_SALDO_QRY = "update tarjeta as t1 " +
                                                    "set saldo = saldo + importe " +
                                                    "from pago where pago.idAutorizacion = ? " +
                                                    "and pago.numeroTarjeta = t1.numeroTarjeta";
   // TODO : Definir UPDATE sobre la tabla pagos para poner
   // codRespuesta a 999 dado un código de autorización 


  public VisaCancelacionJMSBean() {
  }

  // TODO : Método onMessage de ejemplo
  // Modificarlo para ejecutar el UPDATE definido más arriba,
  // asignando el idAutorizacion a lo recibido por el mensaje
  // Para ello conecte a la BD, prepareStatement() y ejecute correctamente
  // la actualización
  public void onMessage(Message inMessage) {
      TextMessage msg = null;
      int idAutorizacion;
      Connection con = null;
      PreparedStatement pstmt = null;
      ResultSet rs = null;

      try {
          if (inMessage instanceof TextMessage) {
              msg = (TextMessage) inMessage;
              logger.info("MESSAGE BEAN: Message received: " + msg.getText());
              idAutorizacion = Integer.parseInt(msg.getText());
              con = getConnection();

              pstmt = con.prepareStatement(SELECT_COD_RESPUESTA);
              pstmt.setInt(1, idAutorizacion);
              rs = pstmt.executeQuery();
              rs.next();

              if(rs.getString("codRespuesta").equals("000")){
                pstmt = con.prepareStatement(UPDATE_CANCELA_QRY);
                pstmt.setInt(1, idAutorizacion);
                pstmt.execute();
              
                pstmt = con.prepareStatement(RECTIFICA_SALDO_QRY);
                pstmt.setInt(1,idAutorizacion);
                pstmt.execute();
              }
              
            } else {
              logger.warning(
                      "Message of wrong type: "
                      + inMessage.getClass().getName());
            }
        } catch (JMSException e) {
          e.printStackTrace();
          mdc.setRollbackOnly();
        } catch (Throwable te) {
          te.printStackTrace();
      }
      if(con != null){
        try{
            closeConnection(con);
        }catch(SQLException sqle) {
            sqle.printStackTrace();
        }
        con = null;
      }
  }


}
