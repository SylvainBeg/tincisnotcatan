package edu.brown.cs.catan;

import java.util.Collections;
import java.util.List;

import edu.brown.cs.board.Intersection;

/**
 * Represents the GameSetup. Keeps track of the Snake draft turn order and the
 * current turn. Also keeps track of the last built settlement.
 *
 */
public class Setup {

  private Intersection _lastBuiltSettlement;
  private int _currentTurn;
  private List<Integer> _setupOrder;

  public Setup(List<Integer> setupOrder) {
    _lastBuiltSettlement = null;
    _currentTurn = 0;
    _setupOrder = setupOrder;
  }

  public List<Integer> getSetupOrder() {
    return Collections.unmodifiableList(_setupOrder);
  }

  public void setLastBuiltSettlement(Intersection i) {
    _lastBuiltSettlement = i;
  }

  public void nextTurn() {
    _currentTurn++;
  }

  public Intersection getLastBuiltSettlement() {
    return _lastBuiltSettlement;
  }

  public int getCurrentPlayerID() {
    if (_currentTurn < _setupOrder.size()) {
      return _setupOrder.get(_currentTurn);
    }
    throw new UnsupportedOperationException("It is not the setup anymore");
  }

}
