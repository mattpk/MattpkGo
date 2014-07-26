import java.io.*;
import java.util.*;

// This is a go engine that follows basic GTP 2 specifications.
// Configured to be run with kgsGtp.
// Made by Matthew Chung
// note: 0 is black, 1 is white always EXCEPT for a single exception on the int[][] board, where it is 1 and 2.
class MattpkGo
{
	// engine variables
	private static final int PROTOCOL_VERSION = 2;
	private static final String ENGINE_NAME = "MattpkGo";
	private static final String ENGINE_VERSION = "1.0";
	private static final String KNOWN_COMMANDS = "protocol_version name version known_command list_commands quit boardsize clear_board komi play genmove";

	private static final String letters = "ABCDEFGHJKLMNOPQRSTUVWXYZ"; // 'I' left out on purpose according to specifications

	private int id = -1;
	private String commandName = "";
	private String[] arguments = new String[0];

	private boolean running = false;

	// Go variables

	int boardsize = 19; // default
	int [][] board; // set up in row, col format, ex. A11 -> board[10][0]
	int[] captures; // 0  is black, 1 is white

	float komi = Float.parseFloat("5.5");

	int[] koPosition = new int[]{-1,-1}; // the place that you cannot play if you are capturing exactly 1 stone by playing there

	private void run()
	{
		Scanner in = new Scanner (System.in);
		while (true)
		{
			if (!running) break;
			// Getting a command
			String take = in.nextLine();
			String[] parts = take.split(" ");
			// If empty, skip
			if (parts.length == 0) continue;
			// If starts with id, save it
			// -1 is none
			int partPointer = 0;
			try
			{
				int saveInt = Integer.parseInt(parts[partPointer]);
				id = saveInt;
				partPointer++;
			} catch (NumberFormatException e)
			{
				id = -1;
			}

			commandName = parts[partPointer];
			arguments = new String[0];
			partPointer++;
			if (parts.length > partPointer)
			{
				arguments = new String[parts.length-partPointer];
				for (int x = partPointer; x < parts.length; x++)
				{
					arguments[x-partPointer] = parts[x];
				}
			}
			// Result variables: int id; String commandName; String[] arguments

			// Testing Input
			/*
			System.out.println("Id: " + id);
			System.out.println("Command Name: " + commandName);
			for (int x =0; x < arguments.length; x++)
			{
				System.out.println ("Argument " + (x+1) + ": " + arguments[x]);
			}
			*/

			// Analyzing response

			switch (commandName) {
				case "protocol_version":
					protocolVersion();
					break;
				case "name":
					name();
					break;
				case "version":
					version();
					break;
				case "known_command":
					knownCommand(arguments);
					break;
				case "list_commands":
					listCommands();
					break;
				case "quit":
					quit();
					break;
				case "boardsize":
					boardsize(arguments);
					break;
				case "clear_board":
					clearBoard();
					break;
				case "komi":
					komi(arguments);
					break;
				case "play":
					play(arguments);
					break;
				case "genmove":
					genmove(arguments);
					break;
				default:
					errorResponse("Unknown Command.");
					break;
			}


		} // End while loop
	}

	private void protocolVersion()
	{
		successResponse(""+PROTOCOL_VERSION);
	}

	private void name()
	{
		successResponse(ENGINE_NAME);
	}

	private void version()
	{
		successResponse(ENGINE_VERSION);
	}

	private void knownCommand(String[] arguments)
	{
		if (arguments.length != 1)
		{
			invalidSyntaxErrorResponse();
			return;
		}
		if (KNOWN_COMMANDS.contains(arguments[0]))
			successResponse("true");
		else successResponse("false");
	}

	private void listCommands() {
		String[] commands = KNOWN_COMMANDS.split(" ");

		String msg = "=";
		if (id != -1)
			msg += id;
		System.out.print(msg + " ");
		for (int x =0; x < commands.length; x++)
		{
			System.out.println(commands[x]);
		}
		System.out.println();
	}

	private void quit() {
		successResponse();
		running = false;
	}

	private void boardsize(String[] arguments) {
		if (arguments.length != 1)
		{
			invalidSyntaxErrorResponse();
			return;
		}
		try
		{
			boardsize = Integer.parseInt(arguments[0]);
		}
		catch (NumberFormatException e)
		{
			invalidSyntaxErrorResponse();
		}
		successResponse();
	}
	
	private void clearBoard() {
		setUpBoard();
		successResponse();
	}

	private void komi(String[] arguments) {
		if (arguments.length != 1)
		{
			invalidSyntaxErrorResponse();
			return;
		}
		try {
		komi = Float.parseFloat(arguments[0]);
		}
		catch (NumberFormatException e)
		{
			invalidSyntaxErrorResponse();
		}
		successResponse();
	}

