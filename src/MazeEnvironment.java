
import java.io.Serializable;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Vector;

/**
 * This class implements a maze environment. 
 * It reads in a maze from a file. This file must contain the maze in a nxm matrix where each
 * entry specifies one distinct position in the maze. If the animat reaches an edge and moves out of the edge, 
 * it will reenter the maze from the other side (if not blocked). Obstacles are coded as 'O'or'0' and 'Q', empty positions as 
 * '.'or'*', and food as 'F' and 'G'. A two bit or three bit coding can be chosen for the coding of the perceptions. In the 
 * three bit coding the perceptions differentiate 'F' and 'G', and 'O'/'0' and 'Q' while they appear the same in the two 
 * bit coding. 
 * Eight movements are possible to the adjacent cells (if not blocked). The environment starts by positioning the animat
 * at a random position. It moves as long as no food position is reached. If a food position is reached, the environment
 * provides the payoff specified in maxPayoff. Also, the reset flag is set to true in this case.
 *
 * @author    Martin V. Butz
 * @version   XCSJava 1.0
 * @since     JDK1.1
 */
public class MazeEnvironment implements Environment, Serializable
{
    /*###################---- Constants which are preset ----###################*/
    
    /**
     * The number of perceptions of the animat.
     */
    private final int conLength=8;

    /**
     * The number of generally possible movements.
     */
    private final int nrActions=8;

    /**
     * The payoff provided at a food position.
     */
    private final int maxPayoff=1000;
    
    /**
     * The binary code of an empty position (000).
     */
    private final char[] freeAtt = {'0','0','0'};
    
    /**
     * The binary code of a food-F position (110).
     */
    private final char[] foodF = {'1','1','0'};
    
    /**
     * The binary code of a food-G position (111).
     */
    private final char[] foodG = {'1','1','1'};
    
    /**
     * The binary code of an O-obstacle position (010).
     */
    private final char[] obstacleO = {'0','1','0'};
    
    /**
     * The binary code of a Q-obstacle position (011).
     */
    private final char[] obstacleQ = {'0','1','1'};



    /*###################---- These variables are set in the constructor ----###################*/

    /** 
     * The attribute Length specifies the number of bits that code each perceived position in the maze.
     * It can be set to 2 or 3.
     */
    private int attributeLength;

    /**
     * The matrix that codes the maze (in binary)
     */
    private char[][] maze;

    /**
     * The size of the maze in positions.
     */
    private int xsize,ysize;

    /**
     * The current position of the animat.
     */ 
    private int xcurrent, ycurrent;

    /**
     * Flag which is set to true when food was reached.
     */
    private boolean reset;

    /**
     * The constructor reads in the specified maze file and sets its global parameters accordingly.
     *
     * @param inFileString must specify the file name where the maze is coded
     * @param attLength specifies the number of bits that specify one attribute (either two or three).
     * If wrongly specified, then it is set to two.
     */
    public MazeEnvironment(String inFileString, int attLength)
    {
	if(attLength==3)
	    attributeLength=3;
	else
	    attributeLength=2;
	
	FileReader fr=null;
	BufferedReader br=null;
	Vector mazeLines=new Vector();
	try{
	    fr=new FileReader(inFileString);
	    br=new BufferedReader(fr);
	    xsize=0;
	    ysize=0;
	    while(br.ready()){
		String in=br.readLine();
		if(xsize==0)
		    xsize=in.length();
		char[] oneLine=new char[xsize*attributeLength];
		for(int i=0; i<xsize; i++){
		    char att=in.charAt(i);
		    switch(att){
		    case '.': case '*': /* Intentionally to sum both cases! */
			for(int j=0; j<attributeLength; j++)
			    oneLine[i*attributeLength + j]=freeAtt[j];
			break;
		    case 'F':
			for(int j=0; j<attributeLength; j++)
			    oneLine[i*attributeLength + j]=foodF[j];
			break;
		    case 'G':
			for(int j=0; j<attributeLength; j++)
			    oneLine[i*attributeLength + j]=foodG[j];
			break;
		    case 'O':case '0': /* Intentionally to sum both cases */
			for(int j=0; j<attributeLength; j++)
			    oneLine[i*attributeLength + j]=obstacleO[j];
			break;
		    case 'Q':
			for(int j=0; j<attributeLength; j++)
			    oneLine[i*attributeLength + j]=obstacleQ[j];
			break;
		    default:
			System.out.println("Unknown Character: "+att);
			System.exit(0);
			break;
		    }
		}
		mazeLines.addElement(oneLine);
	    }
	}catch(Exception e){System.out.println("Could not Read File!"+e);}
	ysize=mazeLines.size();
	maze=new char[xsize*attributeLength][ysize];

	for(int i=0; i<ysize; i++){
	    char[] line = (char [])mazeLines.elementAt(i);
	    for(int j=0; j<line.length; j++){
		maze[j][i]=line[j];
	    }
	}
	reset=false;
	setRandomPosition();
    }

