package client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import global.Constants;
import global.Position;
import server.Entity;
import server.Player;
import server.Wall;
import transferable.*;

import javax.swing.*;

public class Client extends JFrame implements ActionListener, KeyListener {
    private String providedAddress;
    private String userName;
    private int updatePort = Constants.UPDATE_PORT;
    private int volitionPort = Constants.VOLITION_PORT;
    private Socket volitionSocket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private ClientInformation me;
    private Update latest;
    private Thread upd;
    private Volition currentVolition;
    ArenaDisplay myDisp;
    QueueDisplay myQueueDisp;
    private Entity[][] board = new Entity[Constants.BOARD_VIEW_WINDOW_SIZE][Constants.BOARD_VIEW_WINDOW_SIZE];


    Image scaledBulletIcon = new ImageIcon("bullet.png").getImage().getScaledInstance(40, 40, Image.SCALE_FAST);
    Image scaledPlayerIcon = new ImageIcon("client.png").getImage().getScaledInstance(40, 40, Image.SCALE_FAST);
    Image scaledEnemyIcon = new ImageIcon("enemy.png").getImage().getScaledInstance(40, 40, Image.SCALE_FAST);

    public static void main(String args[]) throws IOException, InterruptedException {
        Client myClient = new Client();
        myClient.queue();

    }

    public Client() throws IOException {
        super("Java Battle Arena");
        while(userName == null || userName.isEmpty() || userName == "")
            userName = JOptionPane.showInputDialog(null, "Enter a Username:", "Input", JOptionPane.INFORMATION_MESSAGE);
        while(providedAddress == null || providedAddress.isEmpty() || providedAddress == "")
            providedAddress = (String) JOptionPane.showInputDialog(null, "Enter the Server's IP:", "Input", JOptionPane.QUESTION_MESSAGE, null, null, "127.0.0.1");
        guiInit();
        currentVolition = new Volition(false,false);
        me = new ClientInformation(userName, new Position(0,0), false);
        upd = new Thread(new ServerListener(this, new Socket(providedAddress, updatePort), me));
        upd.start();
        this.volitionSocket = new Socket(providedAddress, volitionPort);
        this.outputStream = new ObjectOutputStream(volitionSocket.getOutputStream());
        this.outputStream.flush(); //Necessary to avoid 'chicken or egg' situation
        this.inputStream = new ObjectInputStream(volitionSocket.getInputStream());
    }

    public void guiInit(){

        myDisp = new ArenaDisplay();

        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setSize(new Dimension(700,700));
        this.setBackground(Color.WHITE);
        this.setLocationRelativeTo(null);
        this.setResizable(false);
        this.setVisible(true);

    }

    private void updateVolition() throws IOException {
        //Use to update server of a new Volition
        outputStream.writeObject(currentVolition);
    }

    private void queue() throws InterruptedException {
        while(latest == null){
            renderQueue();
            TimeUnit.SECONDS.sleep(1);
        }
    }

    public void getServerUpdate(Update u){
        //Called by ServerListener, not intended for other use.
        this.latest = u;
        this.repaint();
        this.revalidate();
    }

    public void loseConnection() throws InterruptedException {
        //Called by ServerListener, not intended for other use.
        TimeUnit.SECONDS.sleep(5);
        System.exit(-1);
    }

    public void renderQueue(){
        myQueueDisp = new QueueDisplay();
        this.add(myQueueDisp);
        this.pack();
    }

