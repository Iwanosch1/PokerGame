package de.TimBrian;

import de.TimBrian.enums.Role;
import handChecker.HandChecker;
import handChecker.HandValue;
import handChecker.PokerCard;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Player {
    public final Stack hand = new Stack();
    private final String name;
    private HandValue hv;
    private int chips = 10000;
    private Role currentRole = Role.DEFAULT;
    private int playerPot = 0;
    private boolean inRound = true;

    public Player(int chips, String name){
        this.chips = chips;
        this.name = name;
    }

    public Player(int number) {
        name = "Felix " + number;
    }

    public int getChips() {
        return chips;
    }

    public void addChips(int amount) {
        chips += amount;
    }

    public int getPlayerPot() {
        return playerPot;
    }

    public int subtractPlayerPot(int amount) {
        if (playerPot >= amount) {
            playerPot -= amount;
            return amount;
        }
        else {
            int out = playerPot;
            playerPot = 0;
            return out;
        }
    }

    /**
     * function evaluates given amount
     * @param amount amount the player wants to increment playerPot
     * @param table current Table player is playing on
     * @return whether or not amount was correct
     */
    public boolean placeBet(int amount, Table table) {
        //fold
        if (amount < 0) {
            inRound = false;
            return true;
        }
        //enough chips and raise/call/check
        if (amount <= chips) {
            if (table.maxBet <= amount + playerPot) {
                playerPot += amount;
                chips -= amount;
                table.maxBet = playerPot;
            } //Allin
            else if (chips == amount) {
                playerPot += amount;
                chips -= amount;
            }
            else
                return false;
            return true;
        } else
            return false;
    }

    public Role getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(Role r) {
        currentRole = r;
    }

    private List<PokerCard> getCompleteHand(List<PokerCard> openCards) {
        return Stream.concat(hand.getCards().stream(), openCards.stream()).collect(Collectors.toList());
    }

    public HandValue getHandValue(List<PokerCard> openCards) {
        if (hv == null) {
            HandChecker handChecker = new HandChecker();
            hv = handChecker.check(getCompleteHand(openCards));
        }
        return hv;

    }

    public String toString() {
        return name + ":\n" + hand.toString() + "\tchips:\t\t" + chips + "\n\tplayerpot:\t" + playerPot + "\n\tRolle:\t" + currentRole.toString();
    }

    public boolean isInRound() {
        return inRound;
    }

    public void leaveRound() {
        inRound = false;
    }
}
