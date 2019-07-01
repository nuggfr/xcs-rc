import java.io.*;
import java.text.DecimalFormat;
import java.util.Random;

public class MPX {

	private static FileWriter corr = null, classf = null, dele = null;

	public static void main(String[] args) {

		Random rSeed = new Random();
		DecimalFormat df = new DecimalFormat("0.000");
		int numberOfActions = 2;
		boolean debugMode = true;

		File outputFile = null;

		/*
		 * File parameters
		 * 
		 * addressBits = number of address bits: 2 for MP6, 3 for MP11 etc.
		 * startRS = starting random seed
		 * trials = number of learning cycles to be performed
		 * 
		 * inputLength = number of input bits, following address bits
		 * totalSims = number of simulations
		 * 
		 */
		int addressBits = (args.length>0) ? Integer.parseInt(args[0]):5;
		//int addedBits = (args.length > 1) ? Integer.parseInt(args[1]):4; // only for filtering mode
		int inputLength = addressBits + (int) Math.pow(2, addressBits);
		int startRS = (args.length > 2) ? Integer.parseInt(args[2]):1;
		int totalSims = (args.length > 2) ? startRS:20;
		int trials  = (args.length > 3) ? Integer.parseInt(args[3]):0;
		String name = (args.length > 2) ? "_" + args[2] : "";
		int expNum  = totalSims - startRS + 1;

		// initialize logfile
		File file_perf = new File("MPX_Perf_" + inputLength + name + ".csv");
		try {
			file_perf.delete();
			corr = new FileWriter(file_perf, true);
			file_perf.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		File file_classf = new File("MPX_Classf_" + inputLength + name + ".csv");
		try {
			file_classf.delete();
			classf = new FileWriter(file_classf, true);
			file_classf.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		File file_del = new File("MPX_Del_" + inputLength + name + ".csv");
		try {
			file_del.delete();
			dele = new FileWriter(file_del, true);
			file_del.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}			

		// initialize XCS learning system
		XCSManager xcsLearn = null;

		// set maximum number of trials
		//				MP	0 	 1		2	 	3		4
		int[] maxTrials = { 0, 1000, 10000, 50000, 200000, 100000 };
		int maxTrial = (trials == 0)?maxTrials[addressBits]:trials;
		
		// set input mode
		int inputMode = 0; // 0 binary, 1 real
		
		// set population size limit
		//					MP 0  1	  2	 	3	4
		int[] maxPopBinary = { 0, 0, 400, 800, 1000, 10000 }; // values commonly used for binary input
		int[] maxPopReal  = { 0, 0, 2000, 4000}; // values commonly used for real-valued input
		int[] maxPopSize = (inputMode == 0)?maxPopBinary:maxPopReal;

		// set rule combining mode
		boolean combineMode = true;

		// set combining period
		//			MP	 0  1	  2	  3		4
		int[] Tcombs = { 0, 40, 100, 200, 500, 5000 };
		int Tcomb = (combineMode) ? Tcombs[addressBits]:0; // performed after Tcomb/2 exploit trials

		// set filtering mode
		//boolean filterMode = false; // NOTE: in this release no filtering capability is added
		
		// set outlier detection
		boolean outlierDetection = false;

		// initialize correctness rate for the first interval
		int intervalCorrect = 0;

		// set sliding window value for recording interval
		//					MP	0	1	2	 3		4
		//int[] slidingWindows = { 0, 40, 100, 200, 500, 2000 };
		int slidingWindow = Tcomb; //slidingWindows[addressBits];

		// set max disproval rate
		XCSConstants.maxDispRate = (outlierDetection)?XCSConstants.maxDispRate:0;
		
		// set filtering period (will be multiplied by combining period)
		// int Tfil = (filterMode) ? 2:0;
		
		String title = "Trials";
		int[][] correctness = new int[totalSims][maxTrial / slidingWindow + 1];
		int[][] numOfClassf = new int[totalSims][maxTrial / slidingWindow + 1];
		int[][] deletions = new int[totalSims][maxTrial / slidingWindow + 1];

		if (args.length == 0)
			title += ";AVE";
		int n = 20;

		System.out.println("MP"+inputLength+"; N="+maxPopSize[addressBits]+"; Tcomb="+Tcomb);
		System.out.println("No. Random seed; overall correctness rate; final population size; deletions; time (ms)");

		long[] timeReq = new long[totalSims+1];
		long start_time = System.nanoTime();

		// loop number of simulations		
		for (int rs = 1; rs <= totalSims; rs++) {
			correctness[rs - 1][0] = 0;
			numOfClassf[rs - 1][0] = 0;
			deletions[rs - 1][0] = 0;

			if (debugMode) {
				String filename = "MPX_Debug_";
				if (rs<10) filename += "0";
				outputFile = new File(filename + rs + ".csv");				
			}

			// activate XCS learning system
			xcsLearn = new XCSManager(numberOfActions, maxPopSize[addressBits], Tcomb, debugMode, outputFile, rs * n);

			double[] state = new double[inputLength];
			String stringState = "";
			int rand = (int) (Math.random() * 10000);
			rSeed.setSeed(rs);
			double reward = 0.;
			int simCorrect = 0;
			int action = 0;
			boolean explore = false;
			XCSConstants.deletion=0;

			// learning cycle starts here
			
			for (int trial = 0; trial <= maxTrial + 1; trial++) {
				explore = (trial % 2 == 0);

				// form input
				stringState = "";
				if (inputMode == 0) {
					// binary
					for (int i=0; i<inputLength; i++) {
						state[i] = rSeed.nextInt(2);
						stringState += (int) state[i];
					}					
				} else
				if (inputMode == 1) {
					// real
					for (int i=0; i<inputLength; i++) {
						state[i] = (double) Math.round(1000 * rSeed.nextDouble()) / 1000;
						stringState += state[i];
						if (i<inputLength-1) stringState +=";";
					}					
				}
				
				//System.out.println(stringState);

				// feed to XCS
				start_time = System.nanoTime();
				action = xcsLearn.nextAction(stringState, (explore)?1:0);
				timeReq[rs] += System.nanoTime() - start_time;

				// determine expected output
				int pos = 0;
				for (int i=0; i<addressBits; i++)
					pos += Math.round(state[addressBits-i-1]) * Math.pow(2,i);
				int expectedOutput = (int) Math.round(state[pos + addressBits]);
				
				// judge output and update population
				reward = 0;
				if (action == expectedOutput) {
					reward = 1000;
					if (!explore) {
						intervalCorrect++;
						simCorrect++;
					}
				}
				start_time = System.nanoTime();
				xcsLearn.updateActionSet(reward);
				timeReq[rs] += System.nanoTime() - start_time;

				// record result
				if ((trial+1) % slidingWindow == 0) {
					int popsize = xcsLearn.getPopSize();
					correctness[rs - startRS][trial / slidingWindow+1] += intervalCorrect;
					numOfClassf[rs - startRS][trial / slidingWindow+1] += popsize;
					deletions[rs - startRS][trial / slidingWindow+1] += XCSConstants.deletion;
					
					System.out.println("Trial #"+(trial+1)+": Perf "+(float)2*intervalCorrect/slidingWindow+"; PopSize "+popsize);
					xcsLearn.writeDebug("Trial #" + (trial+1));
					intervalCorrect = 0;
				}

				/* test input and expected output
				String display = "";
				if (inputMode == 0) {
					for (int i=0;i<inputLength;i++) { 
						display += Math.round(state[i]);
						if (i+1 == addressBits) display += " | ";
						if (i > addressBits && (i+1-addressBits) % 4 == 0) display += " ";
					}
				} else
				if (inputMode == 1) {
					display = df.format((double) state[0]);
					for (int i=1;i<inputLength;i++) display += ";" + df.format((double) state[i]);
					display += " ";
				}
				display += ": " + expectedOutput;
				System.out.println(display);
				//*/

			}

			// record final population
			System.out.println("SIM "+rs + ". " + rand + ";"
					+ df.format((double) simCorrect / (maxTrial / 2)) + ";"
					+ xcsLearn.getPopSize() + ";" + XCSConstants.deletion + ";"
					+ Math.round(timeReq[rs]/1e6) + "\n");
			if (debugMode)
				xcsLearn.writeDebug("Final Pop");
		}

		double difference = 0;
		for (int i=startRS; i <= totalSims; i++) difference += timeReq[i]/1e6;
		
		System.out.println("MP"+inputLength+"; N="+maxPopSize[addressBits]+"; Tcomb="+Tcomb);
		System.out.println("Average time in ms: " + Math.round(difference / totalSims));

		for (int a = 0; a < expNum; a++)
			title += ";RS-" + (a + startRS);
		try {
			corr.append(title + "\r\n");
			for (int b = 0; b <= maxTrial / slidingWindow; b++) {
				String values = (b * slidingWindow) + ";";
				double perf = 0;
				for (int a = 0; a < expNum; a++)
					perf += correctness[a][b];
				if (args.length == 0)
					values += df.format(perf / (slidingWindow * expNum / 2)) + ";";
				for (int a = 0; a < expNum; a++)
					values += df.format((double) correctness[a][b] / (slidingWindow / 2)) + ";";
				corr.append(values + "\r\n");
			}
			corr.flush();
		} catch (IOException e) {
			System.err.println("Error writing to file!");
		}

		try {
			classf.append(title + "\r\n");
			for (int b = 0; b <= maxTrial / slidingWindow; b++) {
				String values = (b * slidingWindow) + ";";
				double clas = 0;
				for (int a = 0; a < expNum; a++)
					clas += numOfClassf[a][b];
				if (args.length == 0)
					values += df.format(clas / expNum) + ";";
				for (int a = 0; a < expNum; a++)
					values += df.format((double) numOfClassf[a][b]) + ";";
				classf.append(values + "\r\n");
			}
			classf.flush();
		} catch (IOException e) {
			System.err.println("Error writing to file!");
		}

		try {
			dele.append(title + "\r\n");
			for (int b = 0; b <= maxTrial / slidingWindow; b++) {
				String values = (b * slidingWindow) + ";";
				double clas = 0;
				for (int a = 0; a < expNum; a++)
					clas += deletions[a][b];
				if (args.length == 0)
					values += df.format(clas / expNum) + ";";
				for (int a = 0; a < expNum; a++)
					values += df.format((double) deletions[a][b]) + ";";
				dele.append(values + "\r\n");
			}
			dele.flush();
		} catch (IOException e) {
			System.err.println("Error writing to file!");
		}

	}
}
