package edu.brown.cs.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Random;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import edu.brown.cs.board.Tile;
import edu.brown.cs.catan.Bank;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;

public class RollDice implements Action {

  private Player _player;
  private Referee _ref;
  private Bank _bank;

  public RollDice(Referee ref, int playerID) {
    assert ref != null;
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    _bank = _ref.getBank();
    if (_player == null) {
      String err = String.format("No player exists with the id: %d", playerID);
      throw new IllegalArgumentException(err);
    }
    // assert ref.currentPlayer().equals(_player);
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    Random r = new Random();
    PrimitiveIterator.OfInt rolls = r.ints(1, 7).iterator();
    int diceRoll = rolls.nextInt() + rolls.nextInt();
    Map<Integer, Map<Resource, Integer>> playerResourceCount = new HashMap<>();
    Map<Integer, ActionResponse> toRet = new HashMap<>();

    if (diceRoll != 7) {
      Collection<Tile> tiles = _ref.getBoard().getTiles();
      // Iterate through tiles on the board
      for (Tile t : tiles) {
        // If the tile matches the roll and does not have the robber
        if (t.getRollNumber() == diceRoll && !t.hasRobber()) {
          // Find out who should collect what from the intersections
          Map<Integer, Map<Resource, Integer>> fromTile = t
              .notifyIntersections();
          // Iterate through this and consolidate collections for each person
          for (int playerID : fromTile.keySet()) {
            if (!playerResourceCount.containsKey(playerID)) {
              playerResourceCount.put(playerID,
                  new HashMap<Resource, Integer>());
            }
            Map<Resource, Integer> resourceCount = fromTile.get(playerID);
            Map<Resource, Integer> playerCount = playerResourceCount
                .get(playerID);
            for (Resource res : resourceCount.keySet()) {
              if (playerCount.containsKey(res)) {
                // Update the count
                playerCount.replace(res,
                    playerCount.get(res) + resourceCount.get(res));
              } else {
                playerCount.put(res, resourceCount.get(res));
              }
              // Make sure the player collects the resource
              _ref.getPlayerByID(playerID).addResource(res,
                  resourceCount.get(res));
              // Update Bank stats
              _bank.getResource(res, resourceCount.get(res));
            }
          }
        }
      }
      for (Integer playerID : playerResourceCount.keySet()) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("%d was rolled", diceRoll));
        Map<Resource, Integer> resourceCount = playerResourceCount
            .get(playerID);
        for (Resource res : resourceCount.keySet()) {
          switch (res) {
          case WHEAT:
            message.append(String.format(", you received %d wheat",
                resourceCount.get(res)));
            break;
          case SHEEP:
            message.append(String.format(", you received %d sheep",
                resourceCount.get(res)));
            break;
          case ORE:
            message.append(String.format(", you received %d ore",
                resourceCount.get(res)));
            break;
          case BRICK:
            message.append(String.format(", you received %d brick",
                resourceCount.get(res)));
            break;
          case WOOD:
            message.append(String.format(", you received %d wood",
                resourceCount.get(res)));
            break;
          default:
            message.append(".");
            break;
          }
        }
        message.append(".");
        ActionResponse toAdd = new ActionResponse(true, message.toString(),
            resourceCount);
        toRet.put(playerID, toAdd);
      }
      for (Player p : _ref.getPlayers()) {
        if (!toRet.containsKey(p.getID())) {
          ActionResponse toAdd = new ActionResponse(true, String.format(
              "%d was rolled.", diceRoll), new HashMap<Resource, Integer>());
          toRet.put(p.getID(), toAdd);
        }
      }
    } else {
      // 7 is rolled:
      List<Integer> playersToDrop = new ArrayList<>();
      Map<Integer, String> followUp = new HashMap<>();
      boolean mustDiscard = false;
      Map<Integer, JsonObject> jsonToSend = new HashMap<>();
      String message = "7 was rolled.";
      for (Player p : _ref.getPlayers()) {
        if (p.getNumResourceCards() > Settings.DROP_CARDS_THRESH) {
          mustDiscard = true;
          double numToDrop = p.getNumResourceCards() / 2.0;
          if (!_ref.getGameSettings().isDecimal) {
            numToDrop = Math.floor(numToDrop);
          }
          playersToDrop.add(p.getID());
          message += String.format(" %s must discard cards", p.getName());
          JsonObject jsonForPlayer = new JsonObject();
          _ref.playerMustDiscard(_player.getID(), numToDrop);
          jsonForPlayer.addProperty("numToDrop", numToDrop);
          jsonToSend.put(p.getID(), jsonForPlayer);
        }
      }
      if (mustDiscard) {
        message += ".";
        for (Player p : _ref.getPlayers()) {
          if (playersToDrop.contains(p.getID())) {
            followUp.put(p.getID(), DropCards.ID);
            toRet.put(p.getID(),
                new ActionResponse(true,
                    "7 was rolled. You must drop half of your cards.",
                    jsonToSend.get(p.getID())));
          } else {
            toRet.put(p.getID(), new ActionResponse(true, message, null));
          }
        }
        _ref.addFollowUp(followUp);
      } else {
        ActionResponse respToAll = new ActionResponse(true,
            "7 was rolled. No one has more than 7 cards.", null);
        ActionResponse respToPlayer = new ActionResponse(true,
            "7 was rolled. You get to move the Robber.", null);
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), respToPlayer);
          } else {
            toRet.put(p.getID(), respToAll);
          }
        }
      }
      _ref.addFollowUp(ImmutableMap.of(_player.getID(), MoveRobber.ID));
    }
    return toRet;
  }
}
