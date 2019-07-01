import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Vector;

/**
 * This class handles the different sets of classifiers. It stores each set in
 * an array. The array is initialized to a sufficient large size so that no
 * changes in the size of the array will be necessary. The class provides
 * constructors for constructing
 * <ul>
 * <li>the empty population,
 * <li>the match set, and
 * <li>the action set.
 * </ul>
 * It provides all necessary different sums and averages of parameters
 * in the set. Finally, it handles addition, deletion and combining of
 * classifiers.
 * 
 * @author Nugroho Fredivianus, based on Martin Butz
 * @version XCS-RC 2.0
 */
public class XClassifierSet implements Serializable {
	/**
	 * Each set keeps a reference to the parent set out of which it was
	 * generated. In the population itself this pointer is set to zero.
	 */
	private XClassifierSet parentSet;

	/**
	 * The classifier list (in form of an array)
	 */
	private XClassifier[] clSet;

	/**
	 * The actual number of macro-classifiers in the list (which is in fact
	 * equal to the number of entries in the array).
	 */
	private int cllSize;

	/**
	 * Indicator whether the population has been changed since the last
	 * combining execution.
	 */
	public boolean changed = false;

	/**
	 * Creates a new, empty population initializing the population array to the
	 * maximal population size plus the number of possible actions.
	 * 
	 * @see XCSConstants#maxPopSize
	 * @param numberOfActions
	 *            The number of actions possible in the problem.
	 */
	public XClassifierSet(int numberOfActions, int maxPopSize) {
		cllSize = 0;
		parentSet = null;
		clSet = new XClassifier[maxPopSize + numberOfActions];
	}

	/**
	 * Constructs a match set out of the population. After the creation, it is
	 * checked if the match set covers all possible actions in the environment.
	 * If one or more actions are not present, covering occurs, generating the
	 * missing action(s). If maximal population size is reached when covering,
	 * deletion occurs.
	 * 
	 * @see XClassifier#XClassifier(double,int,String,int)
	 * @see XCSConstants#maxPopSize
	 * @see #deleteFromPopulation
	 * @param state
	 *            The current situation/problem instance.
	 * @param pop
	 *            The current population of classifiers.
	 * @param time
	 *            The actual number of instances the XCS learned from so far.
	 * @param numberOfActions
	 *            The number of actions possible in the environment.
	 * @param maxPopSize
	 *            The maximum number of classifiers in the set.
	 * @param exploreMode
	 *            Used for defining exploration/exploitation by match set.
	 */
	public XClassifierSet(double[] state, XClassifierSet pop, int time,
			int numberOfActions, int maxPopSize, boolean exploreMode) {
		parentSet = pop;
		cllSize = 0;
		clSet = new XClassifier[pop.cllSize + numberOfActions];
		XClassifier[] addCl = new XClassifier[pop.cllSize + numberOfActions];
		int entersMatchSet = 0;

		// check, if all possible actions are existed in Population.
		boolean[] coveredActions = new boolean[numberOfActions + 1];
		for (int i = 0; i < numberOfActions; i++)
			coveredActions[i] = false;
		coveredActions[numberOfActions] = true;

		for (int i=0; i<parentSet.cllSize; i++) {
			XClassifier cl = parentSet.clSet[i];
			if (cl.match(state)) {
				coveredActions[cl.getAction()] = true;
				addCl[entersMatchSet++] = cl;
			}
		}

		int uncovered = 0;
		int[] pickAction = new int[numberOfActions];
		for (int i=0; i<numberOfActions; i++)
			if (!coveredActions[i]) {
				coveredActions[numberOfActions] = false;
				pickAction[uncovered] = i;
				uncovered++;
			}

		int space = uncovered;

		while (pop.getNumerositySum() > maxPopSize - space) {
			pop.deleteFromPopulation(state);
			pop.changed = true;
		}

		XClassifier newCl = null;
		int nums = getNumerositySum() + 1;
		// create new XClassifer with random action and add it into the
		// Population.
		if ((uncovered > 0 && exploreMode) || uncovered == numberOfActions)
			for (int i = 0; i < space; i++) {
				newCl = new XClassifier(nums++, time, state, pickAction[i]);
				pop.addXClassifierToPopulation(newCl);
				addCl[entersMatchSet++] = newCl;
			}

		// adding matching XClassifier to the match set
		for (int i = 0; i < entersMatchSet; i++)
			addClassifier(addCl[i]);
	}

