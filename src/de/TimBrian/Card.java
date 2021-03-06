package de.TimBrian;

import GUI.Texture;
import handChecker.PokerCard;

import java.io.Serializable;

class Card implements PokerCard, Serializable {
    private final Value value;
    private final Color color;
    private Texture texture;


    public Card(Value value, Color color, Boolean ClientSide) {
        this.value = value;
        this.color = color;

        //TODO INDICATE REMOTE
        if (ClientSide)
            texture = new Texture(value.name().toUpperCase() + "_" + color.name().toUpperCase() + ".png");
    }

    public Texture getTexture() {
        if (texture != null)
            return texture;
        else
            return null;
    }

    public Value getValue() {
        return value;
    }

    public Color getColor() {
        return color;
    }

    public String toString() {
        return value.toString().toLowerCase() + " of " + color.toString().toLowerCase();
    }
}

