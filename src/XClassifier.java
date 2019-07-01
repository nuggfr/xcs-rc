import java.io.Serializable;
import java.io.PrintWriter;
import java.text.DecimalFormat;

/**
 * Each instance of this class represents one classifier. The class provides
 * different constructors for generating
 * <ul>
 * <li>copies of existing classifiers,
 * <li>new matching classifiers with random action,
 * <li>new matching classifiers with specified action, and
 * <li>new completely random classifier.
 * </ul>
 * 
 * @author Nugroho Fredivianus, based on Martin Butz
 * @version XCS-RC 2.0
 */
public class XClassifier implements Serializable {

	/**
	 * The condition of this classifier, only for printout purposes.
	 */
	private String condition;

	/**
	 * The action of this classifier.
	 */
	private int action;

	/**
	 * The reward prediction value of this classifier.
	 */
	private double prediction;

	/**
	 * The reward prediction error of this classifier.
	 */
	private double predictionError;

	/**
	 * The fitness of the classifier in terms of the macro-classifier.
	 */
	private double fitness;

	/**
	 * The numerosity of the classifier. This is the number of micro-classifier
	 * this macro-classifier represents.
	 */
	private int numerosity;

	/**
	 * The experience of the classifier. This is the number of problems the
	 * classifier learned from so far.
	 */
	private int experience;

	/**
	 * The action set size estimate of the classifier.
	 */
	private double actionSetSize;

	/**
	 * The number of elements in a real classifier.
	 */
	private double[] elements;

	/**
	 * The number of disproving given to a candidate classifier in a combinig process.
	 */
	private int disproving;

	/**
	 * Constructs a classifier with matching elements and specified action.
	 * 
	 * @param setSize
	 *            The size of the current set which the new classifier matches.
	 * @param time
	 *            The actual number of instances the XCS learned from so far.
	 * @param situation
	 *            The current problem instance/perception.
	 * @param act
	 *            The action of the new classifier.
	 */
	public XClassifier(double setSize, int time, double[] state, int act) {
		createMatchingElements(state);
		action = act;
		classifierSetVariables(setSize, time);
	}

	/**
	 * Constructs a child classifier with matching elements and specified action.
	 * 
	 * @param setSize
	 *            The size of the current set which the new classifier matches.
	 * @param time
	 *            The actual number of instances the XCS learned from so far.
	 * @param situation
	 *            The current problem instance/perception.
	 * @param act
	 *            The action of the new classifier.
	 */
	public XClassifier(double[] state, int act) {
		elements=state;
		action = act;
		classifierSetVariables(1, 0);
	}

	/**
	 * Constructs a classifier with specific properties.
	 * 
	 */
	public XClassifier(double[] cond, int act, double pred, double predErr,
			double fit, int nums, int exp, int actSetSize) {
		elements = cond;
		action = act;
		prediction = pred;
		predictionError = predErr;
		fitness = fit;
		numerosity = nums;
		experience = exp;
		actionSetSize = actSetSize;
	}

	/**
	 * Construct matching classifier with random action.
	 * 
	 * @param setSize
	 *            The size of the current set which the new classifier matches.
	 * @param time
	 *            The actual number of instances the XCS learned from so far.
	 * @param numberOfActions
	 *            The number of different actions to chose from (This should be
	 *            set to the number of actions possible in the problem).
	 * @param situation
	 *            The current problem instance/perception.
	 */
	public XClassifier(double setSize, int time, int numberOfActions,
			double[] state) {
		createMatchingElements(state);
		createRandomAction(numberOfActions);
		classifierSetVariables(setSize, time);
	}

	/**
	 * Construct a classifier with random elements and random action.
	 * 
	 * @param setSize
	 *            The size of the current set which the new classifier matches.
	 * @param time
	 *            The actual number of instances the XCS learned from so far.
	 * @param condLength
	 *            The length of the elements of the new classifier.
	 * @param numberOfActions
	 *            The number of different actions to chose from
	 */
	public XClassifier(double setSize, int time, int condElements,
			int numberOfActions) {
		createRandomelements(condElements);
		createRandomAction(numberOfActions);
		classifierSetVariables(setSize, time);
	}

