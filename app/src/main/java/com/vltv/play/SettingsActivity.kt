package com.vltv.play

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val switchParental: Switch = findViewById(R.id.switchParental)
        val etPin: EditText = findViewById(R.id.etPin)
        val btnSavePin: Button = findViewById(R.id.btnSavePin)

        // carregar estado atual
        switchParental.isChecked = ParentalControlManager.isEnabled(this)
        etPin.setText(ParentalControlManager.getPin(this))

        switchParental.setOnCheckedChangeListener { _, isChecked ->
            ParentalControlManager.setEnabled(this, isChecked)
        }

        btnSavePin.setOnClickListener {
            val pin = etPin.text.toString().trim()
            if (pin.length != 4) {
                Toast.makeText(this, "PIN precisa ter 4 dígitos", Toast.LENGTH_SHORT).show()
            } else {
                ParentalControlManager.setPin(this, pin)
                Toast.makeText(this, "PIN salvo", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
