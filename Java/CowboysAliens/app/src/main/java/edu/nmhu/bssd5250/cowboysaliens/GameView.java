package edu.nmhu.bssd5250.cowboysaliens;

/**
 * Created by scott.bing on 11/7/2014.
 */
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class GameView extends View
{
    // constant for accessing the high score in SharedPreference
    private static final String HIGH_SCORE = "HIGH_SCORE";
    private SharedPreferences preferences; // stores the high score

    // variables for managing the game
    private int gamePiecesTouched; // number of gamePieces touched
    private int score; // current score
    private int level; // current level
    private int viewWidth; // stores the width of this View
    private int viewHeight; // stores the height of this view
    private long animationTime; // how long each gamePiece remains on the screen
    private boolean gameOver; // whether the game has ended
    private boolean gamePaused; // whether the game has ended
    private boolean dialogDisplayed; // whether the game has ended
    private int highScore; // the game's all time high score

    // collections of gamePieces (ImageViews) and Animators
    private final Queue<ImageView> gamePieces =
            new ConcurrentLinkedQueue<ImageView>();
    private final Queue<Animator> animators =
            new ConcurrentLinkedQueue<Animator>();

    private TextView highScoreTextView; // displays high score
    private TextView currentScoreTextView; // displays current score
    private TextView levelTextView; // displays current level
    private LinearLayout livesLinearLayout; // displays lives remaining
    private RelativeLayout relativeLayout; // displays gamePieces
    private Resources resources; // used to load resources
    private LayoutInflater layoutInflater; // used to inflate GUIs

    // time in milliseconds for gamePiece and touched gamePiece animations
    private static final int INITIAL_ANIMATION_DURATION = 6000;
    private static final Random random = new Random(); // for random coords
    private static final int GAME_PIECE_DIAMETER = 300; // initial gamePiece size
    private static final float SCALE_X = 0.25f; // end animation x scale
    private static final float SCALE_Y = 0.25f; // end animation y scale
    private static final int INITIAL_GAMEPIECES = 5; // initial # of gamePieces
    private static final int GAMEPIECE_DELAY = 500; // delay in milliseconds
    private static final int LIVES = 3; // start with 3 lives
    private static final int MAX_LIVES = 7; // maximum # of total lives
    private static final int NEW_LEVEL = 10; // gamePieces to reach new level
    private Handler gamePieceHandler; // adds new gamePieces to the game

    // sound IDs, constants and variables for the game's sounds
    private static final int HIT_SOUND_ID = 1;
    private static final int MISS_SOUND_ID = 2;
    private static final int DISAPPEAR_SOUND_ID = 3;
    private static final int SOUND_PRIORITY = 1;
    private static final int SOUND_QUALITY = 100;
    private static final int MAX_STREAMS = 4;
    private SoundPool soundPool; // plays sound effects
    private int volume; // sound effect volume
    private Map<Integer, Integer> soundMap; // maps ID to soundpool

    // constructs a new GameControllerView
    public GameView(Context context, SharedPreferences sharedPreferences,
                    RelativeLayout parentLayout)
    {
        super(context);

        // load the high score
        preferences = sharedPreferences;
        highScore = preferences.getInt(HIGH_SCORE, 0);

        // save Resources for loading external values
        resources = context.getResources();

        // save LayoutInflater
        layoutInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        // get references to various GUI components
        relativeLayout = parentLayout;
        livesLinearLayout = (LinearLayout) relativeLayout.findViewById(
                R.id.lifeLinearLayout);
        highScoreTextView = (TextView) relativeLayout.findViewById(
                R.id.highScoreTextView);
        currentScoreTextView = (TextView) relativeLayout.findViewById(
                R.id.scoreTextView);
        levelTextView = (TextView) relativeLayout.findViewById(
                R.id.levelTextView);

        gamePieceHandler = new Handler(); // used to add gamePieces when game starts
    } // end GameControllerView constructor

    // store GameControllerView's width/height
    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh)
    {
        viewWidth = width; // save the new width
        viewHeight = height; // save the new height
    } // end method onSizeChanged

    // called by the GameController Activity when it receives a call to onPause
    public void pause()
    {
        gamePaused = true;
        soundPool.release(); // release audio resources
        soundPool = null;
        cancelAnimations(); // cancel all outstanding animations
    } // end method pause

    // cancel animations and remove ImageViews representing gamePieces
    private void cancelAnimations()
    {
        // cancel remaining animations
        for (Animator animator : animators)
            animator.cancel();

        // remove remaining gamePieces from the screen
        for (ImageView view : gamePieces)
            relativeLayout.removeView(view);

        gamePieceHandler.removeCallbacks(addGamePieceRunnable);
        animators.clear();
        gamePieces.clear();
    } // end method cancelAnimations

    // called by the GameController Activity when it receives a call to onResume
    public void resume(Context context)
    {
        gamePaused = false;
        initializeSoundEffects(context); // initialize app's SoundPool

        if (!dialogDisplayed)
            resetGame(); // start the game
    } // end method resume

    // start a new game
    public void resetGame()
    {
        gamePieces.clear(); // empty the List of gamePieces
        animators.clear(); // empty the List of Animators
        livesLinearLayout.removeAllViews(); // clear old lives from screen

        animationTime = INITIAL_ANIMATION_DURATION; // init animation length
        gamePiecesTouched = 0; // reset the number of gamePieces touched
        score = 0; // reset the score
        level = 1; // reset the level
        gameOver = false; // the game is not over
        displayScores(); // display scores and level

        // add lives
        for (int i = 0; i < LIVES; i++)
        {
            // add life indicator to screen
            livesLinearLayout.addView(
                    (ImageView) layoutInflater.inflate(R.layout.life, null));
        } // end for

        // add INITIAL_GAMEPIECES new gamePieces at GAMEPIECE_DELAY time intervals in ms
        for (int i = 1; i <= INITIAL_GAMEPIECES; ++i)
            gamePieceHandler.postDelayed(addGamePieceRunnable, i * GAMEPIECE_DELAY);
    } // end method resetGame

    // create the app's SoundPool for playing game audio
    private void initializeSoundEffects(Context context)
    {
        // initialize SoundPool to play the app's three sound effects
        soundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC,
                SOUND_QUALITY);

        // set sound effect volume
        AudioManager manager =
                (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // create sound map
        soundMap = new HashMap<Integer, Integer>(); // create new HashMap

        // add each sound effect to the SoundPool
        soundMap.put(HIT_SOUND_ID,
                soundPool.load(context, R.raw.hit, SOUND_PRIORITY));
        soundMap.put(MISS_SOUND_ID,
                soundPool.load(context, R.raw.miss, SOUND_PRIORITY));
        soundMap.put(DISAPPEAR_SOUND_ID,
                soundPool.load(context, R.raw.disappear, SOUND_PRIORITY));
    } // end method initializeSoundEffect

    // display scores and level
    private void displayScores()
    {
        // display the high score, current score and level
        highScoreTextView.setText(
                resources.getString(R.string.high_score) + " " + highScore);
        currentScoreTextView.setText(
                resources.getString(R.string.score) + " " + score);
        levelTextView.setText(
                resources.getString(R.string.level) + " " + level);
    } // end function displayScores

    // Runnable used to add new gamePieces to the game at the start
    private Runnable addGamePieceRunnable = new Runnable()
    {
        public void run()
        {
            addNewGamePiece(); // add a new gamePiece to the game
        } // end method run
    }; // end Runnable

    // adds a new gamePiece at a random location and starts its animation
    public void addNewGamePiece()
    {
        // choose two random coordinates for the starting and ending points
        int x = random.nextInt(viewWidth - GAME_PIECE_DIAMETER);
        int y = random.nextInt(viewHeight - GAME_PIECE_DIAMETER);
        int x2 = random.nextInt(viewWidth - GAME_PIECE_DIAMETER);
        int y2 = random.nextInt(viewHeight - GAME_PIECE_DIAMETER);

        // create new gamePiece
        final ImageView gamePiece =
                (ImageView) layoutInflater.inflate(R.layout.untouched, null);
        gamePieces.add(gamePiece); // add the new gamePiece to our list of gamePieces
        gamePiece.setLayoutParams(new RelativeLayout.LayoutParams(
                GAME_PIECE_DIAMETER, GAME_PIECE_DIAMETER));
        gamePiece.setImageResource(random.nextInt(2) == 0 ?
                R.drawable.cowboy : R.drawable.alien);
        gamePiece.setX(x); // set gamePiece's starting x location
        gamePiece.setY(y); // set gamePiece's starting y location
        gamePiece.setOnClickListener( // listens for gamePiece being clicked
                new OnClickListener()
                {
                    public void onClick(View v)
                    {
                        touchedGamePiece(gamePiece); // handle touched gamePiece
                    } // end method onClick
                } // end OnClickListener
        ); // end call to setOnClickListener
        relativeLayout.addView(gamePiece); // add gamePiece to the screen

        // configure and start gamePiece's animation
        gamePiece.animate().x(x2).y(y2).scaleX(SCALE_X).scaleY(SCALE_Y)
                .setDuration(animationTime).setListener(
                new AnimatorListenerAdapter()
                {
                    @Override
                    public void onAnimationStart(Animator animation)
                    {
                        animators.add(animation); // save for possible cancel
                    } // end method onAnimationStart

                    public void onAnimationEnd(Animator animation)
                    {
                        animators.remove(animation); // animation done, remove

                        if (!gamePaused && gamePieces.contains(gamePiece)) // not touched
                        {
                            missedGamePiece(gamePiece); // lose a life
                        } // end if
                    } // end method onAnimationEnd
                } // end AnimatorListenerAdapter
        ); // end call to setListener
    } // end addNewGamePiece method

    // called when the user touches the screen, but not a gamePiece
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // play the missed sound
        if (soundPool != null)
            soundPool.play(MISS_SOUND_ID, volume, volume,
                    SOUND_PRIORITY, 0, 1f);

        score -= 15 * level; // remove some points
        score = Math.max(score, 0); // do not let the score go below zero
        displayScores(); // update scores/level on screen
        return true;
    } // end method onTouchEvent

    // called when a gamePiece is touched
    private void touchedGamePiece(ImageView gamePiece)
    {
        relativeLayout.removeView(gamePiece); // remove touched gamePiece from screen
        gamePieces.remove(gamePiece); // remove old gamePiece from list

        ++gamePiecesTouched; // increment the number of gamePieces touched
        score += 10 * level; // increment the score

        // play the hit sounds
        if (soundPool != null)
            soundPool.play(HIT_SOUND_ID, volume, volume,
                    SOUND_PRIORITY, 0, 1f);

        // increment level if player touched 10 gamePieces in the current level
        if (gamePiecesTouched % NEW_LEVEL == 0)
        {
            ++level; // increment the level
            animationTime *= 0.95; // make game 5% faster than prior level

            // if the maximum number of lives has not been reached
            if (livesLinearLayout.getChildCount() < MAX_LIVES)
            {
                ImageView life =
                        (ImageView) layoutInflater.inflate(R.layout.life, null);
                livesLinearLayout.addView(life); // add life to screen
            } // end if
        } // end if

        displayScores(); // update score/level on the screen

        if (!gameOver)
            addNewGamePiece(); // add another untouched gamePiece
    } // end method touchedGamePiece

    // called when a gamePiece finishes its animation without being touched
    public void missedGamePiece(ImageView gamePiece)
    {
        gamePieces.remove(gamePiece); // remove gamePiece from gamePieces List
        relativeLayout.removeView(gamePiece); // remove gamePiece from screen

        if (gameOver) // if the game is already over, exit
            return;

        // play the disappear sound effect
        if (soundPool != null)
            soundPool.play(DISAPPEAR_SOUND_ID, volume, volume,
                    SOUND_PRIORITY, 0, 1f);

        // if the game has been lost
        if (livesLinearLayout.getChildCount() == 0)
        {
            gameOver = true; // the game is over

            // if the last game's score is greater than the high score
            if (score > highScore)
            {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(HIGH_SCORE, score);
                editor.commit(); // store the new high score
                highScore = score;
            } // end if

            cancelAnimations();

            // display a high score dialog
            Builder dialogBuilder = new AlertDialog.Builder(getContext());
            dialogBuilder.setTitle(R.string.game_over);
            dialogBuilder.setMessage(resources.getString(R.string.score) +
                    " " + score);
            dialogBuilder.setPositiveButton(R.string.reset_game,
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            displayScores(); // ensure that score is up to date
                            dialogDisplayed = false;
                            resetGame(); // start a new game
                        } // end method onClick
                    } // end DialogInterface
            ); // end call to dialogBuilder.setPositiveButton
            dialogDisplayed = true;
            dialogBuilder.show(); // display the reset game dialog
        } // end if
        else // remove one life
        {
            livesLinearLayout.removeViewAt( // remove life from screen
                    livesLinearLayout.getChildCount() - 1);
            addNewGamePiece(); // add another gamePiece to game
        } // end else
    } // end method missedGamePiece
} // end class GameView
