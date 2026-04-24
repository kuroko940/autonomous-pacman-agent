package logic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

import java.util.Collections;

import view.Gomme;


/**
 * class used to represent plan. It will provide for a given set of results an action to perform in each result
 */
class Plans{
	ArrayList<Result> results;
	ArrayList<ArrayList<String>> actions;
	
	/**
	 * construct an empty plan
	 */
	public Plans() {
		this.results = new ArrayList<Result>();
		this.actions = new ArrayList<ArrayList<String>>();
	}
	
	/**
	 * add a new pair of belief-state and corresponding (equivalent) actions 
	 * @param beliefBeliefState the belief state to add
	 * @param action a list of alternative actions to perform. Only one of them is chosen but their results should be similar
	 */
	public void addPlan(Result beliefBeliefState, ArrayList<String> action) {
		this.results.add(beliefBeliefState);
		this.actions.add(action);
	}
	
	/**
	 * return the number of belief-states/actions pairs
	 * @return the number of belief-states/actions pairs
	 */
	public int size() {
		return this.results.size();
	}
	
	/**
	 * return one of the belief-state of the plan
	 * @param index index of the belief-state
	 * @return the belief-state corresponding to the index
	 */
	public Result getResult(int index) {
		return this.results.get(index);
	}
	
	/**
	 * return the list of actions performed for a given belief-state
	 * @param index index of the belief-state
	 * @return the set of actions to perform for the belief-state corresponding to the index
	 */
	public ArrayList<String> getAction(int index){
		return this.actions.get(index);
	}
}

/**
 * class used to represent a transition function i.e., a set of possible belief states the agent may be in after performing an action
 */
class Result{
	private ArrayList<BeliefState> beliefStates;

	/**
	 * construct a new result
	 * @param states the set of states corresponding to the new belief state
	 */
	public Result(ArrayList<BeliefState> states) {
		this.beliefStates = states;
	}

	/**
	 * returns the number of belief states
	 * @return the number of belief states
	 */
	public int size() {
		return this.beliefStates.size();
	}

	/**
	 * return one of the belief state
	 * @param index the index of the belief state to return
	 * @return the belief state to return
	 */
	public BeliefState getBeliefState(int index) {
		return this.beliefStates.get(index);
	}
	
	/**
	 * return the list of belief-states
	 * @return the list of belief-states
	 */
	public ArrayList<BeliefState> getBeliefStates(){
		return this.beliefStates;
	}
}


/**
 * class implement the AI to choose the next move of the Pacman
 */
public class AI{
	/**
	 * function that compute the next action to do (among UP, DOWN, LEFT, RIGHT)
	 * @param beliefState the current belief-state of the agent
	 * @param deepth the deepth of the search (size of the largest sequence of action checked)
	 * @return a string describing the next action (among PacManLauncher.UP/DOWN/LEFT/RIGHT)
	 */
	
	private static final int MAX_DEPTH = 3;
	private static String lastAction = "";
	
	// ═══════════════════════════════════════════════════════════════════
	// NOUVEAU : Variables pour détecter la stagnation (pas de gomme mangée)
	// ═══════════════════════════════════════════════════════════════════
	private static int lastGommeCount = -1;           // Nombre de gommes au tour précédent
	private static int stagnationCounter = 0;         // Nombre de tours sans manger
	private static final int STAGNATION_THRESHOLD = 5;   // Seuil avant d'appliquer des pénalités
	private static final int STAGNATION_PENALTY = 500;   // Pénalité de base par tour de stagnation
	// ═══════════════════════════════════════════════════════════════════