    /**
     * Sets the animat to a randomly selected empty position.
     */
    private void setRandomPosition()
    {
	do{
	    xcurrent=(int)(XCSConstants.random.nextDouble()*xsize);
	    ycurrent=(int)(XCSConstants.random.nextDouble()*ysize);
	}while(maze[xcurrent*attributeLength][ycurrent]!=freeAtt[0] || maze[xcurrent*attributeLength+1][ycurrent]!=freeAtt[1]);     
    }

    /**
     * Returns a String of the perceptions in the current position.
     */
    private double[] getPerceptions()
    {
	char[] perc=new char[getConditionLength()];
	for(int i=0; i<conLength; i++){
	    for(int j=0; j<attributeLength; j++){
		perc[i*attributeLength + j] = 
		    maze[j + attributeLength * 
			(((xcurrent + (int)(((Math.ceil((double)(i-3)/4.)*2)-1)*(-1)* Math.ceil((double)(i%4)/4.)))+xsize)%xsize)]
		    [((ycurrent + (int)(((Math.ceil(Math.floor((double)(i%7)/2.)/4.)*2)-1.)*Math.ceil((double)((i+2)%4)/4.)))+ysize)%ysize];  
	    }
	}

	double[] perception = new double[perc.length];
	
	for (int i=0; i<perc.length; i++)
		perception[i] = (double) (perc[i] - 48);
	
	return perception;
    }

    /**
     * Resets the animat to a random empty position and returns the perceptions in this position.
     */
    public double[] resetState()
    {
	setRandomPosition();
	reset=false;
	return getPerceptions();
    }
  
    /**
     * Returns the current perceptions.
     */
    public double[] getCurrentState()
    {
	return getPerceptions();
    }
  
    /**
     * Executes the specified action in the environment and returns possible payoff.
     *
     * @param action The action to be executed.
     */
    public double executeAction(int action)
    {
	if(action<0 || action>7){
	    System.out.println("Not an action!");
	    System.exit(0);
	}
	/* Test if new position is empty!!! */
	int xaim=(( xcurrent + (int)(((Math.ceil((double)(action-3)/4.)*2)-1)*(-1)* Math.ceil((double)(action%4)/4.)))+xsize)%xsize;
	int yaim=((ycurrent + (int)(((Math.ceil(Math.floor((double)(action%7)/2.)/4.)*2)-1.)*Math.ceil((double)((action+2)%4)/4.)))
		  +ysize) % ysize; 
	if(maze[xaim*attributeLength][yaim]!=obstacleO[0] || maze[1+ xaim*attributeLength][yaim]!=obstacleO[1]){
	    xcurrent=xaim;
	    ycurrent=yaim;
	    if(maze[xcurrent*attributeLength][ycurrent]==foodF[0] && maze[1+ xcurrent*attributeLength][ycurrent]==foodF[1]){
		reset=true;
		return maxPayoff;
	    }
	    return 0.;
	}
	return 0.;
    }

    /**
     * Returns always false since there is no real correct or wrong action in the maze environment.
     */
    public boolean wasCorrect()
    {
	return false;/* No correct or wrong action in this environment */
    }

    /**
     * Returns true since any maze is a multi-step environment.
     */
    public boolean isMultiStepProblem()
    {
	return true;
    }

    /** 
     * Returns if the animat should be reseted. 
     * The reset flag is set to true once the animat reached a food position.
     */
    public boolean doReset()
    {
	return reset;
    }

    /**
     * Returns the length of the perceptions.
     */
    public int getConditionLength()
    {
	return conLength * attributeLength;
    }

    /**
     * Returns the maximal Payoff in the maze.
     */
    public int getMaxPayoff()
    {
	return maxPayoff;
    }

    /**
     * Returns the number of actions possible in the maze environment.
     */
    public int getNrActions()
    {
	return nrActions;
    }
}
