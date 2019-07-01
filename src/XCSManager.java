import java.io.*;

/**
 * This class manages the XCS-System. It stores the population, determines a
 * suitable action and evolves the the stored data.
 * 
 * @author Nugroho Fredivianus, based on Clemens Gerbacher
 * @version XCS-RC 2.0
 */

public class XCSManager implements Serializable {

	static final long serialVersionUID = 512947934;
	/**
	 * Stores the current population of XCS.
	 */
	public XClassifierSet pop = null;
	private XClassifierSet matchSet = null;
	private PredictionArray predictionArray = null;

	public int maxPopSize;
	public int cllSize;
	public boolean combCov;
	private String input;
	private int actionWinner;
	private int cycles = 0;
	private int minExp;
	private int Tcomb = 0;
	private boolean debugMode = false;

	private MazeEnvironment env = null;

	/**
	 * Stores the actionSet
	 */
	private XClassifierSet actionSet = null;

	/**
	 * Writer to the specified log file.
	 */
	private PrintWriter writer = null;

	/**
	 * number of available actions in the environment
	 */
	private int numberOfActions;

	/**
	 * Number of explorations
	 */
	public int explorationCount = 0;

	/**
	 * Constructor of the XCSManager with active logfile-mode.
	 * 
	 * @param numberOfActions
	 *            number of possible actions.
	 * @param outFile
	 *            DebugFile
	 * @param seed
	 *            Random seed
	 */
	public XCSManager(int numberOfActions, int maxPopSize, int Tcomb, boolean debugMode, File outFile, int seed) {
		this.maxPopSize = maxPopSize;
		XCSConstants.maxPopSize = maxPopSize;
		this.Tcomb = Tcomb;
		XCSConstants.Tcomb = Tcomb;
		this.debugMode = debugMode;
		XCSConstants.random.setSeed(seed);
		this.minExp = (Tcomb>0)? XCSConstants.minExp:1;

		// specify output file
		if (debugMode)
			try {
				writer = new PrintWriter(new FileOutputStream(outFile), true);
			} catch (IOException e) {
				System.err.println("Error accesing file");
			}

		pop = new XClassifierSet(numberOfActions, maxPopSize);
		this.numberOfActions = numberOfActions;

	}

	/**
	 * Constructs the XCS system for a multi-step problem.
	 */
	public XCSManager(MazeEnvironment e, int maxPopSize, int Tcomb, boolean debugMode, File outFile, int seed) {
		this.maxPopSize = maxPopSize;
		this.Tcomb = Tcomb;
		this.debugMode = debugMode;
		XCSConstants.random.setSeed(seed);
		minExp = (Tcomb>0) ? XCSConstants.minExp:1;
		env = e;

		// specify output file
		if (debugMode)
			try {
				writer = new PrintWriter(new FileOutputStream(outFile), true);
			} catch (IOException ex) {
				System.err.println("Error accesing file");
			}

		// initialize XCS
		this.numberOfActions = e.getNrActions();
		pop = new XClassifierSet(this.numberOfActions, maxPopSize);
	}

	/**
	 * Initiate a match set.
	 * 
	 * @param state
	 *            The current state of the environment.
	 * @param steps
	 *            How many steps had XCS run.
	 * @param exploreMode
	 *            Action selection mode.
	 * @return the match set.
	 */
	public XClassifierSet getMatchSet(double[] state, int steps, int exploreMode) {
		// create MatchSet
		XClassifierSet dummySet = new XClassifierSet(state, pop, steps,
				numberOfActions, maxPopSize, (exploreMode == 1));

		return dummySet;
	}

	/**
	 * Selects a winner by forming a prediction array.
	 * 
	 * @param dummySet
	 *            The classifier set out of which a prediction array is formed
	 *            (normally the match set).
	 * @param exploreMode
	 *            The action selection mode, set to '1' if exploration.
	 * @return the selected action to be executed.
	 */
	public int getActionWinner(XClassifierSet dummySet, int exploreMode) {
		predictionArray = new PredictionArray(dummySet, numberOfActions, minExp);
		int dummyWinner = -1;

		explorationCount += exploreMode;

		if (exploreMode == 1)
			dummyWinner = (Tcomb>0) ? predictionArray.exploreActionWinner(numberOfActions, minExp)
					: predictionArray.randomActionWinner(numberOfActions);
		else
			dummyWinner = predictionArray
					.luckyBestActionWinner(numberOfActions);

		return dummyWinner;
	}