	/**
	 * Constructs an identical XClassifier. However, the experience of the copy
	 * is set to 0 and the numerosity is set to 1 since this is indeed a new
	 * individual in a population.
	 * 
	 * @param clOld
	 *            The to be copied classifier.
	 */
	public XClassifier(XClassifier clOld) {
		this.elements = clOld.elements;
		this.action = clOld.action;
		this.prediction = clOld.prediction;
		this.predictionError = clOld.predictionError;
		// Here we should divide the fitness by the numerosity to get a accurate
		// value for the new one!
		this.fitness = clOld.fitness / clOld.numerosity;
		this.numerosity = 1;
		this.experience = 0;
		this.actionSetSize = clOld.actionSetSize;
	}

	/**
	 * Resets a classifier to the initial values.
	 */
	public void reset() {
		this.prediction = XCSConstants.predictionIni;
		this.predictionError = XCSConstants.predictionErrorIni;
		this.fitness = XCSConstants.fitnessIni;
		this.numerosity = 1;
		this.experience = 0;
		this.actionSetSize = 0;
	}

	/**
	 * Creates a elements randomly:
	 */
	private void createRandomelements(int numElements) {
		elements = new double[2 * numElements];
		for (int i = 0; i < numElements; i++) {
			double value1 = XCSConstants.random.nextDouble();
			double value2 = XCSConstants.random.nextDouble();
			if (value1<value2) {
				elements[2*i] = value1;					
				elements[2*i+1] = value2;					
			} else {
				elements[2*i] = value2;					
				elements[2*i+1] = value1;										
			}				
		}
	}

	/**
	 * Creates a matching elements considering the constant DontCare
	 * probability.
	 * 
	 * @see XCSConstants#P_dontcare
	 */
	private void createMatchingElements(double[] state) {
		int stateElements = state.length;
		double[] dummyCond = new double[2*stateElements];

		for (int i=0; i<stateElements; i++) {
			/*
			if (XCSConstants.random.nextDouble() < XCSConstants.P_dontcare) {
				dummyCond[2*i] = state[i] - XCSConstants.random.nextDouble()/2;
				dummyCond[2*i+1] = state[i] + XCSConstants.random.nextDouble()/2;
				if (dummyCond[2*i]<0) state[i]=0;
				if (dummyCond[2*i+1]>1) state[i]=1;
			} else {
			*/
				dummyCond[2*i] = state[i];				
				dummyCond[2*i+1] = state[i];				
			//}
		}
		elements = dummyCond;
	}

	/**
	 * Creates a random action.
	 * 
	 * @param numberOfActions
	 *            The number of actions to chose from.
	 */
	private void createRandomAction(int numberOfActions) {
		action = (int) (XCSConstants.random.nextDouble() * numberOfActions);
	}

	/**
	 * Sets the initial variables of a new classifier.
	 * 
	 * @see XCSConstants#predictionIni
	 * @see XCSConstants#predictionErrorIni
	 * @see XCSConstants#fitnessIni
	 * @param setSize
	 *            The size of the set the classifier is created in.
	 * @param time
	 *            The actual number of instances the XCS learned from so far.
	 */
	private void classifierSetVariables(double setSize, int time) {
		this.prediction = XCSConstants.predictionIni;
		this.predictionError = XCSConstants.predictionErrorIni;
		this.fitness = XCSConstants.fitnessIni;

		this.numerosity = 1;
		this.experience = 0;
		this.actionSetSize = setSize;
	}

