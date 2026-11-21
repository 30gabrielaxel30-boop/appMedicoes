package com.keyhistory.testfield

import android.app.Activity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView

class FieldActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field)
        val field = findViewById<EditText>(R.id.field_text)
        val last = findViewById<TextView>(R.id.last_sent)
        field.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                last.text = v.text.toString()
                field.setText("")
                true
            } else false
        }
    }
}
