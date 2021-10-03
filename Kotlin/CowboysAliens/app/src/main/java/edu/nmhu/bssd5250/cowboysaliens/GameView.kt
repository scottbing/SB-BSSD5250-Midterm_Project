package edu.nmhu.bssd5250.cowboysaliens

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue


class GameView(
    context: Context, // stores the high score
    private val preferences: SharedPreferences,
    parentLayout: RelativeLayout
) : View(context) {
    // variables for managing the game
    private var gamePiecesTouched = 0 // number of gamePieces touched
    private var score = 0 // current score
    private var level = 0 // current level
    private var viewWidth = 0 // stores the width of this View
    private var viewHeight = 0 // stores the height of this view
    private var animationTime : Long = 0// how long each gamePiece remains on the screen
    private var gameOver = false // whether the game has ended
    private var gamePaused = false // whether the game has ended
    private var dialogDisplayed = false // whether the game has ended
    private var highScore = 0 // the game's all time high score

    // collections of gamePieces (ImageViews) and Animators
    private val gamePieces: Queue<ImageView> = ConcurrentLinkedQueue()
    private val animators: Queue<Animator> = ConcurrentLinkedQueue()
    private val highScoreTextView : TextView // displays high score
    private val currentScoreTextView : TextView // displays current score
    private val levelTextView : TextView // displays current level
    private val livesLinearLayout  : LinearLayout// displays lives remaining
    private val relativeLayout : RelativeLayout // displays gamePieces
    private val resource : Resources // used to load resource
    private val layoutInflater : LayoutInflater // used to inflate GUIs
    private val gamePieceHandler : Handler // adds new gamePieces to the game
    private var soundPool: SoundPool? = null // plays sound effects
    private var volume = 0 // sound effect volume
    private var soundMap : MutableMap<Int, Int>? = null // maps ID to soundpool

    // store GameControllerView's width/height
    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        viewWidth = width // save the new width
        viewHeight = height // save the new height
    } // end method onSizeChanged

    // called by the GameController Activity when it receives a call to onPause
    fun pause() {
        gamePaused = true
        soundPool!!.release() // release audio resource
        soundPool = null
        cancelAnimations() // cancel all outstanding animations
    } // end method pause

    // cancel animations and remove ImageViews representing gamePieces
    private fun cancelAnimations() {
        // cancel remaining animations
        for (animator: Animator in animators) animator.cancel()

        // remove remaining gamePieces from the screen
        for (view: ImageView in gamePieces) relativeLayout.removeView(view)
        gamePieceHandler.removeCallbacks(addGamePieceRunnable)
        animators.clear()
        gamePieces.clear()
    } // end method cancelAnimations

    // called by the GameController Activity when it receives a call to onResume
    fun resume(context: Context) {
        gamePaused = false
        initializeSoundEffects(context) // initialize app's SoundPool
        if (!dialogDisplayed) resetGame() // start the game
    } // end method resume

    // start a new game
    fun resetGame() {
        gamePieces.clear() // empty the List of gamePieces
        animators.clear() // empty the List of Animators
        livesLinearLayout.removeAllViews() // clear old lives from screen
        animationTime = INITIAL_ANIMATION_DURATION.toLong() // init animation length
        gamePiecesTouched = 0 // reset the number of gamePieces touched
        score = 0 // reset the score
        level = 1 // reset the level
        gameOver = false // the game is not over
        displayScores() // display scores and level

        // add lives
        for (i in 0 until LIVES) {
            // add life indicator to screen
            livesLinearLayout.addView(
                layoutInflater.inflate(R.layout.life, null) as ImageView
            )
        } // end for

        // add INITIAL_GAMEPIECES new gamePieces at GAMEPIECE_DELAY time intervals in ms
        for (i in 1..INITIAL_GAMEPIECES) gamePieceHandler.postDelayed(
            addGamePieceRunnable,
            (i * GAMEPIECE_DELAY).toLong()
        )
    } // end method resetGame

    // create the app's SoundPool for playing game audio
    private fun initializeSoundEffects(context: Context) {
        // initialize SoundPool to play the app's three sound effects
        soundPool = SoundPool(
            MAX_STREAMS, AudioManager.STREAM_MUSIC,
            SOUND_QUALITY
        )

        // set sound effect volume
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC)

        // create sound map
        soundMap = HashMap() // create new HashMap

        // add each sound effect to the SoundPool
        (soundMap as HashMap<Int, Int>)[HIT_SOUND_ID] = soundPool!!.load(context, R.raw.hit, SOUND_PRIORITY)
        (soundMap as HashMap<Int, Int>)[MISS_SOUND_ID] = soundPool!!.load(context, R.raw.miss, SOUND_PRIORITY)
        (soundMap as HashMap<Int, Int>)[DISAPPEAR_SOUND_ID] =
            soundPool!!.load(context, R.raw.disappear, SOUND_PRIORITY)
    } // end method initializeSoundEffect

    // display scores and level
    private fun displayScores() {
        // display the high score, current score and level
        var tmp_string: String = resource.getString(R.string.high_score).toString() + " " + highScore
        highScoreTextView.text = tmp_string
        tmp_string = resource.getString(R.string.score).toString() + " " + score
        currentScoreTextView.text = tmp_string
        tmp_string = resource.getString(R.string.level).toString() + " " + level
        levelTextView.text = tmp_string
    } // end function displayScores

    // Runnable used to add new gamePieces to the game at the start
    private val addGamePieceRunnable: Runnable = Runnable {
        addNewGamePiece() // add a new gamePiece to the game
    } // end method run

    // end Runnable
    // adds a new gamePiece at a random location and starts its animation
    fun addNewGamePiece() {
        // choose two random coordinates for the starting and ending points
        val x = random.nextInt(viewWidth - GAME_PIECE_DIAMETER)
        val y = random.nextInt(viewHeight - GAME_PIECE_DIAMETER)
        val x2 = random.nextInt(viewWidth - GAME_PIECE_DIAMETER)
        val y2 = random.nextInt(viewHeight - GAME_PIECE_DIAMETER)

        // create new gamePiece
        val gamePiece = layoutInflater.inflate(R.layout.untouched, null) as ImageView
        gamePieces.add(gamePiece) // add the new gamePiece to our list of gamePieces
        gamePiece.layoutParams = RelativeLayout.LayoutParams(
            GAME_PIECE_DIAMETER, GAME_PIECE_DIAMETER
        )
        gamePiece.setImageResource(if (random.nextInt(2) == 0) R.drawable.cowboy else R.drawable.alien)
        gamePiece.x = x.toFloat() // set gamePiece's starting x location
        gamePiece.y = y.toFloat() // set gamePiece's starting y location
        gamePiece.setOnClickListener() // listens for gamePiece being clicked
        {
            touchedGamePiece(gamePiece) // handle touched gamePiece
        } // end method onClick
        // end OnClickListener
        // end call to setOnClickListener
        relativeLayout.addView(gamePiece) // add gamePiece to the screen

        // configure and start gamePiece's animation
        gamePiece.animate().x(x2.toFloat()).y(y2.toFloat()).scaleX(SCALE_X).scaleY(SCALE_Y)
            .setDuration(animationTime).setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        animators.add(animation) // save for possible cancel
                    } // end method onAnimationStart

                    override fun onAnimationEnd(animation: Animator) {
                        animators.remove(animation) // animation done, remove
                        if (!gamePaused && gamePieces.contains(gamePiece)) // not touched
                        {
                            missedGamePiece(gamePiece) // lose a life
                        } // end if
                    } // end method onAnimationEnd
                } // end AnimatorListenerAdapter
            ) // end call to setListener
    } // end addNewGamePiece method

    // called when the user touches the screen, but not a gamePiece
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // play the missed sound
        if (soundPool != null) soundPool!!.play(
            MISS_SOUND_ID, volume.toFloat(), volume.toFloat(),
            SOUND_PRIORITY, 0, 1f
        )
        score -= 15 * level // remove some points
        score = Math.max(score, 0) // do not let the score go below zero
        displayScores() // update scores/level on screen
        return true
    } // end method onTouchEvent

    // called when a gamePiece is touched
    private fun touchedGamePiece(gamePiece: ImageView) {
        relativeLayout.removeView(gamePiece) // remove touched gamePiece from screen
        gamePieces.remove(gamePiece) // remove old gamePiece from list
        ++gamePiecesTouched // increment the number of gamePieces touched
        score += 10 * level // increment the score

        // play the hit sounds
        if (soundPool != null) soundPool!!.play(
            HIT_SOUND_ID, volume.toFloat(), volume.toFloat(),
            SOUND_PRIORITY, 0, 1f
        )

        // increment level if player touched 10 gamePieces in the current level
        if (gamePiecesTouched % NEW_LEVEL == 0) {
            ++level // increment the level
            animationTime *= 0.95.toLong() // make game 5% faster than prior level

            // if the maximum number of lives has not been reached
            if (livesLinearLayout.childCount < MAX_LIVES) {
                val life = layoutInflater.inflate(R.layout.life, null) as ImageView
                livesLinearLayout.addView(life) // add life to screen
            } // end if
        } // end if
        displayScores() // update score/level on the screen
        if (!gameOver) addNewGamePiece() // add another untouched gamePiece
    } // end method touchedGamePiece

    // called when a gamePiece finishes its animation without being touched
    fun missedGamePiece(gamePiece: ImageView) {
        gamePieces.remove(gamePiece) // remove gamePiece from gamePieces List
        relativeLayout.removeView(gamePiece) // remove gamePiece from screen
        if (gameOver) // if the game is already over, exit
            return

        // play the disappear sound effect
        if (soundPool != null) soundPool!!.play(
            DISAPPEAR_SOUND_ID, volume.toFloat(), volume.toFloat(),
            SOUND_PRIORITY, 0, 1f
        )

        // if the game has been lost
        if (livesLinearLayout.childCount == 0) {
            gameOver = true // the game is over

            // if the last game's score is greater than the high score
            if (score > highScore) {
                val editor = preferences.edit()
                editor.putInt(HIGH_SCORE, score)
                editor.commit() // store the new high score
                highScore = score
            } // end if
            cancelAnimations()

            // display a high score dialog
            val dialogBuilder = AlertDialog.Builder(
                context
            )
            dialogBuilder.setTitle(R.string.game_over)
            dialogBuilder.setMessage(
                resource.getString(R.string.score) +
                        " " + score
            )
            dialogBuilder.setPositiveButton(
                R.string.reset_game
            ) { dialog, which ->
                displayScores() // ensure that score is up to date
                dialogDisplayed = false
                resetGame() // start a new game
            } // end method onClick
            // end DialogInterface
            // end call to dialogBuilder.setPositiveButton
            dialogDisplayed = true
            dialogBuilder.show() // display the reset game dialog
        } // end if
        else  // remove one life
        {
            livesLinearLayout.removeViewAt( // remove life from screen
                livesLinearLayout.childCount - 1
            )
            addNewGamePiece() // add another gamePiece to game
        } // end else
    } // end method missedGamePiece

    companion object {
        // constant for accessing the high score in SharedPreference
        private val HIGH_SCORE = "HIGH_SCORE"

        // time in milliseconds for gamePiece and touched gamePiece animations
        private val INITIAL_ANIMATION_DURATION = 6000
        private val random = Random() // for random coords
        private val GAME_PIECE_DIAMETER = 300 // initial gamePiece size
        private val SCALE_X = 0.25f // end animation x scale
        private val SCALE_Y = 0.25f // end animation y scale
        private val INITIAL_GAMEPIECES = 5 // initial # of gamePieces
        private val GAMEPIECE_DELAY = 500 // delay in milliseconds
        private val LIVES = 3 // start with 3 lives
        private val MAX_LIVES = 7 // maximum # of total lives
        private val NEW_LEVEL = 10 // gamePieces to reach new level

        // sound IDs, constants and variables for the game's sounds
        private val HIT_SOUND_ID = 1
        private val MISS_SOUND_ID = 2
        private val DISAPPEAR_SOUND_ID = 3
        private val SOUND_PRIORITY = 1
        private val SOUND_QUALITY = 100
        private val MAX_STREAMS = 4
    }

    // constructs a new GameControllerView
    init {

        // load the high score
        highScore = preferences.getInt(HIGH_SCORE, 0)

        // save Resources for loading external values
        resource = context.resources

        // save LayoutInflater
        layoutInflater = context.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE
        ) as LayoutInflater

        // get references to various GUI components
        relativeLayout = parentLayout
        livesLinearLayout = relativeLayout.findViewById<View>(
            R.id.lifeLinearLayout
        ) as LinearLayout
        highScoreTextView = relativeLayout.findViewById<View>(
            R.id.highScoreTextView
        ) as TextView
        currentScoreTextView = relativeLayout.findViewById<View>(
            R.id.scoreTextView
        ) as TextView
        levelTextView = relativeLayout.findViewById<View>(
            R.id.levelTextView
        ) as TextView
        gamePieceHandler = Handler() // used to add gamePieces when game starts
    } // end GameControllerView constructor
} // end class GameView



