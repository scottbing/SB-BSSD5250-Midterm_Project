package edu.nmhu.bssd5250.cowboysaliens;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;


public class GameController extends AppCompatActivity {

    private GameView view; // displays and manages the game

    // called when this Activity is first created
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // create a new GameControllerView and add it to the RelativeLayout
        RelativeLayout layout =
                (RelativeLayout) findViewById(R.id.relativeLayout);
        view = new GameView(this, getPreferences(Context.MODE_PRIVATE),
                layout);
        layout.addView(view, 0); // add view to the layout
    } // end method onCreate

    // called when this Activity moves to the background
    @Override
    public void onPause()
    {
        super.onPause();
        view.pause(); // release resources held by the View
    } // end method onPause

    // called when this Activity is brought to the foreground
    @Override
    public void onResume()
    {
        super.onResume();
        view.resume(this); // re-initialize resources released in onPause
    } // end method onResume



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.game_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

} // end class GameController
