import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Random;

/**
 * This class is the XCS itself. It stores the population and the posed problem.
 * The class provides methods for the main learning cycles in XCS distinguishing
 * between single-step and multi-step problems as well as exploration vs.
 * exploitation trials. Moreover, it handles the performance evaluation.
 * 
 * @author Martin V. Butz
 * @version XCSJava 1.0
 * @since JDK1.1
 */
public class ENV {

	public static void main(String args[]) {
		DecimalFormat df = new DecimalFormat("0.000");
		MazeEnvironment env = null;
		int maxTrials = 4001;
		int interval = 50;
		File out = null;
		double[] perf = new double[maxTrials / interval + 1];
		double[] serr = new double[maxTrials / interval + 1];
		double[] popSize = new double[maxTrials / interval + 1];

		boolean combineMode = true;
		int Tcomb = (combineMode)? (2*50):0;
		boolean debugMode = true;
		Random rSeed = new Random();
		int totRS = 20;

		// The available environments so far: Woods1, Woods2, Maze4, Maze5, and Maze6
		String enviFileString = "Environments\\Maze4.txt";
		String outFile = "ENV_" + enviFileString.substring(enviFileString.lastIndexOf('\\') + 1, enviFileString.lastIndexOf('.')) + ".csv";
		int bit = 3;
		int maxPopSize = 800;

		System.out.println("Construct maze environment with maze coded in " + enviFileString + " coding each feature with " + bit + " bits");

		FileWriter fW = null;
		BufferedWriter bW = null;
		PrintWriter pW = null;
		try {
			fW = new FileWriter(outFile);
			bW = new BufferedWriter(fW);
			pW = new PrintWriter(bW);
		} catch (Exception ex) {
			System.out.println("Mistake in create file Writers" + ex);
		}

		for (int i = 0; i <= maxTrials / interval; i++) {
			perf[i] = 0.;
			serr[i] = 0.;
			popSize[i] = 0.;
		}

		env = new MazeEnvironment(enviFileString, bit);

		for (int rs = 1; rs <= totRS; rs++) {
			if (debugMode) {
				String filename = "ENV_Debug_";
				if (rs<10) filename += "0";
				out = new File(filename + rs + ".csv");				
			}
			rSeed.setSeed(rs * 9);

			XCSManager xcsLearn = new XCSManager(env, maxPopSize, Tcomb, debugMode, out, rs * 99);

			int explore = 0, exploreStepCounter = 0;
			int[] stepsToFood = new int[interval];
			double[] sysError = new double[interval];

			for (int exploreTrialC = 0; exploreTrialC <= maxTrials; exploreTrialC += explore) {
				if (exploreTrialC == 0) {
					System.out.println("\n=====\nSIMULATION " + rs);					
					System.out.println("Trials;Perf;SysErr;PopSize;Prediction");					
				}
				explore = (explore + 1) % 2;

				double[] state = env.resetState();
				xcsLearn.doOneMultiStep(state, explore, stepsToFood, sysError, exploreTrialC, exploreStepCounter);

				if (exploreTrialC % interval == 0 && explore == 0) {
					double perfNow = 0.;
					double serrNow = 0.;
					for (int i = 0; i < interval; i++) {
						perfNow += stepsToFood[i];
						serrNow += sysError[i];
					}
					perf[exploreTrialC / interval] += perfNow / interval;
					serr[exploreTrialC / interval] += serrNow / interval;
					popSize[exploreTrialC / interval] += xcsLearn.getPopSize();
					System.out.println(exploreTrialC + ";"
							+ df.format((float) perfNow / interval) + ";"
							+ df.format((float) serrNow) + ";"
							+ (xcsLearn.getPopSize()));
					if (debugMode)
						xcsLearn.writeDebug("Trials " + exploreTrialC);
				}
			}

			if (debugMode)
				xcsLearn.writeDebug("Final Pop");
		}

		pW.println("Trials;Performance;SysError;PopSize;Prediction");
		for (int i = 0; i <= maxTrials / interval; i++)
			pW.println(i * interval + ";" + df.format((float) perf[i] / totRS)
					+ ";" + df.format((float) serr[i] / totRS) + ";"
					+ df.format(popSize[i] / totRS));

		try {
			pW.flush();
			bW.flush();
			fW.flush();
			fW.close();
		} catch (Exception ex) {
			System.out.println("Mistake in closing the file writer!" + ex);
		}

		return;
	}

}