    public void renderBoard(){
        ArrayList<Entity> allEntities = latest.getEntities();
        Player me = latest.getPlayer();
        Position myPos = me.getPosition();
        System.out.println("Position: " + myPos.x + "," + myPos.y);
        Position topLeft = new Position(myPos.x - 7, myPos.y - 7);
        Position botRight = new Position(myPos.x + 7, myPos.y + 7);



        for(Entity cur: allEntities){
            //System.out.println("OUTSIDE: " + cur.toString());
            if(cur.getPosition().x >= topLeft.x && cur.getPosition().x <= botRight.x){
                if(cur.getPosition().y >= topLeft.y && cur.getPosition().y <= botRight.y){
                    board[cur.getPosition().x - topLeft.x][cur.getPosition().y - topLeft.y] = cur;
                    System.out.println("INSIDE: " + cur.toString() + (cur.getPosition().x - topLeft.x) + "   " + (cur.getPosition().y - topLeft.y));
                }
            }
        }




        //Loops through every space that I will render
        int realx = 0;
        int realy = 0;
        for(int y = topLeft.y; y < botRight.y; y++){
            for(int x = topLeft.x; x < botRight.x; x++){
                System.out.print("      [x: " + x + "  y: " + y + "]");
                if(x < 0 || y < 0){
                    board[realx][realy] = new Wall();
                    System.out.println("adding wall at " + x + " " + y + "    really " + realx + " " + realy);
                }
                realx++;
            }
            realy++;
        }
        System.out.println();
    }


    public void actionPerformed(ActionEvent event){

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    public void keyPressed(KeyEvent event) { //https://docs.oracle.com/javase/tutorial/uiswing/events/keylistener.html

        if (event.getKeyCode() == KeyEvent.VK_UP) {
            modVolitionDirectional(Constants.FACING_NORTH);
        } else if (event.getKeyCode() == KeyEvent.VK_DOWN) {
            modVolitionDirectional(Constants.FACING_SOUTH);
        } else if (event.getKeyCode() == KeyEvent.VK_LEFT) {
            modVolitionDirectional(Constants.FACING_WEST);
        } else if (event.getKeyCode() == KeyEvent.VK_RIGHT) {
            modVolitionDirectional(Constants.FACING_EAST);
        } else if(event.getKeyCode() == KeyEvent.VK_SPACE){
            currentVolition.setFacingVolition(false);
            currentVolition.setMovementVolition(false);
            currentVolition.setShootingVolition(true);
        }
        try {
            updateVolition();
        } catch (IOException e) {
            System.err.print("Volition not sent properly");
            e.printStackTrace();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    public void modVolitionDirectional(int facing){
        currentVolition.setFacingVolition(facing);
        if (latest.getPlayer().facing() == facing){
            currentVolition.setMovementVolition(true);
            currentVolition.setFacingVolition(false);
            currentVolition.setShootingVolition(false);
        } else {
            currentVolition.setMovementVolition(false);
            currentVolition.setFacingVolition(true);
            currentVolition.setShootingVolition(false);
        }
    }


    /**
     * In-Line Classes for JPanel elements intended for rendering
     */
    class QueueDisplay extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setFont(new Font("TimesRoman", Font.PLAIN, 30));
            g.drawString("Waiting for Server...",215,350);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(700, 700);
        }
    }

    class ArenaDisplay extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            renderBoard();
            //g.setFont(new Font("Helvetica", Font.PLAIN, 30));
            //g.drawString("Got an Update!",215,350);

            int yOffset = 0;
            for(Entity[] row : board){//Each Row
                int xOffset = 0;
                for(Entity space : row){ //Each Square
                    System.out.print("OFFSETS: X " + xOffset + "  Y " + yOffset + "      :");
                    if(space == null){
                        System.out.println("empty");
                        g.setColor(Color.lightGray);
                        g.fillRect(xOffset, yOffset, Constants.SQUARE_DIM, Constants.SQUARE_DIM);
                        xOffset += Constants.SQUARE_DIM;
                        continue;
                    }
                    if(space.isWall) {
                        System.out.println("wall");
                        g.setColor(Color.DARK_GRAY);
                        g.fillRect(xOffset, yOffset, Constants.SQUARE_DIM, Constants.SQUARE_DIM);
                    }
                    if(space.equals(latest.getPlayer())){
                        System.out.println("me");
                        g.drawImage(scaledPlayerIcon, xOffset, yOffset, this);
                    } else if(space.isPlayer){
                        System.out.println("enemy");
                        g.drawImage(scaledEnemyIcon, xOffset, yOffset, this);
                    } else {
                        System.out.println("bullet");
                        g.drawImage(scaledBulletIcon, xOffset, yOffset, this);
                    }
                    xOffset += Constants.SQUARE_DIM;
                }
                yOffset += Constants.SQUARE_DIM;
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(700, 700);
        }
    }
}
