package com.example.logintegrity.desktop;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

/** Optional desktop companion. Starts after the Spring Boot server is running. */
public class SwingConsole {
    private static final String DASHBOARD_URL = System.getProperty("dashboard.url","https://algologguard-java.onrender.com");
     private static final String API = System.getProperty("api.url","https://algologguard-java.onrender.com/api");
    public static void main(String[] args) { SwingUtilities.invokeLater(SwingConsole::show); }
    private static void show() {
        JFrame frame=new JFrame("SecureLog • Integrity Console"); frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.setSize(760,560); frame.setLocationRelativeTo(null);
        JPanel root=new JPanel(new BorderLayout(12,12)); root.setBorder(BorderFactory.createEmptyBorder(18,18,18,18)); root.setBackground(new Color(13,20,36));
        JLabel title=new JLabel("SECURELOG  /  DESKTOP CONSOLE"); title.setForeground(Color.WHITE); title.setFont(new Font("SansSerif",Font.BOLD,20)); root.add(title,BorderLayout.NORTH);
        JTextArea output=new JTextArea(); output.setEditable(false); output.setBackground(new Color(22,32,52)); output.setForeground(new Color(210,224,245)); output.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
        JPanel form=new JPanel(new GridLayout(0,2,8,8)); form.setOpaque(false);
        JTextField source=new JTextField("auth-service"), type=new JTextField("LOGIN_FAILURE"), actor=new JTextField("demo-user"), ip=new JTextField("192.0.2.10");
        JTextField message=new JTextField("Multiple failed login attempts detected"); JComboBox<String> severity=new JComboBox<>(new String[]{"INFO","LOW","MEDIUM","HIGH","CRITICAL"});
        add(form,"Severity",severity); add(form,"Source",source); add(form,"Event type",type); add(form,"Actor",actor); add(form,"IP address",ip); add(form,"Message",message);
        JButton submit=new JButton("Anchor security log"), refresh=new JButton("Check API status"), website=new JButton("Open web dashboard");
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT)); actions.setOpaque(false); actions.add(submit); actions.add(refresh); actions.add(website);
        JPanel center=new JPanel(new BorderLayout(8,8)); center.setOpaque(false); center.add(form,BorderLayout.NORTH); center.add(new JScrollPane(output),BorderLayout.CENTER); center.add(actions,BorderLayout.SOUTH); root.add(center,BorderLayout.CENTER);
        submit.addActionListener(e -> { try { String json="{\"severity\":\""+esc(severity.getSelectedItem().toString())+"\",\"source\":\""+esc(source.getText())+"\",\"eventType\":\""+esc(type.getText())+"\",\"message\":\""+esc(message.getText())+"\",\"actor\":\""+esc(actor.getText())+"\",\"ipAddress\":\""+esc(ip.getText())+"\"}"; HttpRequest req=HttpRequest.newBuilder(URI.create(API+"/logs")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build(); HttpResponse<String> res=HttpClient.newHttpClient().send(req,HttpResponse.BodyHandlers.ofString()); output.append("POST /logs → "+res.statusCode()+"\n"+res.body()+"\n\n"); } catch(Exception ex){ output.append("Error: "+ex.getMessage()+"\n"); } });
        refresh.addActionListener(e -> { try { HttpResponse<String> res=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(API+"/health")).GET().build(),HttpResponse.BodyHandlers.ofString()); output.append("GET /health → "+res.statusCode()+"\n"+res.body()+"\n\n"); } catch(Exception ex){ output.append("Error: "+ex.getMessage()+"\n"); } });
        website.addActionListener(e -> {
    try {
        if (!Desktop.isDesktopSupported()) {
            throw new UnsupportedOperationException(
                "Desktop is not supported on this system"
            );
        }

        Desktop desktop = Desktop.getDesktop();

        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            throw new UnsupportedOperationException(
                "Browser opening is not supported"
            );
        }

        URI dashboardUri = URI.create(DASHBOARD_URL);
        desktop.browse(dashboardUri);

        output.append("Opening dashboard: "
                + DASHBOARD_URL + "\n");

    } catch (Exception ex) {
        output.append("Dashboard error: "
                + ex.getMessage() + "\n");

        JOptionPane.showMessageDialog(
            null,
            "Unable to open dashboard:\n" + ex.getMessage(),
            "Dashboard Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
});
        frame.setContentPane(root); frame.setVisible(true);
    }
    private static void add(JPanel p,String label,JComponent c){ JLabel l=new JLabel(label); l.setForeground(new Color(180,197,221)); p.add(l); p.add(c); }
    private static String esc(String s){ return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," "); }
}
