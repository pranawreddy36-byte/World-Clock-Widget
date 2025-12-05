import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import java.awt.TrayIcon;
import java.awt.SystemTray;
import java.awt.PopupMenu;
import java.awt.MenuItem;

public class WorldClockWidget extends JFrame {

    private final JLabel dateLabel;
    private final JLabel mainTimeLabel;
    private final JTextArea otherTimesArea;
    private final JComboBox<String> countryBox;
    private final JToggleButton themeToggle;
    private final JButton trayButton;

    private final Map<String, String> countries = new LinkedHashMap<>();
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    private int mouseX, mouseY;
    private boolean darkMode = true;

    // Tray
    private SystemTray tray;
    private TrayIcon trayIcon;

    public WorldClockWidget() {
        // ==== 1. Countries (your list) ====
        countries.put("India", "Asia/Kolkata");
        countries.put("USA", "America/New_York");
        countries.put("UK", "Europe/London");
        countries.put("Australia", "Australia/Sydney");
        countries.put("Japan", "Asia/Tokyo");
        countries.put("Dubai", "Asia/Dubai");

        // ==== 2. Components ====
        dateLabel = new JLabel("Date", SwingConstants.CENTER);
        dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        mainTimeLabel = new JLabel("Time", SwingConstants.CENTER);
        mainTimeLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));

        otherTimesArea = new JTextArea();
        otherTimesArea.setEditable(false);
        otherTimesArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        otherTimesArea.setOpaque(false);
        otherTimesArea.setBorder(null);

        countryBox = new JComboBox<>(countries.keySet().toArray(new String[0]));
        countryBox.setSelectedItem("India");
        countryBox.addActionListener(e -> updateTimes());

        themeToggle = new JToggleButton("Dark");
        themeToggle.setFocusPainted(false);
        themeToggle.setSelected(true);
        themeToggle.addActionListener(e -> {
            darkMode = themeToggle.isSelected();
            themeToggle.setText(darkMode ? "Dark" : "Light");
            applyTheme();
        });

        trayButton = new JButton("To Tray");
        trayButton.setMargin(new Insets(2, 6, 2, 6));
        trayButton.setFocusPainted(false);
        trayButton.addActionListener(e -> minimizeToTray());

        // ==== 3. Top panel (Country + theme + tray button) ====
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.setOpaque(false);

        JLabel countryLabel = new JLabel("Country: ");
        countryLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        topPanel.add(countryLabel);
        topPanel.add(Box.createHorizontalStrut(5));
        topPanel.add(countryBox);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(themeToggle);
        topPanel.add(Box.createHorizontalStrut(5));
        topPanel.add(trayButton);
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // ==== 4. Center panel (Date + main time) ====
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        centerPanel.setOpaque(false);
        centerPanel.add(dateLabel);
        centerPanel.add(mainTimeLabel);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ==== 5. Bottom (converted times) ====
        JScrollPane scrollPane = new JScrollPane(
                otherTimesArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // ==== 6. Frame layout ====
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        setSize(360, 260);
        setLocation(50, 50);
        setAlwaysOnTop(true);
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        getRootPane().setBorder(
                BorderFactory.createLineBorder(new Color(120, 120, 120)));

        applyTheme();
        enableDragging();
        setupTray();

        // ==== 7. Timer ====
        Timer timer = new Timer(1000, e -> updateTimes());
        timer.start();
        updateTimes();
    }

    // ===== Time update logic (your converter adapted) =====
    private void updateTimes() {
        String selectedCountry = (String) countryBox.getSelectedItem();
        if (selectedCountry == null)
            return;

        String inputZone = countries.get(selectedCountry);
        ZonedDateTime inputTime = ZonedDateTime.now(ZoneId.of(inputZone));

        // Date shown once
        dateLabel.setText("Date: " + inputTime.format(dateFormat));

        // Time of selected country
        mainTimeLabel.setText("Time in " + selectedCountry + ": "
                + inputTime.format(timeFormat));

        // Converted times
        StringBuilder sb = new StringBuilder();
        sb.append("Converted Times:\n");
        for (String country : countries.keySet()) {
            if (!country.equals(selectedCountry)) {
                ZonedDateTime converted = inputTime.withZoneSameInstant(ZoneId.of(countries.get(country)));
                sb.append(String.format("%-10s : %s%n",
                        country, converted.format(timeFormat)));
            }
        }
        otherTimesArea.setText(sb.toString());
    }

    // ===== Dark / Light theme =====
    private void applyTheme() {
        Color bg, fgMain, fgSecondary, borderColor;

        if (darkMode) {
            bg = Color.BLACK;
            fgMain = Color.WHITE;
            fgSecondary = Color.LIGHT_GRAY;
            borderColor = new Color(80, 80, 80);
        } else {
            bg = Color.WHITE;
            fgMain = Color.BLACK;
            fgSecondary = Color.DARK_GRAY;
            borderColor = new Color(180, 180, 180);
        }

        getContentPane().setBackground(bg);

        dateLabel.setForeground(fgMain);
        mainTimeLabel.setForeground(fgMain);
        otherTimesArea.setForeground(fgSecondary);

        themeToggle.setBackground(bg);
        themeToggle.setForeground(fgMain);

        trayButton.setBackground(bg);
        trayButton.setForeground(fgMain);

        getRootPane().setBorder(BorderFactory.createLineBorder(borderColor));
        repaint();
    }

    // ===== Dragging the widget =====
    private void enableDragging() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int x = e.getXOnScreen() - mouseX;
                int y = e.getYOnScreen() - mouseY;
                setLocation(x, y);
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        dateLabel.addMouseListener(mouseAdapter);
        dateLabel.addMouseMotionListener(mouseAdapter);
        mainTimeLabel.addMouseListener(mouseAdapter);
        mainTimeLabel.addMouseMotionListener(mouseAdapter);
        otherTimesArea.addMouseListener(mouseAdapter);
        otherTimesArea.addMouseMotionListener(mouseAdapter);
    }

    // ===== System tray support =====
    private void setupTray() {
        if (!SystemTray.isSupported()) {
            tray = null;
            trayIcon = null;
            return;
        }

        tray = SystemTray.getSystemTray();

        // Simple clock icon
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillOval(1, 1, 14, 14);
        g.setColor(Color.WHITE);
        g.drawLine(8, 8, 8, 3); // hour hand
        g.drawLine(8, 8, 12, 10); // minute hand
        g.dispose();

        PopupMenu popup = new PopupMenu();
        MenuItem openItem = new MenuItem("Open");
        MenuItem exitItem = new MenuItem("Exit");

        openItem.addActionListener(e -> restoreFromTray());
        exitItem.addActionListener(e -> System.exit(0));

        popup.add(openItem);
        popup.add(exitItem);

        trayIcon = new TrayIcon(img, "World Clock Widget", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> restoreFromTray());
    }

    private void minimizeToTray() {
        if (tray == null || trayIcon == null) {
            // If tray unsupported, just hide window
            setState(Frame.ICONIFIED);
            return;
        }
        try {
            tray.add(trayIcon);
            setVisible(false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void restoreFromTray() {
        if (tray != null && trayIcon != null) {
            tray.remove(trayIcon);
        }
        setVisible(true);
        setState(Frame.NORMAL);
        toFront();
        repaint();
    }

    // ===== main =====
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WorldClockWidget clock = new WorldClockWidget();
            clock.setVisible(true);
        });
    }
}