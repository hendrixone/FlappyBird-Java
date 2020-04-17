package main.java;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

/**
 * @author HendrixWan
 */
public abstract class GameObject {
    private int rotation = 0;
    private BufferedImage image;
    private Rectangle rectangle;

    Rectangle getRectangle() {
        return rectangle;
    }

    void setRectangle(Rectangle rectangle) {
        this.rectangle = rectangle;
    }

    BufferedImage getImage(){
        return image;
    }

    void setImage(BufferedImage image){
        this.image = image;
    }

    int getRotation() {
        return rotation;
    }

    void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public void draw(Graphics g) {
        if (getRotation() == 0) {
            g.drawImage(getImage(), getRectangle().x, getRectangle().y, null);
        } else {
            Graphics2D g2d = (Graphics2D) g;
            g2d.rotate(Math.toRadians(getRotation()), getRectangle().getCenterX(), getRectangle().getCenterY());
            g.drawImage(getImage(), getRectangle().x, getRectangle().y, null);
            g2d.rotate(-Math.toRadians(getRotation()), getRectangle().getCenterX(), getRectangle().getCenterY());
        }
    }

    static BufferedImage loadImage(String name){
        File file = new File("assets/sprites/" + name);
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            System.out.println("图片加载失败，路径："+file.getAbsolutePath());
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}

/**
 * 游戏中的背景对象
 */
class BackGround extends GameObject{
    int speed;

    Rectangle rect2;

    BackGround(){
        speed = Integer.parseInt(FlappyBird.properties.getProperty("background-speed"));
        String time = FlappyBird.properties.getProperty("time");
        this.setImage(loadImage("background-"+time+".png"));
        Rectangle rectangle = new Rectangle(0, 0, getImage().getWidth(), getImage().getHeight());
        rect2 = new Rectangle(FlappyBird.frameWidth, getImage().getWidth(), getImage().getWidth(), getImage().getHeight());
        setRectangle(rectangle);
    }

    void act(){
        getRectangle().translate(-speed, 0);
        rect2.translate(-speed, 0);
        if(getRectangle().x < -getRectangle().width){
            getRectangle().x = (int) rect2.getMaxX();
        }
        if(rect2.x < -getRectangle().width){
            rect2.x = (int) getRectangle().getMaxX();
        }
    }

    @Override
    public void draw(Graphics g){
        super.draw(g);
        g.drawImage(getImage(), rect2.x, getRectangle().y, null);
    }
}

class Base extends BackGround{
    private static int baseHeight = Integer.parseInt(FlappyBird.properties.getProperty("base-height"));

    Base(){
        speed = Integer.parseInt(FlappyBird.properties.getProperty("base-speed"));
        setImage(loadImage("base.png"));
        Rectangle rectangle = new Rectangle(0, baseHeight, getImage().getWidth(), getImage().getHeight());
        rect2 = new Rectangle(getImage().getWidth() + 0, baseHeight, getImage().getWidth(), getImage().getHeight());
        setRectangle(rectangle);
    }
}

class Pipe extends GameObject{
    private static int speed = Integer.parseInt(FlappyBird.properties.getProperty("base-speed"));
    private static String pipeColor = FlappyBird.properties.getProperty("pipe-color");
    private static BufferedImage image = loadImage("pipe-" + pipeColor + ".png");

    public Pipe(Point startPos, int rotation){
        setRotation(rotation);
        setImage(image);
        Rectangle rectangle = new Rectangle();
        rectangle.setLocation(startPos);
        rectangle.setSize(getImage().getWidth(), getImage().getHeight());
        setRectangle(rectangle);
    }

    void act(){
        getRectangle().translate(-speed, 0);
    }