	/**
	 * Constructs an action set out of the given match set.
	 * 
	 * @param matchSet
	 *            The current match set
	 * @param action
	 *            The chosen action for the action set.
	 */
	public XClassifierSet(XClassifierSet matchSet, int action) {
		parentSet = matchSet;
		cllSize = 0;
		clSet = new XClassifier[matchSet.cllSize];

		for (int i = 0; i < matchSet.cllSize; i++) {
			if (matchSet.clSet[i].getAction() == action)
				addClassifier(matchSet.clSet[i]);
		}
	}

	/**
	 * Transform real string to binary for condition
	 * 
	 * @param realString
	 * @return binary string
	 */
	private String condRealToBin(String realString) {
		String replacedString = realString.replace("[0.0]", "0");
		replacedString = replacedString.replace("[1.0]", "1");
		replacedString = replacedString.replace("[0.0..1.0]", "#");
		return ("'" + replacedString);
	}
	
	/**
	 * Checks whether the interval of given values satisfies a tolerance value
	 * of predErrTol
	 * 
	 * @see predErrTol
	 * @return true if the interval is less than or equal to the tolerance.
	 */
	private boolean withinRange(double val1, double val2) {
		return (val2 + XCSConstants.predTol >= val1 && val2 <= val1
				+ XCSConstants.predTol);
	}

