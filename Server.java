import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class Server extends JFrame {
    private ServerSocket serverSocket = null;
    private ArrayList<ClientHandler> clients = new ArrayList<>();
    private JPanel chatPanel = null;
    private JTextField sender = null;
    private JButton sendButton;
    private JButton leaveButton;
    private JScrollPane scrollPane = null;

    public Server() {
        setTitle("Server Chat Window");
        setSize(450, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // UI 구성
        Container c = this.getContentPane();
        c.setLayout(new BorderLayout());

        // 채팅 패널 설정
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(new Color(238, 238, 238)); // 파스텔톤 배경색

        // 스크롤 가능하게 설정
        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(null);
        c.add(scrollPane, BorderLayout.CENTER);

        // 입력창 및 버튼 설정
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));

        sender = new JTextField();
        sender.setFont(new Font("Arial", Font.PLAIN, 16));
        sender.setBackground(new Color(255, 178, 125));
        sender.setForeground(Color.black);
        sender.setCaretColor(Color.black);
        sender.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        sender.addActionListener(e -> sendMessage());
        sender.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Arial", Font.PLAIN, 18));
        sendButton.setBackground(new Color(255, 127, 39));
        sendButton.setForeground(Color.white);
        sendButton.addActionListener(e -> sendMessage());

        leaveButton = new JButton("Leave");
        leaveButton.setFont(new Font("Arial", Font.PLAIN, 18));
        leaveButton.setBackground(new Color(213, 97, 18));
        leaveButton.setForeground(Color.white);
        leaveButton.addActionListener(e -> leaveChatRoom());

        inputPanel.add(sender);
        inputPanel.add(sendButton);
        inputPanel.add(leaveButton);
        c.add(inputPanel, BorderLayout.SOUTH);

        setVisible(true);

        // 서버 소켓 연결
        try {
            serverSocket = new ServerSocket(9999); // 포트 9999
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 서버와 클라이언트 간 메시지 브로드캐스트
    public void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) { // 메시지를 보낸 클라이언트 제외
                client.sendMessage(message);
            }
        }
    }

    // 채팅 메시지를 채팅 패널에 추가
    private void addChatMessage(String msg, boolean isServer) {
        JPanel messageBubble = new JPanel();
        messageBubble.setLayout(new BoxLayout(messageBubble, BoxLayout.X_AXIS));
        JLabel messageLabel = new JLabel(msg);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        messageLabel.setOpaque(true);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Check if the message is about someone leaving the chat room
        if (msg.contains("has left the chat room")) {
            messageLabel.setBackground(new Color(0, 0, 0, 0)); // Transparent background
            messageLabel.setForeground(Color.RED); // Red text color
            messageBubble.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(10, 15, 10, 15);
            gbc.anchor = GridBagConstraints.CENTER;
            messageBubble.add(messageLabel, gbc);
        } else {
            messageLabel.setBackground(isServer ? new Color(255, 232, 224) : new Color(255, 204, 169)); // Server and client color differentiation
            if (isServer) {
                messageBubble.add(Box.createHorizontalGlue());
                messageBubble.add(messageLabel);
            } else {
                messageBubble.add(messageLabel);
                messageBubble.add(Box.createHorizontalGlue());
            }
        }

        chatPanel.add(messageBubble);
        chatPanel.revalidate();
        chatPanel.repaint();

        // Auto-scroll
        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
    }

    // 메시지 전송
    private void sendMessage() {
        String msg = sender.getText();
        if (!msg.isEmpty()) {
            broadcastMessage("Server: " + msg, null);
            addChatMessage(msg, true); // 서버에서 보내는 메시지
            sender.setText("");
        }
    }

    // 채팅 방 나가기
    private void leaveChatRoom() {
        String leaveMessage = "Server has left the chat room";
        broadcastMessage(leaveMessage, null);
        addChatMessage(leaveMessage, false); // 서버에서 나갔을 때의 메시지
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0); // 애플리케이션 종료
    }

    // 클라이언트 관리 스레드
    private class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private BufferedWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.contains("has left the chat room")) {
                        broadcastMessage(message, this); // Notify all clients
                        addChatMessage(message, false); // Display in server chat
                        break; // Exit loop when client leaves
                    } else {
                        broadcastMessage(message, this); // All clients receive the message
                        addChatMessage(message, false); // Server chat displays the message
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // 클라이언트에게 메시지 전송
        public void sendMessage(String message) {
            try {
                out.write(message + "\n");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}
