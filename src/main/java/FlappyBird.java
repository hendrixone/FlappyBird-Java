package main.java;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Properties;

/**
 * FlappyBird小游戏
 * @author HendrixWan
 */
public class FlappyBird extends JPanel{

    private static final int STARTING = 0;
    private static final int RUNNING = 1;
    static final int PAUSED = 2;
    private static final int ENDING = 3;
    private static final int ENDED = 4;

    //配制文件
    static Properties properties;
    static int frameWidth;
    static int frameHeight;
    private int pipeSpawnRate;
    private int pipeInterval;

    private int ticks;

    private int gameState;


    private Bird bird;
    private BackGround backGround;
    private Base base;
    private Score score;
    private Message message;
    private GameOver gameOver;
    private LinkedList<Pipe> pipes;

    private LinkedList<GameObject> drawingQueue = new LinkedList<>();

    /**
     * @throws InterruptedException
     */
    private void action() throws InterruptedException {
        //初始化
        ticks = 0;
        gameState = STARTING;

        //创建窗口
        frameWidth = Integer.parseInt(properties.getProperty("width"));
        frameHeight = Integer.parseInt(properties.getProperty("height"));
        pipeSpawnRate = Integer.parseInt(properties.getProperty("pipe-spawn-rate"));
        pipeInterval = Integer.parseInt(properties.getProperty("pipe-interval"));

        //创建对象
        score = new Score(0);
        bird = new Bird();
        backGround = new BackGround();
        base = new Base();
        message = new Message((frameWidth - Message.image.getWidth()) / 2, (frameHeight - Message.image.getHeight()) / 2);
        gameOver = new GameOver((frameWidth - GameOver.image.getWidth()) / 2, (frameHeight - GameOver.image.getHeight()) / 2);
        pipes = new LinkedList<>();

        //创建鼠标对象
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(gameState == STARTING){
                    gameState = RUNNING;
                }
                if(gameState == RUNNING) {
                    bird.fly();
                }
                if (gameState == ENDED){
                    gameState = STARTING;
                    pipes.clear();
                    score.setScore(0);
                    bird = new Bird();
                }
            }
        };
        //监听鼠标对象
        this.addMouseListener(mouseAdapter);

        //游戏主循环
        long cycleTime;
        int cycleInterval = (1000 / Integer.parseInt(properties.getProperty("refresh-rate")));
        while (true){
            try {
                ticks++;
                long startTime = System.nanoTime();
                run();
                repaint();
                //根据每tick时间差暂停线程，保持以60tick每秒运行
                cycleTime = (System.nanoTime() - startTime) / 1000000;
                if ((cycleInterval - cycleTime) > 0) {
                    Thread.sleep(cycleInterval - cycleTime);
                }
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    private void run(){
        //开始等待阶段
        if(gameState == STARTING){
            if(ticks % 2 == 0) {
                base.act();
                backGround.act();
            }
            drawingQueue.add(backGround);
            drawingQueue.add(base);
            drawingQueue.add(message);
        }
        //游戏进行
        if(gameState == RUNNING){
            if(ticks % pipeSpawnRate == 0){
                spawnPipe();
                score.setScore(score.getScore() + 1);
            }
            bird.act();
            //判断是否撞击地面
            if(bird.getRectangle().intersects(base.getRectangle())){
                gameState = ENDING;
            }
            //枚举管道，判断是否撞击
            for (Pipe pipe : pipes){
                if(bird.getRectangle().intersects(pipe.getRectangle())){
                    gameState = ENDING;
                }
            }

            if(ticks % 2 == 0) {
                base.act();
                backGround.act();
                if(!pipes.isEmpty()) {
                    for (Pipe pipe : pipes)
                        pipe.act();
                    if (pipes.peekFirst() != null && pipes.peekFirst().isOffBound())
                        pipes.removeFirst();
                }
            }
            drawingQueue.add(backGround);
            drawingQueue.addAll(pipes);
            drawingQueue.add(base);
            drawingQueue.add(bird);
            drawingQueue.add(score);

        }
        //游戏结束中
        if(gameState == ENDING){
            drawingQueue.add(backGround);
            drawingQueue.addAll(pipes);
            drawingQueue.add(base);
            bird.act();
            drawingQueue.add(bird);
            drawingQueue.add(score);
            if(bird.getRectangle().y > FlappyBird.frameHeight){
                try {
                    Thread.sleep(500);
                    gameState = ENDED;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        //游戏已结束
        if(gameState == ENDED){
            drawingQueue.add(gameOver);
            drawingQueue.add(score);
        }
    }

    private void spawnPipe() {
        int pipeHeight = (int) ((Math.random() - 0.5) * pipeInterval);
        Pipe pipeTop = new Pipe(new Point(frameWidth, (frameHeight / 2 + pipeHeight)), 0);
        Pipe pipeBot = new Pipe(new Point(
                frameWidth,
                (frameHeight / 2 + pipeHeight) - pipeTop.getRectangle().height - pipeInterval),
                180);
        pipes.add(pipeTop);
        pipes.add(pipeBot);
    }


    @Override
    public void paint(Graphics g) {
        while(!drawingQueue.isEmpty()){
            drawingQueue.pop().draw(g);
        }
    }

    /**
     * 从根目录读取配制文件
     * @return 加载完毕的Properties类
     */
    private static Properties loadConfig(){
        Properties properties = new Properties();
        InputStream inputStream = FlappyBird.class.getClassLoader().getResourceAsStream("config");
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("配制文件加载失败");
        }
        if(!properties.isEmpty()){
            System.out.println("config load success");
        }else {
            System.out.println("config missing, load failed");
            throw new RuntimeException();
        }
        return properties;
    }

    public static void main(String[] args) throws InterruptedException {
        //初始化游戏数值
        properties = loadConfig();
        frameWidth = Integer.parseInt(FlappyBird.properties.getProperty("width"));

        //初始化游戏窗口
        JFrame frame = new JFrame();

        FlappyBird flappyBird = new FlappyBird();
        frame.add(flappyBird);

        frame.setTitle("FlappyBird");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(Integer.parseInt(properties.getProperty("width")), Integer.parseInt(properties.getProperty("height")));
        frame.setVisible(true);
        frame.setResizable(false);
        flappyBird.action();
    }
}


