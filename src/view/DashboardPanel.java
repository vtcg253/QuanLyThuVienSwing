package view;

import model.TableData;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Dashboard charts are drawn from database values, without an external chart dependency. */
final class DashboardPanel extends JPanel {
    private static final Color INK = new Color(30, 47, 40);
    private static final Color MUTED = new Color(116, 127, 119);
    private static final Color GREEN = new Color(31, 113, 84);
    private static final Color[] COLORS = {new Color(175, 48, 51), new Color(43, 165, 80),
            new Color(121, 47, 169), new Color(174, 151, 42), new Color(46, 156, 169),
            new Color(90, 114, 185)};

    DashboardPanel(Map<String, Long> statistics, TableData books, Runnable openBooks, Runnable openLoans) {
        super(new BorderLayout());
        setOpaque(false);
        JPanel body = new DashboardBody();
        body.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JPanel header = new JPanel(new BorderLayout(0, 18));
        header.setOpaque(false);
        JPanel intro = new JPanel(new GridLayout(0, 1, 0, 8));
        intro.setOpaque(false);
        intro.add(label("Dashboard quản lý", 27, true, INK));
        intro.add(label("Tổng quan hoạt động thư viện và tình trạng kho sách.", 15, false, MUTED));
        header.add(intro, BorderLayout.NORTH);
        JPanel tiles = new JPanel(new GridLayout(1, 6, 12, 0));
        tiles.setOpaque(false);
        int index = 0;
        for (Map.Entry<String, Long> entry : statistics.entrySet()) {
            Color tint = index == 3 ? new Color(255, 248, 235) : index == 4 ? new Color(239, 247, 255)
                    : index == 5 ? new Color(255, 241, 241) : new Color(237, 248, 243);
            JPanel tile = new Surface(new BorderLayout(0, 8), tint);
            tile.setBorder(BorderFactory.createEmptyBorder(18, 16, 18, 12));
            tile.add(label(entry.getKey().toUpperCase(java.util.Locale.ROOT), 12, false, MUTED), BorderLayout.NORTH);
            tile.add(label(entry.getValue().toString(), 32, true, INK), BorderLayout.CENTER);
            if (index == 5) {
                JButton link = link("Xem phiếu →", openLoans);
                link.setForeground(COLORS[0]);
                tile.add(link, BorderLayout.SOUTH);
            }
            tiles.add(tile);
            index++;
        }
        header.add(tiles, BorderLayout.CENTER);
        body.add(header, BorderLayout.NORTH);
        JPanel charts = new JPanel(new GridBagLayout());
        charts.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.68;
        c.gridx = 0;
        c.insets = new Insets(0, 0, 0, 16);
        charts.add(chartCard("Tồn kho theo đầu sách", "Số cuốn hiện còn trong kho của từng đầu sách.",
                "Xem danh sách →", openBooks, new StockChart(books.rows())), c);
        c.gridx = 1;
        c.weightx = 0.32;
        c.insets = new Insets(0, 0, 0, 0);
        charts.add(chartCard("Tỷ lệ mượn theo đầu sách", "Tỷ trọng số cuốn đã mượn.",
                "Phiếu mượn →", openLoans, new LoanChart(books.rows())), c);
        body.add(charts, BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(new Color(245, 243, 236));
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER);
    }

    private static JPanel chartCard(String title, String subtitle, String action, Runnable callback, JPanel chart) {
        JPanel card = new Surface(new BorderLayout(0, 14), Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JPanel heading = new JPanel(new BorderLayout(0, 6));
        heading.setOpaque(false);
        heading.add(label(title, 17, true, INK), BorderLayout.NORTH);
        heading.add(label(subtitle, 12, false, MUTED), BorderLayout.CENTER);
        heading.add(link(action, callback), BorderLayout.SOUTH);
        card.add(heading, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        card.setMinimumSize(new Dimension(270, 400));
        return card;
    }

    private static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    private static JButton link(String text, Runnable callback) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setForeground(GREEN);
        button.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> callback.run());
        return button;
    }

    private static class Surface extends JPanel {
        private final Color fill;
        Surface(LayoutManager layout, Color fill) { super(layout); this.fill = fill; setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            g.setColor(new Color(231, 226, 215));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            g.dispose();
        }
    }

    private static final class DashboardBody extends Surface implements Scrollable {
        DashboardBody() { super(new BorderLayout(0, 24), Color.WHITE); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 20; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return visible.height - 20; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private abstract static class Chart extends JPanel {
        final List<Object[]> rows;
        Chart(List<Object[]> rows) { this.rows = rows; setOpaque(false); }
        Graphics2D graphics(Graphics source) {
            Graphics2D g = (Graphics2D) source.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            return g;
        }
        void center(Graphics2D g, String text, int x, int y) {
            g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y);
        }
        String fit(Graphics2D g, String text, int width) {
            if (g.getFontMetrics().stringWidth(text) <= width) return text;
            while (!text.isEmpty() && g.getFontMetrics().stringWidth(text + "…") > width)
                text = text.substring(0, text.length() - 1);
            return text + "…";
        }
        long value(Object[] row, int column) { return ((Number) row[column]).longValue(); }
    }

