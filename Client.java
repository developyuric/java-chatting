import java.awt.*; 
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class Client extends JFrame implements ActionListener {
    private BufferedReader in = null;
    private BufferedWriter out = null;
    private Socket socket = null;
    private JPanel chatPanel = null;
    private JTextField sender = null;
    private JButton sendButton;
    private JButton leaveButton;
    private JScrollPane scrollPane = null;
    private String clientName = "Client"; // Client name

    public Client() {
        setTitle("Client Chat Window");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Container c = this.getContentPane();
        c.setLayout(new BorderLayout());

        // Chat Panel Settings
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(new Color(238, 238, 238)); // Pastel background color

        // Scroll
        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(null);
        c.add(scrollPane, BorderLayout.CENTER);

        // Input Field and Buttons
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));

        sender = new JTextField();
        sender.setFont(new Font("Arial", Font.PLAIN, 16));
        sender.setBackground(new Color(255, 178, 125));
        sender.setForeground(Color.black);
        sender.setCaretColor(Color.black);
        sender.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        sender.addActionListener(this); // ActionListener for Enter key

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Arial", Font.PLAIN, 18));
        sendButton.setBackground(new Color(255, 127, 39));
        sendButton.setForeground(Color.WHITE);
        sendButton.addActionListener(this);

        leaveButton = new JButton("Leave");
        leaveButton.setFont(new Font("Arial", Font.PLAIN, 18));
        leaveButton.setBackground(new Color(213, 97, 18));
        leaveButton.setForeground(Color.WHITE);
        leaveButton.addActionListener(e -> leaveChatRoom());

        inputPanel.add(sender);
        inputPanel.add(sendButton);
        inputPanel.add(leaveButton);
        c.add(inputPanel, BorderLayout.SOUTH);

        setSize(450, 600);
        setVisible(true);

        try {
            setupConnection();
        } catch (IOException e) {
            handleError(e.getMessage());
        }

        // Start Thread for Receiving Messages
        Thread receiverThread = new Thread(new Receiver());
        receiverThread.start();
    }

    // Server Connection Settings
    private void setupConnection() throws IOException {
        socket = new Socket("localhost", 9999); // Connect to server
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    // Error handling
    private static void handleError(String string) {
        System.out.println(string);
        System.exit(1);
    }

    // Send Message to Server
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendButton || e.getSource() == sender) {
            String msg = sender.getText();
            try {
                if (!msg.isEmpty()) {
                    out.write(clientName + ": " + msg + "\n");
                    out.flush();
                    addChatMessage(msg, true);
                    sender.setText(null);
                }
            } catch (IOException e1) {
                handleError(e1.getMessage());
            }
        }
    }

    // Add Chat Message
    private void addChatMessage(String msg, boolean isClient) {
        JPanel messageBubble = new JPanel();
        messageBubble.setLayout(new BoxLayout(messageBubble, BoxLayout.X_AXIS));

        JLabel messageLabel = new JLabel(msg);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        messageLabel.setBackground(isClient ? new Color(255, 232, 224) : new Color(255, 204, 169));
        messageLabel.setOpaque(true);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Message Alignment
        if (isClient) {
            messageBubble.add(Box.createHorizontalGlue());
            messageBubble.add(messageLabel);
        } else {
            messageBubble.add(messageLabel);
            messageBubble.add(Box.createHorizontalGlue());
        }

        chatPanel.add(messageBubble);
        chatPanel.revalidate();
        chatPanel.repaint();

        // Auto-scroll
        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
    }

    // Handle leaving the chat room
    private void leaveChatRoom() {
        try {
            String leaveMessage = clientName + " has left the chat room";
            out.write(leaveMessage + "\n");
            out.flush();
            addChatMessage(leaveMessage, false); // Add message to chat panel
            socket.close();
            System.exit(0); // Close the application
        } catch (IOException e) {
            handleError(e.getMessage());
        }
    }

    // Add centered message
    private void addCenteredMessage(String msg) {
        JPanel messageBubble = new JPanel();
        messageBubble.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel messageLabel = new JLabel(msg);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        messageLabel.setBackground(new Color(0, 0, 0, 0)); // Transparent background
        messageLabel.setForeground(Color.RED); // Red text color
        messageLabel.setOpaque(true);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        messageBubble.add(messageLabel, gbc);

        chatPanel.add(messageBubble);
        chatPanel.revalidate();
        chatPanel.repaint();

        // Auto-scroll
        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
    }

    // Message Receiving Thread
    private class Receiver implements Runnable {
        @Override
        public void run() {
            String msg;
            try {
                while ((msg = in.readLine()) != null) {
                    if (msg.contains("has left the chat room")) {
                        addCenteredMessage(msg); // Display centered message
                    } else {
                        addChatMessage(msg, false); // Display received messages
                    }
                }
            } catch (IOException e) {
                handleError(e.getMessage());
            }
        }
    }
    
    

    public static void main(String[] args) {
        new Client();
    }
}
