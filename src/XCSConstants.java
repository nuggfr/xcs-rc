
import java.io.Serializable;
import java.util.Random;

/**
 * This class provides all relevant learning parameters for the XCS as well as 
 * other experimental settings and flags. Most parameter-names are chosen similar to 
 * the 'An Algorithmic Description of XCS' ( Butz&Wilson, IlliGAL report 2000017). 
 *
 * @author Nugroho Fredivianus, based on Martin Butz
 * @version XCS-RC 2.0
 */

public class XCSConstants implements Serializable
{

    /**
     * Specifies the maximal number of micro-classifiers in the population.
     * In the multiplexer problem this value is set to 400, 800, 2000 in the 6, 11, 20 multiplexer resp..
     * In the Woods1 and Woods2 environment the parameter was set to 800. 
     */
    public static int maxPopSize=800;

    /**
     * The fall of rate in the fitness evaluation.
     */
    final public static double alpha=0.1;

    /**
     * The learning rate for updating fitness, prediction, prediction error, 
     * and action set size estimate in XCS's classifiers.
     */ 
    final public static double beta=0.15;

    /**
     * The discount rate in multi-step problems.
     */
    final public static double gamma=0.71;

    /**
     * The fraction of the mean fitness of the population below which the fitness of a classifier may be considered 
     * in its vote for deletion.
     */
    final public static double delta=0.1;

    /**
     * Specifies the exponent in the power function for the fitness evaluation.
     */
    final public static double nu=5.;
    
    /**
     * The error threshold under which the accuracy of a classifier is set to one.
     */
    final public static double epsilon_0=0.01;

    /**
     * Specified the threshold over which the fitness of a classifier may be considered in its deletion probability.
     */
    final public static int theta_del=25;

    /**
     * The experience of a classifier required to be a subsumer.
     */
    final public static int theta_sub=20;
  
    /**
     * The maximal number of steps executed in one trial in a multi-step problem.
     */
    final public static int teletransportation=50;
  
    /**
     * The initial prediction value when generating a new classifier (e.g in covering).
     */ 
    final public static double predictionIni=500.0;

    /**
     * The initial prediction error value when generating a new classifier (e.g in covering).
     */ 
    final public static double predictionErrorIni=0.0;

    /**
     * The initial prediction value when generating a new classifier (e.g in covering).
     */ 
    final public static double fitnessIni=10.0;

    /**
     * The don't care symbol (normally '#')
     */
    final public static char dontCare='#';

    /**
     * Random variable
     */
    final public static Random random = new Random();

    final public static double predTol=10;
    final public static double predErrTol=260.; //ENV 5., MPX 100.
    final public static int minExp=1; //ENV 1, MPX 1
    public static int Tcomb=50;
    public static int maxDispRate=0; // for outlier detection
    public static int deletion=0;
    
    /**
     * The default constructor.
     */
    public XCSConstants()//Default constructor
    {}

    /**
     * This method converts an array to an index
     * 
     * @param action
     * 
     * @see convertIndexToArray(int, int, int)
     */
    public static int convertArrayToIndex(int[] action){
    	int index = 0;
    	for (int i = 0; i < action.length; i++)
    		index = index + ((int)Math.pow(2,i))*action[i];
    	return index;
    }
    
    /**
     * This method converts an index to array
     * 
     * @param index index of the action
     * @param numberOfActions
     * @return corresponding action-array
     *
     *@see convertArrayToIndex(int[], int, int)
     */
    public static int[] convertIndexToArray(int index, int numberOfActions){
    	int a = -1;
    	do a++; while ((int)Math.pow(2, a) < numberOfActions);
    	int[] action = new int[a];
    	
    	for (int i = a - 1; i >= 0; i--){
    		action[i]= index /((int)Math.pow(numberOfActions,i));
    		index = index - (action[i] * ((int)Math.pow(numberOfActions,i)));
    	}
    	
    	return action;
    }

}
