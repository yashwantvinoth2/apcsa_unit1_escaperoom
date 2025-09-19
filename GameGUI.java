import java.awt.Color;
import java.awt.Font;
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

  // Add this field to track last direction
  private int lastDx = 0;
  private int lastDy = 0;

  // Add these fields to your GameGUI class:
  private boolean onTrap = false;
  private Rectangle currentTrap = null;
  private javax.swing.Timer trapTimer = null;

  // flag to indicate screen tint when on a trap
  private boolean trapTint = false;

  // Add this field at the top of your class:
  private String scoreMsg = "";

  // Add a field to track game state
  private boolean gameWon = false;

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
    infoButton.setFocusable(false);
    infoButton.setMargin(new java.awt.Insets(0,0,0,0));
    infoButton.setFont(infoButton.getFont().deriveFont(10f));

  frame.setResizable(false);
  frame.setVisible(true);

    //use arrow keys to move player around the board
    this.setFocusable(true);
    this.requestFocusInWindow();

    this.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_RIGHT) {
          movePlayer(SPACE_SIZE, 0);
          lastDx = SPACE_SIZE;
          lastDy = 0;
        } else if (key == KeyEvent.VK_LEFT) {
          movePlayer(-SPACE_SIZE, 0);
          lastDx = -SPACE_SIZE;
          lastDy = 0;
        } else if (key == KeyEvent.VK_DOWN) {
          movePlayer(0, SPACE_SIZE);
          lastDx = 0;
          lastDy = SPACE_SIZE;
        } else if (key == KeyEvent.VK_UP) {
          movePlayer(0, -SPACE_SIZE);
          lastDx = 0;
          lastDy = -SPACE_SIZE;
        } else if (key == KeyEvent.VK_D) { // D key for disarm
          if (onTrap && currentTrap != null && currentTrap.getWidth() > 0) {
            // Disarm the trap in time
            System.out.println("TRAP IS SPRUNG!");
            EscapeRoom.score += 5;
            setScore(EscapeRoom.score);
            scoreMsg = "+5 from disarming trap";
            currentTrap.setSize(0, 0);
            repaint();
            onTrap = false;
            // clear tint and stop timer
            trapTint = false;
            if (trapTimer != null && trapTimer.isRunning()) {
              trapTimer.stop();
            }
            currentTrap = null;
          } else {
            int result = springTrap(0, 0);
            EscapeRoom.score += result;
            setScore(EscapeRoom.score);
          }
          GameGUI.this.requestFocusInWindow();
        } else if (key == KeyEvent.VK_SPACE) { // Spacebar for jump
          // Only jump if a direction has been set
          if (lastDx != 0 || lastDy != 0) {
            // Check for walls in both spaces
            boolean blocked = false;
            int currX = x;
            int currY = y;
            for (int i = 1; i <= 2; i++) {
              int testX = currX + lastDx / 2 * i;
              int testY = currY + lastDy / 2 * i;
              for (Rectangle r : walls) {
                if (r.contains(testX, testY)) {
                  blocked = true;
                  break;
                }
              }
              if (blocked) break;
            }
            if (!blocked) {
              movePlayer(lastDx * 2 / Math.abs(lastDx + lastDy == 0 ? 1 : 1), lastDy * 2 / Math.abs(lastDx + lastDy == 0 ? 1 : 1));
            } else {
              System.out.println("Can't jump over a wall!");
            }
          }
        } else if (key == KeyEvent.VK_R) {
      // allow R to restart: generate a new board and reset state
      // stop any running trap timer
      if (trapTimer != null && trapTimer.isRunning()) trapTimer.stop();
      onTrap = false;
      currentTrap = null;
      trapTint = false;
      // regenerate the board (new walls/prizes/traps)
      createBoard();
      // reset player and score
      x = START_LOC_X;
      y = START_LOC_Y;
      playerSteps = 0;
      EscapeRoom.score = 0;
      setScore(0);
      gameWon = false;
      // clear any temporary score message (magenta text)
      scoreMsg = "";
      repaint();
        } else if (key == KeyEvent.VK_Q) {
            // allow Q to quit the game
            endGame();
            System.exit(0);
        }
      }
    });
    
    // finalize the Info button placement and behavior at the top-right
    infoButton.setBounds(WIDTH - btnW - 30, 6, btnW, btnH);
    // show the full instructions dialog when clicked
    infoButton.addActionListener(ae -> {
      showInfoDialog();
      this.requestFocusInWindow();
    });
    // add the finalized info button to the game canvas so it appears over the grid
    this.add(infoButton);

    frame.setResizable(false);
    frame.setVisible(true);

    // set default config
    totalWalls = 20;
    totalPrizes = 3;
    totalTraps = 5;
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
    // Special case: attempting to move off the right edge should end the game
    if (newX > WIDTH - SPACE_SIZE)
    {
      // check if a wall is in the way between current x and the off-grid point
      for (Rectangle r: walls)
      {
        int startX =  (int)r.getX();
        int startY =  (int)r.getY();
        int endY = (int) r.getY() + (int)r.getHeight();

        if ((incrx > 0) && (x <= startX) && (startX <= newX) && (y >= startY) && (y <= endY))
        {
          // wall blocks exit to the right: treat as hitting a wall
          System.out.println("A WALL IS IN THE WAY");
          EscapeRoom.score -= 1;
          setScore(EscapeRoom.score);
          scoreMsg = "-1 from running into a wall";
          repaint();
          return -hitWallVal;
        }
      }

      // no wall blocking: player exits to the right and wins
      System.out.println("YOU MADE IT OFF THE GRID!");
      // move player off the grid so other logic that checks playerLoc will recognize end
      x = newX;
      playerLoc.setLocation(x, y);
      gameWon = true;
      scoreMsg = "You win! Final score: " + EscapeRoom.score;
      repaint();
      return 0;
    }

    // normal off-grid handling (other sides) -> penalty
    if ( (newX < 0 || newY < 0 || newY > HEIGHT-SPACE_SIZE) )
    {
      System.out.println ("OFF THE GRID!");
      EscapeRoom.score -= 1;
      setScore(EscapeRoom.score);
      scoreMsg = "-1 from going off the grid";
      repaint();
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
        EscapeRoom.score -= 1;
        setScore(EscapeRoom.score);
        scoreMsg = "-1 from running into a wall";
        repaint();
        return -hitWallVal;
      }
      else if ((incrx < 0) && (x >= startX) && (startX >= newX) && (y >= startY) && (y <= endY))
      {
        System.out.println("A WALL IS IN THE WAY");
        EscapeRoom.score -= 1;
        setScore(EscapeRoom.score);
        scoreMsg = "-1 from running into a wall";
        repaint();
        return -hitWallVal;
      }
      else if ((incry > 0) && (y <= startY && startY <= newY && x >= startX && x <= endX))
      {
        System.out.println("A WALL IS IN THE WAY");
        EscapeRoom.score -= 1;
        setScore(EscapeRoom.score);
        scoreMsg = "-1 from running into a wall";
        repaint();
        return -hitWallVal;
      }
      else if ((incry < 0) && (y >= startY) && (startY >= newY) && (x >= startX) && (x <= endX))
      {
        System.out.println("A WALL IS IN THE WAY");
        EscapeRoom.score -= 1;
        setScore(EscapeRoom.score);
        scoreMsg = "-1 from running into a wall";
        repaint();
        return -hitWallVal;
      }     
    }

    // check for trap at new location
    for (Rectangle r : traps)
    {
      if (r.getWidth() > 0 && r.contains(newX, newY))
      {
        onTrap = true;
        currentTrap = r;

        // set tint and notify in terminal
        trapTint = true;
        System.out.println("You stepped on a trap! You have 2 seconds to disarm it.");
        repaint();

        // If a previous timer is running, stop it
        if (trapTimer != null && trapTimer.isRunning()) {
          trapTimer.stop();
        }

        // Start a new timer for 2 seconds (2000 ms)
        trapTimer = new javax.swing.Timer(2000, evt -> {
          if (onTrap && currentTrap != null && currentTrap.getWidth() > 0) {
            System.out.println("Trap triggered! -10 points.");
            EscapeRoom.score -= 10;
            setScore(EscapeRoom.score);
            scoreMsg = "-10 from not disarming a trap";
            // Remove the trap
            currentTrap.setSize(0, 0);
            // clear tint and state
            trapTint = false;
            repaint();
            onTrap = false;
            currentTrap = null;
          }
        });
        trapTimer.setRepeats(false);
        trapTimer.start();
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
        EscapeRoom.score += 5;
        setScore(EscapeRoom.score);
        scoreMsg = "+5 from coin";
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
    playerLoc.setLocation(x, y);
    repaint();

  // (win condition is now when player moves off the right edge without a blocking wall)
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
          scoreMsg = "+5 from disarming trap";
          repaint();
          return trapVal;
        }
      }
    }
    // no trap here, penalty
    System.out.println("THERE IS NO TRAP HERE TO SPRING");
    scoreMsg = "-5 from trying to spring a non-existent trap";
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
        scoreMsg = "+5 from coin";
        p.setSize(0,0);
        repaint();
        return prizeVal;
      }
    }
    System.out.println("OOPS, NO PRIZE HERE");
    scoreMsg = "-5 from trying to pick up a non-existent prize";
    repaint();
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
  g2.setColor(Color.MAGENTA);
  g2.drawString(scoreMsg, 100, 12); // Draw message next to score
  

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

  // draw player
  if (player != null) {
    g2.drawImage(player, x, y, this);
  }

  // draw red tint when standing on a trap
  if (trapTint) {
    g2.setColor(new Color(255, 0, 0, 80)); // translucent red
    g2.fillRect(0, 0, getWidth(), getHeight());
  }

  // draw win message overlay if game is won (draw last so it overlays walls/player)
  if (gameWon) {
    // Draw a dark, semi-transparent rectangle over the whole component
    g2.setColor(new Color(0, 0, 0, 220)); // More opaque for a darker overlay
    g2.fillRect(0, 0, getWidth(), getHeight());

    g2.setColor(Color.YELLOW);
    // use a larger font for visibility
    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
    g2.drawString("Congratulations! You win!", 80, 140);
    g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 22f));
    g2.drawString("Final score: " + EscapeRoom.score, 80, 180);
    g2.drawString("Press 'R' to restart or 'Q' to quit.", 80, 220);
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
                + " - Jump over squares by pressing the spacebar (after moving in a direction with an arrow key, doesn't jump over walls)\n"
               + " - Running into walls or going off the grid decreases the score\n"
               + "  - Landmines are placed in random unknown places in the grid, and running over them reduces the score\n"
               + "  - Defuse/Spring Traps by typing 'd' to increase the score and avoid the trap within 2 seconds\n"
                + "  - The screen turns red when a trap is about to be triggered\n"
                + "  - Go off the grid on the right side to end the game\n"
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
