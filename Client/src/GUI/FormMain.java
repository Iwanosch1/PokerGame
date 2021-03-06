package GUI;

import GameLogic.Card;
import GameLogic.Stack;
import GameLogic.enums.Role;
import Network.HandlerClient;
import handChecker.PokerCard;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Vector;

public class FormMain extends JFrame{
    private JPanel contentPanel;
    public JTextArea textArea;
    private JTextField txt_input;
    public JPanel panelGame;
    public JPanel panelHandcards;
    public JPanel panelOpencards;
    public JPanel panelOpponents;

    public DialogLogin dial = new DialogLogin();
    private HandlerClient handlerClient;

    public Stack cards;
    public Stack openCards = new Stack();
    public Role role;

    public FormMain() {
        super();
        setContentPane(contentPanel);
        pack();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setVisible(true);

        txt_input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onEnterPressed();
                }
            }
        });

    }



    protected void connectToServer() throws IOException {
        dial.showDialog();
        handlerClient = new HandlerClient(this, dial.txt_ip.getText(), 25565);
        handlerClient.start();
    }

    private void onEnterPressed() {
        if (txt_input.getText() != "") {
            handlerClient.sendData("BETGIVEN", txt_input.getText());
            txt_input.setText("");
        }
    }

    public static void main(String[] args) {
        FormMain main = new FormMain();

        try {
            main.connectToServer();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(main,
                    "Kein Server an der angebenen Adresse gefunden.",
                    "Verbindungsfehler",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