    boolean isOffBound(){
        return getRectangle().x < -getRectangle().width;
    }

}
/**
 * 游戏中的小鸟对象
 */
class Bird extends GameObject{
    private static LinkedList<BufferedImage> images;
    private int index = 0;
    private int tick;
    private double speed = 0;
    private int desAngle;
    private static int animation_interval = Integer.parseInt(FlappyBird.properties.getProperty("bird-animation-interval"));

    private double gravity = Double.parseDouble(FlappyBird.properties.getProperty("gravity"));
    private double speedBuffer = Double.parseDouble(FlappyBird.properties.getProperty("speed-buffer"));
    private double birdFlyHeight = Integer.parseInt(FlappyBird.properties.getProperty("bird-fly-height"));
    static {
        images = new LinkedList<>();
        String birdColor = FlappyBird.properties.getProperty("bird-color");
        images.add(loadImage(birdColor + "bird-upflap.png"));
        images.add(loadImage(birdColor + "bird-midflap.png"));
        images.add(loadImage(birdColor + "bird-downflap.png"));
    }

    Bird(){
        this.setRotation(0);
        this.setImage(images.peek());
        Rectangle rectangle = null;
        if (images.peek() != null) {
            rectangle = new Rectangle(30, 200, images.peek().getWidth(), images.peek().getHeight());
        }
        this.setRectangle(rectangle);
    }

    private void animate(){
        if(tick % animation_interval == 0) {
            this.setImage(images.get(index));
            index++;
            if (index == images.size()) {
                index = 0;
            }
        }
    }

    void fly(){
        speed = -birdFlyHeight;
    }

    void act(){
        //大于这个速度时，小鸟会面朝斜上方
        int birdFaceUpSpeed = 5;
        animate();
        tick++;
        speed += (gravity - speed * speedBuffer);
        this.getRectangle().translate(0, (int) speed);
        if(speed <= birdFaceUpSpeed){
            desAngle = -15;
        }else {
            desAngle = 90;
        }
        if(getRotation() > desAngle){
            setRotation(getRotation() - 10);
        }else if (getRotation() < desAngle){
            setRotation(getRotation() + 5);
        }
    }

}

class Message extends GameObject{
    static BufferedImage image = loadImage("message.png");

    Message(int x, int y){
        setImage(image);
        Rectangle rectangle = new Rectangle(x ,y, image.getWidth(), image.getHeight());
        setRectangle(rectangle);
    }

    @Override
    public void draw(Graphics g){
        super.draw(g);
    }
}

class GameOver extends GameObject{
    static BufferedImage image = loadImage("gameover.png");

    GameOver(int x, int y){
        setImage(image);
        Rectangle rectangle = new Rectangle(x ,y, image.getWidth(), image.getHeight());
        setRectangle(rectangle);
    }

    @Override
    public void draw(Graphics g){
        super.draw(g);
    }
}

class Score extends GameObject{
    private static BufferedImage[] images;

    private int score;

    private Rectangle rect1;
    private Rectangle rect2;
    private Rectangle rect3;
    private int score1;
    private int score2;
    private int score3;

    Score(int score){
        images = new BufferedImage[10];
        for (int i = 0; i < 10; i++) {
            images[i] = loadImage(i + ".png");
        }

        this.score = score;
        int x = FlappyBird.frameWidth - 100;
        int y = 25;
        int width = images[0].getWidth();
        int height = images[0].getHeight();
        rect1 = new Rectangle(x, y, width, height);
        rect2 = new Rectangle(x + width, y, width, height);
        rect3 = new Rectangle(x + width * 2, y, width, height);
    }

    void setScore(int score){
        this.score =score;
        score3 = score % 10;
        score2 = (score / 10) % 10;
        score1 = (score / 100) % 10;
    }

    int getScore(){
        return score;
    }

    @Override
    public void draw(Graphics g){
        g.drawImage(images[score1], rect1.x, rect1.y, null);
        g.drawImage(images[score2], rect2.x, rect2.y, null);
        g.drawImage(images[score3], rect3.x, rect3.y, null);
    }
}


