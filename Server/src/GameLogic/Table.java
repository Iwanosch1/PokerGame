package GameLogic;


import GameLogic.enums.Role;
import Network.VarObserver;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Table {
    private final Stack cardStack = new Stack();
    private final List<Player> players = new LinkedList<>();
    private final Stack openCards = new Stack();

    HashSet<VarObserver> observers = new HashSet<>();

    protected int maxBet = 0;
    private int blind = 100;
    private int roundCounter = 0;
    private int turnCounter = 0;
    private int dealerPos;

    public void broadcastToPlayers(String message) {
       for (Player out : players) {
           out.handlerServer.sendData("MESSAGE", message);
       }
    }

    public boolean allPlayersFinished() {
        return players.stream().anyMatch(player -> player.getName().equals("ERROR"));
    }

    public int playerAmount() {
        return players.size();
    }

    public Stack getCardStack() {
        return cardStack;
    }

    public int getRoundCounter() {

        return roundCounter;
    }

    public Stack getOpenCards() {
        return openCards;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void addPlayer(Player p) {

        players.add(p);

        VarObserver obs = new VarObserver(this);
        p.addObserver(obs);
        observers.add(obs);
    }

    private void removePlayer(Player p) {
        if (p.getCurrentRole() == Role.DEALER)
            dealerPos--;

        players.remove(p);
    }

    private List<Player> decideWinner() {
        List<Player> comparePlayers = players.stream().filter(Player::isInRound).collect(Collectors.toList());
        int pos = 0;
        while (comparePlayers.size() > 1) {
            if (pos == comparePlayers.size() - 1)
                break;
            else {
                switch (comparePlayers.get(pos).getHandValue(openCards.getCards()).compareTo(comparePlayers.get(pos + 1).getHandValue(openCards.getCards()))) {
                    case 1:
                        comparePlayers.remove(pos + 1);
                        break;
                    case -1:
                        comparePlayers.remove(pos);
                        break;
                    case 0:
                        pos++;
                        break;
                }
            }
        }
        return comparePlayers;
    }

    //TODO send message to clients who won
    private void distributePot(List<Player> winners) {
        if (winners.size() == 1) {
            //Allin
            if (winners.get(0).getChips() == 0) {
                for (Player p : players) {
                    winners.get(0).addChips(p.subtractPlayerPot(winners.get(0).getPlayerPot()));
                }
                winners.get(0).leaveRound();
                distributePot(decideWinner());
            } else //no Allin
            {
                for (Player p : players) {
                    winners.get(0).addChips(p.subtractPlayerPot(winners.get(0).getPlayerPot()));
                }
            }
        } else {
            if (winners.get(0).getChips() == 0) {
                Player minPlayer = winners.stream().min((p1, p2) -> ((Integer) p1.getChips()).compareTo(p2.getChips())).orElseThrow(RuntimeException::new);

                for (Player p : players) {
                    minPlayer.addChips(p.subtractPlayerPot(winners.get(0).getPlayerPot()) / winners.size());
                }
                minPlayer.leaveRound();
                distributePot(decideWinner());
            } else {
                for (Player w : winners) {
                    for (Player p : players) {
                        w.addChips(p.subtractPlayerPot(winners.get(0).getPlayerPot()) / winners.size());
                    }
                }
            }
        }
    }

    /**
     * main game tick method; calls assignRole and distributeCards
     */
    public void nextTurn() {

        //SERVER
        turnMessage();
        //

        distributeCards();

        //SERVER
        handCardMessage();
        openCardMessage();
        //
        if (turnCounter == 0) {
            assignRole();
            //SERVER
            roleMessage();
            //
            preFlop();
        } else if (turnCounter == 3 || countActivePlayers() == 1) {
            distributePot(decideWinner());
            nextRound();
            return;
        } else {
            playerBet();
        }
        turnCounter++;

        System.out.println("-----------------------------------------------------------");
    }

    private void nextRound() {
        openCards.clearCards();
        cardStack.fillStack();

        players.forEach(Player::clearHand);
        players.stream().filter(player -> player.getChips() == 0).forEach(this::removePlayer);

        maxBet = 0;
        turnCounter = 0;
        roundCounter++;

    }

    /**
     * special preflop bet
     */
    private void preFlop() {

        int firstDefault = dealerPos;

        //int blind = 100;
        if (players.size() > 2) {
            players.get((dealerPos + 1) % players.size()).placeBet(blind / 2, this);
            players.get((dealerPos + 2) % players.size()).placeBet(blind, this);

            firstDefault = (dealerPos + 3) % players.size();

        } //only 2 players
        else {
            players.get(dealerPos).placeBet(blind / 2, this);
            players.get((dealerPos + 1) % players.size()).placeBet(blind, this);
        }

        for (Player p : players) {
            betMessage(p.getName(), p.getPlayerPot());
        }

        for (int j = firstDefault, i = 0; i < players.size(); i++, j = (j + 1) % players.size()) {
            while (players.get(j).isInRound()) {
                int bet = getUserInput(players.get(j));
                if (players.get(j).placeBet(bet, this)) {

                    //SERVER
                    for (Player p : players) {
                        betMessage(p.getName(), p.getPlayerPot());
                    }

                    break;
                }
            }
        }

        if (fixBets())
            playerBet();
    }

    private int getUserInput(Player p) {

        p.handlerServer.sendData("BETSET" ,"Bitte gib deinen Bet ein. (" + maxBet + ")\n");
        while (true) {
            if (p.handlerServer.betGiven != null) {
                int out = Integer.parseInt((String) p.handlerServer.betGiven);
                p.handlerServer.betGiven = null;
                return out;
            }
            else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void playerBet() {
        int posSmall = dealerPos;
        if (players.size() > 2)
            posSmall = (dealerPos + 1) % players.size();
        /*
        j = current playing player
        i = sum of the players already played
        countActivePlayers() > 1 = make sure the last player in a roundCounter doesn't fold
         */
        for (int j = posSmall, i = 0; i < players.size() && countActivePlayers() > 1; i++, j = (j + 1) % players.size()) {
            while (players.get(j).isInRound()) {
                int bet = getUserInput(players.get(j));
                if (players.get(j).placeBet(bet, this)) {
                    break;
                }
            }
        }

        //SERVER
        for (Player p : players) {
            betMessage(p.getName(), p.getPlayerPot());
        }

        if (fixBets())
            playerBet();
    }

    private boolean fixBets() {
        for (Player p : players) {
            if (p.isInRound() && (p.getPlayerPot() != maxBet ^ p.getChips() == 0))
                return true;
        }
        return false;
    }

    private int countActivePlayers() {
        int sum = 0;
        for (Player p : players) {
            if (p.isInRound())
                sum++;
        }
        return sum;
    }


    /**
     * used for giving players and table cards according to game situation
     */
    private void distributeCards() {
        switch (turnCounter) {
            case 0:
                //zwei Karten an alle aktiven Spieler
                players.stream().filter(Player::isInRound).forEach(player -> {
                    player.addCardToHand(cardStack.remove());
                    player.addCardToHand(cardStack.remove());
                });
                break;

            case 1:
                //drei offene Karten
                for (int i = 0; i < 3; i++) {
                    openCards.add(cardStack.remove());
                }
                break;

            default:
                openCards.add(cardStack.remove());
                break;
        }
    }

    /**
     * assigns roles to every player
     */
    private void assignRole() {
        if (roundCounter == 0)
            //Zufällige Postion des Dealers in der ersten Runde
            dealerPos = new Random(System.currentTimeMillis()).nextInt(players.size());
        else {
            //Postion um eins nach links in den darauf folgeneden Runden
            dealerPos++;
        }
        //special case initialization
        if (players.size() == 2) {
            players.get(dealerPos).setCurrentRole(Role.DEALERSPECIAL);
            players.get((dealerPos + 1) % players.size()).setCurrentRole(Role.BIG);
        } else {
            players.get(dealerPos).setCurrentRole(Role.DEALER);
            players.get((dealerPos + 1) % players.size()).setCurrentRole(Role.SMALL);
            players.get((dealerPos + 2) % players.size()).setCurrentRole(Role.BIG);
        }
    }

    //SERVER

    private void sendToHandler(Player p, String header, Object payload){
        p.handlerServer.sendData(header, payload);
    }

    private void turnMessage() {
        Map<String, Integer> playerChips = new HashMap<>();
        for(Player p: players){
            playerChips.put(p.getName(), p.getChips());
        }
        for (Player p : players) {
            sendToHandler(p, "PLAYERCHIPS", playerChips);
        }

        int pot = 0;
        for (Player p : players) {
            pot += p.getPlayerPot();
        }
        for (Player p : players) {
            sendToHandler(p, "POT", pot);
        }
    }

    private void roleMessage() {
        Map<String, Integer> roles = new HashMap<>();
        for(Player p: players){
            roles.put(p.getName(), p.getCurrentRole().ordinal());
        }

        for (Player p : players) {
            sendToHandler(p, "ROLE", roles);
        }
    }

    private void inRoundMessage() {
        Map<String, Boolean> inRound = new HashMap<>();

        for(Player p: players){
            inRound.put(p.getName(), p.isInRound());
        }

        for (Player player : players) {
            sendToHandler(player, "INROUND" , inRound);
        }
    }

    private void handCardMessage(){
        if (turnCounter == 0) {
            for (Player p : players) {
                sendToHandler(p, "HANDCARDS", p.getHand());
            }
        }
    }

    private void openCardMessage() {
        if (turnCounter > 0) {
            for (Player p : players) {
                sendToHandler(p, "OPENCARDS", getOpenCards());
            }
        }
    }

    private void betMessage(String name, int bet) {
        inRoundMessage();
        Map<String, Integer> playerBet = new HashMap<>();
        playerBet.put(name, bet);
        for (Player p: players) {
            sendToHandler(p, "BET", playerBet);
        }
    }
}