	/**
	 * Returns if the classifier matches in the current situation.
	 * 
	 * @param state
	 *            The current situation which can be the current state or
	 *            problem instance.
	 */
	public boolean match(double[] state) {
		if (elements.length != 2*state.length)
			return false;
		for (int i = 0; i < state.length; i++) {
			if (state[i] < elements[2*i] || state[i] > elements[2*i+1])
				return false;
		}
		return true;
	}

	/**
	 * Check the possibility of an environmental state matched by the elements
	 * and the given state.
	 * 
	 * @param givenState
	 * @return boolean
	 */
	public boolean condOverlap(double[] otherCond) {
		if (elements.length != otherCond.length)
			return false;
		for (int i=0; i<elements.length / 2; i++) {
			if (otherCond[2*i] > elements[2*i+1] || elements[2*i] > otherCond[2*i+1])
				return false;
		}
		return true;
	}

	/**
	 * Counts the resemblance of the classifier's elements and the given
	 * elements.
	 * 
	 * @param givenCond
	 *            The given elements.
	 * @return Number of resemblance found.
	 */
	public int condResemblance(double[] otherCond) {
		if (elements.length != otherCond.length)
			return 0;
		int a = 0;
		for (int i=0; i < elements.length / 2; i++) {
			if ((otherCond[2*i] <= elements[2*i+1] && otherCond[2*i+1] >= elements[2*i]) || 
				(elements[2*i] <= otherCond[2*i+1] && elements[2*i+1] >= otherCond[2*i]))
				a++;
		}
		return a;
	}

	/**
	 * UNUSED in XCS-RC!!!
	 * Applies two point crossover and returns if the classifiers changed.
	 * 
	 * @see XCSConstants#pX
	 * @param cl
	 *            The second classifier for the crossover application.
	 *
	public boolean twoPointCrossover(XClassifier cl) {
		boolean changed = false;
		if (XCSConstants.random.nextDouble() < XCSConstants.pX) {
			int length = elements.length();
			int sep1 = (int) (XCSConstants.random.nextDouble() * (length));
			int sep2 = (int) (XCSConstants.random.nextDouble() * (length)) + 1;
			if (sep1 > sep2) {
				int help = sep1;
				sep1 = sep2;
				sep2 = help;
			} else if (sep1 == sep2) {
				sep2++;
			}
			char[] cond1 = elements.toCharArray();
			char[] cond2 = cl.elements.toCharArray();
			for (int i = sep1; i < sep2; i++) {
				if (cond1[i] != cond2[i]) {
					changed = true;
					char help = cond1[i];
					cond1[i] = cond2[i];
					cond2[i] = help;
				}
			}
			if (changed) {
				elements = new String(cond1);
				cl.elements = new String(cond2);
			}
		}
		return changed;
	}

	/**
	 * UNUSED in XCS-RC!!!
	 * 
	 * Applies a niche mutation to the classifier. This method calls
	 * mutateelements(state) and mutateAction(numberOfActions) and returns if
	 * at least one bit or the action was mutated.
	 * 
	 * @param state
	 *            The current situation/problem instance
	 * @param numberOfActions
	 *            The maximal number of actions possible in the environment.
	 *
	public boolean applyMutation(String state, int numberOfActions) {
		boolean changed = mutateelements(state);
		if (mutateAction(numberOfActions))
			changed = true;
		return changed;
	}

	/**
	 * UNUSED in XCS-RC!!!
	 * 
	 * Mutates the elements of the classifier. If one allele is mutated depends
	 * on the constant pM. This mutation is a niche mutation. It assures that
	 * the resulting classifier still matches the current situation.
	 * 
	 * @see XCSConstants#pM
	 * @param state
	 *            The current situation/problem instance.
	 *
	private boolean mutateelements(String state) {
		boolean changed = false;
		int condLength = elements.length();

		for (int i = 0; i < condLength; i++) {
			if (XCSConstants.random.nextDouble() < XCSConstants.pM) {
				char[] cond = elements.toCharArray();
				char[] stateC = state.toCharArray();
				changed = true;
				if (cond[i] == XCSConstants.dontCare) {
					cond[i] = stateC[i];
				} else {
					cond[i] = XCSConstants.dontCare;
				}
				elements = new String(cond);
			}
		}
		return changed;
	}

	/**
	 * UNUSED in XCS-RC!!!
	 * 
	 * Mutates the action of the classifier.
	 * 
	 * @see XCSConstants#pM
	 * @param numberOfActions
	 *            The number of actions/classifications possible in the
	 *            environment.
	 *
	private boolean mutateAction(int numberOfActions) {
		boolean changed = false;

		if (XCSConstants.random.nextDouble() < XCSConstants.pM) {
			int act = 0;
			do {
				act = (int) (XCSConstants.random.nextDouble() * numberOfActions);
			} while (act == action);
			action = act;
			changed = true;
		}
		return changed;
	}
	*/

