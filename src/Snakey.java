import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.stage.StageStyle;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.geometry.Pos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.media.AudioClip;
import java.io.File;

public class HelloFX extends Application {

    static final int TILE = 20;
    static final int COLS = 30;
    static final int ROWS = 20;

    List<int[]> snake = new ArrayList<>();
    int[] food = new int[2];

    int dx = 1, dy = 0;
    int nextDx = 1, nextDy = 0;

    int score = 0;
    boolean gameOver = false;
    boolean gameStarted = false;
    boolean paused = false;
    boolean muted = false;
    int speed = 150;
    String difficultyLevel = "MEDIUM";
    VBox startUI;
    VBox gameOverUI;
    HBox difficultyBox;
    Label muteBtnUI;
    Label gameOverScoreLabel;

    Canvas canvas;
    GraphicsContext gc;
    Timeline gameTimer;
    Image bgImage;
    Image headImage;
    Image appleImage;
    Image rockImage;
    AudioClip biteSound;
    List<int[]> rocks = new ArrayList<>();

    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.UNDECORATED);

        canvas = new Canvas(COLS * TILE, ROWS * TILE);
        gc = canvas.getGraphicsContext2D();

        bgImage = new Image("file:src/assests/dirt-bg.png");
        headImage = new Image("file:src/assests/sanke_head.png");
        appleImage = new Image("file:src/assests/apple1.png");
        rockImage = new Image("file:src/assests/png-clipart-rock-stone-pixel-art-nature-hard-pixel-8-bit-removebg-preview.png");
        
        try {
            File soundFile = new File("src/assests/bite.wav");
            if (soundFile.exists()) {
                biteSound = new AudioClip(soundFile.getAbsoluteFile().toURI().toString());
                biteSound.setVolume(1.0);
                System.out.println("Audio fixed: bite.wav loaded.");
            }
        } catch (Throwable t) {
            System.out.println("Audio system unavailable.");
        }

        HBox titleBar = new HBox();
        titleBar.setPrefHeight(30);
        titleBar.setStyle("-fx-background-color: #3e2723; -fx-padding: 0 10 0 10;");
        titleBar.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Snake Game");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");
        
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label muteBtn = new Label(" \ud83d\udd0a ");
        muteBtn.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
        muteBtn.setOnMouseClicked(e -> {
            muted = !muted;
            muteBtn.setText(muted ? " \ud83d\udd07 " : " \ud83d\udd0a ");
            if (muted) {
                muteBtn.setStyle("-fx-text-fill: #ffa000; -fx-font-size: 14px; -fx-cursor: hand;");
            } else {
                muteBtn.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
            }
        });

        Label closeBtn = new Label(" \u2715 ");
        closeBtn.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand;");
        closeBtn.setOnMouseClicked(e -> stage.close());
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-text-fill: #ff5555; -fx-font-size: 16px; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand;"));

        titleBar.getChildren().addAll(titleLabel, spacer, muteBtn, closeBtn);

        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        Pane gamePane = new Pane();
        gamePane.getChildren().add(canvas);

        startUI = buildStartUI();
        gameOverUI = buildGameOverUI();
        
        gamePane.getChildren().addAll(startUI, gameOverUI);
        gameOverUI.setVisible(false);

        VBox root = new VBox(titleBar, gamePane);
        root.setStyle("-fx-border-color: #3e2723; -fx-border-width: 2;");

        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            boolean isActionKey = (code == KeyCode.ENTER || code == KeyCode.SPACE);

            if (!gameStarted) {
                if (isActionKey) startGame();
                if (code == KeyCode.D) {
                    cycleDifficulty();
                    draw();
                }
                return;
            }

            if (gameOver) {
                if (isActionKey) startGame();
                return;
            }

            if (isActionKey) {
                paused = !paused;
                draw();
                return;
            }

            if (paused) return;

            switch (code) {
                case UP:    if (dy != 1)  { nextDx = 0;  nextDy = -1; } break;
                case DOWN:  if (dy != -1) { nextDx = 0;  nextDy =  1; } break;
                case LEFT:  if (dx != 1)  { nextDx = -1; nextDy =  0; } break;
                case RIGHT: if (dx != -1) { nextDx =  1; nextDy =  0; } break;
            }
        });

        stage.setScene(scene);
        stage.setOnShown(e -> {
            root.requestFocus();
            draw();
        });
        stage.show();
        
        root.setFocusTraversable(true);
        root.requestFocus();

        Platform.runLater(() -> {
            root.requestFocus();
            draw();
        });
    }

    void startGame() {
        snake.clear();
        snake.add(new int[] { COLS / 2, ROWS / 2 });

        dx = 1;
        dy = 0;
        nextDx = 1;
        nextDy = 0;
        score = 0;
        gameOver = false;
        gameStarted = true;
        paused = false;
        startUI.setVisible(false);
        gameOverUI.setVisible(false);

        placeRocks();
        placeFood();

        if (gameTimer != null)
            gameTimer.stop();
        gameTimer = new Timeline(new KeyFrame(Duration.millis(speed), e -> update()));
        gameTimer.setCycleCount(Timeline.INDEFINITE);
        gameTimer.play();

        draw();
    }

    private VBox buildStartUI() {
        VBox ui = new VBox(25);
        ui.setAlignment(Pos.CENTER);
        ui.setPrefSize(COLS * TILE, ROWS * TILE);
        ui.setStyle("-fx-background-color: rgba(20, 10, 5, 0.85);");

        Label title = new Label("SNAKE GAME");
        title.setStyle("-fx-text-fill: #efebe9; -fx-font-size: 48px; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");

        VBox diffSection = new VBox(10);
        diffSection.setAlignment(Pos.CENTER);
        Label diffLabel = new Label("CHOOSE DIFFICULTY");
        diffLabel.setStyle("-fx-text-fill: #a1887f; -fx-font-size: 14px; -fx-font-family: 'Consolas';");
        
        difficultyBox = new HBox(15);
        difficultyBox.setAlignment(Pos.CENTER);
        String[] levels = {"EASY", "MEDIUM", "HARD"};
        for (String lv : levels) {
            Label btn = new Label(lv);
            btn.setPrefWidth(85);
            btn.setAlignment(Pos.CENTER);
            btn.setCursor(javafx.scene.Cursor.HAND);
            btn.setOnMouseClicked(e -> setDifficulty(lv));
            difficultyBox.getChildren().add(btn);
        }
        updateDifficultyUI();
        diffSection.getChildren().addAll(diffLabel, difficultyBox);

        muteBtnUI = new Label("SOUND: ON");
        muteBtnUI.setPrefWidth(140);
        muteBtnUI.setAlignment(Pos.CENTER);
        muteBtnUI.setCursor(javafx.scene.Cursor.HAND);
        muteBtnUI.setStyle("-fx-background-color: #5d4037; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5; -fx-font-family: 'Consolas';");
        muteBtnUI.setOnMouseClicked(e -> {
            muted = !muted;
            muteBtnUI.setText(muted ? "SOUND: OFF" : "SOUND: ON");
            muteBtnUI.setStyle(muted ? "-fx-background-color: #3e2723; -fx-text-fill: #a1887f; -fx-padding: 10 20; -fx-background-radius: 5; -fx-font-family: 'Consolas';" 
                                    : "-fx-background-color: #5d4037; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5; -fx-font-family: 'Consolas';");
        });

        Label startBtn = new Label("START GAME");
        startBtn.setPrefWidth(220);
        startBtn.setAlignment(Pos.CENTER);
        startBtn.setCursor(javafx.scene.Cursor.HAND);
        startBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold; -fx-padding: 15 30; -fx-background-radius: 10; -fx-font-family: 'Consolas';");
        startBtn.setOnMouseClicked(e -> startGame());

        ui.getChildren().addAll(title, diffSection, muteBtnUI, startBtn);
        return ui;
    }

    private VBox buildGameOverUI() {
        VBox ui = new VBox(20);
        ui.setAlignment(Pos.CENTER);
        ui.setPrefSize(COLS * TILE, ROWS * TILE);
        ui.setStyle("-fx-background-color: rgba(62, 39, 35, 0.9);"); // Dark reddish brown

        Label title = new Label("GAME OVER");
        title.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 54px; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");

        gameOverScoreLabel = new Label("Final Score: 0");
        gameOverScoreLabel.setStyle("-fx-text-fill: #efebe9; -fx-font-size: 24px; -fx-font-family: 'Consolas';");

        Label restartBtn = new Label("TRY AGAIN");
        restartBtn.setPrefWidth(220);
        restartBtn.setAlignment(Pos.CENTER);
        restartBtn.setCursor(javafx.scene.Cursor.HAND);
        restartBtn.setStyle("-fx-background-color: #d2b48c; -fx-text-fill: #3e2723; -fx-font-size: 22px; -fx-font-weight: bold; -fx-padding: 15 30; -fx-background-radius: 10; -fx-font-family: 'Consolas';");
        restartBtn.setOnMouseClicked(e -> startGame());

        ui.getChildren().addAll(title, gameOverScoreLabel, restartBtn);
        return ui;
    }

    private void setDifficulty(String level) {
        difficultyLevel = level;
        if (level.equals("EASY")) speed = 220;
        else if (level.equals("MEDIUM")) speed = 150;
        else speed = 100;
        updateDifficultyUI();
    }

    private void updateDifficultyUI() {
        for (javafx.scene.Node node : difficultyBox.getChildren()) {
            Label l = (Label) node;
            if (l.getText().equals(difficultyLevel)) {
                l.setStyle("-fx-background-color: #8d6e63; -fx-text-fill: #efebe9; -fx-padding: 8 15; -fx-background-radius: 5; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");
            } else {
                l.setStyle("-fx-background-color: #4e342e; -fx-text-fill: #a1887f; -fx-padding: 8 15; -fx-background-radius: 5; -fx-font-family: 'Consolas';");
            }
        }
    }

    void placeRocks() {
        rocks.clear();
        Random random = new Random();
        int count = 5; // Default for EASY
        if (difficultyLevel.equals("MEDIUM")) count = 8;
        else if (difficultyLevel.equals("HARD")) count = 12;

        for (int i = 0; i < count; i++) {
            int rx, ry;
            boolean valid;
            do {
                rx = random.nextInt(COLS);
                ry = random.nextInt(ROWS);
                valid = true;
                // Avoid snake head
                if (rx == COLS/2 && ry == ROWS/2) valid = false;
                // Avoid other rocks
                for (int[] r : rocks) if (r[0] == rx && r[1] == ry) valid = false;
            } while (!valid);
            rocks.add(new int[]{rx, ry});
        }
    }

    void cycleDifficulty() {
        // Redundant
    }
    
    void placeFood() {
        Random random = new Random();
        boolean validSpot;

        do {
            food[0] = random.nextInt(COLS);
            food[1] = random.nextInt(ROWS);

            validSpot = true;
            for (int[] segment : snake) {
                if (segment[0] == food[0] && segment[1] == food[1]) {
                    validSpot = false;
                    break;
                }
            }
            if (validSpot) {
                for (int[] r : rocks) {
                    if (r[0] == food[0] && r[1] == food[1]) {
                        validSpot = false;
                        break;
                    }
                }
            }
        } while (!validSpot);
    }

    void update() {
        if (gameOver || paused)
            return;

        dx = nextDx;
        dy = nextDy;

        int[] head = snake.get(0);
        int newX = head[0] + dx;
        int newY = head[1] + dy;
        int[] newHead = new int[] { newX, newY };

        if (newX < 0 || newX >= COLS || newY < 0 || newY >= ROWS) {
            endGame();
            return;
        }

        for (int[] segment : snake) {
            if (segment[0] == newX && segment[1] == newY) {
                endGame();
                return;
            }
        }

        for (int[] r : rocks) {
            if (r[0] == newX && r[1] == newY) {
                endGame();
                return;
            }
        }

        snake.add(0, newHead);

        if (newX == food[0] && newY == food[1]) {
            score += 10;
            if (biteSound != null && !muted) biteSound.play();
            placeFood();
        } else {
            snake.remove(snake.size() - 1);
        }

        draw();
    }

    void endGame() {
        gameOver = true;
        gameOverScoreLabel.setText("Final Score: " + score);
        gameOverUI.setVisible(true);
        draw();
    }

    void draw() {
        if (bgImage != null) {
            int imgW = (int) bgImage.getWidth();
            int imgH = (int) bgImage.getHeight();
            for (int x = 0; x < canvas.getWidth(); x += imgW) {
                for (int y = 0; y < canvas.getHeight(); y += imgH) {
                    gc.drawImage(bgImage, x, y);
                }
            }
        }

        if (!gameStarted) {
            // Background is drawn, now UI components take over
            return;
        }

        if (appleImage != null) {
            double s = TILE * 1.2;
            gc.drawImage(appleImage, food[0] * TILE - (s - TILE) / 2.0, food[1] * TILE - (s - TILE) / 2.0, s, s);
        }

        if (rockImage != null) {
            double rs = TILE * 4.0;
            for (int[] r : rocks) {
                gc.drawImage(rockImage, r[0] * TILE - (rs - TILE) / 2.0, r[1] * TILE - (rs - TILE) / 2.0, rs, rs);
            }
        }

        for (int i = snake.size() - 1; i >= 0; i--) {
            int[] seg = snake.get(i);
            double sx = seg[0] * TILE, sy = seg[1] * TILE;

            if (i == 0 && headImage != null) {
                gc.save();
                gc.translate(sx + TILE / 2.0, sy + TILE / 2.0);
                double angle = (dx == 1) ? 180 : (dx == -1) ? 0 : (dy == -1) ? 90 : 270;
                gc.rotate(angle);
                double hScale = 4.5;
                gc.drawImage(headImage, -TILE * hScale / 2.0, -TILE * hScale / 2.0 + TILE * 0.35, TILE * hScale, TILE * hScale);
                gc.restore();
            } else {
                double fade = 1.0 - (double) i / (snake.size() + 4);
                // Back to vibrant green for the snake tail
                gc.setFill(Color.web("#27ae60").interpolate(Color.web("#145a32"), 1.0 - fade));
                gc.fillRoundRect(sx + 2, sy + 2, TILE - 4, TILE - 4, 6, 6);
            }
        }

        // Score indicator
        gc.setFill(Color.rgb(62, 39, 35, 0.8)); // #3e2723 with transparency
        gc.fillRoundRect(6, 6, 130, 28, 10, 10);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 15));
        gc.setFill(Color.web("#efebe9"));
        gc.fillText("Score: " + score, 14, 25);

        if (gameOver) {
            // Drawn via gameOverUI now
        }

        if (paused && !gameOver) {
            double cx = canvas.getWidth() / 2.0;
            double cy = canvas.getHeight() / 2.0;

            gc.setFill(Color.rgb(20, 10, 5, 0.4));
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

            gc.setFill(Color.web("#5d4037"));
            gc.fillRoundRect(cx - 105, cy - 45, 210, 90, 20, 20);
            gc.setFill(Color.web("#efebe9"));
            gc.fillRoundRect(cx - 100, cy - 40, 200, 80, 18, 18);

            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 32));
            gc.setFill(Color.web("#3e2723"));
            gc.fillText("PAUSED", cx - 55, cy + 5);

            gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
            gc.setFill(Color.web("#8d6e63"));
            gc.fillText("Press ENTER to Resume", cx - 55, cy + 30);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}