	public static String findNextMove(BeliefState beliefState) {
        
		// ═══════════════════════════════════════════════════════════════════
		// MISE À JOUR DU COMPTEUR DE STAGNATION
		// ═══════════════════════════════════════════════════════════════════
		int gommesActuelles = beliefState.getNbrOfGommes();
		
		if (lastGommeCount == -1) {
			// Premier tour du jeu : initialisation
			lastGommeCount = gommesActuelles;
			stagnationCounter = 0;
		} else if (gommesActuelles < lastGommeCount) {
			// Pac-Man a mangé une ou plusieurs gommes ! On reset le compteur
			stagnationCounter = 0;
		} else {
			// Aucune gomme mangée ce tour, on incrémente le compteur
			stagnationCounter++;
		}
		lastGommeCount = gommesActuelles;
		
		// DEBUG : décommenter pour voir le compteur en console
		// System.out.println("[STAGNATION] " + stagnationCounter + " tours sans manger");
		// ═══════════════════════════════════════════════════════════════════
		
        Plans plans = beliefState.extendsBeliefState(); 
        if (plans.size() == 0) return PacManLauncher.UP;
        
        String bestAction = plans.getAction(0).get(0);
        double bestValue = Double.NEGATIVE_INFINITY;
        
        int gommesInitiales = beliefState.getNbrOfGommes();
        int distInitiale = beliefState.distanceMinToGum();
        
        // === POSITION DE PACMAN ===
        Position pacmanPos = beliefState.getPacmanPosition();
        int pacRow = pacmanPos.getRow();
        int pacCol = pacmanPos.getColumn();
        
        // === FANTÔME BLEU : Position et distance ===
        int[] fantomeBleuPos = trouverFantomeBleuPlusProche(beliefState, pacRow, pacCol);
        int distInitialeFantomeBleu = fantomeBleuPos[2];
        
        // === FANTÔME NORMAL : Distance du plus proche ===
        // (pour savoir si c'est sûr de chasser les bleus)
        int distFantomeNormal = Integer.MAX_VALUE;
        for (int g = 0; g < beliefState.getNbrOfGhost(); g++) {
            if (beliefState.getCompteurPeur(g) > 0) continue; // ignorer les bleus
            for (Object obj : beliefState.getGhostPositions(g)) {
                Position ghostPos = (Position) obj;
                int dist = Math.abs(ghostPos.getRow() - pacRow) 
                         + Math.abs(ghostPos.getColumn() - pacCol);
                if (dist < distFantomeNormal) {
                    distFantomeNormal = dist;
                }
            }
        }

        for (int i = 0; i < plans.size(); i++) {
            ArrayList<String> actions = plans.getAction(i);
            Result result = plans.getResult(i);
            String currentAction = actions.get(0);
            
            // ═══════════════════════════════════════════════════════════════════
            // PÉNALITÉ POUR LES ACTIONS QUI NE BOUGENT PAS (MUR)
            // Au lieu d'ignorer ces actions, on les pénalise fortement
            // ═══════════════════════════════════════════════════════════════════
            boolean aBouge = true;
            if (result.size() > 0) {
                Position posResultat = result.getBeliefState(0).getPacmanPosition();
                aBouge = (posResultat.getRow() != pacRow || posResultat.getColumn() != pacCol);
            }
            // ═══════════════════════════════════════════════════════════════════
			
            // Évaluer avec l'algorithme AND-OR
            double value = andSearch(result, MAX_DEPTH - 1, gommesInitiales);
            
            // ═══════════════════════════════════════════════════════════════════
            // PÉNALITÉ SI L'ACTION NE FAIT PAS BOUGER PAC-MAN (MUR)
            // ═══════════════════════════════════════════════════════════════════
            if (!aBouge) {
                value -= 5000;  // Forte pénalité, mais on n'ignore pas l'action
            }
            // ═══════════════════════════════════════════════════════════════════
            
            // Pénalité pour mouvements opposés (anti-oscillation simple)
            if (isOpposite(currentAction, lastAction)) {
                value -= 2000;
            }
            
            // ═══════════════════════════════════════════════════════════════════
            // PÉNALITÉ/BONUS DE STAGNATION
            // Plus Pac-Man reste longtemps sans manger, plus il est "forcé" 
            // d'aller vers les gommes
            // ═══════════════════════════════════════════════════════════════════
            if (stagnationCounter > STAGNATION_THRESHOLD) {
                int toursDeStagnation = stagnationCounter - STAGNATION_THRESHOLD;
                
                if (result.size() > 0) {
                    BeliefState nextState = result.getBeliefState(0);
                    int distApres = nextState.distanceMinToGum();
                    int gommesApres = nextState.getNbrOfGommes();
                    
                    if (gommesApres < gommesInitiales) {
                        // Cette action MANGE une gomme : GROS BONUS proportionnel à la stagnation
                        value += toursDeStagnation * STAGNATION_PENALTY * 3;
                    } else if (distApres < distInitiale) {
                        // Se rapproche de la gomme la plus proche : bonus modéré
                        value += toursDeStagnation * STAGNATION_PENALTY;
                    } else if (distApres == distInitiale) {
                        // Reste à la même distance : légère pénalité
                        value -= toursDeStagnation * STAGNATION_PENALTY / 2;
                    } else {
                        // S'ÉLOIGNE de la gomme : PÉNALITÉ forte
                        value -= toursDeStagnation * STAGNATION_PENALTY;
                    }
                }
            }
            // ═══════════════════════════════════════════════════════════════════
            
            // === BONUS FANTÔME BLEU : SÉCURISÉ ===
            // On ne chasse QUE si le fantôme normal est assez loin (>= 4)
            if (distInitialeFantomeBleu <= 2 && distInitialeFantomeBleu > 0 && distFantomeNormal >= 4) {
                
                int newPacRow = pacRow;
                int newPacCol = pacCol;
                if (currentAction.equals("UP")) newPacRow--;
                else if (currentAction.equals("DOWN")) newPacRow++;
                else if (currentAction.equals("LEFT")) newPacCol--;
                else if (currentAction.equals("RIGHT")) newPacCol++;
                
                int newDistFantomeBleu = Math.abs(newPacRow - fantomeBleuPos[0]) 
                                       + Math.abs(newPacCol - fantomeBleuPos[1]);
                
                // BONUS si Pac-Man se RAPPROCHE du fantôme bleu
                if (newDistFantomeBleu < distInitialeFantomeBleu) {
                    if (newDistFantomeBleu == 0) {
                        value += 8000;   // Va le manger
                    } else if (newDistFantomeBleu == 1) {
                        value += 5000;   // Juste à côté
                    } else if (newDistFantomeBleu == 2) {
                        value += 3000;   // Se rapproche
                    } 
                }
                // PÉNALITÉ si s'éloigne du fantôme bleu
                else if (newDistFantomeBleu > distInitialeFantomeBleu) {
                    value -= 2000;
                }
            }
            // CAS SPÉCIAL : fantôme bleu juste à côté (dist=1), on le mange même si danger
            else if (distInitialeFantomeBleu == 1) {
                int newPacRow = pacRow;
                int newPacCol = pacCol;
                if (currentAction.equals("UP")) newPacRow--;
                else if (currentAction.equals("DOWN")) newPacRow++;
                else if (currentAction.equals("LEFT")) newPacCol--;
                else if (currentAction.equals("RIGHT")) newPacCol++;
                
                int newDistFantomeBleu = Math.abs(newPacRow - fantomeBleuPos[0]) 
                                       + Math.abs(newPacCol - fantomeBleuPos[1]);
                
                if (newDistFantomeBleu == 0) {
                    value += 3000;  // Petit bonus pour le manger "en passant"
                }
            }
            
            // === BONUS POUR GOMMES (progression immédiate) ===
            double progressBonus = evaluerProgressionImmediate(result, gommesInitiales, distInitiale);
            value += progressBonus;
            
            // Petit bruit aléatoire pour départager les égalités
            value += Math.random() * 3.0;

            if (value > bestValue) {
                bestValue = value;
                bestAction = currentAction;
            }
        }
        lastAction = bestAction;
        return bestAction;
    }
    