	/**
	 * Returns if the two classifiers are identical in elements and action.
	 * 
	 * @param cl
	 *            The classifier to be compared.
	 */
	public boolean equals(XClassifier cl) {
		if (cl.elements.equals(elements)) {
			boolean identicAction = true;
			identicAction = identicAction && (action == cl.action);
			if (identicAction)
				return true;
		}

		return false;
	}

	/**
	 * Returns if the classifier subsumes cl.
	 * 
	 * @param The
	 *            new classifier that possibly is subsumed.
	 */
	public boolean subsumes(XClassifier cl) {
		if (cl.action == action)
			if (isSubsumer())
				if (isMoreGeneral(cl))
					return true;
		return false;
	}

	/**
	 * Returns if the classifier is a possible subsumer. It is affirmed if the
	 * classifier has a sufficient experience and if its reward prediction error
	 * is sufficiently low.
	 * 
	 * @see XCSConstants#theta_sub
	 * @see XCSConstants#epsilon_0
	 */
	public boolean isSubsumer() {
		if (experience > XCSConstants.theta_sub
				&& predictionError < (double) XCSConstants.epsilon_0)
			return true;
		return false;
	}

	/**
	 * Returns if the classifier is more general than cl. It is made sure that
	 * the classifier is indeed more general and not equally general as well as
	 * that the more specific classifier is completely included in the more
	 * general one (do not specify overlapping regions)
	 * 
	 * @param The classifier that is tested to be more specific.
	 */
	public boolean isMoreGeneral(XClassifier cl) {
		if (elements.length != cl.elements.length)
			return false;
		for (int i=0; i<elements.length/2; i++) {
			if (cl.elements[2*i] < elements[2*i] || cl.elements[2*i+1] > elements[2*i+1])
				return false;
		}
		return true;
	}

	/**
	 * Check whether the classifier is subsumable to the given elements.
	 * 
	 * @param givenCond
	 *            The given elements.
	 * @return true if the classifier is subsumable to the given elements.
	 */
	public boolean isSubsumableTo(double[] otherCond) {
		if (elements.length != otherCond.length)
			return false;
		for (int i=0; i<elements.length/2; i++) {
			if (otherCond[2*i] > elements[2*i] || otherCond[2*i+1] < elements[2*i+1])
				return false;
		}
		return true;
	}

	/**
	 * Returns the vote for deletion of the classifier.
	 * 
	 * @see XCSConstants#delta
	 * @see XCSConstants#theta_del
	 * @param meanFitness
	 *            The mean fitness in the population.
	 */
	public double getDelProp(double meanFitness) {
		if (fitness / numerosity >= XCSConstants.delta * meanFitness
				|| experience < XCSConstants.theta_del)
			return actionSetSize * numerosity;
		return actionSetSize * numerosity * meanFitness
				/ (fitness / numerosity);
	}