	private boolean playNoSuccessConfirm (String[] arguments) {
		if (arguments.length != 2)
		{
			invalidSyntaxErrorResponse();
			return false;
		}



		int color = processColor(arguments[0]);
		if (color == -1 || arguments[1].length() < 2 ||  arguments[1].length() > 4) {
			invalidSyntaxErrorResponse();
			return false;
		}
		
		if (arguments[1].toLowerCase().equals("pass")) {
			int[] koPosition = new int[]{-1,-1}; // reset ko
			return true;
		}
		
		int col = letters.indexOf(arguments[1].substring(0,1).toUpperCase());
		int row = -1;
		try
		{
			row = Integer.parseInt(arguments[1].substring(1))-1;
		}
		catch (NumberFormatException e)
		{
			invalidSyntaxErrorResponse();
			return false;
		}

		if (legalMove(color,row,col))
		{
			// make the move
			// move is made in numKilledByMove

			// Alright move is legit, now kill pieces if necessary
			int sum = numKilledByMove(color,row,col);
			captures[color] += sum;

			// put ko code kere
			int koRow = -1;
			int koCol = -1;
			if (sum == 1)
			{
				if (row < boardsize-1 && board[row+1][col] == 0)
				{
					koRow = row+1;
					koCol = col;
				}
				if (row > 0 && board[row-1][col] == 0)
				{

					if (koRow == -1 && koCol == -1)
					{
						koRow = row-1;
						koCol = col;
					}
					else
					{
						koRow = -2;
						koCol = -2;	
					}
				}
				if (col < boardsize-1 && board[row][col+1] == 0)
				{
					if (koRow == -1 && koCol == -1)
					{
						koRow = row;
						koCol = col+1;
					}
					else
					{
						koRow = -2;
						koCol = -2;	
					}
				}
				if (col > 0 && board[row][col-1] == 0)
				{
					if (koRow == -1 && koCol == -1)
					{
						koRow = row;
						koCol = col-1;
					}
					else
					{
						koRow = -2;
						koCol = -2;	
					}
				}
			}
			if (koRow >= 0 && koCol >= 0)
			{
				koPosition = new int[]{koRow,koCol};
			}
			else
				koPosition = new int[]{-1,-1};

			return true;
		}
		else
		{
			errorResponse("illegal move");
		}
		return false;
	}

	private void play (String[] arguments) {
		boolean result = playNoSuccessConfirm(arguments);
		if (result)
			successResponse();
	}

	private int numKilledByMoveUndo(int color,int row,int col)
	{
		int[][] saveBoard = new int[boardsize][boardsize];
		for (int x =0; x< boardsize; x++)
			for (int y=0; y < boardsize;y++)
				saveBoard[x][y] = board[x][y];

		int sum = numKilledByMove(color,row,col);

		board = new int[boardsize][boardsize];
		for (int x =0; x< boardsize; x++)
			for (int y=0; y < boardsize;y++)
				board[x][y] = saveBoard[x][y];

		return sum;


	}

	//Also kills the piece.
	private int numKilledByMove(int color,int row,int col)
	{
		board[row][col] = color+1;
		int sum =0;
		if (row < boardsize-1 && !isAlive(row+1,col))
		{
			sum += killGroup(opp(color),row+1,col);
		}
		if (row > 0 && !isAlive(row-1,col))
		{
			sum += killGroup(opp(color),row-1,col);
		}
		if (col < boardsize-1 && !isAlive(row,col+1))
		{
			sum += killGroup(opp(color),row,col+1);
		}
		if (col > 0 && !isAlive(row,col-1))
		{
			sum += killGroup(opp(color),row,col-1);
		}
		return sum;
	}

	private int processColor(String text)
	{
		text = text.toLowerCase();
		if (text.equals("b") || text.equals("black")) return 0;
		if (text.equals("w") || text.equals("white")) return 1;
		return -1;
	}

	private void genmove(String[] arguments) {
		if (arguments.length != 1) {
			invalidSyntaxErrorResponse();
			return;
		}
		int color = processColor(arguments[0]);
		if (color == -1)
		{
			invalidSyntaxErrorResponse();
			return;
		}
		int[] coord = new int[]{0,0};
		for (int x =0; x< 15; x++)
		{
			if (x == 14)
			{
				successResponse("resign");
				play(new String[]{"resign"});
				return;
			}
			int row = randInt(0,boardsize-1);
			int col = randInt(0,boardsize-1);
			if (legalMove(color,row,col))
			{
				coord = new int[]{row,col};
				break;
			}
		}
		successResponse(coordToVertex(coord));
		playNoSuccessConfirm(new String[]{arguments[0],coordToVertex(coord)});
	}

	// Converts a coordinate to vertex form (4,4 to C3)
	private String coordToVertex(int[] coord)
	{
		return letters.charAt(coord[1]) +""+(coord[0]+1);
	}

	private void successResponse(String response)
	{
		String msg = "=";
		if (id != -1)
			msg += id;
		System.out.println(msg + " " + response);
		System.out.println();


		// Debug (Draws the map)
		/*
		for (int x = boardsize-1; x>=0; x--) // x is col
		{
			for (int y =0; y < boardsize; y++) // y is row
			{
				System.out.print(board[x][y]);
			}
			System.out.println();
		}
		*/
	}
	private void successResponse()
	{
		successResponse("");
	}

