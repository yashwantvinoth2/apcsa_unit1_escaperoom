import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * A Game board on which to place and move players.
 * 
 * @author PLTW
 * @version 1.0
 */
public class GameGUI extends JComponent
{
  static final long serialVersionUID = 141L; // problem 1.4.1

  private static final int WIDTH = 510;
  private static final int HEIGHT = 360;
  private static final int SPACE_SIZE = 60;
  private static final int GRID_W = 8;
  private static final int GRID_H = 5;
  private static final int START_LOC_X = 15;
  private static final int START_LOC_Y = 15;
  
  // initial placement of player
  int x = START_LOC_X; 
  int y = START_LOC_Y;

  // grid image to show in background
  private Image bgImage;

  // player image and info
  private Image player;
  private Point playerLoc;
  private int playerSteps;
  // score to display in GUI
  private int guiScore = 0;

  /**
   * Update the score displayed in the GUI.
   */
  public void setScore(int s)
  {
    guiScore = s;
    repaint();
  }

  public int getGuiScore()
  {
    return guiScore;
  }

  // walls, prizes, traps
  private int totalWalls;
  private Rectangle[] walls; 
  private Image prizeImage;
  private int totalPrizes;
  private Rectangle[] prizes;
  private int totalTraps;
  private Rectangle[] traps;
  // keep original locations so replay can restore exact original positions
  private Rectangle[] origPrizes;
  private Rectangle[] origTraps;

  // scores, sometimes awarded as (negative) penalties
  private int prizeVal = 1;
  private int trapVal = 5;
  private int endVal = 10;
  private int offGridVal = 5; // penalty only
  private int hitWallVal = 5;  // penalty only

  // game frame
  private JFrame frame;