	/**
	 * Updates the prediction of the classifier according to P.
	 * 
	 * @see XCSConstants#beta
	 * @param P
	 *            The actual Q-payoff value (actual reward + max of predicted
	 *            reward in the following situation).
	 */
	public double updatePrediction(double P) {
		if ((double) experience < 1. / XCSConstants.beta) {
			prediction = (prediction * ((double) experience - 1.) + P)
					/ (double) experience;
		} else {
			prediction += XCSConstants.beta * (P - prediction);
		}
		return prediction * numerosity;
	}

	/**
	 * Updates the prediction error of the classifier according to P.
	 * 
	 * @see XCSConstants#beta
	 * @param P
	 *            The actual Q-payoff value (actual reward + max of predicted
	 *            reward in the following situation).
	 */
	public double updatePreError(double P) {
		if ((double) experience < 1. / XCSConstants.beta) {
			predictionError = (predictionError * ((double) experience - 1.) + Math
					.abs(P - prediction))
					/ (double) experience;
		} else {
			predictionError += XCSConstants.beta
					* (Math.abs(P - prediction) - predictionError);
		}
		return predictionError * numerosity;
	}

	/**
	 * Returns the accuracy of the classifier. The accuracy is determined from
	 * the prediction error of the classifier using Wilson's power function as
	 * published in 'Get Real! XCS with continuous-valued inputs' (1999)
	 * 
	 * @see XCSConstants#epsilon_0
	 * @see XCSConstants#alpha
	 * @see XCSConstants#nu
	 */
	public double getAccuracy() {
		double accuracy;

		if (predictionError <= (double) XCSConstants.epsilon_0) {
			accuracy = 1.;
		} else {
			accuracy = XCSConstants.alpha
					* Math.pow(predictionError / XCSConstants.epsilon_0,
							-XCSConstants.nu);
		}
		return accuracy;
	}

	/**
	 * Updates the fitness of the classifier according to the relative accuracy.
	 * 
	 * @see XCSConstants#beta
	 * @param accSum
	 *            The sum of all the accuracies in the action set
	 * @param accuracy
	 *            The accuracy of the classifier.
	 */
	public double updateFitness(double accSum, double accuracy) {
		fitness += XCSConstants.beta
				* ((accuracy * numerosity) / accSum - fitness);
		return fitness;
	}

	/**
	 * Updates the action set size.
	 * 
	 * @see XCSConstants#beta
	 * @param numeriositySum
	 *            The number of micro-classifiers in the population
	 */
	public double updateActionSetSize(double numerositySum) {
		if (experience < 1. / XCSConstants.beta) {
			actionSetSize = (actionSetSize * (double) (experience - 1) + numerositySum)
					/ (double) experience;
		} else {
			actionSetSize += XCSConstants.beta
					* (numerositySum - actionSetSize);
		}
		return actionSetSize * numerosity;
	}

	/**
	 * Returns the condition string, only for printout purposes.
	 */
	public String getCondition() {
		condition = "";
		
		for (int i=0; i<elements.length/2; i++) {
			condition += "["+elements[2*i]+";"+elements[2*i+1]+"]";
		}
		return condition;
	}

	/**
	 * Returns the number of elements of the classifier.
	 */
	public int getNumElements() {
		return elements.length/2;
	}

	/**
	 * Returns the elements of the classifier.
	 */
	public double[] getElements() {
		return elements;
	}

	/**
	 * Change the elements of the classifier.
	 */
	public void setElements(double[] newCond) {
		elements = newCond;
	}

	/**
	 * Returns the action of the classifier.
	 */
	public int getAction() {
		return action;
	}

	/**
	 * Change the elements of the classifier.
	 */
	public void setAction(int newAct) {
		action = newAct;
	}

	/**
	 * Returns the experience of the classifier.
	 */
	public int getExperience() {
		return experience;
	}

	public void setExperience(int exp) {
		experience = exp;
	}

	/**
	 * Increases the Experience of the classifier by one.
	 */
	public void increaseExperience() {
		experience++;
	}