	/**
	 * Initiates an action set.
	 * 
	 * @param secondDummySet
	 *            The match set.
	 * @param dummyWinner
	 *            The action winner.
	 * @return the action set.
	 */
	public XClassifierSet getActionSet(XClassifierSet secondDummySet, int dummyWinner) {
		XClassifierSet dummySet = new XClassifierSet(secondDummySet, dummyWinner);
		return dummySet;
	}

	public void writeDebug(String title) {
		// print Classifier into file
		if (debugMode)
			writeDebugFile(title, pop, matchSet, actionSet);
	}

	public void updateActionSet(double reward) {

		// updating actionSet
		this.actionSet.updateSet(0., getInput(), this.actionWinner, reward);

		if (Tcomb>0) {
			cycles++;
			if (cycles % Tcomb == 0 && this.pop.changed) {
				//this.pop = sortSet(this.pop);
				this.pop.combine(numberOfActions);				
			}
		}
	}

	/**
	 * Executes one explore or exploit trial for a multi-step problem.
	 * 
	 * @param state
	 *            coded current situation (starting state)
	 * @param exploreMode
	 *            integer '1' if explore mode
	 * @param stepsToGoal
	 * @return action for the next period
	 *
	 */
	public int doOneMultiStep(double[] state, int exploreMode, int[] stepsToGoal,
			double[] sysError, int trialCounter, int stepCounter) {
		XClassifierSet prevActionSet = null;
		double prevReward = 0., prevPrediction = 0.;
		int steps;

		if (exploreMode == 0)
			sysError[trialCounter % 50] = 0.;

		for (steps = 0; steps < XCSConstants.teletransportation; steps++) {
			this.matchSet = getMatchSet(state, stepCounter + steps
					* exploreMode, exploreMode);
			this.actionWinner = getActionWinner(this.matchSet, exploreMode);
			this.actionSet = getActionSet(this.matchSet, this.actionWinner);

			double reward = env.executeAction(this.actionWinner);

			if (prevActionSet != null) {
				prevActionSet.confirmClassifiersInSet();
				prevActionSet.updateSet(predictionArray.getBestValue(), getInput(), this.actionWinner, prevReward);
				if (exploreMode == 0)
					sysError[trialCounter % 50] += (double) Math .abs(XCSConstants.gamma
						* predictionArray.getValue(actionWinner)
						+ prevReward - prevPrediction) / (double) env.getMaxPayoff();
			}

			if (env.doReset()) {
				actionSet.confirmClassifiersInSet();
				actionSet.updateSet(0., getInput(), this.actionWinner, reward);
				if (exploreMode == 0) {
					sysError[trialCounter % 50] += (double) Math.abs(reward
						- predictionArray.getValue(actionWinner)) / (double) env.getMaxPayoff();
					steps++;
				}

				if (Tcomb>0)// && this.pop.changed)
					if ((2 * trialCounter + exploreMode + 1) % Tcomb == 0)
						this.pop.combine(this.numberOfActions);
				break;
			}

			prevActionSet = actionSet;
			if (exploreMode == 0)
				prevPrediction = predictionArray.getValue(actionWinner);
			prevReward = reward;
			state = env.getCurrentState();
		}
		if (exploreMode == 0) {
			sysError[trialCounter % 50] /= steps;
			stepsToGoal[trialCounter % 50] = steps;
		}

		return stepCounter + steps;
	}
	
	private double[] getInput() {
		String[] inputs = this.input.split(";");
		int len = inputs.length;
		double[] state = new double[len];
		
		for (int i=0; i<len; i++)
			state[i] = Double.valueOf(inputs[i]);
		
		return state;
	}

	/**
	 * Executes one explore or exploit loop for a single step problem.
	 * 
	 * @param state
	 *            coded current situation
	 * @param exploreMode
	 *            integer '1' if explore mode
	 * @return action for the next period
	 */
	public int nextAction(String originalInput, int exploreMode) {
		input = "";
		
		if (originalInput.matches("[0-9]+")) {
			int len = originalInput.length();
			
			for (int i=0; i<len; i++) {
				input += originalInput.charAt(i);
				if (i<len - 1) input += ";";
			}
		} else
		if (originalInput.matches("[0-9, /., /;]+")) {
			input = originalInput;
		}
			
		double[] state = getInput();
		
		this.matchSet = getMatchSet(state, explorationCount, exploreMode);
		this.actionWinner = getActionWinner(this.matchSet, exploreMode);
		this.actionSet = getActionSet(this.matchSet, this.actionWinner);

		return actionWinner;
	}

