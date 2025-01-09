import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class TetrisGame extends JPanel {
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int BLOCK_SIZE = 30;
    private static final int GRID_LINE_WIDTH = 1;
    private static final int SIDE_PANEL_WIDTH = 200; 
    private static final int BUTTON_SIZE = 40;
    private static final int BUTTON_SPACING = 5;
    private static final Color BUTTON_COLOR = new Color(80, 80, 80);
    private static final Color BUTTON_BORDER_COLOR = new Color(150, 150, 150);
    private Rectangle upButton;
    private Rectangle leftButton;
    private Rectangle rightButton;
    private Rectangle downButton;
    private Rectangle spaceButton;
    private Rectangle pauseButton;
    private boolean isPaused = false;
    private String activeButton = "";
    
    private static final Color GRID_COLOR = new Color(50, 50, 50);
    private static final Color[] COLORS = {
        new Color(0, 0, 0, 0), 
        Color.CYAN, 
        Color.BLUE, 
        Color.ORANGE, 
        Color.YELLOW, 
        Color.GREEN, 
        Color.PINK, 
        Color.RED, 
    };
    
    private static final int[][][] SHAPES = {
        {{1, 1, 1, 1}},
        {{2, 0, 0}, {2, 2, 2}}, 
        {{0, 0, 3}, {3, 3, 3}}, 
        {{4, 4}, {4, 4}}, 
        {{0, 5, 5}, {5, 5, 0}}, 
        {{0, 6, 0}, {6, 6, 6}}, 
        {{7, 7, 0}, {0, 7, 7}}, 
    };
    
    private int[][] board = new int[BOARD_HEIGHT][BOARD_WIDTH];
    private int[][] currentPiece;
    private int[][] nextPiece;
    private int currentX, currentY;
    private Timer gameTimer;
    private long lastFrameTime = 0;
    private static final long FRAME_DELAY = 16; 
    private int score = 0;
    
    public TetrisGame() {
        setPreferredSize(new Dimension(BOARD_WIDTH * BLOCK_SIZE + SIDE_PANEL_WIDTH + 1, BOARD_HEIGHT * BLOCK_SIZE + 1));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);

        upButton = new Rectangle();
        leftButton = new Rectangle();
        rightButton = new Rectangle();
        downButton = new Rectangle();
        spaceButton = new Rectangle();
        pauseButton = new Rectangle();

        resetGame();
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (pauseButton.contains(e.getPoint())) {
                    activeButton = "pause";
                    togglePause();
                } else if (!isPaused) { 
                    if (upButton.contains(e.getPoint())) {
                        activeButton = "up";
                        rotatePiece();
                    } else if (leftButton.contains(e.getPoint())) {
                        activeButton = "left";
                        movePieceLeft();
                    } else if (rightButton.contains(e.getPoint())) {
                        activeButton = "right";
                        movePieceRight();
                    } else if (downButton.contains(e.getPoint())) {
                        activeButton = "down";
                        dropPiece();
                    } else if (spaceButton.contains(e.getPoint())) {
                        activeButton = "space";
                        hardDrop();
                    }
                }
                repaint();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                activeButton = "";
                repaint();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_P) {
                    activeButton = "pause";
                    togglePause();
                } else if (!isPaused) { 
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT:
                            activeButton = "left";
                            movePieceLeft();
                            break;
                        case KeyEvent.VK_RIGHT:
                            activeButton = "right";
                            movePieceRight();
                            break;
                        case KeyEvent.VK_DOWN:
                            activeButton = "down";
                            dropPiece();
                            break;
                        case KeyEvent.VK_UP:
                            activeButton = "up";
                            rotatePiece();
                            break;
                        case KeyEvent.VK_SPACE:
                            activeButton = "space";
                            hardDrop();
                            break;
                    }
                }
                repaintWithFrameLimit();
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                activeButton = "";
                repaint();
            }
        });

        setFocusable(true);
        
        gameTimer = new Timer(500, e -> {
            dropPiece();
            repaintWithFrameLimit();
        });
        gameTimer.start();
    }
    private void drawArrowKey(Graphics2D g2d, int x, int y, String symbol, String label, String buttonType) {
        Rectangle buttonRect = new Rectangle(x - BUTTON_SIZE/2, y - BUTTON_SIZE/2, BUTTON_SIZE, BUTTON_SIZE);
        switch (buttonType) {
            case "up": upButton = buttonRect; break;
            case "left": leftButton = buttonRect; break;
            case "right": rightButton = buttonRect; break;
            case "down": downButton = buttonRect; break;
        }
        g2d.setColor(activeButton.equals(buttonType) ? BUTTON_COLOR.brighter() : BUTTON_COLOR);
        g2d.fillRect(x - BUTTON_SIZE/2, y - BUTTON_SIZE/2, BUTTON_SIZE, BUTTON_SIZE);
        
        g2d.setColor(BUTTON_BORDER_COLOR);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x - BUTTON_SIZE/2, y - BUTTON_SIZE/2, BUTTON_SIZE, BUTTON_SIZE);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        int symbolWidth = g2d.getFontMetrics().stringWidth(symbol);
        g2d.drawString(symbol, x - symbolWidth/2, y + 7);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        int labelWidth = g2d.getFontMetrics().stringWidth(label);
        g2d.drawString(label, x - labelWidth/2, y + BUTTON_SIZE/2 + 15);
    }
    private void drawSpaceBar(Graphics2D g2d, int x, int y, String label) {
        int width = BUTTON_SIZE * 4;
        int height = BUTTON_SIZE/2;
        
        spaceButton = new Rectangle(x, y, width, height);
        
        g2d.setColor(activeButton.equals("space") ? BUTTON_COLOR.brighter() : BUTTON_COLOR);
        g2d.fillRect(x, y, width, height);
        
        g2d.setColor(BUTTON_BORDER_COLOR);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x, y, width, height);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("SPACE", x + width/2 - 25, y + height/2 + 5);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        int labelWidth = g2d.getFontMetrics().stringWidth(label);
        g2d.drawString(label, x + width/2 - labelWidth/2, y + height + 15);
    }
    private void drawPauseButton(Graphics2D g2d, int x, int y) {
        int width = BUTTON_SIZE * 2;
        int height = BUTTON_SIZE;
        
        pauseButton = new Rectangle(x, y, width, height);
        
        g2d.setColor(activeButton.equals("pause") ? BUTTON_COLOR.brighter() : BUTTON_COLOR);
        g2d.fillRect(x, y, width, height);
        
        g2d.setColor(BUTTON_BORDER_COLOR);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x, y, width, height);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String buttonText = isPaused ? "RESUME" : "PAUSE";
        int textWidth = g2d.getFontMetrics().stringWidth(buttonText);
        g2d.drawString(buttonText, x + width/2 - textWidth/2, y + height/2 + 5);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String shortcut = "(P)";
        int shortcutWidth = g2d.getFontMetrics().stringWidth(shortcut);
        g2d.drawString(shortcut, x + width/2 - shortcutWidth/2, y + height + 17);
    }

    private void drawSidePanel(Graphics2D g2d) {
        int panelX = BOARD_WIDTH * BLOCK_SIZE + 20;
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Score: " + score, panelX, 50);
        
        drawPauseButton(g2d, panelX + SIDE_PANEL_WIDTH/2 - BUTTON_SIZE, 80);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Next Piece:", panelX, 180);
        
        if (nextPiece != null) {
            int previewX = panelX + 20;
            int previewY = 220;
            
            for (int y = 0; y < nextPiece.length; y++) {
                for (int x = 0; x < nextPiece[y].length; x++) {
                    if (nextPiece[y][x] != 0) {
                        drawBlock(g2d, x + (previewX / BLOCK_SIZE), y + (previewY / BLOCK_SIZE), 
                                COLORS[nextPiece[y][x]]);
                    }
                }
            }
        }
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Controls:", panelX, 340);
        
        int centerX = panelX + SIDE_PANEL_WIDTH / 2;
        int startY = 380;
        
        drawArrowKey(g2d, centerX, startY, "↑", "Rotate", "up");
        drawArrowKey(g2d, centerX - BUTTON_SIZE - BUTTON_SPACING, startY + BUTTON_SIZE + BUTTON_SPACING, "←", "Left", "left");
        drawArrowKey(g2d, centerX, startY + BUTTON_SIZE + BUTTON_SPACING, "↓", "Down", "down");
        drawArrowKey(g2d, centerX + BUTTON_SIZE + BUTTON_SPACING, startY + BUTTON_SIZE + BUTTON_SPACING, "→", "Right", "right");
        
        int spaceBarY = startY + 2 * (BUTTON_SIZE + BUTTON_SPACING) + 20;
        drawSpaceBar(g2d, centerX - BUTTON_SIZE * 2, spaceBarY, "Hard Drop");
        
        if (isPaused) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, BOARD_WIDTH * BLOCK_SIZE, BOARD_HEIGHT * BLOCK_SIZE);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            String pausedText = "PAUSED";
            int textWidth = g2d.getFontMetrics().stringWidth(pausedText);
            g2d.drawString(pausedText, (BOARD_WIDTH * BLOCK_SIZE - textWidth) / 2, BOARD_HEIGHT * BLOCK_SIZE / 2);
        }
    }
    
    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            gameTimer.stop();
        } else {
            gameTimer.start();
        }
        repaint();
    }

    private void resetGame() {
        score = 0;
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                board[y][x] = 0;
            }
        }
        nextPiece = generateNewPiece();
        spawnNewPiece();
    }
    
    private int[][] generateNewPiece() {
        int pieceIndex = (int) (Math.random() * SHAPES.length);
        return SHAPES[pieceIndex];
    }
    
    private void spawnNewPiece() {
        currentPiece = nextPiece;
        nextPiece = generateNewPiece();
        currentX = BOARD_WIDTH / 2 - currentPiece[0].length / 2;
        currentY = 0;
        
        if (isCollision()) {
            gameOver();
        }
    }
    
    private boolean isCollision() {
        for (int y = 0; y < currentPiece.length; y++) {
            for (int x = 0; x < currentPiece[y].length; x++) {
                if (currentPiece[y][x] != 0) {
                    if (currentX + x < 0 || currentX + x >= BOARD_WIDTH || 
                        currentY + y >= BOARD_HEIGHT || 
                        (currentY + y >= 0 && board[currentY + y][currentX + x] != 0)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void mergePiece() {
        for (int y = 0; y < currentPiece.length; y++) {
            for (int x = 0; x < currentPiece[y].length; x++) {
                if (currentPiece[y][x] != 0) {
                    board[currentY + y][currentX + x] = currentPiece[y][x];
                }
            }
        }
    }
    
    private void clearLines() {
        int linesCleared = 0;
        for (int y = BOARD_HEIGHT - 1; y >= 0; y--) {
            boolean fullLine = true;
            for (int x = 0; x < BOARD_WIDTH; x++) {
                if (board[y][x] == 0) {
                    fullLine = false;
                    break;
                }
            }
            if (fullLine) {
                linesCleared++;
                for (int i = y; i > 0; i--) {
                    System.arraycopy(board[i - 1], 0, board[i], 0, BOARD_WIDTH);
                }
                for (int x = 0; x < BOARD_WIDTH; x++) {
                    board[0][x] = 0;
                }
                y++;
            }
        }
        if (linesCleared > 0) {
            score += linesCleared * 10 * linesCleared; 
            repaint(); 
        }
    }
    
    private void dropPiece() {
        currentY++;
        if (isCollision()) {
            currentY--;
            mergePiece();
            clearLines();
            spawnNewPiece();
        }
    }
    
    private void movePieceLeft() {
        currentX--;
        if (isCollision()) {
            currentX++;
        }
    }
    
    private void movePieceRight() {
        currentX++;
        if (isCollision()) {
            currentX--;
        }
    }
    
    private void rotatePiece() {
        int[][] rotatedPiece = new int[currentPiece[0].length][currentPiece.length];
        for (int y = 0; y < currentPiece.length; y++) {
            for (int x = 0; x < currentPiece[y].length; x++) {
                rotatedPiece[x][currentPiece.length - y - 1] = currentPiece[y][x];
            }
        }
        
        int[][] prevPiece = currentPiece;
        int prevX = currentX;
        int prevY = currentY;
        
        currentPiece = rotatedPiece;
        if (isCollision()) {
            currentPiece = prevPiece;
            currentX = prevX;
            currentY = prevY;
        }
    }

    private void hardDrop() {
        while (!isCollision()) {
            currentY++;
        }
        currentY--;
        mergePiece();
        clearLines();
        spawnNewPiece();
    }

    private void gameOver() {
        gameTimer.stop();
        JOptionPane.showMessageDialog(this, "Game Over!\nFinal Score: " + score);
        
        resetGame();
        gameTimer.start();
        requestFocusInWindow(); 
    }
    
    private void repaintWithFrameLimit() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime >= FRAME_DELAY) {
            repaint();
            lastFrameTime = currentTime;
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        drawBoard(g2d);
        
        drawSidePanel(g2d);
    }
    
    private void drawBoard(Graphics2D g2d) {
        g2d.setColor(GRID_COLOR);
        for (int y = 0; y <= BOARD_HEIGHT; y++) {
            g2d.fillRect(0, y * BLOCK_SIZE, BOARD_WIDTH * BLOCK_SIZE + 1, GRID_LINE_WIDTH);
        }
        for (int x = 0; x <= BOARD_WIDTH; x++) {
            g2d.fillRect(x * BLOCK_SIZE, 0, GRID_LINE_WIDTH, BOARD_HEIGHT * BLOCK_SIZE + 1);
        }
        
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                if (board[y][x] != 0) {
                    drawBlock(g2d, x, y, COLORS[board[y][x]]);
                }
            }
        }
        
        if (currentPiece != null) {
            for (int y = 0; y < currentPiece.length; y++) {
                for (int x = 0; x < currentPiece[y].length; x++) {
                    if (currentPiece[y][x] != 0) {
                        drawBlock(g2d, currentX + x, currentY + y, COLORS[currentPiece[y][x]]);
                    }
                }
            }
        }
    }

    
    private void drawBlock(Graphics2D g2d, int x, int y, Color color) {
        int px = x * BLOCK_SIZE + GRID_LINE_WIDTH;
        int py = y * BLOCK_SIZE + GRID_LINE_WIDTH;
        int size = BLOCK_SIZE - GRID_LINE_WIDTH;
        
        g2d.setColor(color.brighter());
        g2d.fillRect(px, py, size, size);
        
        g2d.setColor(color.darker());
        g2d.drawRect(px, py, size - 1, size - 1);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tetris");
            TetrisGame gamePanel = new TetrisGame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(gamePanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }
}