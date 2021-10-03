package edu.nmhu.bssd5250.cowboysaliens

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.RelativeLayout
import android.view.View


class GameController : AppCompatActivity() {

    private var view : GameView? = null // displays and manages the game


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        // create a new GameControllerView and add it to the RelativeLayout
        // create a new GameControllerView and add it to the RelativeLayout
        val layout = findViewById<View>(R.id.relativeLayout) as RelativeLayout
        view = GameView(
            this, getPreferences(MODE_PRIVATE),
            layout
        )
        layout.addView(view, 0) // add view to the layout

    } // end method onCreate

    // called when this Activity moves to the background
    override fun onPause() {
        super.onPause()
        view?.pause() // release resources held by the View
    } // end method onPause


    // called when this Activity is brought to the foreground
    override fun onResume() {
        super.onResume()
        view?.resume(this) // re-initialize resources released in onPause
    } // end method onResume
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.game_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }

}