	/**
	 * Collects classifiers in the population having a particular action. The
	 * classifiers will enter the Combining Set.
	 */
	private XClassifier[] combineSet(XClassifier[] clComb, int action) {
		// Set reporting true to print changes to the console.
		boolean reporting = false;
		int numElements = clComb[0].getNumElements();
		double[] clStar = new double[numElements];
		int noCombining = 0;

		while (noCombining<2) {

			int minExp = XCSConstants.minExp;
			double predTol = XCSConstants.predTol;

			for (int i=0; i<cllSize; i++)
				for (int j=i+1; j<cllSize; j++)
					if (clComb[i].getExperience() >= minExp && clComb[j].getExperience() >= minExp 
					&& Math.abs(clComb[i].getPrediction() - clComb[j].getPrediction()) <= predTol) {
						clStar = combineCondition(i, j);
						String stringElements = "";
						for (int m=0;m<clStar.length/2;m++)
							if (clStar[2*m] == clStar[2*m+1]) stringElements += "[" + clStar[2*m] + "]";
							else stringElements += "[" + clStar[2*m] + ".." + clStar[2*m+1] + "]";
						
						int testNum = clComb[i].getNumerosity() + clComb[j].getNumerosity();
						double testPred = clComb[i].getPrediction() * clComb[i].getNumerosity()
							+ clComb[j].getPrediction() * clComb[j].getNumerosity();
						double clStarPred = testPred / testNum;
						
						if (reporting) {
							System.out.println(testPred + ";" +testNum);
							System.out.println("Parent1("+i+"): " + clComb[i].stringCondition() + ":" + clComb[i].getAction() + "->" + clComb[i].getPrediction() +
									"\nParent2("+j+"): " + clComb[j].stringCondition() + ":" + clComb[j].getAction() + "->" + clComb[j].getPrediction());
							System.out.print("Candidate:" + condRealToBin(stringElements) + ":" + action + "->" + clStarPred);
						}
						
						boolean noDisproval = true;
						for (int k=0; k<cllSize; k++)
							if (k != i && k != j && clComb[k].getExperience() > 0)
								if (clComb[k].condOverlap(clStar) && !withinRange(clStarPred, clComb[k].getPrediction())) {
									noDisproval = false;
									if (reporting) {
										System.out.println(" ... is disproved by " + clComb[k].stringCondition()
												+ ":" + action + "->" + clComb[k].getPrediction());										
									}
									if (XCSConstants.maxDispRate>0) clComb[k].incrementDisproving();
										else k = cllSize;
								}
						
						if (noDisproval) {
							if (reporting)
								System.out.println(" ... is accepted.");
							
							XClassifier[] clDel = new XClassifier[cllSize];
							int dels = 0;
							double clPred = 0.0;
							int clNum = 0;
							int clExp = 0;

							for (int n = cllSize - 1; n >= 0; n--) {
								double itsPred = clComb[n].getPrediction();
								int itsExp = clComb[n].getExperience();
								boolean range = withinRange(clStarPred, itsPred);
								if (clComb[n].isSubsumableTo(clStar) && (range || itsExp == 0)) {
									clDel[dels] = clComb[n];

									if (itsExp > 0) {
										int itsNum = clDel[dels].getNumerosity();
										clExp += itsExp;
										clNum += itsNum;
										clPred += clDel[dels].getPrediction()
												* itsNum;
									}

									parentSet.removeClassifier(clDel[dels]);
									removeClassifier(clDel[dels]);
									dels++;
								}
							}
							clPred = clPred / clNum;

							XClassifier clNew = new XClassifier(clStar, action);

							double beta = XCSConstants.beta;
							double predIni = XCSConstants.predictionIni;
							double expLim = 1 / beta;
							double clPredErr = (clExp <= (int) Math.floor(expLim)) ? Math.abs(clPred - predIni) / clExp
									: (Math.abs(clPred - predIni) / expLim) * Math.pow(1 - beta, clExp - (int) Math.floor(expLim));
							double clFit = (XCSConstants.fitnessIni - 1) * Math.pow(1 - beta, clExp) + 1;

							clNew.addNumerosity(clNum - 1);
							clNew.setExperience(clExp);
							clNew.setPrediction(clPred);
							clNew.setFitness(clFit);
							clNew.setPredictionError(clPredErr);
							clNew.setDisproving(0);

							parentSet.addXClassifierToPopulation(clNew);
							addClassifier(clNew);

							parentSet.changed = true;
							noCombining = 0;
						}
					}
			noCombining++;
		}

		//check outlier
		if (XCSConstants.maxDispRate > 0)
			for (int i=0; i<cllSize; i++)
				if (clSet[i].getExperience() > 0) {
					if (clSet[i].getDisproving() / clSet[i].getExperience() > Math.pow(10, XCSConstants.maxDispRate)) {
						parentSet.removeClassifier(clSet[i]);
						removeClassifier(i);
						parentSet.changed = true;
					}			
				}

		return clComb;
	}

	/**
	 * Constructs a combining set out of population.
	 * 
	 * @param pop
	 *            Parent of the combining set (should be the population).
	 * @param length
	 *            The length of the classifiers to be recruited to the combining
	 *            set. Set it zero to ignore the length.
	 * @param action
	 *            The action of the member classifiers.
	 */
	public XClassifierSet(XClassifierSet pop, int length, int action) {
		this.parentSet = pop;
		cllSize = 0;
		parentSet.changed = false;

		// recruiting
		int[] member = new int[parentSet.cllSize];
		int members = 0;
		for (int i = 0; i < parentSet.cllSize; i++)
			if (parentSet.clSet[i].getAction() == action
					&& (length == 0 || parentSet.clSet[i].getNumElements() == length))
				member[members++] = i;

		clSet = new XClassifier[members];

		for (int i = 0; i < members; i++)
			addClassifier(parentSet.clSet[member[i]]);

		this.clSet = combineSet(this.clSet, action);
	}

	/**
	 * Returns the position of the classifier in the set if it is present and -1
	 * otherwise.
	 */
	/*
	private int containsClassifier(XClassifier cl) {
		for (int i = 0; i < cllSize; i++)
			if (clSet[i] == cl)
				return i;
		return -1;
	}
	*/