    private static final class StockChart extends Chart {
        StockChart(List<Object[]> rows) {
            super(rows);
            setPreferredSize(new Dimension(540, 350));
        }
        @Override protected void paintComponent(Graphics source) {
            super.paintComponent(source);
            Graphics2D g = graphics(source);
            int count = Math.min(rows.size(), 8);
            if (count == 0) {
                g.setColor(MUTED); center(g, "Chưa có dữ liệu sách", getWidth() / 2, 100); g.dispose(); return;
            }
            long max = 1;
            for (int i = 0; i < count; i++) max = Math.max(max, value(rows.get(i), 2));
            int baseline = Math.min(getHeight() - 65, 290);
            int height = Math.max(20, baseline - 35);
            double slot = (getWidth() - 12.0) / count;
            g.setColor(new Color(237, 241, 237));
            for (int i = 0; i < 5; i++) g.drawLine(6, baseline - height * i / 4, getWidth() - 6, baseline - height * i / 4);
            for (int i = 0; i < count; i++) {
                Object[] row = rows.get(i);
                int x = (int) (6 + slot * (i + 0.5));
                int width = Math.max(6, (int) (slot * 0.48));
                int bar = (int) (height * value(row, 2) / max);
                g.setPaint(new GradientPaint(0, baseline - bar, new Color(105, 179, 143), 0, baseline, GREEN));
                if (bar > 0) g.fillRoundRect(x - width / 2, baseline - bar, width, bar, 10, 10);
                g.setColor(INK);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                center(g, Long.toString(value(row, 2)), x, baseline - bar - 10);
                center(g, fit(g, row[1].toString(), (int) slot - 8), x, baseline + 22);
                g.setColor(MUTED);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
                center(g, row[0].toString(), x, baseline + 43);
            }
            if (rows.size() > count) {
                g.setColor(MUTED); center(g, "Hiển thị 8 đầu sách có tồn kho cao nhất", getWidth() / 2, baseline + 62);
            }
            g.dispose();
        }
    }

    private static final class LoanChart extends Chart {
        private final long total;
        LoanChart(List<Object[]> input) {
            super(new ArrayList<>(input));
            rows.removeIf(row -> value(row, 3) == 0);
            rows.sort(Comparator.comparingLong((Object[] row) -> value(row, 3)).reversed());
            total = rows.stream().mapToLong(row -> value(row, 3)).sum();
            // Keep the legend compact; the final segment includes all remaining books.
            if (rows.size() > 6) {
                long other = rows.subList(5, rows.size()).stream().mapToLong(row -> value(row, 3)).sum();
                rows.subList(5, rows.size()).clear();
                rows.add(new Object[]{"", "Sách khác", 0, other});
            }
            setPreferredSize(new Dimension(285, 270 + rows.size() * 32));
        }
        @Override protected void paintComponent(Graphics source) {
            super.paintComponent(source);
            Graphics2D g = graphics(source);
            int size = Math.min(220, getWidth() - 24);
            int x = (getWidth() - size) / 2;
            double start = 90;
            if (total == 0) { g.setColor(new Color(233, 239, 234)); g.fillOval(x, 6, size, size); }
            for (int i = 0; i < rows.size(); i++) {
                double extent = value(rows.get(i), 3) * 360.0 / total;
                g.setColor(COLORS[i % COLORS.length]);
                g.fill(new Arc2D.Double(x, 6, size, size, start, -extent, Arc2D.PIE));
                start -= extent;
            }
            int hole = (int) (size * 0.61);
            g.setColor(Color.WHITE);
            g.fillOval((getWidth() - hole) / 2, 6 + (size - hole) / 2, hole, hole);
            g.setColor(INK);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
            center(g, Long.toString(total), getWidth() / 2, size / 2 + 10);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g.setColor(MUTED);
            center(g, "cuốn đã mượn", getWidth() / 2, size / 2 + 31);
            if (total == 0) center(g, "Chưa có dữ liệu mượn sách", getWidth() / 2, size + 42);
            for (int i = 0; i < rows.size(); i++) {
                Object[] row = rows.get(i);
                int y = size + 40 + i * 34;
                g.setColor(COLORS[i % COLORS.length]);
                g.fillRoundRect(0, y - 11, 10, 10, 4, 4);
                g.setColor(INK);
                String quantity = value(row, 3) + " (" + Math.round(value(row, 3) * 100.0 / total) + "%)";
                int right = getWidth() - g.getFontMetrics().stringWidth(quantity);
                g.drawString(fit(g, row[1].toString(), Math.max(15, right - 24)), 18, y);
                g.drawString(quantity, right, y);
                g.setColor(new Color(237, 232, 222));
                g.drawLine(0, y + 12, getWidth(), y + 12);
            }
            g.dispose();
        }
    }
}