  /**
   * Constructor for the GameGUI class.
   * Creates a frame with a background image and a player that will move around the board.
   */
  public GameGUI()
  {
    
    try {
      bgImage = ImageIO.read(new File("grid.png"));      
    } catch (Exception e) {
      System.err.println("Could not open file grid.png");
    }      
    try {
      prizeImage = ImageIO.read(new File("coin.png"));      
    } catch (Exception e) {
      System.err.println("Could not open file coin.png");
    }
  
    // player image, student can customize this image by changing file on disk
    try {
      player = ImageIO.read(new File("player.png"));      
    } catch (Exception e) {
     System.err.println("Could not open file player.png");
    }
    // save player location
    playerLoc = new Point(x,y);

    // create the game frame
    frame = new JFrame();
    frame.setTitle("EscapeRoom");
    frame.setSize(WIDTH, HEIGHT);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

  // use absolute positioning so we can place components (the game canvas and a button)
  frame.setLayout(null);
  // place this component to fill the frame
  this.setBounds(0, 0, WIDTH, HEIGHT);
  // set null layout on the canvas so child components can be positioned
  this.setLayout(null);
  frame.add(this);

  // add an Info button that sits on the game canvas (so it appears over the grid)
  JButton infoButton = new JButton("info");
    // make the button as small as reasonably possible
    int btnW = 28;
    int btnH = 20;
    infoButton.setBounds(WIDTH - btnW - 25, 6, btnW, btnH);
    infoButton.setFocusable(false);
    infoButton.setMargin(new java.awt.Insets(0,0,0,0));
    infoButton.setFont(infoButton.getFont().deriveFont(10f));
    infoButton.addActionListener(ae -> showInfoDialog());
  // add button to the canvas instead of the frame so it is on the grid
  this.add(infoButton);

  frame.setResizable(false);
  frame.setVisible(true);

    // set default config
    totalWalls = 20;
    totalPrizes = 3;
    totalTraps = 5;

    // Add key listener for arrow keys
    frame.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_RIGHT) {
          movePlayer(SPACE_SIZE, 0);
        } else if (key == KeyEvent.VK_LEFT) {
          movePlayer(-SPACE_SIZE, 0);
        } else if (key == KeyEvent.VK_DOWN) {
          movePlayer(0, SPACE_SIZE);
        } else if (key == KeyEvent.VK_UP) {
          movePlayer(0, -SPACE_SIZE);
        } else if (key == KeyEvent.VK_R) {
          // teleport to start when 'R' is pressed
          teleportToStart();
        } else if (key == KeyEvent.VK_Q) {
          // quit the game when 'Q' is pressed
          endGame();
        }
      }
    });
  }

 /**
  * After a GameGUI object is created, this method adds the walls, prizes, and traps to the gameboard.
  * Note that traps and prizes may occupy the same location.
  */
 


  public void createBoard()
  {
    traps = new Rectangle[totalTraps];
    createTraps();
    
    prizes = new Rectangle[totalPrizes];
    createPrizes();

    walls = new Rectangle[totalWalls];
    createWalls();
  }

  /**
   * Increment/decrement the player location by the amount designated.
   * This method checks for bumping into walls and going off the grid,
   * both of which result in a penalty.
   * <P>
   * precondition: amount to move is not larger than the board, otherwise player may appear to disappear
   * postcondition: increases number of steps even if the player did not actually move (e.g. bumping into a wall)
   * <P>
   * @param incrx amount to move player in x direction
   * @param incry amount to move player in y direction
   * @return penalty score for hitting a wall or potentially going off the grid, 0 otherwise
   */
  public int movePlayer(int incrx, int incry)
  {
    int newX = x + incrx;
    int newY = y + incry;
    
    // increment regardless of whether player really moves
    playerSteps++;

    // check if off grid horizontally and vertically
    if ( (newX < 0 || newX > WIDTH-SPACE_SIZE) || (newY < 0 || newY > HEIGHT-SPACE_SIZE) )
    {
      System.out.println ("OFF THE GRID!");
      // deduct one point from global score and update GUI
      EscapeRoom.score -= 1;
      setScore(EscapeRoom.score);
      return -offGridVal;
    }

    // determine if a wall is in the way
    for (Rectangle r: walls)
    {
      int startX =  (int)r.getX();
      int endX  =  (int)r.getX() + (int)r.getWidth();
      int startY =  (int)r.getY();
      int endY = (int) r.getY() + (int)r.getHeight();

      if ((incrx > 0) && (x <= startX) && (startX <= newX) && (y >= startY) && (y <= endY))
      {
        System.out.println("A WALL IS IN THE WAY");
        // deduct one point from global score and update GUI
        EscapeRoom.score -= 1;
        setScore(EscapeRoom.score);
        return -hitWallVal;
      }
      else if ((incrx < 0) && (x >= startX) && (startX >= newX) && (y >= startY) && (y <= endY))
      {
        System.out.println("A WALL IS IN THE WAY");
        // deduct one point from global score and update GUI
        EscapeRoom.score -= 1;
        setScore(EscapeRoom.score);
        return -hitWallVal;
      }
      else if ((incry > 0) && (y <= startY && startY <= newY && x >= startX && x <= endX))
      {
        System.out.println("A WALL IS IN THE WAY");
        // deduct one point from global score and update GUI
        EscapeRoom.score -= 1;
        setScore(EscapeRoom.score);
        return -hitWallVal;
      }
      else if ((incry < 0) && (y >= startY) && (startY >= newY) && (x >= startX) && (x <= endX))
      {
        System.out.println("A WALL IS IN THE WAY");
        // deduct one point from global score and update GUI
        EscapeRoom.score -= 1;
        setScore(EscapeRoom.score);
        return -hitWallVal;
      }     
    }

    // check for trap at new location
    for (Rectangle r : traps)
    {
      if (r.getWidth() > 0 && r.contains(newX, newY))
      {
        System.out.println("You stepped on a trap!");
        break;
      }
    }

    // check for prize at new location (coin)
    for (Rectangle p : prizes)
    {
      if (p.getWidth() > 0 && p.contains(newX, newY))
      {
        System.out.println("YOU PICKED UP A PRIZE!");
        // increment the global score and update GUI display
        EscapeRoom.score += 1;
        setScore(EscapeRoom.score);
        // remove the prize so it cannot be picked up again
        p.setSize(0,0);
        // move player onto the prize square so graphics update correctly
        x += incrx;
        y += incry;
        repaint();
        // return 0 because the GUI already updated the global score
        return 0;
      }
    }

    // all is well, move player
    x += incrx;
    y += incry;
    repaint();   
    return 0;   
  }

  /**
   * Check for a trap where the player will land
   *
   * <P>
   * precondition: newx and newy must be the amount a player regularly moves, otherwise an existing trap may go undetected
   * <P>
   * @param newx a location indicating the space to the right or left of the player
   * @param newy a location indicating the space above or below the player
   * @return true if the new location has a trap that has not been sprung, false otherwise
   */
  public boolean isTrap(int newx, int newy)
  {
    double px = x + newx;
    double py = y + newy;

    for (Rectangle r : traps)
    {
      // zero size traps have already been sprung, ignore
      if (r.getWidth() > 0 && r.contains(px, py))
      {
        return true;
      }
    }
    // there is no trap 
    return false;
  }

  /**
   * Spring the trap. Traps can only be sprung once and attempts to spring
   * a sprung trap results in a penalty.
   * <P>
   * precondition: newx and newy must be the amount a player regularly moves, otherwise an existing trap may go unsprung
   * <P>
   * @param newx a location indicating the space to the right or left of the player
   * @param newy a location indicating the space above or below the player
   * @return a positive score if a trap is sprung, otherwise a negative penalty for trying to spring a non-existent trap
   */
  public int springTrap(int newx, int newy)
  {
    double px = x + newx;
    double py = y + newy;

    for (Rectangle r : traps)
    {
      if (r.contains(px, py))
      {
        // zero size traps indicate it has been sprung, cannot spring again, so ignore
        if (r.getWidth() > 0)
        {
          r.setSize(0, 0);
          System.out.println("TRAP IS SPRUNG!");
          repaint();
          return trapVal;
        }
      }
    }
    // no trap here, penalty
    System.out.println("THERE IS NO TRAP HERE TO SPRING");
    return -trapVal;
  }

  /**
   * Pickup a prize and score points. If no prize is in that location, this results in a penalty.
   * <P>
   * @return positive score if a location had a prize to be picked up, otherwise a negative penalty
   */
  public int pickupPrize()
  {
    double px = playerLoc.getX();
    double py = playerLoc.getY();

    for (Rectangle p: prizes)
    {
      // DEBUG: System.out.println("prizex:" + p.getX() + " prizey:" + p.getY() + "\npx: " + px + " py:" + py);
      // if location has a prize, pick it up
      if (p.getWidth() > 0 && p.contains(px, py))
      {
        System.out.println("YOU PICKED UP A PRIZE!");
        p.setSize(0,0);
        repaint();
        return prizeVal;
      }
    }
    System.out.println("OOPS, NO PRIZE HERE");
    return -prizeVal;  
  }

  /**
   * Return the numbers of steps the player has taken.
   * <P>
   * @return the number of steps
   */
  public int getSteps()
  {
    return playerSteps;
  }
  
  /**
   * Set the designated number of prizes in the game.  This can be used to customize the gameboard configuration.
   * <P>
   * precondition p must be a positive, non-zero integer
   * <P>
   * @param p number of prizes to create
   */
  public void setPrizes(int p) 
  {
    totalPrizes = p;
  }
  
  /**
   * Set the designated number of traps in the game. This can be used to customize the gameboard configuration.
   * <P>
   * precondition t must be a positive, non-zero integer
   * <P>
   * @param t number of traps to create
   */
  public void setTraps(int t) 
  {
    totalTraps = t;
  }
  
  /**
   * Set the designated number of walls in the game. This can be used to customize the gameboard configuration.
   * <P>
   * precondition t must be a positive, non-zero integer
   * <P>
   * @param w number of walls to create
   */
  public void setWalls(int w) 
  {
    totalWalls = w;
  }

  /**
   * Reset the board to replay existing game. The method can be called at any time but results in a penalty if called
   * before the player reaches the far right wall.
   * <P>
   * @return positive score for reaching the far right wall, penalty otherwise
   */
  public int replay()
  {

    int win = playerAtEnd();
  
    // restore prizes and traps to their original positions and sizes
    if (origPrizes != null && prizes != null) {
      for (int i = 0; i < origPrizes.length; i++) {
        Rectangle op = origPrizes[i];
        if (op != null) {
          prizes[i] = new Rectangle((int)op.getX(), (int)op.getY(), (int)op.getWidth(), (int)op.getHeight());
        }
      }
    }
    if (origTraps != null && traps != null) {
      for (int i = 0; i < origTraps.length; i++) {
        Rectangle ot = origTraps[i];
        if (ot != null) {
          traps[i] = new Rectangle((int)ot.getX(), (int)ot.getY(), (int)ot.getWidth(), (int)ot.getHeight());
        }
      }
    }

    // move player to start of board
    x = START_LOC_X;
    y = START_LOC_Y;
    playerSteps = 0;
    repaint();
    return win;
  }

 /**
  * End the game, checking if the player made it to the far right wall.
  * <P>
  * @return positive score for reaching the far right wall, penalty otherwise
  */
  public int endGame() 
  {
    int win = playerAtEnd();
  
    setVisible(false);
    frame.dispose();
    return win;
  }

  /*------------------- public methods not to be called as part of API -------------------*/

  /** 
   * For internal use and should not be called directly: Users graphics buffer to paint board elements.
   */
  @Override
  protected void paintComponent(Graphics g)
{
  super.paintComponent(g);
  Graphics2D g2 = (Graphics2D) g;

  // draw background
  if (bgImage != null) {
    g2.drawImage(bgImage, 0, 0, this);
  }

  // draw Score in top-left
  g2.setColor(Color.BLUE);
  g2.drawString("Score: " + guiScore, 10, 12);

  // draw walls
  g2.setColor(Color.BLACK);
  if (walls != null) {
    for (Rectangle r : walls) {
      g2.fill(r);
    }
  }

  // draw prizes
  if (prizes != null && prizeImage != null) {
    for (Rectangle p : prizes) {
      if (p.getWidth() > 0) {
        g2.drawImage(prizeImage, (int)p.getX(), (int)p.getY(), this);
      }
    }
  }

  // draw traps in RED so they are visible
  g2.setColor(Color.RED);
  if (traps != null) {
    for (Rectangle t : traps) {
      if (t.getWidth() > 0) {
        g2.fill(t);
      }
    }
  }

  // draw player
  if (player != null) {
    g2.drawImage(player, x, y, this);
  }
}

  /*------------------- private methods -------------------*/

  /*
   * Add randomly placed prizes to be picked up.
   * Note:  prizes and traps may occupy the same location, with traps hiding prizes
   */
  private void createPrizes()
  {
    int s = SPACE_SIZE; 
    Random rand = new Random();
    origPrizes = new Rectangle[totalPrizes];
    prizes = new Rectangle[totalPrizes];
    for (int numPrizes = 0; numPrizes < totalPrizes; numPrizes++)
    {
    int h = rand.nextInt(GRID_H);
    int w = rand.nextInt(GRID_W);

    Rectangle r = new Rectangle((w*s + 15),(h*s + 15), 15, 15);
    origPrizes[numPrizes] = new Rectangle((int)r.getX(), (int)r.getY(), (int)r.getWidth(), (int)r.getHeight());
    prizes[numPrizes] = new Rectangle((int)r.getX(), (int)r.getY(), (int)r.getWidth(), (int)r.getHeight());
    }
  }

  /*
   * Add randomly placed traps to the board. They will be painted white and appear invisible.
   * Note:  prizes and traps may occupy the same location, with traps hiding prizes
   */
  private void createTraps()
  {
    int s = SPACE_SIZE; 
    Random rand = new Random();
    origTraps = new Rectangle[totalTraps];
    traps = new Rectangle[totalTraps];
    for (int numTraps = 0; numTraps < totalTraps; numTraps++)
    {
    int h = rand.nextInt(GRID_H);
    int w = rand.nextInt(GRID_W);

    Rectangle r = new Rectangle((w*s + 15),(h*s + 15), 15, 15);
    origTraps[numTraps] = new Rectangle((int)r.getX(), (int)r.getY(), (int)r.getWidth(), (int)r.getHeight());
    traps[numTraps] = new Rectangle((int)r.getX(), (int)r.getY(), (int)r.getWidth(), (int)r.getHeight());
    }
  }

  /*
   * Add walls to the board in random locations 
   */
  private void createWalls()
  {
     int s = SPACE_SIZE; 

     Random rand = new Random();
     for (int numWalls = 0; numWalls < totalWalls; numWalls++)
     {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);

      Rectangle r;
       if (rand.nextInt(2) == 0) 
       {
         // vertical wall
         r = new Rectangle((w*s + s - 5),h*s, 8,s);
       }
       else
       {
         /// horizontal
         r = new Rectangle(w*s,(h*s + s - 5), s, 8);
       }
       walls[numWalls] = r;
     }
  }

  /**
   * Show the Info dialog with the exact instructions.
   */
  private void showInfoDialog()
  {
    String msg = " - Use the arrow keys to navigate through the gate\n"
               + " - Getting a coin increases the score\n"
               + " - Running into walls or going off the grid decreases the score\n"
               + "  - Landmines are placed in random unknown places in the grid, and running over them ends the attempt\n"
               + " - Type 'r' to restart the game\n"
               + "  - Type 'q' to quit the game";
    JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Public wrapper to open the info dialog from outside the class.
   */
  public void openInfo()
  {
    showInfoDialog();
  }

  /**
   * Reset the game board back to original positions and reset player to start.
   * This will restore coins and traps to their original locations and reset player steps.
   */
  public void resetGame()
  {
    // restore prizes/traps from original buffers
    if (origPrizes != null) {
      prizes = new Rectangle[origPrizes.length];
      for (int i = 0; i < origPrizes.length; i++) {
        Rectangle op = origPrizes[i];
        if (op != null) prizes[i] = new Rectangle((int)op.getX(), (int)op.getY(), (int)op.getWidth(), (int)op.getHeight());
      }
    }
    if (origTraps != null) {
      traps = new Rectangle[origTraps.length];
      for (int i = 0; i < origTraps.length; i++) {
        Rectangle ot = origTraps[i];
        if (ot != null) traps[i] = new Rectangle((int)ot.getX(), (int)ot.getY(), (int)ot.getWidth(), (int)ot.getHeight());
      }
    }
    // reset player
    x = START_LOC_X;
    y = START_LOC_Y;
    playerSteps = 0;
    repaint();
  }

  /**
   * Teleport the player to the start (top-left) corner.
   */
  public void teleportToStart()
  {
    x = START_LOC_X;
    y = START_LOC_Y;
    // update playerLoc if present
    if (playerLoc == null) playerLoc = new Point(x,y);
    else playerLoc.setLocation(x,y);

    // If original prize/trap positions are recorded, ensure any missing ones are restored.
    if (origPrizes != null) {
      if (prizes == null) prizes = new Rectangle[origPrizes.length];
      for (int i = 0; i < origPrizes.length; i++) {
        Rectangle op = origPrizes[i];
        if (op == null) continue;
        // recreate prize if it doesn't exist or was picked up (zero size)
        if (prizes[i] == null || prizes[i].getWidth() == 0) {
          prizes[i] = new Rectangle((int)op.getX(), (int)op.getY(), (int)op.getWidth(), (int)op.getHeight());
        }
      }
    }

    if (origTraps != null) {
      if (traps == null) traps = new Rectangle[origTraps.length];
      for (int i = 0; i < origTraps.length; i++) {
        Rectangle ot = origTraps[i];
        if (ot == null) continue;
        // recreate trap if it doesn't exist or was sprung (zero size)
        if (traps[i] == null || traps[i].getWidth() == 0) {
          traps[i] = new Rectangle((int)ot.getX(), (int)ot.getY(), (int)ot.getWidth(), (int)ot.getHeight());
        }
      }
    }

    // reset score to 0 when teleporting to start
    EscapeRoom.score = 0;
    setScore(0);
    repaint();
  }

  /**
   * Checks if player as at the far right of the board 
   * @return positive score for reaching the far right wall, penalty otherwise
   */
  private int playerAtEnd() 
  {
    int score;

    double px = playerLoc.getX();
    if (px > (WIDTH - 2*SPACE_SIZE))
    {
      System.out.println("YOU MADE IT!");
      score = endVal;
    }
    else
    {
      System.out.println("OOPS, YOU QUIT TOO SOON!");
      score = -endVal;
    }
    return score;
  
  }
}