	/**
	 * Updates all parameters in the current set (should be the action set).
	 * Essentially, reinforcement Learning as well as the fitness evaluation
	 * takes place in this set. Moreover, the prediction error and the action
	 * set size estimate is updated. Also, action set subsumption takes place if
	 * selected. As in the algorithmic description, the fitness is updated after
	 * prediction and prediction error. However, in order to be more
	 * conservative the prediction error is updated before the prediction.
	 * 
	 * @see XCSConstants#gamma
	 * @see XClassifier#increaseExperience
	 * @see XClassifier#updatePreError
	 * @see XClassifier#updatePrediction
	 * @see XClassifier#updateActionSetSize
	 * @see #updateFitnessSet
	 * @see XCSConstants#doActionSetSubsumption
	 * @see #doActionSetSubsumption
	 * @param maxPrediction
	 *            The maximum prediction value in the successive prediction
	 *            array (should be set to zero in single step environments).
	 * @param reward
	 *            The actual resulting reward after the execution of an action.
	 */
	public void updateSet(double maxPrediction, double[] state, int action, double reward) {
		XClassifierSet pop = parentSet;
		while (pop.parentSet != null)
			pop = pop.parentSet;

		if (clSet[0] != null) {
			double P = reward + XCSConstants.gamma * maxPrediction;

			for (int i = 0; i < cllSize; i++) {
				double prevPredErr = clSet[i].getPredictionError();

				clSet[i].increaseExperience();
				clSet[i].updatePreError(P);
				clSet[i].updatePrediction(P);

				boolean removed = false;
				if (clSet[i].getPredictionError() > XCSConstants.predErrTol
						&& clSet[i].getPredictionError() >= prevPredErr
						//&& prevPredErr < XCSConstants.predErrTol
						&& clSet[i].getExperience() > 2 * XCSConstants.minExp) {
					//System.out.println(clSet[i].stringCondition() + ":" + clSet[i].getAction() + "->" + clSet[i].getPrediction() + " ... is removed.");
					pop.removeClassifier(clSet[i]);
					removeClassifier(i);
					i--;
					removed = true;
					pop.changed = true;
					XCSConstants.deletion++;
					
					XClassifier newCl = new XClassifier(getNumerositySum()+1, 0, state, action);
					pop.addXClassifierToPopulation(newCl);
				}

				if (!removed) {
					if (clSet[i].getExperience() == XCSConstants.minExp)
						pop.changed = true;
					clSet[i].updateActionSetSize(getNumerositySum());
				}
			}
			updateFitnessSet();				
		}
	}

	/**
	 * Special function for updating the fitnesses of the classifiers in the
	 * set.
	 * 
	 * @see XClassifier#updateFitness
	 */
	private void updateFitnessSet() {
		double accuracySum = 0.;
		double[] accuracies = new double[cllSize];

		// First, calculate the accuracies of the classifier and the accuracy
		// sums
		for (int i = 0; i < cllSize; i++) {
			accuracies[i] = clSet[i].getAccuracy();
			accuracySum += accuracies[i] * clSet[i].getNumerosity();
		}

		// Next, update the fitnesses accordingly
		for (int i = 0; i < cllSize; i++) {
			clSet[i].updateFitness(accuracySum, accuracies[i]);
		}
	}

	/**
	 * Combines two condition strings.
	 * 
	 * @param ds
	 *            The first condition to be combined.
	 * @param ds2
	 *            The second condition to be combined.
	 * @return The combining result.
	 */
	private double[] combineCondition(double[] cond1, double[] cond2) {
		int condLength = cond1.length;
		double[] dummyCond = new double[condLength];

		for (int i=0; i<condLength/2; i++) {
			if (cond1[2*i] < cond2[2*i]) dummyCond[2*i] = cond1[2*i];
				else dummyCond[2*i] = cond2[2*i];
			if (cond1[2*i+1] > cond2[2*i+1]) dummyCond[2*i+1] = cond1[2*i+1];
				else dummyCond[2*i+1] = cond2[2*i+1];
		}
		
		return dummyCond;
	}

	/**
	 * Combines two classifiers' condition strings.
	 * 
	 * @param i
	 *            The index of the first classifier to be combined.
	 * @param j
	 *            The index of the second classifier to be combined.
	 * @return The combining result.
	 */
	private double[] combineCondition(int i, int j) {
		return combineCondition(clSet[i].getElements(), clSet[j].getElements());
	}

