package Server;
import Global.*;
import Transferable.Update;

import java.io.Serializable;
import java.util.ArrayList;

public class Arena implements EntityActionListener, Serializable{
    public String title = "Java Battle Arena";
    protected ArrayList<Entity> entities = new ArrayList<>(); //Maintained to inform client renders
    protected ArrayList<Player> players = new ArrayList<>(); //Maintained to inform scoreboard
    protected ArrayList<Bullet> bullets = new ArrayList<>(); //Maintained to inform scoreboard
    Update sendToClients;
    public int totalPlayers = 0;
    protected int livingPlayers = 0;
    static int id = 0;


    public Arena(String title) {
        this.title = title;
    }

    public void add(Entity entity, Position position) { //Adds something new to the map, use to initialize locations of players or to add bullets
        entity.addActionListener(this);
        entities.add(entity);
        entity.setPosition(position);
        if(entity.isPlayer) {
            players.add((Player)entity);
            totalPlayers++;
            livingPlayers++;
        }
    }

    public void cycle() {
        //TODO, give cycles a fixed time interval using system time
        for(Entity e : entities){
            e.fulfillVolition();
        }
        validateEntities();
        refreshSendables();
        //TODO serialize and send the Update called sendToClients. **EntitySender is now an Update.
        //TODO --> It only has entities since that contains Players and Bullets. Reduces Serialization and network lag.**
    }

    public void refreshSendables(){
        sendToClients = new Update(entities);
    }

    public int getId(){
        id++;
        return id;

    }

    public void validateEntities(){ //To be called after all entities have fulfilled volition
        //Evaluates all damage
        ArrayList<Player> playersToTakeDamage = new ArrayList<>();
        ArrayList<Bullet> bulletsToTakeDamage = new ArrayList<>();
        for(Bullet b : bullets){
            boolean gotAHit = false;
            for(Player p : players){
                if(b.getPosition().equals(p.getPosition())){
                    gotAHit = true;
                    playersToTakeDamage.add(p);
                }
            }
            if(gotAHit){
                bulletsToTakeDamage.add(b);
            }
        }

        for(Player toHurt : playersToTakeDamage){
            toHurt.takeDamage();
        }
        for(Bullet toHurt: bulletsToTakeDamage){
            toHurt.takeDamage();
        }

        //Damage has been evaluated, let's resolve multiple entities in the same position
        while(!playersAreValidated()) {
            ArrayList<Position> playerPositions = new ArrayList<>();
            for (Player p : players) {
                if (!playerPositions.contains(p.getPosition())) {
                    playerPositions.add(p.getPosition());
                }
            }
            for(Position pos : playerPositions){
                int pcount = 0;
                ArrayList <Player> inCurSquare = new ArrayList<Player>();
                for(Player p : players){
                    if(p.getPosition().equals(pos)){
                        inCurSquare.add(p);
                        pcount++;
                    }
                }
                if(pcount > 1) { //This checks if there was more than one entity in that position
                    for(Player p : inCurSquare){  //Bounce the players
                        p.aboutFace();
                        p.move();
                        p.aboutFace();
                    }
                }
            }
        }
    }

    public boolean playersAreValidated(){
        ArrayList<Position> playerPositions = new ArrayList<>();
        for(Player p : players){
            if(!playerPositions.contains(p.getPosition())){
                playerPositions.add(p.getPosition());
            }
        }
        for(Position pos : playerPositions){
            int pcount = 0;
            for(Player p : players){
                if(p.getPosition().equals(pos)){
                    pcount++;
                }
            }
            if(pcount > 1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void die(Entity toMourn) {
        entities.remove(toMourn);
        if(toMourn.isPlayer) players.remove(toMourn);
        if(!toMourn.isPlayer) bullets.remove(toMourn);
        //TODO: Update the ScoreBoard with the player's final statistics, and death
    }

    @Override
    public void move(Entity toMove, Position newPosition) {
        //TODO: Determine if this listener is necessary
    }
}
