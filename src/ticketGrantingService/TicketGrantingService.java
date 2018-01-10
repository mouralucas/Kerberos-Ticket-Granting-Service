package ticketGrantingService;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lucas Penha de Moura - 1208977
 */
public class TicketGrantingService {

    private String T_C_TGS, T_C_TGS_open, K_TGS, K_C_TGS, K_C_S, N2, T_C_S, K_S, ID_C, ID_S, T_A, M4;
    private String M3_Pt1_encrypt;
    private String M3_Pt1_open;

    public void startTGS() throws IOException {
        String clientMessage;

        ServerSocket welcomeSocket = new ServerSocket();
        welcomeSocket.setReuseAddress(true);
        welcomeSocket.bind(new InetSocketAddress(54321));

        while (true) {

            Socket connectionSocket = welcomeSocket.accept();
            BufferedReader messageFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

            DataOutputStream messageToClient = new DataOutputStream(connectionSocket.getOutputStream());
            clientMessage = messageFromClient.readLine();

            K_C_S_gen();
            K_TGS = newKeyGen("KTGS");
            K_S = newKeyGen("KS");
            opemMsg(clientMessage);
            messageGen();

            messageToClient.writeBytes(M4 + '\n');
        }
    }

    private void opemMsg(String mensagem) {
        String splitMsg[] = mensagem.split(";");
        //abre as 4 partes da msg
        M3_Pt1_encrypt = splitMsg[0];
        T_C_TGS = splitMsg[1];
        ID_S = splitMsg[2];
        N2 = splitMsg[3];

        //descriptografa o T_C_TGS e pega as informações
        T_C_TGS_open = Encode.decode(T_C_TGS, K_TGS);
        String TCTGS[] = T_C_TGS_open.split(";");

        ID_C = TCTGS[0];
        K_C_TGS = TCTGS[2];

    }

    private void messageGen() {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        T_A = new SimpleDateFormat("dd/MM/yyyy-HH:mm").format(timestamp.getTime());

        String M4_Pt1 = Encode.encode(K_C_S + ";" + N2, K_C_TGS);
        T_C_S = Encode.encode(ID_C + ";" + T_A + ";" + K_C_S, K_S);

        M4 = M4_Pt1 + ";" + T_C_S;
        System.out.println("M4: " + M4);
    }

    private void K_C_S_gen() {
        K_C_S = KeyGen.newKey();
        System.out.println("K_C_S = " + K_C_S);
    }

    public String newKeyGen(String passwd) {
        String retorno = null;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(passwd.getBytes());

            byte byteData[] = md.digest();

            StringBuilder hash = new StringBuilder();
            for (int i = 0; i < byteData.length; i++) {
                hash.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            retorno = hash.toString();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(TicketGrantingService.class.getName()).log(Level.SEVERE, null, ex);
        }

        return retorno.substring(0, 16);
    }
}