    /**
     * Trouve la position du fantôme bleu le plus proche
     * @return [row, col, distance] ou [0, 0, MAX_VALUE] si aucun fantôme bleu
     */
    private static int[] trouverFantomeBleuPlusProche(BeliefState state, int pacRow, int pacCol) {
        int[] result = {0, 0, Integer.MAX_VALUE};
        
        for (int i = 0; i < state.getNbrOfGhost(); i++) {
            if (state.getCompteurPeur(i) > 0) {
                for (Object obj : state.getGhostPositions(i)) {
                    Position ghostPos = (Position) obj;
                    int dist = Math.abs(ghostPos.getRow() - pacRow) 
                             + Math.abs(ghostPos.getColumn() - pacCol);
                    if (dist < result[2]) {
                        result[0] = ghostPos.getRow();
                        result[1] = ghostPos.getColumn();
                        result[2] = dist;
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Vérifie si deux actions sont opposées (pour éviter les oscillations)
     */
    private static boolean isOpposite(String a1, String a2) {
        if (a1 == null || a2 == null) return false;
        return (a1.equals("LEFT")  && a2.equals("RIGHT")) ||
               (a1.equals("RIGHT") && a2.equals("LEFT"))  ||
               (a1.equals("UP")    && a2.equals("DOWN"))  ||
               (a1.equals("DOWN")  && a2.equals("UP"));
    }
    
    /**
     * Évalue si une action fait du progrès immédiat (gommes seulement)
     */
    private static double evaluerProgressionImmediate(Result result, int gommesInitiales, int distInitiale) {
        double bonus = 0;
        
        if (result.size() > 0) {
            BeliefState firstState = result.getBeliefState(0);
            
            // BONUS si on mange une gomme
            int gommesResultat = firstState.getNbrOfGommes();
            if (gommesResultat < gommesInitiales) {
                int gommesMangees = gommesInitiales - gommesResultat;
                bonus += gommesMangees * 4000;
            }
            
            // Bonus si on se rapproche de la gomme la plus proche
            int distResultat = firstState.distanceMinToGum();
            if (distResultat < distInitiale) {
                bonus += (distInitiale - distResultat) * 100;
            }
        }
        
        return bonus;
    }

    /**
     * AND-SEARCH : moyenne pondérée sur tous les états possibles
     */
    private static double andSearch(Result result, int depth, int gommesInitiales) {
        if (result.size() == 0) return Double.NEGATIVE_INFINITY;

        double totalScore = 0;
        int countValid = 0;
        
        for (BeliefState bs : result.getBeliefStates()) {
            double val = orSearch(bs, depth, gommesInitiales);
            
            // Pondération : on réduit l'impact des états catastrophiques
            if (val > -50000) {
                totalScore += val;
                countValid++;
            } else {
                totalScore += val * 0.3;
                countValid++;
            }
        }
        
        return countValid > 0 ? totalScore / countValid : Double.NEGATIVE_INFINITY;
    }

    /**
     * OR-SEARCH : cherche la meilleure action possible
     */
    private static double orSearch(BeliefState state, int depth, int gommesInitiales) {
        // Conditions d'arrêt
        if (depth == 0 || state.getNbrOfGommes() == 0 || state.getLife() <= 0) {
            return eval(state, gommesInitiales);
        }

        Plans plans = state.extendsBeliefState();
        if (plans.size() == 0) return eval(state, gommesInitiales);

        double maxVal = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < plans.size(); i++) {
            Result res = plans.getResult(i);
            double val = andSearch(res, depth - 1, gommesInitiales);
            if (val > maxVal) {
                maxVal = val;
            }
        }
        
        return (maxVal == Double.NEGATIVE_INFINITY) ? -100000.0 : maxVal;
    }

    /**
     * Fonction d'évaluation d'un état
     */
    private static double eval(BeliefState state, int gommesInitiales) {
        
        // État terminal : mort
        if (state.getLife() <= 0) return -100000.0;
        
        double score = state.getScore();
        
        int nbGommes = state.getNbrOfGommes();
        int distGomme = state.distanceMinToGum();
        
        // Bonus pour les gommes mangées
        int gommesMangees = gommesInitiales - nbGommes;
        score += gommesMangees * 500;
        
        // Pénalité pour les gommes restantes
        score -= nbGommes * 100;
        
        // Distance à la gomme (poids dynamique : plus important quand peu de gommes)
        int poidsDistance = 50 + (1500 / (nbGommes + 1));
        
        if (distGomme < Integer.MAX_VALUE) {
            score -= distGomme * poidsDistance;
        }
        
        // Danger des fantômes
        score -= evaluerDangerFantomes(state);

        return score;
    }

    /**
     * Évalue le danger représenté par les fantômes normaux (non-bleus)
     */
    private static double evaluerDangerFantomes(BeliefState state) {
        double danger = 0;
        
        Position pacman = state.getPacmanPosition();
        int pacRow = pacman.getRow();
        int pacCol = pacman.getColumn();
        
        int distFantomeLePlusProche = Integer.MAX_VALUE;
        int nombreFantomesProches = 0;
        
        for (int i = 0; i < state.getNbrOfGhost(); i++) {
            
            // FANTÔME BLEU : ignoré (pas de danger)
            if (state.getCompteurPeur(i) > 0) {     
                continue;
            }
            
            // FANTÔME NORMAL
            for (Object obj : state.getGhostPositions(i)) {
                Position ghostPos = (Position) obj;
                int dist = Math.abs(ghostPos.getRow() - pacRow) 
                         + Math.abs(ghostPos.getColumn() - pacCol);
                if (dist < distFantomeLePlusProche) {
                    distFantomeLePlusProche = dist;
                }
                
                if (dist == 1) {
                    nombreFantomesProches++;
                }
            }
        }
        
        // Pénalités selon la distance
        if (distFantomeLePlusProche == 0) {
            danger += 100000;  // collision = mort
        } else if (distFantomeLePlusProche == 1) {           
            danger += 2000;           
        } else if (distFantomeLePlusProche == 2) {
            danger += 1000;
        } else if (distFantomeLePlusProche == 3) {
            danger += 500;
        } else if (distFantomeLePlusProche == 4) {
            danger += 200;
        }
        
        return danger;
    }
    
    /**
     * Réinitialise les compteurs statiques (utile si on relance une partie)
     */
    public static void reset() {
        lastAction = "";
        lastGommeCount = -1;
        stagnationCounter = 0;
    }
	
}

    
    
	
