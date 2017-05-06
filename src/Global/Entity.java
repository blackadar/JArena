package Global;

import Server.Arena;

import java.util.ArrayList;

public abstract class Entity {
    public Position currentPosition;
    public Arena container;
    public int facing;
    public int health;
    public boolean alive = true;
    public boolean hasVolition = false;
    public boolean isPlayer;

    protected ArrayList<EntityActionListener> listeners = new ArrayList<>();


    public Position getPosition() {
        return currentPosition;
    }

    public void addActionListener(EntityActionListener x){
        listeners.add(x);
    }

    public void takeDamage() {
        health--;
        if(health < 1) die();
    }

    public int facing() {
        return facing;
    }

    public void changeDirection(int facingDirection){
        facing = facingDirection;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public void addVolition(){
        this.hasVolition = true;
    }

    public boolean fulfillVolition(){
        if(hasVolition){
            this.move();
            hasVolition = false;
            return true;
        }
        else{
            return false;
        }
    }

    public void move() {
        switch(facing){
            case(Constants.FACING_NORTH) :
                if(currentPosition.y + 1 < container.length)
                    currentPosition.y++;
                break;
            case(Constants.FACING_SOUTH) :
                if(currentPosition.y - 1 >= 0)
                    currentPosition.y--;
                break;
            case(Constants.FACING_EAST) :
                if(currentPosition.x + 1 < container.width)
                    currentPosition.x++;
                break;
            case(Constants.FACING_WEST) :
                if(currentPosition.x - 1 >= 0)
                    currentPosition.x--;
                break;
        }
        for(EntityActionListener x : listeners) x.move(this, currentPosition);
    }

    protected void die(){
        alive = false;
        for(EntityActionListener x : listeners) x.die(this);
    }
}