	/**
	 * Returns the prediction of the classifier.
	 */
	public double getPrediction() {
		return prediction;
	}

	/**
	 * Sets the prediction of the classifier.
	 * 
	 * @param pre
	 *            The new prediction of the classifier.
	 */
	public void setPrediction(double pre) {
		prediction = pre;
	}

	/**
	 * Returns the prediction error of the classifier.
	 */
	public double getPredictionError() {
		return predictionError;
	}

	/**
	 * Sets the prediction error of the classifier.
	 * 
	 * @param predErr
	 *            The new prediction error of the classifier.
	 */
	public void setPredictionError(double predErr) {
		predictionError = predErr;
	}

	/**
	 * Returns the fitness of the classifier.
	 */
	public double getFitness() {
		return fitness;
	}

	/**
	 * Sets the fitness of the classifier.
	 * 
	 * @param fit
	 *            The new fitness of the classifier.
	 */
	public void setFitness(double fit) {
		fitness = fit;
	}

	/**
	 * Returns the numerosity of the classifier.
	 */
	public int getNumerosity() {
		return numerosity;
	}

	/**
	 * Adds to the numerosity of the classifier.
	 * 
	 * @param num
	 *            The added numerosity (can be negative!).
	 */
	public void addNumerosity(int num) {
		numerosity += num;
	}

	/**
	 * Returns the action set size of the classifier.
	 */
	public double getActionSetSize() {
		return actionSetSize;
	}

	public boolean inexp() {
		if (experience > 0)	return false;
		return true;
	}

	/**
	 * Change the disproving attribute of the classifier.
	 */
	public void setDisproving(int disp) {
		disproving = disp;
	}

	/**
	 * Returns the disproving attribute of the classifier.
	 */
	public int getDisproving() {
		return disproving;
	}

	/**
	 * Reset the disproving attribute of the classifier.
	 */
	public void incrementDisproving() {
		disproving++;
	}

	/**
	 * Prints the classifier to the control panel. The method prints elements
	 * action prediction predictionError fitness numerosity experience
	 * actionSetSize timeStamp.
	 */
	public void printXClassifier() {
		DecimalFormat report = new DecimalFormat("0.000");

		System.out.println(stringCondition() + ":" + action + " -> "
				+ report.format(prediction) + ";"
				+ report.format(predictionError) + ";" + report.format(fitness)
				+ ";" + numerosity + ";" + experience + ";"
				+ (float) actionSetSize);
	}

	/**
	 * Returns the string of the condition
	 */
	public String stringCondition() {
		double[] values = elements;
		
		boolean binaryInput = true;	
		for (int i=0;i<values.length;i++)
			if (values[i] != 0.0 && values[i] != 1.0) { 
				binaryInput=false;
				break;
			}
		
		String stringElements = (binaryInput)?"\'":"";
		for (int i=0;i<values.length/2;i++) {
			if (values[2*i] == values[2*i+1])
				stringElements += (binaryInput)?(int) values[2*i]:"[" + values[2*i] + "]";
			else 
				stringElements += (binaryInput)?"#":"[" + values[2*i] + "..." + values[2*i+1] + "]";
		}

		return stringElements;
	}

	/**
	 * Prints the classifier to the print writer (normally referencing a file).
	 * The method prints elements action prediction predictionError fitness
	 * numerosity experience actionSetSize timeStamp.
	 * 
	 * @param condLen
	 * 
	 * @param pW
	 *            The writer to which the classifier is written.
	 */
	public void printXClassifier(PrintWriter pW) {
		DecimalFormat report = new DecimalFormat("0.000");

		String actionString = "" + action;
		pW.println(stringCondition() + ";" + actionString + ";" + report.format(prediction) + ";" + report.format(predictionError)
			+ ";" + report.format(fitness) + ";" + numerosity + ";" + experience + ";" + disproving + ";" + (int) actionSetSize);
	}
}
