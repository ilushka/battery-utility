package com.moskovko.myfirstapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.animation.ValueAnimator;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_MESSAGE = "com.moskovko.myfirstapp.MESSAGE";
    private BatteryChargeView mBatteryCharge;
    private Random mRandomChargeGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBatteryCharge = (BatteryChargeView)findViewById(R.id.battery_charge);
        mRandomChargeGenerator = new Random();
    }

    /*
    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText)findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }
    */

    public void startAnimation(View view) {
        /*
        ValueAnimator animation = ValueAnimator.ofFloat(mBatteryCharge.getCurrentChargeLevel(),
                        mRandomChargeGenerator.nextFloat());
        animation.setDuration(1500);
        animation.addUpdateListener(mBatteryCharge);
        animation.start();
        */
        mBatteryCharge.setCurrentChargeLevel(mRandomChargeGenerator.nextFloat());
    }
}