	/**
	 * Returns the action set.
	 * 
	 * @return the action set.
	 */
	public XClassifierSet getActionSet() {
		return this.actionSet;
	}

	/**
	 * Returns the number of classifiers in the population.
	 * 
	 * @return the number of classifiers in the population.
	 */
	public int getPopSize() {
		return this.pop.getSize();
	}

	/**
	 * Run one learning cycle, starting from updating the action set.
	 * 
	 * @param reward
	 *            Received reward for the last action.
	 * @param state
	 *            Current observed state.
	 * @param exploreMode
	 *            Action selection mode; set to '1' if explore
	 * @param tick
	 *            Current cycle number.
	 * @return the next action to be executed.
	 */
	public int oneLoop(double reward, String state, int exploreMode, int tick) {

		if (this.actionSet != null)
			updateActionSet(reward);
		return nextAction(state, exploreMode);
	}

	/**
	 * Sets the population to empty.
	 */
	public void emptyRuleBase() {
		this.pop = null;
		this.pop = new XClassifierSet(numberOfActions, maxPopSize);
		this.matchSet = null;
		this.actionSet = null;
	}

	/**
	 * Sorting a classifier set, is mainly used for getting printable outputs.
	 * 
	 * @param sortPop
	 *            The current classifier set to be sorted.
	 * @return a sorted set.
	 *
	 */
	private XClassifierSet sortSet(XClassifierSet theSet) {

		boolean change = true;
		cllSize = theSet.getSize();
		double[] pred = new double[cllSize];
		int[] acti = new int[cllSize];
		int[] expe = new int[cllSize];
		double[][] cond = new double[cllSize][];
		double[] val = new double[cllSize];
		XClassifier[] clSet = theSet.getSet();

		for (int i=0; i<cllSize; i++) {
			pred[i] = Math.round(clSet[i].getPrediction());
			expe[i] = clSet[i].getExperience();
		}

		while (change) {
			change = false;

			for (int i=0; i<cllSize; i++)
				for (int j = i + 1; j < cllSize; j++)
					if ((pred[i] < pred[j] || expe[i] == 0) && expe[j] > 0) {
						XClassifier cl = clSet[i];
						clSet[i] = clSet[j];
						clSet[j] = cl;

						double pre = pred[i];
						pred[i] = pred[j];
						pred[j] = pre;

						int exp = expe[i];
						expe[i] = expe[j];
						expe[j] = exp;

						change = true;

						j--;
					}
		}

		for (int i=0; i<cllSize; i++) {
			pred[i] = Math.round(clSet[i].getPrediction());
			acti[i] = clSet[i].getAction();
			expe[i] = clSet[i].getExperience();
		}

		for (int i=0; i<cllSize; i++)
			for (int j = i + 1; j < cllSize; j++)
				if (pred[i] == pred[j] && acti[i] > acti[j] && expe[j] > 0) {

					XClassifier cl = clSet[i];
					clSet[i] = clSet[j];
					clSet[j] = cl;

					int act = acti[i];
					acti[i] = acti[j];
					acti[j] = act;

					j--;
				}

		double max = 0;
		double min = 999999;

		for (int i=0; i<cllSize; i++) {
			pred[i] = Math.round(clSet[i].getPrediction());
			acti[i] = clSet[i].getAction();
			cond[i] = clSet[i].getElements();
			val[i] = 0;
			double ch = cond[i][0];
			if (max < ch) max = ch;
			if (min > ch) min = ch;
		}

		max++;
		min=0;

		for (int i=0; i<cllSize; i++) {
			val[i] = 0;
			for (int j=0; j<cond[i].length/2; j++)
				val[i] += Math.pow(2, cond[i].length/2 - j) * cond[i][2*j+1];
		}

		for (int i=0; i<cllSize; i++)
			for (int j = i + 1; j < cllSize; j++)
				if (pred[i] == pred[j] && acti[i] == acti[j] && val[i] > val[j] && expe[j] > 0) {

					XClassifier cl = clSet[i];
					clSet[i] = clSet[j];
					clSet[j] = cl;

					double value = val[i];
					val[i] = val[j];
					val[j] = value;

					j--;
				}

		return theSet;

	}

	/**
	 * Writes the current Status into a log-file
	 */
	private void writeDebugFile(String title, XClassifierSet popx,
			XClassifierSet matchSetx, XClassifierSet actionSetx) {

		XClassifierSet myPop = sortSet(popx);

		writer.println(title);
		writer.println("Population");
		popx.printSet(writer);

		writer.println();

	}
}