	private void errorResponse(String errorMessage)
	{
		String msg = "?";
		if (id != -1)
			msg += id;
		System.out.println(msg + " " + errorMessage);
		System.out.println();
	}
	private void errorResponse()
	{
		errorResponse("");
	}
	private void invalidSyntaxErrorResponse()
	{
		errorResponse("Invalid Syntax");
	}

	private void setUpBoard()
	{
		board = new int[boardsize][boardsize]; // 0 is empty, 1 is black, 2 is white
		for (int x =0; x< boardsize;x++)
			for (int y =0; y < boardsize;y++)
				board[x][y] = 0;
		captures = new int[2]; // index 0 is black index 1 is white
		captures[0] = 0;
		captures[1] = 0;
		int[] koPosition = new int[]{-1,-1};
	}

	// Returns whether or not a move is legal.
	private boolean legalMove(int color, int row, int col)
	{
		// Checks boundaries
		if ((row >= boardsize || col >= boardsize)|| row < 0 || col < 0) return false;
		// Checks that it is unoccupied
		if (board[row][col] != 0)
				return false;
		// Checks for ko
		if (!(koPosition[0] == row && koPosition[1] == col && numKilledByMoveUndo(color,row,col) == 1)) // is not ko
		{
			board[row][col] = (color+1);
			if (!((row < boardsize-1 && !isAlive(opp(color),row+1,col)) || (row > 0 && !isAlive(opp(color),row-1,col)) || (col < boardsize-1 && !isAlive(opp(color),row,col+1)) || (col > 0 && !isAlive(opp(color),row,col-1))))
			{
				// Checks for self-kill
				boolean alive = isAlive(row,col);
				if (!alive)
				{
		    		board[row][col] = 0;
					return false;
				}
		    }
		    board[row][col] = 0;
		    return true;
		}
		return false;
	}

	// Class used to transverse board in the bfs
	private class Node
	{	
		public int row;
		public int col;
		public ArrayList<int[]> alreadyVisited;
		public Node(int row, int col, ArrayList<int[]> alreadyVisited) {
			this.row = row;
			this.col = col;
			this.alreadyVisited = alreadyVisited;
		}
	}

	// Does a breadth-first search to check if a piece has any liberties connected to it.
	private boolean isAlive(int row, int col)
	{
		int color = board[row][col]-1;
		return isAlive(color, row, col);
	}

	private boolean isAlive(int color, int row, int col)
	{
		if (color != board[row][col]-1) return true;
		if (color == -1) return true; // empty
		ArrayDeque<Node> queue = new ArrayDeque<Node>();
		queue.add(new Node(row,col,new ArrayList<int[]>()));
		outer:
		while (!queue.isEmpty())
		{
			Node node = queue.remove();
			int nodeRow = node.row;
			int nodeCol = node.col;
			ArrayList<int[]> av = node.alreadyVisited;
			// base case: check if already visited
			ArrayList<int[]> newAv = new ArrayList<int[]>();
			for (int x= 0; x< av.size(); x++)
			{
				if (av.get(x)[0] == nodeRow && av.get(x)[1] == nodeCol)
				{
					continue outer; // Already visited
				}
				newAv.add(av.get(x));
			}
			newAv.add(new int[]{nodeRow,nodeCol});
			// base cases: out of bounds
			if ((nodeRow >= boardsize || nodeCol >= boardsize)|| nodeRow < 0 || nodeCol < 0)
			{
				continue;
			}
			// base case: is a liberty
			if (board[nodeRow][nodeCol] == 0)
			{
				return true;
			}
			// base case: is an opposite colour piece
			if (opp(board[nodeRow][nodeCol]-1) == color) continue;
			// then if its a same colour piece, then we need to check the adjacent positions to see if they are alive
			queue.add(new Node(nodeRow+1,nodeCol,newAv));
			queue.add(new Node(nodeRow-1,nodeCol,newAv));
			queue.add(new Node(nodeRow,nodeCol+1,newAv));
			queue.add(new Node(nodeRow,nodeCol-1,newAv));
		}
		return false;
	}

	// opposite 
	private int opp (int a)
	{
		if (a == 0) return 1;
		return 0;
	}

	//Recursively kill the group that is at this point return how many killed pieces.
	private int killGroup(int color, int row, int col)
	{
		// Base case oob
		if ((row >= boardsize || col >= boardsize)|| row < 0 || col < 0) return 0;
		// Base case, not right color
		if (board[row][col]-1 != color) return 0;

		board[row][col] = 0; // killed.
		return 1 + killGroup(color,row+1,col) +killGroup(color,row-1,col) + killGroup(color,row,col+1) + killGroup(color,row,col-1);
	}

	
	//Returns a pseudo-random number between min and max, inclusive.
	public static int randInt(int min, int max) {
	    Random rand = new Random();
	    int randomNum = rand.nextInt((max - min) + 1) + min;
	    return randomNum;
	}

	public static void main (String[] args) throws IOException
	{
		MattpkGo mattpkGo = new MattpkGo();
		mattpkGo.setUpBoard();
		mattpkGo.running = true;
		mattpkGo.run();
	} // End main method

} // End class