	/**
	 * Creating a combining set for specific action.
	 * 
	 * @param action
	 *            The specific action.
	 */
	public void createCombiningSet(int action) {
		new XClassifierSet(this, 0, action);
	}

	/**
	 * Loop actions for combining set creation.
	 * 
	 * @param numberOfActions
	 *            The number of actions involved.
	 */
	public void combine(int numberOfActions) {
		for (int i=0; i<numberOfActions; i++)
			this.createCombiningSet(i);
	}

	/**
	 * Selects one classifier using roulette wheel selection according to the
	 * fitnesses of the classifiers.
	 */
	/*
	private XClassifier selectXClassifierRW(double fitSum) {
		double choiceP = XCSConstants.random.nextDouble() * fitSum;
		int i = 0;
		double sum = clSet[i].getFitness();
		while (choiceP > sum) {
			i++;
			sum += clSet[i].getFitness();
		}

		return clSet[i];
	}
	*/

	/**
	 * Adds the classifier to the population and checks if an identical
	 * classifier exists. If an identical classifier exists, its numerosity is
	 * increased.
	 * 
	 * @see #getIdenticalClassifier
	 * @param cl
	 *            The to be added classifier.
	 */
	private void addXClassifierToPopulation(XClassifier cl) {
		// set pop to the actual population
		XClassifierSet pop = this;
		while (pop.parentSet != null)
			pop = pop.parentSet;

		XClassifier oldcl = null;
		if ((oldcl = pop.getIdenticalClassifier(cl)) != null) {
			oldcl.addNumerosity(1);
		} else {
			pop.addClassifier(cl);
		}

	}

	/**
	 * Looks for an identical classifier in the population.
	 * 
	 * @param newCl
	 *            The new classifier.
	 * @return Returns the identical classifier if found, null otherwise.
	 */
	private XClassifier getIdenticalClassifier(XClassifier newCl) {
		for (int i = 0; i < cllSize; i++)
			if (newCl.equals(clSet[i]))
				return clSet[i];
		return null;
	}

	/**
	 * Deletes one classifier in the population. The classifier that will be
	 * deleted is chosen by roulette wheel selection considering the deletion
	 * vote. Returns the macro-classifier which got decreased by one
	 * micro-classifier.
	 * 
	 * @param state
	 * 
	 * @see XClassifier#getDelProp
	 */
	private XClassifier deleteFromPopulation(double[] state) {
		double meanFitness = getFitnessSum() / (double) getNumerositySum();
		double sum = 0.;

		for (int i = 0; i < cllSize; i++) {
			sum += clSet[i].getDelProp(meanFitness);
		}

		boolean done = false;

		while (!done) {
			double choicePoint = sum * XCSConstants.random.nextDouble();
			sum = 0.;

			for (int i = 0; i < cllSize; i++) {
				sum += clSet[i].getDelProp(meanFitness);
				if (sum > choicePoint) {
					while (clSet[i].match(state)
							&& clSet[i].getNumerosity() == 1) {
						if (i < cllSize - 1)
							i++;
						else
							i = 0;
					}
					clSet[i].addNumerosity(-1);
					if (clSet[i].getNumerosity() == 0)
						removeClassifier(i);
					return clSet[i];
				}
			}
		}

		return null;
	}

	/**
	 * Updates the numerositySum of the set and deletes all classifiers with
	 * numerosity 0.
	 */
	public void confirmClassifiersInSet() {
		int copyStep = 0;
		int i;
		for (i = 0; i < cllSize - copyStep; i++) {
			if (clSet[i + copyStep].getNumerosity() == 0) {
				copyStep++;
				i--;
			} else {
				if (copyStep > 0) {
					clSet[i] = clSet[i + copyStep];
				}
			}
		}
		for (; i < cllSize; i++) {
			clSet[i] = null;
		}
		cllSize -= copyStep;
	}

	/**
	 * Adds a classifier to the set and increases the numerositySum value
	 * accordingly.
	 * 
	 * @param classifier
	 *            The to be added classifier.
	 */
	void addClassifier(XClassifier classifier) {
		clSet[cllSize] = classifier;
		cllSize++;
	}

	/**
	 * Removes the specified (possible macro-) classifier from the population.
	 * The function returns true when the classifier was found and removed and
	 * false otherwise. It does not update the numerosity sum of the set,
	 * neither recursively remove classifiers in the parent set. This must be
	 * done manually where required.
	 */
	public boolean removeClassifier(XClassifier classifier) {
		int i;
		for (i = 0; i < cllSize; i++)
			if (clSet[i] == classifier)
				break;
		if (i == cllSize) {
			return false;
		}
		for (; i < cllSize - 1; i++)
			clSet[i] = clSet[i + 1];
		clSet[i] = null;

		cllSize--;

		return true;
	}

	/**
	 * Removes the (possible macro-) classifier at the specified array position
	 * from the population. The function returns true when the classifier was
	 * found and removed and false otherwise. It does not update the numerosity
	 * of the set, neither recursively remove classifiers in the parent set.
	 * This must be done manually where required.
	 */
	public boolean removeClassifier(int pos) {
		int i;
		for (i = pos; i < cllSize - 1; i++)
			clSet[i] = clSet[i + 1];
		clSet[i] = null;
		cllSize--;

		return true;
	}

	/**
	 * Returns the sum of the prediction values of all classifiers in the set.
	 */
	private double getPredictionSum() {
		double sum = 0.;

		for (int i = 0; i < cllSize; i++) {
			sum += clSet[i].getPrediction() * clSet[i].getNumerosity();
		}
		return sum;
	}

	/**
	 * Returns the sum of the fitnesses of all classifiers in the set.
	 */
	private double getFitnessSum() {
		double sum = 0.;

		for (int i = 0; i < cllSize; i++)
			sum += clSet[i].getFitness();
		return sum;
	}

	/**
	 * Returns the number of micro-classifiers in the set.
	 */
	public int getNumerositySum() {
		// return numerositySum;
		if (clSet == null) {
			return 0;
		}
		int num = 0;
		for (int i=0; i<clSet.length; i++) {
			if (clSet[i] == null) {
				continue;
			}
			num += Math.abs(clSet[i].getNumerosity());
		}
		return num;
	}

	/**
	 * Returns the classifier at the specified position.
	 */
	public XClassifier elementAt(int i) {
		return clSet[i];
	}

	/**
	 * Returns the number of macro-classifiers in the set.
	 */
	public int getSize() {
		return cllSize;
	}

	/**
	 * Returns the set.
	 */
	public XClassifier[] getSet() {
		return clSet;
	}

	/**
	 * Prints the classifier set to the control panel.
	 */
	public void printSet() {
		System.out.println("Pre: " + (getPredictionSum() / getNumerositySum())
				+ " Fit: " + (getFitnessSum() / getNumerositySum()) + " Num: " + getNumerositySum());
		for (int i=0; i<cllSize; i++) {
			String experienced = ""; //(clSet[i].getExperience()>0)?"":"_";
			System.out.print(experienced + (i+1) + ". ");
			clSet[i].printXClassifier();
		}
	}

	/**
	 * Prints the classifier set to the specified print writer (which usually
	 * refers to a file).
	 * 
	 * @param pW
	 *            The print writer that normally refers to a file writer.
	 */
	public void printSet(PrintWriter pW) {
		DecimalFormat report = new DecimalFormat("0.000");

		pW.println("Pre: " + report.format(getPredictionSum() / getNumerositySum())
				+ ";;Fit: " + report.format(getFitnessSum() / getNumerositySum())
				+ ";;Num: " + getNumerositySum());
		pW.println("No;Cond;Act;Predict;PredictErr;Fit;Nums;Exper;Disp;ActSetSize");
		for (int i=0; i<cllSize; i++) {
			String experienced = ""; //(clSet[i].getExperience()>0)?"":"_";
			pW.print(experienced + (i+1) + ";");
			clSet[i].printXClassifier(pW);
		}
	}
}
